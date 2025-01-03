# MusicPlayerApp

Una aplicación sencilla para reproducción de música offline escrita en Kotlin para Android. La aplicación tien dos versiones: KTMusic Bass que usa librería [BASS](https://www.un4seen.com/) de [un4seen](https://www.un4seen.com/) y KTMusic Exo que usa La API [MediaPlayer](https://developer.android.com/media/media3/exoplayer) de Android.

# Demo

# Características

- Selector de archivos
- Equalizador de 10 bandas y control de volúmen (en la versión con BASS)
- Equalizador de 5 bandas (en la versión con MediaPlayer)
- Modo: repetir uno, repetir todo y aleatorio.
- Modo repetición de sección A-B
- Muestra la letra de la canción(si está disponible en los metadatos ID3)
- Editor de etiquetas de audio
- Agregar a favoritos
- Filtro por: Artista, Álbum, Género, Favoritos
- Búsqueda de canción por nombre
- Agregar canción a favoritos
- Creación de listas de reproducción
- Avance y retroceso rápido.
- Notificaciones multimedia.
- Controles multimedia en modo de bloqueo.
- Soporte para dispoditivos bluetooth
- Cambiar estilo de la cubierta del album mostrada en modo tarjeta o disco compacto.

# Módulos
- App
- bass (contiene los archivos de la librería BASS)
- core
- data
- di
- features
  - audioeffects (equalizadores)
  - mfilepicker (selector de archivos)
    
# Versiones (Sabores)
- KTMusic Bass, para los siguientes archivos y módulos:
  - Clase MusicPlayerService
  - Módulo audioeffects
- KTMusic Exo:
  - Clase MusicPlayerService
  - Módulo audioeffects
    
# Se utilizó en el proyecto
- [Arquitectura MVVM](https://developer.android.com/jetpack/guide)
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

# Capturas

