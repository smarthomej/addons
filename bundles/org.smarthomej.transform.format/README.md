# Format Transformation Service

Transforms the input by using the java formatter.
It expects the format string to be read from a file which is stored under the `transform` folder.
The file name must have the `.format` extension and contain at least one occurrence of `%1$s` which is replaced by the input string. 

## Example

### transform/identity.format:

```
%1$s
```

applied to `TESTSTRING` results in

```
TESTSTRING
```

### transform/xml.format

```
<?xml version="1.0" encoding="UTF-8"?>
<surroundingTag>
    <innerTag>%1$s</innerTag>
</surroundingTag>
```

applied to `TESTSTRING` results in

```
<?xml version="1.0" encoding="UTF-8"?>
<surroundingTag>
    <innerTag>TESTSTRING</innerTag>
</surroundingTag>
```

### transform/double.format

```
%1$s - %1$s
```

applied to `TESTSTRING` results in

```
TESTSTRING - TESTSTRING
```
