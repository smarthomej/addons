# Telenot Binding

_The Telenot binding connects the Telenot Complex 400 to openHAB or compatible systems._

_It is able to read the states of the contacts and the security areas._

## Requirement

You have to enable the GMS-Protocol in the CompasX Software.

## Hardware for ipbridge

To get the serial data to the ethernet bus use the following converter or another like this: USR-TCP232-302 Tiny - RS232 to Ethernet TCP-IP-Server-Modul. 
Connect this module to the GMS-Port (Serial).

Adapter Config:

```
Baud Rate: 9600 bps
Data Size:    8 bit
Parity:        none
Stop Bits:    1 bit
Mode:    TCP Server
```

## Note

The Building management protocol is a two-way street. 
Not only can you get info from your alarm system, that same protocol can be used to send commands to the alarm system too! 
A black hatter might use this to tell your system to disarm itself using the cimmunications infrastructure that we have conveniently built for him. 
While we will not use those functions in our integration, someone that knows the right binary data to send can disable the system without setting foot into your home and entering the security code or otherwise authenticate himself to the system. 
Especially if you have a clause in your theft protection policy, that you are only covered if the alarm system is turned on, I can only strongly discourage you from enabling the building management protocol on your alarm system.
You have been warned! Take care that you protect your home installation, and make sure that you have locked down external access properly. 
At the very least, I’d recommend using a proper firewall and VPN server rather than just drilling holes into your NAT and exposing ports.

## Supported Things

The binding supports the following thing types:

* `ipbridge` - Supports TCP connection to the serial tcp adapter.
* `serialbridge` - Supports serial/USB connection to the Telenot.
* `input` - Telenot reporting group (Discovery)
* `output` - Telenot reporting area (Discovery)
* `mb` - Reports the reporting area.
* `mp` - Reports the inputs (contacts).
* `sb` - Reports the security area.
* `emaState` - Reports the state of the system.

## Discovery

You have to enable discovery in the bridge thing.
After turning on the discovery will start and the switch goes to off.
The discovery takes about 5 minutes.

* 1. Add `input` and `output` things.
* 2. Enable discovery in the bridge thing.
* 3. All available channel will be added automatically.

## Thing Configuration

### ipbridge

The `ipbridge` thing supports a TCP/IP connection to an RS323 to LAN adapter.

* `hostname` (required) The hostname or IP address of the serial to LAN adapter
* `tcpPort` (default = 4116) TCP port number for the serial to LAN adapter connection
* `discovery` Enables the discovery
* `updateClock` The period in hours for updating the clock on the Telenot system.
Set to 0 to disable.
* `reconnect` (1-60, default = 2) The period in minutes that the handler will wait between connection checks and connection attempts
* `timeout` (0-60, default = 5) The period in minutes after which the connection will be reset if no valid messages have been received. Set to 0 to disable.
* `refreshData` The period in minutes that the handler will refresh the data to eventbus.

Thing config file example:

```
Bridge telenot:ipbridge:device [ hostname="xxx.xxx.xxx.xxx", tcpPort=4116 ] {
  Thing ...
  Thing ...
}
```

### serialbridge

The `serialbridge` thing supports a serial or USB connection to a Telenot alarm system.

* `serialPort` (required) The name of the serial port used to connect to the Telenot system.
* `discovery` Enables the discovery
* `updateClock` The period in hours for updating the clock on the Telenot alarm system.
Set to 0 to disable.
* `refreshData` The period in minutes that the handler will refresh the data to eventbus.


### `input`

The `input` thing provides all channels with the state of each single reporting group.

### `output`

The `output` thing provides all channels with the state of each single reporting area.

### `mb`

The `mb` thing provides the state of each single reporting area (Meldebereich).

* `address` (required) The number of reporting area.

### `mp`

The `mp` thing provides the state of each single reporting point (Meldepunkt).

* `address` (required) The number of reporting ponit.

### `sb`

The `sb` thing provides the state of each single security area (Sicherungsbereich).

* `address` (required) The number of security area.


### `emaState`

The `emaState` thing currently provides the state of the system.

## Channels

The Telenot things expose the following channels:

### `mb`

| channel     | type    | RO/RW | description                    |
|-------------|---------|-------|--------------------------------|
| `contactMB` | Contact |   RO  | Reporting area contact state   |
| `disableMB` | Switch  |   RW  | Disable Reporting area contact |

