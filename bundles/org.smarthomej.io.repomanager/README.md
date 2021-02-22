# Repository Manager

The repository manager is used to control which additional feature repositories are used.
The current version supports the SmartHome/J bindings.

## Installation

Make the repository manager bundle available to your openHAB 3 or openHAB 3 compatible instance.
For openHAB this is as simple as dropping the .jar-file in the addons folder.
After that a new servlet is installed with the URL `/repomanager`, e.g. `http://openhab:8080/repomanager`.

## Repository Administration

The page is divided in two sections: Snapshot and Release.
After calling the page for the first time, both distributions are `inactive`.
You can activate one or both by clicking on the `Add` link at the bottom of the respective section.

After activating the page should show the available versions below the Status.
Behind each version the current status is displayed (either `active` or `inactive`).
Clicking on the link (version-name) toggles the status of that version.
You can activate as many versions as you like.

## Using SmartHome/J Bindings

After activating at least one version a new option shows up in the Main UI administration settings.
A new line with the label "SmartHomeJ Bindings" is shown in the Addons section.
Selecting that section shows a list of all available bindings with all available versions.
Installation of an addon works in the same way as for original distribution addons.

**Attention:** Make sure that you do not install two versions of the same binding.
You might run into severe trouble otherwise.
