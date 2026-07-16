---
title: Sensing & Measurement
---

<script setup>
import SensingDiagram from '../../.vitepress/theme/components/SensingDiagram.vue'
</script>


# Sensing & Measurement

The perception layer is the IoT's "skin and nerve endings" — it turns invisible, intangible physical quantities like
temperature, pressure, and vibration into numbers a machine can read. This chapter explains how a sensor encodes the
physical world into an electrical signal, what the common families and key metrics are, what conditioning a signal needs
before it reaches a processor, and how those physical quantities are ultimately modeled in IoT DC3 as
readable/writable [Points](../introduction/concepts/point).

> You are here: the bottom of the four-layer reference architecture. Next,
> read [Identification & Location](./identification), or return to the [IoT Technology Overview](./).

## What This Layer Is / Why It Exists

Digital systems only process numbers, while the real world is all continuous physical quantities. A bridge spans the
gap: the **sensor**. At heart it is an **energy converter** — it turns some physical quantity (temperature, force, light
intensity, displacement…) into an easily measured electrical quantity (voltage, current, resistance, capacitance,
frequency). Without this step, no amount of computing power can "see" the field.

The perception layer is its own layer because it carries constraints no other layer has: it faces the physical world's
noise, nonlinearity, thermal drift, and aging head-on, and its output is an **analog quantity carrying error** rather
than a clean number. Keeping that error in check and digitizing the analog signal reliably is this layer's entire job.
Upward it delivers one thing only: **a trustworthy numeric reading with a unit and a range**. In DC3, that reading is
the value of a [Point](../introduction/concepts/point).

By conversion principle, sensors fall into a few families; knowing the taxonomy helps you judge a sensor's temperament
during selection:

- **Resistive**: the quantity changes resistance. RTDs (PT100) measure temperature, strain gauges measure force and
  pressure, photoresistors measure light. Good linearity, but they need an excitation current and self-heating adds
  error.
- **Capacitive / Inductive**: the quantity changes capacitance or inductance. Capacitive types measure displacement,
  humidity, liquid level; inductive types (LVDT) measure displacement. Non-contact and long-lived, but sensitive to
  parasitics.
- **Piezoelectric**: force produces charge, so they **measure dynamic quantities only** (vibration, shock, sound). High
  bandwidth, but cannot measure static force.
- **Thermoelectric**: a temperature difference produces an EMF (thermocouple), with an extremely wide range (up to
  thousands of degrees), but it needs cold-junction compensation.
- **Semiconductor / Photoelectric**: PN junctions, Hall elements, photodiodes turn temperature, magnetic field, and
  light into electrical signals — the basis of MEMS and on-chip integration.

## Key Technologies & Trade-offs

From sensing a physical quantity to handing over a number, a sensor runs a fixed pipeline: the **sensing element** first
turns the quantity into a weak electrical signal, **signal conditioning** amplifies, filters, and linearizes it into a
suitable range, and the **A/D converter (ADC)** quantizes the continuous analog voltage into a discrete digital code.
Every hop on this pipeline shapes the quality of the final reading.

<SensingDiagram lang="en" />

**Signal conditioning** is the analog world's preprocessing. A sensing element's output is often just millivolts,
high-impedance, and noisy — not something an ADC can take directly. The conditioning circuit must: amplify (an
instrumentation amp lifts the magnitude), filter (an anti-aliasing low-pass removes high-frequency noise), level-shift (
align to the ADC input range), excite (supply constant current/voltage to resistive sensors), and take the difference
across a bridge to reject common-mode interference. How well conditioning is done often matters more to accuracy than
the ADC's bit count.

**A/D conversion** has two independent dimensions; don't conflate them:

- **Sample rate** sets time resolution. By the Nyquist theorem, the sample rate must exceed twice the signal's highest
  frequency, or **aliasing** occurs — high frequencies masquerade as low ones, unrecoverable after the fact. Vibration
  needs several kHz or more; room temperature is fine at once per minute.
- **Quantization bits** set amplitude resolution. 12 bits slice the range into 4096 steps, 16 bits into 65536. More bits
  resolve finer, but cost more, run slower, and are noise-limited (effective number of bits, ENOB, is usually below the
  nominal count).

When selecting and evaluating a sensor, you look at the following set of metrics — which almost always trade off against
one another:

| Metric      | Meaning                                       | Engineering trade-off                                        |
|-------------|-----------------------------------------------|--------------------------------------------------------------|
| Range       | Upper/lower bounds of what can be measured    | A wider range means coarser resolution at the same bit count |
| Accuracy    | How close a reading is to the true value      | High-accuracy parts cost markedly more                       |
| Resolution  | The smallest change that can be distinguished | Limited jointly by ADC bits and the noise floor              |
| Sample Rate | Samples per unit time                         | Higher consumes more bandwidth, power, storage               |
| Linearity   | How proportionally output tracks input        | Nonlinearity needs lookup-table / polynomial correction      |
| Drift       | Slow shift over time/temperature              | Decides how often recalibration is due                       |

::: warning Accuracy ≠ Resolution
High resolution does not mean correct. A thermometer that displays down to 0.001℃ may, if uncalibrated, carry an
absolute error of 2℃. **Resolution is "how finely you see," accuracy is "how correctly you see"** — evaluate them
separately.
:::

