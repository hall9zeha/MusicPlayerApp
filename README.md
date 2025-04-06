# KMusic - Music player app

Una aplicación sencilla para reproducción de música offline escrita en Kotlin para Android. La aplicación tiene dos versiones: KMusic Bass que usa la librería [BASS](https://www.un4seen.com/) de [un4seen](https://www.un4seen.com/) y KMusic Exo que usa La API [MediaPlayer](https://developer.android.com/media/media3/exoplayer) de Android.

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

# Características que podrían ser agregadas
Creo que hay muchas xD. Una de las más interesantes es el uso de MediaStore para escanear y listar los archivos de música. Cuando descubrí que podía usarla, ya tenía el proyecto bastante avanzado y me dio un poco de pereza implementarlo en ese momento xD. Además, me gusta poder agregar y quitar archivos como quiera, en el orden que desee. Por ahora, los archivos o directorios donde están las pistas se agregan manualmente de manera muy sencilla, por lo que no debería ser un desafío. De todas formas, tengo en cuenta la posibilidad de agregar una versión que use MediaStore.

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
||||
|--|--|--|
|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen1.jpg"  alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen2.jpg" alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen4.jpg"  alt="drawing" width="70%" height="70%"/></p>
|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen4_1.jpg"  alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen6.jpg" alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen7.jpg"  alt="drawing" width="70%" height="70%"/></p>
|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen11.jpg"  alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen11_1.jpg" alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen13.jpg"  alt="drawing" width="70%" height="70%"/></p>
|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen14.jpg"  alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen15.jpg" alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen17.jpg"  alt="drawing" width="70%" height="70%"/></p>

|||
|--|--|
|Landscape|||
|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen8.jpg"  alt="drawing" width="70%" height="70%"/></p>|<p align="center" width="70%"><img src="https://github.com/hall9zeha/MusicPlayerApp/blob/main/docs/screenshots/screen9.jpg" alt="drawing" width="70%" height="70%"/></p>|

## Agradecimientos
Un agradecimiento especial a [MarthaB94](https://github.com/MarthaB94) por diseñar el ícono y el texto de marca para el splash screen. Su contribución ha sido invaluable para darle vida a la identidad visual de la aplicación.
|||
|--|--|
|[MarthaB94](https://github.com/MarthaB94)| ![](https://avatars.githubusercontent.com/u/128934015?s=48)|
## Licencia y uso de la librería Bass
KMusic es de uso gratuito. En su variante KMusic Bass, utiliza la librería Bass de [un4seen](https://www.un4seen.com/), la cual está disponible de forma gratuita para aplicaciones de uso libre. Si tu aplicación también es gratuita, puedes usarla sin ningún costo. Sin embargo, Bass ofrece también varios niveles de licencias de pago para aplicaciones comerciales.

Para más detalles sobre la licencia de la librería Bass, puedes consultar el sitio oficial de [un4seen](https://www.un4seen.com/).

