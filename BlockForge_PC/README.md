# BlockForge PC — Studio independiente

BlockForge PC vive en esta carpeta y en su propia rama. No reemplaza, modifica ni compila sobre el proyecto Android.

## Interfaz de escritorio

En Windows, el objetivo `BlockForge_PC.exe` abre **BlockForge Studio** con:
- Acciones Nuevo, Abrir, Guardar y Ejecutar.
- Editor de archivos `.grafi` y panel de salida.
- Marketplace visible como **Próximamente** (desactivada; no distrae del editor).
- Consola con errores de Grafisript y número de línea.

En sistemas que no sean Windows se conserva un ejecutor de consola para poder desarrollar y probar el núcleo.

## Estado real — trabajo en progreso

La interfaz de Windows es el primer esqueleto funcional de Studio, no el editor final. Aún faltan resaltado de sintaxis, autocompletado, árbol de proyecto, inspector, escenas, renderizador 2D/3D, importación de sprites/sonidos, sistema de eventos completo, depurador y empaquetador de juegos.

El runtime C++20 implementa actualmente variables, texto/interpolación, entrada por callback, listas de texto, condiciones básicas, bucles con límites, operaciones aritméticas básicas, comentarios y operaciones de archivos confinadas a la carpeta del proyecto. Funciones, eventos de juego, concatenación completa, expresiones generales, categorías declarativas con semántica y módulos gráficos todavía necesitan implementación y pruebas.

## Compilar en Windows

Requisitos: CMake 3.24+ y Visual Studio 2022 con el componente de desarrollo de escritorio con C++.

```powershell
cmake -S BlockForge_PC -B build -A x64
cmake --build build --config Release
ctest --test-dir build -C Release --output-on-failure
```

En Windows, CMake genera una aplicación gráfica `BlockForge_PC.exe`. El flujo de GitHub Actions también debe completar la compilación y las pruebas antes de considerar distribuible el ejecutable.

## Seguridad

- No ejecutar C++ arbitrario, comandos del sistema ni scripts externos desde Grafisript.
- Las rutas de `Guardar`, `Leer` y `Crear carpeta` se restringen al directorio del proyecto.
- Mantener límites de instrucciones, repeticiones y profundidad para evitar bucles o recursión descontrolados.
- No introducir dependencias ni cambios en Android desde esta carpeta.
