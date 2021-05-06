# TCP/UDP Binding

This binding allows using TCP or UDP connection to bring external data into openHAB or execute requests on commands.

## Supported Things

Two things are available: 

- `client` for connecting to remote hosts
- `receiver` for receiving from remote hosts
 
Both can be extended with different channels.

## Thing Configuration

*Note:* Optional "no" means that you have to configure a value unless a default is provided and you are ok with that setting.

### `client`

| parameter         | optional | default | description |
|-------------------|----------|---------|-------------|
| `host`            | no       |    -    | The address of the remote host (can be an IP address or a hostname (FQDN). |
| `port`            | no       |    -    | The port on the remote host that this thing shall connect to. |
| `protocol`        | no       |    -    | protocol for this connection: `TCP` or `UDP`. |
| `refresh`         | no       |   30    | Time in seconds between two refresh calls for the channels of this thing. |
| `timeout`         | no       |  3000   | Timeout for requests in ms. |
| `bufferSize`      | no       |  2048   | The buffer size for the response data (in kB). |
| `delay`           | no       |    0    | Delay between two requests in ms (advanced parameter). |
| `encoding`        | yes      |    -    | Encoding to be used if no encoding is found in responses (advanced parameter). |  

### `receiver`

| parameter         | optional | default | description |
|-------------------|----------|---------|-------------|
| `localAddress`    | yes      |    -    | The address of the receiving network interface (default is: listen on all interfaces). |
| `port`            | no       |    -    | The port on the local machine that this thing shall listen to. |
| `protocol`        | no       |    -    | protocol for this connection: `TCP` or `UDP`. |
| `bufferSize`      | no       |  2048   | The buffer size for the response data (in kB). |
| `delay`           | no       |    0    | Delay between two requests in ms (advanced parameter). |
| `encoding`        | yes      |    -    | Encoding to be used if no encoding is found in responses (advanced parameter). |  

## Channels

Each item type has its own channel-type.
Channel-types with `receiver-`-prefix are available on `receiver` things, channel-types without prefix are available on `client` things. 
Depending on the channel-type, channels have different configuration options.

### Common parameters for channels

All `client`-channel-types (except `image`) have `stateContent`, `stateTransformation`, `commandTransformation` and `mode` parameters.
The `image` channel-type supports `stateContent` only.

All `receiver`-channel-types (except `image`) have `addressFilter` and `stateTransformation` parameters.
The `receiver-image` channel-type supports `addressFilter` only.

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `stateTransformation  ` | yes      |      -      | One or more transformation applied to received values before updating channel. |
| `commandTransformation` | yes      |      -      | One or more transformation applied to channel value before sending to a remote. |
| `stateContent`          | no       |      -      | Content for state requests. |
| `mode`                  | no       | `READWRITE` | Mode this channel is allowed to operate. `READONLY` means receive state, `WRITEONLY` means send commands. |
| `addressFilter`         | no       |     `*`     | Address filter for incoming connections. |

Transformations need to be specified in the same format as
Some channels have additional parameters.

### Value Transformations (`stateTransformation`, `commandTransformation`)

Transformations can be used if the supplied value (or the required value) is different from what openHAB internal types require.
Here are a few examples to unwrap an incoming value via `stateTransformation` from a complex response:

| Received value                                                      | Tr. Service | Transformation                            |
|---------------------------------------------------------------------|-------------|-------------------------------------------|
| `{device: {status: { temperature: 23.2 }}}`                         | JSONPATH    | `JSONPATH:$.device.status.temperature`    |
| `<device><status><temperature>23.2</temperature></status></device>` | XPath       | `XPath:/device/status/temperature/text()` |
| `THEVALUE:23.2°C`                                                   | REGEX       | `REGEX::(.*?)°`                           |

Transformations can be chained by separating them with the mathematical intersection character "∩", e.g. `JSONPATH:$.device.status∩MAP:onoff.map` would first apply the JSONPATH transformation and then apply the given MAP transformation on the result. 
Please note that the values will be discarded if one transformation fails (e.g. REGEX did not match).

The same mechanism works for commands (`commandTransformation`) for outgoing values.

### Address filter (`addressFilter`)

Channels on `receiver` things allow to add a filter for the incoming connection.
The format is `ip:port`, the `*`-wildcard can be used. 
The default is `*` (i.e. accept everything).

Examples:

- `192.168.178.2:*` accept values from the host `192.168.178.2` from every port
- `192.168.0.*:*` accepts values from every host with an address that matches `192.168.0.*` from every port
- `*:4444` accepts values from every host but only if the SENDING port matches `4444`

Usually it is a good idea to use `*` for the sending port because this usually can't be controlled.

### Additional parameters for channel-types `color`, `color-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `onValue`               | yes      |      -      | A special value that represents `ON` |
| `offValue`              | yes      |      -      | A special value that represents `OFF` |
| `increaseValue`         | yes      |      -      | A special value that represents `INCREASE` |
| `decreaseValue`         | yes      |      -      | A special value that represents `DECREASE` |
| `step`                  | no       |      1      | The amount the brightness is increased/decreased on `INCREASE`/`DECREASE` |
| `colorMode`             | no       |    RGB      | Mode for color values: `RGB` or `HSB` |

All values that are not `onValue`, `offValue`, `increaseValue`, `decreaseValue` are interpreted as color value (according to the color mode) in the format `r,g,b` or `h,s,v`.

### Additional parameters for channel-types `contact`, `contact-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `openValue`             | no       |      -      | A special value that represents `OPEN` |
| `closedValue`           | no       |      -      | A special value that represents `CLOSED` |

### Additional parameters for channel-types `dimmer`, `dimmer-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `onValue`               | yes      |      -      | A special value that represents `ON` |
| `offValue`              | yes      |      -      | A special value that represents `OFF` |
| `increaseValue`         | yes      |      -      | A special value that represents `INCREASE` |
| `decreaseValue`         | yes      |      -      | A special value that represents `DECREASE` |
| `step`                  | no       |      1      | The amount the brightness is increased/decreased on `INCREASE`/`DECREASE` |

All values that are not `onValue`, `offValue`, `increaseValue`, `decreaseValue` are interpreted as brightness 0-100% and need to be numeric only.

### Additional parameters for channel-types `number`, `number-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `unit`                  | yes      |      -      | The unit label for this channel |

`number` channels can be used for `DecimalType` or `QuantityType` values.
If a unit is given in the `unit` parameter, the binding tries to create a `QuantityType` state before updating the channel, if no unit is present, it creates a `DecimalType`.
Please note that incompatible units (e.g. `°C` for a `Number:Density` item) will fail silently, i.e. no error message is logged even if the state update fails.

### Additional parameters for channel-types `player`, `player-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `play`                  | yes      |      -      | A special value that represents `PLAY` |
| `pause`                 | yes      |      -      | A special value that represents `PAUSE` |
| `next`                  | yes      |      -      | A special value that represents `NEXT` |
| `previous`              | yes      |      -      | A special value that represents `PREVIOUS` |
| `fastforward`           | yes      |      -      | A special value that represents `FASTFORWARD` |
| `rewind`                | yes      |      -      | A special value that represents `REWIND` |

### Additional parameters for channel-types `rollershutter`, `rollershutter-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `upValue`               | yes      |      -      | A special value that represents `UP` |
| `downValue`             | yes      |      -      | A special value that represents `DOWN` |
| `stopValue`             | yes      |      -      | A special value that represents `STOP` |
| `moveValue`             | yes      |      -      | A special value that represents `MOVE` |

All values that are not `upValue`, `downValue`, `stopValue`, `moveValue` are interpreted as position 0-100% and need to be numeric only.

### Additional parameters for channel-types `switch`, `switch-receiver`

| parameter               | optional | default     | description |
|-------------------------|----------|-------------|-------------|
| `onValue`               | no       |      -      | A special value that represents `ON` |
| `offValue`              | no       |      -      | A special value that represents `OFF` |

**Note:** Special values need to be exact matches, i.e. no leading or trailing characters and comparison is case-sensitive.

## Example configurations

```xtend
Thing tcpudp:receiver:string "TCPUDP String" [ localAddress="0.0.0.0", port="17236", protocol="UDP" ] {
    Channels:
        Type receiver-string : bewgtrp "Bewegungsmelder Treppenhaus" [ addressFilter="192.168.179.41:*" ]
        Type receiver-string : fschlaf "Fenster Schlafzimmer R" [ addressFilter="192.168.179.31:*" ]
}
```

This creates a thing that 

- listens on all network interfaces on port 17236 for UDP connections
- has one channel accepting only connections from a client with the IP address 192.168.179.41 and outputs the result to a channel that can be linked to a String item
- has one channel accepting only connections from a client with the IP address 192.168.179.31 and outputs the result to a channel that can be linked to a String item
