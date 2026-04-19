# Armageddon Plugin

Standalone Paper plugin source for the custom Armageddon server features running on Gimli.

## What is included

- `/armageddon`
- `/nuke`
- `/build`
- `/maps`
- `/joinmap`
- `/resetmap`
- `/renew`
- `/followme`
- `/game effect`
- `/spawnnukecode`

## What is intentionally not included

- live server data
- player state
- backups
- host-specific runtime scripts
- API keys, tokens, or panel credentials

## Build

This project targets Java 21 and Paper API `1.21.8-R0.1-SNAPSHOT`.

```bash
./gradlew build
```

If you do not use the Gradle wrapper yet, you can generate it locally with:

```bash
gradle wrapper
```

The built plugin jar will be created under `build/libs/`.

## Runtime notes

Some features expect supporting server-side directories or workflows that live outside this repository:

- persistent map templates under `map-templates/`
- staging data under `map-staging/`
- BlueMap config output under `plugins/BlueMap/maps/`
- `/renew` relies on the server startup wrapper honoring a `.renew_pending` marker in the world container

Those integration points are part of the server environment, not this repository.
