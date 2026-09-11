# Iteración 2 — Implementación técnica de la web

**Estado:** en planificación.

Este documento es para charlar lo técnico: stack, directorios, rutas, API,
React y cómo se trabaja y se despliega. El **diseño** está en:

- `docs/planificaciones/planificacion-it-2.md`: el brief que se le pasó al
  LLM de diseño.
- `assets/Fierros Web (offline).html`: el prototipo resultante. **Es la fuente
  de verdad visual**: colores, tipografía, medidas y textos se copian de ahí.

---

## 1. Decisiones tomadas

| Tema | Decisión |
|---|---|
| Dónde vive | En el repo de la API: `homelab.luis-server-node-api/fierros-web/` |
| Stack | React + Vite |
| Router | React Router con `BrowserRouter` (URLs limpias: `/fierros/anteriores`) |
| Cómo se sirve | El `Dockerfile` compila la web al hacer el deploy y la sirve Express. El build no va a git |
| API | Todos los endpoints pasan a `/api/fierros/...` (también el del reloj) |
| Estilo | Tal cual el prototipo: fondo índigo, Inter, lavanda |
| Responsive | Todavía no: se diseña y prueba para escritorio |
| Login y usuarios | Solo diseño; no se implementan en esta iteración |

**Por qué la API se muda a `/api`:** con `BrowserRouter`, Express tiene que
devolver el `index.html` de la web para cualquier URL bajo `/fierros/`
(`/fierros/anteriores`, `/fierros/actualizar`...), porque de esas rutas se
encarga React. Si la API siguiera colgando de `/fierros/rutina`, las dos cosas
se pisarían. Con `/api/fierros/...` para los datos y `/fierros/...` para la web,
cada una tiene lo suyo.

---

## 2. Estructura de directorios

```
homelab.luis-server-node-api/
├── main.js                       ← monta /api/fierros y /fierros
├── controllers/
│   ├── fierros-api.controller.js ← rutas de /api/fierros
│   └── fierros-web.controller.js ← /fierros: la web y el fallback de la SPA
├── service/
│   └── fierros.service.js        ← rutina, validar, historial, actividad, registros
├── public/
│   └── fierros/                  ← BUILD local de la web (npm run build; ignorado por git)
│       ├── index.html
│       └── assets/               ← js/css con hash en el nombre
└── fierros-web/                  ← CÓDIGO de la web (React + Vite)
    ├── package.json
    ├── vite.config.js
    ├── index.html
    ├── public/
    │   └── logo.png              ← el logo (assets/logo-render.png del repo de Fierros)
    └── src/
        ├── main.jsx              ← createBrowserRouter, basename "/fierros"
        ├── App.jsx               ← layout (navbar) + rutas
        ├── api/
        │   └── fierros.js        ← funciones fetch contra /api/fierros
        ├── estilos/
        │   ├── tokens.css        ← variables del prototipo (colores, radios, fuentes)
        │   └── global.css        ← reset, body, foco, selección, scrollbar
        ├── componentes/          ← piezas reutilizables (sección 6)
        ├── paginas/              ← una carpeta por pantalla (sección 3)
        ├── hooks/                ← useCarga, useAviso...
        └── util/                 ← resumen, superseries, fechas, JSON
```

Cada componente y cada página llevan su CSS al lado, como **CSS Module**
(`TablaDia.jsx` + `TablaDia.module.css`). Vite los soporta sin configurar nada y
evita que los estilos de una pantalla pisen los de otra.

---

## 3. Rutas de la web

`BrowserRouter` con `basename="/fierros"`:

| URL | Página | Notas |
|---|---|---|
| `/fierros/` | Rutina actual | Semana 1. Incluye los estados sin rutina, cargando y sin conexión |
| `/fierros/semana/:n` | Rutina actual | La pestaña de semana queda en la URL: se puede compartir o recargar |
| `/fierros/editar` | Editar la rutina actual | El mismo formulario, precargado. Guarda sobre la actual |
| `/fierros/actualizar` | Actualizar: Pegar JSON | Modo por defecto |
| `/fierros/actualizar/formulario` | Actualizar: Formulario | |
| `/fierros/anteriores` | Rutinas anteriores + Actividad | |
| `/fierros/anteriores/:id` | Una rutina anterior (solo lectura) | `:id` = nombre del archivo del historial |
| `/fierros/login` | Login | Más adelante (solo diseño por ahora) |
| cualquier otra | "No encontrada" dentro de la web | |

