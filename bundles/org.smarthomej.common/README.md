# SmartHome/J Commons

This bundle provides some helper functions for use in bindings.

### package `transform`

- `CascadedValueTransformation`: a value transformation for cascaded transformations (with `∩` as cascading character)
- `NoOpValueTransformation`: a value transformation that does nothing
- `SingleValueTransformation`: a value transformation for a single step
- `ValueTransformation`: interface for value transformation
- `ValueTransformationProvider`: interface for a value transformation provider

### package `itemvalueconverter`

- `ChannelMode`: enum defines control modes for channels (READONLY, READWRITE etc.)
- `ContentWrapper`: wrapper for a byte content and conversion to String (input to converters)
- `ItemValueConverter`: interface for the converters (used by the channel handlers)
- `ItemValueConverterChannelConfig`: base class for channel configurations that use the value converters with fields for special values

### package `itemvalueconverter.converter`

- `ColorItemConverter`
- `DimmerItemConverter`
- `ImageItemConverter`
- `NumberItemConverter`
- `PlayerItemConverter`
- `RollershutterItemConverter`

- `GenericItemConverter` for all other item types