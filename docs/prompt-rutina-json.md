# Conversión de rutinas de Mynter a JSON

Te voy a pasar screenshots de mi rutina de gimnasio, sacados de Mynter (la
plataforma de mi entrenador). Tu trabajo es convertirlos a un JSON con el
formato exacto de abajo, que después carga una app de mi reloj.

## Cómo es la rutina en Mynter

- Un mesociclo son 4 semanas × 4 días.
- Cada día está dividido en bloques ("BLOQUE 1", "BLOQUE 2"...). Un bloque puede
  tener un solo ejercicio o varios, marcados con el cartel "Superserie".
- **Mynter no muestra letras.** Las letras las asignás vos (ver reglas).
- Por lo general, entre semanas solo cambian los pesos; series y reps se
  mantienen.

## Formato de salida

```json
{
  "nombre": "Mesociclo septiembre 2026",
  "semanas": [
    { "numero": 1, "dias": [
      { "numero": 1, "ejercicios": [
        { "letra": "A1", "nombre": "Band pull apart", "series": 3, "reps": "20", "peso": "0 kg", "nota": null },
        { "letra": "A2", "nombre": "Puente de glúteos a una pierna", "series": 3, "reps": "20", "peso": "0 kg", "nota": "por pierna" },
        { "letra": "B", "nombre": "Press militar con barra", "series": 5, "reps": "12-10", "peso": "0 kg", "nota": null }
      ]}
    ]}
  ]
}
```

## Reglas

- **Letra:** una por bloque, en orden de aparición dentro del día (A, B, C...).
  Si el bloque tiene un solo ejercicio, va la letra sola ("B"). Si tiene varios
  (superserie), cada uno lleva la letra del bloque más un número según su orden
  ("A1", "A2"). Esto vale también si el mismo ejercicio aparece repetido en el
  bloque: "D1 Sentadilla Hack 4×10-8" y "D2 Sentadilla Hack 2×20".
- **Nombre:** tal cual aparece, sin traducir ni corregir.
- **Series:** número entero.
- **Reps:** siempre string, respetando rangos ("10-15") y esquemas ("10-8-6").
  Sacale los paréntesis que agrega Mynter: "(12-8)" va como "12-8". También
  valen reps que no son números: "fallo", "20 pasos", "ida y vuelta".
- **Peso:** siempre `"0 kg"`, aunque el screenshot muestre un peso. Por ahora
  no se usa.
- **Nota:** aclaraciones del ejercicio (tempo, pausa, lo que diga Mynter). Si no
  hay nada, `null`. En los ejercicios unilaterales va `"por pierna"` (piernas) o
  `"por lado"` (brazos). En Mynter "10xl" significa 10 por lado: va como
  `"reps": "10"` con `"nota": "por lado"`. No marques un ejercicio como
  unilateral si Mynter no lo dice.
- **Series descendentes:** solo si Mynter lo indica explícitamente. En ese caso,
  `"nota": "serie descendente"`. Un ejercicio repetido dentro de una superserie
  no es un descendente: va con la regla de letras de arriba.
- **Orden:** semanas y días numerados del 1 al 4; los ejercicios en el mismo
  orden que en Mynter.
- **Nombre del mesociclo:** "Mesociclo" + mes y año de la fecha del plan que
  muestra Mynter, salvo que te diga otro.

## Semanas

Si te paso una sola semana, **replicala idéntica en las 4 semanas**. Escribí
cada semana completa, sin referencias ni abreviaturas, y avisalo en el resumen.
Si te paso más de una semana, cada una va con sus propios datos.

## Qué hacer ante dudas

- **No inventes datos.**
- Si algo cambia la estructura (cuántos ejercicios hay, cómo se agrupan, qué
  significa una notación nueva), preguntame antes de generar el JSON.
- Si es una lectura dudosa de un solo valor (un número chico que se lee mal),
  generá el JSON con tu mejor lectura y listala en el resumen para que la
  verifique.

## Formato y decisiones ya tomadas

El formato está cerrado. No propongas cambiarlo. En particular, estas dos cosas
son a propósito:

- Las semanas repetidas.
- Que las letras se asignen por orden.

## Respuesta

1. El JSON completo, válido y sin comentarios. Si podés, como archivo
   descargable `rutina.json`; si no, en un único bloque de código.
2. Debajo, un resumen corto:
   - la cantidad de ejercicios por día;
   - las semanas replicadas, si hubo;
   - las lecturas dudosas para verificar.
