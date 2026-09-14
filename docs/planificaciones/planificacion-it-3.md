# Iteración 3 — Diseño de la app del reloj

**Estado:** en planificación. Este documento es un **brief de diseño**: está
escrito para que un LLM (o un diseñador) arme las maquetas de la app del reloj
**completa**, incluyendo lo que ya existe y andando, más lo nuevo de esta
iteración: **registrar cuánto levanté en cada ejercicio**.

No describe cómo implementarlo. Describe cómo tiene que verse y comportarse.

---

## 1. Qué tenés que entregar

- **Maquetas estáticas en HTML + CSS**, una página por pantalla, con los datos de
  ejemplo de la sección 11.
- **Cada pantalla dibujada dentro de un círculo**, simulando el reloj (ver
  sección 4). Lo que quede fuera del círculo no existe.
- **Sin JavaScript y sin interacción.** Todo se dibuja en reposo: un estado por
  archivo. Si una pantalla tiene dos estados interesantes (vacío y con datos),
  son dos archivos.
- **Sin frameworks ni librerías de componentes.** HTML semántico y **una sola
  hoja de estilos compartida**, con los tokens de la sección 5 como variables
  CSS.
- **Todos los textos en español rioplatense** (voseo: "Sincronizá", "Tocá",
  "Guardá"). Los textos que ya existen hoy están en la sección 6 y van
  **exactamente así**; los nuevos los proponés vos.
- **Un `decisiones.md`** corto donde expliques y justifiques lo que resolviste de
  la sección 9. Eso es parte de la entrega, no un extra.

Archivos, en `docs/diseno-reloj/`:

```
docs/diseno-reloj/
├── estilos.css
├── indice.html                  ← todas las pantallas en una grilla, para verlas juntas
│
│   —— lo que ya existe ——
├── semanas.html                 ← pantalla de inicio, con rutina cargada
├── semanas-vacio.html           ← sin rutina guardada
├── semanas-cargando.html        ← mientras lee el archivo al arrancar
├── dias.html                    ← los días de una semana
├── ejercicios.html              ← los ejercicios de un día
├── ejercicio.html               ← un ejercicio (la pantalla más importante)
├── ejercicio-largo.html         ← un ejercicio con reps largas y nota (ver sección 10)
├── ajustes.html
├── servidor.html
├── aviso-ok.html                ← "¡Actualizado!" sobre la pantalla de semanas
├── aviso-error.html             ← "No se pudo conectar" sobre la pantalla de semanas
│
│   —— lo nuevo de la iteración 3 ——
├── ejercicio-con-registro.html  ← el ejercicio mostrando lo que levanté
├── registrar.html               ← cargar el peso
├── registrar-editando.html      ← el mismo, en el momento de cambiar el valor
├── ejercicios-registrados.html  ← la lista del día marcando lo ya registrado
├── ajustes-pendientes.html      ← Ajustes avisando que hay registros sin subir
└── conflicto.html               ← llegó una rutina nueva y hay registros sin subir
```

El logo está en `assets/logo-render.png` (500 × 500, negro sobre círculo blanco).

---

## 2. Contexto: qué es Fierros

Fierros es una app para el **reloj** (Samsung Galaxy Watch8 40mm, Wear OS, sin
LTE) que muestra la rutina de gimnasio que arma mi entrenador.

**El problema que resuelve:** voy al gimnasio **dejando el celular en casa** por
seguridad. Sin el celular no tengo la rutina (no sé qué ejercicio toca, cuántas
series ni con cuánto peso) y no tengo dónde anotar lo que levanté. Cuando vuelvo
a casa cargo en Mynter —la plataforma del entrenador— lo que me acuerdo, y de
memoria el registro sale mal.

La rutina vive en un servidor propio en casa (homelab). El reloj **la baja cuando
está en el Wi-Fi de casa y la usa offline**. En el gimnasio no hay conexión de
ninguna clase: ni Wi-Fi, ni datos, ni el celular cerca. **Todo lo que pase en el
gimnasio pasa sin red.**

