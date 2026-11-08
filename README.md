# AYN Translate

Play Japanese games on the top screen of a dual-screen Android handheld and read them on the
bottom screen: every line with furigana, a translation, a grammar note, and a tap-to-look-up
word and kanji dictionary.

Built for the [AYN Thor](https://www.ayntec.com/), and works on other Android 11+ devices with a
second screen. Bring your own model: Mistral (default), or any OpenAI-compatible server
such as OpenAI, OpenRouter, Ollama or LM Studio. It also has an offline mode with on-device OCR.

| Top screen: the game | Bottom screen: the lines |
| --- | --- |
| ![A Japanese RPG dialogue on the top screen](docs/screenshots/game.png) | ![The dialogue with furigana, French translation and grammar notes](docs/screenshots/lines.png) |

| Tap a word | Tap a kanji | Read only the text box |
| --- | --- | --- |
| ![Word details for 無理](docs/screenshots/word.png) | ![Kanji details for 無](docs/screenshots/kanji.png) | ![Selecting the dialogue box](docs/screenshots/region.png) |

## Features

- **Reads the game screen.** Android screen capture grabs the top screen when you press
  **Scan**, and your model returns each line of Japanese text with its translation.
- **Auto mode.** Watches the screen (every 1 to 10 s, 2 s by default) and reads it only when
  the picture changes, so dialogue follows you without button presses and without paying for
  identical frames.
- **Word and kanji dictionary.** Tap a word for its reading, dictionary form, part of speech,
  meaning in context and its kanji. Tap a kanji for strokes, school grade, radical, frequency,
  on and kun readings and meanings, from KANJIDIC2, offline.
- **Text box region.** Drag a rectangle over the game's dialogue box so menus and HUD
  are ignored. The region is saved.
- **Keeps the controller on the game.** Touching the bottom screen does not steal the
  buttons from the emulator on the top screen. This can be turned off in the settings.
- **Furigana and romaji**, each optional.
- **Translates into** English, French, Spanish, German, Italian, Portuguese, Dutch, Polish,
  Russian, Ukrainian, Chinese, Korean, Arabic, Turkish, Indonesian or Vietnamese.
- English and French interface.

## Install

Download `ayn-translate-<version>.apk` from the
[latest release](https://github.com/alouafi/ayn-translate/releases/latest) and open it on the
device, or:

- **Obtainium:** add `https://github.com/alouafi/ayn-translate` as an app source to get updates.
- **adb:** `adb install ayn-translate-<version>.apk`.

The APK is arm64 only (`arm64-v8a`), like the AYN Thor and most recent handhelds.

## Set up on the AYN Thor

1. Open **AYN Translate** from the app drawer. From Cocoon or any other launcher on the top
   screen, it moves itself to the bottom screen.
2. Tap the gear, choose a model and paste your API key. For Mistral, create a key at
   [console.mistral.ai](https://console.mistral.ai/). Then tap **Test connection**.
3. Go back and tap **Start capture**. Android asks for permission **on the top screen**: choose
   **Start now**. A notification stays visible while the capture is on. Stop it with the ✕
   next to "Capture on".
4. Start your game on the top screen and press **Scan**, or turn on **Auto**.
5. Optional: tap the crop icon and drag over the game's text box.

Android forgets the capture permission when the capture stops, so you confirm it once per
play session.

## How the screen is read

| Mode | What happens | Needs |
| --- | --- | --- |
| **Vision model** (default) | One request: the model reads the screenshot, translates it and splits it into words. | A model that accepts images |
| **OCR, then language model** | OCR reads the text, then a text model translates it. | Any chat model, plus an OCR engine |
| **OCR only** | Japanese text with the kanji dictionary, without translation. | Nothing online with on-device OCR |

OCR engines:

- **On device:** Google ML Kit's Japanese model, bundled in the APK. It is free and works
  offline, and is best on clean, large fonts. On small pixel fonts it can misread kanji.
- **Mistral OCR** (`mistral-ocr-latest`): better on small and pixel fonts. It uses your
  Mistral API key.

### Models

- **Mistral** (default provider): **Small** (`mistral-small-latest`) is the default and the
  recommended model. In tests on game dialogue, Medium sometimes "corrected" the game's text
  into a more common phrase (for example 来た instead of 戻った), while Small copied it
  faithfully.
- **OpenAI-compatible:** set the base URL, an optional API key and the model name. Examples:
  - OpenAI: `https://api.openai.com/v1` with `gpt-4.1-mini`
  - OpenRouter: `https://openrouter.ai/api/v1`
  - Ollama or LM Studio on your network: `http://192.168.1.20:11434/v1` with a vision model
    such as `qwen2.5vl`

  The Vision mode needs a model that accepts images. The OCR modes work with text-only
  models.

Each scan sends one JPEG of the game screen, at most 1600 px on the long side. With a region,
it sends only the region.

## Privacy

- Screenshots go only to the provider you configure, and only when you scan or when Auto
  sees a new picture.
- API keys are encrypted with a key held in the Android Keystore. App data is excluded from
  backups and device transfers.
- The app has no analytics and makes no other network calls. Google ML Kit, used for
  on-device OCR, may send anonymous usage metrics to Google, but your screenshots stay on
  the device.

## Build

Requirements: JDK 21 and the Android SDK (compile SDK 37).

```sh
./gradlew testDebugUnitTest lintDebug assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`. To sign it with your own key, create
`keystore.properties` at the repository root (it is ignored by git):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

You can also set the `AYN_TRANSLATE_STOREFILE`, `AYN_TRANSLATE_STOREPASSWORD`,
`AYN_TRANSLATE_KEYALIAS` and `AYN_TRANSLATE_KEYPASSWORD` environment variables. Without a
signing key, the release build is signed with the debug key.

### Project layout

```
app/src/main/kotlin/io/github/alouafi/ayntranslate/
  analysis/    prompts, JSON parsing, and the screen readers for each mode
  capture/     MediaProjection service, frame capture, scene change detection
  dictionary/  offline kanji dictionary, kana and romaji helpers
  display/     picks the second screen for the companion
  llm/         OpenAI-compatible chat client (Mistral uses the same API)
  ocr/         ML Kit and Mistral OCR
  settings/    settings, encrypted keys, region
  ui/          launcher, companion (bottom screen), settings, capture consent
scripts/build_kanji_data.py   rebuilds app/src/main/assets/kanji.tsv from KANJIDIC2
```

## Status

Version 0.1.0 was tested on an Android 13 emulator set up like the Thor: a 1920×1080 main
screen and a 1240×1080 second screen. The tests covered:

- Every mode, both providers, Auto, the region editor and the dictionary.
- Controller focus staying on the game.

It has not been tested on a real AYN Thor yet. Issues and pull requests are welcome.

## Credits and license

The code is under the [MIT license](LICENSE).

The kanji dictionary (`app/src/main/assets/kanji.tsv`) is built from
[KANJIDIC2](https://www.edrdg.org/wiki/index.php/KANJIDIC_Project), © the Electronic Dictionary
Research and Development Group, used under the
[Creative Commons Attribution-ShareAlike 4.0 license](https://www.edrdg.org/edrdg/licence.html).
That file stays under CC BY-SA 4.0.

Text recognition on the device uses Google ML Kit, under the
[ML Kit terms](https://developers.google.com/ml-kit/terms).

---

## En bref (français)

AYN Translate affiche le jeu japonais sur l'écran du haut et, sur l'écran du bas, chaque
ligne avec furigana, traduction et une note de grammaire. Touchez un mot ou un kanji pour
ouvrir le dictionnaire, qui marche hors ligne.

1. Installez l'APK de la [dernière release](https://github.com/alouafi/ayn-translate/releases/latest),
   ou via Obtainium avec l'URL du dépôt.
2. Ouvrez AYN Translate. L'app s'affiche sur l'écran du bas.
3. Dans Réglages (roue dentée), collez votre clé API Mistral et gardez le modèle **Small**,
   puis touchez **Tester la connexion**.
4. Touchez **Lancer la capture d'écran**, puis acceptez la demande d'Android sur l'écran du haut.
5. Lancez le jeu, puis appuyez sur **Scanner** ou activez **Auto**. L'icône de recadrage
   permet de ne lire que la boîte de dialogue.
