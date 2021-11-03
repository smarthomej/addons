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
package org.smarthomej.binding.viessmann.internal.dto.features;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ViessmannFeatureMap} class defines maping for features.
 *
 * @author Ronny Grun - Initial contribution
 */
@NonNullByDefault
public class ViessmannFeatureMap {
    public static final Map<String, String> NAME_MAP = new HashMap<>();
    public static final Map<String, String> DESCRIPTION_MAP = new HashMap<>();

    public Map<String, String> getNameMap() {
        NAME_MAP.put("device.serial", "Serial");
        NAME_MAP.put("device.timezone", "Time zone");
        NAME_MAP.put("gateway.devices", "Devices");
        NAME_MAP.put("gateway.ethernet", "Ethernet");
        NAME_MAP.put("gateway.ethernet.config", "Ethernet config");
        NAME_MAP.put("gateway.wifi", "wifi");
        NAME_MAP.put("heating.boiler.sensors.temperature.commonSupply", "Boiler temperature sensor - Common supply");
        NAME_MAP.put("heating.boiler.sensors.temperature.main", "Boiler temperature sensor - Main");
        NAME_MAP.put("heating.boiler.serial", "Boiler serial");
        NAME_MAP.put("heating.boiler.temperature", "Boiler temperature");
        NAME_MAP.put("heating.circuits", "Circuits");
        NAME_MAP.put("heating.circuits.N", "Circuits");
        NAME_MAP.put("heating.circuits.N.circulation.pump", "Circulation pump");
        NAME_MAP.put("heating.circuits.N.circulation.secondaryPump", "Secondary circulation pump");
        NAME_MAP.put("heating.circuits.N.dhw.pumps.circulation.schedule", "DHW pump circulation schedule");
        NAME_MAP.put("heating.circuits.N.dhw.schedule", "DHW schedule");
        NAME_MAP.put("heating.circuits.N.frostprotection", "Frost protection");
        NAME_MAP.put("heating.circuits.N.heating.schedule", "Heating schedule");
        NAME_MAP.put("heating.circuits.N.operating.modes.active", "Operating mode - active");
        NAME_MAP.put("heating.circuits.N.operating.modes.cooling", "Operating mode - cooling");
        NAME_MAP.put("heating.circuits.N.operating.modes.dhw", "Operating mode - DHW");
        NAME_MAP.put("heating.circuits.N.operating.modes.dhwAndHeating", "Operating mode - DHW and heating");
        NAME_MAP.put("heating.circuits.N.operating.modes.dhwAndHeatingCooling",
                "Operating mode - DHW and heating/cooling");
        NAME_MAP.put("heating.circuits.N.operating.modes.heating", "Operating mode - heating");
        NAME_MAP.put("heating.circuits.N.operating.modes.heatingCooling", "Operating mode - heating/cooling");
        NAME_MAP.put("heating.circuits.N.operating.modes.normalStandby", "Operating mode - normal standby");
        NAME_MAP.put("heating.circuits.N.operating.modes.standby", "Operating mode - standby");
        NAME_MAP.put("heating.circuits.N.operating.programs.active", "Operating program - active");
        NAME_MAP.put("heating.circuits.N.operating.programs.comfort", "Operating program - comfort");
        NAME_MAP.put("heating.circuits.N.operating.programs.eco", "Operating program - eco");
        NAME_MAP.put("heating.circuits.N.operating.programs.external", "Operating program - external");
        NAME_MAP.put("heating.circuits.N.operating.programs.fixed", "Operating program - fixed");
        NAME_MAP.put("heating.circuits.N.operating.programs.forcedLastFromSchedule",
                "Operating program - Forced last from schedule");
        NAME_MAP.put("heating.circuits.N.operating.programs.holiday", "Operating program - holiday");
        NAME_MAP.put("heating.circuits.N.operating.programs.holidayAtHome", "Operating program - holiday at home");
        NAME_MAP.put("heating.circuits.N.operating.programs.manual", "Operating program - manual");
        NAME_MAP.put("heating.circuits.N.operating.programs.noDemandByZone", "Operating program - no demand by zone");
        NAME_MAP.put("heating.circuits.N.operating.programs.normal", "Operating program - normal");
        NAME_MAP.put("heating.circuits.N.operating.programs.reduced", "Operating program - reduced");
        NAME_MAP.put("heating.circuits.N.operating.programs.standby", "Operating program - standby");
        NAME_MAP.put("heating.circuits.N.operating.programs.summerEco", "Operating program - summer eco");
        NAME_MAP.put("heating.circuits.N.sensors.temperature.room", "Room temperature sensor");
        NAME_MAP.put("heating.circuits.N.sensors.temperature.supply", "Supply temperature sensor");
        NAME_MAP.put("heating.circuits.N.temperature", "Temperature");
        NAME_MAP.put("heating.circuits.N.temperature.levels", "Temperature levels");
        NAME_MAP.put("heating.circuits.N.zone.demand", "Zone demand");
        NAME_MAP.put("heating.circuits.N.zone.mode", "Zone mode");
        NAME_MAP.put("heating.configuration.multiFamilyHouse", "Configuration - multi family house");
        NAME_MAP.put("heating.controller.serial", "Controller serial");
        NAME_MAP.put("heating.device.time.offset", "Device time offset");
        NAME_MAP.put("heating.device.variant", "Device variant");
        NAME_MAP.put("heating.dhw", "DHW in installation");
        NAME_MAP.put("heating.dhw.charging", "DHW charging function");
        NAME_MAP.put("heating.dhw.charging.level", "DHW charging levels");
        NAME_MAP.put("heating.dhw.comfort", "DHW comfort function");
        NAME_MAP.put("heating.dhw.oneTimeCharge", "DHW one time charge function");
        NAME_MAP.put("heating.dhw.pumps.circulation", "DHW circulation pump");
        NAME_MAP.put("heating.dhw.pumps.circulation.schedule", "DHW circulation pump - schedule");
        NAME_MAP.put("heating.dhw.pumps.primary", "DHW pump - primary");
        NAME_MAP.put("heating.dhw.schedule", "DHW schedule");
        NAME_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage", "DHW temperature sensor");
        NAME_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.bottom", "DHW temperature sensor - bottom");
        NAME_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.midBottom",
                "DHW temperature sensor - mid bottom");
        NAME_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.middle", "DHW temperature sensor - middle");
        NAME_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.top", "DHW temperature sensor - top");
        NAME_MAP.put("heating.dhw.sensors.temperature.outlet", "DHW temperature sensor - outlet");
        NAME_MAP.put("heating.dhw.temperature.hysteresis", "DHW temperature hysteresis");
        NAME_MAP.put("heating.dhw.temperature.levels", "DHW temperature levels");
        NAME_MAP.put("heating.dhw.temperature.main", "DHW main temperature");
        NAME_MAP.put("heating.dhw.temperature.temp2", "DHW temperature 2");
        NAME_MAP.put("heating.operating.programs.holiday", "Operating program - holiday");
        NAME_MAP.put("heating.operating.programs.holidayAtHome", "Operating program - holiday at home");
        NAME_MAP.put("heating.sensors.temperature.outside", "Outside temperature sensor");
        NAME_MAP.put("heating.sensors.temperature.return", "Flow return temperature sensor");
        NAME_MAP.put("heating.increasedReturn.temperature", "Return increase temperature");
        NAME_MAP.put("heating.compressors", "Compressors");
        NAME_MAP.put("heating.compressors.N", "Compressors - N");
        NAME_MAP.put("heating.sensors.temperature.hydraulicSeparator", "Sensor - hydraulic separator temperature");
        NAME_MAP.put("heating.sensors.temperature.increasedReturn", "Sensor - increased return temperature");
        NAME_MAP.put("heating.sensors.temperature.systemReturn", "Sensor - system return temperature");
        NAME_MAP.put("heating.sensors.valve.bufferDischargeThreeWayValve", "Sensor - buffer discharge");
        NAME_MAP.put("heating.sensors.volumetricFlow.return", "Sensor - return volumetric flow");
        NAME_MAP.put("heating.burners.N", "Burner");
        NAME_MAP.put("heating.circuits.N.heating.curve", "Heating curve");
        NAME_MAP.put("heating.burners.N.modulation", "Burner modulation");
        NAME_MAP.put("heating.compressors.N.statistics", "Compressor statistics");
        NAME_MAP.put("heating.burners.N.statistics", "Burner statistics");
        NAME_MAP.put("heating.solar.power.production", "Solar thermal power production");
        NAME_MAP.put("heating.gas.consumption.dhw", "Gas consumption DHW");
        NAME_MAP.put("heating.gas.consumption.heating", "Gas consumption heating");
        NAME_MAP.put("heating.solar.pumps.circuit", "Solar thermal pump");
        NAME_MAP.put("heating.solar.sensors.temperature.collector", "Solar thermal temperature collector");
        NAME_MAP.put("heating.solar.sensors.temperature.dhw", "Solar thermal temperature DHW");
        NAME_MAP.put("heating.power.consumption.total", "Power consumption total");
        NAME_MAP.put("heating.power.consumption.dhw", "Power consumption DHW");
        NAME_MAP.put("heating.power.consumption.heating", "Power consumption heating");
        NAME_MAP.put("heating.gas.consumption.total", "Gas consumption total");
        NAME_MAP.put("heating.dhw.temperature.hygiene", "DHW temperature hygiene");
        NAME_MAP.put("ventilation.operating.modes.active", "Operating mode - active");
        NAME_MAP.put("ventilation.operating.modes.standard", "Operating mode - standard");
        NAME_MAP.put("ventilation.operating.modes.standby", "Operating mode - standby");
        NAME_MAP.put("ventilation.operating.modes.ventilation", "Operating mode - ventilation");
        NAME_MAP.put("ventilation.operating.programs.active", "Operating program - active");
        NAME_MAP.put("ventilation.operating.programs.basic", "Operating program - basic");
        NAME_MAP.put("ventilation.operating.programs.intensive", "Operating program - intensive");
        NAME_MAP.put("ventilation.operating.programs.reduced", "Operating program - reduced");
        NAME_MAP.put("ventilation.operating.programs.standard", "Operating program - standard");
        NAME_MAP.put("ventilation.operating.programs.standby", "Operating program - standby");
        NAME_MAP.put("ventilation.schedule", "Schedule");

