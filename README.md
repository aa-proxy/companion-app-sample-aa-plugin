# AA Proxy Companion Sample AA Plugin

Sample Android plugin for **AA Proxy Companion**.

The plugin is a separate APK that exposes an AIDL service, receives host-provided
vehicle/system/event data as JSON, renders a **Remote Compose** document, and
returns the rendered document bytes to the host.

The host app is responsible for:

- Android Auto surface rendering
- plugin discovery and binding
- vehicle data collection
- WebSocket/topic subscriptions
- host-owned navigation state
- executing plugin actions
- playing the returned Remote Compose document on the car surface

The plugin is responsible for:

- declaring its manifest
- declaring which data keys it wants
- rendering the current screen
- emitting host actions through Remote Compose action metadata

---

## What this sample contains

- An exported plugin service: `AAProxyPluginService`
- A shared AIDL contract: `IAAProxyPlugin`
- A plugin contract model set:
  - `PluginManifest`
  - `NavigationState`
  - `PluginAction`
  - `PluginRenderRequest`
  - vehicle/system/event data models
- A sample Remote Compose renderer
- A simple Android activity so the plugin APK can be installed and opened normally

---

## How the plugin is discovered

The host discovers plugins by querying services with this action:

```text
com.github.deadknight.aaproxycompanion.PLUGIN_AA
```

The plugin declares the service in `AndroidManifest.xml`:

```xml
<service
    android:name=".plugin.AAProxyPluginService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.github.deadknight.aaproxycompanion.PLUGIN_AA" />
    </intent-filter>
</service>
```

The service must be exported because it is bound by the host app from another package.

---

## AIDL contract

The plugin service implements `IAAProxyPlugin`:

```aidl
package com.github.deadknight.aaproxycompanion.plugin.aidl;

interface IAAProxyPlugin {
    int getApiVersion();
    String getManifestJson();
    byte[] render(String renderRequestJson);
    byte[] onAction(String actionJson, String renderRequestJson);
}
```

### Method responsibilities

| Method | Direction | Purpose |
|---|---|---|
| `getApiVersion()` | Host → Plugin | Returns plugin API version. Current sample uses `1`. |
| `getManifestJson()` | Host → Plugin | Returns the plugin manifest as JSON. |
| `render(renderRequestJson)` | Host → Plugin | Renders the current screen and returns Remote Compose document bytes. |
| `onAction(actionJson, renderRequestJson)` | Host → Plugin | Optional hook if plugin-owned state is needed after an action. For normal navigation, the host owns the stack and calls `render(...)` again. |

---

## Plugin manifest

`getManifestJson()` returns a `PluginManifest` JSON payload. The manifest tells
the host what the plugin supports.

Example:

```json
{
  "pluginId": "retro.cluster",
  "version": 1,
  "displayName": "Retro Cluster",
  "initialScreen": "home",
  "screens": [
    "home",
    "tpms_detail",
    "trip_stats",
    "battery",
    "settings"
  ],
  "requestedPluginDataKeys": [
    "VEHICLE_SPEED",
    "VEHICLE_ODOMETER",
    "VEHICLE_TPMS",
    "VEHICLE_TRIP",
    "VEHICLE_BATTERY",
    "VEHICLE_DRIVE_STATE",
    "SYSTEM_THEME"
  ],
  "customDataKeys": [
    "script.rest.result",
    "custom.demo.topic"
  ],
  "supportedActions": [
    "PUSH",
    "REPLACE",
    "POP",
    "SUBSCRIBE_WS_TOPIC",
    "UNSUBSCRIBE_WS_TOPIC",
    "SCRIPT_EVENT",
    "CONSUME_DATA"
  ]
}
```

### Important manifest fields

| Field | Meaning |
|---|---|
| `pluginId` | Stable plugin identifier. |
| `version` | Plugin manifest/API version. |
| `displayName` | Human-readable plugin name. |
| `initialScreen` | First screen the host should open. |
| `screens` | Screen IDs owned by this plugin. |
| `requestedPluginDataKeys` | Built-in host data keys the plugin wants. |
| `customDataKeys` | Extra WebSocket/topic data keys the plugin wants the host to subscribe to. |
| `supportedActions` | Actions the plugin may emit to the host. |

