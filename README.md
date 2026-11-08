# Nodoro

A minimal Pomodoro timer for Android, built with Material 3 Expressive and first-class support for the
**Nothing Phone Glyph Matrix**.

This is the **open build**: every feature is unlocked, there are no ads, no purchases, and the app does not
request the Internet permission.

## Features

- **Focus / break cycles** with configurable focus, short break and long break lengths
- **Two dials**: a classic circular dial, or a Glyph-style phone artwork. Your last choice is remembered
- **Glyph Matrix support** on Nothing phones: live progress on the back, with brightness and reverse options
- **Stats**: today, this week, this month and this year, plus lifetime totals and a daily goal
- **Customisation**: light, dark and AMOLED themes, dynamic color or accent colors, in-app language picker
- **Alerts**: alarm sound (choose your own, set the ring length) and vibration
- **Private by design**: no account, no analytics, no network. Everything stays on your device

## Requirements

- Android 14 (API 34) or newer
- Glyph features need a Nothing phone with a Glyph Matrix (other phones work fine without it)

## Build

```
./gradlew assembleRelease
```

Release signing: create `keystore.properties` (git-ignored) with `storeFile`, `storePassword`, `keyAlias`
and `keyPassword`.

The Nothing Glyph Matrix SDK is bundled in `app/libs/`.

## Project layout

| Path                                         | What lives there                       |
|----------------------------------------------|----------------------------------------|
| `app/src/main/java/app/kohese/nothingtimer/` | Activity, `TimerViewModel`, Compose UI |
| `.../glyph/`                                 | Glyph Matrix controller, dial artwork  |
| `.../ui/settings/`                           | Settings screen                        |
| `docs/`                                      | Privacy policy site (GitHub Pages)     |

## Privacy

See the [privacy policy](https://kohesee.github.io/Nodoro/privacy-policy.html) (source: `docs/`).

## Author

Kaivalya Beohar - kohese.dev@proton.me