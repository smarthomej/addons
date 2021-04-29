# Notifications for Fire TV Binding

The Notifications for Fire TV Binding provides support for sending notifications to a Fire TV from rule.
To use the binding it is necessary to install the free app `Notifications for Fire TV` by Christian Fees on the Fire TV.
The app is available on the Amazon app store by searching for the name.

## Supported Things

There is only one thing: `notification` which represents a Fire TV.

## Thing Configuration

There is only one mandatory parameter `hostname`.

The `hostname` must be an valid IP address or a FQDN.

The other parameters are optional but recommended.

The `tile` parameter may be the sender of the notification.
The `duration` defines how long the notifications stay on the screen.
The `position` is used to set the location of the notification on the screen.
The `transparency` set the alpha channel of the notification background.
The `offsetX` changes the x (horizontal) coordinate of the notification relative to the `position`.
The `offsetY` changes the y (vertical) coordinate of the notification relative to the `position`.
The `force` parameter can instruct the Fire TV to show the notification in any case.

## Channels

There are no channels for the `notification` thing.

## Rule Action

This binding includes rule actions for sending notifications to a Fire TV.
Two different actions available:

* `boolean success = sendNotification(String message, String icon)`
* `boolean success = sendNotification(String message, String icon, String image)`

The `sendNotification(...)` actions send a notification with icon (with image if supplied) to the Fire TV.

The function returns a boolean as the result of the operation.

`icon` and `image` must be a valid path to an image in png format.
`icon` should be a small icon which shows the topic of the notification.
`image` can be a larger image like a picture captured from a camera.

Please note: All strings are expected to be UTF-8 encoded.
Using different character sets may produce unwanted results.

Examples (Rules DSL):

```
val notificationsForFireTVActions = getActions("notificationsforfiretv","notificationsforfiretv:notification:1")
var boolean success = notificationsForFireTVActions.sendNotification("This is the notification content.", "/path/to/icon.png")
success = notificationsForFireTVActions.sendNotification("This is the notification content.", "/path/to/icon.png", "/path/to/image.png")
```
