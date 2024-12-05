# MusicPlayerApp

A simple offline music player app written in Kotlin for Android. The app has two versions: KTMusic Bass, which uses the [BASS](https://www.un4seen.com/) library from [un4seen](https://www.un4seen.com/), and KTMusic Exo, which uses the Android [MediaPlayer](https://developer.android.com/media/media3/exoplayer) API.

# Demo

# Features

- File selector
- 10-band equalizer and volume control (in the BASS version)
- 5-band equalizer (in the MediaPlayer version)
- Modes: repeat one, repeat all, and shuffle.
- Audio tag editor
- Add to favorites
- Filter by: Artist, Album, Genre, Favorites
- Song search by name
- Add song to favorites
- Create playlists
- Fast forward and rewind.
- Multimedia notifications.
- Media controls in lock screen mode.
- Support for Bluetooth devices
- Change album cover style to show in card or CD format.

# Modules
- App
- bass (contains BASS library files)
- core
- data
- di
- features
  - audioeffects (equalizers)
  - mfilepicker (file selector)
    
# Versions (Flavors)
- KTMusic Bass, for the following files and modules:
  - MusicPlayerService class
  - audioeffects module
- KTMusic Exo:
  - MusicPlayerService class
  - audioeffects module
    
# Used in the project
- [MVVM Architecture](https://developer.android.com/jetpack/guide)
- [ViewModel](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
- [Kotlin coroutines](https://developer.android.com/kotlin/coroutines)
- [un4seen](https://www.un4seen.com/)
- [MediaPlayer](https://developer.android.com/media/media3/exoplayer)
- [Android-splash-screen](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Glide](https://developer.android.com/training/dependency-injection/hilt-android)
- [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [DiscCoverView](https://github.com/hall9zeha/DiscCoverView)
- [Room](https://developer.android.com/jetpack/androidx/releases/room?gclid=EAIaIQobChMIh-Hoi7C_-gIVRxXUAR2kZAAsEAAYASAAEgJnivD_BwE&gclsrc=aw.ds)
- [Jaudiotagger](https://www.jthink.net/jaudiotagger/)

# Screenshots
