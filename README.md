# OpenCharge

Aplicación Android para **optimizar la carga inalámbrica** del móvil. Con un toggle, activas la gestión dinámica: mientras cargas por inducción, la app monitoriza la temperatura de la batería y ajusta la velocidad de carga (rápida/lenta) para evitar sobrecalentamiento, especialmente útil en verano y por la noche.

> **IMPORTANTE:** Solo actúa sobre la carga **inalámbrica**. La carga por **cable no tiene restricciones** y nunca se toca.

Probada pensando en un **Galaxy S25 / One UI 8.5 (Android 16)** con un cargador inalámbrico genérico compatible con carga rápida (15 W).

## Qué hace

- **Toggle 1 — Gestión dinámica de carga:** arma/desarma el control.
- **Toggle 2 — Autoactivar con carga inalámbrica:** al poner el móvil en el cargador inalámbrico, la app se activa sola y gestiona la carga; al quitarlo, se desactiva sola. *Pon el móvil en la mesita de noche y olvídate.*
- Mientras carga por inducción:
  - Si la temperatura sube por encima del umbral (por defecto **40 °C**), baja la carga a **lenta (~5 W)**.
  - Cuando baja a la temperatura segura (por defecto **36 °C**) durante un tiempo de espera (5 min), vuelve a **rápida (~15 W)**.
  - **Modo suave** opcional: alterna períodos rápido/lento para cargar "poco a poco" sin picos de calor.
- Carga por cable: la app solo muestra el estado, no hace nada.
- Notificación persistente con temperatura, porcentaje, corriente y modo.

## Cómo funciona técnicamente

Android no expone una API pública para fijar la velocidad de carga inalámbrica. Samsung la expone como un ajuste de sistema:

```
wireless_fast_charging  (Settings.System / 0 o 1)
```

- `1` → carga inalámbrica rápida (el móvil pide ~15 W al cargador Qi).
- `0` → carga inalámbrica lenta (~5 W).

En Android 13+ escribir ese ajuste requiere el permiso `WRITE_SECURE_SETTINGS`, que se concede **una sola vez por adb (sin root, no toca garantía ni bootloader)**:

```
adb shell pm grant com.tomasmm.opencharge android.permission.WRITE_SECURE_SETTINGS
```

La app detecta carga inalámbrica vía `BatteryManager` (`BATTERY_PLUGGED_WIRELESS`), lee temperatura y corriente, y conmuta el ajuste según histéresis. La pantalla de **Diagnóstico** verifica que la clave existe y que la escritura aplica en tu One UI.

## Instalación

1. Descarga el último APK desde la **GitHub Release** `latest` del repo (o desde los *Artifacts* del último run del workflow).
2. Instálalo en el móvil (permite "instalar apps desconocidas").
3. Activa en el móvil:
   - **Ajustes → Acerca del teléfono → Información de software →** toca *Número de compilación* 7 veces (Developer options).
   - **Developer options → USB debugging** (y "USB debugging inalámbrico" si prefieres).
4. Conecta el móvil al PC con adb y ejecuta:

   ```
   adb shell pm grant com.tomasmm.opencharge android.permission.WRITE_SECURE_SETTINGS
   ```

5. En la app, pulsa **"Desactivar optimización de batería"** para que el sistema no mate el servicio de fondo (crítico en Samsung).

## Uso

- Activa **"Gestión dinámica de carga"** (y si quieres **"Autoactivar con carga inalámbrica"**).
- Pon el móvil en el cargador inalámbrico.
- La notificación y la pantalla principal muestran la temperatura y el modo activo.
- Verifica con `adb shell settings get system wireless_fast_charging` que el valor cambia según la temperatura.

## Ajustes configurables

| Parámetro | Defecto | Qué hace |
|---|---|---|
| Temp. alta | 40 °C | Por encima, baja a carga lenta |
| Temp. baja | 36 °C | Por debajo, vuelve a carga rápida |
| Espera | 5 min | Tiempo a baja temperatura antes de volver a rápido |
| Modo suave | off | Alterna rápido/lento en ciclos |
| Minutos rápido / lento | 20 / 10 | Ciclos del modo suave |

## Notas y advertencias

- Samsung documenta que el toggle de carga rápida no puede cambiarse "mientras se carga"; en algunos firmwares el cambio aplica al siguiente ciclo de carga. La pantalla de **Diagnóstico** permite probarlo en tu dispositivo. Si el cambio no aplica en caliente, actívalo desde el toggle de la app *antes* de poner el móvil en el cargador.
- En One UI 8.5 la clave puede variar; si `wireless_fast_charging` no aparece, la app lo indica en Diagnóstico (la clave se intenta escribir en `Settings.System`, `Settings.Global` y `Settings.Secure`).
- El control es binario (rápido/lento). No existe API para fijar un wattaje intermedio; el "aumento gradual" se aproxima con histéresis y modo suave.
- Detener por completo la carga inalámbrica no es posible sin root; al 100 % la detiene el propio sistema.

## Desarrollo

```
./gradlew assembleDebug        # build local (requiere JDK 17 + Android SDK)
```

El workflow de GitHub (`build-android.yml`) compila un APK en cada push a `main` (o manualmente con *Run workflow*) y publica la **Release `latest`** lista para descargar.
