# BlockForge para PC (Windows x64)

Esta carpeta añade el primer objetivo de escritorio de BlockForge usando el motor C++ y SDL2 del repositorio.

## Compilación local

1. Instala CMake, Visual Studio 2022 con C++ y Git.
2. Clona SDL2:
   `git clone --depth 1 --branch release-2.32.10 https://github.com/libsdl-org/SDL.git third_party/SDL`
3. Configura:
   `cmake -S desktop -B build -DSDL2_SOURCE_DIR="$PWD/third_party/SDL" -DCMAKE_BUILD_TYPE=Release`
4. Compila:
   `cmake --build build --config Release --parallel`

El ejecutable se genera en `build/package/BlockForge.exe` (la ruta final puede variar según el generador de CMake).

## Paquete de Windows

El flujo de GitHub Actions compila Windows x64, incluye `SDL2.dll` y publica `BlockForge-Windows-x64.zip` como artefacto descargable.

## Estado real de esta primera versión

- Es un primer ejecutable de escritorio basado en el motor C++/SDL2 existente.
- Mantiene el prototipo actual del mundo de bloques y los comandos Grafisript que ya reconoce ese motor.
- Todavía no equivale a una versión PC completa ni a una paridad funcional con todas las pantallas Android.
- La interfaz SDL2 actual es experimental; el siguiente trabajo es construir la interfaz de escritorio (paneles, editor, explorador de proyectos, inspector, atajos y escalado) y ampliar el intérprete compartido.
- No se eliminan límites físicos del equipo: PC puede aprovechar más memoria, CPU, GPU y almacenamiento disponibles, pero el programa debe gestionar esos recursos correctamente.

## Dirección multiplataforma

La meta es mantener el motor y Grafisript en C++ compartidos, con capas de interfaz y entrada adaptadas a PC y Android. No se debe mantener una versión distinta del lenguaje por plataforma.
