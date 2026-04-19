# Armageddon Plugin

Standalone Paper plugin source for the custom Armageddon gameplay and world-management features running on Gimli.

This repository contains only the plugin code and build scaffolding. It does **not** include live server data, player state, secrets, control-panel credentials, or host-specific operational scripts.

## Features

- cinematic chaos commands such as `/armageddon` and `/nuke`
- instant structure spawning with `/build`
- persistent map routing with `/maps`, `/joinmap`, and `/resetmap`
- clean-world rebuild flow with `/renew`
- companion mob behavior with `/followme`
- rotating server game modes via `/game`
- hidden launch-code generation with `/spawnnukecode`

## Commands

| Command | Description |
| --- | --- |
| `/armageddon [on <player>]` | Summon aerial chaos around yourself or another player |
| `/nuke <code> [on <player>]` | Drop a world-ending TNT toward yourself or another player |
| `/build <house\|cage\|tower\|arena\|bunker\|skybox\|lava_trap> [on <player>]` | Place a quick structure |
| `/maps` | List persistent maps |
| `/joinmap <clean_slate\|fresh_survival\|lobby\|hogwarts\|zombieapocalypse\|greenfield>` | Join a persistent map |
| `/resetmap <map> [confirm] [final]` | Reset a persistent map with confirmation |
| `/renew` | Restore the main world from the server baseline |
| `/followme <horse\|pig\|chicken> [on <player>]` | Spawn a mob companion |
| `/game effect <true\|false>` | Toggle the rotating effect mode |
| `/spawnnukecode` | Hide the current launch code somewhere in the world |

## Requirements

- Java 21
- Paper / Paper API `1.21.x`

## Build

Use the Gradle wrapper:

```bash
./gradlew build
```

The built jar will be written to:

```text
build/libs/armageddon-plugin-<version>.jar
```

## Install

1. Build the plugin with `./gradlew build`
2. Copy the jar from `build/libs/` into your Paper server's `plugins/` directory
3. Start or restart the server

## Repository boundaries

This repository intentionally excludes:

- live world data
- player data
- runtime backups
- API keys or tokens
- control-panel integration secrets
- machine-local deployment scripts

## Server integration notes

Several features expect supporting server-side infrastructure outside this repository:

- persistent map templates under `map-templates/`
- staging data under `map-staging/`
- BlueMap config output under `plugins/BlueMap/maps/`
- `/renew` depends on the server startup wrapper honoring a `.renew_pending` marker in the world container

Those pieces belong to the server environment, not the plugin source tree.