### `mp`

| channel     | type    | RO/RW | description                   |
|-------------|---------|-------|-------------------------------|
| `contact`   | Contact |  RO   | Reporting point contact state |

### `sb`

| channel                    | type     | RO/RW | description                                                |
|----------------------------|----------|-------|------------------------------------------------------------|
| `disarmed`                 | Switch   |  RO   | State for security area is disarmend                       |
| `internallyArmed`          | Switch   |  RO   | State for security area is internally armend               |
| `externallyArmed`          | Switch   |  RO   | State for security area is externally armend               |
| `alarm`                    | Switch   |  RO   | Security area alarm                                        |
| `disarm`                   | Switch   |  RW   | Disarm security area                                       |
| `internalArm`              | Switch   |  RW   | Arm internally security area                               |
| `externalArm`              | Switch   |  RW   | Arm externally security area                               |
| `resetAlarm`               | Switch   |  RW   | Reset security area alarm                                  |
| `malfunction`              | Switch   |  RO   | Security area malfunction                                  |
| `readyToArmInternally`     | Switch   |  RO   | Security area is ready to arm internally                   |
| `readyToArmExternally`     | Switch   |  RO   | Security area is ready to arm externally                   |
| `statusInternalSignalHorn` | Switch   |  RO   | Security area state of internally horn                     |
| `disarmedDatetime`         | Datetime |  RO   | Date and time the system was disarmed                      |
| `disarmedContact`          | String   |  RO   | Name of the contact wich dis armed the system              |
| `intArmedDatetime`         | Datetime |  RO   | Date and time the system was internally armed              |
| `intArmedContact`          | String   |  RO   | Name of the contact wich armed intenally the system        |
| `extArmedDatetime`         | Datetime |  RO   | Date and time the system was externally armed              |
| `extArmedContact`          | String   |  RO   | Name of the contact wich armed extenally the system        |
| `alarmDatetime`            | Datetime |  RO   | Date and time alarm was detected                           |
| `alarmContact`             | String   |  RO   | Name of the contact where alarm was detected               |
| `alarmSetClear`            | Switch   |  RO   | State alarm was set / clear                                |


### `emaState`

| channel                      | type     | RO/RW | description                                                |
|------------------------------|----------|-------|------------------------------------------------------------|
| `intrusionDatetime`          | Datetime |  RO   | Date and time intrusion was detected                       |
| `intrusionContact`           | String   |  RO   | Name of the contact where intrusion was detected           |
| `intrusionSetClear`          | Switch   |  RO   | State intrusion was set / clear                            |
| `batteryMalfunctionDatetime` | Datetime |  RO   | Date and time battery malfunction was detected             |
| `batteryMalfunctionContact`  | String   |  RO   | Name of the contact where battery malfunction was detected |
| `batteryMalfunctionSetClear` | Switch   |  RO   | State battery malfunction was set / clear                  |
| `powerOutageDatetime`        | Datetime |  RO   | Date and time power outage was detected                    |
| `powerOutageContact`         | String   |  RO   | Name of the contact where power outage was detected        |
| `powerOutageSetClear`        | Switch   |  RO   | State power outage was set / clear                         |
| `flasherMalfunctionDatetime` | Datetime |  RO   | Date and time flasher malfunction was detected             |
| `flasherMalfunctionContact`  | String   |  RO   | Name of the contact where flasher malfunction was detected |
| `flasherMalfunctionClear`    | Switch   |  RO   | State flasher malfunction was set / clear                  |
| `horn1MalfunctionDatetime`   | Datetime |  RO   | Date and time horn 1 malfunction was detected              |
| `horn1MalfunctionContact`    | String   |  RO   | Name of the contact where horn 1 malfunction was detected  |
| `horn1MalfunctionClear`      | Switch   |  RO   | State horn 1 malfunction was set / clear                   |
| `horn2MalfunctionDatetime`   | Datetime |  RO   | Date and time horn 2 malfunction was detected              |
| `horn2MalfunctionContact`    | String   |  RO   | Name of the contact where horn 2 malfunction was detected  |
| `horn2MalfunctionClear`      | Switch   |  RO   | State horn 2 malfunction was set / clear                   |

## Actions

The `ipbridge` and  the `serialbridge` thing expose the following action to the automation engine:
*setDateTime* - Send the date and time to Telenot device. 
Accepts no parameters.
