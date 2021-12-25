# Tuya Binding

This addon connects Tuya WiFi devices with openHAB or compatible systems.
The control and status reporting is done on the local network.
Cloud access is only needed for discovery and initial connection.

Devices need to be connected to a Tuya account (Tuya App or SmartLife App).
Each device has a unique "local key" (password/secret) which needs to be added during thing creation.
It is highly recommended to use the discovery feature for that but you can also sniff the local key with a MITM proxy during pairing.

## Supported Things

There are two things: `project` and `tuyadevice`.

The `project` thing represents a Tuya developer portal cloud project (see below).
`project` things must be configured manually and are needed for discovery only.

`tuyadevice` things represent a single device.
They can be configured manually or by discovery.

## Discovery

Discovery is supported for `tuyadevice` things.
By using discovery all necessary setting of the device are retrieved from your cloud account.

## Thing Configuration

### `project`

First create and link a Tuya Develop Account:

- Go to `iot.tuya.com` (the Tuya developer portal) and create an account. 
You can choose any credentials (email/password) you like (it is not necessary that they are the same as in the app). 
After confirming your account, log in to your new account. 
- On the left navigation bar, select "Cloud", then "Create new Cloud project" (upper right corner).
Enter a name (e.g. "My Smarthome"), select "Smart Home" for "Industry" and "Development Method".
For security reasons, select only the "Data Center" that your app is connected to (you can change that later if you select the wrong one). 
Select "IoT Core", "Authorization" and "Device Status Notification" as APIs.
- You should be redirected to the "Overview" tab of your project. 
Write down (or copy) "Access ID/Client ID" and "Access Secret/Client Secret" (ypu can always look it up in your account).
- In the upper menu bar, select the "Devices" tab, then go to "Link Tuya App Account" and link you App account.

Add a `project` and enter your apps credentials (`username`/`password`) and the cloud credentials (`accessId`/`accessSecret`).
The `countryCode` is the international dial prefix of the country you registered your app in (e.g. `49` for Germany or `43` for Austria).
Depending on the app you use, set `schema` to `tuyaSmart` (for the Tuya Smart app) or `smartLife` (for the Smart Life app).

The thing should come online immediately.

If the thing does not come online, check 

- if you entered the correct country code (check in the App if you accidentally choose a wrong country)
- check if you selected the correct "Data Center" in your cloud project (you can select more than one for testing).

### `tuyaDevice`

The best way to configure a `tuyaDevice` is using the discovery service.

The mandatory parameters are `deviceId`, `productId` and `localKey`.
The `deviceId` is used to identify the device, the `productId` identifies the type of the device and the `localKey` is a kind of password for access control.
These parameters are set during discovery.
If you want to manually configure the device, you can also read those values from the cloud project above.

For line powered device on the same subnet `ip` address and `protocol` version are automatically detected.
Tuya devices announce their presence via UDP broadcast packets, which is usually not available in other subnets.
Battery powered devices do not announce their presence at all.
There is no clear rule how to determine if a device has protocol 3.3 or 3.1.
It is recommended to start with 3.3 and watch the log file if it that works and use 3.1 otherwise.

*Note:* Support for protocol 3.1 is considered experimental.

## Channels

For auto-discovered devices the schema of the device is requested from the cloud.
Based on that channels are added to the thing on first startup.

In case you manually configured the device (or no schema was retrieved during discovery), a database is used to lookup the correct schema.
The device will change to OFFLINE status if this is not successful.

Channels can also be added manually.
The available channel-types are `color`, `dimmer`, `number`, `string` and  `switch`.
Depending on the channel one or more parameters are available.
If a schema is available (which should be the case in most setups), these parameters are auto-configured.

All channels have at least the `dp` parameter which is used to identify the channel when communication with the device.

### Type `color`

The `color` channel has a second (optional) parameter `dp2`.
This parameter identifies the ON/OFF switch that is usually available on color lights.

### Type `dimmer`

The `dimmer` channel has two additional mandatory (`min` and `max`) and one (optional) parameter `dp2`.
The `min` and `max` parameters define the range allowed for controlling the brightness (most common are 0-255 or 10-1000).
The `dp2`parameter identifies the ON/OFF switch that is usually available on dimmable lights.

### Type `number`

The `number` channel has two additional mandatory (`min` and `max`) parameters.
The `min` and `max` parameters define the range allowed (e.g. 0-86400 for turn-off "countdown").

### Type `string`

The `string` channel has one additional (optional) parameter `range`.
It contains a comma-separated list of command options for this channel (e.g. `white,colour,scene,music` for the "workMode" channel).

## Troubleshooting

- Check if there are errors in the log and if you see messages like `Configuring IP address '192.168.1.100' for thing 'tuya:tuya:tuyaDevice:bf3122fba012345fc9pqa'`.
If this is missing, try configuring the IP manually.
The MAC of your device can be found in the auto-discovered thing properties (this helps to identify the device in your router).
- Provide TRACE level logs.
Type `log:set TRACE org.smarthomej.binding.tuya` on the Karaf console to enable TRACE logging.
Use `log:tail` to display the log.
You can revert to normal logging with `log:set DEFAULT org.smarthomej.binding.tuya`
- At least disable/enable the thing when providing logs.
For most details better remove the device, use discovery and re-add the device. 
Please use PasteBin or a similar service, do not use JPG or other images, they can't be analysed properly.
Check that the log doesn't contain any credentials. 
- Add the thing configuration to your report (in the UI use the "Code" view).