Ya existe una **web** en ese mismo servidor (iteración 2, terminada) para ver la
rutina, cargarla y editarla desde la compu. La web ya tiene lugar reservado para
lo que levanté: una columna **"Levanté"** y un panel de ejercicio con el historial
de registros. Hoy están vacíos porque el reloj todavía no manda nada. **Esta
iteración es la que los llena.**

**Estructura de una rutina** (los datos con los que trabajás):

- Un **mesociclo** tiene semanas (normalmente 4, tope 5).
- Cada **semana** tiene días (normalmente 4). Un día puede tener nombre
  ("Full Body", "Tren superior").
- Cada **día** tiene ejercicios en orden. Cada ejercicio tiene:
  - **letra**: "A", "B"... Las letras con número ("A1", "A2") son una
    **superserie**: ejercicios que se hacen seguidos, uno atrás del otro, sin
    descanso en el medio;
  - **nombre**: tal cual viene del entrenador, con sus mayúsculas raras y sus
    tildes faltantes ("Db floor press", "Press CONVERGENTE");
  - **series** (número) y **reps** (texto: "20", "12-10", "20-12-10-8-6",
    "20 PASOS", "ida y vuelta", "fallo");
  - **peso del plan** (texto: "0 kg", "12 kg c/mano"). Ojo con esto: **hoy
    siempre vale "0 kg"** porque nunca se cargó de verdad. Ver sección 8.
  - **nota** opcional ("por pierna", "por lado", "serie descendente").

---

## 3. Cómo se usa esto en la vida real

Es lo más importante del brief. El contexto de uso manda sobre cualquier idea
linda.

- **Estoy entrenando.** Miro el reloj entre serie y serie, con 30 a 90 segundos
  de descanso, respirando fuerte y con pulsaciones altas.
- **Tengo las manos ocupadas o sucias.** Transpiradas, con magnesio, a veces con
  guantes. La pantalla táctil responde mal con los dedos mojados.
- **Estoy parado, con el brazo en el aire.** Sostener el brazo levantado cansa:
  cada interacción tiene que durar segundos, no minutos.
- **La pantalla se apaga sola** a los pocos segundos. Si estoy a mitad de cargar
  un peso y se apaga, al volver no puedo haber perdido lo que estaba haciendo.
- **No quiero pensar.** Si tengo que decidir algo, ya perdí. Lo que la app
  proponga por defecto tiene que ser lo correcto el 90% de las veces.
- **Un error no puede costarme la serie.** Si toco mal y borro un registro, es
  peor que no haber registrado nada.

**Principio de diseño: la menor fricción posible.** Registrar un peso tiene que
costar **un toque o dos**, y tiene que poder hacerse sin leer nada. Si el
diseño obliga a mirar fijo la pantalla más de tres segundos, está mal.

---

## 4. El dispositivo y sus reglas

**Pantalla redonda.** Es un círculo, no un cuadrado con las esquinas
redondeadas. Nada útil puede vivir cerca del borde: arriba, abajo y a los
costados el ancho disponible se achica. Un texto largo centrado se corta contra
la curva.

**Canvas de las maquetas: 384 × 384 px** (equivale a 192 dp a 2×, la medida de
referencia "small round" que usa Wear OS). El reloj real es un poco más grande,
así que **no hagas nada pixel-exact**: usá medidas relativas y dejá aire.
Dibujá el círculo con un borde fino gris para que se entienda dónde termina la
pantalla, y fondo negro alrededor.

**Gestos, y qué está prohibido:**

