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

## Primer bloque del creador 2D

Se añadió un modelo de escena 2D independiente de la interfaz y conectado a Grafisript:
- Escenas y objetos con posición, tamaño y color.
- Velocidad, gravedad y física básica.
- Colisiones rectangulares (AABB) y objetos sólidos.
- Cámara que sigue a un objeto.
- Ruta de sprite guardada como recurso del objeto (la carga y el dibujo real de texturas aún están pendientes).
- Vista previa nativa de Windows con movimiento de Jugador mediante A/D o flechas y salto con Espacio.
- Selección y arrastre de objetos con el ratón; al cerrar la vista, sus posiciones se escriben de vuelta en las líneas `Posicion` del editor para poder guardarlas.

### Sintaxis 2D disponible en esta etapa

```text
Crear escena "Aventura"
Crear objeto Jugador
Posicion Jugador = 80, 80
Tamaño Jugador = 32, 48
Color Jugador = #42A5F5
Fisica Jugador = Verdadero
Camara seguir Jugador

Crear objeto Suelo
Posicion Suelo = 0, 360
Tamaño Suelo = 900, 48
Solido Suelo = Verdadero
Gravedad = 980
```

Ejemplo completo: `BlockForge_PC/ejemplos/aventura_2d.grafi`.

**Limitaciones actuales del 2D:** el render de vista previa dibuja rectángulos de color; todavía no carga PNG/JPG como texturas. La vista de escena permite selección y arrastre básicos, pero todavía no hay un panel de jerarquía/inspector completo, tilesets, animaciones, audio, partículas, iluminación, exportador de juego ni eventos de colisión programables. La física es inicial y debe ampliarse y probarse antes de producción.
