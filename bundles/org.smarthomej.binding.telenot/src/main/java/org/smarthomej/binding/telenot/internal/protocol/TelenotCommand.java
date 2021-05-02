/**
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
package org.smarthomej.binding.telenot.internal.protocol;

import java.util.Calendar;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.binding.telenot.internal.TelenotCommandException;

/**
 * The {@link TelenotCommand} class represents an Telenot command, and contains the static methods and definitions
 * used to construct one. Not all supported Telenot commands are necessarily used by the current binding.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public final class TelenotCommand {

    private static final String COMMAND_SEND_NORM = "6802026840024216";
    private static final String COMMAND_CONF_ACK = "6802026800020216";
    private static final String COMMAND_USED_STATE = "680909687302051000000071241f16";
    private static final String COMMAND_SB_STATE_ON = "680909687301050200";
    private static final String COMMAND_SB_STATE_OFF = "680909687300050200";

    public final String command;
    public final String logMsg;

    public TelenotCommand(String command, String logMsg) {
        this.command = command;
        this.logMsg = logMsg;
    }

    @Override
    public String toString() {
        return command;
    }

    public static TelenotCommand setDateTime() {
        Calendar now = Calendar.getInstance();

        int year = now.get(Calendar.YEAR) - 2000;
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DATE);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case 1:
                dayOfWeek = 6;
                break;
            case 2:
                dayOfWeek = 0;
                break;
            case 3:
                dayOfWeek = 1;
                break;
            case 4:
                dayOfWeek = 2;
                break;
            case 5:
                dayOfWeek = 3;
                break;
            case 6:
                dayOfWeek = 4;
                break;
            case 7:
                dayOfWeek = 5;
                break;
        }

        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);

        String strYear = Integer.toHexString(year);
        strYear = (strYear.length() == 1 ? "0" + strYear : strYear);
        String strMonth = Integer.toHexString(month);
        strMonth = (strMonth.length() == 1 ? "0" + strMonth : strMonth);
        String strDay = Integer.toHexString(day);
        strDay = (strDay.length() == 1 ? "0" + strDay : strDay);
        String strDayOfWeek = Integer.toHexString(dayOfWeek);
        strDayOfWeek = (strDayOfWeek.length() == 1 ? "0" + strDayOfWeek : strDayOfWeek);
        String strHour = Integer.toHexString(hour);
        strHour = (strHour.length() == 1 ? "0" + strHour : strHour);
        String strMinute = Integer.toHexString(minute);
        strMinute = (strMinute.length() == 1 ? "0" + strMinute : strMinute);
        String strSecond = Integer.toHexString(second);
        strSecond = (strSecond.length() == 1 ? "0" + strSecond : strSecond);

        String msg = "680B0B6873010750" + strYear + strDayOfWeek + strMonth + strDay + strHour + strMinute + strSecond;
        msg = msg + checksum(msg) + "16";

        return new TelenotCommand(msg, "set date/time msg: " + msg);
    }

    /**
     * Construct an Telenot command to acknowledge that a received CRC message was valid.
     *
     * @return TelenotCommand object containing the constructed command
     */
    public static TelenotCommand confirmACK() {
        return new TelenotCommand(COMMAND_CONF_ACK, "Confirm_ACK");
    }

    /**
     * Construct an Telenot command to acknowledge that a received CRC message was valid.
     *
     * @return TelenotCommand object containing the constructed command
     */
    public static TelenotCommand sendNorm() {
        return new TelenotCommand(COMMAND_SEND_NORM, "SEND_NORM");
    }

    /**
     * Construct an Telenot command to return used cntacts.
     *
     * @return TelenotCommand object containing the constructed command
     */
    public static TelenotCommand sendUsedState() {
        return new TelenotCommand(COMMAND_USED_STATE, "Used Contacts");
    }

    /**
     * Construct an Telenot command to disarm area.
     *
     * @param address The SB area number (1-8) for the command.
     * @return TelenotCommand object containing the constructed command
     * @throws TelenotCommandException
     */
    public static TelenotCommand disarmArea(int address) throws TelenotCommandException {
        if (address < 1 || address > 8) {
            throw new TelenotCommandException("Invalid parameter(s)");
        }
        String hex = Integer.toHexString(1320 + (address * 8));
        String msg = COMMAND_SB_STATE_ON + "0" + hex + "02E1";
        msg = msg + checksum(msg) + "16";
        return new TelenotCommand(msg, "DISARM security area msg: " + msg);
    }

    /**
     * Construct an Telenot command to internal arm area.
     *
     * @param address The SB area number (1-8) for the command.
     * @return TelenotCommand object containing the constructed command
     * @throws TelenotCommandException
     */
    public static TelenotCommand intArmArea(int address) throws TelenotCommandException {
        if (address < 1 || address > 8) {
            throw new TelenotCommandException("Invalid parameter(s)");
        }
        String hex = Integer.toHexString(1321 + (address * 8));
        String msg = COMMAND_SB_STATE_ON + "0" + hex + "0262";
        msg = msg + checksum(msg) + "16";
        return new TelenotCommand(msg, "INT_ARM security area msg: " + msg);
    }

    /**
     * Construct an Telenot command to external arm area.
     *
     * @param address The SB area number (1-8) for the command.
     * @return TelenotCommand object containing the constructed command
     * @throws TelenotCommandException
     */
    public static TelenotCommand extArmArea(int address) throws TelenotCommandException {
        if (address < 1 || address > 8) {
            throw new TelenotCommandException("Invalid parameter(s)");
        }
        String hex = Integer.toHexString(1322 + (address * 8));
        String msg = COMMAND_SB_STATE_ON + "0" + hex + "0261";
        msg = msg + checksum(msg) + "16";
        return new TelenotCommand(msg, "EXT_ARM security area msg: " + msg);
    }

    /**
     * Construct an Telenot command to reset alarm area.
     *
     * @param address The SB area number (1-8) for the command.
     * @param state The new state (0 or 1) for the area.
     * @return TelenotCommand object containing the constructed command
     * @throws TelenotCommandException
     */
    public static TelenotCommand resetAlarm(int address) throws TelenotCommandException {
        if (address < 1 || address > 8) {
            throw new TelenotCommandException("Invalid parameter(s)");
        }
        String hex = Integer.toHexString(1323 + (address * 8));
        String msg = COMMAND_SB_STATE_ON + "0" + hex + "0252";
        msg = msg + checksum(msg) + "16";
        return new TelenotCommand(msg, "RESET_ALARM security area msg: " + msg);
    }

    /**
     * Construct an Telenot command to enable/disable reporting area.
     *
     * @param address The SB area number (1-8) for the command.
     * @param state The new state (0 or 1) for the area.
     * @return TelenotCommand object containing the constructed command
     * @throws TelenotCommandException
     */
    public static TelenotCommand disableReportingPoint(int address, int state) throws TelenotCommandException {
        if (address < 1 || address > 128 || state < 0 || state > 1) {
            throw new TelenotCommandException("Invalid parameter(s)");
        }
        String hex = Integer.toHexString(1519 + address);
        String msg = "";
        String logString = "";
        if (state == 1) {
            msg = COMMAND_SB_STATE_OFF + "0" + hex + "0251";
            msg = msg + checksum(msg) + "16";
            logString = "DISABLE_REPORTING_POINT msg: " + msg;
        } else if (state == 0) {
            msg = COMMAND_SB_STATE_ON + "0" + hex + "02D1";
            msg = msg + checksum(msg) + "16";
            logString = "ENABLE_REPORTING_POINT msg: " + msg;
        }
        return new TelenotCommand(msg, logString);
    }

    /**
     * Construct an Telenot command to enable/disable reporting area.
     *
     * @param address The SB area number (1-8) for the command.
     * @param state The new state (0 or 1) for the area.
     * @return TelenotCommand object containing the constructed command
     * @throws TelenotCommandException
     */
    public static TelenotCommand disableHexReportingPoint(String address, int state) throws TelenotCommandException {
        String msg = "";
        String logString = "";
        if (state == 1) {
            msg = COMMAND_SB_STATE_OFF + address + "0251";
            msg = msg + checksum(msg) + "16";
            logString = "DISABLE_REPORTING_POINT msg: " + msg;
        } else if (state == 0) {
            msg = COMMAND_SB_STATE_ON + address + "02D1";
            msg = msg + checksum(msg) + "16";
            logString = "ENABLE_REPORTING_POINT msg: " + msg;
        }
        return new TelenotCommand(msg, logString);
    }

    /**
     * Construct an Telenot command to get contact info.
     *
     * @param address The hex string (address) for the contact.
     * @return TelenotCommand object containing the constructed command
     */
    public static TelenotCommand getContactInfo(String address) {
        String hex = address.substring(2, 6);
        String msg = "680909687302051000" + hex + "730C";
        msg = msg + checksum(msg) + "16";
        return new TelenotCommand(msg, "GET_CONTACT_INFO msg: " + msg);
    }

    /**
     * Converts String into checksum
     */
    public static String checksum(String s) {
        String sum = "";
        int x = 0;
        int dataLength = Integer.parseInt(s.substring(2, 4), 16);
        int a = 8;

        for (int i = 0; i < dataLength; i++) {
            x = x + Integer.parseInt(s.substring(a, a + 2), 16);
            a = a + 2;
        }
        sum = Integer.toHexString(x);
        sum = sum.substring(1, 3);
        return sum;
    }
}
