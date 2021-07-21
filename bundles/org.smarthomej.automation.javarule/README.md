# Java Rule Automation

This addon allows writing rules in Java and provides support for code-completion for most IDEs.

All paths referenced in this document are relative to the `<openhab-config-directory>/automation/javarule`.
This is `/etc/openhab/automation/javarule` on most Linux systems and `c:\openhab-3.1.0\conf\automation\javarule` or similar on Windows systems.
The addon creates all needed directories during startup.

## Setup And Configuration

For supporting ease of development a `dependency.jar` is created in the `helper` directory. 
This includes the exported classes from `javax.measure.unit-api`, `org.openhab.core`,
`org.openhab.core.model.script`, `org.openhab.core.thing`, `org.openhab.core.persistence`and `org.smarthomej.automation.javarule`.
In most cases this is sufficient and provides all core data types, items, things and core actions (including persistence). 
If you need to expose additional classes, you can add them using the `additionalBundles` configuration option.

During startup the bundle first compiles the dependency-bundle and then creates the helper libraries (see below).
Since the helper libraries are required before compiling rules, a delay is recommended before the rules start running.
This can be configured with the `initDelay` parameter.
The default value is 5s.

If you use an IDE for development, you should add `dependency.jar` and `helper.jar` to your class path.
In IntelliJ IDEA this is named `Add as Library`.

## Rule Development

Rules are annotated methods in classes in the `org.smarthomej.automation.javarule.rules.user` package.
You can also create sub-packages below that.
The `.java` files have to be placed in the rules directory, including the package: `rules/org/smarthomej/automation/javarule/rules/user`-
The directory is automatically created on startup.

## Libraries

### Helper Library

The `helper.jar` contains several classes to ease the development of rules and prevent unnecessary erros.

The `org.smarthomej.automation.javarule.helper.Items` class contains `String` constants for all items.
It is re-generated if items are added or removed.
You can use is at all places where you would normally put the item name e.g. instead of `postUpdate("MySwitchItem", OFF);` you could use `postUpdate(Items.MySwitchItem, OFF)`.
This allows code completion (if your IDE supports it)  and reduces the risk of typos.

Additional classes are generated for each thing action (see below).

### Additional Libraries

You can use additional libraries (that are not available within openHAB) by putting a JAR-file in the `ext-lib` folder.
They are automatically picked up and made available when compiling the rules.
They are NOT included in the `dependency.jar`, you have to add them to the classpath of your IDE to allow code completion.

## The `JavaRule` Class

Rules classes must inherit from the `JavaRule` class.
This class provides some fields and methods needed for development (naming is the same as for JSR-223 scripting):

- `itemRegistry` - The item registry of the system
- `things` - The thing registry
- `scheduler` - A `ScheduledExecutorService` that can be used for scheduling events

- `postUpdate(String itemName, State state)` and `postUpdate(Item item, State state)` - post a state update to the given item
- `sendCommand(String itemName, Command command)` and `sendCommand(Item item, Command command)` - send a command to the given item

Methods/fields not mentioned in this documentation are for internal use only and should not be used in rules.

## Annotations

Rules, triggers and conditions are defined by annotation methods in the class inherited from `JavaRule`.
All triggers and some conditions (see below) can be repeated.

### `@Rule`

Each individual rules has to be annotated with the `@Rule` annotation.

There is one mandatory parameter `name` which is used for logging purposes.
You should use unique rule names, but this is not enforced by the addon.

An optional parameter is `disabled`.
If set to `true`, the method will be ignored (similar to methods without `@Rule` annotation).
The default value is `false`.

### Event Based Triggers

