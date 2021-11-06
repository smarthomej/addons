/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * Copyright (c) 2021 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.binding.androiddebugbridge.internal;

import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

/**
 * The {@link AndroidDebugBridgeConfiguration} class encapsulates adb device connection logic.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class AndroidDebugBridgeDevice {
    public static final int ANDROID_MEDIA_STREAM = 3;
    private static final String ADB_FOLDER = OpenHAB.getUserDataFolder() + File.separator + ".adb";
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidDebugBridgeDevice.class);
    private static final Pattern VOLUME_PATTERN = Pattern
            .compile("volume is (?<current>\\d.*) in range \\[(?<min>\\d.*)\\.\\.(?<max>\\d.*)]");
    private static final Pattern TAP_EVENT_PATTERN = Pattern.compile("(?<x>\\d+),(?<y>\\d+)");
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern
            .compile("^([A-Za-z]{1}[A-Za-z\\d_\\/]*\\.)+[A-Za-z][A-Za-z\\d_]*$");
    private static final Pattern INTENT_STRING_PATTERN = Pattern
            .compile("intent://(?:[\\w\\./\\-_]*?)#Intent;(?:[\\w\\.\\-_]+=[\\w\\.\\-_]+;)+end");

    private static @Nullable AdbCrypto adbCrypto;

    static {
        try {
            File directory = new File(ADB_FOLDER);
            if (!directory.exists()) {
                directory.mkdir();
            }
            adbCrypto = loadKeyPair(ADB_FOLDER + File.separator + "adb_pub.key",
                    ADB_FOLDER + File.separator + "adb.key");
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
            LOGGER.warn("Unable to setup adb keys: {}", e.getMessage());
        }
    }

    private final ScheduledExecutorService scheduler;

    private String ip = "127.0.0.1";
    private int port = 5555;
    private int timeoutSec = 5;
    private final Map<String, FallbackModes> channelFallbackMap = new HashMap<>();
    private @Nullable Socket socket;
    private @Nullable AdbConnection connection;
    private @Nullable Future<String> commandFuture;

    private Lock commandLock = new ReentrantLock();

    AndroidDebugBridgeDevice(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public void configure(String ip, int port, int timeout) {
        this.ip = ip;
        this.port = port;
        this.timeoutSec = timeout;
    }

    public void sendKeyEvent(String eventCode)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        runAdbShell("input", "keyevent", eventCode);
    }

    public void sendText(String text)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        runAdbShell("input", "text", URLEncoder.encode(text, StandardCharsets.UTF_8));
    }

    public void sendTap(String point)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        Matcher matcher = TAP_EVENT_PATTERN.matcher(point);
        if (!matcher.matches()) {
            LOGGER.warn("Unable to parse tap event");
            return;
        }
        runAdbShell("input", "mouse", "tap", matcher.group("x"), matcher.group("y"));
    }

    public void startPackage(String packageName)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        if (!PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
            LOGGER.warn("{} is not a valid package name", packageName);
            return;
        }
        if (channelFallbackMap.get(START_PACKAGE_CHANNEL) == FallbackModes.MONKEY_LEANBACK_LAUNCHER) {
            startPackageWithMonkeyLeanbackLauncher(packageName);
            return;
        }
        if (channelFallbackMap.get(START_PACKAGE_CHANNEL) == FallbackModes.MONKEY) {
            startPackageWithMonkey(packageName);
            return;
        }
        String output = runAdbShell("am", "start", "-n", packageName);
        if (output.contains("usage: am") || output.contains("Exception")) {
            LOGGER.debug("set fallback {} for {}", FallbackModes.MONKEY, START_PACKAGE_CHANNEL);
            channelFallbackMap.put(START_PACKAGE_CHANNEL, FallbackModes.MONKEY);
            startPackageWithMonkey(packageName);
        }
    }

    private void startPackageWithMonkey(String packageName)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        String result = runAdbShell("monkey", "--pct-syskeys", "0", "-p", packageName, "-v", "1");
        if (result.contains("monkey aborted")) {
            // use LEANBACK launcher if not successfull - see https://stackoverflow.com/a/54929232
            LOGGER.debug("set fallback {} for {}", FallbackModes.MONKEY_LEANBACK_LAUNCHER, START_PACKAGE_CHANNEL);
            channelFallbackMap.put(START_PACKAGE_CHANNEL, FallbackModes.MONKEY_LEANBACK_LAUNCHER);
            startPackageWithMonkeyLeanbackLauncher(packageName);
        }
    }

    private void startPackageWithMonkeyLeanbackLauncher(String packageName)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        String result = runAdbShell("monkey", "--pct-syskeys", "0", "-p", packageName, "-c",
                "android.intent.category.LEANBACK_LAUNCHER", "1");
        if (result.contains("monkey aborted")) {
            LOGGER.debug("removed fallback {}", START_PACKAGE_CHANNEL);
            channelFallbackMap.remove(START_PACKAGE_CHANNEL);
        }
    }

    public void stopPackage(String packageName)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        if (!PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
            LOGGER.warn("{} is not a valid package name", packageName);
            return;
        }
        runAdbShell("am", "force-stop", packageName);
    }

    public void openURL(String url)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        runAdbShell("am", "start", "-a", "android.intent.action.VIEW", "-d", url);
    }

    public void startIntent(String intentString)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        if (!INTENT_STRING_PATTERN.matcher(intentString).matches()) {
            LOGGER.warn("{} is not a valid intent string", intentString);
            return;
        }
        runAdbShell("am", "start", "\"" + intentString + "\"");
    }

    public String getCurrentPackage() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        if (channelFallbackMap.get(CURRENT_PACKAGE_CHANNEL) == FallbackModes.DUMPSYS_ACTIVITY_RECENTS) {
            return getCurrentPackageWithDumpsysActivityRecents();
        }
        String result = runAdbShell("dumpsys window windows", "|", "grep 'mFocusedApp'", "|", "cut -d '/' -f1", "|",
                "sed 's/.* //g'");
        if (!result.isEmpty()) {
            return result;
        } else {
            LOGGER.debug("set fallback {} for {}", FallbackModes.DUMPSYS_ACTIVITY_RECENTS, CURRENT_PACKAGE_CHANNEL);
            channelFallbackMap.put(CURRENT_PACKAGE_CHANNEL, FallbackModes.DUMPSYS_ACTIVITY_RECENTS);
            return getCurrentPackageWithDumpsysActivityRecents();
        }
    }

    public String getCurrentPackageWithDumpsysActivityRecents() throws AndroidDebugBridgeDeviceException,
            InterruptedException, AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        // try another method if we failed, see https://stackoverflow.com/a/28573364
        String result = runAdbShell("dumpsys activity recents", "|", "grep 'Recent #0'", "|", "cut -d= -f2", "|",
                "sed 's/ .*//'", "|", "cut -d '/' -f1");
        if (!result.isEmpty()) {
            return result;
        }
        LOGGER.debug("removed fallback {}", HDMI_STATE_CHANNEL);
        channelFallbackMap.remove(HDMI_STATE_CHANNEL);
        throw new AndroidDebugBridgeDeviceReadException(CURRENT_PACKAGE_CHANNEL, result);
    }

    public void rebootDevice()
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        try {
            runAdbShell("reboot", "&", "sleep", "0.1", "&&", "exit");
        } finally {
            disconnect();
        }
    }

    public void powerOffDevice()
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        try {
            runAdbShell("reboot", "-p", "&", "sleep", "0.1", "&&", "exit");
        } finally {
            disconnect();
        }
    }

    public boolean isAwake() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        String result = runAdbShell("dumpsys", "activity", "|", "grep", "mWakefulness");
        if (result.contains("mWakefulness=")) {
            return result.contains("mWakefulness=Awake");
        }
        throw new AndroidDebugBridgeDeviceReadException(AWAKE_STATE_CHANNEL, result);
    }

    public boolean isScreenOn() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        String result = runAdbShell("dumpsys", "power", "|", "grep", "'Display Power'");
        String[] splitResult = result.split("=");
        if (splitResult.length >= 2) {
            return "ON".equals(splitResult[1]);
        }
        throw new AndroidDebugBridgeDeviceReadException(SCREEN_STATE_CHANNEL, result);
    }

    public Optional<Boolean> isHDMIOn() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        if (channelFallbackMap.get(HDMI_STATE_CHANNEL) == FallbackModes.LOGCAT) {
            return isHDMIOnWithLogcat();
        }
        String result = runAdbShell("cat", "/sys/devices/virtual/switch/hdmi/state");
        if ("0".equals(result) || "1".equals(result)) {
            return Optional.of("1".equals(result));
        } else {
            LOGGER.debug("set fallback {} for {}", FallbackModes.LOGCAT, HDMI_STATE_CHANNEL);
            channelFallbackMap.put(HDMI_STATE_CHANNEL, FallbackModes.LOGCAT);
            return isHDMIOnWithLogcat();
        }
    }

    private Optional<Boolean> isHDMIOnWithLogcat() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        String result = runAdbShell("logcat", "-d", "|", "grep", "hdmi", "|", "grep", "SWITCH_STATE=", "|", "tail",
                "-1");
        if (result.contains("SWITCH_STATE=")) {
            return Optional.of(result.contains("SWITCH_STATE=1"));
        } else if (result.isEmpty()) {
            // IF THE DEVICE DO NOT SUPPORT THIS VALUE IN LOGCAT THE USER WILL NEVER KNOW THE CHANNEL WON'T WORK
            // FIND A BETTER SOLUTION
            return Optional.empty();
        }
        LOGGER.debug("removed fallback {}", HDMI_STATE_CHANNEL);
        channelFallbackMap.remove(HDMI_STATE_CHANNEL);
        throw new AndroidDebugBridgeDeviceReadException(HDMI_STATE_CHANNEL, result);
    }

    public boolean isPlayingMedia(String currentApp) throws AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, InterruptedException, TimeoutException, ExecutionException {
        String result = runAdbShell("dumpsys", "media_session", "|", "grep", "-A", "100", "'Sessions Stack'", "|",
                "grep", "-A", "50", currentApp);
        String[] mediaSessions = result.split("\n\n");
        if (mediaSessions.length == 0) {
            // no media session found for current app
            return false;
        } else if (mediaSessions[0].contains("PlaybackState {state=3")) {
            boolean isPlaying = mediaSessions[0].contains("PlaybackState {state=3");
            LOGGER.debug("device media state playing {}", isPlaying);
            return isPlaying;
        }
        throw new AndroidDebugBridgeDeviceReadException(MEDIA_CONTROL_CHANNEL, result);
    }

    public boolean isPlayingAudio() throws AndroidDebugBridgeDeviceException, AndroidDebugBridgeDeviceReadException,
            InterruptedException, TimeoutException, ExecutionException {
        String result = runAdbShell("dumpsys", "audio", "|", "grep", "ID:");
        if (result.contains("state:")) {
            return result.contains("state:started");
        }
        throw new AndroidDebugBridgeDeviceReadException(MEDIA_CONTROL_CHANNEL, result);
    }

    public VolumeInfo getMediaVolume() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        return getVolume(ANDROID_MEDIA_STREAM);
    }

    public void setMediaVolume(int volume)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        setVolume(ANDROID_MEDIA_STREAM, volume);
    }

    public int getPowerWakeLock() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        String result = runAdbShell("dumpsys", "power", "|", "grep", "Locks", "|", "grep", "'size='");
        String[] splitResult = result.split("=");
        if (splitResult.length >= 2) {
            try {
                return Integer.parseInt(splitResult[1]);
            } catch (NumberFormatException e) {
                LOGGER.debug("Unable to parse device wake-lock: {}", e.getMessage());
            }
        }
        throw new AndroidDebugBridgeDeviceReadException(WAKE_LOCK_CHANNEL, result);
    }

    private void setVolume(int stream, int volume)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        runAdbShell("media", "volume", "--show", "--stream", String.valueOf(stream), "--set", String.valueOf(volume));
    }

    public String getModel() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        return getDeviceProp("ro.product.model");
    }

    public String getAndroidVersion() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        return getDeviceProp("ro.build.version.release");
    }

    public String getBrand() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        return getDeviceProp("ro.product.brand");
    }

    public String getSerialNo() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        return getDeviceProp("ro.serialno");
    }

    private String getDeviceProp(String name) throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        String result = runAdbShell("getprop", name, "&&", "sleep", "0.3").replace("\n", "").replace("\r", "");
        if (result.length() == 0) {
            throw new AndroidDebugBridgeDeviceReadException("Device does not support properties");
        }
        return result;
    }

    private VolumeInfo getVolume(int stream) throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        String result = runAdbShell("media", "volume", "--show", "--stream", String.valueOf(stream), "--get", "|",
                "grep", "volume");
        Matcher matcher = VOLUME_PATTERN.matcher(result);
        if (!matcher.find()) {
            throw new AndroidDebugBridgeDeviceReadException(MEDIA_VOLUME_CHANNEL, result);
        }
        VolumeInfo volumeInfo = new VolumeInfo(Integer.parseInt(matcher.group("current")),
                Integer.parseInt(matcher.group("min")), Integer.parseInt(matcher.group("max")));
        LOGGER.debug("Device {}:{} VolumeInfo: current {}, min {}, max {}", this.ip, this.port, volumeInfo.current,
                volumeInfo.min, volumeInfo.max);
        return volumeInfo;
    }

    public boolean isConnected() {
        Socket currentSocket = socket;
        return currentSocket != null && currentSocket.isConnected();
    }

    public void connect() throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException {
        this.disconnect();
        AdbConnection adbConnection;
        Socket sock;
        AdbCrypto crypto = adbCrypto;
        if (crypto == null) {
            throw new AndroidDebugBridgeDeviceException("Device not connected");
        }
        try {
            sock = new Socket();
            socket = sock;
            sock.connect(new InetSocketAddress(ip, port), (int) TimeUnit.SECONDS.toMillis(15));
        } catch (IOException e) {
            LOGGER.debug("Error connecting to {}: [{}] {}", ip, e.getClass().getName(), e.getMessage());
            if ("Socket closed".equals(e.getMessage())) {
                // Connection aborted by us
                throw new InterruptedException();
            } else if ("No route to host (Host unreachable)".equals(e.getMessage())
                    || "Connection failed".equals(e.getMessage())) {
                throw new TimeoutException(e.getMessage());
            }
            throw new AndroidDebugBridgeDeviceException("Unable to open socket " + ip + ":" + port);
        }
        try {
            adbConnection = AdbConnection.create(sock, crypto);
            connection = adbConnection;
            adbConnection.connect(15, TimeUnit.SECONDS, false);
        } catch (IOException e) {
            LOGGER.debug("Error connecting to {}: [{}] {}", ip, e.getClass().getName(), e.getMessage());
            if ("No route to host (Host unreachable)".equals(e.getMessage())
                    || "Connection failed".equals(e.getMessage())) {
                throw new TimeoutException(e.getMessage());
            }
            throw new AndroidDebugBridgeDeviceException("Unable to open adb connection " + ip + ":" + port);
        }
    }

    private String runAdbShell(String... args)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        AdbConnection adb = connection;
        if (adb == null) {
            throw new AndroidDebugBridgeDeviceException("Device not connected");
        }
        commandLock.lock();
        try {
            stopCommandFuture(); // make sure there is not future
            Future<String> commandFuture = scheduler.submit(() -> {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                String cmd = String.join(" ", args);
                LOGGER.debug("{} - shell:{}", ip, cmd);
                try {
                    AdbStream stream = adb.open("shell:" + cmd);
                    do {
                        byteArrayOutputStream.writeBytes(stream.read());
                    } while (!stream.isClosed());
                } catch (IOException | IllegalStateException e) {
                    String message = e.getMessage();
                    if (message != null && !"Stream closed".equals(message)
                            && !"connect() must be called first".equals(message)) {
                        throw e;
                    }
                }
                return byteArrayOutputStream.toString(StandardCharsets.US_ASCII);
            });
            this.commandFuture = commandFuture;
            return commandFuture.get(timeoutSec, TimeUnit.SECONDS).trim();
        } finally {
            stopCommandFuture();
            commandLock.unlock();
        }
    }

    private static AdbBase64 getBase64Impl() {
        Charset asciiCharset = Charset.forName("ASCII");
        return bytes -> new String(Base64.getEncoder().encode(bytes), asciiCharset);
    }

    private static AdbCrypto loadKeyPair(String pubKeyFile, String privKeyFile)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        File pub = new File(pubKeyFile);
        File priv = new File(privKeyFile);
        AdbCrypto c = null;
        // load key pair
        if (pub.exists() && priv.exists()) {
            try {
                c = AdbCrypto.loadAdbKeyPair(getBase64Impl(), priv, pub);
            } catch (IOException ignored) {
                // Keys don't exits
            }
        }
        if (c == null) {
            // generate key pair
            c = AdbCrypto.generateAdbKeyPair(getBase64Impl());
            c.saveAdbKeyPair(priv, pub);
        }
        return c;
    }

    private void stopCommandFuture() {
        Future<?> commandFuture = this.commandFuture;
        if (commandFuture != null) {
            commandFuture.cancel(true);
            this.commandFuture = null;
        }
    }

    public void disconnect() {
        stopCommandFuture();
        AdbConnection adb = connection;
        Socket sock = socket;
        if (adb != null) {
            try {
                adb.close();
            } catch (IOException ignored) {
            }
            connection = null;
        }
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
    }

    public static class VolumeInfo {
        public int current;
        public int min;
        public int max;

        VolumeInfo(int current, int min, int max) {
            this.current = current;
            this.min = min;
            this.max = max;
        }
    }

    private enum FallbackModes {
        MONKEY,
        MONKEY_LEANBACK_LAUNCHER,
        DUMPSYS_ACTIVITY_RECENTS,
        LOGCAT,
    }
}