| Gesto | Qué hace | Consecuencia para el diseño |
|---|---|---|
| Deslizar a la derecha | **Volver atrás** (gesto del sistema) | **No dibujes botones de "volver"**. Y no podés usar deslizamientos horizontales para nada más: te los come el sistema. |
| Girar la corona | Hacer scroll | Es la forma cómoda de recorrer una lista larga sin taparla con el dedo. Se puede aprovechar para otras cosas. |
| Tocar | Lo obvio | Área táctil mínima **48 dp**. En un círculo de 192 dp entran, cómodos, unos 3 botones apilados. |

**Tamaño de texto:** nada por debajo de ~12 sp. El nombre del ejercicio y el
peso se leen de reojo: van grandes.

**El teclado existe, pero duele.** Wear OS tiene teclado propio, con dictado por
voz. La app ya lo usa para escribir la URL del servidor (algo que se hace una vez
cada tanto, sentado en casa). **Para el gimnasio es mala idea**: teclear números
en 40 mm con la mano transpirada es lento y erra. Si tu diseño lo usa, tenés que
justificar por qué gana igual.

**Componentes disponibles** (Compose for Wear OS, Material 3). No es una lista
para copiar, es para que sepas qué es barato de construir:

- lista vertical con efecto de escala en los extremos (los ítems se achican al
  acercarse al borde del círculo);
- botones de ancho completo con esquinas muy redondeadas;
- **botón de borde**: un botón pegado al borde inferior, con la forma de la curva
  de la pantalla. La app ya lo usa para la tuerquita de Ajustes;
- título de sección arriba de la lista;
- diálogos, selectores tipo rueda, sliders, checkboxes, progreso circular;
- **vibración**: el reloj puede vibrar. Todavía no se usa para nada.

---

## 5. Identidad visual

**Tema oscuro únicamente.** Un fondo claro en el gimnasio encandila y gasta
batería (la pantalla es OLED: el negro directamente no consume).

Hoy la app usa **el tema por defecto de Material 3 para Wear OS**, sin tocar
nada. Eso significa que esta es la oportunidad de fijar una paleta propia. La web
de Fierros ya se diseñó con estos tokens, que son los mismos valores base de
Material 3, así que **respetalos para que el reloj y la web se vean de la misma
familia**:

| Token | Valor | Uso |
|---|---|---|
| `--fondo` | `#0F0F12` | Fondo de la pantalla |
| `--superficie` | `#1C1B1F` | Botones de lista, tarjetas |
| `--superficie-alta` | `#2B2930` | Avisos, campos, estado presionado |
| `--borde` | `#3A3740` | Bordes y separadores |
| `--texto` | `#E6E1E5` | Texto principal |
| `--texto-suave` | `#A8A2AE` | Texto secundario, notas, etiquetas |
| `--primario` | `#D0BCFF` | Acento: la letra del ejercicio, botón principal |
| `--sobre-primario` | `#21005D` | Texto sobre `--primario` |
| `--error` | `#F2B8B5` | Errores (valor de error de Material 3 en oscuro) |

**Tipografía:** la del sistema (Roboto). Sin fuentes externas: el reloj está
offline.

**Jerarquía que ya usa la app hoy**, para que la respetes:

- la **letra** del ejercicio va en `--primario`, arriba de todo;
- el **nombre** del ejercicio, grande y centrado;
- las **series × reps**, en el tamaño más grande de la pantalla: es el dato que
  miro entre series;
- el **peso** y la **nota**, más chicos, la nota en `--texto-suave`.

---

## 6. Lo que ya existe (y no se tira)

Esto **ya está construido, instalado en el reloj y funcionando**. Rediseñalo si
mejora, pero entendé que funciona y que los textos de abajo son los reales.

La navegación es **por niveles**, y se vuelve deslizando a la derecha:

```
Semanas ──▶ Días ──▶ Ejercicios del día ──▶ Un ejercicio
   │
   └──▶ Ajustes ──▶ Servidor
```

### 6.1 Semanas (`semanas.html`) — la pantalla de inicio

Título **"Semanas"** y un botón por semana: **"Semana 1"**, "Semana 2"...
Abajo de todo, pegada al borde inferior, la **tuerquita de Ajustes**.

