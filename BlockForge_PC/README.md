# BlockForge PC — raíz independiente

Esta carpeta es el proyecto de escritorio. No reemplaza ni modifica los archivos Android.

## Estado real
- El prototipo SDL2 existente vive en `BlockForge_Studio.cpp` y todavía tiene un intérprete muy limitado.
- Esta carpeta define la nueva raíz de trabajo para PC y el contrato que debe implementar el equipo.
- **Aún no existe paridad completa Android/PC ni un .exe validado.** No anunciarlo como terminado hasta compilar y ejecutar las pruebas.

## Objetivo de arquitectura
1. Mantener Android intacto.
2. Portar el núcleo real de Grafisript desde Android a `src/core/` después de identificar sus fuentes reales en el proyecto Android.
3. Separar adaptadores de plataforma: `src/platform/windows/` y `src/ui/`.
4. Compartir una misma gramática, reglas de tipos, errores y pruebas entre Android y PC. No mantener dos intérpretes divergentes.
5. Leer y guardar los mismos archivos de proyecto `.grafi`.

## Estructura prevista
```
BlockForge_PC/
  CMakeLists.txt
  README.md
  CONTRATO_GRAFISRIPT.md
  src/
    core/       # copia/adaptación del núcleo Android, pendiente de port real
    platform/   # APIs específicas de Windows
    ui/         # editor, paneles y ventanas de PC
  tests/
    casos/      # programas .grafi de prueba
```

## Compilación
Requisitos: CMake 3.24+, compilador C++20 y SDL2 (si el equipo conserva SDL2 para la interfaz).
```powershell
cmake -S BlockForge_PC -B BlockForge_PC/build
cmake --build BlockForge_PC/build --config Release
```
El ejecutable solo se considera entregable cuando el workflow de Windows termine correctamente y el .exe se pruebe en Windows limpio.

## Reglas para el equipo
- No editar ni borrar el proyecto Android para hacer el port.
- Antes de copiar, localizar el intérprete y sus dependencias Android reales; documentar cada archivo copiado y cada adaptación.
- No simular funciones no implementadas. Si una API de Android no tiene equivalente, crear un adaptador Windows o reportarla como no disponible.
- Mantener pruebas de compatibilidad con los mismos archivos .grafi en ambas plataformas.