---

## Built-in data keys

The host passes plugin data as a map:

```kotlin
Map<String, String>
```

Each key is usually one of `PluginDataKeys`.

Supported built-in keys:

```text
VEHICLE_SPEED
VEHICLE_ODOMETER
VEHICLE_TPMS
VEHICLE_TRIP
VEHICLE_BATTERY
VEHICLE_DRIVE_STATE

EVENT_TOPIC_DATA
EVENT_SUBSCRIBE_TOPIC_RESULT
EVENT_UNSUBSCRIBE_TOPIC_RESULT

SYSTEM_THEME

HOST_NAVIGATION_STATE
```

Some models exist in the contract but may only be populated when the host supports
that data source. Always treat fields as optional.

---

## Render request

The host calls `render(...)` with a `PluginRenderRequest` encoded as JSON.

```kotlin
data class PluginRenderRequest(
    val pluginId: String,
    val navigationState: NavigationState,
    val data: Map<String, String>,
    val config: Map<String, String> = emptyMap(),
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int
)
```

Example request:

```json
{
  "pluginId": "retro.cluster",
  "navigationState": {
    "pluginId": "retro.cluster",
    "stack": [
      {
        "screen": "home",
        "paramsJson": null
      }
    ]
  },
  "data": {
    "VEHICLE_SPEED": "{\"speedKph\":87.0}",
    "VEHICLE_ODOMETER": "{\"totalKm\":124532.4,\"tripKm\":18.7}",
    "SYSTEM_THEME": "{\"version\":1,\"timestamp\":1710000000000,\"dataJson\":\"{\\\"theme\\\":\\\"dark\\\"}\"}"
  },
  "config": {},
  "widthPx": 1280,
  "heightPx": 720,
  "densityDpi": 230
}
```

---

## Density and sizing

The host sends:

```text
widthPx
heightPx
densityDpi
```

The plugin must use these values during Remote Compose capture.

Example:

```kotlin
captureSingleRemoteDocument(
    creationDisplayInfo = RemoteCreationDisplayInfo(
        request.widthPx,
        request.heightPx,
        request.densityDpi
    ),
    context = context
) {
    // Remote Compose document
}
```

Remote-only scaling is controlled at document creation time through
`RemoteCreationDisplayInfo.densityDpi`.

Do not rely only on playback-time scaling.

---

## Data format

Host data values are JSON strings.

For some keys the host may pass the raw payload directly. For other system/event
values the host may pass a `JsonEnvelope`.

```kotlin
data class JsonEnvelope(
    val version: Int,
    val timestamp: Long,
    val dataJson: String
)
```

Envelope example:

```json
{
  "version": 1,
  "timestamp": 1710000000000,
  "dataJson": "{\"theme\":\"dark\"}"
}
```

A safe plugin should support both patterns where useful:

```kotlin
inline fun <reified T> readDirectOrEnvelope(json: String?): T? {
    if (json.isNullOrBlank()) return null

    PluginJson.decode<T>(json)?.let { return it }

    val envelope = PluginJson.decode<JsonEnvelope>(json) ?: return null
    return PluginJson.decode<T>(envelope.dataJson)
}
```

---

## Vehicle data examples

### Vehicle speed

Model:

```kotlin
data class VehicleSpeedData(
    val speedKph: Float? = null
)
```

JSON:

```json
{
  "speedKph": 87.0
}
```

Usage:

```kotlin
val speed = readDirectOrEnvelope<VehicleSpeedData>(
    request.data[PluginDataKeys.VEHICLE_SPEED.value]
)

val speedText = speed?.speedKph?.toInt()?.toString() ?: "--"
```

---

### Vehicle odometer

Model:

```kotlin
data class VehicleOdometerData(
    val totalKm: Float? = null,
    val tripKm: Float? = null
)
```

JSON:

```json
{
  "totalKm": 124532.4,
  "tripKm": 18.7
}
```

Usage:

```kotlin
val odometer = readDirectOrEnvelope<VehicleOdometerData>(
    request.data[PluginDataKeys.VEHICLE_ODOMETER.value]
)

val totalKm = odometer?.totalKm
val tripKm = odometer?.tripKm
```

