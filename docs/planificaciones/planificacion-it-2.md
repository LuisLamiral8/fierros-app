# Iteración 2 — Diseño de la web de Fierros

**Estado:** en planificación. Este documento es un **brief de diseño**: está
escrito para que un LLM (o un diseñador) arme las maquetas de la web completa.
Solo describe cómo se ve cada pantalla. La implementación (servidor, datos
reales, login funcionando) es otra etapa.

---

## 1. Qué tenés que entregar

- **Maquetas estáticas en HTML + CSS**, una página por pantalla, con los datos de
  ejemplo de la sección 10.
- **Sin JavaScript y sin interacción.** Los botones, pestañas y menús se dibujan
  en su estado de reposo (una pestaña activa, el menú del usuario abierto en una
  maqueta aparte, etc.). Nada tiene que funcionar.
- **Sin frameworks ni librerías de componentes** (nada de Bootstrap, Tailwind,
  React). HTML semántico y **una sola hoja de estilos compartida**, con los
  tokens de la sección 3 como variables CSS. Así después se reutiliza tal cual al
  implementar.
- **Todos los textos en español rioplatense** (voseo: "Pegá", "Tocá", "Ingresá"),
  exactamente como aparecen en este documento.
- **Responsive:** cada pantalla tiene que verse bien en escritorio (1280 px),
  tablet (768 px) y celular (390 px). La sección 8 dice qué cambia en cada una.

Archivos, en `docs/diseno-web/`:

```
docs/diseno-web/
├── estilos.css
├── login.html
├── rutina.html                 ← pantalla principal
├── rutina-vacia.html           ← sin rutina cargada
├── rutina-menu-usuario.html    ← la principal con el menú del usuario abierto
├── ejercicio.html              ← detalle de un ejercicio (lo que levanté)
├── actualizar-json.html
├── actualizar-json-error.html
├── actualizar-formulario.html
├── anteriores.html             ← rutinas anteriores + actividad
├── anterior-detalle.html       ← una rutina anterior, solo lectura
└── estados.html                ← cargando, sin conexión, aviso "¡Actualizado!"
```

El logo está en `assets/logo-render.png` (500 × 500, negro sobre un círculo
blanco). Referencialo con una ruta relativa desde `docs/diseno-web/`.

---

## 2. Contexto: qué es Fierros

Fierros es una app para el **reloj** (Samsung Galaxy Watch, Wear OS) que muestra
la rutina de gimnasio que arma mi entrenador, para ir al gimnasio sin el
celular. La rutina vive en un servidor propio en casa (homelab). El reloj la baja
cuando está en el Wi-Fi de casa y la usa offline.

La **web** es el panel de control de ese servidor, y se usa desde la compu o el
celular, en casa:

- ver la rutina actual, semana por semana, con **cuánto levanté** en cada
  ejercicio (eso lo anota el reloj);
- **actualizar la rutina**: pegando un JSON (el camino rápido, lo arma un LLM a
  partir de capturas de la app del entrenador) o con un formulario;
- ver las **rutinas anteriores** y la **actividad** del servidor (cuándo se cargó
  una rutina, cuándo la bajó el reloj, qué se rechazó);
- en el futuro, **usuarios**: por eso hay login y un navbar con el usuario, que
  por ahora son solo diseño.

**Principio de diseño: la menor fricción posible.** Una persona que viene de
entrenar tiene que poder ver lo que necesita en un vistazo y actualizar la rutina
en dos pasos. Pocas pantallas, pocos botones, textos cortos, nada de adornos que
no informen.

**Estructura de una rutina** (para entender los datos):