**Qué semana toca la elijo yo, siempre.** No se calcula desde una fecha: apenas
falto una semana o el entrenador corre algo, un cálculo automático miente.

### 6.2 Semanas sin rutina (`semanas-vacio.html`)

Si nunca sincronizó, en vez de la lista dice **"No hay rutina guardada.
Sincronizá desde casa."** y ofrece un solo botón: **"Sincronizar datos"**
(mientras trabaja dice **"Sincronizando…"**). La tuerquita de Ajustes sigue
abajo, por si hay que corregir el servidor.

### 6.3 Semanas cargando (`semanas-cargando.html`)

Un instante, mientras lee el archivo: solo el título "Fierros" y
**"Cargando…"**.

### 6.4 Días (`dias.html`)

Título **"Semana 1"**. Un botón por día. Si el día tiene nombre, el botón muestra
**"Día 1"** en una columna angosta a la izquierda y el nombre al lado
("Full Body"). Si no tiene nombre, solo "Día 1".

### 6.5 Ejercicios del día (`ejercicios.html`)

Título **"Semana 1 · Día 2"**. Un botón por ejercicio, con la **letra** en
negrita en una columna angosta a la izquierda y el **nombre** al lado.

Las superseries (A1, A2) hoy **no se agrupan visualmente**: son botones sueltos y
la letra es la única pista. Si se te ocurre algo mejor que no complique la
pantalla, proponelo.

### 6.6 Un ejercicio (`ejercicio.html`) — la pantalla más importante

Es donde paso el tiempo. **Una pantalla = un ejercicio.** De arriba a abajo:

1. la **letra** ("A2"), en `--primario`;
2. el **nombre** ("Puente de glúteos a una pierna"), grande y centrado;
3. **series × reps** ("3 × 20"), en el tamaño más grande de todos;
4. el **peso del plan** ("0 kg");
5. la **nota**, si hay ("por pierna"), en `--texto-suave`.

A los costados, **flechas** para ir al ejercicio anterior y al siguiente **dentro
del mismo día**: en el primero no hay flecha izquierda, en el último no hay
derecha, y nunca saltan a otro día. Los márgenes laterales del texto están
agrandados para que no quede debajo de las flechas.

### 6.7 Ajustes (`ajustes.html`)

Título **"Ajustes"**, dos botones: **"Sincronizar datos"** y **"Servidor"**.

### 6.8 Servidor (`servidor.html`)

Título **"Servidor"**. Debajo, la URL actual sin el `http://`
(`192.168.100.240:3000`). Dos botones: **"Cambiar"** (abre el teclado del
sistema) y **"Restablecer"**. La IP del homelab no es fija: por eso se edita
desde el reloj.

### 6.9 El aviso de abajo (`aviso-ok.html`, `aviso-error.html`)

**Todos los resultados** —éxitos y errores— se avisan igual: una franja pegada al
borde inferior, con las esquinas de arriba redondeadas, que **la propia pantalla
redonda recorta con forma de U**. Entra deslizando desde abajo, se va sola
(2 segundos si salió bien, 3 si fue error) y tocarla la cierra antes. El texto va
arriba de la franja, donde hay más ancho, en dos renglones como máximo.

Textos reales de hoy:

| Situación | Texto |
|---|---|
| Salió bien | **¡Actualizado!** |
| No hay red / el servidor no responde | **No se pudo conectar** |
| El servidor no tiene rutina | **El servidor no tiene rutina** |
| Error del servidor | **Error del servidor (500)** |
| La rutina del servidor está rota | **Rutina inválida en el servidor** |
| No se pudo escribir en el reloj | **No se pudo guardar la rutina** |
| La URL escrita no sirve | **Dirección no válida** |

Diseñá un aviso de éxito y uno de error (el de error, en `--error`).

---

## 7. Lo nuevo: registrar el peso