::: tip Calibration is the prerequisite for a trustworthy engineering value
Factory parameters drift over time. Calibration measures the sensor against a known standard, records the deviation, and
builds a correction relationship (zero + slope, and the full curve when needed). In DC3, the most common linear
correction lands directly on the Point's scaling parameters (see below).
:::

**MEMS (Micro-Electro-Mechanical Systems)** is the technology of building the sensing structure and circuitry together
on a silicon die, mass-producing micron-scale moving structures with semiconductor processes. It makes sensors extremely
small, cheap, and power-efficient — today's phone accelerometers, gyroscopes, microphones, and barometers are nearly all
MEMS. The cost is that per-part accuracy and long-term stability usually trail traditional industrial-grade devices, so
the field still runs MEMS and traditional sensors side by side, per scenario.

## Engineering Notes

- **Fix the range before discussing resolution.** Range is a hard constraint set by what you measure; within a fixed
  range you trade ADC bits for resolution — but the noise floor is the real lower bound.
- **Sample rate obeys the signal, not habit.** What quantity you measure and how fast it changes decide the sample rate
  and whether anti-aliasing is needed. Oversampling then decimating is often cheaper than piling on bits.
- **Leave room for linearization and calibration.** Nonlinear sensors (thermocouples, thermistors) require curve
  correction; even linear parts need zero and slope calibration. Pin down the "raw code → engineering value" conversion
  so the field stays maintainable.
- **Drift sets the maintenance cadence.** Translate the datasheet's temperature and time drift straight into "how often
  to recalibrate," and write it into the maintenance plan rather than reacting once readings clearly stray.
- **An actuator is the mirror of a sensor.** If a sensor turns a physical quantity into an electrical signal (input),
  the **actuator** is the reverse energy converter, turning a signal back into physical action (output): a motor turns,
  a valve opens, a relay switches, a heater warms. A complete control loop is the "sense → decide → act → sense again"
  cycle — the perception layer must both read accurately and drive effectively. In DC3, reading maps to read-only Points
  and writing to writable Points, both expressed by the same Point model.

## How It Lands in IoT DC3

"One quantity" in the physical world is abstracted in DC3 as **one Point**. The model has three layers, matching "type —
template — instance":

- A [Profile](../introduction/concepts/profile) is a **capability template for a class of devices**. Create one Profile
  for the "ZS-100 temperature-humidity sensor," define its shared capabilities once, and every same-model device reuses
  it.
- A [Point](../introduction/concepts/point) is **one concrete measurement point** under a Profile. Each collected or
  written physical quantity is one Point, carrying all of that quantity's metadata: data type `pointTypeFlag`,
  read/write capability `rwFlag`, engineering unit `unit`, and scaling parameters `multiple`/`baseValue`/`valueDecimal`.
- A [Device](../introduction/concepts/device) is **one physical thing in the field**, bound to a Profile via `profileId`
  and thereby inheriting all its Points.

So the physical concepts in this chapter land precisely in DC3:

- **Unit and range** → the Point's `unit` field (e.g. `℃`, `kPa`), describing what the quantity is.
- **Read vs. write (sensor vs. actuator)** → the Point's `rwFlag`: a sensor reading uses `READ_ONLY`, a controllable
  point (actuator, setpoint) uses `READ_WRITE` or `WRITE_ONLY`. Whether a Point can be written is decided solely by
  `rwFlag`.
- **Calibration and linear scaling** → the Point converts the **raw code** the driver reads into an **engineering value
  **, with a formula that exactly mirrors the last hop of this chapter's signal chain:

```text
engineering value = raw value × multiple + baseValue   (then rounded by valueDecimal)
```

Example: a temperature transmitter register reads `2531`; with `multiple=0.01`, `baseValue=0`, `unit=℃`,
`valueDecimal=2`, the Point's value after conversion is `25.31 ℃`. This is exactly how a sensor's linear calibration
parameters are baked into the model.

::: info One temperature reading = one Point's value
A field temperature sensor reading 25.3℃ right now is, in DC3, one [PointValue](../introduction/concepts/point) of the
temperature Point on its device. The physical quantity travels sense → conditioning → ADC → scaling, and what lands is
precisely this one number. For exact field semantics, the Point concept page and the source code are authoritative.
:::

This way, the perception layer's engineering details (sensing principle, conditioning, ADC, calibration) collapse into a
few stable Point attributes; upper services need not care about the sensor model and face only "a digital quantity with
a unit, a read/write capability, and already scaled to its engineering value." The physical world's complexity is
absorbed, once, at the Point layer.

## Further Reading

- [Identification & Location](./identification) — the other half of perception: acquiring identity and position
- [IoT Technology Overview](./) — the four-layer reference architecture and where this layer sits
- [Point](../introduction/concepts/point) — where a physical quantity lands in DC3: type, read/write, unit, scaling
- [Profile](../introduction/concepts/profile) — the capability template for a class of devices, aggregating all Points
- [Device](../introduction/concepts/device) — the mirror of a physical thing, binding a Profile to inherit its Points
