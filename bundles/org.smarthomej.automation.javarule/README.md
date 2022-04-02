# Java Rule Automation

This addon allows writing rules in Java and provides support for code-completion for most IDEs.

All paths referenced in this document are relative to the `<openhab-config-directory>/automation`.
This is `/etc/openhab/automation` on most Linux systems and `c:\openhab-3.1.0\conf\automation` or similar on Windows systems.
The addon creates all needed directories during startup.

## Version specific information

Due to limitations/bugs in the openHAB core, there are some points you should know:

- A restart is necessary after updating the Java Rule bundle.
- Changes to own classes ("Personal Libraries") are not picked up automatically. You need to restart the system or edit/save rules using one of these classes.

These issues have been fixed in openHAB versions >= 3.2.0.M2.

## Setup And Configuration

For supporting ease of development a `core-dependency.jar` is created in the `lib/java` directory. 
This includes the exported classes from `javax.measure.unit-api`, `org.openhab.core`, `org.openhab.core.automation`, `org.openhab.core.model.script`, `org.openhab.core.thing`, `org.openhab.core.persistence`, `org.ops4j.pax.logging.pax-logging-api` and `org.smarthomej.automation.javarule`.
In most cases this is sufficient and provides all core data types, items, things and core actions (including persistence). 
If you need to expose additional classes, you can add them using the `additionalBundles` configuration option.

During startup the bundle first compiles the dependency-bundle and then creates the helper libraries (see below).

If you use an IDE for development, you should add `core-dependency.jar` and `javarule-dependency.jar` to your class path.
In IntelliJ IDEA this is named `Add as Library`.

## Rule Development

All scripts need to provide a class that inherits from `org.smarthomej.automation.javarule.rules.JavaRule`.
Please note that you need to `import` this class if your rule is not residing in the same package (which is the case in the examples below).

If the script is evaluated, the `runScript` method is executed.
The default implementation of `runScript` scans all methods in the class for annotations and creates corresponding triggers, conditions and rules (see below).
In case all triggers and conditions are set by other means (e.g. the UI) and you just want to execute some code, you have to override that method and place actual code there.

### Input values

For UI defined scripts you can find information about the trigger in the `Map<String, Object> ctx` field.
Rules defined using annotations can use a `Map<String, Object> input` parameter containing e.g. the trigger information.

## Libraries

### Helper Library

The `javarule-dependency.jar` contains several classes to ease the development of rules and prevent unnecessary errors.

The `org.smarthomej.automation.javarule.Items` class contains `String` constants for all items.
It is re-generated if items are added or removed.
You can use is at all places where you would normally put the item name e.g. instead of `postUpdate("MySwitchItem", OFF);` you could use `postUpdate(Items.MySwitchItem, OFF)`.
This allows code completion (if your IDE supports it)  and reduces the risk of typos.

The `org.smarthomej.automation.javarule.Things` class contains `String` constants for all things.
It is re-generated if things are added or removed.
You can use is at all places where you would normally put the thing UID e.g. instead of `actions.get("deconz", "deconz:deconz:1234abcd");` you could use `actions.get("deconz", Things.deconz_deconz_1234abcd)`.
This allows code completion (if your IDE supports it)  and reduces the risk of typos.

Additional classes are generated for each thing action (see below).

### Personal Libraries

Re-using code is one of the great advantages of Java.
You can create custom classes by putting the corresponding `.java`-file in the `lib/java` folder.
A class named `Own` in package `foo.bar` needs to be

- placed in the correct location for the package (i.e. `lib/java/foo/bar/Own.java`) or
- have the package name in the file name (i.e. `foo.bar.Own.java`).

Not following these conventions might result in failure to start the script engine.
The classes generated from this code are also part of the `javarule-dependency.jar`.

### Additional 3rd party Libraries

You can use additional libraries (that are not available within openHAB) by putting a JAR-file in the `lib/java` folder.
They are automatically picked up and made available when compiling the rules.
You have to add them to the classpath of your IDE to allow code completion.

## The `JavaRule` Class