**El corazón de esta iteración.** Hoy la app es de solo lectura. Ahora tiene que
dejarme **anotar cuánto levanté en cada ejercicio**, ahí mismo, en el gimnasio,
sin conexión.

### 7.1 Las reglas del registro

- **Un peso por ejercicio, no por serie.** Si hago 5 series de press, anoto un
  solo número. Anotar serie por serie es más preciso y es exactamente lo que no
  voy a hacer entre jadeo y jadeo.
- **El registro queda pegado a la posición en la rutina**: semana, día y letra.
  Más la fecha y hora en que lo anoté.
- **Se guarda en el reloj al instante**, sin red. Sube al servidor después, en
  casa.
- **Se puede corregir.** Si me equivoco o hago una serie más pesada, tengo que
  poder cambiarlo sin dar vueltas.
- **Nada se registra solo.** Si no anoto, no pasa nada: la app sigue sirviendo
  para leer, como hasta ahora.

### 7.2 Lo que el registro habilita, y que importa más que el número

Cuando voy a hacer el press de la semana 3, la pregunta que tengo en la cabeza
no es "¿cuánto levanto hoy?", es **"¿cuánto levanté la vez pasada?"**. Ese dato
—el registro anterior del mismo ejercicio— es lo más valioso de toda la
iteración: es lo que me deja subir el peso con criterio.

**Tiene que estar a la vista en la pantalla del ejercicio, sin tocar nada**, y
debería ser el punto de partida cuando voy a cargar el peso nuevo (si la semana
pasada hice 37,5 kg, arrancar en 37,5 y no en 0).

### 7.3 Las dos pantallas nuevas

**`ejercicio-con-registro.html`** — la pantalla del ejercicio (6.6), ahora
mostrando:

- lo que levanté **esta vez**, si ya registré;
- lo que levanté **la vez anterior**, con alguna referencia a cuándo
  ("semana 2", "hace 7 días");
- la forma de entrar a registrar.

Y acá está el problema de diseño: **esa pantalla ya está llena** (letra, nombre,
series × reps, peso del plan, nota) y mide 192 dp de diámetro. Resolvelo. Ver
también la sección 8, que puede darte espacio gratis.

**`registrar.html`** y **`registrar-editando.html`** — cargar el peso. Lo de
siempre: pocos toques, números grandes, y que se entienda sin leer. Mostrá el
estado en reposo y el momento de cambiar el valor.

**`ejercicios-registrados.html`** — la lista de ejercicios del día (6.5), con
alguna marca discreta en los que ya registré. Sirve para saber por dónde voy
cuando vuelvo a la lista, y para no registrar dos veces el mismo.

### 7.4 Subir los registros

En algún momento, en casa, los registros suben al servidor y aparecen en la web.
Hoy existe un solo botón, **"Sincronizar datos"**, que **baja** la rutina.

**`ajustes-pendientes.html`** — Ajustes tiene que poder decirme, de un vistazo,
**cuántos registros tengo sin subir**. Si hay 12 registros esperando y yo no lo
sé, la iteración entera no sirve.

**`conflicto.html`** — el caso feo. Un registro apunta a *semana 2, día 1, letra
B*. Si bajo una rutina nueva, la "B" del día 1 puede ser otro ejercicio, y ese
registro pasa a mentir. Diseñá qué ve el usuario cuando **llega una rutina nueva
y hay registros sin subir**. La sección 9 te pide que decidas qué hace la app;
esta maqueta muestra cómo se lo cuenta.

---

## 8. El peso del plan: un problema abierto

En la pantalla del ejercicio, hoy, debajo de las series × reps, dice **"0 kg"**.

Ese es el **peso del plan**, el que puso el entrenador. **Nunca se cargó de
verdad**: viene en "0 kg" en todos los ejercicios de todas las rutinas, desde el
primer día, porque hasta ahora no se usaba para nada. O sea que hoy la app le
dedica un renglón a un dato que siempre dice lo mismo y no informa nada.