El **panel del ejercicio** (el que se abre desde la tabla) es estado de la
página, no una ruta: se abre y se cierra sin cambiar la URL.

### Del lado de Express

`main.js` monta un controller para la API y otro para la web:

```js
app.use("/api/fierros", fierrosApiRoutes);   // controllers/fierros-api.controller.js
app.use("/fierros", fierrosWebRoutes);       // controllers/fierros-web.controller.js
```

Dentro de `fierros-web.controller.js`, en este orden:

```js
// 1. Los archivos de la web (index.html, assets/...)
router.use(express.static(WEB_FIERROS));

// 2. Fallback de la SPA: cualquier otra URL bajo /fierros devuelve index.html
//    y React Router decide qué pantalla mostrar. (Express 5: comodín con nombre.)
router.get("/{*ruta}", (req, res) => res.sendFile(path.join(WEB_FIERROS, "index.html")));
```

- `index.html` se sirve con `Cache-Control: no-cache`, para que después de un
  deploy el navegador no quede con la versión vieja.
- Los archivos de `assets/` llevan un hash en el nombre (Vite los genera así),
  así que se pueden cachear "para siempre" (`immutable`).
- `/` ("Hola mundo") y `/dormitorio` no se tocan.

---

## 4. La API

### 4.1 Endpoints

Todos bajo `/api/fierros`:

| Método | Ruta | Fase | Qué hace |
|---|---|---|---|
| `GET` | `/rutina` | 0 | La rutina actual. Suma el header `Last-Modified` (para "Actualizada el...") |
| `POST` | `/rutina` | 0 | Guarda una rutina (la anterior pasa al historial). Igual que hoy |
| `GET` | `/registros` | 1 | Lo que se levantó por ejercicio. **Por ahora devuelve `[]`**: la web muestra "0 kg" |
| `POST` | `/rutina/validar` | 2 | Valida sin guardar: `200 { ok: true, resumen }` o `400 { error }`. Reutiliza `validarRutina()` |
| `GET` | `/historial` | 3 | Lista de rutinas anteriores: `[{ id, fecha, resumen }]`, de la más nueva a la más vieja |
| `GET` | `/historial/:id` | 3 | Una rutina anterior completa |
| `POST` | `/historial/:id/restaurar` | 3 | "Volver a usar esta rutina": la copia como actual (la actual pasa al historial) |
| `GET` | `/actividad?limite=50&antesDe=<fecha>` | 3 | Eventos del log, del más nuevo al más viejo. `antesDe` es para "Ver más" |

Formato propuesto de los **registros** (se cierra en la iteración 3, cuando el
reloj los mande):

```json
{ "semana": 1, "dia": 1, "letra": "A1", "peso": "20 kg", "fecha": "2026-09-15T19:40:00-03:00" }
```

### 4.2 Actividad: log en formato JSON por renglón

Hoy `fierros.log` es texto libre ("2026-09-11 02:30:23  rutina guardada: ...").
Para que la web lo lea sin adivinar el texto, los eventos nuevos se escriben en
**`fierros-actividad.jsonl`**, un JSON por renglón:

```json
{"fecha":"2026-09-11T02:35:38-03:00","tipo":"guardada","resumen":"4 semanas, 16 días, 112 ejercicios","anterior":"rutina-2026-09-11-023538.json","origen":"web"}
{"fecha":"2026-09-11T02:34:21-03:00","tipo":"rechazada","error":"semanas[0].numero tiene que ser un entero"}
{"fecha":"2026-09-11T02:31:32-03:00","tipo":"descargada"}
```

Tipos: `guardada`, `rechazada`, `descargada`, `pedida-sin-rutina`,
`restaurada`. `origen` distingue si vino de la web, del `curl` o del reloj.

`fierros.log` queda como está, con la historia vieja.

### 4.3 Transición del reloj a `/api`

