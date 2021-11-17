# Viessmann Binding

This binding connects Viessmann Heatings via the new Viessmann API.
It provides features like the ViCare-App.

## Note / Important

You have to register your ViCare Account at the Viessmann developer portal and create an API Key (Client ID).

* `name` - `i.e. openhab`
* `Google reCAPTCHA` - `off`
* `Redicect URI` - `http://localhost:8080/viessmann/authcode/`

## Supported Things

The binding supports the following thing types:

* `bridge` - Supports connection to the Viesmmann API.
* `device` - Provides a device which is connected (Discovery)

## Discovery

Discovery is supported for all devices connected in your account.

## Binding Configuration

The `bridge` thing supports the connection to the Viessmann API.

* `apiKey` (required) The API Key (Client ID) form the Viessman developer portal 
* `user` (required) The E-Mail address which is registered for the ViCare App
* `password` (required) The password which is registered for the ViCare App
* `installationId` (optional / it will be discovered) The installation Id which belongs to your installation 
* `gatewaySerial` (optional / it will be discovered) The gateway serial which belongs to your installation
* `apiCallLimit` (default = 1450) The limit how often call the API (*) 
* `bufferApiCommands` (default = 450) The buffer for commands (*)
* `pollingInterval` (default = 0) How often the available devices should be queried in seconds (**) 


(*) Used to calcuate refresh time in seconds.
(**) If set to 0, then the interval will be calculated by the binding.

## Thing Configuration

_All configurations are made in the UI_

## Channels

### `bridge`

| channel         | type   | RO/RW | description                          |
|-----------------|--------|-------|--------------------------------------|
| `countApiCalls` | Number |   RO  | How often the API is called this day |

### `device`

There are many different channels.
The channels are automatically generated for all available features.
