# Minecraft Armageddon Plugin

Armageddon is a custom Paper plugin for a family-run Minecraft server running on our Oracle Cloud free tier host. It bundles a handful of playful server commands, map-management helpers, and experimental game modes into one plugin that can be dropped into a Paper server.

This project was created with **Codex** as part of a fun learning experiment with my 7th grade son. Along the way, it's been helping my son learn:

- Minecraft server management
- local networking concepts like SSH and command-line workflows
- iterative plugin development with Codex

So this repository is both a working plugin and a record of that learning project.

## What This Project Is

At a high level, Armageddon adds a mix of:

- over-the-top chaos commands like `/armageddon` and `/nuke`
- convenience tools like `/build` and `/followme`
- persistent map commands like `/maps`, `/joinmap`, and `/resetmap`
- server-state controls like `/renew`
- experimental game modes through `/game`

If you are new to this codebase, the most important thing to know is that this is not a general-purpose framework. It is a purpose-built plugin for a specific Minecraft server, shaped by real play, experiments, and a lot of iteration.

## What This Repository Includes

This repository contains:

- the plugin source code
- Gradle build files
- the Paper plugin manifest
- a basic default config
- GitHub Actions build automation

This repository does **not** include:

- live world data
- player data
- backups
- server control-panel credentials
- API keys or tokens
- host-specific deployment scripts

## Main Commands

| Command | Description |
| --- | --- |
| `/armageddon [on <player>]` | Summon aerial chaos around yourself or another player |
| `/nuke <code> [on <player>]` | Drop a world-ending TNT toward yourself or another player |
| `/build <house\|cage\|tower\|arena\|bunker\|skybox\|lava_trap> [on <player>]` | Instantly place a simple structure |
| `/maps` | List the persistent maps available on the server |
| `/joinmap <clean_slate\|fresh_survival\|lobby\|hogwarts\|zombieapocalypse\|greenfield>` | Join a persistent map |
| `/resetmap <map> [confirm] [final]` | Reset a persistent map back to its template |
| `/renew` | Restore the main server world from its saved baseline |
| `/followme <horse\|pig\|chicken> [on <player>]` | Spawn a companion mob that follows a player |
| `/game effect <true\|false>` | Toggle the rotating effect game mode |
| `/spawnnukecode` | Hide the current launch code somewhere in the world |

## How To Build It

### Requirements

- Java 21
- a Paper-compatible server target in the `1.21.x` family

### Build Command

Use the Gradle wrapper:

```bash
./gradlew build
```

The built jar will be created at:

```text
build/libs/armageddon-plugin-<version>.jar
```

## How To Install It

1. Run `./gradlew build`
2. Copy the jar from `build/libs/` into your server's `plugins/` directory
3. Start or restart the Paper server

## Notes About Server-Specific Behavior

Some features rely on server-side directories or operational workflows that are intentionally outside this repository. For example:

- persistent map templates under `map-templates/`
- staging data under `map-staging/`
- BlueMap config output under `plugins/BlueMap/maps/`
- the `/renew` flow depending on a startup wrapper that honors a `.renew_pending` marker

That means this repo gives you the plugin itself, but not every piece of surrounding server infrastructure from the original environment.

## Why This Repo Exists

The goal here is not just to publish code. It is to share a real project that was built while learning together:

- how Minecraft servers work
- how to manage a remote box with SSH and the CLI
- how to use Codex as a practical development partner

That context matters. Armageddon is meant to be fun, a little chaotic, and educational at the same time.