Rules classes must inherit from the `JavaRule` class.
This class provides some fields needed for development, as explained in the official [JSR-223 documentation](https://www.openhab.org/docs/configuration/jsr223.html).

- `items`:	Instance of java.util.Map<String, State>
- `itemRegistry`, `ir`:	Instance of org.openhab.core.items.ItemRegistry
- `things`:	Instance of org.openhab.core.thing.ThingRegistry
- `rules`:	Instance of org.openhab.core.automation.RuleRegistry
- `events`:	Used to send events, post commands, etc.
- `actions`:  Used to get and call thing actions
- `scriptExtension`: An `Object` that can be used to import further presets

The documented fields for `ON`, `CLOSED`, etc. are not present, since the corresponding core `OnOffType`, `OpenClosedType`, etc. are available.

In addition, you can use

- `scheduler`: A scheduled executor for scheduling tasks. Be careful, you have to take care on your own that scheduled tasks are cancelled when the rule is unloaded. One way to ensure this, is to add the returned `ScheduledFuture` to the `futures` set.
- `futures`: A `Map<String, ScheduledFuture<?>>` of String/future-pairs that will be cancelled when the script is unloaded. The keys have to be unique and can be used to reference the future within the script. 
    
Besides the `runScript` method above, there are two methods that can be overridden:

- `scriptLoaded(String scriptIdentifier)`: Called when the script is loaded. This can be used for initialization. Make sure to call `super.scriptLoaded`.
- `scriptUnloaded`: Called when the script is unloaded. This can be used for clean-up. The default implementation cancels all `futures`. Make sure you do this yourself or call `super.scriptUnloaded`.

Methods/fields not mentioned in this documentation are for internal use only and should not be used in rules.
This currently applies to the method name `run`.

## Annotations

Annotations are used to mark special methods as rules in stand-alone files.
Rules, triggers and conditions are defined by annotation methods in the class inherited from `JavaRule`.
All triggers and some conditions (see below) can be repeated.

### `@Rule`

Each individual rule has to be annotated with the `@Rule` annotation.

There are two optional parameters: `name` and `disabled`.

The `name` is used for some logging, if it is not present or empty, the method name is used.
You should use unique rule names, but this is not enforced by the addon.

The disabled parameter enables or disables a rule.
If set to `true`, the method will be ignored (similar to methods without `@Rule` annotation).
The default value is `false`.

### Event Based Triggers

- `@ChannelEventTrigger`: triggers when the channel with the `channelUid` receives an event.
- `@ItemCommandTrigger`: triggers when the item `itemName` receives a command, if the optional `command` parameter is defined set, only if the received command matches the configured one.
- `@ItemStateChangeTrigger`: triggers when the item `itemName` changes its state from the (optional) `oldState` to the (optional) `state`.
- `@ItemStateUpdateTrigger`: triggers when the item `itemName` receives an update, if the optional `state` parameter is defined set, only if the received update matches the configured one.
- `@GroupCommandTrigger`: triggers when an item in group `groupName` receives a command, if the optional `command` parameter is defined set, only if the received command matches the configured one.
- `@GroupStateChangeTrigger`: triggers when an item in group `groupName` changes it state from the (optional) `oldState` to the (optional) `state`.
- `@GroupStateUpdateTrigger`: triggers when an item in group `groupName` receives an update, if the optional `state` parameter is defined set, only if the received update matches the configured one.
- `@ThingStatusChangeTrigger`: triggers when the thing `thingUID` changes its state from (optional) `previousStatus` to the (optional) `status`. 
- `@ThingStatusUpdateTrigger`: triggers every time a binding updates the status of `thingUID` to the (optional) `status`.

### Time Based Triggers

- `@GenericCronTrigger`: triggers the rule according to the `cronExpression` parameter.
- `@TimeOfDayTrigger`: triggers the rule at the given `time`.

### Other Triggers

- `@GenericAutomationTrigger`: triggers on a custom trigger with uid `typeUid`.
Custom triggers are provided by non-core automation modules (like the PID Controller)
Trigger module configuration can be added with the `params` parameter as an array of "key=value" entries.
- `@GenericEventTrigger`: triggers on a custom event defined by the `eventTopic`, `eventSource` and `eventTypes` parameters. Wildcards are allowed.
- `@SystemStartLevelTrigger`: triggers when the system reaches the configured `startLevel`.
- `@ScriptLoadedTrigger`: triggers when the script containing the rule is loaded. Rules with parameter are invoked with a `null` value. In case other parameter values are needed, you can override the `scriptLoaded` and call the rule method manually.

### Time Based Conditions

- `@DayOfWeekCondition`: allows the rule execution only if the current day is listed in the `days` array. Allowed values are `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT` and `SUN`.
- `@EphemerisDaysetCondition`: allows the rule execution only if the current day is a member of the ephemeris day set `dayset`.
- `@EphemerisHolidayCondition`, `@EphemerisNotHolidayCondition`: allows the rule execution only if the current day is a holiday/not a holiday.
- `@EphemerisWeekdayCondition`, `@EphemerisWeekendCondition`: allows the rule execution only if the current day is a weekday/not a weekday.
- `@TimeOfDayCondition`: allows the rule execution only if the current time is between `startTime` and `endTime`. If `startTime` or `endTime` is not configured, start or end of the day is used.

### Other Conditions

- `@GenericCompareCondition`: allows the rule execution only if the condition "`input`.`inputproperty` `operator` `right`" is true. Allowed operators are `eq` (equal), `lt` (less than), `lte` (less than or equal), `gt` (greater), `gte` (greater or equal), `matches`).
- `@ItemStateCondition`: allows the rule execution only if the condition "`itemName` `operator` `state`" is true. Allowed operators are `=`, `!=`, `<`, `<=`, `>`, `>=`.

