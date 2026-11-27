# BlockForge PC — base de escritorio independiente

Esta carpeta es una raíz separada para Windows. No reemplaza ni modifica el proyecto Android.

## Estado honesto de esta entrega

Esta rama ya contiene un **núcleo inicial de Grafisript en C++20**, un ejecutable de consola para correr archivos `.grafi`, pruebas automatizadas y un flujo de compilación Windows.

**Todavía no es BlockForge Studio completo ni el motor gráfico final.** Aún faltan el editor visual de escenas, renderizado integrado con sprites/sonidos, inspector, importador de recursos, sistema completo de eventos y funciones, módulos de plataforma y la paridad completa con Android.

## Lo que ya implementa el núcleo PC

- Variables: `Crear variable`, `Cambiar`, `Sumar`, `Restar`.
- Texto: `Poner_texto:`, `Mostrar` e interpolación `{Variable}`.
- Entrada: `Preguntar ... y guardar en ...`.
- Listas: crear, añadir y quitar elementos.
- Condiciones: `Si`, `Si_no`, `Fin_si`, comparaciones y lógica básica.
- Bucles: `Repetir` y `Mientras`, con límites de seguridad.
- Archivos: `Guardar`, `Leer` y `Crear carpeta`, restringidos a la carpeta del proyecto.
- Comentarios con `#` y mensajes de error en español con número de línea.

Las funciones, eventos, automatización avanzada y módulos gráficos/sonoros no están implementados todavía. No se deben anunciar como disponibles.

## Compilar en Windows

Requisitos: CMake 3.24+ y Visual Studio 2022 con herramientas C++.

```powershell
cmake -S BlockForge_PC -B build -A x64
cmake --build build --config Release
ctest --test-dir build -C Release --output-on-failure
```

El workflow `.github/workflows/build-windows.yml` compila, ejecuta las pruebas y publica un ZIP como artefacto de GitHub Actions si todo pasa.

## Ejecutar un proyecto

```powershell
.`build`\Release\BlockForge_PC.exe BlockForge_PC\ejemplos\primer_proyecto.grafi
```

## Reglas de seguridad y arquitectura

- No editar ni borrar Android al desarrollar PC.
- Mantener el núcleo del lenguaje separado de la interfaz gráfica.
- No ejecutar rutas de archivos fuera de la carpeta del proyecto.
- No evaluar código C++ arbitrario ni ejecutar comandos del sistema desde Grafisript.
- Toda nueva función del lenguaje debe incluir pruebas.
