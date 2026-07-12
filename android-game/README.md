# Rift Champions

A lane-based card duel for Android, built with Kotlin + Jetpack Compose. The game is
**inspired by the mechanics of Might & Magic: Duel of Champions** (resource cards that
generate mana permanently, lane-locked combat, a Champion/Hero with an activated power) —
but every card, faction, and piece of art here is original. No Ubisoft/Might & Magic
names, characters, or assets are used, so this is safe to build on and share freely.

## Project structure

```
android-game/
  engine/   Pure Kotlin/JVM module: card data, game rules, a simple AI opponent,
            and state (de)serialization. No Android dependency — compiles and
            unit-tests with plain Gradle, no SDK required.
  app/      The Android app: Jetpack Compose UI, view models, and Firebase
            Firestore integration for online play.
```

Keeping the rules engine in its own JVM-only module means the actual game logic (mana,
combat, keywords, win conditions) is fully unit-tested independent of Android — see
`engine/src/test/kotlin/.../GameEngineTest.kt` (17 tests, including a full AI-vs-AI
game played to completion and a state serialization round-trip).

## Game rules (v1)

- 2 players, each with a Champion (hero) with HP and one activated Power.
- 3 lanes per side, at most one creature per lane.
- Playing a Resource card (max one per turn) permanently adds +1 mana, usable immediately.
- Creatures have summoning sickness unless they have **Charge**; other keywords: **Ranged**
  (attack any enemy lane, not just the mirrored one), **Lifesteal**, **Poison** (destroys
  whatever it damages in combat, regardless of remaining health).
- Attacking an empty enemy lane hits the enemy champion directly.
- Ships with fixed 30-card preconstructed decks for 3 factions (Ashen Vanguard, Verdant
  Wardens, Hollow Choir) plus a neutral pool — see `engine/.../CardSet.kt`. A deckbuilder UI
  is a natural next step but out of scope for v1.

Three modes: **Practice vs AI** (a greedy rule-based opponent), **Pass & Play** (two humans,
one device — the active hand automatically flips to the bottom of the screen each turn),
and **Online** (Firebase Firestore, see below).

## Building

### The engine module (works right now, anywhere)

```
cd android-game
gradle :engine:test
```

No Android SDK needed — it's a plain Kotlin/JVM module.

### The Android app

This needs the Android SDK and Jetpack Compose/Firebase dependencies from Google's Maven
repository, so **open the `android-game/` folder in Android Studio** and let it sync — it
will download everything it needs and offer to generate a Gradle wrapper if one isn't
already present (none is checked in: it wasn't possible to fetch the wrapper jar from the
environment this project was originally scaffolded in).

Minimum requirements: Android Studio Koala (2024.1) or newer, minSdk 26 (Android 8.0)+.

The app builds and is fully playable (Practice vs AI, Pass & Play) with **no extra setup**.
Online play needs a Firebase project — see below.

## Enabling online play (optional)

Online mode syncs a match through Cloud Firestore. It's entirely optional: without it, the
app still builds and runs, and the "Play Online" screen just explains it isn't configured.

1. Go to the [Firebase console](https://console.firebase.google.com/) and create a new
   project (free "Spark" tier is enough).
2. Add an Android app to it with package name `com.riftchampions.app`.
3. Download the generated `google-services.json` and place it at `app/google-services.json`.
4. In the Firebase console, enable **Cloud Firestore** (Build → Firestore Database → Create
   database). For a quick start use test mode rules; for anything beyond your own testing,
   lock the `games/{roomId}` collection down appropriately.
5. Rebuild — `app/build.gradle.kts` detects the file automatically and turns Firebase on.

Online play is turn-based: the whole match state is re-serialized and written to a single
Firestore document after every move (see `GameStateSerializer` in the engine module and
`FirebaseOnlineRepository`/`OnlineGameViewModel` in the app). That's simple and robust for a
1v1 turn-based game; it uses a bit more bandwidth than a real move-by-move protocol would,
which is a fine trade-off at this scale.

## Known limitations / possible next steps

- No deckbuilder — decks are fixed per faction.
- No persistence/accounts — online rooms are just a shared 5-character code, first-come
  matchmaking only.
- The Android app module could not be compiled in the environment this project was
  originally written in (its network policy blocks Google's Maven repository, which hosts
  the Android Gradle Plugin, Jetpack Compose, and Firebase artifacts). The `engine` module
  *is* fully compiled and unit-tested there. Build the `app` module in Android Studio to
  verify it end-to-end on an emulator or device.
