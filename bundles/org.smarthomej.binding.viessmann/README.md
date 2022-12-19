# Viessmann Binding

<img src="org.smarthomej.binding.viessmann/doc/viessmann_wordmark_rgb_1_vitorange.png" width="140"/>

This binding connects Viessmann Heating via the new Viessmann API.
It provides features like the ViCare-App.

## Note / Important

You have to register your ViCare Account at the [Viessmann developer portal](https://developer.viessmann.com/) and create a Client ID.

* `name` - `i.e. openhab`
* `Google reCAPTCHA` - `off`
* `Redirect URI` - `http://localhost:8080/viessmann/authcode/` (*)

(*) If your openHAB system is running on a different port than `8080`, you have to change this in the `Redirect URI`

### Hint: 

On the Viessmann developer portal you can add more than one RedirectURI by tapping the plus sign.

## Supported Things

The binding supports the following thing types:

* `bridge` - Supports connection to the Viessmann API.
* `device` - Provides a device which is connected (Discovery)

## Discovery

Discovery is supported for all devices connected in your account.

## Binding Configuration

The `bridge` thing supports the connection to the Viessmann API.

* `apiKey` (required) The Client ID from the Viessmann developer portal 
* `user` (required) The E-Mail address which is registered for the ViCare App
* `password` (required) The password which is registered for the ViCare App
* `installationId` (optional / it will be discovered) The installation ID which belongs to your installation 
* `gatewaySerial` (optional / it will be discovered) The gateway serial which belongs to your installation
* `apiCallLimit` (default = 1450) The limit how often call the API (*) 
* `bufferApiCommands` (default = 450) The buffer for commands (*)
* `pollingInterval` (default = 0) How often the available devices should be queried in seconds (**) 
* `pollingIntervalErrors` (default = 60) How often the errors should be queried in minutes 
* `disablePolling` (default = OFF) Deactivates the polling to carry out the manual poll using an item


(*) Used to calculate refresh time in seconds.
(**) If set to 0, then the interval will be calculated by the binding.

## Thing Configuration

_All configurations are made in the UI_

## Channels

### `bridge`

| channel             | type   | RO/RW | description                                |
|---------------------|--------|-------|--------------------------------------------|
| `countApiCalls`     | Number | RO    | How often the API is called this day       |
| `errorIsActive`     | Switch | RO    | Indicates whether the error is set / unset |
| `lastErrorMessage`  | String | RO    | Last error message from the installation   |
| `runQueryOnce`      | Switch | W     | Run device query once                      |
| `runErrorQueryOnce` | Switch | W     | Run error query once                       |


### `device`

There are many different channels.
The channels are automatically generated for all available features.

## Console

In the openHAB console you can reload all channels from the specified `device`.

* `viessmann listDevices` - list all devices
* `viessmann reloadChannels <ThingUID>` - reload all channels from the device

## Breaking changes

### Version 2.3.10

All channels on `device` - thing needs to be recreated to support Units Of Measurement.
This happens automatically.

The item type of each item has to be adjusted:

| unit              | old item type | new item type         |
|-------------------|---------------|-----------------------|
| hour, minutes,... | Number        | Number:Time           |
| percent           | Number        | Number:Dimensionless  |
| temperature       | Number        | Number:Temperature    |
