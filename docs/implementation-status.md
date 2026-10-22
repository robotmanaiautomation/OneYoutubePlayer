# Combined initial test build — 2026-10-22

## Implemented

- One app ID / APK contains PipePipe and SmartTube playback runtimes.
- Starts directly in PipePipe MainActivity, with its home/search/subscription UI.
- Hamburger drawer → Playback engine, remembered selection, and Reopen last video.
- YouTube video navigation from search/lists/intents uses the chosen engine before
  PipePipe stream extraction. Existing detail-player play actions also respect it.
- No engine selection prompt during normal startup or video selection.
- PipePipe uses its existing video detail/player and player services.
- SmartTube uses its real YouTubeServiceManager and custom relocated ExoPlayer,
  with DASH/SABR/HLS/direct source routing matching the upstream source preference.
- Embedded SmartTube screen has aspect-preserving video, media audio focus,
  play/pause, ±10s, back, loading timeout and visible errors. Its player and pending
  extraction subscription are released on stop; coordinator closes the previous session.
- Clean upstream submodules; generated relocation/export script and a combined build script.
- Packaged SmartTube, custom ExoPlayer, Commons IO and J2V8 license notices.

## Dependency adaptations

SmartTube's Gradle 7.5 / AGP 7.4.2 / JDK17 builds eight runtime AARs. The host's
Gradle 9.5.1 / AGP 9.2.1 / JDK25 consumes them. The full TV UI/common module and its
patched AndroidX Fragment/Leanback are excluded. Custom ExoPlayer is relocated to
`dev.oneyoutubeplayer.smarttube.exoplayer2` to coexist with PipePipe ExoPlayer 2.18.7.

The host retains its full Protobuf runtime (which includes the lite API), its
NanoJSON fork, and OkHttp 5.4.0 with matching logging/DNS-over-HTTPS/Brotli modules.
An actual first playback attempt identified a NoSuchMethodError in SmartTube's
old logging interceptor; version alignment resolved the observed failure.
These shared dependency choices need regression testing with upstream updates.
Cronet 105's split package requires `android.uniquePackageNames=false` in AGP9.

## Validation performed

Pixel9_Pro Android 34 ARM64 emulator, with no signed-in YouTube account:

- Unified ARM64 APK installed and launched.
- `jNQXAC9IVRw` (Me at the zoo) played through SmartTube / SABR: visible picture,
  positions from 0 through 18 seconds, duration 19 seconds.
- Returned to selector, chose PipePipe and opened the same ID. SmartTube logged
  session release; PipePipe displayed the matching title and 0:18 / 0:19 player.
- `aqz-KE-bpKQ` (Big Buck Bunny) played through SmartTube / SABR: visible picture,
  635-second duration, progress over a minute, and 10-second seeking.
- Corrected and visually inspected the SmartTube aspect ratio. Touch and D-pad
  focus/navigation and seek buttons were exercised on the phone emulator.
- A full `tools/build_debug.sh` run regenerated sources, exported all runtime AARs,
  assembled all four ABI APKs, and passed the five Java core tests and two Python
  relocation tests. `git diff --check` passed.

These are initial smoke checks, not proof of sustained ad-free reliability or of
fallback working during an upstream outage. No ad playback was observed in the
sample videos; long-term ad-free behavior depends on each upstream implementation.

## Current limitations

- Search/browsing metadata still uses PipePipe. Explicit background/popup/queue
  workflows retain PipePipe; SmartTube currently plays individual selected videos.
- SmartTube's full TV UI, login, background service, quality menu, SponsorBlock UI,
  casting, advanced error-recovery controllers and player-response reload handling
  are not ported. Source support alone does not claim those features.
- Real phones/tablets/TVs, TV emulator, live streams, long sessions and difficult
  codec/network/token cases have not been validated.
- No automatic fallback or mid-playback engine switching; manually reopen the video.

## Unified UI checks — 2026-10-22

- Installed the updated ARM64 APK on the Android 34 emulator; MainActivity displays
  the inherited home UI directly, without the old URL/engine entry screen.
- Opened Playback engine from the drawer, selected SmartTube and applied the change.
  Searched for jNQXAC9IVRw in the inherited search UI and selected the first result;
  SmartTube opened it as SABR with the matching 19-second duration.
- Returned to the main drawer, selected PipePipe, and used Reopen last video.
  The most recently selected video (Bloomberg live, opened during navigation testing)
  appeared in the PipePipe player with its live timeline and metadata.
- Confirmed the persisted engine preference and the selection dialog state.
- Unified the release version code across ABI/universal outputs after the first
  universal-install check exposed an inherited 100-vs-104 downgrade mismatch.
  Rebuilt universal APK then installed successfully over the existing ARM64 build.

## Test instructions

Install the matching ABI APK from `app/build/outputs/apk/debug/`, open OneYoutubePlayer,
and browse/search as in PipePipe. For manual recovery, return to the main screen,
open ☰ → Playback engine, choose the other engine, then Reopen last video.
Apply without reopening to use the choice for subsequent video selections.