**Sin ruta vieja.** El plan era dejar `/fierros/rutina` como alias mientras se
actualizaba el reloj, pero como la Fase 0 y la Fase 1 van juntas a producción, la
API pasa directo a `/api/fierros` y el alias no se deja:

1. Se despliega la API (con la web).
2. Enseguida se instala en el Watch8 la app que pide `/api/fierros/rutina`
   (README de Fierros, "Instalar y actualizar").
3. Se prueba que el reloj sincroniza contra la ruta nueva.

Entre el paso 1 y el 2, el reloj viejo no puede sincronizar: `/fierros/rutina`
ahora es una URL de la web y devuelve la página, no el JSON. La rutina que ya
tenía guardada se sigue viendo.

También hay que actualizar los `curl` del README de la API y de Fierros.

---

## 5. Estilos

Los valores salen del prototipo (`assets/Fierros Web (offline).html`, los
estilos escritos en cada elemento).

### `tokens.css`

```css
:root {
  --fondo: #161826;
  --superficie: #232532;        /* tarjetas */
  --superficie-barra: #1c1e2b;  /* navbar, barra de guardar, panel */
  --superficie-alta: #2e3140;   /* inputs, hover, avisos */
  --borde: #3a3d4d;
  --separador: #31343f;         /* entre filas */
  --texto: #e9e9ed;
  --texto-suave: #a5a8b8;
  --primario: #d0bcff;
  --primario-hover: #e0d1ff;
  --sobre-primario: #21005d;
  --primario-tenue: #4f378b;    /* pestaña activa, avatar, barra de superserie */
  --exito: #7dd3a0;
  --error: #f2b8b5;
  --error-fondo: #3b1d1d;

  --radio-pildora: 999px;
  --radio-tarjeta: 16px;
  --radio-input: 12px;

  --ancho-contenido: 1040px;
  --alto-navbar: 64px;

  --fuente: "Inter", system-ui, sans-serif;
  --fuente-mono: "Roboto Mono", ui-monospace, monospace;
}
```

- **Fuentes:** `@fontsource/inter` y `@fontsource/roboto-mono`, instaladas con
  npm. Quedan dentro del build y no dependen de Google Fonts ni de internet.
- **Íconos:** el prototipo usa **Phosphor Icons**. Se usa
  `@phosphor-icons/react` (solo entran al build los íconos usados).
- **Números:** `font-variant-numeric: tabular-nums` en series, pesos y fechas,
  como en el prototipo.
- **La hoja "Nocturne"** que viene en el prototipo es de la herramienta de
  diseño y casi no se usa; no se copia.

---

## 6. Componentes

| Componente | Dónde se usa | Qué hace |
|---|---|---|
| `Layout` + `Navbar` | Todas menos login | Logo, links (con el activo subrayado), avatar y menú del usuario (Mi cuenta / Cerrar sesión, sin acción por ahora) |
| `Boton` | Todas | Variantes `primario`, `secundario`, `texto`; estado deshabilitado |
| `PestanasSemana` | Rutina, vista previa, anterior, formulario | Píldoras "Semana N"; en la rutina navega a `/semana/:n` |
| `TablaDia` | Rutina, vista previa, anterior | Tarjeta de un día. Props: `dia`, `registros?` (si viene, muestra "Levanté"), `compacta?`, `onEjercicio?` |
| `PanelEjercicio` | Rutina | Panel lateral: plan, levanté, registros, lugar para progresión |
| `EditorJson` | Actualizar | Área de texto mono, resalta la línea con error |
| `FormularioRutina` | Actualizar | Días y filas editables, + ejercicio, + día, copiar semana |
| `ChipEstado` | JSON | "JSON válido" / "Hay un error" |
| `MensajeError` | JSON, login | Caja roja con ícono |
| `BarraGuardar` | Actualizar | Barra fija abajo con el texto y "Guardar rutina" |
| `Aviso` + `AvisosProvider` | Global | Píldora flotante abajo al centro: "¡Actualizado!" 2 s, errores 3 s (igual que el reloj) |
| `Esqueleto` | Rutina | Estado cargando |
| `SinConexion` | Todas las que leen datos | "No se pudo conectar con el servidor" + Reintentar |

