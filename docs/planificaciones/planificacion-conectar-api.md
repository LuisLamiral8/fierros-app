# Conectar Fierros con la API del homelab

Cierra la iteración 1: hoy la rutina llega al reloj a mano con `adb push`. Con
esto llega tocando **Ajustes → Sincronizar datos**.

## 1. Objetivo

- Cargar la rutina en la API desde la compu, con el JSON que arma el LLM.
- Bajarla al reloj desde casa con el botón de sincronizar, y guardarla para
  usarla offline en el gimnasio.

Nada más. Sin sincronización automática, sin subir registros (eso es la
iteración 2) y sin web de carga (iteración 3).

## 2. Cómo viaja la rutina

```
 Mynter ──screenshots──► LLM ──► rutina.json (compu)
                                     │
                                     │ POST /fierros/rutina   (curl, a mano)
                                     ▼
                           API Node en el homelab
                           guarda rutina.json en disco
                                     ▲
                                     │ GET /fierros/rutina    (botón "Sincronizar datos")
                                     │
                           Reloj, en el Wi-Fi de casa
                           valida → guarda en filesDir → la muestra
```

La API es un depósito: guarda un archivo y lo devuelve. No arma ni modifica
rutinas.

## 3. Lo que tiene que cumplir la API

### 3.0 La API que ya existe

El código está en `homelab.luis-server-node-api`:

- **Express 5** (CommonJS), organizado como un proyecto Spring: `controllers/`
  define las rutas (un `Router` por área, montado en `main.js`, por ejemplo
  `app.use("/dormitorio", ...)`) y `service/` tiene la lógica. El puerto sale
  de `PORT` en el `.env`.
- Ya usa `express.json()`, así que el body del `POST` llega parseado. Su límite
  por defecto (100 KB) alcanza: la rutina pesa unos 28 KB.
- Corre en **Docker** en el servidor `192.168.100.240`, puerto `3000`, desde
  `/opt/services/node-api` (imagen `node:20-alpine`). El `Dockerfile` y el
  `docker-compose.yml` están solo en el servidor, no en el repo.
- El compose monta la carpeta del proyecto entera dentro del contenedor
  (`.:/app`): lo que la API escriba en `/app` queda en el disco del servidor.

Fierros se suma igual que dormitorio: `controllers/fierros.controller.js` y
`service/fierros.service.js`, montado en `/fierros`. Las rutas quedan
`GET /fierros/rutina` y `POST /fierros/rutina`.

### 3.1 Endpoints

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| `GET` | `/fierros/rutina` | Devuelve la rutina actual | `200` con el JSON · `404` si nunca se cargó ninguna |
| `POST` | `/fierros/rutina` | Reemplaza la rutina actual | `200` si la guardó · `400` con el motivo si el JSON no es válido |

- Las respuestas van con `Content-Type: application/json; charset=utf-8`. Los
  nombres llevan tildes y "×".
- El `GET` devuelve el archivo tal cual se cargó, sin transformarlo.
- Los errores devuelven un JSON corto, por ejemplo
  `{ "error": "falta semanas[0].dias" }`, para entender qué pasó sin mirar logs.

### 3.2 Validación en el POST

La API rechaza lo que el reloj no podría leer. Es preferible enterarse en la
compu y no en el gimnasio. El formato es el de `datos/rutina.json`:

- `semanas`: lista no vacía. Cada semana tiene `numero` (entero) y `dias` (lista
  no vacía).
- Cada día tiene `numero` (entero), `nombre` opcional (texto) y `ejercicios`
  (lista no vacía).
- Cada ejercicio tiene `letra`, `nombre`, `reps` y `peso` (texto), `series`
  (entero) y `nota` (texto o `null`).
- Si hay campos de más, se aceptan: el reloj los ignora.

Alcanza con validar a mano con unos `if`. No hace falta una librería de esquemas
para esto.

### 3.3 Guardado

