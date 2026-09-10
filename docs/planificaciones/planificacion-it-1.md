# Iteración 1 — Visualizador


## 1. Objetivo

Ver la rutina del día en el reloj, offline. Solo lectura.

## 2. Alcance

**Dentro:**

- Sincronización inicial: GET a la API del homelab, guarda el JSON en el
  almacenamiento interno de la app.
- Botón de sincronizar para volver a bajar la rutina cuando cambia el
  mesociclo.
- Selector de semana y día.
- Lista de ejercicios del día.
- Pantalla de ejercicio individual: nombre, series, reps, peso.
- Navegación entre ejercicios.
- Estado vacío: si nunca sincronizó, la app lo dice y ofrece sincronizar.

**Fuera:**

- Registro de peso levantado, checkboxes, "serie OK".
- Timer de descanso.
- Room o cualquier base de datos.
- POST de nada a la API.
- Edición desde el reloj.
- App companion, web de carga.
- Sincronización automática.
- Ver el historial de rutinas desde el reloj (se guarda, no se muestra).

## 3. Datos

**La rutina no viaja en el APK.** No hay JSON en `assets/`. La única fuente es
la API: el reloj hace un GET y guarda el archivo en `filesDir`. Si nunca
sincronizó, la app está vacía y lo muestra.

**El endpoint devuelve el mesociclo entero**, las 4 semanas × 4 días. Son unos
pocos KB y la sync pasa una vez cada 4-8 semanas; mandar solo la semana en curso
obligaría a sincronizar todas las semanas.

**Formato: JSON**, parseado con `kotlinx.serialization`. `reps` va como string
porque se usan rangos ("10-15") y esquemas ("10-8-6"). Ojo con las estocadas,
que van por pierna.

**Estructura:** anidada igual que la navegación, semanas → días → ejercicios.

```json
{
  "nombre": "Mesociclo sept-oct",
  "semanas": [
    { "numero": 1, "dias": [
      { "numero": 1, "ejercicios": [
        { "letra": "A", "nombre": "Sentadilla", "series": 4, "reps": "10-8-6", "peso": "0 kg", "nota": null },
        { "letra": "E1", "nombre": "Estocadas", "series": 3, "reps": "12", "peso": "0 kg", "nota": "por pierna" }
      ]}
    ]}
  ]
}
```

- `letra`: Mynter no la muestra; se asigna al convertir, una por bloque en orden
  de aparición (A, B...), con número en las superseries (A1, A2).
- `reps` y `peso` son strings. El peso admite cosas como "peso corporal" o
  "12 kg c/mano".
- **`peso` va en "0 kg" en esta iteración**: el campo existe en el esquema pero
  todavía no se maneja. Se completa de verdad a partir de la iteración 2.
- `nota`: aclaraciones sueltas ("por pierna"), o `null`.
- Los datos se repiten por semana en vez de agrupar "un ejercicio con 4
  valores". Duplica información, pero el JSON lo arma un LLM o se tipea una vez,
  y a cambio la UI queda trivial y aguanta que una semana cambie un ejercicio.

**Cómo llega la rutina a la API en esta iteración:** a mano, con un POST del
JSON armado en la compu. La web de carga es la iteración 3. El JSON se puede
tipear o generarse a partir de los screenshots de Mynter pegados en un chat con
un LLM — es un atajo manual, no una integración, y da lo mismo cuál se use.

**Sincronizar pisa la rutina guardada, sin perder la anterior:**

1. GET a la API.
2. Parsear el JSON. Si falla, no se toca nada y la app muestra el error.
3. Escribir el JSON nuevo a un archivo temporal.
4. Mover la rutina actual a `historial/`, con la fecha en el nombre
   (`rutina-AAAA-MM-DD.json`).
5. Renombrar el temporal a `rutina.json`.

Así un GET cortado o un JSON roto nunca dejan al reloj sin rutina, y los
mesociclos viejos quedan guardados para cuando haya histórico y progresión.

**Durante el desarrollo**, mientras la UI no esté lista, el archivo se puede
poner a mano en `filesDir`:

```
adb push datos/rutina.json /data/local/tmp/
adb shell run-as com.luis.fierros cp /data/local/tmp/rutina.json files/
```

Sirve para separar los dos problemas nuevos: si la pantalla sale vacía, saber
si el que falla es el layout o el cliente HTTP.

## 4. Modelo

Mesociclo de 4 semanas × 4 días. En Mynter cada día se divide en bloques, y un
bloque con varios ejercicios es una superserie. Al convertir, cada bloque recibe
una letra por orden de aparición; los ejercicios de una superserie llevan
además un número (A1, A2), aunque sea el mismo ejercicio repetido.

**Superseries: no se modelan como grupo.** Son ejercicios sueltos en la lista,
con la letra visible. El agrupamiento recién importa cuando haya navegación
automática entre series, que es de la iteración 2 en adelante.

## 5. UI

Restricción dura: 40mm. Una pantalla = un ejercicio.

- Compose for Wear OS con Material 3, como trae la plantilla:
  `TransformingLazyColumn` dentro de `ScreenScaffold`, scroll con bisel táctil.
- Jerarquía por niveles: semanas → días → lista de ejercicios → ejercicio.
  Volver es deslizar hacia la derecha, como en todo Wear OS.
- Pantalla de ejercicio: letra y nombre grandes arriba; series, reps y peso
  debajo; la nota, si hay, al final.

**La semana y el día los elijo yo**, no se calculan desde una fecha de inicio.
Un cálculo automático se rompe apenas falto una semana o el entrenador corre
algo.

El selector de semana no es opcional: series y reps cambian por semana, y los
pesos también cuando se empiecen a manejar. Sin eso, desde la semana 2 la app
muestra datos equivocados.

## 6. Técnico

- **Kotlin + Compose for Wear OS**, plantilla de Android Studio.
- **Ktor Client** con `kotlinx.serialization`. Un solo endpoint.
- **HTTP en claro contra la IP local.** Red doméstica, un cliente, datos que no
  son secretos. Android lo bloquea por defecto: hace falta un
  `network_security_config.xml` habilitándolo solo para esa IP y declararlo en
  el manifest.
- **Sync manual con botón**, no `WorkManager`. Pasa una vez cada 4-8 semanas y
  sé cuándo.
- **Instalación:** sideload por ADB. Habilitar opciones de desarrollador
  (Ajustes → Acerca del reloj → tocar 5 veces el número de versión), activar
  depuración por Wi-Fi, `adb connect`.

## 7. Riesgos

- **Cero experiencia en Android/Kotlin**, y en esta iteración entran dos cosas
  nuevas a la vez: la app y el cliente HTTP. Mitigado poniendo el archivo a
  mano al principio y dejando la red para cuando la UI ya ande.
- **Que la app no se use.** Riesgo principal del proyecto entero. Es
  exactamente lo que esta iteración existe para medir.
- **Sideload.** Conviene resolverlo el primer día, antes de escribir código: si
  el ADB por Wi-Fi no funciona, no hay proyecto.
- **Batería del 40mm** con la pantalla prendida durante una hora. Se mide
  usándola, no se especula antes.

## 8. Decisiones cerradas

- Solo lectura. Sin registro de ningún tipo.
- La rutina llega únicamente por sync. Nada de rutina base en el APK.
- JSON, no formato compacto de texto.
- Estructura semanas → días → ejercicios, con `reps` y `peso` como string.
- `peso` en "0 kg" durante esta iteración.
- El endpoint devuelve el mesociclo entero.
- Semana y día los elijo yo; no se calculan desde una fecha.
- Sincronizar pisa la rutina actual; la anterior pasa a `historial/`.
- Superseries como ejercicios sueltos.
- Selector de semana dentro del alcance, como navegación por niveles
  (semanas → días), no como pantalla de inicio combinada.
- Sync manual con botón.
- API en Node, la que ya está corriendo en el homelab.
- Ingeniería inversa de la API de Mynter: descartada.