**Superseries** (`util/superseries.js`): una letra con número (A1, A2) es
superserie. Dos filas seguidas con la misma letra base (A1 → A2) van unidas por
la barra y **sin separador** entre ellas. Es la misma lógica que tiene el
prototipo (`prepararDia`).

---

## 7. Datos y estado

- **Sin librerías de estado ni de datos.** Son pocos endpoints y pantallas
  independientes: alcanza con `fetch` y un par de hooks.
- `api/fierros.js`: una función por endpoint (`obtenerRutina`, `validar`,
  `guardar`, `obtenerHistorial`...). Un 404 de la rutina devuelve `null` (no hay
  rutina); un fallo de red lanza un error de "sin conexión".
- `hooks/useCarga(fn)`: devuelve `{ estado, datos, reintentar }`, con `estado`
  en `cargando` / `ok` / `vacio` / `sinConexion`. Las pantallas eligen qué
  dibujar según eso.
- `util/resumen.js`: "4 semanas · 16 días · 112 ejercicios", calculado en el
  navegador (igual que `resumir()` en la API).
- **Pegar JSON:**
  1. Al escribir, espera 500 ms sin cambios.
  2. Primero `JSON.parse` en el navegador: si falla, calcula en qué renglón
     está el error (a partir de la posición que da el mensaje) y lo marca.
  3. Si parsea, `POST /rutina/validar`: el error de la API se muestra tal
     cual y la vista previa aparece cuando es válido.
- **Formulario:** trabaja sobre una copia en memoria de la rutina, arma el mismo
  JSON y usa `validar` y `guardar`. Si hay cambios sin guardar, avisa al salir
  (`useBlocker` de React Router y `beforeunload` del navegador).
- **"Actualizada el...":** sale del header `Last-Modified` de `GET /rutina`.

---

## 8. Desarrollo local

Dos terminales:

```powershell
# 1) La API, en la raíz del repo de la API (datos de prueba en ./data)
$env:PORT = "3001"; node main.js

# 2) La web, con recarga en caliente
cd fierros-web
npm run dev          # http://localhost:5173/fierros/
```

`vite.config.js`:

```js
export default defineConfig({
  plugins: [react()],
  base: "/fierros/",
  build: { outDir: "../public/fierros", emptyOutDir: true },
  server: {
    proxy: { "/api": "http://localhost:3001" },  // la API local, sin CORS
  },
});
```

Para probar contra los datos reales, el proxy puede apuntar a
`http://192.168.100.240:3000`. **Ojo:** ahí guardar pisa la rutina de verdad
(aunque la anterior queda en el historial).

**Requisito en la PC:** hoy tiene Node 21.1, una versión impar que ya no tiene
soporte. Las versiones actuales de Vite piden Node 20.19+ o 22.12+. Conviene
instalar **Node 22 LTS** antes de arrancar.

---

## 9. Build y deploy

Se commitea solo el código (`fierros-web/src`, etc.). En el servidor, como siempre:

```bash
cd /opt/services/node-api
git pull
docker compose up -d --build
```

- **El build lo hace Docker.** El `Dockerfile` tiene una etapa con Node 24 que
  corre `npm ci` y `npm run build` en `fierros-web/` y deja la web en
  `/opt/fierros-web`. La imagen final la copia y define
  `FIERROS_WEB_DIR=/opt/fierros-web`, que es de donde la sirve
  `fierros-web.controller.js`.
- **Por qué fuera de `/app`:** el compose monta el repo en `/app` (`.:/app`),
  así que lo que la imagen tenga ahí queda tapado por la carpeta del servidor.
- **Siempre con `--build`**, aunque el cambio sea solo de la web: sin eso no se
  vuelve a compilar.
- En la PC, sin Docker, Express sirve `public/fierros/` (lo genera
  `npm run build`); para programar conviene `npm run dev` (Vite, puerto 5173).
- (Hasta la fase 4 el build se commiteaba y el deploy de la web era solo
  `git pull`. Se cambió para no commitear archivos generados ni desplegar un
  build viejo por olvidarse de compilar.)

### `.gitignore` de la API

```
node_modules          ← también el de fierros-web/
.env
.env.*.local
*.local
data                  ← datos de prueba locales
public/fierros        ← build local de la web (en el servidor lo compila Docker)
fierros-web/dist      ← por si alguien compila a la carpeta por defecto
.vite
npm-debug.log*
coverage
```

