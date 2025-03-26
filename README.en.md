# MusicPlayerApp

A simple offline music player app written in Kotlin for Android. The app has two versions: KTMusic Bass, which uses the [BASS](https://www.un4seen.com/) library from [un4seen](https://www.un4seen.com/), and KTMusic Exo, which uses the Android [MediaPlayer](https://developer.android.com/media/media3/exoplayer) API.

## Motivation
I wanted a music app that would allow me to loop only a part of the songs I like (A-B loop), and at the same time, use the [BASS](https://www.un4seen.com/) library from [un4seen](https://www.un4seen.com/) because it plays tracks with great quality, and I like all the equalizer effects that can be implemented. But none of the apps I liked had the A-B loop repeat function, so I ended up doing more than I needed since I'm not a purist when it comes to audio player features. Still, I'm happy with what I’ve achieved and what I’ve learned, although it’s not perfect, I’ll keep improving it over time, and I hope it can be helpful to anyone who needs an example of an audio player for Android. Feel free to use the code published here as you see fit.

## Demo

## Features

- File selector
- 10-band equalizer and volume control (in the BASS version)
- 5-band equalizer (in the MediaPlayer version)
- Modes: repeat one, repeat all, and shuffle.
- A-B section repeat mode.
- Display the lyrics of the song (if available in ID3 metadata)
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

## Modules
- App
- bass (contains BASS library files)
- core
- data
- di
- features
  - audioeffects (equalizers for bass and exoplayer)
  - mfilepicker (file selector)
    
## Versions (Flavors)
- KTMusic Bass, for the following files and modules:
  - MusicPlayerService class
  - audioeffects module
- KTMusic Exo:
  - MusicPlayerService class
  - audioeffects module
    
## Used in the project
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
- [Fast scroll](https://github.com/L4Digital/FastScroll/tree/main)

## Screenshots
