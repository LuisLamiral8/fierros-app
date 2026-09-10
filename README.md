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

### Iteración 2 — Registrar

Anotar el peso levantado en cada ejercicio, una vez por ejercicio y no por cada
serie. Se guarda en el reloj y se sincroniza a la API cuando vuelvo a casa.

**Para qué:** que el registro en Mynter deje de salir de la memoria.

### Iteración 3 — Cargar

Una web mínima en el homelab para cargar y corregir rutinas cómodamente, y para
ver lo registrado en formato copiable a la hora de volcarlo a Mynter.

### Después (sin compromiso)

- Timer de descanso con vibración.
- Un acceso rápido en el reloj para ver el próximo ejercicio sin abrir la app.
- Histórico y progresión.

Nada de esto se decide hasta que las tres primeras iteraciones estén andando y
usadas.

## ToDo

### Iteración 1

- [ ] Resolver el sideload por ADB Wi-Fi, antes de escribir código.
- [x] Crear el proyecto (Kotlin + Compose for Wear OS).
- [x] Parsear `rutina.json` puesto a mano en `filesDir`.
- [x] Selector de semana y día (navegación por niveles: semanas → días).
- [x] Lista de ejercicios del día.
- [x] Pantalla de ejercicio.
- [ ] Navegación entre ejercicios (pasar al siguiente sin volver a la lista).
- [ ] Estado vacío cuando nunca sincronizó.
- [ ] Recordar la pantalla exacta (semana, día y ejercicio) y volver ahí al
      abrir la app.
- [ ] Sync con botón: GET, `network_security_config.xml`, guardado atómico y
      rutina anterior a `historial/`.
- [ ] Dos semanas de uso real en el gimnasio.

### Iteración 2

- [ ] Registrar el peso levantado, uno por ejercicio.
- [ ] Empezar a cargar el peso real en el JSON (hoy va en "0 kg").
- [ ] Sincronizar los registros a la API.
- [ ] Definir qué pasa al sincronizar la rutina si hay registros sin subir.

### Iteración 3

- [ ] Web mínima en el homelab para cargar y corregir rutinas.
- [ ] Vista de lo registrado en formato copiable para volcar a Mynter.

## Contexto

- **Reloj:** Samsung Galaxy Watch8 40mm, Wear OS, sin LTE.
- **Servidor:** homelab propio con la API ya corriendo.
- **Estado:** iteración 1 definida, por arrancar.