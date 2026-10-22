# Initial implementation status — 2026-10-22

## Scope

One app identity for phone/tablet/TV with manual PipePipe/SmartTube selection.
Reopening a video is sufficient; no hot swap or automatic failover is required.

## Implemented

- Public forks: `robotmanaiautomation/OneYoutubePlayer` (PipePipeClient base) and
  `robotmanaiautomation/OneYoutubePlayer-SmartTube`.
- Self-contained extractor path and pinned source submodules.
- App ID, launcher, output naming and update feed use OneYoutubePlayer identity.
- Removed inherited release/donation/remote-announcement popups from startup;
  attribution is retained in source/about/license files.
- Launcher harness accepts video URLs/IDs and opens the connected PipePipe backend.
- Engine contracts carry video identity rather than expiring stream URLs or credentials.
- Unknown backends never silently fall back. A failed shutdown retains session ownership,
  preventing another player from starting over potentially active playback.
- Repeatable SmartTube source relocation preserves upstream submodules and changes only
  a disposable generated workspace. Includes protobuf Java packages and reflection/JNI names.

## Validation

- Host: `:app:assembleDebug :playback-core:test` succeeded with four ABI split APKs.
- Five Java tests passed, including switch shutdown order, failed shutdown, unavailable
  backend behavior and URL parsing/rejection.
- Two Python source-transformation checks passed.
- Initial APK installed on Pixel9_Pro Android 34 ARM64 emulator; launcher Activity started
  successfully with no observed AndroidRuntime crash. Input reached the inherited
  MainActivity. This is not a sustained playback verification.
- Relocated SmartTube `:smarttubetv:assembleStfdroidDebug` succeeded, including relocated
  SABR protobuf generation and dex packaging. First attempt failed on protoc 3.17.3
  (bad CPU type); generated-copy adaptation to 3.25.5 resolved that build failure.
- Final OneYoutubePlayer 0.1.0 debug APK reinstalled and launched successfully.
  Opening `jNQXAC9IVRw` through the manual entry loaded "Me at the zoo" in the
  PipePipe player. The UI showed 0:06 / 0:19 and Android's media session reported
  PLAYING with the matching title. This is a short-video smoke test, not evidence
  of long-video, live-stream or cross-device stability.
- SmartTube has NOT been combined with PipePipe in one APK; runtime playback compatibility
  and phone/tablet/TV interaction remain unverified.

## Next

The minimal runtime/library boundary and dependency isolation are the next gates.
Do not apply SmartTube's global Gradle dependency force/exclude rules to the host.
Prebuilding AARs alone does not solve duplicate dependency classes.

SmartTube remains marked pending in the UI, not exposed as a working playback option.