Y un `.dockerignore` para que la imagen no copie `node_modules`, `public/fierros`,
`data`, `.env` ni `.git`.

---

## 10. Fases, en técnico

Cada fase termina desplegada y usable.

### Fase 0 — Base

- Crear `fierros-web/` con Vite (plantilla React), `vite.config.js`, fuentes,
  `tokens.css` y `global.css`.
- `Layout` + `Navbar` + rutas vacías ("en construcción") para cada página.
- Express: mudar la API a `/api/fierros` (sin alias de la ruta vieja, ver 4.3),
  servir `public/fierros/` y el fallback de la SPA.
- Reloj: `ApiFierros` pide `/api/fierros/rutina`; instalar y probar.
- Actualizar los READMEs (API y Fierros) con las rutas nuevas.
- **Listo cuando:** `http://192.168.100.240:3000/fierros/` muestra el navbar,
  recargar en `/fierros/anteriores` no da 404, y el reloj sincroniza por `/api`.

### Fase 1 — Ver la rutina

- `Rutina`: encabezado con resumen y fecha, botones, `PestanasSemana` con
  `/semana/:n`, `TablaDia` con superseries y "Levanté", `PanelEjercicio`.
- Estados: `Esqueleto`, sin rutina, `SinConexion`.
- API: `Last-Modified` en `GET /rutina` y `GET /registros` (lista vacía).
- **Listo cuando:** se ve la rutina real igual que en el prototipo, con "0 kg"
  en Levanté, y cada estado se puede provocar (sin rutina, API apagada).

### Fase 2 — Actualizar con JSON

- `EditorJson`, validación en dos pasos, vista previa, `BarraGuardar`, aviso
  "¡Actualizado!" y vuelta a la rutina.
- API: `POST /rutina/validar`.
- **Listo cuando:** se carga `prueba-a.json`, se ve en la rutina y el reloj la
  sincroniza. Un JSON roto marca el renglón y un JSON inválido muestra el error
  de la API, sin poder guardar.

### Fase 3 — Anteriores y actividad

