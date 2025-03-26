# MusicPlayerApp

Una aplicación sencilla para reproducción de música offline escrita en Kotlin para Android. La aplicación tien dos versiones: KMusic Bass que usa librería [BASS](https://www.un4seen.com/) de [un4seen](https://www.un4seen.com/) y KMusic Exo que usa La API [MediaPlayer](https://developer.android.com/media/media3/exoplayer) de Android.

## Motivación
Quería una aplicación de música con la cual reproducir en bucle solo una parte de las canciones que me gustan con (A-B loop) y de paso usar la librería [BASS](https://www.un4seen.com/) de [un4seen](https://www.un4seen.com/) porque reproduce las pistas con muy buena calidad y me gustan todos los efectos de ecualizador que se pueden implementar. Pero ninguna de las apps que me gustaban tenían la función de repetición en bucle A-B, así que terminé haciendo más de lo que necesitaba ya que no soy un sibarita de las funciones en un reproductor de audio. Aún así estoy contento con lo que ha resultado y lo que he aprendido, aunque no es perfecta, la iré mejorando con el tiempo y  espero que le pueda servir a cualquiera que necesite un ejemplo de reproductor de audio en Android. Siéntase libre de usar el código aquí publicado como más le convenga. 

## Demo

## Características

- Selector de archivos
- Ecualizador de 10 bandas y control de volúmen (en la versión con BASS)
- Ecualizador de 5 bandas (en la versión con MediaPlayer)
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

## Módulos
- App
- bass (contiene los archivos de la librería BASS)
- core
- data
- di
- features
  - audioeffects (ecualizadores para bass y exoplayer)
  - mfilepicker (selector de archivos)
    
## Versiones (Sabores)
- KTMusic Bass, para los siguientes archivos y módulos:
  - Clase MusicPlayerService
  - Módulo audioeffects
- KTMusic Exo:
  - Clase MusicPlayerService
  - Módulo audioeffects
    
## Se utilizó en el proyecto
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

## Capturas

