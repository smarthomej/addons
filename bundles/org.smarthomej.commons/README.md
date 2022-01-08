# SmartHome/J Commons

This bundle provides some helper functions for use in bindings.

### `UpdatingBaseThingHandler`, `UpdatingBaseBridgeHandler`

Updating the channel configuration (i.e. removing, adding, changing channels) of a thing-type occurs during the evolution of bindings.
Managed things (created via UI and stored in the JSON-database) are restored from the database on startup in the same way they have been stored previously while unmanaged things are newly created all the time.
This results in out-dated managed things (missing channels, no longer existing channels or wrong channel-type).
With the current core implementation this can only be fixed by deleting and re-creating the thing manually, which is very inconvenient for the user.

The`UpdatingBaseThingHandler` (`UpdatingBaseBridgeHandler`) can be used as parent class for thing handlers instead of the usual `BaseThingHandler` (`BaseBridgeHandler`).
The only difference between both classes is the behaviour during startup if a file with update instructions is present.
The file is named `<thingtype>.update`, e.g. `light.update` for a thing with the id `light`.
Only files in the `update`-folder of the bundle are used (source-tree location is `src/main/resources/update`).

The file has the format `version;action;parameter(s)`.
The version is a plain integer and only instructions with a version higher than the current thing-type version are considered.
The thing-type version is stored in a thing property named `thingTypeVersion`.
If no thing-type version is present, the thing is considered to have version `0`.

Three actions are available (see examples below):

- `ADD_CHANNEL`: Add a new channel. Parameters are `<id>,<acceptedItemType>,<channelTypeUid>[,<label>,[<description>]]`. The `acceptedItemtype` is no longer needed (or processed) but kept for backward compatibility. The item type is now retrieved from the channel type.  
- `DELETE_CHANNEL`: Delete a channel. The only parameter is `<id>`.
- `UPDATE_CHANNNEL`: Update an existing channel. Parameters are the same as for `ADD_CHANNEL`.

Example:

Starting with a thing that has two channels (`testChannel0` and `testChannel1`).
After some time the binding evolves and two modifications are needed.
`testChannel1` modified (e.g. changed the label) and a new channel `testChannel2` is added.

```
1;UPDATE_CHANNEL;testChannel1,String,testBinding:testChannelTypeNewId
1;ADD_CHANNEL;testChannel2,Switch,testBinding:testChannelTypeId,TestLabel
```

When the thing-type is changed again to remove the originally present `testChannel0`, the update instructions are changed.
For things already at version `1`, only the last instruction is processed.
Things that are still version `0` are first upgraded to version `1` and then immediately to version `2`

```
1;UPDATE_CHANNEL;testChannel1,String,testBinding:testChannelTypeNewId
1;ADD_CHANNEL;testChannel2,Switch,testBinding:testChannelTypeId,TestLabel
2;REMOVE_CHANNEL;testChannel0
```

### package `transform`

- `CascadedValueTransformation`: a value transformation for cascaded transformations (with `∩` as cascading character)
- `NoOpValueTransformation`: a value transformation that does nothing
- `SingleValueTransformation`: a value transformation for a single step
- `ValueTransformation`: interface for value transformation
- `ValueTransformationProvider`: interface for a value transformation provider

### package `itemvalueconverter`

- `ChannelMode`: enum defines control modes for channels (READONLY, READWRITE etc.)
- `ContentWrapper`: wrapper for a byte content and conversion to String (input to converters), `null` represents `UNDEF`
- `ItemValueConverter`: interface for the converters (used by the channel handlers)
- `ItemValueConverterChannelConfig`: base class for channel configurations that use the value converters with fields for special values

### package `itemvalueconverter.converter`

- `ColorItemConverter`
- `DimmerItemConverter`
- `ImageItemConverter`
- `NumberItemConverter`
- `PlayerItemConverter`
- `RollershutterItemConverter`
- `GenericItemConverter`: for all other item types

### package `util`

- `ColorUtil`: methods for converting HSB colors to CIE xy colors
- `ResourceUtil`: methods for handling files in the `resources` directory
