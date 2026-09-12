# Fierros

App de Wear OS para consultar y registrar rutinas de gimnasio desde el reloj,
sin depender del celular.

---

## El problema

Voy al gimnasio dejando el celular en casa por seguridad. La rutina me la arma
mi entrenador y vive en Mynter, una plataforma de gestión para entrenadores
personales que no tiene app de Wear OS.

Sin el celular encima me pasan dos cosas:

1. **No tengo la rutina.** No sé qué ejercicio toca, ni cuántas series, ni con
   cuánto peso.
2. **No tengo dónde anotar.** Cuando vuelvo a casa cargo en Mynter lo que me
   acuerdo, y de memoria el registro sale mal.

## Qué hace Fierros

Vive en el reloj y funciona sin conexión. En el gimnasio no hay señal y el
celular está en casa, así que todo tiene que andar offline.

- Muestra la rutina del día: ejercicios, series, repeticiones y peso.
- Permite anotar cuánto levanté en cada ejercicio.
- Se sincroniza con un servidor propio cuando vuelvo al Wi-Fi de casa.

La rutina se carga desde una API que corre en mi homelab. El reloj se la baja
cuando está en casa y la guarda para usarla offline.

## Lo que Fierros no hace

**No se integra con Mynter.** No hay forma de conectarse automáticamente, así
que lo que registre en el reloj lo tengo que volver a cargar a mano en Mynter
para que mi entrenador lo vea. Es doble carga y es una molestia real, pero es
el precio de dejar el celular en casa.

Tampoco edita rutinas desde el reloj. La pantalla de 40mm no da para escribir;
la carga se hace desde la compu.

## Roadmap

Cada iteración tiene que ser usable sola. Si una no me sirve en la práctica, me
ahorro las siguientes.

### Iteración 1 — Visualizar

Ver la rutina del día en el reloj. Se baja de la API una vez y queda guardada
para usar offline. Solo lectura.

**Para qué:** contestar la pregunta más barata del proyecto, que es si voy a
usar la app. Mirar la muñeca entre serie y serie puede ser cómodo o puede ser
un fastidio, y no lo sé hasta probarlo. Dos semanas de uso real lo contestan.
Si no la uso, todo lo demás es trabajo tirado.

