# Viessmann Binding

<img src="/bundles/org.smarthomej.binding.viessmann/doc/viessmann_wordmark_rgb_1_vitorange.png" width="100"/>

This binding connects Viessmann Heatings via the new Viessmann API.
It provides features like the ViCare-App.

## Note / Important

You have to register your ViCare Account at the Viessmann developer portal and create a Client ID.

* `name` - `i.e. openhab`
* `Google reCAPTCHA` - `off`
* `Redicect URI` - `http://localhost:8080/viessmann/authcode/`

### Hint: 

On the Viessman developer portal you can add more than one RedirectURI by tapping the plus sign.

## Supported Things

The binding supports the following thing types:

* `bridge` - Supports connection to the Viesmmann API.
* `device` - Provides a device which is connected (Discovery)

## Discovery

Discovery is supported for all devices connected in your account.

## Binding Configuration

The `bridge` thing supports the connection to the Viessmann API.

* `apiKey` (required) The Client ID from the Viessman developer portal 
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

## Breaking changes

### Version 2.3.10

All channels on `device` - thing needs to be recreated to support Units Of Measurement.
Simply delete the thing and rediscover.
The channel links do not need to be deleted.
But it could be that some have to be relinked.
Or the item type has to be adjusted. 
(e.g. Number -> Number:Time)