- **Un archivo en disco** (por ejemplo `data/rutina.json`), sin base de datos.
- **Carpeta aparte, fuera del código:** `/opt/services/fierros-data` en el
  servidor, montada en el contenedor como `/data`. La API la toma de
  `FIERROS_DATA_DIR`. En el `docker-compose.yml`:

  ```yaml
      environment:
        - FIERROS_DATA_DIR=/data
        - TZ=America/Argentina/Buenos_Aires   # fechas del historial y del log en hora local
      volumes:
        - .:/app
        - /app/node_modules
        - /opt/services/fierros-data:/data
  ```

  Por el `.:/app`, un archivo en `/app/data` hoy ya sobreviviría a recrear el
  contenedor, pero quedaría mezclado con el código: un re-clone o un deploy que
  limpie la carpeta se llevaría la rutina. Con la carpeta aparte, eso no pasa.

  La carpeta depende de dónde corre la API, parecido a un perfil de Spring:

  | Dónde corre | `FIERROS_DATA_DIR` | Dónde queda `rutina.json` |
  |---|---|---|
  | Tu PC (`npm run dev`) | sin definir: usa `./data` | `homelab.luis-server-node-api/data/` (ignorada por git) |
  | Servidor (Docker) | `/data`, definida en el compose | `/opt/services/fierros-data/` en el disco del servidor |

  El código es el mismo en los dos lados; solo cambia la variable de entorno.
- **Escritura atómica:** se escribe primero a un temporal y después se renombra
  encima del original. Así, si algo falla a mitad del guardado, la rutina
  anterior no se rompe.
- **Historial:** antes de reemplazar la rutina, la actual se copia a
  `historial/rutina-AAAA-MM-DD-HHmmss.json`. El reloj también guarda el suyo.
- **Log:** `fierros.log`, con un renglón con fecha y hora por cada rutina
  guardada (con un resumen y a dónde fue la anterior), cada rutina rechazada
  (con el motivo) y cada descarga del reloj. Si no se puede escribir el log, la
  request sigue igual.

  ```
  fierros-data/
  ├── rutina.json            ← la actual, la que baja el reloj
  ├── historial/             ← cada rutina reemplazada
  └── fierros.log            ← qué pasó y cuándo
  ```

### 3.4 Red

- **La IP del servidor no es fija.** No hay reserva de DHCP (no hay acceso al
  router), aunque en casi un año nunca cambió. Por eso la URL de la API se puede
  cambiar desde Ajustes en el reloj (4.4), sin recompilar.
- **El reloj le pega directo a `http://192.168.100.240:3000`**, sin pasar por
  Pi-hole ni Nginx. Probado desde la PC: responde `Hola mundo`.
  - La IP sola (puerto 80) cae en la página por defecto de Nginx Proxy Manager.
  - `http://api.luis` redirige a `https://api.luis`, que usa los certificados
    propios del homelab (desde la PC ni siquiera se completó la conexión TLS).
    El reloj tendría que confiar en ese certificado y además depender del DNS
    que use. Por IP y puerto hay menos piezas que pueden fallar.
- **HTTP en claro, solo en la red de casa.** No se expone a internet, no hay
  HTTPS ni usuarios. Son datos de gimnasio, en tu red y con un solo cliente.
- **Sin CORS por ahora:** el reloj no es un navegador. Va a hacer falta en la
  iteración 3, cuando exista la web de carga.

### 3.5 Cómo se prueba (desde la compu)

```bash
# Cargar la rutina
curl -X POST http://192.168.100.240:3000/fierros/rutina \
     -H "Content-Type: application/json" \
     --data-binary @datos/rutina.json

# Bajarla, igual que va a hacer el reloj
curl http://192.168.100.240:3000/fierros/rutina

# Un JSON roto tiene que devolver 400 y no pisar la rutina guardada
curl -X POST http://192.168.100.240:3000/fierros/rutina -H "Content-Type: application/json" -d '{"semanas": []}'
```

### 3.6 Deploy

El código llega al servidor con `git pull`. El `docker-compose.yml` no está en
el repo, así que el volumen se agrega a mano, una sola vez.

**Una sola vez, en el servidor:**