- `@ChannelEventTrigger`: triggers when the channel with the `channelUid` receives an event.
- `@ItemCommandTrigger`: triggers when the item `itemName` receives a command, if the optional `command` parameter is defined set, only if the received command matches the configured one.
- `@ItemStateChangeTrigger`: triggers when the item `itemName` changes it state from the (optional) `oldState` to the (optiona) `state`.
- `@ItemStateUpdateTrigger`: triggers when the item `itemName` receives an update, if the optional `state` parameter is defined set, only if the received update matches the configured one.
- `@GroupCommandTrigger`: triggers when an item in group `groupName` receives a command, if the optional `command` parameter is defined set, only if the received command matches the configured one.
- `@GroupStateChangeTrigger`: triggers when an item in group `groupName` changes it state from the (optional) `oldState` to the (optiona) `state`.
- `@GroupStateUpdateTrigger`: triggers when an item in group `groupName` receives an update, if the optional `state` parameter is defined set, only if the received update matches the configured one.
- `@ThingStatusChangeTrigger`: triggers when the thing `thingUID` changes its state from (optional) `previousStatus` to the (optional) `status`. 
- `@ThingStatusUpdateTrigger`: triggers every time a binding updates the status of `thingUID` to the (optional) `status`.

### Time Based Triggers

- `@GenericCronTrigger`: triggers the rule according to the `cronExpression` parameter.
- `@TimeOfDayTrigger`: triggers the rule at the given `time`.

### Other Triggers

- `@GenericEventTrigger`: triggers on a custom event defined by the `eventTopic`, `eventSource` and `eventTypes` parameters. Wildcards are allowed.
- `@SystemStartLevelTrigger`: triggers when the system reaches the configured `startLevel`.

### Time Based Conditions

- `@DayOfWeekCondition`: allows the rule execution only if the current day is listed in the `days` array. Allowed values are `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT` and `SUN`.
- `@EphemerisDaysetCondition`: allows the rule execution only if the current day is a member of the ephemeris dayset `dayset`.
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

Each binding defines a scope for their thing actions.
The corresponding class in the helper livrary is named like the `scope` in upper-case letters.
See the see binding documentation for the correct value of `scope` and available actions.
E.g. the class for the deconz binding is names `DECONZ` and can be used like `DECONZ.BridgeActions.get("deconz:deconz:myBridgeId").permitJoin(5);`.

## Examples

### The "hello world" rule

The following `TestRule.java` logs the "Hello World!" message every minute at INFO level using the core `Log` action. 

```java
package org.smarthomej.automation.javarule.rules.user;

import org.openhab.core.model.script.actions.Log;
import org.smarthomej.automation.javarule.rules.JavaRule;
import org.smarthomej.automation.javarule.rules.annotation.GenericCronTrigger;
import org.smarthomej.automation.javarule.rules.annotation.Rule;

public class TestRule extends JavaRule {

    @Rule(name="Hello World")
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
package org.smarthomej.automation.javarule.rules.user;

import org.openhab.core.model.script.actions.Voice;
import org.smarthomej.automation.javarule.helper.Items;
import org.smarthomej.automation.javarule.rules.JavaRule;
import org.smarthomej.automation.javarule.rules.annotation.ItemStateUpdateTrigger;
import org.smarthomej.automation.javarule.rules.annotation.Rule;
import org.smarthomej.automation.javarule.rules.annotation.TimeOfDayCondition;

public class DoorBell extends JavaRule {

    @Rule(name="Ring the bell")
    @ItemStateUpdateTrigger(itemName = Items.DoorBellSwitch, state = "ON")
    @TimeOfDayCondition(startTime = "07:00", endTime = "22:00")
    public void doorbell() {
        Voice.say("Attention, someone rang the bell!");
    }

}
```

### Send a value if rule triggers on item 

If the item `Presence` changes to `ON` (someone is in the house) the heating setpoint is set to 20 °C, if the item changes to `OFF` (everybody left), it is reduced to 17 °C to save energy.
A warning is logged if an unexpected value is received. 

```java
package org.smarthomej.automation.javarule.rules.user;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.model.script.actions.Log;
import org.openhab.core.types.State;
import org.smarthomej.automation.javarule.helper.Items;
import org.smarthomej.automation.javarule.rules.JavaRule;
import org.smarthomej.automation.javarule.rules.annotation.ItemStateChangeTrigger;
import org.smarthomej.automation.javarule.rules.annotation.Rule;

import java.util.Map;

public class Thermostat extends JavaRule {

    @Rule(name="Standby Switch")
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

### Timer