---

### TPMS

Models:

```kotlin
data class TpmsWheelData(
    val pressureKpa: Float? = null,
    val tempC: Float? = null
)

data class VehicleTpmsData(
    val fl: TpmsWheelData? = null,
    val fr: TpmsWheelData? = null,
    val rl: TpmsWheelData? = null,
    val rr: TpmsWheelData? = null
)
```

JSON:

```json
{
  "fl": {
    "pressureKpa": 235.0,
    "tempC": 32.0
  },
  "fr": {
    "pressureKpa": 233.0,
    "tempC": 31.0
  },
  "rl": {
    "pressureKpa": 228.0,
    "tempC": 30.0
  },
  "rr": {
    "pressureKpa": 229.0,
    "tempC": 30.0
  }
}
```

Usage:

```kotlin
val tpms = readDirectOrEnvelope<VehicleTpmsData>(
    request.data[PluginDataKeys.VEHICLE_TPMS.value]
)

val frontLeftKpa = tpms?.fl?.pressureKpa
```

---

### Trip

Model:

```kotlin
data class VehicleTripData(
    val tripKm: Float? = null,
    val tripDurationSec: Long? = null,
    val averageSpeedKph: Float? = null
)
```

JSON:

```json
{
  "tripKm": 42.5,
  "tripDurationSec": 3600,
  "averageSpeedKph": 42.5
}
```

Usage:

```kotlin
val trip = readDirectOrEnvelope<VehicleTripData>(
    request.data[PluginDataKeys.VEHICLE_TRIP.value]
)

val tripKm = trip?.tripKm
val avgSpeed = trip?.averageSpeedKph
```

---

### Battery

Model:

```kotlin
data class VehicleBatteryData(
    val batteryPercent: Float? = null,
    val batteryKwh: Float? = null,
    val charging: Boolean? = null
)
```

JSON:

```json
{
  "batteryPercent": 78.5,
  "batteryKwh": 61.2,
  "charging": false
}
```

Usage:

```kotlin
val battery = readDirectOrEnvelope<VehicleBatteryData>(
    request.data[PluginDataKeys.VEHICLE_BATTERY.value]
)

val percent = battery?.batteryPercent
val charging = battery?.charging
```

---

### Drive state

Model:

```kotlin
data class VehicleDriveStateData(
    val gear: String? = null,
    val ignitionOn: Boolean? = null,
    val moving: Boolean? = null
)
```

JSON:

```json
{
  "gear": "D",
  "ignitionOn": true,
  "moving": true
}
```

Usage:

```kotlin
val driveState = readDirectOrEnvelope<VehicleDriveStateData>(
    request.data[PluginDataKeys.VEHICLE_DRIVE_STATE.value]
)

val gear = driveState?.gear ?: "--"
```

---

### System theme

Model:

```kotlin
data class SystemThemeData(
    val theme: String
)
```

JSON:

```json
{
  "theme": "dark"
}
```

Usage:

```kotlin
val theme = readDirectOrEnvelope<SystemThemeData>(
    request.data[PluginDataKeys.SYSTEM_THEME.value]
)

val isDark = theme?.theme == PluginSystemTheme.DARK.value
```

---

## Navigation model

The plugin defines screen names, but the host owns the navigation stack.

```kotlin
data class NavigationState(
    val pluginId: String,
    val stack: List<NavigationEntry>
)

data class NavigationEntry(
    val screen: String,
    val paramsJson: String? = null
)
```

Example:

```json
{
  "pluginId": "retro.cluster",
  "stack": [
    {
      "screen": "home",
      "paramsJson": null
    },
    {
      "screen": "tpms_detail",
      "paramsJson": "{\"wheel\":\"fl\"}"
    }
  ]
}
```

The current screen is the last entry:

```kotlin
val currentEntry = request.navigationState.stack.lastOrNull()
val currentScreen = currentEntry?.screen ?: "home"
val paramsJson = currentEntry?.paramsJson
```

---

## Actions

The plugin emits actions to the host through Remote Compose action metadata.