- Un **mesociclo** tiene semanas (normalmente 4).
- Cada **semana** tiene días (normalmente 4). Un día puede tener nombre ("Full
  Body").
- Cada **día** tiene ejercicios en orden. Cada ejercicio tiene:
  - **letra**: "A", "B"... Las letras con número ("A1", "A2") son una
    **superserie**: ejercicios que se hacen seguidos, y conviene que se vean
    agrupados;
  - **nombre**;
  - **series** (un número) y **reps** (texto: "20", "10-15", "20-12-10-8-6",
    "20 PASOS", "fallo");
  - **peso** del plan (texto: "0 kg", "12 kg c/mano");
  - **nota** opcional ("por pierna").
- **Levanté**: el peso que registré en el reloj para ese ejercicio. **En esta
  iteración siempre vale "0 kg"**, pero el diseño tiene que contemplarlo como un
  dato real e importante.

---

## 3. Identidad visual

La web tiene que sentirse **de la misma familia que la app del reloj**: fondo
oscuro, botones redondeados color lavanda con texto oscuro, avisos grises. Tema
**oscuro únicamente**.

### Colores (variables CSS)

| Token | Valor | Uso |
|---|---|---|
| `--fondo` | `#0F0F12` | Fondo de la página |
| `--superficie` | `#1C1B1F` | Tarjetas, navbar, tablas |
| `--superficie-alta` | `#2B2930` | Hover de filas, inputs, avisos |
| `--borde` | `#3A3740` | Bordes y separadores |
| `--texto` | `#E6E1E5` | Texto principal |
| `--texto-suave` | `#A8A2AE` | Texto secundario, notas, etiquetas |
| `--primario` | `#D0BCFF` | Botón principal, pestaña activa, letra del ejercicio |
| `--sobre-primario` | `#21005D` | Texto sobre `--primario` |
| `--primario-tenue` | `#4F378B` | Fondo de la pestaña activa, chips |
| `--exito` | `#7DD3A0` | Validación correcta |
| `--error` | `#F2B8B5` | Errores (texto) |
| `--error-fondo` | `#3B1D1D` | Fondo de los mensajes de error |

El logo es blanco y negro: sobre el fondo oscuro va dentro de su círculo blanco,
sin tocarlo.

### Tipografía

- **Roboto** (la de Android) para todo; **Roboto Mono** para el JSON. Con
  `system-ui` y `monospace` como respaldo.
- Tamaños: título de página 32 px / 600; título de sección 22 px / 600; título
  de día 18 px / 600; texto 16 px / 400; secundario 14 px / 400; etiquetas 12 px
  / 500 en mayúsculas con espaciado.
- Números (series, reps, pesos) con `font-variant-numeric: tabular-nums`, para
  que las columnas queden alineadas.

### Forma y espacio

- Grilla de 8 px. Márgenes laterales: 32 px en escritorio, 16 px en celular.
- Contenido centrado, ancho máximo 1040 px.
- **Botones en forma de píldora** (borde totalmente redondeado), como en el
  reloj. Alto 48 px, texto 16 px / 500.
- Tarjetas con radio de 16 px; inputs con radio de 12 px.
- Sombras casi nulas: la jerarquía la dan los tonos de superficie, no las
  sombras.

### Íconos

Material Symbols Outlined (o SVG equivalentes), trazo fino, 20–24 px. Solo donde
ayudan: menú, usuario, flecha de volver, papelera, más, copiar, check, error.

---

## 4. Componentes

Diseñalos una vez y reutilizalos en todas las pantallas.

- **Navbar** (sección 5.2).
- **Botón principal:** fondo `--primario`, texto `--sobre-primario`.
- **Botón secundario:** fondo transparente, borde 1 px `--borde`, texto
  `--texto`.
- **Botón deshabilitado:** el principal al 38 % de opacidad.
- **Botón de texto:** sin fondo ni borde, color `--primario` ("+ Ejercicio",
  "← Volver").
- **Pestañas de semana:** fila de píldoras "Semana 1", "Semana 2"... La activa
  con fondo `--primario-tenue` y texto `--texto`; las demás, texto
  `--texto-suave` sin fondo. En celular la fila se desplaza de costado.
- **Tabla de día** (sección 6.3).
- **Chip de estado:** píldora chica con ícono y texto ("✔ JSON válido",
  "✖ Hay un error").
- **Mensaje de error:** caja con fondo `--error-fondo`, borde izquierdo de 4 px
  `--error`, texto `--error`.
- **Aviso "¡Actualizado!":** píldora con fondo `--superficie-alta` y texto
  `--texto`, abajo al centro de la pantalla, flotando sobre el contenido. Es el
  equivalente web del aviso del reloj.
- **Avatar:** círculo de 36 px con la inicial del usuario ("L"), fondo
  `--primario-tenue`.
- **Inputs:** fondo `--superficie-alta`, borde 1 px `--borde`, alto 44 px. Con
  foco: borde `--primario`. Con error: borde `--error` y el mensaje debajo en
  14 px.

---

## 5. Pantallas de acceso y marco

### 5.1 Login (`login.html`)

Solo diseño: por ahora la web no tiene usuarios.

```
┌──────────────────────────────────────────────┐
│                                              │
│                  (  logo  )                  │
│                   Fierros                    │
│        Tu rutina de gimnasio, en casa        │
│                                              │
│   ┌──────────────────────────────────────┐   │
│   │ Usuario                              │   │
│   │ [                                  ] │   │
│   │ Contraseña                           │   │
│   │ [                                  ] │   │
│   │                                      │   │
│   │ (         Ingresar                 ) │   │
│   └──────────────────────────────────────┘   │
│                                              │
└──────────────────────────────────────────────┘
```

- Todo centrado vertical y horizontalmente, en una tarjeta de 400 px de ancho.
- Logo de 96 px arriba, "Fierros" (32 px) y el subtítulo en `--texto-suave`.
- Botón **Ingresar** principal, a todo el ancho de la tarjeta.
- Sin "Crear cuenta" ni "Olvidé mi contraseña" por ahora.
- Dibujá también, debajo del botón, cómo se vería el error "Usuario o contraseña
  incorrectos" (con el componente de mensaje de error), para tenerlo diseñado.

### 5.2 Navbar (en todas las pantallas menos el login)

```
┌──────────────────────────────────────────────────────────────────┐
│ (logo) Fierros    Rutina   Actualizar   Anteriores       (L) Luis ▾ │
└──────────────────────────────────────────────────────────────────┘
```

- Alto 64 px, fondo `--superficie`, borde inferior `--borde`. Fija arriba.
- Izquierda: logo de 32 px y "Fierros" (18 px / 600).
- Centro-izquierda: tres links: **Rutina**, **Actualizar**, **Anteriores**. El de
  la pantalla actual en `--texto` con una línea de 2 px `--primario` debajo; los
  otros en `--texto-suave`.
- Derecha: avatar con la inicial, nombre "Luis" y una flecha ▾.
- **Menú del usuario** (en `rutina-menu-usuario.html`, abierto): tarjeta chica
  debajo del avatar con "Luis" y "luis@ejemplo.com" arriba, un separador, y dos
  opciones: **Mi cuenta** y **Cerrar sesión**.
- En celular: logo + "Fierros" a la izquierda, avatar a la derecha, y los tres
  links pasan a una barra fija abajo con ícono + texto (Rutina, Actualizar,
  Anteriores).

---

## 6. Pantalla principal: rutina actual (`rutina.html`)

Es la pantalla que más se usa. En orden, de arriba a abajo:

```
┌──────────────────────────────── navbar ─────────────────────────────┐
│                                                                     │
│                          Rutina actual                              │
│                4 semanas · 16 días · 112 ejercicios                 │
│             Actualizada el jueves 11/09/2026 a las 02:35            │
│                                                                     │
│          (  Actualizar rutina  )   ( Ver rutinas anteriores )       │
│                                                                     │
│   ( Semana 1 )  Semana 2   Semana 3   Semana 4                      │
│                                                                     │
│   ┌─ Día 1 · Full Body ─────────────────────────────────────────┐   │
│   │      EJERCICIO                    SERIES        PESO  LEVANTÉ│   │
│   │ ┃A1  Dislocaciones con banda      3 × 20        0 kg   0 kg  │   │
│   │ ┃A2  Puente de gluteos isometrico 3 × 20        0 kg   0 kg  │   │
│   │      + pull apart                                            │   │
│   │  B   Db floor press               5 × 20-12-    0 kg   0 kg  │   │
│   │                                   10-8-6                     │   │
│   │  ...                                                         │   │
│   └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─ Día 2 · Full Body ─────────────────────────────────────────┐   │
│   │  ...                                                         │   │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.1 Encabezado (el texto del medio)

- Centrado. "Rutina actual" (32 px / 600).
- Debajo, en `--texto-suave`: el resumen ("4 semanas · 16 días · 112 ejercicios")
  y, en otra línea, "Actualizada el jueves 11/09/2026 a las 02:35".

### 6.2 Botones

- Centrados debajo del encabezado, uno al lado del otro, con 16 px de separación.
- **Actualizar rutina** (principal) y **Ver rutinas anteriores** (secundario).
- En celular, uno debajo del otro, a todo el ancho.

### 6.3 Pestañas y tablas de días

- Las pestañas de semana (componente de la sección 4), alineadas a la izquierda
  del contenido. **Semana 1 activa.**
- Debajo, **una tarjeta por día** de la semana activa, una debajo de la otra, con
  24 px de separación.
- Título de la tarjeta: "Día 1" en `--texto` y " · Full Body" en `--texto-suave`.
  Si el día no tiene nombre, solo "Día 1".
- Tabla con estas columnas:

  | Columna | Contenido | Estilo |
  |---|---|---|
  | (sin título) | La letra: "A1", "B" | 16 px / 700, color `--primario`, ancho fijo 48 px |
  | Ejercicio | Nombre; la nota debajo, en 14 px `--texto-suave` ("por pierna") | Ocupa el espacio que sobra; el nombre puede ir en dos líneas |
  | Series | "3 × 20". Las reps largas pasan a la línea de abajo sin cortar los números | Alineado a la izquierda, tabular |
  | Peso | El peso del plan ("0 kg") | `--texto-suave`, alineado a la derecha |
  | Levanté | Lo registrado en el reloj ("0 kg") | **16 px / 600 `--texto`**, alineado a la derecha: es el dato que más interesa |

- Encabezados de columna con el estilo de etiqueta (12 px mayúsculas,
  `--texto-suave`).
- **Superseries agrupadas:** los ejercicios que comparten letra con número (A1,
  A2) llevan una barra vertical de 3 px `--primario-tenue` a la izquierda que los
  une, y no tienen separador entre ellos. El resto de las filas se separan con
  una línea `--borde`.
- Toda la fila de un ejercicio parece un link: al pasar el mouse cambia a
  `--superficie-alta` y aparece una flecha › al final. Lleva al detalle del
  ejercicio (sección 6.5).

### 6.4 Sin rutina (`rutina-vacia.html`)

- Mismo encabezado, pero "Todavía no hay ninguna rutina cargada" y, debajo, "Pegá
  el JSON o cargala con el formulario."
- Solo el botón **Actualizar rutina**, centrado. **Ver rutinas anteriores** en
  secundario si hay historial.
- En lugar de las pestañas, una ilustración mínima: el logo en gris al 20 %.

### 6.5 Detalle de un ejercicio: lo que levanté (`ejercicio.html`)

Panel lateral de 420 px que entra desde la derecha sobre la pantalla principal
oscurecida (en celular, pantalla completa).

```
┌──────────────────────────────────────┐
│ ✕                                    │
│ A1 · Dislocaciones con banda         │
│ Semana 1 · Día 1 · Full Body         │
│                                      │
│ PLAN                                 │
│ 3 × 20 · 0 kg                        │
│                                      │
│ LEVANTÉ                              │
│ ┌────────────┐                       │
│ │    0 kg    │  último registro      │
│ └────────────┘  lun 15/09 19:40      │
│                                      │
│ REGISTROS                            │
│ lun 15/09/2026 · 19:40      0 kg     │
│ jue 11/09/2026 · 19:12      0 kg     │
│ lun 08/09/2026 · 19:05      0 kg     │
│                                      │
│ Se registra desde el reloj.          │
└──────────────────────────────────────┘
```

- El dato grande es el último "Levanté" (40 px / 600) en una tarjeta
  `--superficie-alta`.
- Debajo, la lista de registros de ese ejercicio, del más nuevo al más viejo.
- Al pie, en `--texto-suave`: "Se registra desde el reloj."
- Dejá lugar, sin dibujarlo, para un gráfico de progresión en el futuro (no hace
  falta en esta iteración).

---

## 7. Actualizar rutina

Arriba de todo: "← Volver a la rutina" (botón de texto) y el título "Actualizar
rutina". Debajo, dos pestañas para el modo: **Pegar JSON** y **Formulario**.

Al pie de las dos pantallas va una **barra fija** con el botón **Guardar rutina**
a la derecha y, a la izquierda, en `--texto-suave`: "La rutina actual no se
pierde: queda en Anteriores."

### 7.1 Pegar JSON (`actualizar-json.html`, pestaña activa)

```
┌──────────────────────────────── navbar ─────────────────────────────┐
│ ← Volver a la rutina                                                │
│ Actualizar rutina                                                   │
│ ( Pegar JSON )  Formulario                                          │
│                                                                     │
│ Pegá el JSON que armó el LLM:                                       │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ {                                                               │ │
│ │   "semanas": [                                                  │ │
│ │     { "numero": 1, "dias": [ ...                                │ │
│ └─────────────────────────────────────────────────────────────────┘ │
│ (✔ JSON válido)  4 semanas · 16 días · 112 ejercicios               │
│                                                                     │
│ Vista previa                                                        │
│ ( Semana 1 )  Semana 2   Semana 3   Semana 4                        │
│ ┌─ Día 1 · Full Body ... (misma tabla de la rutina, sin Levanté)  ┐ │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│ La rutina actual no se pierde: queda en Anteriores.  ( Guardar )    │
└─────────────────────────────────────────────────────────────────────┘
```

- Área de texto grande (320 px de alto), Roboto Mono 14 px, fondo
  `--superficie`, con el JSON de ejemplo ya pegado.
- Debajo, el **chip "✔ JSON válido"** en `--exito` y el resumen en
  `--texto-suave`.
- **Vista previa:** el título "Vista previa" y la misma estructura de pestañas y
  tablas de la pantalla principal, pero **sin la columna Levanté** (es una rutina
  nueva) y con las tablas un poco más compactas.
- **Guardar rutina** habilitado.

### 7.2 Pegar JSON con error (`actualizar-json-error.html`)

- La misma pantalla, pero el chip dice "✖ Hay un error" en `--error`.
- Debajo del área de texto, un **mensaje de error**:
  "semanas[0].dias[1].ejercicios[2].series tiene que ser un entero".
- Para un JSON mal formado, una segunda variante en la misma maqueta: "El JSON
  está mal formado cerca de la línea 14". Resaltá esa línea en el área de texto
  con fondo `--error-fondo`.
- Sin vista previa.
- **Guardar rutina** deshabilitado.

### 7.3 Formulario (`actualizar-formulario.html`)

```
│ Pegar JSON  ( Formulario )                                          │
│                                                                     │
│ ( Semana 1 )  Semana 2   Semana 3   Semana 4   ( + )                │
│                                                                     │
│ ┌─ Día 1 ──────────────────────── Nombre del día [ Full Body ]  🗑 ┐ │
│ │ LETRA  EJERCICIO              SERIES  REPS        PESO    NOTA   │ │
│ │ [A1 ]  [Dislocaciones con...] [ 3 ]  [ 20      ] [0 kg] [    ] 🗑│ │
│ │ [A2 ]  [Puente de gluteos...] [ 3 ]  [ 20      ] [0 kg] [    ] 🗑│ │
│ │ [B  ]  [Db floor press      ] [ 5 ]  [20-12-10.] [0 kg] [    ] 🗑│ │
│ │ + Ejercicio                                                      │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ + Día                                                               │
│                                                                     │
│ ( ⧉ Copiar esta semana a las siguientes )                           │
```

- Arriba, las pestañas de semana más una píldora **+** para agregar una semana.
- **Una tarjeta por día**: "Día 1" como título, a la derecha el campo "Nombre
  del día" y una papelera para borrar el día.
- Cada ejercicio es una fila de inputs: **Letra** (64 px), **Ejercicio**
  (flexible), **Series** (72 px), **Reps** (140 px), **Peso** (96 px), **Nota**
  (flexible, más angosta que Ejercicio) y una papelera al final.
- Debajo de las filas, "+ Ejercicio"; debajo de las tarjetas, "+ Día" (botones de
  texto).
- Botón secundario con ícono: **Copiar esta semana a las siguientes**.
- Mostrá **un campo con error** para tener el estado diseñado: en la fila de
  "Db floor press", Series vacío con borde `--error` y debajo "Tiene que ser un
  número".
- En celular, cada ejercicio es una tarjeta con los campos uno debajo del otro
  (Letra y Series en la misma línea).

---

## 8. Rutinas anteriores

### 8.1 Lista y actividad (`anteriores.html`)

Dos secciones, una debajo de la otra.

**Rutinas anteriores**

```
│ Rutinas anteriores                                                  │
│ Cada vez que se carga una rutina, la anterior queda guardada acá.   │
│                                                                     │
│ ┌─────────────────────────────────────────────────────────────────┐ │
│ │ ● Actual                                                        │ │
│ │ jue 11/09/2026 · 02:35      4 semanas · 16 días · 112 ej.       │ │
│ ├─────────────────────────────────────────────────────────────────┤ │
│ │ jue 11/09/2026 · 02:30      4 semanas · 16 días · 112 ej.   ›  │ │
│ │ lun 11/08/2026 · 21:10      4 semanas · 16 días · 104 ej.   ›  │ │
│ │ vie 11/07/2026 · 20:45      4 semanas · 12 días ·  84 ej.   ›  │ │
│ └─────────────────────────────────────────────────────────────────┘ │
```

- Una tarjeta con una fila por rutina: fecha y hora de carga a la izquierda, el
  resumen en `--texto-suave` al centro y › a la derecha.
- La primera fila es la **actual**: un chip "● Actual" en `--exito` y sin ›.

**Actividad**

```
│ Actividad                                                           │
│                                                                     │
│ jue 11/09 · 02:35  ⬆ Rutina guardada: 4 semanas, 16 días, 112 ej.   │
│ jue 11/09 · 02:34  ✖ Rutina rechazada: semanas[0].numero tiene que  │
│                      ser un entero                                  │
│ jue 11/09 · 02:31  ⌚ El reloj bajó la rutina                        │
│ jue 11/09 · 02:30  ⬆ Rutina guardada (era la primera)               │
│ jue 11/09 · 02:28  ⌚ El reloj pidió la rutina, pero no había        │
│                                                                     │
│                         ( Ver más )                                 │
```

- Una línea de tiempo simple: hora en `--texto-suave` (ancho fijo), ícono y texto.
- Íconos por tipo: **⬆ guardada** (`--exito`), **✖ rechazada** (`--error`),
  **⌚ el reloj la bajó** (`--texto-suave`).
- Al final, botón secundario **Ver más**.
- En celular, la hora va arriba de cada evento.

### 8.2 Una rutina anterior (`anterior-detalle.html`)

- "← Volver a Anteriores" y el título "Rutina del jueves 11/09/2026 · 02:30".
- Un **aviso** a todo el ancho, en `--superficie-alta`: "Estás viendo una rutina
  anterior. Solo lectura." y, a la derecha, el botón secundario **Volver a usar
  esta rutina**.
- Debajo, las pestañas de semana y las tablas iguales a las de la pantalla
  principal, **sin la columna Levanté**.

---

## 9. Estados y avisos (`estados.html`)

Una página que muestre, uno debajo del otro:

- **Cargando:** la pantalla principal con bloques grises (`--superficie-alta`)
  en lugar de textos y filas ("esqueleto").
- **Sin conexión con el servidor:** en el centro, "No se pudo conectar con el
  servidor" y, debajo, "¿Está prendido el homelab?" en `--texto-suave`, con un
  botón secundario **Reintentar**.
- **Aviso "¡Actualizado!":** la píldora flotante abajo al centro (componente de
  la sección 4), sobre la pantalla principal. Mostrá también la variante de error
  del mismo aviso: "No se pudo guardar la rutina", con el texto en `--error`.

---

## 10. Datos de ejemplo

Usá estos datos en todas las maquetas. En **Levanté** va siempre "0 kg" (así van
a venir en esta iteración).

**Semana 1 · Día 1 · Full Body**

| Letra | Ejercicio | Series | Reps | Peso | Nota |
|---|---|---|---|---|---|
| A1 | Dislocaciones con banda | 3 | 20 | 0 kg | |
| A2 | Puente de gluteos isometrico + pull apart | 3 | 20 | 0 kg | |
| B | Db floor press | 5 | 20-12-10-8-6 | 0 kg | |
| C | remo seal con barra | 4 | 12-8 | 0 kg | |
| D | trap bar deadlift | 4 | 15-20 | 0 kg | |
| E | caminata con trap bar | 4 | 20 PASOS | 0 kg | |

**Semana 1 · Día 2 · Full Body**

| Letra | Ejercicio | Series | Reps | Peso | Nota |
|---|---|---|---|---|---|
| A1 | Band pull apart | 3 | 20 | 0 kg | |
| A2 | Puente de glúteos a una pierna | 3 | 20 | 0 kg | por pierna |
| B | Press militar con barra | 5 | 12-10 | 0 kg | |
| C | Press plano con pausa 1" | 3 | 10 | 0 kg | |
| D1 | Sentadilla Hack | 4 | 10-8 | 0 kg | |
| D2 | Sentadilla Hack | 2 | 20 | 0 kg | |
| E | sillon de cuadriceps a una pierna | 10 | 10 | 0 kg | por pierna |

**Semana 1 · Día 3 · Full Body**

| Letra | Ejercicio | Series | Reps | Peso | Nota |
|---|---|---|---|---|---|
| A1 | Face pull parado | 3 | 20 | 0 kg | |
| A2 | Sentadilla Goblet | 3 | 20 | 0 kg | |
| B | Remo con barra / Bent over row | 5 | 12-10-8-6-4 | 0 kg | |
| C | Curl de Biceps en Banco Scott con Barra Z | 3 | 10-12 | 0 kg | |
| D | Vuelos frontales neutro | 4 | 10 | 0 kg | |
| E | Vuelos Laterales en Maquina | 4 | 10 | 0 kg | |
| F | caminata del granjero con DB | 5 | ida y vuelta | 0 kg | |

**Semana 1 · Día 4 · Full Body**

| Letra | Ejercicio | Series | Reps | Peso | Nota |
|---|---|---|---|---|---|
| A1 | Press McGill en banco plano | 2 | 20 | 0 kg | |
| A2 | Sillon de Cuadriceps | 3 | 20 | 0 kg | |
| B | Sentadilla zercher a cajon | 4 | 8-6 | 0 kg | |
| C | press vertical a un brazo parado | 4 | 10 | 0 kg | por lado |
| D | Flexiones skullcrushers | 3 | fallo | 0 kg | |
| E | Jalon al pecho neutro | 3 | 12-10 | 0 kg | |
| F | Press CONVERGENTE | 3 | 12-10 | 0 kg | |
| G | Isquios Parado Maquina | 10 | 10 | 0 kg | |

Las semanas 2, 3 y 4 son iguales a la 1 (solo se dibuja la activa).

Los nombres van tal cual, con sus mayúsculas y tildes faltantes: vienen así de la
app del entrenador.

**Usuario:** Luis, luis@ejemplo.com, inicial "L".

**Rutina actual:** 4 semanas · 16 días · 112 ejercicios, actualizada el jueves
11/09/2026 a las 02:35.

**Rutinas anteriores y actividad:** las de las secciones 8.1 (fechas y resúmenes
tal cual).

**JSON de ejemplo** para el área de texto: el comienzo de la rutina de arriba en
el formato real:

```json
{
  "semanas": [
    {
      "numero": 1,
      "dias": [
        {
          "numero": 1,
          "nombre": "Full Body",
          "ejercicios": [
            { "letra": "A1", "nombre": "Dislocaciones con banda", "series": 3, "reps": "20", "peso": "0 kg", "nota": null },
            { "letra": "A2", "nombre": "Puente de gluteos isometrico + pull apart", "series": 3, "reps": "20", "peso": "0 kg", "nota": null },
            { "letra": "B", "nombre": "Db floor press", "series": 5, "reps": "20-12-10-8-6", "peso": "0 kg", "nota": null }
          ]
        }
      ]
    }
  ]
}
```

---

## 11. Accesibilidad

- Contraste AA como mínimo en todos los textos (los tokens de la sección 3 lo
  cumplen sobre `--fondo` y `--superficie`).
- Foco visible en todo lo que se pueda tocar: anillo de 2 px `--primario` con
  2 px de separación.
- Estructura semántica: `header`/`nav`/`main`, títulos en orden, `table` con
  `th` para las tablas, `label` para cada input, y las pestañas marcadas como
  pestañas (`role="tablist"` / `role="tab"` / `aria-selected`).
- Áreas táctiles de 44 px como mínimo en celular.
- El estado nunca depende solo del color: los chips y avisos llevan ícono y
  texto.

---

## 12. Lo que no va

- Nada de interacción ni JavaScript: son maquetas.
- Nada de datos reales ni conexión al servidor.
- Nada de registro de usuarios, recuperación de contraseña ni perfiles: el login
  y el menú del usuario son solo diseño.
- Nada de gráficos de progresión todavía (solo el lugar reservado en el detalle
  del ejercicio).
- Nada de tema claro.
