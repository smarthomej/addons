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

import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.AWAKE_STATE_CHANNEL;
import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.CURRENT_PACKAGE_CHANNEL;
import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.HDMI_STATE_CHANNEL;
import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.MEDIA_CONTROL_CHANNEL;
import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.MEDIA_VOLUME_CHANNEL;
import static org.smarthomej.binding.androiddebugbridge.internal.AndroidDebugBridgeBindingConstants.SCREEN_STATE_CHANNEL;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.*;
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
    private final Logger logger = LoggerFactory.getLogger(AndroidDebugBridgeDevice.class);
    private static final Pattern VOLUME_PATTERN = Pattern
            .compile("volume is (?<current>\\d.*) in range \\[(?<min>\\d.*)\\.\\.(?<max>\\d.*)]");

    private static @Nullable AdbCrypto adbCrypto;

    static {
        var logger = LoggerFactory.getLogger(AndroidDebugBridgeDevice.class);
        try {
            File directory = new File(ADB_FOLDER);
            if (!directory.exists()) {
                directory.mkdir();
            }
            adbCrypto = loadKeyPair(ADB_FOLDER + File.separator + "adb_pub.key",
                    ADB_FOLDER + File.separator + "adb.key");
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
            logger.warn("Unable to setup adb keys: {}", e.getMessage());
        }
    }

    private final ScheduledExecutorService scheduler;

    private String ip = "127.0.0.1";
    private int port = 5555;
    private int timeoutSec = 5;
    private @Nullable Socket socket;
    private @Nullable AdbConnection connection;
    private @Nullable Future<String> commandFuture;

    private Lock lock = new ReentrantLock();

    AndroidDebugBridgeDevice(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public void configure(String ip, int port, int timeout) {
        this.ip = ip;
        this.port = port;
        this.timeoutSec = timeout;
    }

    public void sendMouseTap(String mouseTap)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        String x = mouseTap.split(",")[0];
        String y = mouseTap.split(",")[1];
        runAdbShell("input", "mouse", "tap", x, y);
    }

    public void sendKeyEvent(String eventCode)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        runAdbShell("input", "keyevent", eventCode);
    }

    public void sendText(String text)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        runAdbShell("input", "text", URLEncoder.encode(text, StandardCharsets.UTF_8));
    }

    public void startPackage(String packageName)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        runAdbShell("am", "start", "-n", packageName);
    }

    public void stopPackage(String packageName)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        runAdbShell("am", "force-stop", packageName);
    }

    public void openURL(String url)
            throws AndroidDebugBridgeDeviceException, InterruptedException, TimeoutException, ExecutionException {
        runAdbShell("am", "start", "-a", "android.intent.action.VIEW", "-d", url);
    }

    public String getCurrentPackage() throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        var out = runAdbShell("dumpsys", "window", "windows", "|", "grep", "mFocusedApp");
        var targetLine = Arrays.stream(out.split("\n")).findFirst().orElse("");
        var lineParts = targetLine.split(" ");
        if (lineParts.length >= 2) {
            var packageActivityName = lineParts[lineParts.length - 2];
            if (packageActivityName.contains("/"))
                return packageActivityName.split("/")[0];
        }
        throw new AndroidDebugBridgeDeviceReadException(
                "Device does not support " + CURRENT_PACKAGE_CHANNEL + ": " + out);
    }

    public @Nullable Boolean isAwake() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        var out = runAdbShell("dumpsys", "activity", "|", "grep", "mWakefulness");
        if (out.contains("mWakefulness=")) {
            return out.contains("mWakefulness=Awake");
        } else if (out.isEmpty()) {
            return null;
        }
        throw new AndroidDebugBridgeDeviceReadException("Device does not support " + AWAKE_STATE_CHANNEL + ": " + out);
    }

    public @Nullable Boolean isScreenOn() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        var out = runAdbShell("dumpsys", "power", "|", "grep", "'Display Power'");
        if (out.contains("=")) {
            var state = out.split("=")[1];
            return state.equals("ON");
        } else if (out.isEmpty()) {
            return null;
        }
        throw new AndroidDebugBridgeDeviceReadException("Device does not support " + SCREEN_STATE_CHANNEL + ": " + out);
    }

    public @Nullable Boolean isHDMIOn() throws InterruptedException, AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        var out = runAdbShell("cat", "/sys/devices/virtual/switch/hdmi/state");
        if (out.equals("0") || out.equals("1")) {
            return out.equals("1");
        } else if (out.isEmpty()) {
            return null;
        } else {
            var fallback = runAdbShell("logcat", "-d", "|", "grep", "hdmi", "|", "grep", "SWITCH_STATE=", "|", "tail",
                    "-1");
            if (fallback.contains("SWITCH_STATE=")) {
                return fallback.contains("SWITCH_STATE=1");
            } else if (fallback.isEmpty()) {
                return null;
            }
            throw new AndroidDebugBridgeDeviceReadException(
                    "Device does not support " + HDMI_STATE_CHANNEL + ": " + out + " | " + fallback);
        }
    }

    public @Nullable Boolean isPlayingMedia(String currentApp) throws AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, InterruptedException, TimeoutException, ExecutionException {
        var out = runAdbShell("dumpsys", "media_session", "|", "grep", "-A", "100", "'Sessions Stack'", "|", "grep",
                "-A", "50", currentApp);
        String[] mediaSessions = out.split("\n\n");
        if (mediaSessions.length == 0) {
            // no media session found for current app
            return false;
        } else if (mediaSessions[0].contains("PlaybackState {state=3")) {
            boolean isPlaying = mediaSessions[0].contains("PlaybackState {state=3");
            logger.debug("device media state playing {}", isPlaying);
            return isPlaying;
        } else if (out.isEmpty()) {
            return null;
        }
        throw new AndroidDebugBridgeDeviceReadException(
                "Device does not support " + MEDIA_CONTROL_CHANNEL + ": " + out);
    }

    public @Nullable Boolean isPlayingAudio() throws AndroidDebugBridgeDeviceException,
            AndroidDebugBridgeDeviceReadException, InterruptedException, TimeoutException, ExecutionException {
        var out = runAdbShell("dumpsys", "audio", "|", "grep", "ID:");
        if (out.contains("state:")) {
            return out.contains("state:started");
        } else if (out.isEmpty()) {
            return null;
        }
        throw new AndroidDebugBridgeDeviceReadException(
                "Device does not support " + MEDIA_CONTROL_CHANNEL + ": " + out);
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
        var out = runAdbShell("dumpsys", "power", "|", "grep", "Locks", "|", "grep", "'size='");
        if (out.contains("=")) {
            try {
                return Integer.parseInt(out.split("=")[1]);
            } catch (NumberFormatException e) {
                logger.debug("Unable to parse device wake lock: {}", e.getMessage());
            }
        }
        throw new AndroidDebugBridgeDeviceReadException("Device does not support wake lock");
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
        var out = runAdbShell("getprop", name, "&&", "sleep", "0.3").replace("\n", "").replace("\r", "");
        if (out.length() == 0) {
            throw new AndroidDebugBridgeDeviceReadException("Device does not support properties");
        }
        return out;
    }

    private VolumeInfo getVolume(int stream) throws AndroidDebugBridgeDeviceException, InterruptedException,
            AndroidDebugBridgeDeviceReadException, TimeoutException, ExecutionException {
        var out = runAdbShell("media", "volume", "--show", "--stream", String.valueOf(stream), "--get", "|", "grep",
                "volume");
        Matcher matcher = VOLUME_PATTERN.matcher(out);
        if (!matcher.find())
            throw new AndroidDebugBridgeDeviceReadException(
                    "Device does not support " + MEDIA_VOLUME_CHANNEL + ": " + out);
        var volumeInfo = new VolumeInfo(Integer.parseInt(matcher.group("current")),
                Integer.parseInt(matcher.group("min")), Integer.parseInt(matcher.group("max")));
        logger.debug("Device {}:{} VolumeInfo: current {}, min {}, max {}", this.ip, this.port, volumeInfo.current,
                volumeInfo.min, volumeInfo.max);
        return volumeInfo;
    }

    public boolean isConnected() {
        var currentSocket = socket;
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
            logger.debug("Error connecting to {}: [{}] {}", ip, e.getClass().getName(), e.getMessage());
            if (e.getMessage().equals("Socket closed")) {
                // Connection aborted by us
                throw new InterruptedException();
            } else if (e.getMessage().equals("No route to host (Host unreachable)")) {
                throw new TimeoutException();
            }
            throw new AndroidDebugBridgeDeviceException("Unable to open socket " + ip + ":" + port);
        }
        try {
            adbConnection = AdbConnection.create(sock, crypto);
            connection = adbConnection;
            adbConnection.connect(15, TimeUnit.SECONDS, false);
        } catch (IOException e) {
            logger.debug("Error connecting to {}: {}", ip, e.getMessage());
            throw new AndroidDebugBridgeDeviceException("Unable to open adb connection " + ip + ":" + port);
        }
    }

    private String runAdbShell(String... args)
            throws InterruptedException, AndroidDebugBridgeDeviceException, TimeoutException, ExecutionException {
        var adb = connection;
        if (adb == null) {
            throw new AndroidDebugBridgeDeviceException("Device not connected");
        }
        lock.lock();
        try {
            commandFuture = scheduler.submit(() -> {
                var byteArrayOutputStream = new ByteArrayOutputStream();
                String cmd = String.join(" ", args);
                logger.debug("{} - shell:{}", ip, cmd);
                try {
                    AdbStream stream = adb.open("shell:" + cmd);
                    do {
                        byteArrayOutputStream.writeBytes(stream.read());
                    } while (!stream.isClosed());
                } catch (IOException e) {
                    String message = e.getMessage();
                    if (message != null && !message.equals("Stream closed")) {
                        throw e;
                    }
                }
                return byteArrayOutputStream.toString(StandardCharsets.US_ASCII);
            });
            return commandFuture.get(timeoutSec, TimeUnit.SECONDS).trim();
        } finally {
            if (commandFuture != null) {
                commandFuture.cancel(true);
            }
            lock.unlock();
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

    public void disconnect() {
        var commandFuture = this.commandFuture;
        if (commandFuture != null) {
            commandFuture.cancel(true);
            this.commandFuture = null;
        }
        var adb = connection;
        var sock = socket;
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
}