Current host action id:

```kotlin
PluginContract.HostActionIds.PLUGIN_ACTION // 1000
```

Action model:

```kotlin
data class PluginAction(
    val action: PluginActions,
    val screen: String? = null,
    val paramsJson: String? = null,
    val name: String? = null,
    val payloadJson: String? = null
)
```

Supported actions:

```text
PUSH
REPLACE
POP
SUBSCRIBE_WS_TOPIC
UNSUBSCRIBE_WS_TOPIC
SCRIPT_EVENT
CONSUME_DATA
```

---

## Action: PUSH

Pushes a plugin-defined screen onto the host-owned stack.

```kotlin
PluginAction(
    action = PluginActions.PUSH,
    screen = "tpms_detail",
    paramsJson = """{"wheel":"fl"}"""
)
```

Remote Compose example:

```kotlin
RemoteText(
    "OPEN TPMS",
    modifier = RemoteModifier
        .padding(top = 24.rdp, bottom = 24.rdp)
        .clickable(
            HostAction(
                PluginContract.HostActionIds.PLUGIN_ACTION,
                RemoteString("plugin_action"),
                RemoteString(
                    PluginJson.encode(
                        PluginAction(
                            action = PluginActions.PUSH,
                            screen = "tpms_detail",
                            paramsJson = """{"wheel":"fl"}"""
                        )
                    )
                )
            )
        )
)
```

---

## Action: REPLACE

Replaces the current navigation stack entry.

```kotlin
PluginAction(
    action = PluginActions.REPLACE,
    screen = "battery",
    paramsJson = null
)
```

---

## Action: POP

Pops the current navigation stack entry if possible.

```kotlin
PluginAction(
    action = PluginActions.POP
)
```

This is also used when the car surface requests back navigation.

---

## Action: SUBSCRIBE_WS_TOPIC

Subscribes the host to a WebSocket/topic source.

The topic name is passed in `name`.

```kotlin
PluginAction(
    action = PluginActions.SUBSCRIBE_WS_TOPIC,
    name = "script.rest.result"
)
```

The host replies by updating plugin data key:

```text
EVENT_SUBSCRIBE_TOPIC_RESULT
```

Payload model:

```kotlin
data class EventSubscribeTopicResultdata(
    val uuid: String,
    val topic: String
)
```

Example result JSON:

```json
{
  "uuid": "9c8e9c85-4d8b-4e32-a4d8-b60d73502f9f",
  "topic": "script.rest.result"
}
```

Save the returned `uuid`. It is needed for unsubscribe.

---

## Action: UNSUBSCRIBE_WS_TOPIC

Unsubscribes a topic previously subscribed with `SUBSCRIBE_WS_TOPIC`.

The subscription UUID is passed in `name`.

```kotlin
PluginAction(
    action = PluginActions.UNSUBSCRIBE_WS_TOPIC,
    name = "9c8e9c85-4d8b-4e32-a4d8-b60d73502f9f"
)
```

The host replies by updating plugin data key:

```text
EVENT_UNSUBSCRIBE_TOPIC_RESULT
```

Payload model:

```kotlin
data class EventUnsubscribeTopicResultdata(
    val uuid: String,
    val topic: String
)
```

---

## Action: SCRIPT_EVENT

Sends a script event to the aa-proxy WebSocket scripting layer.

The event topic is passed in `name`.

The event payload is passed in `payloadJson`.

```kotlin
PluginAction(
    action = PluginActions.SCRIPT_EVENT,
    name = "script.battery",
    payloadJson = """{"batteryPercent":80.0,"charging":false}"""
)
```

This is useful for integrating the plugin UI with aa-proxy scripting hooks.

---

## Action: CONSUME_DATA

Removes a plugin data entry from the host-side plugin data map.

The key to consume is passed in `name`.

```kotlin
PluginAction(
    action = PluginActions.CONSUME_DATA,
    name = PluginDataKeys.EVENT_TOPIC_DATA.value
)
```

This is useful for one-shot event payloads, especially:

```text
EVENT_TOPIC_DATA
EVENT_SUBSCRIBE_TOPIC_RESULT
EVENT_UNSUBSCRIBE_TOPIC_RESULT
```

