# Smarthome/J Add-ons

[![EPL-2.0](https://img.shields.io/badge/license-EPL%202-green.svg)](https://opensource.org/licenses/EPL-2.0)
[![Build Status 4.0.x](https://github.com/smarthomej/addons/actions/workflows/ci-build-40.yml/badge.svg?branch=4.0.x)](https://github.com/smarthomej/addons/actions/workflows/ci-build-40.yml)
[![Build Status 4.1.x](https://github.com/smarthomej/addons/actions/workflows/ci-build-41.yml/badge.svg?branch=4.1.x)](https://github.com/smarthomej/addons/actions/workflows/ci-build-41.yml)
[![Build Status 4.2.x](https://github.com/smarthomej/addons/actions/workflows/ci-build-42.yml/badge.svg?branch=4.2.x)](https://github.com/smarthomej/addons/actions/workflows/ci-build-42.yml)

This repository contains the add-ons that are implemented on top of [openHAB Core APIs](https://github.com/openhab/openhab-core).
Some parts of this repository are forked from [openHAB Addons](https://github.com/openhab/openhab-addons).

## Installation / Usage

It is recommended to add `https://download.smarthomej.org/addons.json` as JSON 3rd Party Addon Service in the settings.
Afterwards all addons can be installed from the UI.
Due to limitations openHAB Core, please make sure to disable "Show incompatible add-ons" on the "Add-on Management" page (NOT the "JSON 3rd Party Addon Service" page).

### Compatibility

Due to a breaking changes in openHAB, older and newer versions of bundles are not compatible in every combination.
Currently the last two stable releases of openHAB (4.0 and 4.1) as well as the current development version (4.2, only latest snapshots) are supported.

The downloads for openHAB 3.2-3.4 are still available, but these versions will no longer receive updates.
Please consider upgrading your openHAB installation.

Starting with 4.0, the version numbers between openHAB Core and SmartHome/J are the same, i.e. SmartHome/J 4.0.x is compatible with openHAB 4.0.x.

### Upgrading SmartHome/J bindings installed from the JSON 3rd Party Add-on service

The newest version (and only that !) is available for installation in the UIs Add-ons section.
You'll always install the newest version, but installed addons keep their version.

Unfortunately there is no automatic or half-automatic update process.
If you want to upgrade after a new version is released, you have to manually uninstall and re-install the binding.
Your configurations (binding-configurations and thing configurations) are safe and will be picked up by the new version.
Things will automatically update their type/definition where necessary.

For version changes in openHAB that require a change in the version change (i.e. not only changes of the last number) of SmartHome/J, it is recommended to uninstall the add-ons BEFORE the openHAB upgrade and re-install them afterwards. 

*Attention:* Even though we try to reduce breaking changes to an absolute minimum, please always look at the release notes prior to updating.

## Development

SmartHome/J add-ons are [Java](https://en.wikipedia.org/wiki/Java_(programming_language)) `.jar` and [Apache Karaf](https://karaf.apache.org) `.kar` files.
Regarding development, code-style and alike the same rules and tooling that apply (or are used) within openHAB are also used.

The following differences apply:

- null checks: some warnings have been increased to error level
- null checks: annotations are required 

Happy coding! 
Pull requests always welcome, and we'll try to review as soon as possible.
In case you need assistance, feel free to ask.