A partir de esta iteración van a convivir **dos pesos distintos**:

- el **del plan** ("hacé esto con 40 kg"), que sigue viniendo del entrenador;
- el **que levanté** ("hice 42,5 kg"), que es lo nuevo.

Decidí vos cómo se llevan en 192 dp. Algunas salidas posibles, pero elegí la que
te cierre: mostrar los dos siempre; esconder el del plan cuando vale "0 kg";
mostrar uno y el otro solo al registrar; fusionarlos en un renglón. **Lo que no
puede pasar es que se confundan**: si miro rápido y creo que levanté 40 cuando
eso era lo que decía el plan, el registro no sirve.

---

## 9. Decisiones que tenés que resolver

No están decididas. **Elegí una opción para cada una, dibujala en las maquetas y
justificala en `decisiones.md`.** Si se te ocurre algo mejor que las opciones que
listo, mejor todavía.

**9.1 ¿Cómo se carga el número?** Es la decisión central.

- Botones **− / +** con salto fijo (2,5 kg, que es el salto real de los discos
  chicos), manteniendo presionado para avanzar rápido. Rápido y a ciegas, pero
  llegar a 100 kg desde 0 cuesta.
- **Teclado del sistema**. Entra cualquier valor, incluso texto como "peso
  corporal" o "12 kg c/mano". Lento y propenso a errores con la mano mojada.
- **Rueda girando la corona**. Cómodo, no se tapa la pantalla con el dedo, pero
  los valores son una lista fija.
- Alguna mezcla: arrancar en el valor de la semana pasada y ajustar con − / +,
  con el teclado escondido para los casos raros.

Ojo con un detalle: el peso no siempre es un número. "peso corporal",
"12 kg c/mano" y "banda roja" son respuestas válidas. Tu solución tiene que
poder con eso, aunque sea por un camino secundario.

**9.2 ¿Cuándo suben los registros?**

- El botón **"Sincronizar datos"** que ya existe pasa a hacer las dos cosas:
  sube lo registrado y baja la rutina. Un solo botón, un solo concepto.
- Un botón **aparte** en Ajustes, "Subir registros". Más control, dos cosas que
  acordarse.
- **Solo, al volver al Wi-Fi**, en segundo plano. Nada que recordar, pero es
  justo lo que la iteración 1 evitó a propósito: gasta batería y cuando falla no
  me entero.

**9.3 ¿Qué pasa si sincronizo y hay registros sin subir?** (el caso de
`conflicto.html`)

- **Sube primero, y si falla no baja la rutina.** Nunca se pierde nada, a veces
  no se actualiza la rutina.
- **Baja igual y los deja pendientes.** La rutina siempre al día, con el riesgo
  de que los registros viejos apunten a ejercicios que cambiaron.
- **Avisa y elijo yo.** Más control, una decisión más.

**9.4 ¿Vibra?** El reloj puede vibrar y hoy no lo usa. ¿Un toquecito al guardar
un registro confirma sin mirar, o es ruido? Decidilo.

---

## 10. Un problema concreto a resolver de paso

En la pantalla del ejercicio, las **reps largas se cortan mal**. Un ejercicio de
5 series con esquema "20-12-10-8-6" se muestra como "5 × 20-12-10-8-6" y hoy se
parte así:

```
      5 × 20-12-10-8
            -6
```

Queda feo y se lee mal, que es lo peor que puede pasar justo con el dato que más
miro.

**Ya se probó achicar la letra automáticamente** para que entrara en un renglón,
y se descartó: el texto quedaba chico y no gustó. Buscá otra salida.

Dibujalo en `ejercicio-largo.html`, con el caso peor: reps largas **y** nota
**y**, si tu diseño los muestra ahí, los dos pesos.

---

## 11. Datos de ejemplo

Usá estos, que son de una rutina real. Los nombres van **tal cual**, con sus
mayúsculas y sus tildes faltantes: vienen así de la app del entrenador.

