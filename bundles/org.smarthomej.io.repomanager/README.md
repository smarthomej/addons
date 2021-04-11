# Repository Manager

The repository manager is used to control which additional feature repositories are used.
The current version supports the SmartHome/J bindings.

## Installation

Make the repository manager bundle available to your openHAB 3 or openHAB 3 compatible instance.
For openHAB this is as simple as dropping the .jar-file in the addons folder.
After that a new servlet is installed with the URL `/repomanager`, e.g. `http://openhab:8080/repomanager`.

### Compatibility

Due to a breaking change in openHAB 3.1 (starting with SNAPSHOT #2305), older and newer versions of bundles are no longer compatible.
We'll continue to support 3.0 compatible addons until further notice and provide the same set of addons for newer versions.
Please check the table to see which versions of SmartHome/J are compatible with which openHAB version:

| | openHAB 3.0.x (snapshots, milestones, releases) |  openHAB 3.1.0 (snapshots <= #2305, milestones <= M3) | openHAB 3.1.0 (snapshots > #2305, milestones > M3 |
|---|:---:|:---:|:---:|
| SmartHome/J 3.1.x (snapshots, releases) | yes | yes | no |
| SmartHome/J 3.2.0 (snapshots, releases) | no | no | yes |
| compatible RepoManager | [latest 3.1.x](https://download.smarthomej.org/repomanager-latest) | [latest 3.1.x](https://download.smarthomej.org/repomanager-latest) | [latest 3.2.x](https://download.smarthomej.org/repomanager-latest-3.2.x) |

## Repository Administration

The page is divided in two sections: Snapshot and Release.
After calling the page for the first time, both distributions are not active.
You can enable one or both by checking the corresponding checkbox element.
To store and activate this setting, press the "Apply" button in the top-right corner.

After activating this setting, the page should show the available versions next to the distribution.
In the same way you can now enable or disable one or more of the available versions.
The RepoManager takes care of compatibility issues, so you'll only see compatible versions.
Don't forget to store that setting with the "Apply" button.

For the snapshots distribution only the latest version is shown.
The reason is that snapshots are short lived artifacts that are overwritten when newer versions are available.
The RepoManager takes care of upgrading your installed bindings if necessary.

## Using SmartHome/J Bindings

After activating at least one version new option shows up in the Main UI administration settings.
New lines starting with "SmartHome/J" are shown in the Addons section.
Selecting one these sections shows a list of all available addons with all available versions.
Installation of an addon works in the same way as for original distribution addons.

**Attention:** Make sure that you do not install two versions of the same binding.
You might run into severe trouble otherwise.