- Páginas `Anteriores` y `AnteriorDetalle` (solo lectura, "Volver a usar esta
  rutina").
- API: `historial`, `historial/:id`, `restaurar`, `actividad`, y el log nuevo
  `fierros-actividad.jsonl`.
- **Listo cuando:** se ven las rutinas del historial real, se restaura una y
  pasa a ser la actual, y la actividad muestra cargas, rechazos y descargas del
  reloj.

### Fase 4 — Formulario

- `FormularioRutina`: precarga la actual, edita, agrega y borra días y
  ejercicios, sugiere la letra, copia la semana, marca errores por campo y avisa
  si hay cambios sin guardar.
- **Listo cuando:** se corrige un ejercicio y se guarda, y se arma una rutina
  chica desde cero.

### Después

- Responsive (celular).
- Login y usuarios (sesiones en la API, `/fierros/login`).
- Registro real de pesos desde el reloj (iteración 3): `POST /registros`, y la
  web ya los muestra.

---

## 11. Decisiones de detalle

- **TypeScript.** Los archivos de `fierros-web/src` son `.ts`/`.tsx` (donde el
  documento dice `.jsx`/`.js`, léase `.tsx`/`.ts`). La rutina, los registros y
  la actividad tienen tipos que reflejan la API.
- **La pestaña de semana va en la URL** (`/fierros/semana/2`).
- **`/` sigue siendo el "Hola mundo"** de la API; no redirige a la web.
- **Tests con Vitest** para lo que tiene lógica (superseries, resumen, línea del
  error de JSON). Las pantallas se prueban a mano contra el prototipo.
- **Node 24** en la PC (instalado para esta iteración).
- **La web no ensucia el log.** Sus pedidos a `GET /rutina` mandan el header
  `X-Fierros-Origen: web` y la API no los anota: en el log (y en la actividad de
  la fase 3) las descargas son solo las del reloj o del `curl`.
- **Sin registros**, el panel del ejercicio muestra "0 kg" en Levanté, oculta
  "último registro" y dice "Todavía no hay registros." en la lista.
- **Una semana que no existe** en la URL (`/semana/9`) lleva a la primera.
- **Pegar JSON:** el editor es un `<textarea>` sin corte de renglones; el renglón
  con error se pinta con una franja detrás del texto que acompaña el scroll. El
  renglón sale del mensaje de `JSON.parse` (`line N` o `position N`, según el
  navegador); si el JSON quedó cortado, se marca el último.
- **Mientras se escribe** queda a la vista la revisión anterior con "Revisando…",
  y "Guardar rutina" se deshabilita hasta que termina.
- **`/rutina/validar` no deja nada en el log**: es una consulta, no una carga.
- **Los avisos flotantes suben** cuando está la barra de guardar, para no taparla.
- **El historial se nombra con la fecha en que se cargó cada rutina** (el mtime de
  `rutina.json` al archivarla), no con la fecha en que se reemplazó: es la que
  muestran Anteriores y el título "Rutina del...". Los archivos de antes de la
  fase 3 quedan con la fecha en que se reemplazaron. Archivar nunca pisa: si el
  nombre existe, agrega `-2`, `-3`...
- **Origen de cada evento:** `web` (header `X-Fierros-Origen`), `reloj` (el
  User-Agent de OkHttp/Ktor), `curl` u `otro`.
- **`fierros.log` se sigue escribiendo** (en texto, con el origen entre
  corchetes) para leerlo por ssh; la web lee solo `fierros-actividad.jsonl`, que
  arranca vacío: la historia de antes no se migra.
- **`Cache-Control: no-cache`** en todo `/api/fierros`, puesto una vez en el
  router.
- **Router "de datos"** (`createBrowserRouter` + `RouterProvider`, con
  `basename: "/fierros"`) en vez de `<BrowserRouter>`: `useBlocker` solo anda
  así. Las URLs son las mismas.
- **Formulario:**
  - Semanas y días se numeran por su posición al guardar.
  - **Hasta 5 semanas** (`MAX_SEMANAS`): más no tiene sentido para un
    mesociclo. Al llegar a 5 desaparece el "+".
  - "+" al lado de las semanas agrega una **copia de la última** (las rutinas
    suelen repetir la estructura). "Copiar esta semana a la siguiente" copia los
    días a la semana que sigue; si no existe, la crea. Solo pregunta cuando esa
    semana ya existe, porque le pisa los días. Borrar un día o una semana
    también pregunta.
  - **El peso no se carga en el formulario** (se levanta en el gimnasio): las
    filas nuevas van con "0 kg" y las que ya tenían peso lo conservan.
  - **Al guardar, los ejercicios de cada día se ordenan por letra** (A, A1, A2,
    B...), así se pueden cargar en cualquier orden.
  - No se puede borrar el último ejercicio de un día ni el último día de una
    semana (el botón no aparece): así no hay días ni semanas vacíos.
  - **"Editar rutina"** (botón en la pantalla Rutina → `/fierros/editar`) abre el
    mismo formulario precargado con la rutina actual, con la fecha de carga y el
    resumen arriba, y abajo "Descartar cambios" y "Guardar cambios". No tiene las
    pestañas Pegar JSON / Formulario ni "Empezar de cero": es para retocar lo que
    ya está. En el navbar queda marcado Rutina. La lógica (errores, guardar,
    aviso de cambios sin guardar) está en `hooks/useEdicionRutina.ts`, que
    comparten las dos pantallas.
  - **El formulario de Actualizar arranca siempre vacío** (una semana, un día, un ejercicio):
    es para armar una rutina. "Cargar la rutina actual" trae la guardada para
    editarla, y "Empezar de cero" vuelve a dejarlo vacío. Las dos preguntan si
    hay algo cargado.
  - Los errores de un campo aparecen al salir de él o al tocar "Guardar"; en
    ese caso va a la semana del primer error y le pone el foco. Obligatorios:
    letra, ejercicio, series (entero mayor que 0) y reps. Peso y nota, no.
  - "Guardar" se habilita cuando hay cambios. Salir con cambios sin guardar
    pregunta (dentro de la web con `useBlocker`; al recargar o cerrar, el
    navegador).
