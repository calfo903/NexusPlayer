# NexusPlayer

The Ultimate Modern Android Video Player combining the raw playback power and advanced codec control of VLC Media Player with the ultra-smooth, premium gesture interactions of MX Player Pro.

## Features

- **Playback Engine** — Hardware-accelerated ExoPlayer (Media3) with multi-container support (MP4, MKV, HLS, DASH, RTSP)
- **Audio Equalizer** — VLC-style 5-band equalizer with bass boost, virtualizer, and up to 200% loudness enhancement
- **Gesture Controls** — MX Player-style double-tap seek, vertical swipe for brightness/volume, horizontal scrubbing, pinch-to-zoom, and two-finger speed control
- **Subtitle Engine** — External subtitle loading (SRT/VTT/ASS), timing offset, font customization, and Whisper AI auto-generation
- **Auto-Next Episode** — Smart TV series detection (S01E04 → S01E05) with countdown prompt
- **Picture-in-Picture** — System PiP and in-app floating window with configurable aspect ratios
- **Chromecast / DLNA** — Cast support via Media3 Cast library
- **Bookmarks & Resume** — Save playback position and scene bookmarks per video
- **Playlist Management** — Create, populate, and play video playlists
- **Network Streams** — Open and play RTMP, HTTP, and HLS network URLs

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.23 |
| UI | Jetpack Compose (Material 3, Compose BOM 2024.06) |
| Media | AndroidX Media3 (ExoPlayer) 1.3.1 |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 |
| Networking | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Images | Coil 2.6.0 |
| Build | Gradle 8.7, AGP 8.4.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Project Structure

```
app/src/main/java/com/nexusplayer/app/
├── data/          # Room DB, DAOs, Retrofit API, repository implementations
├── domain/        # Models (VideoItem, Playlist, Settings) and repository interfaces
├── player/        # NexusVideoPlayer engine, gestures, whisper AI, services
├── ui/            # Compose screens (library, player, dialogs, theme)
└── util/          # Sleep timer, screenshot helper, auto-next detector
```

## Build

```bash
# Set Android SDK path
export ANDROID_HOME=/path/to/android/sdk

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```