The `@GenericCompareCondition` and `@ItemStateCondition` can be repeated.
In that case, all conditions need to be true.

## Actions

### Core Actions

All core actions (`Log`, `Voice`, `Exec`, `HTTP`, `Things`, `Audio`) are available with their names.
They can be used like `Log.logInfo("Test topic", "My log message");`

### Binding/Thing Actions

Thing actions can be used similar to the way described in the JSR-223 documentation.
A field `actions` is available in the `JavaRule` class.
The only available method in that class is `get(String scope, String thingUid)`.
The returned value is `null` (if either the scope is unknown, the thing not present or no actions are defined for that thing in the given scope).
Non-`null` values are of type `Object` and need a type-cast before usage.
Calling the `permitJoin` action on Deconz bridge things would look like 

```
BridgeActions bridgeAction = (BridgeActions) actions.get("deconz", "deconz:deconz:00212E040ED9");
if (bridgeAction != null) {
    bridgeAction.permitJoin(10);
}
```

If you don't know the correct action class, look into the `javarule-dependency.jar`.

## Sharing variables between rules

The easiest way to achieve this is to put all rules in the same `.java` file and declare a field in the class.

Another option to create a class in the proper folder under `lib/java` (see above).
A `public static` member of that class is then accessible as import in other rules.
But take care: the value of the variables are not persisted, if the libraries (and subsequently the rules) reload, the value is reset.

```java
package org.smarthomej.automation.javarule;

public class Shared {
    public static int COUNTER;
} 
```

```java
package org.smarthomej.automation.javarule;

import static org.smarthomej.automation.javarule.Shared.COUNTER;
import org.smarthomej.automation.javarule.annotation.Rule;

public class TestRule extends JavaRule {
    
    @Rule
    public void counter() {
        COUNTER++;
        // do something else
    }
}
```

## Examples

### The "hello world" rule

The following `TestRule.java` logs the "Hello World!" message every minute at INFO level using the core `Log` action.

```java
package org.smarthomej.automation.javarule;

import org.openhab.core.model.script.actions.Log;
import org.smarthomej.automation.javarule.annotation.GenericCronTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;

public class TestRule extends JavaRule {

    @Rule(name = "Hello World") 
    @GenericCronTrigger(cronExpression = "0 0/1 * * * *") 
    public void helloWorld() {
        Log.logInfo("Test Rule", "Hello World!");
    }

}
```

### Using the voice action as doorbell 

Announce via the core voice action that the state of the `DoorBellSwitch` item's state changed to `ON`.
The condition ensures that the kids will not wake up, because the rule only triggers between 07:00 in the morning and 22:00 in the evening.

```java
package org.smarthomej.automation.javarule;

import org.openhab.core.model.script.actions.Voice;
import org.smarthomej.automation.javarule.Items;
import org.smarthomej.automation.javarule.annotation.ItemStateUpdateTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;
import org.smarthomej.automation.javarule.annotation.TimeOfDayCondition;

public class DoorBell extends JavaRule {

    @Rule(name = "Ring the bell") 
    @ItemStateUpdateTrigger(itemName = Items.DoorBellSwitch, state = "ON") 
    @TimeOfDayCondition(startTime = "07:00", endTime = "22:00") 
    public void doorbell() {
        Voice.say("Attention, someone rang the bell!");
    }

}
```

### Send a value if rule triggers on item 

If the item `Presence` changes to `ON` (someone is in the house) the heating set point is set to 20 °C, if the item changes to `OFF` (everybody left), it is reduced to 17 °C to save energy.
A warning is logged if an unexpected value is received.

```java
package org.smarthomej.automation.javarule;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.model.script.actions.Log;
import org.openhab.core.types.State;
import org.smarthomej.automation.javarule.Items;
import org.smarthomej.automation.javarule.annotation.ItemStateChangeTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;

import java.util.Map;

public class Thermostat extends JavaRule {

    @Rule(name = "Standby Switch") 
    @ItemStateChangeTrigger(itemName = Items.Presence) 
    public void standby(Map<String, Object> input) {
        State newState = (State) input.get("newState");
        if (OnOffType.ON.equals(newState)) {
            sendCommand(Items.HeatingSetpoint, new QuantityType<>("20°C"));
        } else if (OnOffType.OFF.equals(newState)) {
            sendCommand(Items.HeatingSetpoint, new QuantityType<>("17°C"));
        } else {
            Log.logWarn("Thermostat", "unknown new state {}", newState);
        }
    }

}
```

