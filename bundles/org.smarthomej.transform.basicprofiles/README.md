# Basic Profiles

This bundle provides a list of useful Profiles.

## Invert / Negate Profile

The Invert / Negate Profile inverts or negates a Command / State.
It requires no specific configuration.

The values of `QuantityType`, `PercentType` and `DecimalTypes` are negated (multiplied by `-1`).
Otherwise the following mapping is used:
 
`IncreaseDecreaseType`: `INCREASE` <-> `DECREASE`
`NextPreviousType`: `NEXT` <-> `PREVIOUS`
`OnOffType`: `ON` <-> `OFF`
`OpenClosedType`: `OPEN` <-> `CLOSED`
`PlayPauseType`: `PLAY` <-> `PAUSE`
`RewindFastforwardType`: `REWIND` <-> `FASTFORWARD`
`StopMoveType`: `MOVE` <-> `STOP`
`UpDownType`: `UP` <-> `DOWN`

### Full Example

```java
Switch invertedSwitch { channel="xxx" [profile="basic-profiles:invert"] }
```

## Round Profile

The Round Profile scales the state to a specific number of decimal places.
Optionally the [Rounding mode](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/math/RoundingMode.html) can be set.
Source Channels should accept Item Type `Number`.

### Configuration

| Configuration Parameter | Type    | Description                                                                                                     |
|-------------------------|---------|-----------------------------------------------------------------------------------------------------------------|
| `scale`                 | integer | Scale (non-negative number) to indicate the resulting number of decimal places (min: 0, max: 16) **mandatory**. |
| `mode`                  | text    | Rounding mode to be used (e.g. "UP", "DOWN", "CEILING", "FLOOR", "HALF_UP" or "HALF_DOWN" (default: "HALF_UP"). |

### Full Example

```java
Number roundedNumber { channel="xxx" [profile="basic-profiles:round", scale=1] }
```

## Threshold Profile

The Threshold Profile triggers `ON` or `OFF` behavior when being linked to a Switch item if value is below a given threshold (default: 10).
Source Channels should accept Item Type `Dimmer` or `Number`.

### Configuration

| Configuration Parameter | Type    | Description                                                                                         |
|-------------------------|---------|-----------------------------------------------------------------------------------------------------|
| `threshold`             | integer | Triggers `ON` if value is below the given threshold, otherwise OFF (default: 10, min: 0, max: 100). |

### Full Example

```java
Switch thresholdLowItem { channel="xxx" [profile="basic-profiles:threshold", threshold=15] }
```
