# Smarthome/J Add-ons

[![EPL-2.0](https://img.shields.io/badge/license-EPL%202-green.svg)](https://opensource.org/licenses/EPL-2.0)
[![Build Status](https://www.travis-ci.com/smarthomej/addons.svg?branch=main)](https://www.travis-ci.com/smarthomej/addons)

This repository contains the add-ons that are implemented on top of [openHAB Core APIs](https://github.com/openhab/openhab-core).
Some parts of this repository are forked from [openHAB Addons](https://github.com/openhab/openhab-addons).

## Installation / Usage

The easiest way to use the bindings in this repository is to install the SmartHome/J repository manager.
The documentation can be found [here](https://github.com/smarthomej/addons/tree/main/bundles/org.smarthomej.io.repomanager).

The latest version of RepoManager can be downloaded [here](https://download.smarthomej.org/repomanager-latest).

### Compatibility

Due to a breaking change in openHAB 3.1 (starting with SNAPSHOT #2305), older and newer versions of bundles are no longer compatible.
Please check the table to see which versions of SmartHome/J are compatible with which openHAB version:

| | openHAB 3.0.x (snapshots, milestones, releases) |  openHAB 3.1.0 (snapshots <= #2305, milestones <= M3) | openHAB 3.1.0 (snapshots > #2305, milestones > M3 |
|---|:---:|:---:|:---:|
| SmartHome/J 3.1.x (snapshots, releases) | yes | yes | no |
| SmartHome/J 3.2.0 (snapshots, releases) | no | no | yes |

We'll continue to support 3.0 compatible addons until further notice.

## Development

SmartHome/J add-ons are [Java](https://en.wikipedia.org/wiki/Java_(programming_language)) `.jar` files.
Regarding development, code-style and alike the same rules and tooling that apply (or are used) within openHAB are also used.

Happy coding!