---

## Event topic data

When a subscribed topic receives data, the host updates:

```text
EVENT_TOPIC_DATA
```

Payload model:

```kotlin
data class EventTopicData(
    val uuid: String,
    val topic: String,
    val payload: String
)
```

Example JSON:

```json
{
  "uuid": "9c8e9c85-4d8b-4e32-a4d8-b60d73502f9f",
  "topic": "script.rest.result",
  "payload": "{\"requestId\":\"abc\",\"result\":\"...\"}"
}
```

Usage:

```kotlin
val event = readDirectOrEnvelope<EventTopicData>(
    request.data[PluginDataKeys.EVENT_TOPIC_DATA.value]
)

if (event?.topic == "script.rest.result") {
    val resultPayload = event.payload

    // After handling it, optionally consume it:
    // PluginAction(action = PluginActions.CONSUME_DATA, name = PluginDataKeys.EVENT_TOPIC_DATA.value)
}
```

---

## Renderer flow

`AAPluginRenderer.render(...)` receives a `PluginRenderRequest`, reads host-provided
data, checks the current screen from `request.navigationState`, captures a Remote
Compose document, and returns the captured bytes.

Simplified flow:

```kotlin
suspend fun render(request: PluginRenderRequest): ByteArray {
    val speed = readDirectOrEnvelope<VehicleSpeedData>(
        request.data[PluginDataKeys.VEHICLE_SPEED.value]
    )

    val battery = readDirectOrEnvelope<VehicleBatteryData>(
        request.data[PluginDataKeys.VEHICLE_BATTERY.value]
    )

    val currentScreen = request.navigationState.stack.lastOrNull()?.screen
        ?: "home"

    val captured = captureSingleRemoteDocument(
        creationDisplayInfo = RemoteCreationDisplayInfo(
            request.widthPx,
            request.heightPx,
            request.densityDpi
        ),
        context = context
    ) {
        RemoteBox(
            modifier = RemoteModifier
                .fillMaxSize()
                .background(Color(0xFF1E293B))
        ) {
            RemoteColumn(
                modifier = RemoteModifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.rdp, vertical = 24.rdp)
            ) {
                RemoteText("PLUGIN: ${request.pluginId}")
                RemoteText("SCREEN: $currentScreen")
                RemoteText("Speed: ${speed?.speedKph?.toInt() ?: "--"} km/h")
                RemoteText("Battery: ${battery?.batteryPercent?.toInt() ?: "--"}%")
            }
        }
    }

    return captured.bytes
}
```

---

## Full example: home screen with actions

```kotlin
private fun renderHome(
    request: PluginRenderRequest,
    speed: VehicleSpeedData?,
    odometer: VehicleOdometerData?,
    battery: VehicleBatteryData?,
    tpms: VehicleTpmsData?
) {
    RemoteColumn(
        modifier = RemoteModifier
            .fillMaxSize()
            .background(Color(0xFF081C34))
            .padding(horizontal = 24.rdp, vertical = 24.rdp)
    ) {
        RemoteText("AA Proxy Plugin")

        RemoteText("Speed: ${speed?.speedKph?.toInt() ?: "--"} km/h")
        RemoteText("Total: ${odometer?.totalKm?.toInt() ?: "--"} km")
        RemoteText("Trip: ${odometer?.tripKm ?: "--"} km")
        RemoteText("Battery: ${battery?.batteryPercent?.toInt() ?: "--"}%")
        RemoteText("FL tyre: ${tpms?.fl?.pressureKpa?.toInt() ?: "--"} kPa")

        RemoteText(
            "Open TPMS detail",
            modifier = RemoteModifier
                .padding(top = 24.rdp, bottom = 12.rdp)
                .clickable(
                    HostAction(
                        PluginContract.HostActionIds.PLUGIN_ACTION,
                        RemoteString("plugin_action"),
                        RemoteString(
                            PluginJson.encode(
                                PluginAction(
                                    action = PluginActions.PUSH,
                                    screen = "tpms_detail",
                                    paramsJson = """{"wheel":"fl"}"""
                                )
                            )
                        )
                    )
                )
        )

        RemoteText(
            "Send script event",
            modifier = RemoteModifier
                .padding(top = 12.rdp, bottom = 12.rdp)
                .clickable(
                    HostAction(
                        PluginContract.HostActionIds.PLUGIN_ACTION,
                        RemoteString("plugin_action"),
                        RemoteString(
                            PluginJson.encode(
                                PluginAction(
                                    action = PluginActions.SCRIPT_EVENT,
                                    name = "script.ping",
                                    payloadJson = """{"from":"plugin"}"""
                                )
                            )
                        )
                    )
                )
        )
    }
}
```

