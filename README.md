# Welcome to RunePath Sync! 🧭

**Thanks for stopping by.** RunePath Sync is a companion project built for Old School RuneScape players who want their adventures, milestones and achievements in one place. Whether you're grinding skills, chasing quest cape requirements or collecting combat achievements, the goal is simple: **play OSRS normally, then see your progress in RunePath without manually transferring JSON files.**

This project is being developed in the open, and feedback, bug reports and constructive suggestions are welcome. Please remember that it's still a **development-stage** RuneLite plugin and isn't yet published on the RuneLite Plugin Hub.

## What it does

RunePath Sync is an opt-in, Java 11-compatible RuneLite plugin using the [official RuneLite example-plugin](https://github.com/runelite/example-plugin) structure.

- Shows a short-lived device pairing code in RuneLite, approved in the RunePath **Account** tab.
- Transfers your current **skill levels and XP**, **quest states**, **achievement diary tiers and task counts**, and **combat achievement task completion**.
- Captures optional *named diary tasks* from journal pages you actually open; missing journal coverage isn't treated as an incomplete task.
- Records a separate, last-known snapshot for each dataset, so a partial or unavailable capture doesn't erase unrelated progress.
- Verifies the active character against the linked RunePath character.
- Uses HTTPS and a background worker for network requests, with a dedicated in-client status panel.
- Lets you revoke a connected device through RunePath.

The plugin never asks for Jagex credentials, and **bank, inventory, equipment and seed-vault contents are not transmitted**.

## Getting connected

1. Open the [RunePath app](https://floot.com/) using its published address. Sign in and choose **Account**.
2. Run this development plugin inside RuneLite using `./gradlew run` (Windows: `gradlew.bat run`).
3. In RunePath Sync settings, enter your **published RunePath HTTPS origin**, not a Floot preview link. Enable third-party progression sync, which defaults to **off**.
4. Log into your OSRS character and open the RunePath Sync sidebar.
5. Select **Connect RunePath** and enter the temporary code in RunePath's **Account → Connect RuneLite** area.
6. Check the requested character and device name before approving. The client should then start syncing supported progression.

**Note:** RuneLite is temporarily unavailable for end-to-end game tests. Cloud compilation alone does not prove live game compatibility. This code is **not yet a Plugin Hub release**.

## Data coverage and reliability

| Dataset | Status | Notes |
| --- | --- | --- |
| Skills and XP | Implemented | Full snapshots from the active character |
| Quests | Implemented | Finished, in progress and not started |
| Achievement diaries | Implemented; requires in-game validation | Completion flags and task counts for all 12 diary areas |
| Individual diary tasks | Partial | Named task rows are captured only from journal pages observed in game; Karamja offers individual-task varbits |
| Combat achievements | Implemented; requires in-game validation | Game-cache task metadata and bit-packed completion flags; fails closed when metadata is incomplete |
| Collection log, bank and inventory | Not supported | Deliberately omitted from network synchronisation |

IDs for some RuneLite game variables and the combat achievement task catalogue can change when the game updates. The initial diary and combat task mapping was researched against the third-party [Character Export](https://github.com/DZWNK/Character-Export) project. Its author and [BSD-2-Clause licence](https://github.com/DZWNK/Character-Export/blob/main/LICENSE) are acknowledged. Always cross-check values against live RuneLite game data after updates.

## Remote build

This repository runs automated Java compilation and tests through **GitHub Actions** using a Java 11 bytecode target. Open the [Actions tab](https://github.com/fkperry/runepath-sync/actions) for the latest result.

The development branch is `feature/runepath-sync-mvp`; changes should remain under review until they have passed real RuneLite gameplay testing. See [pull request #1](https://github.com/fkperry/runepath-sync/pull/1).

## Privacy and security

- **Explicit consent:** third-party syncing is disabled by default.
- **Independent account:** RunePath needs a RunePath login, **not** your Jagex password.
- **Data transmitted:** character display name, selected progression snapshots, and the device's IP address as part of normal HTTPS communication.
- **One-time approval:** RunePath's pairing code expires after 10 minutes and requires approval by a signed-in user.
- **Revocable devices:** disconnect in RunePath to invalidate the stored token.
- **Local credentials:** the device token is stored using RuneLite's configuration service, **not an encrypted vault**. Keep your computer secure and revoke access from RunePath if necessary.
- **Developer status:** RunePath is a third-party community app, not affiliated with Jagex or RuneLite.

## Contributing

Found an issue with how quests, diaries or combat achievements sync? Please open a [GitHub Issue](https://github.com/fkperry/runepath-sync/issues) with the affected dataset, RuneLite version, a description of the behavior and reproduction steps. Never share passwords, device tokens, pairing secrets, account cookies or private inventory files.

---

*Built for the OSRS grind, including those stretches when the next drop has apparently decided to take annual leave.*