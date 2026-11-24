# RunePath Sync

An **early-development**, opt-in RuneLite plugin that synchronises Old School RuneScape progress with the RunePath companion app. The plugin uses the official [RuneLite example-plugin](https://github.com/runelite/example-plugin) template and builds with Java 11 bytecode.

## Features

- User-initiated device pairing with a 10-minute code, approved in RunePath Account.
- Explicit third-party sync consent (off by default).
- Full skill levels/XP and quest-state snapshots on login, relevant game changes and periodic reconciliation.
- Client-thread-only game state capture; HTTPS calls on an asynchronous worker.
- Character-specific pairing and connection status in the sidebar.
- Server-side device revocation. No Jagex credentials or bank information transmitted.

Achievement diaries and combat achievements are supported by RunePath's server API but **not yet implemented in the desktop plugin**.

## Set up

1. The RunePath app must be **published** at a stable HTTPS origin. Its preview URL is not a suitable server URL.
2. Open RuneLite in developer mode with `./gradlew run` (on Windows, `gradlew.bat run`).
3. In RunePath Sync settings, enter the **published** RunePath origin and enable opt-in third-party sync.
4. Log into your OSRS character, open the RunePath Sync sidebar and click Connect RunePath.
5. Open RunePath > Account on your phone, enter the displayed code, verify the character and approve.
6. Once linked, choose Sync Now. RunePath Account shows incoming data and allows disconnecting the device.

## Cloud builds

GitHub Actions runs the project's Gradle wrapper against JDK 17 with a Java 11 bytecode target. Check the Actions tab on this repository for compilation status.

## Privacy & security

- The RunePath server sees your IP address, character name, levels, XP and quest statuses; it does not receive Jagex passwords, cookies or tokens.
- Connections are opt-in and individually revocable through RunePath.
- Device tokens are saved in RuneLite config, which is not an encrypted credential vault. Treat your local RuneLite configuration as sensitive.
- Forgetting the token locally **does not** revoke it server-side; disconnect the device in RunePath too.
- RunePath is independent of Jagex and the RuneLite developers.

## Status

Development prototype. **Do not submit to the Plugin Hub until cloud compilation and in-client testing pass.** RunePath is not yet published, so the plugin cannot currently pair with its public production API.
