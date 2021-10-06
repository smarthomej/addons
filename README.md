# Smarthome/J Add-ons

[![EPL-2.0](https://img.shields.io/badge/license-EPL%202-green.svg)](https://opensource.org/licenses/EPL-2.0)
[![Build Status 3.1.x](https://jenkins.smarthomej.org/job/SmartHomeJ%203.1.x%20Snapshot/badge/icon?subject=build%20status%203.1.x)](https://jenkins.smarthomej.org/job/SmartHomeJ%203.1.x%20Snapshot/)
[![Build Status 3.2.x](https://jenkins.smarthomej.org/job/SmartHomeJ%203.2.x%20Snapshot/badge/icon?subject=build%20status%203.2.x)](https://jenkins.smarthomej.org/job/SmartHomeJ%203.2.x%20Snapshot/)

This repository contains the add-ons that are implemented on top of [openHAB Core APIs](https://github.com/openhab/openhab-core).
Some parts of this repository are forked from [openHAB Addons](https://github.com/openhab/openhab-addons).

## Installation / Usage

The easiest way to use the bindings in this repository is to install the SmartHome/J repository manager.
The documentation can be found [here](https://github.com/smarthomej/addons/tree/main/bundles/org.smarthomej.io.repomanager).

Starting with openHAB 3.2.0.M3 a community marketplace which is accessible from the Main UI has been added. 
The SmartHome/J addons are available there and the repository manager has been deprecated for these openHAB versions.

### Compatibility

Due to a breaking changes in openHAB, older and newer versions of bundles are not compatible in every combination.
We'll continue to support 3.0 compatible addons until the release of openHAB 3.2.0 (which is expected end of 2021) and provide the same set of addons for newer versions.
Please check the table to see which versions of SmartHome/J are compatible with which openHAB version:

| | openHAB 3.0.x (milestones, releases) |  openHAB 3.1.0 (milestones <= M3) | openHAB 3.1.0 (milestone M4) | openHAB 3.1.0 (milestone M5, release) | openHAB 3.2.0 (milestone <= M2)| openhab 3.2.0 (milestone M3) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| SmartHome/J 3.1.x (snapshots, releases) | yes | yes | no | no | no | no |
| SmartHome/J 3.2.0 (snapshots, releases) | no | no | no | yes | yes | yes |
| compatible RepoManager | [latest 3.1.x](https://download.smarthomej.org/repomanager-latest) | [latest 3.1.x](https://download.smarthomej.org/repomanager-latest) | - | [latest 3.2.x](https://download.smarthomej.org/repomanager-latest-3.2.x) | [latest 3.2.x](https://download.smarthomej.org/repomanager-latest-3.2.x) | - |

## Development

SmartHome/J add-ons are [Java](https://en.wikipedia.org/wiki/Java_(programming_language)) `.jar` and [Apache Karaf](https://karaf.apache.org) `.kar` files.
Regarding development, code-style and alike the same rules and tooling that apply (or are used) within openHAB are also used.

The following differences apply:

- null checks: some warnings have been increased to error level
- null checks: annotations are required 

Happy coding! 
Pull requests always welcome and we'll try to review as soon as possible.
In case you need assistance, feel free to ask.
