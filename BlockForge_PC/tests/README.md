# Pruebas de compatibilidad Grafisript

Crear aquí archivos .grafi de prueba y ejecutarlos en Android y Windows con el mismo resultado esperado.

Lista mínima:
1. Categorías /Enemigo/, /Dinero/ y /Interfas/; comprobar que se preservan al guardar.
2. Poner_texto y sustitución de {Vida}.
3. Variables enteras/decimales/texto/booleanos y operaciones.
4. Si / Si_no / Fin_si, incluidos bloques anidados.
5. Repetir y Mientras con límites de seguridad.
6. Listas: crear, añadir, quitar, indexación y lista vacía.
7. Funciones con parámetros y Devolver.
8. Registro y disparo de eventos del motor.
9. Leer/guardar archivos dentro de la carpeta del proyecto y rechazar rutas que escapen.
10. Errores de sintaxis y tipos con número de línea.

No marcar una prueba como aprobada si solo compila: ejecutar el mismo caso en Android y PC y comparar salida/estado.
