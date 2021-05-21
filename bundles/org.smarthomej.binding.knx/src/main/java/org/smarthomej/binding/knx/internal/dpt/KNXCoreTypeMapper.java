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
package org.smarthomej.binding.knx.internal.dpt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteSigned;
import tuwien.auto.calimero.dptxlator.DPTXlator4ByteUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlator64BitSigned;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitSigned;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.DPTXlatorDate;
import tuwien.auto.calimero.dptxlator.DPTXlatorDateTime;
import tuwien.auto.calimero.dptxlator.DPTXlatorRGB;
import tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl;
import tuwien.auto.calimero.dptxlator.DPTXlatorSceneNumber;
import tuwien.auto.calimero.dptxlator.DPTXlatorString;
import tuwien.auto.calimero.dptxlator.DPTXlatorTime;
import tuwien.auto.calimero.dptxlator.DPTXlatorUtf8;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

/**
 * This class provides type mapping between all openHAB core types and KNX data point types.
 *
 * Each 'MainType' delivered from calimero, has a default mapping
 * for all it's children to a openHAB Typeclass.
 * All these 'MainType' mapping's are put into 'dptMainTypeMap'.
 *
 * Default 'MainType' mapping's we can override by a specific mapping.
 * All specific mapping's are put into 'dptTypeMap'.
 *
 * If for a 'MainType' there is currently no specific mapping registered,
 * you can find a commented example line, with it's correct 'DPTXlator' class.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Volker Daube - improvements
 * @author Jan N. Klug - improvements
 * @author Helmut Lehmeyer - Java8, generic DPT Mapper
 * @author Jan N. Klug - refactor to static class
 */
