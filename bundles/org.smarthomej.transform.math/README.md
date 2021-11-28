# Math Transformation

Transforms the input by applying simple math on it.

## Full Example

Example Items:

```
Number multiply "Value multiplied by [MULTIPLY(1000):%s]" { channel="<channelUID>" }
Number add "Value added [ADD(5.1):%s]" { channel="<channelUID>" }
Number secondsToMinutes "Time [DIVIDE(60):%s]" { channel="<channelUID>" }
Number subtracted "Value subtracted [ADD(-1):%s]" { channel="<channelUID>" }

// Usage as a Profile
Number multiply "Value multiplied by [%.1f]" { channel="<channelUID>" [profile="transform:MULTIPLY", multiplicand=1000] }
Number add "Value added [%.1f]" { channel="<channelUID>" [profile="transform:ADD", addend=5.1] }
Number secondsToMinutes "Time [%d]" { channel="<channelUID>" [profile="transform:DIVIDE, divisor=60] }
Number subtracted "Value subtracted [%.1f]" { channel="<channelUID>" [profile="transform:ADD", addend=-1] }
```

Example in Rules:

```
transform("MULTIPLY", "1000")
transform("ADD", "5.1")
transform("DIVIDE", "60")
transform("ADD", "-1")
```
