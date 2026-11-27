# El Grafisript — guía de programación general

El Grafisript es el lenguaje de programación en español integrado en BlockForge. Esta actualización amplía el intérprete que ya existía: conserva los comandos de videojuegos y añade instrucciones generales para texto, variables de texto y números, listas, decisiones, repeticiones y categorías.

> Estado: las instrucciones de esta guía corresponden a la sintaxis incorporada en esta rama. Las funciones de entrada interactiva, archivos, red, ventanas externas y módulos todavía requieren implementación aparte; no se anuncian como funciones ya disponibles.

## 1. Mostrar texto

La forma recomendada es:

```grafisript
Poner_texto: ("¡Hola, mundo!")
Mostrar "La cuenta está lista"
```

También se conserva la compatibilidad con `mostrar_mensaje` e `imprimir`.

## 2. Variables

```grafisript
Crear variable Nombre = "Ari"
Crear variable Puntos = 2 + 3
Crear variable Activo = Verdadero
Cambiar Puntos = 20
Sumar 5 a Puntos
Restar 2 a Puntos
Mostrar Nombre
Mostrar "Puntos: {Puntos}"
```

Los valores numéricos se pueden usar en operaciones y condiciones. Los valores de texto se pueden mostrar e insertar con `{Nombre}` o `${Nombre}`.

## 3. Decisiones

```grafisript
Si Puntos >= 10 {
    Poner_texto: ("¡Lo conseguiste!")
} Sino {
    Poner_texto: ("Sigue intentando")
}
```

Se admiten comparaciones `>`, `<`, `>=`, `<=`, `==`, `!=`, además de `y`, `o` y `no`.

## 4. Repeticiones

```grafisript
Repetir 3 {
    Mostrar "Hola"
}

Mientras Puntos > 0 {
    Restar 1 a Puntos
}
```

Los bucles tienen un límite de seguridad de 1000 iteraciones por ejecución para evitar que un script bloquee la aplicación.

## 5. Listas

```grafisript
Crear lista Inventario = ["Llave", "Mapa"]
Añadir "Poción" a Inventario
Quitar "Mapa" de Inventario
Mostrar Inventario
```

## 6. Funciones

```grafisript
Funcion Saludar {
    Poner_texto: ("¡Hola!")
}

Llamar Saludar
```

En esta versión las funciones no reciben parámetros ni devuelven valores.

## 7. Categorías originales

Se conserva el formato de categorías definido por el creador:

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
    Boton_inicio
    Texto_puntuacion
}
```

El intérprete registra los nombres contenidos en cada categoría. Se conserva exactamente la escritura `/Interfas/`.

## 8. Comandos de videojuegos conservados

```grafisript
crear_mundo "Mi mundo"
crear_bloque piedra 1 0 1
crear_personaje Jugador
jugador.mover(2, 1)
jugador.saltar(5)
crear_bloque2d 2 3
mover_sprite(1, 0)
camara.zoom(1.2)
camara.centrar()
fisica.activar()
fisica.pausar()
ver_bloques
ayuda
```

## 9. Reglas sencillas

- Una instrucción por línea.
- El texto se escribe entre comillas dobles.
- Los bloques comienzan con `{` y terminan con `}`.
- También se aceptan cierres en español: `Fin_si`, `Fin_repetir`, `Fin_mientras` y `Fin_funcion`.
- Los comentarios comienzan con `#`.
- Los errores de ejecución muestran la línea donde se detectó el problema.

## 10. Próximas ampliaciones, no incluidas todavía

Entrada interactiva (`Preguntar`), lectura y escritura de archivos, módulos, interfaces de ventana, conexiones de red y herramientas de datos necesitan diseño, permisos y pruebas propios. No deben confundirse con las instrucciones ya ejecutables de esta actualización.
