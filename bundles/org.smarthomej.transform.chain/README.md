# Chain Transformation Profile Service

The `CHAIN` profile allows chaining multiple transformations.

## Configuration

The profile has three configuration parameters:

- `toItem`: the chain defined in this parameter is used for values that are send from the channel to the item.
- `toChannel`: the chain defined in this parameter is used for values that are send from the item to the channel.
- `undefOnError`: if one transformation does not return a value and this parameter is set to `true`, `UNDEF` is returned. If the parameter is set to `false` (default), the update/command is ignored. 

Empty chains result in pass-through of the original value.
Individual transformations are chained with the mathematical intersection character (`∩`).

## Example: Door lock

A lock that can report its status and accepts commands as JSON and accepts commands.
It shall be represented as a `Switch` item on openHAB.

### Incoming values (`toItem`)

The received value for the status is `{ "device" : { "status" : { "lock" : "locked" }}}` or `{ "device" : { "status" : { "lock" : "unlocked" }}}`.
The transformation needs to first extract `locked` or `unlocked` and then map those values to `ON` or `OFF`.

The first part is done by using the `JSONPATH` transformation: `$.device.status.lock`.

The second part is done by using the `MAP` transformation using a file `doorlock.map`

```
locked=ON
unlocked=OFF
```

So the full `toItem` transformation is 

```
JSONPATH:$.device.status.temperature∩MAP:doorlock.map
```

### Outgoing values (`toChannel`)

For setting the lock's state we need to produce another JSON: `{"device" : { "command" : "lock" }}` or `{ "device" : { "command" : "unlock" }}`.
The transformation needs to do the inverse of the above: first map `ON` or `OFF` to `lock` or `unlock` and then insert that into a JSON-template.

For the first part we again use the `MAP` transformation and extend the map from above:

```
locked=ON
unlocked=OFF
ON=lock
OFF=unlock
```

For the second part we use the `FORMAT` transformation with a `doorlock.format`:

```
{ "device" : { "command" : "%1$s" }}
```

The full`toChannel` transformation is 

```
MAP:doorlock.map∩FORMAT:doorlock.format
```

Sidenote: Of course, it would have been easier to just use the `MAP` transformation and enter the full JSON string as target value.