1. Editar `/opt/services/node-api/docker-compose.yml` y agregar el
   `environment` y el volumen de 3.3.
2. Correr `docker compose up -d --build` para recrear el contenedor con el
   volumen. Si `/opt/services/fierros-data` no existe, Docker la crea.

**Cada vez que cambia el código:**

```bash
cd /opt/services/node-api
git pull
docker compose up -d --build
```

Como el compose monta la carpeta con `.:/app`, para cambios que solo tocan
código también alcanza con `docker compose restart node-api`.

**No subir el `Dockerfile` ni el `docker-compose.yml` al repo sin preparar el
servidor:** el próximo `git pull` fallaría, porque ahí ya existen sin
versionar. Si se suben, antes del pull hay que renombrarlos en el servidor (por
ejemplo a `docker-compose.yml.bak`) y después comparar.

## 4. Lo que tiene que hacer Fierros

### 4.1 Permisos y configuración de red

- **Permiso `INTERNET`** en el `AndroidManifest.xml`. Hoy la app no lo tiene.
- **`network_security_config.xml`** que permite HTTP en claro, declarado en el
  manifest (Android lo bloquea por defecto). Como la IP se puede cambiar desde el
  reloj, no alcanza con habilitar una sola IP: se habilita para toda la app. Es
  aceptable porque la app solo habla con el homelab.
- **URL de la API: un valor por defecto que se puede cambiar en el reloj.**
  - Por defecto sale de `wear/local.properties`
    (`api.url.defecto=http://192.168.100.240:3000`) y queda como
    `BuildConfig.API_URL_POR_DEFECTO`, igual que un `@Value("${api.url.defecto}")`.
    `local.properties` no se versiona: en una PC nueva hay que agregar esa línea,
    y si falta, la compilación falla diciendo qué poner.
  - Si se cambia desde Ajustes (4.4), el valor nuevo se guarda en el reloj
    (`DataStore`, las preferencias de Android) y tiene prioridad sobre el de por
    defecto.

### 4.2 Cliente HTTP

- **Ktor Client**, como decía la planificación de la iteración 1, con un único
  `GET /fierros/rutina`.
- El cuerpo se lee como texto y se valida con el mismo lector que ya usa la app
  (`RutinaRepository`, con `kotlinx.serialization`). Si el JSON no parsea, **no se
  guarda nada**.
- **Timeout corto**, unos 10 segundos. Si no hay red, que falle rápido y avise.
- La llamada corre en segundo plano (coroutines, `Dispatchers.IO`), igual que
  hoy la lectura del archivo.

### 4.3 Guardado en el reloj

Es lo mismo que ya describe `planificacion-it-1.md`:

1. `GET /fierros/rutina`.
2. Parsear el JSON. Si falla, no se toca nada.
3. Escribirlo a un temporal en `filesDir`.
4. Copiar la rutina actual a `historial/`, con fecha **y hora** en el nombre
   (`rutina-AAAA-MM-DD-HHmmss.json`, igual que en la API). Se copia en vez de
   moverla para que el reloj nunca quede sin rutina.
5. Renombrar el temporal a `rutina.json`.
6. Recargar la rutina en pantalla, sin tener que cerrar la app.

### 4.4 Pantalla de Ajustes

Al tocar **Sincronizar datos**:

| Estado | Qué se ve |
|---|---|
| Sincronizando | "Sincronizando…". El botón no responde a un segundo toque |
| Salió bien | "Rutina actualizada" |
| Sin conexión o timeout | "No se pudo conectar. ¿Estás en el Wi-Fi de casa?" |
| La API no tiene rutina (404) | "El servidor no tiene ninguna rutina cargada" |
| JSON inválido | "La rutina del servidor no es válida" y se conserva la anterior |

Además hay un segundo ítem, **Servidor**, que muestra la URL actual. Al tocarlo
se abre el teclado del reloj (el mismo de los mensajes, con dictado) para
escribir la nueva, por ejemplo `http://192.168.100.57:3000`. Se valida que
empiece con `http://` y se guarda. Se usa una vez cada mucho, así que alcanza
con el teclado del sistema.