**Semana 1 · Día 2 · Full Body**

| Letra | Ejercicio | Series | Reps | Peso del plan | Nota |
|---|---|---|---|---|---|
| A1 | Band pull apart | 3 | 20 | 0 kg | |
| A2 | Puente de glúteos a una pierna | 3 | 20 | 0 kg | por pierna |
| B | Press militar con barra | 5 | 12-10 | 0 kg | |
| C | Press plano con pausa 1" | 3 | 10 | 0 kg | |
| D1 | Sentadilla Hack | 4 | 10-8 | 0 kg | |
| D2 | Sentadilla Hack | 2 | 20 | 0 kg | |
| E | sillon de cuadriceps a una pierna | 10 | 10 | 0 kg | por pierna |

**Semana 1 · Día 1 · Full Body** (para el caso de las reps largas)

| Letra | Ejercicio | Series | Reps | Peso del plan | Nota |
|---|---|---|---|---|---|
| A1 | Dislocaciones con banda | 3 | 20 | 0 kg | |
| A2 | Puente de gluteos isometrico + pull apart | 3 | 20 | 0 kg | |
| B | Db floor press | 5 | 20-12-10-8-6 | 0 kg | |
| C | remo seal con barra | 4 | 12-8 | 0 kg | |
| D | trap bar deadlift | 4 | 15-20 | 0 kg | |
| E | caminata con trap bar | 4 | 20 PASOS | 0 kg | |

**La rutina tiene 4 semanas × 4 días.** Los días se llaman todos "Full Body".

**Registros de ejemplo** (lo que levanté, para las pantallas de la sección 7):

| Ejercicio | Semana 1 | Semana 2 | Semana 3 |
|---|---|---|---|
| B · Press militar con barra | 30 kg | 32,5 kg | sin registrar (es hoy) |
| C · Press plano con pausa 1" | 45 kg | 47,5 kg | sin registrar |
| A2 · Puente de glúteos a una pierna | peso corporal | peso corporal | peso corporal |
| D1 · Sentadilla Hack | 60 kg | 60 kg | sin registrar |

Para `ajustes-pendientes.html`: **7 registros sin subir**.
Para `conflicto.html`: hay **7 registros sin subir** y el servidor tiene una
rutina nueva.
URL del servidor: `192.168.100.240:3000`.

---

## 12. Accesibilidad y gimnasio

- **Contraste alto**, más que en una pantalla normal: se mira con luz de techo,
  de reojo y a veces con la pantalla al 30% de brillo por ahorro.
- **Áreas táctiles de 48 dp como mínimo**, y separadas: un toque errado tiene que
  ser difícil.
- **Nada que dependa solo del color.** Un ejercicio registrado se distingue por
  un ícono o un texto, no solo porque cambió de tono.
- **Textos cortos.** En 192 dp, una frase de dos renglones ya es larga.
- **Todo con `contentDescription`** (el equivalente del `alt`): la app se puede
  leer con TalkBack.
- **Sin animaciones largas.** Entra, se ve, se va.

---

## 13. Lo que no va

- **Nada de interacción ni JavaScript**: son maquetas.
- **Nada de editar la rutina desde el reloj.** La pantalla de 40 mm no da para
  escribir; eso se hace en la web, que ya existe.
- **Nada de registrar serie por serie.** Un peso por ejercicio, y punto.
- **Nada de timer de descanso** (es una idea para más adelante, sin compromiso).
- **Nada de gráficos de progresión.** El registro anterior sí; una curva de
  progreso, no.
- **Nada de calcular en qué semana estoy** a partir de una fecha: la elijo yo.
- **Nada de sincronización automática en segundo plano**, salvo que sea tu
  respuesta a 9.2 y la justifiques.
- **Nada de tema claro.**
- **Nada de login ni usuarios en el reloj.** Es mi reloj y mi servidor en mi
  casa.
