# OneYoutubePlayer

Personal Android phone/tablet/TV player based on [PipePipe](https://github.com/InfinityLoop1308/PipePipe),
with an embedded playback runtime from [SmartTube](https://github.com/yuliskov/SmartTube).

## Initial test build

One APK contains **both** PipePipe and SmartTube. Enter a YouTube URL or video ID,
select an engine, and press **Play video**. Return to the entry screen to select the
other engine and reopen the video. The last input and engine are remembered.

- PipePipe: inherited player, search, subscriptions, queue and background playback.
- SmartTube: embedded extractor and relocated custom ExoPlayer, including SABR,
  DASH, HLS and direct URL source selection. Initial controls: play/pause, ±10s, back.
- Manual selection; no automatic fallback or switching in the middle of playback.
- Switching closes the previous owned player session before opening the next.
- App ID: `dev.oneyoutubeplayer.app` (`.debug` for debug builds).

This is an integration test harness. Search/subscription playback still uses PipePipe;
the engine selector currently applies to manual URL/ID entry. SmartTube's full TV UI,
login, cast, background service, quality menu and upstream error-recovery controllers
are not included. Real TV/tablet and sustained/live playback remain validation work.
YouTube changes still require upstream fixes, integration and a new app build.

## Sources

```sh
git clone --recurse-submodules https://github.com/robotmanaiautomation/OneYoutubePlayer.git
# Existing checkout:
git submodule update --init --recursive
```

`vendor/PipePipeExtractor` and `vendor/SmartTube` retain pinned upstream sources.
See [upstream-lock.json](upstream-lock.json). Generated copies and caches are ignored.

## Build the combined APK

Requires JDK 17 for SmartTube and JDK 25 for the host, Android SDK platforms 34 and
`android-37.0`, build-tools 30.0.3 and 36.0.0, and `sdk.dir` in `local.properties`.
The script defaults to this Mac's JDK locations; override `ONEPLAYER_JDK17` and
`ONEPLAYER_JDK25` on other systems.

```sh
./tools/build_debug.sh
```

The script regenerates relocated SmartTube sources, builds eight selected AARs with
SmartTube's Gradle 7.5, exports their external dependencies, then builds the unified
app with Gradle 9.5.1 and runs the local contract/relocation tests.

APKs: `app/build/outputs/apk/debug/OneYoutubePlayer_0.1.0-<abi>-debug.apk`.
Most current phones/tablets/TVs use `arm64-v8a`; older ARM devices may need `armeabi-v7a`.
Debug APKs use a development signing key. Install only the ABI that matches the device.

After source preparation, incremental host builds use:

```sh
./gradlew :app:assembleDebug :playback-core:test
python3 -m unittest discover -s tools -p 'test_prepare_smarttube.py' -v
```

## Integration boundary

`tools/prepare_smarttube.py` relocates SmartTube ExoPlayer Java/Kotlin/XML/reflection/
protobuf/JNI references in `.generated/smarttube/`; it never rewrites source submodules.
The generated copy uses protoc 3.25.5 to support Apple Silicon. Its original compiler
was 3.17.3. `tools/export_smarttube.gradle` stages only the extractor, shared utilities,
interfaces, Commons IO and custom ExoPlayer core/DASH/HLS/SABR AARs.

The host uses one full Protobuf runtime and its existing NanoJSON fork, and aligns
OkHttp extensions with host OkHttp 5.4.0. These are integration adaptations that need
regression testing when upstream updates. SmartTube's global dependency overrides
and patched AndroidX Fragment/Leanback modules are not imported.

See [validation and limitations](docs/implementation-status.md).

## Attribution

PipePipe/NewPipe notices and GPL-3.0 license are retained. SmartTube's MIT license,
custom ExoPlayer's Apache license, and Commons IO/J2V8 notices are retained in sources
and packaged under `assets/oneplayer-licenses/`. Other dependencies retain their notices.