---

## Full example: subscribe to script result topic

This action asks the host to subscribe to a topic.

```kotlin
val subscribeAction = PluginAction(
    action = PluginActions.SUBSCRIBE_WS_TOPIC,
    name = "script.rest.result"
)
```

Remote Compose clickable:

```kotlin
RemoteText(
    "Subscribe script results",
    modifier = RemoteModifier
        .padding(top = 12.rdp, bottom = 12.rdp)
        .clickable(
            HostAction(
                PluginContract.HostActionIds.PLUGIN_ACTION,
                RemoteString("plugin_action"),
                RemoteString(PluginJson.encode(subscribeAction))
            )
        )
)
```

The host will later update:

```text
EVENT_SUBSCRIBE_TOPIC_RESULT
```

with:

```json
{
  "uuid": "...",
  "topic": "script.rest.result"
}
```

---

## Full example: trigger async script REST call

If aa-proxy scripting exposes a script event such as `script.battery`, the plugin
can trigger it like this:

```kotlin
val action = PluginAction(
    action = PluginActions.SCRIPT_EVENT,
    name = "script.battery",
    payloadJson = """{"batteryPercent":80.0,"charging":false}"""
)
```

Remote Compose clickable:

```kotlin
RemoteText(
    "Send battery event",
    modifier = RemoteModifier
        .padding(top = 12.rdp, bottom = 12.rdp)
        .clickable(
            HostAction(
                PluginContract.HostActionIds.PLUGIN_ACTION,
                RemoteString("plugin_action"),
                RemoteString(PluginJson.encode(action))
            )
        )
)
```

If the script publishes a result to a subscribed topic, the host will update:

```text
EVENT_TOPIC_DATA
```

The plugin can read it on the next render.

---

## Full example: consume one-shot event data

After handling a one-shot event, emit:

```kotlin
PluginAction(
    action = PluginActions.CONSUME_DATA,
    name = PluginDataKeys.EVENT_TOPIC_DATA.value
)
```

Remote Compose clickable example:

```kotlin
RemoteText(
    "Clear event",
    modifier = RemoteModifier
        .padding(top = 12.rdp, bottom = 12.rdp)
        .clickable(
            HostAction(
                PluginContract.HostActionIds.PLUGIN_ACTION,
                RemoteString("plugin_action"),
                RemoteString(
                    PluginJson.encode(
                        PluginAction(
                            action = PluginActions.CONSUME_DATA,
                            name = PluginDataKeys.EVENT_TOPIC_DATA.value
                        )
                    )
                )
            )
        )
)
```

---

## Plugin service lifecycle

Do not initialize the renderer with `applicationContext` in a property initializer.
Initialize it in `onCreate()`.

```kotlin
class AAProxyPluginService : Service() {
    private lateinit var renderer: AAPluginRenderer

    override fun onCreate() {
        super.onCreate()
        renderer = AAPluginRenderer(applicationContext)
    }
}
```

Using `applicationContext` too early can crash because the `Service` context may
not be attached yet.

---

## Build notes

The app module must enable AIDL and Compose.

```kotlin
android {
    buildFeatures {
        aidl = true
        compose = true
    }
}
```

This sample uses AndroidX Remote Compose alpha artifacts and Compose.

---

## ProGuard / R8

For release builds, keep the plugin service, contract models, AIDL stubs, Moshi
adapters, and Remote Compose classes.

