BLOCKFORGE STUDIO - PROYECTO ANDROID INICIAL
============================================

Este ZIP contiene:
- El código C++ actual de BlockForge.
- El Grafisript integrado en ese código.
- Configuración inicial Android/Gradle/CMake.
- Actividad Android que usa el puente SDL2.

IMPORTANTE:
Este ZIP todavía NO es un APK y no puede compilar hasta agregar SDL2.
Cxxdroid compila y prueba el C++ normal, pero no reemplaza Android SDK,
Android NDK, CMake, Gradle ni el puente Java de SDL2.

PASO OBLIGATORIO: agregar SDL2
1. Descarga el código fuente oficial de SDL2 desde:
   https://github.com/libsdl-org/SDL/releases
2. Extrae el paquete.
3. Copia la carpeta raíz de SDL2 dentro de:
   app/src/main/cpp/SDL
   Debe quedar este archivo:
   app/src/main/cpp/SDL/CMakeLists.txt
4. Copia también la carpeta Java del proyecto oficial SDL2:
   desde SDL/android-project/app/src/main/java/org/libsdl/app
   hacia:
   app/src/main/java/org/libsdl/app
   Debe quedar SDLActivity.java y las demás clases de SDL.

Después:
- Abre la carpeta raíz BlockForge_Android en Android Studio.
- Instala/acepta Android SDK 35, NDK y CMake 3.22.1 desde SDK Manager.
- Sincroniza Gradle y ejecuta Build > Build APK(s).

El APK de depuración, si compila, aparecerá en:
app/build/outputs/apk/debug/app-debug.apk

NOTAS TÉCNICAS:
- El paquete está configurado para arm64-v8a y armeabi-v7a.
- La app se inicia en horizontal.
- El código usa SDL2.
- No se afirma que el proyecto haya sido compilado: falta integrar SDL2 y
  probarlo con el SDK/NDK reales.
- El script mundo.grafi del prototipo usa lectura de archivo local con
  std::ifstream; en Android habrá que adaptar la carga a SDL_RWops/almacenamiento
  de la app antes de poder leer scripts externos desde el APK.
