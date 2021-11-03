# Viessmann Binding

_This binding connects Viessmann Heatings via the new Viessmann API._

_It provides features like the ViCare-App._

## Note / Important

You have to register your ViCare Account at the Viessmann developer portal and create a API Key (Client ID).

* `name` - `i.e. openhab`
* `Google reCAPTCHA` - `off`
* `Redicect URI` - `http://localhost:8080/viessmann-api/authcode/`

## Supported Things

The binding supports the following thing types:

* `bridge` - Supports connection to the Viesmmann API.
* `device` - Provides a device which is connected (Discovery)

## Discovery

_It discover all devices wich are connected through your account._

## Binding Configuration

The `bridge` thing supports the connection to the Viessmann API.

* `apiKey` (required) The API Key (Client ID) form the Viessman developer portal 
* `user` (required) The E-Mail address which is registered for the ViCare App
* `password` (required) The password which is registered for the ViCare App
* `installationId` (optional / it will be discovered) The installation Id which belongs to your installation 
* `gatewaySerial` (optional / it will be discovered) The gateway serial which belongs to your installation
* `apiTimeout` (default = 22) Time in seconds to allow API request to complete
* `apiCallLimit` (default = 1450) The limit how often call the API (*) 
* `bufferApiCommands` (default = 450) The buffer for commands (*)

(*) It is used for calcutating the time how often the data is queried in seconds.

## Thing Configuration

_All configurations are made in the UI_

## Channels

### `bridge`

| channel         | type   | RO/RW | description                          |
|-----------------|--------|-------|--------------------------------------|
| `countApiCalls` | Number |   RO  | How often the API is called this day |

### `device`

_There are many different channels. The channels are automaticaly generaten for all available fetaures_

## Full Example

_TODO_
