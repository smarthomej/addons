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
package org.smarthomej.binding.knx.internal.dpt;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
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
import org.smarthomej.binding.knx.internal.ColorUtil;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlator1BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.DPTXlatorDateTime;
import tuwien.auto.calimero.dptxlator.DPTXlatorSceneControl;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

/**
 * This class decodes raw data received from the KNX bus to an openHAB datatype
 *
 * Parts of this code are based on the openHAB KNXCoreTypeMapper by Kai Kreuzer et al.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ValueDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValueDecoder.class);

    private static final String TIME_DAY_FORMAT = "EEE, HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    // RGB: "r:123 g:123 b:123" value-range: 0-255
    private static final Pattern RGB_PATTERN = Pattern.compile("r:(?<r>\\d+) g:(?<g>\\d+) b:(?<b>\\d+)");
    // RGBW: "100 27 25 12 %", value range: 0-100, invalid values: "-"
    private static final Pattern RGBW_PATTERN = Pattern
            .compile("(?:(?<r>\\d+)|-)\\s(?:(?<g>\\d+)|-)\\s(?:(?<b>\\d+)|-)\\s(?:(?<w>\\d+)|-)\\s%");
    // xyY: "(0,123 0,123) 56 %", value range 0-1 for xy (comma as decimal point), 0-100 for Y, invalid values omitted
    private static final Pattern XYY_PATTERN = Pattern
            .compile("(?:\\((?<x>\\d+(?:,\\d+)?) (?<y>\\d+(?:,\\d+)?)\\))?\\s*(?:(?<Y>\\d+(?:,\\d+)?)\\s%)?");

    /**
     * convert the raw value received to the corresponding openHAB value
     *
     * @param dptId the DPT of the given data
     * @param data a byte array containing the value
     * @param preferredType the preferred datatype for this conversion
     * @return the data converted to an openHAB Type (or null if conversion failed)
     */
    public static @Nullable Type decode(String dptId, byte[] data, Class<? extends Type> preferredType) {
        try {
            DPTXlator translator = TranslatorTypes.createTranslator(0,
                    DPTUtil.NORMALIZED_DPT.getOrDefault(dptId, dptId));
            translator.setData(data);
            String value = translator.getValue();

            String id = dptId; // prefer using the user-supplied DPT

            Matcher m = DPTUtil.DPT_PATTERN.matcher(id);
            if (!m.matches() || m.groupCount() != 2) {
                LOGGER.trace("User-Supplied DPT '{}' did not match for sub-type, using DPT returned from Translator",
                        id);
                id = translator.getType().getID();
                m = DPTUtil.DPT_PATTERN.matcher(id);
                if (!m.matches() || m.groupCount() != 2) {
                    LOGGER.warn("couldn't identify main/sub number in dptID '{}'", id);
                    return null;
                }
            }
            LOGGER.trace("Finally using datapoint DPT = {}", id);

            String mainType = m.group("main");
            String subType = m.group("sub");

            switch (mainType) {
                case "1":
                    return handleDpt1(subType, translator);
                case "2":
                    DPTXlator1BitControlled translator1BitControlled = (DPTXlator1BitControlled) translator;
                    int decValue = (translator1BitControlled.getControlBit() ? 2 : 0)
                            + (translator1BitControlled.getValueBit() ? 1 : 0);
                    return new DecimalType(decValue);
                case "3":
                    return handleDpt3(subType, translator);
                case "10":
                    return handleDpt10(value);
                case "11":
                    return DateTimeType.valueOf(new SimpleDateFormat(DateTimeType.DATE_PATTERN)
                            .format(new SimpleDateFormat(DATE_FORMAT).parse(value)));
                case "18":
                    DPTXlatorSceneControl translatorSceneControl = (DPTXlatorSceneControl) translator;
                    int decimalValue = translatorSceneControl.getSceneNumber();
                    if (value.startsWith("learn")) {
                        decimalValue += 0x80;
                    }
                    return new DecimalType(decimalValue);
                case "19":
                    return handleDpt19(translator);
                case "16":
                case "20":
                case "21":
                case "22":
                case "28":
                    return StringType.valueOf(value);
                case "232":
                    return handleDpt232(value, subType);
                case "242":
                    return handleDpt242(value);
                case "251":
                    return handleDpt251(value, preferredType);
                default:
                    return handleNumericDpt(id, translator, preferredType);
            }
        } catch (NumberFormatException | KNXFormatException | KNXIllegalArgumentException | ParseException e) {
            LOGGER.info("Translator couldn't parse data '{}' for datapoint type '{}' ({}).", data, dptId, e.getClass());
        } catch (KNXException e) {
            LOGGER.warn("Failed creating a translator for datapoint type '{}'.", dptId, e);
        }

        return null;
    }

    private static Type handleDpt1(String subType, DPTXlator translator) {
        DPTXlatorBoolean translatorBoolean = (DPTXlatorBoolean) translator;
        switch (subType) {
            case "008":
                return translatorBoolean.getValueBoolean() ? UpDownType.DOWN : UpDownType.UP;
            case "009":
            case "019":
                // This is wrong for DPT 1.009. It should be true -> CLOSE, false -> OPEN, but unfortunately
                // can't be fixed without breaking a lot of working installations.
                // The documentation has been updated to reflect that. / @J-N-K
                return translatorBoolean.getValueBoolean() ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
            case "010":
                return translatorBoolean.getValueBoolean() ? StopMoveType.MOVE : StopMoveType.STOP;
            case "022":
                return DecimalType.valueOf(translatorBoolean.getValueBoolean() ? "1" : "0");
            default:
                return OnOffType.from(translatorBoolean.getValueBoolean());
        }
    }

    private static @Nullable Type handleDpt3(String subType, DPTXlator translator) {
        DPTXlator3BitControlled translator3BitControlled = (DPTXlator3BitControlled) translator;
        if (translator3BitControlled.getStepCode() == 0) {
            LOGGER.debug("convertRawDataToType: KNX DPT_Control_Dimming: break received.");
            return UnDefType.NULL;
        }
        switch (subType) {
            case "007":
                return translator3BitControlled.getControlBit() ? IncreaseDecreaseType.INCREASE
                        : IncreaseDecreaseType.DECREASE;
            case "008":
                return translator3BitControlled.getControlBit() ? UpDownType.DOWN : UpDownType.UP;
            default:
                LOGGER.warn("DPT3, subtype '{}' is unknown.", subType);
                return null;
        }
    }

    private static Type handleDpt10(String value) throws ParseException {
        if (value.contains("no-day")) {
            /*
             * KNX "no-day" needs special treatment since openHAB's DateTimeType doesn't support "no-day".
             * Workaround: remove the "no-day" String, parse the remaining time string, which will result in a
             * date of "1970-01-01".
             * Replace "no-day" with the current day name
             */
            StringBuilder stb = new StringBuilder(value);
            int start = stb.indexOf("no-day");
            int end = start + "no-day".length();
            stb.replace(start, end, String.format(Locale.US, "%1$ta", Calendar.getInstance()));
            value = stb.toString();
        }
        return DateTimeType.valueOf(new SimpleDateFormat(DateTimeType.DATE_PATTERN)
                .format(new SimpleDateFormat(TIME_DAY_FORMAT, Locale.US).parse(value)));
    }

    private static @Nullable Type handleDpt19(DPTXlator translator) throws KNXFormatException {
        DPTXlatorDateTime translatorDateTime = (DPTXlatorDateTime) translator;
        if (translatorDateTime.isFaultyClock()) {
            // Not supported: faulty clock
            LOGGER.debug("KNX clock msg ignored: clock faulty bit set, which is not supported");
            return null;
        } else if (!translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                && translatorDateTime.isValidField(DPTXlatorDateTime.DATE)) {
            // Not supported: "/1/1" (month and day without year)
            LOGGER.debug("KNX clock msg ignored: no year, but day and month, which is not supported");
            return null;
        } else if (translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                && !translatorDateTime.isValidField(DPTXlatorDateTime.DATE)) {
            // Not supported: "1900" (year without month and day)
            LOGGER.debug("KNX clock msg ignored: no day and month, but year, which is not supported");
            return null;
        } else if (!translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                && !translatorDateTime.isValidField(DPTXlatorDateTime.DATE)
                && !translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
            // Not supported: No year, no date and no time
            LOGGER.debug("KNX clock msg ignored: no day and month or year, which is not supported");
            return null;
        }

        Calendar cal = Calendar.getInstance();
        if (translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                && !translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
            // Pure date format, no time information
            cal.setTimeInMillis(translatorDateTime.getValueMilliseconds());
            String value = new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(cal.getTime());
            return DateTimeType.valueOf(value);
        } else if (!translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                && translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
            // Pure time format, no date information
            cal.clear();
            cal.set(Calendar.HOUR_OF_DAY, translatorDateTime.getHour());
            cal.set(Calendar.MINUTE, translatorDateTime.getMinute());
            cal.set(Calendar.SECOND, translatorDateTime.getSecond());
            String value = new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(cal.getTime());
            return DateTimeType.valueOf(value);
        } else if (translatorDateTime.isValidField(DPTXlatorDateTime.YEAR)
                && translatorDateTime.isValidField(DPTXlatorDateTime.TIME)) {
            // Date format and time information
            cal.setTimeInMillis(translatorDateTime.getValueMilliseconds());
            String value = new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(cal.getTime());
            return DateTimeType.valueOf(value);
        } else {
            LOGGER.warn("Failed to convert '{}'", translator.getValue());
            return null;
        }
    }

    private static @Nullable Type handleDpt232(String value, String subType) {
        Matcher rgb = RGB_PATTERN.matcher(value);
        if (rgb.matches()) {
            int r = Integer.parseInt(rgb.group("r"));
            int g = Integer.parseInt(rgb.group("g"));
            int b = Integer.parseInt(rgb.group("b"));

            switch (subType) {
                case "600":
                    return HSBType.fromRGB(r, g, b);
                case "60000":
                    // MDT specific: mis-use 232.600 for hsv instead of rgb
                    return new HSBType(new DecimalType(r * 360.0 / 255.0), new PercentType(new BigDecimal(g / 2.55)),
                            new PercentType(new BigDecimal(b / 2.55)));
                default:
                    LOGGER.warn("Unknown subtype '232.{}', no conversion possible.", subType);
                    return null;
            }
        }
        LOGGER.warn("Failed to convert '{}' (DPT 232): Pattern does not match", value);
        return null;
    }

    private static @Nullable Type handleDpt242(String value) {
        Matcher xyY = XYY_PATTERN.matcher(value);
        if (xyY.matches()) {
            String xString = xyY.group("x");
            String yString = xyY.group("y");
            String YString = xyY.group("Y");

            if (xString != null && yString != null) {
                double x = Double.parseDouble(xString.replace(",", "."));
                double y = Double.parseDouble(yString.replace(",", "."));
                if (YString == null) {
                    return ColorUtil.xyToHsv(new double[] { x, y });
                } else {
                    double Y = Double.parseDouble(YString.replace(",", "."));
                    return ColorUtil.xyToHsv(new double[] { x, y, Y });
                }
            }
        }
        LOGGER.warn("Failed to convert '{}' (DPT 242): Pattern does not match", value);
        return null;
    }

    private static @Nullable Type handleDpt251(String value, Class<? extends Type> preferredType) {
        Matcher rgbw = RGBW_PATTERN.matcher(value);
        if (rgbw.matches()) {
            String rString = rgbw.group("r");
            String gString = rgbw.group("g");
            String bString = rgbw.group("b");
            String wString = rgbw.group("w");

            if (rString != null && gString != null && bString != null && HSBType.class.equals(preferredType)) {
                // does not support PercentType and r,g,b valid -> HSBType
                int r = (int) (Integer.parseInt(rString) * 2.56);
                int g = (int) (Integer.parseInt(gString) * 2.56);
                int b = (int) (Integer.parseInt(bString) * 2.56);

                return HSBType.fromRGB(r, g, b);
            } else if (wString != null && PercentType.class.equals(preferredType)) {
                // does support PercentType and w valid -> PercentType
                int w = Integer.parseInt(wString);

                return new PercentType(w);
            }
        }
        LOGGER.warn("Failed to convert '{}' (DPT 251): Pattern does not match or invalid content", value);
        return null;
    }

    private static @Nullable Type handleNumericDpt(String id, DPTXlator translator, Class<? extends Type> preferredType)
            throws KNXFormatException {
        Set<Class<? extends Type>> allowedTypes = DPTUtil.getAllowedTypes(id);

        double value = translator.getNumericValue();
        if (allowedTypes.contains(PercentType.class) && PercentType.class.equals(preferredType)) {
            return new PercentType(BigDecimal.valueOf(Math.round(value)));
        } else if (allowedTypes.contains(QuantityType.class)) {
            String unit = DPTUnits.getUnitForDpt(id);
            if (unit != null) {
                return new QuantityType<>(value + " " + unit);
            } else {
                LOGGER.trace("Could not determine unit for DPT '{}', fallback to plain decimal", id);
            }
        } else if (allowedTypes.contains(DecimalType.class)) {
            return new DecimalType(value);
        }
        LOGGER.warn("Failed to convert '{}' (DPT '{}'): no matching type found", value, id);
        return null;
    }
}
