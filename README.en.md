# KMusic - Music player app
<div align="left">
<img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/core/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="drawing" width="24%" height="24%"/>
</div>

<p align="center">
  
<a>[![es](https://img.shields.io/badge/lang-es-yellow.svg)](https://github.com/hall9zeha/MusicPlayerApp/blob/main/README.md) </a>
<a href="https://github.com/RetroMusicPlayer/RetroMusicPlayer" style="text-decoration:none" area-label="Min API: 21"> 
 <img src="https://img.shields.io/badge/minSdkVersion-24-green.svg"></a>
<a area_label="Work"> <img src="https://img.shields.io/badge/works_on-my_machine-yellow"></a>

A simple offline music player app written in Kotlin for Android. The app has two versions: KTMusic Bass, which uses the [BASS](https://www.un4seen.com/) library from [un4seen](https://www.un4seen.com/), and KTMusic Exo, which uses the Android [MediaPlayer](https://developer.android.com/media/media3/exoplayer) API.

## 🚀 Motivation
I wanted a music app that would allow me to loop only a part of the songs I like (A-B loop), and at the same time, use the [BASS](https://www.un4seen.com/) library from [un4seen](https://www.un4seen.com/) because it plays tracks with great quality, and I like all the equalizer effects that can be implemented. But none of the apps I liked had the A-B loop repeat function, so I ended up doing more than I needed since I'm not a purist when it comes to audio player features. Still, I'm happy with what I’ve achieved and what I’ve learned, although it’s not perfect, I’ll keep improving it over time, and I hope it can be helpful to anyone who needs an example of an audio player for Android. Feel free to use the code published here as you see fit.

## 📥 Demo
Version with Bass library [Descargar](https://github.com/hall9zeha/MusicPlayerApp/raw/main/docs/demo/Kmusic_bass_version.apk)

## :memo: Features

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
  
## ✨ Features that could be added
I think there are many, xD. One of the most interesting ones is using MediaStore to scan and list the music files. When I discovered I could use it, the project was already quite advanced, and I was a bit lazy to implement it at that point, xD. Also, I like being able to add and remove files however I want, in whatever order I choose. For now, the files or directories where the tracks are located are added manually in a very simple way, so it shouldn't be a challenge. That said, I'm considering adding a version that uses MediaStore.

## :card_file_box: Modules
- App
- bass (contains BASS library files)
- core
- data
- di
- features
  - audioeffects (equalizers for bass and exoplayer)
  - mfilepicker (file selector)
    
## 📦 Versions (Flavors)
- KTMusic Bass, for the following files and modules:
  - MusicPlayerService class
  - audioeffects module
- KTMusic Exo:
  - MusicPlayerService class
  - audioeffects module
    
## :wrench: Used in the project
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

## :framed_picture: Screenshots
||||
|--|--|--|
|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen1.jpg"  alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen2.jpg" alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen4.jpg"  alt="drawing" width="80%" height="80%"/></p>
|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen4_1.jpg"  alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen6.jpg" alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen7.jpg"  alt="drawing" width="80%" height="80%"/></p>
|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen11.jpg"  alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen11_1.jpg" alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen13.jpg"  alt="drawing" width="80%" height="80%"/></p>
|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen14.jpg"  alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen15.jpg" alt="drawing" width="80%" height="80%"/></p>|<p align="center" width="80%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen17.jpg"  alt="drawing" width="80%" height="80%"/></p>

|||
|--|--|
|||
|<p align="center" width="90%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen8.jpg"  alt="drawing" width="90%" height="90%"/></p>|<p align="center" width="90%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen9.jpg" alt="drawing" width="90%" height="90%"/></p>
## 🙏 Acknowledgments
Special thanks to [MarthaB94](https://github.com/MarthaB94) for designing the icon and the brand text for the splash screen. Her contributions have been invaluable in bringing the visual identity of the application to life.
|||
|--|--|
|[MarthaB94](https://github.com/MarthaB94)| ![](https://avatars.githubusercontent.com/u/128934015?s=48)|

## 📜 Bass Library License and Use
KMusic is free to use. In its KMusic Bass variant, it uses the Bass library from Un4seen, which is available for free for non-commercial applications. If your application is also free, you can use it at no cost. However, Bass also offers several paid license tiers for commercial applications.

For more details on the Bass library's licensing, you can check the official site of Un4seen.