        return NAME_MAP;
    }

    public Map<String, String> getDescriptionMap() {
        DESCRIPTION_MAP.put("device.serial", "Shows the serial number of connected device.");
        DESCRIPTION_MAP.put("device.timezone", "Shows a timezone which is set on device, which can be set by user.");
        DESCRIPTION_MAP.put("gateway.devices", "Devices");
        DESCRIPTION_MAP.put("gateway.ethernet", "Ethernet");
        DESCRIPTION_MAP.put("gateway.ethernet.config", "Ethernet config");
        DESCRIPTION_MAP.put("gateway.wifi", "wifi");
        DESCRIPTION_MAP.put("heating.boiler.sensors.temperature.commonSupply",
                "Shows information related with temperature sensor located on the exit of heating installation of boiler.");
        DESCRIPTION_MAP.put("heating.boiler.sensors.temperature.main",
                "Shows information related with main temperature sensor.");
        DESCRIPTION_MAP.put("heating.boiler.serial",
                "(Deprecated for E3 devices will be replaced by device.serial)Shows the serial number of connected boiler.");
        DESCRIPTION_MAP.put("heating.boiler.temperature", "Shows the value of boiler temperature.");
        DESCRIPTION_MAP.put("heating.circuits", "Shows which heating circuits are enabled on device.");
        DESCRIPTION_MAP.put("heating.circuits.N", "Shows whether the N heating circuit is enabled.");
        DESCRIPTION_MAP.put("heating.circuits.N.circulation.pump", "Shows if circulation pump is on for circuit N.");
        DESCRIPTION_MAP.put("heating.circuits.N.circulation.secondaryPump",
                "Shows if secondary circulation pump is on for circuit 0.");
        DESCRIPTION_MAP.put("heating.circuits.N.dhw.pumps.circulation.schedule",
                "Shows schedule of domestic hot water pump circulation for circuit N and provides command to set a new one.");
        DESCRIPTION_MAP.put("heating.circuits.N.dhw.schedule",
                "Shows schedule of domestic hot water for circuit N and provides command to set a new one.");
        DESCRIPTION_MAP.put("heating.circuits.N.frostprotection",
                "Shows whether frost protection of installation is turn on in device.");
        DESCRIPTION_MAP.put("heating.circuits.N.heating.schedule",
                "Shows heating schedule for circuit N and provides command to set a new one.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.active",
                "Shows current active operating mode on the device and provides command to change it.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.cooling",
                "Shows whether the cooling operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.dhw",
                "Shows whether the DHW operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.dhwAndHeating",
                "Shows whether the DHW and Heating operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.dhwAndHeatingCooling",
                "Shows whether the DHW and HeatingCooling operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.heating",
                "Shows whether the Heating operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.heatingCooling",
                "Shows whether the HeatingCooling operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.normalStandby",
                "Shows whether the NormalStandby operating mode is active now.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.modes.standby",
                "Shows whether the Standby operating mode is active now, which means device will only start to protect installation from frost.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.active",
                "Shows current active operating program enabled on the device.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.comfort",
                "Shows information related to comfort program. Also provides the commands to enable/disable it and change the Comfort temperature value.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.eco", "Shows whether Eco program is active.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.external",
                "Shows information related to External program, which is set when device is handled by external controller.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.fixed", "Shows whether Fixed program is active.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.forcedLastFromSchedule",
                "Shows information related to Forced Last From Schedule program. This is last program which was enabled by schedule and it's sustained until deactivation. It can be activated if device is in any of heating operating modes.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.holiday",
                "Shows information related to Holiday program and provides command to set it.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.holidayAtHome",
                "Shows information related to HolidayAtHome program and provides command to set it. If Holiday and HolidayAtHome are overlapping, holiday at home has priority");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.manual",
                "Shows information related to manual program. Also provides the commands to enable/disable it and change the Manual temperature value.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.noDemandByZone",
                "Shows that the standby mode is the standby for the zone mode.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.normal",
                "Shows whether the Normal temperature program is active and provides command to set the temperature value.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.reduced",
                "Shows whether the Reduced temperature program is active and provides command to set the temperature value");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.standby",
                "Shows whether Standby program is active. It is active in between heating schedule entries.");
        DESCRIPTION_MAP.put("heating.circuits.N.operating.programs.summerEco",
                "Shows that the summer eco mode is active or not.");
        DESCRIPTION_MAP.put("heating.circuits.N.sensors.temperature.room",
                "Shows information about room temperature sensor.");
        DESCRIPTION_MAP.put("heating.circuits.N.sensors.temperature.supply",
                "Shows information about supply temperature sensor, i.e. water temperature on exit from boiler to heating installation.");
        DESCRIPTION_MAP.put("heating.circuits.N.temperature", "Shows the value of the circuit target temperature.");
        DESCRIPTION_MAP.put("heating.circuits.N.temperature.levels", "Temperature levels");
        DESCRIPTION_MAP.put("heating.circuits.N.zone.demand",
                "Shows if at least one zone that is hooked up to this heating circuit has a demand for heat.");
        DESCRIPTION_MAP.put("heating.circuits.N.zone.mode", "Shows if zone is connected to heating circuit.");
        DESCRIPTION_MAP.put("heating.configuration.multiFamilyHouse",
                "Shows whether the device's installation is set for multiple separated circuits. Number of features that are depended on multiFamilyHouse: 'heating.dhw.schedule', 'heating.circuits.N.dhw.schedule' and 'heating.circuits.n.circulation.schedule'.");
        DESCRIPTION_MAP.put("heating.controller.serial", "Shows serial number of the controller.");
        DESCRIPTION_MAP.put("heating.device.time.offset",
                "Shows difference between the device time and the UTC in minutes.");
        DESCRIPTION_MAP.put("heating.device.variant", "Presents further information about the device-variant.");
        DESCRIPTION_MAP.put("heating.dhw",
                "Shows whether the domestic hot water is activated/present on the heating installation.");
        DESCRIPTION_MAP.put("heating.dhw.charging", "Shows whether the hot water charging in DHW storage is active.");
        DESCRIPTION_MAP.put("heating.dhw.charging.level",
                "Shows information related to water temperature in DHW tank, the measurements of top, middle and bottom sensors.");
        DESCRIPTION_MAP.put("heating.dhw.comfort",
                "Shows if heat-exchanger for the DHW is kept warm all the time is active. Also provides the commands to activate/deactivate it.");
        DESCRIPTION_MAP.put("heating.dhw.oneTimeCharge",
                "Shows whether one time charge of dhw is active. Also provides the commands to activate/deactivate it.");
        DESCRIPTION_MAP.put("heating.dhw.pumps.circulation",
                "Shows the status of the Domestic Hot Water circulation pump.");
        DESCRIPTION_MAP.put("heating.dhw.pumps.circulation.schedule",
                "Shows pumps circulation schedule of domestic hot water.");
        DESCRIPTION_MAP.put("heating.dhw.pumps.primary", "Shows the status of the Domestic Hot Water primary pump.");
        DESCRIPTION_MAP.put("heating.dhw.schedule", "Shows schedule of domestic hot water.");
        DESCRIPTION_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage",
                "Shows information about hot water storage sensor.");
        DESCRIPTION_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.bottom",
                "Shows information about hot water storage sensor.");
        DESCRIPTION_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.midBottom",
                "Shows information about hot water storage sensor.");
        DESCRIPTION_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.middle",
                "Shows information about hot water storage sensor.");
        DESCRIPTION_MAP.put("heating.dhw.sensors.temperature.hotWaterStorage.top",
                "Shows information about hot water storage sensor.");
        DESCRIPTION_MAP.put("heating.dhw.sensors.temperature.outlet",
                "Shows information related to Domestic Hot Water temperature sensor.");
        DESCRIPTION_MAP.put("heating.dhw.temperature.hysteresis",
                "Shows the hysteresis value of the Domestic Hot Water temperature in heat pumps. Also provides the command to set it");
        DESCRIPTION_MAP.put("heating.dhw.temperature.levels",
                "Shows available DHW ranges and defaults for E3 systems.");
        DESCRIPTION_MAP.put("heating.dhw.temperature.main",
                "Shows the desired value of the Domestic Hot Water temperature. Also provides the command to set it");
        DESCRIPTION_MAP.put("heating.dhw.temperature.temp2",
                "For controllers with multiple possible dhw-setpoints. Shows the desired value of the Domestic Hot Water Temp 2 temperature. Also provides the command to set it");
        DESCRIPTION_MAP.put("heating.operating.programs.holiday",
                "Shows information related to Holiday program and provides command to set it.");
        DESCRIPTION_MAP.put("heating.operating.programs.holidayAtHome",
                "Shows information related to HolidayAtHome program and provides command to set it. If Holiday and HolidayAtHome are overlapping, holiday at home has priority");
        DESCRIPTION_MAP.put("heating.sensors.temperature.outside",
                "Shows information related to outside temperature sensor.");
        DESCRIPTION_MAP.put("heating.sensors.temperature.return",
                "Shows information about flow return temperature sensor, i.e. water temperature on return to the boiler from heating installation.");
        DESCRIPTION_MAP.put("heating.increasedReturn.temperature",
                "Shows information about desired return Temperature of the interim system e.g. Temperature behind a built-in buffer when a system is supported with a return increase.");
        DESCRIPTION_MAP.put("heating.compressors", "Shows the list of enabled compressors.");
        DESCRIPTION_MAP.put("heating.compressors.N", "Shows whether the compressor is active right now.");
        DESCRIPTION_MAP.put("heating.sensors.temperature.hydraulicSeparator",
                "Shows information about hydraulic separator temperature sensor.");
        DESCRIPTION_MAP.put("heating.sensors.temperature.increasedReturn",
                "Shows information about Return set Temperature of the interim system e.g. Temperature behind a built-in buffer when a system is supported with a return increase.");
        DESCRIPTION_MAP.put("heating.sensors.temperature.systemReturn",
                "Shows information about (Common return temperature) return sum of temperature of a cascade.");
        DESCRIPTION_MAP.put("heating.sensors.valve.bufferDischargeThreeWayValve",
                "Shows information about percentage position of the buffer discharge 3-way valve.");
        DESCRIPTION_MAP.put("heating.sensors.volumetricFlow.return",
                "Shows information about volumetric flow on the return.");
        DESCRIPTION_MAP.put("heating.burners.N", "Shows whether the burner N (0, 1, ...) is active right now.");
        DESCRIPTION_MAP.put("heating.circuits.N.heating.curve",
                "Shows values releted to heating curve and provides commands to modify it: slope and shift");
        DESCRIPTION_MAP.put("heating.burners.N.modulation", "Shows information related with current burner modulation");
        DESCRIPTION_MAP.put("heating.compressors.N.statistics",
                "Shows statistics of compressor N: hours and times started");
        DESCRIPTION_MAP.put("heating.burners.N.statistics", "Shows statistics of burner: hours and times started");
        DESCRIPTION_MAP.put("heating.solar.power.production",
                "Shows statistics of solar power production: daily, weekly, monthly, yearly");
        DESCRIPTION_MAP.put("heating.gas.consumption.dhw", "Shows statistics of gas usage for heating up the DHW");
        DESCRIPTION_MAP.put("heating.gas.consumption.heating", "Shows statistics of gas usage for heating");
        DESCRIPTION_MAP.put("heating.solar.pumps.circuit", "Shows the status of the solar pump");
        DESCRIPTION_MAP.put("heating.solar.sensors.temperature.collector", "Shows Solar Collector temperature in °C");
        DESCRIPTION_MAP.put("heating.solar.sensors.temperature.dhw",
                "Shows solar dhw (domestic hot water) temperature in °C");
        DESCRIPTION_MAP.put("heating.power.consumption.total",
                "Shows statistics of power usage. It is shown in kWh. It supercedes heating.power.consumption");
        DESCRIPTION_MAP.put("heating.power.consumption.dhw", "Shows statistics of power usage for heating up the DHW.");
        DESCRIPTION_MAP.put("heating.power.consumption.heating",
                "Shows statistics of power usage for heating up rooms.");
        DESCRIPTION_MAP.put("heating.gas.consumption.total", "Shows statistics of gas usage");
        DESCRIPTION_MAP.put("heating.dhw.temperature.hygiene",
                "To kill Legionella bacteria the system needs to heat up to at least 65 °C");
        DESCRIPTION_MAP.put("ventilation.operating.modes.active",
                "Shows current active operating mode enabled on the device.");
        DESCRIPTION_MAP.put("ventilation.operating.modes.standard",
                "Shows whether Standard operating mode is active with minimum air flow.");
        DESCRIPTION_MAP.put("ventilation.operating.modes.standby", "Shows whether Standby operating mode is active.");
        DESCRIPTION_MAP.put("ventilation.operating.modes.ventilation",
                "Shows whether Ventilation operating mode is active.");
        DESCRIPTION_MAP.put("ventilation.operating.programs.active",
                "Shows current active operating program enabled on the device.");
        DESCRIPTION_MAP.put("ventilation.operating.programs.basic",
                "Shows whether ventilation operating program is active with minimum air flow.");
        DESCRIPTION_MAP.put("ventilation.operating.programs.intensive",
                "Shows whether ventilation operating program is active with maximum air flow.");
        DESCRIPTION_MAP.put("ventilation.operating.programs.reduced",
                "Shows whether ventilation operating program is active with reduced air flow.");
        DESCRIPTION_MAP.put("ventilation.operating.programs.standard",
                "Shows whether ventilation operating program is active with standard air flow.");
        DESCRIPTION_MAP.put("ventilation.operating.programs.standby",
                "Shows whether ventilation operating program is in standby.");
        DESCRIPTION_MAP.put("ventilation.schedule",
                "Shows schedule of ventilation and provides command to set a new one.");
        return DESCRIPTION_MAP;
    }

    public @Nullable String getName(String feature) {
        getNameMap();
        Pattern pattern = Pattern.compile("(.[0-3])");
        Matcher matcher = pattern.matcher(feature);
        if (matcher.find()) {
            String circuit = matcher.group(0);
            feature = matcher.replaceAll(".N");
            String name = NAME_MAP.get(feature) + " (Circuit: " + circuit.replace(".", "") + ")";
            return name;
        }
        if (NAME_MAP.get(feature) == null) {
            return feature;
        }
        return NAME_MAP.get(feature);
    }

    public @Nullable String getDescription(String feature) {
        getDescriptionMap();
        Pattern pattern = Pattern.compile("(.[0-3])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(feature);
        feature = matcher.replaceAll(".N");

        return DESCRIPTION_MAP.get(feature);
    }
}
