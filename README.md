# Personal TV Home

A native Android TV / Google TV launcher with a **Scenic Glass** look: full-screen photography, a floating translucent glass app dock, a live clock and weather header, and an Apple-style Now Playing player for Spotify, YouTube Music and SoundCloud.

Built with Kotlin, Jetpack Compose and Compose for TV.

## Features

- **Personal header**: a welcome line, your name, a live clock and the date, with an Apple Weather–style widget alongside (place, temperature, condition, high/low and the next few hours; Open-Meteo, no API key needed).
- **Glass app dock**: five favourite apps plus All Apps. Hold OK (or press Menu) on an app to reorder, remove or add it.
- **Now Playing**: an Apple-style player for Spotify, YouTube Music, SoundCloud and music cast to the TV (Google Cast / Spotify Connect). It shows the artwork, title, artist, the source app's icon, elapsed and remaining time, and playback controls, read from Android media sessions. The full-screen view sits on a frosted version of your wallpaper.
- **Player idle mode**: after five seconds without remote input, the full-screen player fades its playback controls and adjusts the layout, both in normal and Party Mode. Press a remote key to reveal the controls again.
- **Lyrics**: synced lyrics from [LRCLIB](https://lrclib.net). The widget shows the line being sung; the full-screen player shows Apple Music–style scrolling lyrics. Turn them on or off with the lyrics button in the player or in **Appearance → Lyrics**.
- **Wallpapers**: a set of Unsplash photos that change automatically every 10 minutes (you can turn this off). Photos are cached on the TV.
- **Appearance**: glass tint, opacity, blur and dock size.
- **Settings panel**: shortcuts to the TV's own display, sound, network, Chromecast and default-home settings.

## Requirements

- An Android TV or Google TV device running Android 8.0 (API 26) or newer.
- A computer with [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) (`adb`) on the same network as the TV.
- To build from source, you also need JDK 17 and the Android SDK (platform 35).

## 1. Prepare the TV

1. On the TV, open **Settings → Device Preferences → About** and select **Build** 7 times to turn on Developer options.
2. Open **Settings → Device Preferences → Developer options** and turn on **USB debugging** (on some TVs it's called **Network debugging** or **ADB debugging**).
3. Find the TV's IP address in **Settings → Network & Internet → (your network)**.

## 2. Get the code

```bash
git clone https://github.com/SmithNathaniel/PersonalTVHome.git
cd PersonalTVHome
```

## 3. Build

On macOS, you can install the tools with Homebrew:

```bash
brew install openjdk@17 android-platform-tools
brew install --cask android-commandlinetools
export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
sdkmanager --sdk_root="$ANDROID_HOME" "platforms;android-35" "build-tools;35.0.0"
```

On any OS, point Gradle at your Android SDK, then build:

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

You can also open the project in Android Studio and run it on the TV directly.

## 4. Install on the TV

```bash
adb connect <TV-IP>:5555          # accept the "Allow debugging?" prompt on the TV
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.nathaniel.tvhome/.MainActivity
```

## 5. Allow Now Playing

Android only lets a launcher read music info after you give it notification access. Do either of these:

- On the TV, open the launcher and go to **Settings (gear) → Now Playing access**, then allow **Personal TV Home**.
- Or from your computer:

  ```bash
  adb shell cmd notification allow_listener com.nathaniel.tvhome/com.nathaniel.tvhome.media.MediaListenerService
  ```

## 6. Make it your home screen

- On the TV, open the launcher and go to **Settings (gear) → Default home app**, then pick **Personal TV Home**.
- Or from your computer:

  ```bash
  adb shell cmd package set-home-activity com.nathaniel.tvhome/.MainActivity
  ```

To go back to the original launcher, choose it again under **Default home app**, or uninstall:

```bash
adb uninstall com.nathaniel.tvhome
```

> Some Google TV builds always send the Home button to the stock launcher. On those devices, open Personal TV Home from the apps row instead.

## Make it yours

The launcher is personalised in code. Change these before you build:

| What | Where |
|---|---|
| Name in the header | `OWNER_NAME` in [`ui/home/PersonalHeader.kt`](app/src/main/java/com/nathaniel/tvhome/ui/home/PersonalHeader.kt) |
| Default dock apps | `FavouriteApps.defaults` in [`apps/LauncherApp.kt`](app/src/main/java/com/nathaniel/tvhome/apps/LauncherApp.kt) |
| Wallpaper photos | `WallpaperCatalog.all` in [`wallpaper/Wallpaper.kt`](app/src/main/java/com/nathaniel/tvhome/wallpaper/Wallpaper.kt). Use the `photo-…` ID from an `images.unsplash.com` URL. |
| Wallpaper change interval | `WALLPAPER_INTERVAL_MS` in [`TvHomeApplication.kt`](app/src/main/java/com/nathaniel/tvhome/TvHomeApplication.kt) |
| Default weather location | `WeatherLocation.Johannesburg` in [`prefs/UserPreferences.kt`](app/src/main/java/com/nathaniel/tvhome/prefs/UserPreferences.kt). You can also change it in Settings → Weather location. |

## Troubleshooting

- **`adb connect` fails**: make sure the computer and TV are on the same network and debugging is turned on. Some TVs use port `5555`; others show the port next to the debugging setting.
- **"Enable Now Playing" stays visible**: allow notification access (step 5), then reopen the launcher.
- **Wallpaper shows a plain landscape**: the TV couldn't download the photo. It will show once the TV is online.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**: an older copy was signed with a different key. Run `adb uninstall com.nathaniel.tvhome`, then install again.

## Privacy and security

- All network traffic is HTTPS-only (plain HTTP is blocked by the app's network security config).
- Now Playing reads only media sessions from known music apps. It never stores notification content.
- Artwork downloads are size-capped and downsampled before display.
- When lyrics are on, only the song's title, artist, album and length are sent to LRCLIB to find them. Turn lyrics off to stop this.
- App data is excluded from Android backups. No accounts, API keys or analytics are used.

## Credits

- Photography from [Unsplash](https://unsplash.com) contributors.
- Weather data from [Open-Meteo](https://open-meteo.com).
- Lyrics from [LRCLIB](https://lrclib.net), a free and open lyrics database.
- [Inter](https://rsms.me/inter/) typeface by Rasmus Andersson, under the SIL Open Font License 1.1 ([licence](third_party/inter/LICENSE.txt)).