### Adjust Set Point Value

If the item `SetpointButton` receives a command, the value of the `HeaterSetpoint` item is increased (`ON`) or decreased (`OFF`) by 1 °C.

```java
package org.smarthomej.automation.javarule;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.smarthomej.automation.javarule.Items;
import org.smarthomej.automation.javarule.annotation.ItemCommandTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;

import javax.measure.quantity.Temperature;

public class Thermostat extends JavaRule {

    @Rule(name = "Adjust Heater Set Point") 
    @ItemCommandTrigger(itemName = Items.SetpointButton) 
    public void adjustSetPoint() {
        QuantityType<Temperature> oldState = items.get(Items.HeaterSetpoint).as(QuantityType.class);
        QuantityType<Temperature> step = OnOffType.ON.equals(input.get("state")) ? new QuantityType<>("1 °C") : new QuantityType<>("-1 °C");
        events.postUpdate(Items.HeaterSetpoint, oldState.add(step));
    }
}
```

### Heating PID controller

This requires the `PID Controller` automation addon.
For the description of the parameters see the addon documentation.
The rule itself coerces the command from the trigger to the allowed range 0-255 (depends on the allowed values for valve control) and sends it to the valve controlling item.

```java
package org.smarthomej.automation.javarule;

import org.smarthomej.automation.javarule.annotation.GenericAutomationTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;

public class HeatingController extends JavaRule {
    
    @Rule
    @GenericAutomationTrigger(type="pidcontroller.trigger",
            params = {"input=" + Items.LivingRoomTemperature, "setpoint=" + Items.LivingRoomSetpoint, 
                    "loopTime=300000", "kp=32", "ki=2.67", "kd=0"})
    public void livingRoom(Map<String, ?> input) {
        // get the value from the trigger
        int control = ((DecimalType) input.get("command")).intValue();
        // coerce to range 0 - 255
        control = Math.max(0, Math.min(control, 255));
        events.sendCommand(Items.LivingRoomValve, new Decimaltype(control));
    }
}
```

### Sleep timer

This realizes a slowly fading light.
A press on button initiates a trigger which executes the rule itself.
If the sleep timer is already running, the trigger is ignored.
The current state of the `Night_Light` is divided by 20 to get 20 equal steps.
A task is scheduled with a delay of 30s.

The task takes the current light state, decreases the brightness by the calculated step and sends a command to the light.
If the brightness is still greater than 0, the task is re-scheduled.
If the light is off, the future is removed from the task list.

If the rule is removed or reloaded, the task is removed automatically because all tasks in the `futures` map are cancelled.

```java
package org.smarthomej.automation.javarule;

import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.script.actions.Log;
import org.smarthomej.automation.javarule.annotation.ChannelEventTrigger;
import org.smarthomej.automation.javarule.annotation.Rule;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Bedroom extends JavaRule {
    private static final String LOGGER_NAME = Bedroom.class.getName();

    private static final String NIGHT_LIGHT_FUTURE = "dimDown";
    private static final int NIGHT_LIGHT_DELAY = 30;
    private double lightStateStep = 0.0;

    private void dimDown() {
        double lightState = Objects.requireNonNullElse((PercentType) items.get(Items.Schlafzimmer_Lampe_Jan),
                PercentType.ZERO).doubleValue() - lightStateJanStep;

        if (lightState < 0.0) {
            lightState = 0.0;
        }
        events.sendCommand(Items.Night_Light, new PercentType((int) lightState));

        if (lightState > 0) {
            futures.put(NIGHT_LIGHT_FUTURE, scheduler.schedule(this::dimDown, NIGHT_LIGHT_DELAY, TimeUnit.SECONDS));
        } else {
            futures.remove(NIGHT_LIGHT_FUTURE);
        }
    }

    @Rule
    @ChannelEventTrigger(channelUID = "deconz:switch:cee86e78:000b57fffec72211000000:buttonevent", event = "1002")
    public void nightLight() {
        double lightState = Objects.requireNonNullElse((PercentType) items.get(Items.Night_Light), PercentType.ZERO)
                .doubleValue();

        lightStateStep = lightState / 20.0;

        if (futures.containsKey(NIGHT_LIGHT_FUTURE)) {
            Log.logDebug(LOGGER_NAME, "Trigger ignored");
        } else {
            futures.put(NIGHT_LIGHT_FUTURE, scheduler.schedule(this::dimDown, NIGHT_LIGHT_DELAY, TimeUnit.SECONDS));
        }

    }
}
```
