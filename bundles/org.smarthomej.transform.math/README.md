# Math Transformation

Transforms the input by applying simple math on it.

## Full Example

Example Items:

```
Number multiply "Value multiplied by [MULTIPLY(1000):%s]" { channel="xxx" }
Number add "Value added [ADD(5.1):%s]" { channel="xxx" }
```

Example in Rules:

```
transform("MULTIPLY", "1000")
transform("ADD", "5.1")
```