**Estado:** desarrollo cerrado el 2026-09-11. En validación: dos semanas de uso
real. Cierre en
[`planificacion-it-1.md`](docs/planificaciones/planificacion-it-1.md#9-cierre).

### Iteración 2 — Cargar

Una web en el homelab para ver la rutina actual con lo que levanté en cada
ejercicio, actualizarla de dos formas (pegando el JSON que arma el LLM o con un
formulario) y ver las rutinas anteriores y la actividad del servidor. El login y
el navbar con el usuario se diseñan ahora, pensando en un sistema de usuarios
más adelante.

**Para qué:** dejar de cargar la rutina con `curl`, y tener listo el lugar para
mirar los pesos antes de empezar a registrarlos. Si el reloj registrara primero,
los datos no se podrían ver en ningún lado.

**Estado:** cerrada el 2026-09-11. Desplegada en el homelab y andando: se ve la
rutina, se carga pegando el JSON o con el formulario, se edita la actual, y están
las rutinas anteriores con la actividad. El reloj sincroniza por `/api`. Cierre en
[`planificacion-it-2-implementacion-tecnica.md`](docs/planificaciones/planificacion-it-2-implementacion-tecnica.md#12-cierre).

**Dónde vive:** el código está en el repo de la API
(`homelab.luis-server-node-api/fierros-web/`), con su propio README.

**Diseño:** [`planificacion-it-2.md`](docs/planificaciones/planificacion-it-2.md)
(brief para diseñar las maquetas; el resultado está en
`assets/Fierros Web (offline).html`, y las mejoras de editar la rutina en
`assets/fierros_web_con_editar.html`).
**Implementación técnica:**
[`planificacion-it-2-implementacion-tecnica.md`](docs/planificaciones/planificacion-it-2-implementacion-tecnica.md).

### Iteración 3 — Registrar

Anotar el peso levantado en cada ejercicio, una vez por ejercicio y no por cada
serie. Se guarda en el reloj, se sincroniza a la API cuando vuelvo a casa y se ve
en la web, en formato copiable para volcarlo a Mynter.

**Para qué:** que el registro en Mynter deje de salir de la memoria.

El orden de las iteraciones 2 y 3 se invirtió el 2026-09-11: primero la web, para
tener dónde ver los registros.

### Después (sin compromiso)

- Timer de descanso con vibración.
- Un acceso rápido en el reloj para ver el próximo ejercicio sin abrir la app.
- Histórico y progresión.

Nada de esto se decide hasta que las tres primeras iteraciones estén andando y
usadas.

## Instalar y actualizar en el reloj

La app se instala por ADB (sideload). Queda instalada como cualquier otra: aparece
en la lista de apps, funciona sin la PC y anda offline. No se actualiza sola: cada
versión nueva se instala de la misma forma desde esta PC.

### Lo que hace falta en la PC

- `adb` en el `PATH` (está en `E:\Android\Sdk\platform-tools`).
- En `wear/local.properties`, la URL por defecto de la API:
  `api.url.defecto=http://<ip-del-servidor>:3000`. Sin esa línea no compila.
- El reloj y la PC en el mismo Wi-Fi.

### Una sola vez: preparar el reloj y emparejarlo

1. **Opciones de desarrollador:** en el reloj, Ajustes → Información del reloj →
   Información de software → tocar **Versión de software** 5 veces, hasta que
   avise que se activaron. Los nombres pueden variar un poco según la versión.
2. En **Ajustes → Opciones de desarrollador**, activar **Depuración ADB** y
   **Depuración inalámbrica**.
3. Entrar a **Depuración inalámbrica → Vincular dispositivo nuevo**. El reloj
   muestra una IP:puerto de vinculación y un código de 6 dígitos (vence rápido).
4. En la PC:

   ```powershell
   adb pair <ip>:<puerto-de-vinculacion> <codigo>
   ```

   Tiene que responder `Successfully paired`. El emparejamiento queda guardado.

### Cada vez que se instala o actualiza

1. En el reloj, entrar a **Depuración inalámbrica** (si se apagó, activarla).
   Arriba muestra la **IP:puerto** para conectar. Es un puerto distinto al de
   vinculación y cambia cada vez que se activa.
2. Conectar y comprobar que aparece:

   ```powershell
   adb connect <ip>:<puerto>
   adb devices          # tiene que listar <ip>:<puerto>  device
   ```

   El reloj puede aparecer dos veces: por IP y como
   `adb-XXXX._adb-tls-connect._tcp`. Es el mismo; en `-s` se usa la entrada por
   IP.
3. Compilar la versión nueva:

   ```powershell
   cd wear
   .\gradlew.bat assembleDebug
   ```

   El APK queda en `wear\app\build\outputs\apk\debug\app-debug.apk`.
4. Instalar (o actualizar) en el reloj:

   ```powershell
   adb -s <ip>:<puerto> install -r app\build\outputs\apk\debug\app-debug.apk
   ```

   `-r` reemplaza la versión anterior y **conserva los datos**: la rutina y la URL
   del servidor guardadas. `-s` elige el reloj si también está el emulador
   conectado.
5. Abrir **Fierros** desde la lista de apps del reloj.
6. Apagar **Depuración inalámbrica** y **Depuración ADB** hasta la próxima
   actualización: ahorra batería y el reloj deja de aceptar conexiones de
   depuración. La app sigue instalada y sincroniza igual (no usa ADB). Para
   actualizar, se vuelven a activar las dos y se sigue desde el paso 1; el
   emparejamiento queda guardado y no hace falta repetirlo. No hace falta apagar
   las opciones de desarrollador enteras.

Alternativa desde Android Studio: con el reloj conectado (paso 2), elegirlo en el
selector de dispositivos y darle **Run ▶**. Hace los pasos 3 a 5 solo.

### Si algo falla

| Síntoma | Qué pasa y qué hacer |
|---|---|
| `failed to connect` en `adb connect` | El puerto cambió, el reloj no está en el mismo Wi-Fi o se apagó la depuración inalámbrica (se apaga sola al perder el Wi-Fi). Volver a mirar la IP:puerto en el reloj. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | La versión instalada se firmó en otra PC. `adb -s <ip>:<puerto> uninstall com.luis.fierros` y volver a instalar. Se pierden la rutina y la URL guardadas: se vuelve a sincronizar. |
| La sincronización falla en el reloj | Ver el motivo real con `adb -s <ip>:<puerto> logcat -s Fierros`. |

## ToDo

### Iteración 1 — desarrollo cerrado, falta la validación

- [x] Resolver el sideload por ADB Wi-Fi (instalada en el Watch8 y sincronizando
      desde casa).
- [x] Crear el proyecto (Kotlin + Compose for Wear OS).
- [x] Parsear `rutina.json` puesto a mano en `filesDir`.
- [x] Selector de semana y día (navegación por niveles: semanas → días).
- [x] Lista de ejercicios del día.
- [x] Pantalla de ejercicio.
- [x] Navegación entre ejercicios (pasar al siguiente sin volver a la lista).
- [x] Estado vacío cuando nunca sincronizó (ofrece sincronizar desde ahí).
- [x] API: `GET` y `POST /fierros/rutina` con historial y log, desplegada en el
      homelab y con la rutina cargada.
- [x] Sync con botón: GET, `network_security_config.xml`, guardado atómico y
      rutina anterior a `historial/`.
- [x] URL del servidor editable desde Ajustes (la IP del homelab no es fija).
- [ ] Dos semanas de uso real en el gimnasio.

### Iteración 2 — Web (hecha en local, falta desplegarla)

- [x] Maquetas estáticas de toda la web (brief en `planificacion-it-2.md`):
      login, navbar con usuario, rutina actual con lo levantado, detalle de
      ejercicio, actualizar (JSON y formulario), rutinas anteriores y actividad.
- [x] Fase 0: la web servida por Express en `/fierros/`, la API mudada a
      `/api/fierros` y el navbar con las rutas.
- [x] Fase 1: ver la rutina actual por semanas, con la columna "Levanté" (en 0
      hasta la iteración 3) y el panel del ejercicio.
- [x] Fase 2: actualizar la rutina pegando el JSON, con validación y vista previa.
- [x] Fase 3: ver rutinas anteriores (el historial del servidor) y la actividad
      (el log), y volver a usar una rutina anterior.
- [x] Fase 4: cargar una rutina con el formulario, desde cero.
- [x] Editar la rutina actual desde el formulario, precargada.
- [ ] Login y usuarios: solo diseño por ahora.
- [ ] Responsive (celular). Hoy está pensada para la compu.

### Puesta en producción de la iteración 2 — hecha (2026-09-11)

- [x] Commitear los dos repos y pushear.
- [x] Desplegar: en el servidor, `git pull` y `docker compose up -d --build`. El
      `Dockerfile` compila la web (etapa con Node 24); sin `--build` no se
      recompila.
- [x] Instalar en el reloj la versión que pide `/api/fierros/rutina` (ver
      "Instalar y actualizar en el reloj").
- [x] Comprobar en Anteriores → Actividad que la sincronización del reloj figure
      como "El reloj bajó la rutina" (la API lo deduce del User-Agent).
- [x] Cerrar la iteración 2 en
      `planificacion-it-2-implementacion-tecnica.md`.
- [ ] Confirmar que el `.env` del servidor tenga
      `TZ=America/Argentina/Buenos_Aires`. El `docker-compose.yml` no lo define y
      sin eso las horas del log y del historial salen en UTC.

### Iteración 3 — Registrar

- [ ] Registrar el peso levantado, uno por ejercicio.
- [ ] Empezar a cargar el peso real en el JSON (hoy va en "0 kg").
- [ ] Sincronizar los registros a la API.
- [ ] Definir qué pasa al sincronizar la rutina si hay registros sin subir.
- [ ] Ver lo registrado en la web, en formato copiable para volcar a Mynter.
- [ ] Mejorar cómo se ven las series × reps largas en la pantalla de ejercicio:
      hoy "5 × 20-12-10-8-6" se corta en "20-12-10-8 / -6". Ya se probó
      achicar la letra automáticamente para que entre en un renglón, y se
      descartó porque no gustó.

### Posibles mejoras (sin compromiso)

- [ ] Recordar dónde quedé (semana, día y ejercicio) y volver ahí al abrir la
      app, solo por unas horas para no arrancar un día nuevo en el ejercicio del
      anterior. Descartado por ahora; mientras tanto ayuda activar en el reloj la
      opción de mostrar la última app al levantar la muñeca.
- [ ] Validar el puerto de la URL del servidor (un número del 1 al 65535). Hoy
      "192.168.1.57:abc" se acepta y recién falla al sincronizar.
- [ ] Aplicar el tope de 5 semanas también en la API. Hoy es solo del formulario:
      un JSON pegado con más semanas se acepta igual.
- [ ] Un `nodemon.json` que ignore `data/`, `public/` y `fierros-web/`, para que
      la API no se reinicie sola al compilar la web o al guardar una rutina.

## Contexto

- **Reloj:** Samsung Galaxy Watch8 40mm, Wear OS, sin LTE.
- **Servidor:** homelab propio con la API ya corriendo.
- **Estado:** iteración 1 con el desarrollo cerrado (2026-09-11): instalada en el
  reloj y sincronizando con la API. En validación: dos semanas de uso real en el
  gimnasio. Iteración 2 (la web) cerrada el 2026-09-11: desplegada en el homelab,
  con el reloj sincronizando por `/api/fierros/rutina`.