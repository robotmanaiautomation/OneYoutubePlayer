# OneYoutubePlayer

Personal Android phone/tablet/TV player based on [PipePipe](https://github.com/InfinityLoop1308/PipePipe),
with a second playback runtime being integrated from [SmartTube](https://github.com/yuliskov/SmartTube).

## Current state

- Own app ID: `dev.oneyoutubeplayer.app` (`.debug` for debug builds).
- Touch/D-pad accessible video URL/ID entry and access to PipePipe search/subscriptions.
- PipePipe is the only connected playback backend today.
- Manual-switching contract with explicit session shutdown and no silent fallback.
- SmartTube source generation isolates its ExoPlayer packages without editing upstream.
- **The unified app and the relocated SmartTube APK both build independently. SmartTube is not yet embedded in the unified APK.**

An independent build does not verify one-APK dependencies/resources/native loading,
nor successful sustained video playback. YouTube changes still require upstream fixes,
integration, testing and an app update.

## Sources

```sh
git clone --recurse-submodules https://github.com/robotmanaiautomation/OneYoutubePlayer.git
# Existing checkout:
git submodule update --init --recursive
```

`vendor/PipePipeExtractor` tracks the extractor. `vendor/SmartTube` tracks our SmartTube fork
with its own pinned submodules. Initial source commits are in [upstream-lock.json](upstream-lock.json).

## Host build

Requires JDK 25, Android `platforms;android-37.0`, build-tools 36.0.0 and `sdk.dir`
in untracked `local.properties`. The wrapper uses Gradle 9.5.1.

```sh
./gradlew :app:assembleDebug :playback-core:test
python3 -m unittest discover -s tools -p 'test_prepare_smarttube.py' -v
```

APKs: `app/build/outputs/apk/debug/OneYoutubePlayer_0.1.0-<abi>-debug.apk`.
Local SDKs/caches and generated source copies are ignored under `.local/` and `.generated/`.

## SmartTube isolation build

```sh
python3 tools/prepare_smarttube.py
cd .generated/smarttube
# Use JDK 17 here, not the host project's JDK 25.
./gradlew :smarttubetv:assembleStfdroidDebug
```

The generator copies source to `.generated/smarttube/`, rewrites ExoPlayer
Java/Kotlin/XML/reflection/protobuf/JNI package references, and writes a hash/commit report.
It replaces protoc/runtime 3.17.3 with 3.25.5 only in the generated copy to address an
Apple Silicon compiler execution failure. This adaptation still needs playback regression checks.
It writes the generated project's `local.properties` when `.local/android-sdk/` exists.

The generated project retains Gradle 7.5 / AGP 7.4.2 and builds a **separate experimental APK**.
Source submodules are not rewritten. The generated directory is disposable.

## Next integration gates

1. Select minimal SmartTube runtime modules and isolate incompatible dependencies.
2. Package both runtimes together and verify duplicate classes/resources/native libraries.
3. Connect SmartTube playback, then test manual selection and complete shutdown on devices.
4. Connect the selector to browsing and test phone/tablet/TV controls.

The current entry screen is an integration harness. Inherited browsing, background,
popup, queue and download flows still use PipePipe. Cross-device sync is not implemented.

## Attribution

PipePipe/NewPipe notices and GPL-3.0 license are retained. SmartTube's MIT license and
vendored component notices remain in its source tree and must be preserved in combined artifacts.
