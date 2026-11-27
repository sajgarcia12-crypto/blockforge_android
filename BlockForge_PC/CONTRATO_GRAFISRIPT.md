# Contrato del lenguaje Grafisript (base de compatibilidad)

Este documento reúne las construcciones definidas en la guía de Grafisript. Especifica el comportamiento esperado; **no significa que todas estén implementadas todavía**.

## 1. Categorías / bloques de datos
Conservar las barras y el bloque único de llaves. Los nombres dentro son elementos de la categoría.

```grafisript
/Enemigo/
{
    Guardia
    Jefe
}

/Dinero/
{
    Monedas
    Gemas
}

/Interfas/
{
    Texto_puntuacion
    Boton_inicio
}
```
Aceptar `/Interfaz/` como alias opcional, pero preservar `/Interfas/` al guardar archivos antiguos.

## 2. Texto
```grafisript
Poner_texto: ("¡Hola, mundo!")
Poner_texto: ("Vida: {Vida}")
```
Las comillas no ejecutan por sí solas. La interpolación `{NombreVariable}` se resuelve en tiempo de ejecución.

## 3. Variables y tipos
```grafisript
Crear variable Vida = 100
Cambiar Vida = 80
Sumar 10 a Vida
Restar 5 a Vida
```
Tipos mínimos: texto, entero, decimal, booleano (Verdadero/Falso), lista y vacío. Detectar errores de tipo con número de línea.

## 4. Entrada, salida y listas
```grafisript
Preguntar "¿Cómo te llamas?" y guardar en Nombre
Mostrar Nombre
Crear lista Inventario = ["Llave", "Mapa", "Poción"]
Añadir "Moneda" a Inventario
Quitar "Mapa" de Inventario
```
La entrada debe usar un adaptador de interfaz; nunca bloquear el hilo de renderizado.

## 5. Condiciones y bucles
```grafisript
Si Vida > 0 entonces
    Poner_texto: ("Sigues con vida")
Si_no
    Poner_texto: ("Fin")
Fin_si

Repetir 5 veces
    Sumar 1 a Vida
Fin_repetir

Mientras Vida > 0
    Restar 1 a Vida
Fin_mientras
```
Soportar comparaciones =, >, <, >=, <= y operadores lógicos y, o, no. Añadir límite configurable de iteraciones/tiempo para evitar bloqueos.

## 6. Funciones
```grafisript
Funcion Duplicar Numero
    Devolver Numero * 2
Fin_funcion
```
Soportar parámetros, ámbito local y Devolver. Informar de funciones inexistentes y argumentos incorrectos.

## 7. Eventos
Eventos documentados: `Al_iniciar`, `Al_pulsar`, `Al_cambiar`, `Al_recibir_dato`, `Al_terminar`, `Al_ocurrir_error`, `Al_matar_enemigos`.
También conservar compatibilidad con los nombres históricos `Poner_texto_al_matar_enemigos` y `Poner_texto_al_conseguir_cosas_del_enemigo` si aparecen en proyectos.
Los eventos se registran como manejadores y los dispara el motor; no se ejecutan inmediatamente al cargar el script.

## 8. Archivos, módulos y datos
```grafisript
Guardar Nombre en "usuario.txt"
Leer "usuario.txt" y guardar en Nombre
Crear carpeta "Datos"
Usar modulo Matematicas
Sumar_todo(Ventas)
```
Restringir rutas al directorio del proyecto (bloquear .., rutas absolutas no autorizadas y enlaces fuera del proyecto). Los módulos Archivos, Ventanas, Internet, Sonido, Imágenes, Datos y Juegos requieren APIs reales; declarar explícitamente cualquier módulo aún no disponible.

## 9. Matemáticas y comentarios
Operadores: +, -, *, /. Funciones previstas: raíz cuadrada y redondear. Comentarios con #. Cierres de bloques: Fin_si, Fin_repetir, Fin_mientras, Fin_funcion, Fin_evento y Fin_para.

## 10. Automatización
```grafisript
Para cada Archivo en Carpeta "Informes"
    Mostrar Archivo
Fin_para
```

## 11. Errores
Cada error debe indicar archivo y línea, describir el problema en español y sugerir una corrección cuando sea posible. No continuar ejecutando tras un error fatal.