```proguard
-keep class com.github.deadknight.aaproxyrsplugin.plugin.** { *; }
-keep class com.github.deadknight.aaproxycompanion.plugin.aidl.** { *; }
-keep interface com.github.deadknight.aaproxycompanion.plugin.aidl.** { *; }
-keep class androidx.compose.remote.** { *; }
-keep interface androidx.compose.remote.** { *; }
-keep class **JsonAdapter { *; }
-keep class **JsonAdapter_* { *; }
-keep class **_JsonAdapter { *; }
```

If your plugin package name differs, adjust the first keep rule.

---

## Development checklist

1. Install the plugin APK.
2. Make sure the plugin service is exported.
3. Make sure the service has the `PLUGIN_AA` intent filter.
4. Make sure the host app has a `<queries>` entry for `com.github.deadknight.aaproxycompanion.PLUGIN_AA`.
5. Select the plugin package in the host app.
6. Host binds to `IAAProxyPlugin`.
7. Host calls `getManifestJson()`.
8. Host builds `NavigationState`.
9. Host sends `PluginRenderRequest` to `render(...)`.
10. Plugin returns Remote Compose document bytes.
11. Host displays the bytes in `RemoteComposePlayer`.
12. Plugin emits `HostAction(1000, ...)` for navigation/actions.
13. Host updates navigation/data and calls `render(...)` again.

---

## Troubleshooting

### Plugin is not discovered

Check the plugin service declaration:

```xml
<service
    android:name=".plugin.AAProxyPluginService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.github.deadknight.aaproxycompanion.PLUGIN_AA" />
    </intent-filter>
</service>
```

Also check the host app `<queries>` block.

---

### `render(...)` is not called

Check that:

- the plugin package is selected in the host app
- the host successfully binds to the service
- `getApiVersion()` returns the expected version
- `getManifestJson()` returns valid JSON
- `pluginId` in the manifest matches render expectations

---

### White screen

Log the returned byte array size.

Empty or tiny byte arrays usually mean plugin-side parsing or rendering failed.

Add logs around:

```kotlin
val request = PluginJson.decode<PluginRenderRequest>(renderRequestJson)
```

and around the Remote Compose capture result.

---

### UI scale is wrong

Use `request.densityDpi` in `RemoteCreationDisplayInfo`.

```kotlin
RemoteCreationDisplayInfo(
    request.widthPx,
    request.heightPx,
    request.densityDpi
)
```

Do not try to fix Remote Compose scale only at playback time.

---

### Clicks do not work

Make sure action metadata is attached to Remote Compose elements using:

```kotlin
HostAction(
    PluginContract.HostActionIds.PLUGIN_ACTION,
    RemoteString("plugin_action"),
    RemoteString(PluginJson.encode(pluginAction))
)
```

The host only handles action id:

```kotlin
PluginContract.HostActionIds.PLUGIN_ACTION // 1000
```

---

### Navigation action does nothing

Check that:

- `supportedActions` includes the action
- `screen` is not null for `PUSH` and `REPLACE`
- the screen name exists in `screens`
- metadata JSON decodes as `PluginAction`
- the `HostAction` id is `1000`

---

### Topic subscription does not produce data

Check that:

- manifest includes `SUBSCRIBE_WS_TOPIC`
- the plugin emitted `PluginAction(SUBSCRIBE_WS_TOPIC, name = "...")`
- the host returned `EVENT_SUBSCRIBE_TOPIC_RESULT`
- the plugin reads `EVENT_TOPIC_DATA`
- the subscribed topic actually receives events

---

### Event data repeats

Event data remains in the host data map until replaced or consumed.

After handling a one-shot event, emit:

```kotlin
PluginAction(
    action = PluginActions.CONSUME_DATA,
    name = PluginDataKeys.EVENT_TOPIC_DATA.value
)
```

---

## Notes

- The host owns Android Auto rendering and navigation state.
- The plugin should be deterministic: render output should depend on `PluginRenderRequest`.
- Avoid blocking work inside `render(...)`.
- Avoid network calls inside `render(...)`; use host actions and host-provided data instead.
- Treat all vehicle data fields as optional.
- Treat unknown data keys as forward-compatible.
- Prefer `paramsJson` for screen-specific parameters.
- Prefer `EVENT_TOPIC_DATA` for asynchronous event results.