### 4.5 Estado vacío

Hoy, si el reloj nunca sincronizó, la app dice "No hay rutina guardada" pero no
muestra la tuerquita, así que no hay forma de sincronizar. Con esto, esa pantalla
tiene que ofrecer **Sincronizar datos** directamente.

### 4.6 Cambio de estructura

Hoy la rutina se lee una sola vez, al abrir la app. Para poder recargarla después
de sincronizar, el estado pasa a un **`ViewModel`**: un objeto que guarda la
rutina, sabe cargarla y sincronizarla, y sobrevive si el sistema recrea la
pantalla. Es lo más parecido a un `@Service` con estado. Las pantallas lo leen y
se actualizan solas cuando cambia.

### 4.7 El reloj real y la red

Es el riesgo principal de esta parte, y en el emulador no aparece:

- Con el celular cerca, el Galaxy Watch suele salir a la red **a través del
  celular, por Bluetooth**. Hay que probar si por ese camino llega a una IP de
  la LAN.
- Si no llega, la app pide explícitamente **usar el Wi-Fi** durante la
  sincronización (`ConnectivityManager.requestNetwork` con transporte Wi-Fi).
  Es un agregado acotado, pero se decide después de probarlo en el reloj.

## 5. Orden de trabajo

Cada paso termina con algo que se puede probar.

1. **API:** `GET` y `POST /fierros/rutina` con guardado en archivo. Se prueba en
   la PC con los `curl` de 3.5 y se carga `datos/rutina.json`.
2. **Servidor:** volumen en el `docker-compose.yml` y deploy (3.6). Se prueba con
   los mismos `curl` contra `192.168.100.240`.
3. **App, configuración:** permiso `INTERNET`, `network_security_config.xml` y
   `api.url.defecto` en `local.properties`. Compila y no cambia nada visible.
4. **App, sincronizar:** `ViewModel`, cliente Ktor, botón con estados, guardado
   atómico e historial. Se prueba en el emulador contra la API real.
5. **App, estado vacío:** borrar `rutina.json` del emulador y sincronizar desde
   cero.
6. **App, servidor editable:** ítem **Servidor** en Ajustes. Se prueba poniendo
   una URL equivocada (la sincronización falla y avisa) y volviendo a la buena.
7. **Reloj real:** sideload y sincronización desde casa. Si no llega a la LAN
   por Bluetooth, se suma el pedido de Wi-Fi (4.7).

Mientras la API no esté lista, la app se puede probar contra un servidor de
archivos cualquiera que sirva `datos/rutina.json` en `/fierros/rutina`.

## 6. Fuera de alcance

- Subir registros de peso (iteración 2).
- Web para cargar y corregir rutinas (iteración 3).
- Sincronización automática o en segundo plano.
- HTTPS, usuarios, tokens, acceso desde fuera de casa.
- Mostrar el historial en el reloj.

## 7. Riesgos

- **El reloj no llega a la LAN por el Bluetooth del celular** (4.7). Se prueba en
  el paso 7.
- **Cambia la IP del homelab** (no es fija). Se corrige desde Ajustes →
  Servidor, sin recompilar.
- **Una rutina mal cargada pisa la buena.** Se evita con la validación del POST
  (3.2) y con la del reloj antes de guardar (4.3).

## 8. Para definir

- Si más adelante se suben el `Dockerfile` y el `docker-compose.yml` al repo
  (ver el cuidado en 3.6).

## 9. Cuándo está listo

- Un `POST` con `datos/rutina.json` desde la compu, y en el reloj **Sincronizar
  datos** muestra "Rutina actualizada" y la rutina nueva, sin reiniciar la app.
- Un reloj sin rutina puede sincronizar desde la pantalla vacía.
- Sin red, la app avisa y conserva la rutina que tenía.
- Se puede cambiar la URL desde Ajustes → Servidor y sincronizar contra la
  nueva.
- Una rutina inválida la rechaza la API, y si igual llegara al reloj, el reloj
  no la guarda.
- Funciona en el Galaxy Watch8 real, no solo en el emulador.