@NonNullByDefault
public class KNXCoreTypeMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KNXCoreTypeMapper.class);

    private static final String TIME_DAY_FORMAT = "EEE, HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Pattern DPT_REGEX_PATTERN = Pattern
            .compile("^(?<main>[1-9][0-9]{0,2})(?:\\.(?<sub>\\d{3,4}))?$");

    /** stores the openHAB type class for all (supported) KNX datapoint types */
    private static final Map<String, Set<Class<? extends Type>>> DPT_TYPE_MAP = Map.ofEntries(
            Map.entry(DPTXlatorBoolean.DPT_UPDOWN.getID(), Set.of(UpDownType.class)), //
            Map.entry(DPTXlatorBoolean.DPT_OPENCLOSE.getID(), Set.of(OpenClosedType.class)), //
            Map.entry(DPTXlatorBoolean.DPT_START.getID(), Set.of(StopMoveType.class)), //
            Map.entry(DPTXlatorBoolean.DPT_WINDOW_DOOR.getID(), Set.of(OpenClosedType.class)), //
            Map.entry(DPTXlatorBoolean.DPT_SCENE_AB.getID(), Set.of(DecimalType.class)), //
            Map.entry(DPTXlator3BitControlled.DPT_CONTROL_BLINDS.getID(), Set.of(UpDownType.class)), //
            Map.entry(DPTXlator8BitUnsigned.DPT_SCALING.getID(), Set.of(PercentType.class)), //
            Map.entry(DPTXlator8BitUnsigned.DPT_PERCENT_U8.getID(), Set.of(PercentType.class)), //
            Map.entry(DPTXlator8BitSigned.DPT_PERCENT_V8.getID(), Set.of(PercentType.class)), //
            Map.entry(DPTXlator8BitSigned.DPT_STATUS_MODE3.getID(), Set.of(StringType.class)), //
            Map.entry(DPTXlator2ByteFloat.DPT_HUMIDITY.getID(), Set.of(PercentType.class)), //
            Map.entry(DPTXlatorString.DPT_STRING_8859_1.getID(), Set.of(StringType.class)), //
            Map.entry(DPTXlatorString.DPT_STRING_ASCII.getID(), Set.of(StringType.class)));

    /** stores the default KNX DPT to use for each openHAB type */
    private static final Map<Class<? extends Type>, String> DEFAULT_DPT_MAP = Map.ofEntries(
            Map.entry(OnOffType.class, DPTXlatorBoolean.DPT_SWITCH.getID()), //
            Map.entry(UpDownType.class, DPTXlatorBoolean.DPT_UPDOWN.getID()), //
            Map.entry(StopMoveType.class, DPTXlatorBoolean.DPT_START.getID()), //
            Map.entry(OpenClosedType.class, DPTXlatorBoolean.DPT_WINDOW_DOOR.getID()), //
            Map.entry(IncreaseDecreaseType.class, DPTXlator3BitControlled.DPT_CONTROL_DIMMING.getID()), //
            Map.entry(PercentType.class, DPTXlator8BitUnsigned.DPT_SCALING.getID()), //
            Map.entry(DecimalType.class, DPTXlator2ByteFloat.DPT_TEMPERATURE.getID()), //
            Map.entry(QuantityType.class, DPTXlator2ByteFloat.DPT_TEMPERATURE.getID()), //
            Map.entry(DateTimeType.class, DPTXlatorTime.DPT_TIMEOFDAY.getID()), //
            Map.entry(StringType.class, DPTXlatorString.DPT_STRING_8859_1.getID()), //
            Map.entry(HSBType.class, DPTXlatorRGB.DPT_RGB.getID()));

    @SuppressWarnings("unused")
    private static final List<Class<? extends DPTXlator>> XLATORS = List.of(DPTXlator1BitControlled.class,
            DPTXlator2ByteFloat.class, DPTXlator2ByteUnsigned.class, DPTXlator3BitControlled.class,
            DPTXlator4ByteFloat.class, DPTXlator4ByteSigned.class, DPTXlator4ByteUnsigned.class,
            DPTXlator64BitSigned.class, DPTXlator8BitSigned.class, DPTXlator8BitUnsigned.class, DPTXlatorBoolean.class,
            DPTXlatorDate.class, DPTXlatorDateTime.class, DPTXlatorRGB.class, DPTXlatorSceneControl.class,
            DPTXlatorSceneNumber.class, DPTXlatorString.class, DPTXlatorTime.class, DPTXlatorUtf8.class);
    /**
     * stores the openHAB type class for (supported) KNX datapoint types in a generic way.
     * dptTypeMap stores more specific type class and exceptions.
     */
    private static final Map<String, Set<Class<? extends Type>>> DPT_MAIN_TYPE_MAP = Map.ofEntries( //
            Map.entry("1", Set.of(OnOffType.class)), //
            Map.entry("2", Set.of(DecimalType.class)), //
            Map.entry("3", Set.of(IncreaseDecreaseType.class)), //
            Map.entry("4", Set.of(StringType.class)), //
            Map.entry("5", Set.of(DecimalType.class)), //
            Map.entry("6", Set.of(DecimalType.class)), //
            Map.entry("7", Set.of(DecimalType.class, QuantityType.class)), //
            Map.entry("8", Set.of(DecimalType.class, QuantityType.class)), //
            Map.entry("9", Set.of(DecimalType.class, QuantityType.class)), //
            Map.entry("10", Set.of(DateTimeType.class)), //
            Map.entry("11", Set.of(DateTimeType.class)), //
            Map.entry("12", Set.of(DecimalType.class)), //
            Map.entry("13", Set.of(DecimalType.class, QuantityType.class)), //
            Map.entry("14", Set.of(DecimalType.class, QuantityType.class)), //
            Map.entry("16", Set.of(StringType.class)), //
            Map.entry("17", Set.of(DecimalType.class)), //
            Map.entry("18", Set.of(DecimalType.class)), //
            Map.entry("19", Set.of(DateTimeType.class)), //
            Map.entry("20", Set.of(StringType.class)), //
            Map.entry("21", Set.of(StringType.class)), //
            Map.entry("22", Set.of(StringType.class)), //
            Map.entry("28", Set.of(StringType.class)), //
            Map.entry("29", Set.of(DecimalType.class, QuantityType.class)), //
            Map.entry("229", Set.of(DecimalType.class)), //
            Map.entry("232", Set.of(HSBType.class)));

    private KNXCoreTypeMapper() {
        // prevent instantiation
    }

    public static @Nullable String toDPTValue(Type type, String dptID) {
        DPT dpt;

        Matcher m = DPT_REGEX_PATTERN.matcher(dptID);
        if (!m.matches() || m.groupCount() != 2) {
            LOGGER.warn("toDPTValue couldn't identify main/sub number in dptID '{}'", dptID);
            return null;
        }

        String mainNumber = m.group("main");
        int subNumber = Integer.parseInt(m.group("sub"));

        try {
            DPTXlator translator = TranslatorTypes.createTranslator(Integer.parseInt(mainNumber), dptID);
            dpt = translator.getType();
        } catch (KNXException e) {
            return null;
        }

        try {
            // check for HSBType first, because it extends PercentType as well
            if (type instanceof HSBType) {
                switch (mainNumber) {
                    case "5":
                        switch (subNumber) {
                            case 3: // * 5.003: Angle, values: 0...360 °
                                return ((HSBType) type).getHue().toString();
                            case 1: // * 5.001: Scaling, values: 0...100 %
                            default:
                                return ((HSBType) type).getBrightness().toString();
                        }
                    case "232":
                        switch (subNumber) {
                            case 600: // 232.600
                                HSBType hc = ((HSBType) type);
                                return "r:" + convertPercentToByte(hc.getRed()) + " g:"
                                        + convertPercentToByte(hc.getGreen()) + " b:"
                                        + convertPercentToByte(hc.getBlue());
                        }
                    default:
                        HSBType hc = ((HSBType) type);
                        return "r:" + hc.getRed().intValue() + " g:" + hc.getGreen().intValue() + " b:"
                                + hc.getBlue().intValue();
                }
            } else if (type instanceof OnOffType) {
                return type.equals(OnOffType.OFF) ? dpt.getLowerValue() : dpt.getUpperValue();
            } else if (type instanceof UpDownType) {
                return type.equals(UpDownType.UP) ? dpt.getLowerValue() : dpt.getUpperValue();
            } else if (type instanceof IncreaseDecreaseType) {
                DPT valueDPT = ((DPTXlator3BitControlled.DPT3BitControlled) dpt).getControlDPT();
                return type.equals(IncreaseDecreaseType.DECREASE) ? valueDPT.getLowerValue() + " 5"
                        : valueDPT.getUpperValue() + " 5";
            } else if (type instanceof OpenClosedType) {
                return type.equals(OpenClosedType.CLOSED) ? dpt.getLowerValue() : dpt.getUpperValue();
            } else if (type instanceof StopMoveType) {
                return type.equals(StopMoveType.STOP) ? dpt.getLowerValue() : dpt.getUpperValue();
            } else if (type instanceof PercentType) {
                return String.valueOf(((DecimalType) type).intValue());
            } else if (type instanceof DecimalType || type instanceof QuantityType<?>) {
                if ("2".equals(mainNumber)) {
                    int intVal = type instanceof DecimalType ? ((DecimalType) type).intValue()
                            : ((QuantityType<?>) type).intValue();
                    DPT valueDPT = ((DPTXlator1BitControlled.DPT1BitControlled) dpt).getValueDPT();
                    switch (intVal) {
                        case 0:
                            return "0 " + valueDPT.getLowerValue();
                        case 1:
                            return "0 " + valueDPT.getUpperValue();
                        case 2:
                            return "1 " + valueDPT.getLowerValue();
                        default:
                            return "1 " + valueDPT.getUpperValue();
                    }
                } else if ("18".equals(mainNumber)) {
                    int intVal = type instanceof DecimalType ? ((DecimalType) type).intValue()
                            : ((QuantityType<?>) type).intValue();
                    if (intVal > 63) {
                        return "learn " + (intVal - 0x80);
                    } else {
                        return "activate " + intVal;
                    }
                } else {
                    BigDecimal bigDecimal = type instanceof DecimalType ? ((DecimalType) type).toBigDecimal()
                            : ((QuantityType<?>) type).toBigDecimal();
                    return bigDecimal.stripTrailingZeros().toPlainString();
                }
            } else if (type instanceof StringType) {
                return type.toString();
            } else if (type instanceof DateTimeType) {
                return formatDateTime((DateTimeType) type, dptID);
            }
        } catch (Exception e) {
            LOGGER.warn("An exception occurred converting type {} to dpt id {}: error message={}", type, dptID,
                    e.getMessage());
            return null;
        }

        LOGGER.debug("toDPTValue: Couldn't convert type {} to dpt id {} (no mapping).", type, dptID);

        return null;
    }

    public static @Nullable Type toType(Datapoint datapoint, byte[] data) {
        try {
            DPTXlator translator = TranslatorTypes.createTranslator(datapoint.getMainNumber(), datapoint.getDPT());
            translator.setData(data);
            String value = translator.getValue();

            String id = translator.getType().getID();
            LOGGER.trace("toType datapoint DPT = {}", datapoint.getDPT());

            Matcher m = DPT_REGEX_PATTERN.matcher(id);
            if (!m.matches() || m.groupCount() != 2) {
                LOGGER.warn("toDPTValue couldn't identify main/sub number in dptID '{}'", id);
                return null;
            }

            /*
             * Following code section deals with specific mapping of values from KNX to openHAB types were the String
             * received from the DPTXlator is not sufficient to set the openHAB type or has bugs
             */
            switch (m.group("main")) {
                case "1":
                    DPTXlatorBoolean translatorBoolean = (DPTXlatorBoolean) translator;
                    switch (m.group("sub")) {
                        case "8":
                            return translatorBoolean.getValueBoolean() ? UpDownType.DOWN : UpDownType.UP;
                        case "9":
                        case "19":
                            // This is wrong for DPT 9. It should be true -> CLOSE, false -> OPEN, but unfortunately
                            // can't be fixed without breaking a lot of working installations.
                            // The documentation has been updated to reflect that. / @J-N-K
                            return translatorBoolean.getValueBoolean() ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
                        case "10":
                            return translatorBoolean.getValueBoolean() ? StopMoveType.MOVE : StopMoveType.STOP;
                        case "22":
                            return DecimalType.valueOf(translatorBoolean.getValueBoolean() ? "1" : "0");
                        default:
                            return OnOffType.from(translatorBoolean.getValueBoolean());
                    }
                case "2":
                    DPTXlator1BitControlled translator1BitControlled = (DPTXlator1BitControlled) translator;
                    int decValue = (translator1BitControlled.getControlBit() ? 2 : 0)
                            + (translator1BitControlled.getValueBit() ? 1 : 0);
                    return new DecimalType(decValue);
                case "3":
                    DPTXlator3BitControlled translator3BitControlled = (DPTXlator3BitControlled) translator;
                    if (translator3BitControlled.getStepCode() == 0) {
                        LOGGER.debug("toType: KNX DPT_Control_Dimming: break received.");
                        return UnDefType.UNDEF;
                    }
                    switch (m.group("sub")) {
                        case "7":
                            return translator3BitControlled.getControlBit() ? IncreaseDecreaseType.INCREASE
                                    : IncreaseDecreaseType.DECREASE;
                        case "8":
                            return translator3BitControlled.getControlBit() ? UpDownType.DOWN : UpDownType.UP;
                    }
                case "18":
                    DPTXlatorSceneControl translatorSceneControl = (DPTXlatorSceneControl) translator;
                    int decimalValue = translatorSceneControl.getSceneNumber();
                    if (value.startsWith("learn")) {
                        decimalValue += 0x80;
                    }
                    value = String.valueOf(decimalValue);

                    break;
                case "19":
                    DPTXlatorDateTime translatorDateTime = (DPTXlatorDateTime) translator;
                    if (translatorDateTime.isFaultyClock()) {
                        // Not supported: faulty clock
                        LOGGER.debug("toType: KNX clock msg ignored: clock faulty bit set, which is not supported");
                        return null;
                    } else if (!translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                            && translatorDateTime.isValidField(DPTXlatorDateTime.DATE)) {
                        // Not supported: "/1/1" (month and day without year)
                        LOGGER.debug(
                                "toType: KNX clock msg ignored: no year, but day and month, which is not supported");
                        return null;
                    } else if (translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                            && !translatorDateTime.isValidField(DPTXlatorDateTime.DATE)) {
                        // Not supported: "1900" (year without month and day)
                        LOGGER.debug(
                                "toType: KNX clock msg ignored: no day and month, but year, which is not supported");
                        return null;
                    } else if (!translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                            && !translatorDateTime.isValidField(DPTXlatorDateTime.DATE)
                            && !translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
                        // Not supported: No year, no date and no time
                        LOGGER.debug("toType: KNX clock msg ignored: no day and month or year, which is not supported");
                        return null;
                    }

                    Calendar cal = Calendar.getInstance();
                    if (translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                            && !translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
                        // Pure date format, no time information
                        cal.setTimeInMillis(translatorDateTime.getValueMilliseconds());
                        value = new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(cal.getTime());
                        return DateTimeType.valueOf(value);
                    } else if (!translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                            && translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
                        // Pure time format, no date information
                        cal.clear();
                        cal.set(Calendar.HOUR_OF_DAY, translatorDateTime.getHour());
                        cal.set(Calendar.MINUTE, translatorDateTime.getMinute());
                        cal.set(Calendar.SECOND, translatorDateTime.getSecond());
                        value = new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(cal.getTime());
                        return DateTimeType.valueOf(value);
                    } else if (translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                            && translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
                        // Date format and time information
                        cal.setTimeInMillis(translatorDateTime.getValueMilliseconds());
                        value = new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(cal.getTime());
                        return DateTimeType.valueOf(value);
                    }
                    break;
            }

            Set<Class<? extends Type>> typeClass = toTypeClass(id);
            if (typeClass.isEmpty()) {
                return null;
            }

            if (typeClass.contains(PercentType.class)) {
                return new PercentType(BigDecimal.valueOf(Math.round(translator.getNumericValue())));
            }
            if (typeClass.contains(DecimalType.class)) {
                return new DecimalType(translator.getNumericValue());
            }
            if (typeClass.contains(StringType.class)) {
                return StringType.valueOf(value);
            }

            if (typeClass.contains(DateTimeType.class)) {
                String date = formatDateTime(value, datapoint.getDPT());
                if (date.isEmpty()) {
                    LOGGER.debug("toType: KNX clock msg ignored: date object empty {}.", date);
                    return null;
                } else {
                    return DateTimeType.valueOf(date);
                }
            }

            if (typeClass.contains(HSBType.class)) {
                // value has format of "r:<red value> g:<green value> b:<blue value>"
                int r = Integer.parseInt(value.split(" ")[0].split(":")[1]);
                int g = Integer.parseInt(value.split(" ")[1].split(":")[1]);
                int b = Integer.parseInt(value.split(" ")[2].split(":")[1]);

                return HSBType.fromRGB(r, g, b);
            }

        } catch (KNXFormatException kfe) {
            LOGGER.info("Translator couldn't parse data for datapoint type '{}' (KNXFormatException).",
                    datapoint.getDPT());
        } catch (KNXIllegalArgumentException kiae) {
            LOGGER.info("Translator couldn't parse data for datapoint type '{}' (KNXIllegalArgumentException).",
                    datapoint.getDPT());
        } catch (KNXException e) {
            LOGGER.warn("Failed creating a translator for datapoint type '{}'.", datapoint.getDPT(), e);
        }

        return null;
    }

    /**
     * Converts a datapoint type id into an openHAB type class
     *
     * @param dptId the datapoint type id
     * @return the openHAB type (command or state) class or {@code null} if the datapoint type id is not supported.
     */
    public static Set<Class<? extends Type>> toTypeClass(@Nullable String dptId) {
        if (dptId == null) {
            return Set.of();
        }

        Set<Class<? extends Type>> ohClass = DPT_TYPE_MAP.get(dptId);
        if (ohClass == null) {
            Matcher m = DPT_REGEX_PATTERN.matcher(dptId);
            if (!m.matches() || m.groupCount() < 1) {
                LOGGER.warn("toDPTValue couldn't identify main number in dptID '{}'", dptId);
                return Set.of();
            }

            ohClass = DPT_MAIN_TYPE_MAP.getOrDefault(m.group("main"), Set.of());
        }
        return ohClass;
    }

    /**
     * Converts an openHAB type class into a datapoint type id.
     *
     * @param typeClass the openHAB type class
     * @return the datapoint type id
     */
    public static @Nullable String toDPTid(Class<? extends Type> typeClass) {
        return DEFAULT_DPT_MAP.get(typeClass);
    }

    /**
     * Formats the given <code>value</code> according to the datapoint type
     * <code>dpt</code> to a String which can be processed by {@link DateTimeType}.
     *
     * @param value
     * @param dpt
     *
     * @return a formatted String like </code>yyyy-MM-dd'T'HH:mm:ss</code> which
     *         is target format of the {@link DateTimeType}
     */
    private static String formatDateTime(String value, String dpt) {
        Date date = null;

        try {
            if (DPTXlatorDate.DPT_DATE.getID().equals(dpt)) {
                date = new SimpleDateFormat(DATE_FORMAT).parse(value);
            } else if (DPTXlatorTime.DPT_TIMEOFDAY.getID().equals(dpt)) {
                if (value.contains("no-day")) {
                    /*
                     * KNX "no-day" needs special treatment since openHAB's DateTimeType doesn't support "no-day".
                     * Workaround: remove the "no-day" String, parse the remaining time string, which will result in a
                     * date of "1970-01-01".
                     * Replace "no-day" with the current day name
                     */
                    StringBuffer stb = new StringBuffer(value);
                    int start = stb.indexOf("no-day");
                    int end = start + "no-day".length();
                    stb.replace(start, end, String.format(Locale.US, "%1$ta", Calendar.getInstance()));
                    value = stb.toString();
                }
                date = new SimpleDateFormat(TIME_DAY_FORMAT, Locale.US).parse(value);
            }
        } catch (ParseException pe) {
            // do nothing but logging
            LOGGER.warn("Could not parse '{}' to a valid date", value);
        }

        return date != null ? new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(date) : "";
    }

    /**
     * Formats the given internal <code>dateType</code> to a knx readable String
     * according to the target datapoint type <code>dpt</code>.
     *
     * @param dateType
     * @param dpt the target datapoint type
     *
     * @return a String which contains either an ISO8601 formatted date (yyyy-mm-dd),
     *         a formatted 24-hour clock with the day of week prepended (Mon, 12:00:00) or
     *         a formatted 24-hour clock (12:00:00)
     *
     * @throws IllegalArgumentException if none of the datapoint types DPT_DATE or
     *             DPT_TIMEOFDAY has been used.
     */
    private static String formatDateTime(DateTimeType dateType, @Nullable String dpt) {
        if (DPTXlatorDate.DPT_DATE.getID().equals(dpt)) {
            return dateType.format("%tF");
        } else if (DPTXlatorTime.DPT_TIMEOFDAY.getID().equals(dpt)) {
            return dateType.format(Locale.US, "%1$ta, %1$tT");
        } else if (DPTXlatorDateTime.DPT_DATE_TIME.getID().equals(dpt)) {
            return dateType.format(Locale.US, "%tF %1$tT");
        } else {
            throw new IllegalArgumentException("Could not format date to datapoint type '" + dpt + "'");
        }
    }

    /**
     * convert 0...100% to 1 byte 0..255
     *
     * @param percent
     * @return int 0..255
     */
    private static int convertPercentToByte(PercentType percent) {
        return percent.toBigDecimal().multiply(BigDecimal.valueOf(255))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).intValue();
    }
}
