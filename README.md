# AA Proxy Companion Sample Plugin

Sample Android plugin for AA Proxy Companion. The plugin is a separate APK that exposes an AIDL service, receives vehicle/system data from the host as JSON, renders a Remote Compose document, and returns the rendered document bytes to the host.

The host app is responsible for Android Auto surface rendering, plugin discovery/binding, vehicle data collection, navigation state, and executing plugin actions. The plugin is responsible for describing its own screens and producing Remote Compose document bytes.

## What this sample contains

- An exported plugin service: `AAProxyPluginService`
- A shared AIDL contract: `IAAProxyPlugin`
- A plugin contract model set: manifest, navigation state, actions, render request, and vehicle data payloads
- A sample Remote Compose renderer: `AAPluginRenderer`
- A simple Android activity only so the app can be installed and opened normally

## How the plugin is discovered

The host discovers plugins by querying services with this action:

```xml
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
| `onAction(actionJson, renderRequestJson)` | Host → Plugin | Optional action hook. The host may call this after plugin actions if plugin-owned state is needed. |

For the basic navigation flow, the host can update `NavigationState` itself and call `render(...)` again.

## Plugin manifest

`getManifestJson()` returns a `PluginManifest` JSON payload. The manifest tells the host what the plugin supports.

Example:

```json
{
  "pluginId": "retro.cluster",
  "version": 1,
  "displayName": "Retro Cluster",
  "initialScreen": "home",
  "screens": ["home", "tpms_detail", "trip_stats"],
  "requestedPluginDataKeys": [
    "VEHICLE_SPEED",
    "VEHICLE_ODOMETER",
    "VEHICLE_TPMS"
  ],
  "customDataKeys": [],
  "supportedActions": ["PUSH", "REPLACE", "POP", "EMIT_EVENT"]
}
```

Important fields:

- `pluginId`: stable plugin identifier.
- `initialScreen`: first screen the host should open.
- `screens`: screen IDs owned by this plugin.
- `requestedPluginDataKeys`: data keys the plugin wants from the host.
- `supportedActions`: navigation/event actions the plugin may emit.

## Render request

The host calls `render(...)` with a `PluginRenderRequest` encoded as JSON.

Conceptually:

```json
{
  "pluginId": "retro.cluster",
  "navigationState": {
    "pluginId": "retro.cluster",
    "stack": [
      { "screen": "home", "paramsJson": null }
    ]
  },
  "data": {
    "VEHICLE_SPEED": "{...JsonEnvelope...}",
    "VEHICLE_ODOMETER": "{...JsonEnvelope...}",
    "VEHICLE_TPMS": "{...JsonEnvelope...}"
  },
  "config": {},
  "widthPx": 1280,
  "heightPx": 720,
  "densityDpi": 230
}
```

### Density and sizing

The host sends `widthPx`, `heightPx`, and `densityDpi`. The plugin must use these values during Remote Compose capture:

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

This is important: Remote-only scaling is controlled at document creation time through `RemoteCreationDisplayInfo.densityDpi`.

## Data format

The host passes data as a map of `PluginDataKeys` to JSON strings.

Each value is a `JsonEnvelope`:

```json
{
  "version": 1,
  "timestamp": 1710000000,
  "dataJson": "{...actual payload json...}"
}
```

### Vehicle speed

```json
{
  "version": 1,
  "timestamp": 1710000000,
  "dataJson": "{\"speedKph\":87.0}"
}
```

### Odometer

```json
{
  "version": 1,
  "timestamp": 1710000000,
  "dataJson": "{\"totalKm\":124532.4,\"tripKm\":18.7}"
}
```

### TPMS

```json
{
  "version": 1,
  "timestamp": 1710000000,
  "dataJson": "{\"fl\":{\"pressureKpa\":235.0,\"tempC\":32.0},\"fr\":{\"pressureKpa\":233.0,\"tempC\":31.0},\"rl\":{\"pressureKpa\":228.0,\"tempC\":30.0},\"rr\":{\"pressureKpa\":229.0,\"tempC\":30.0}}"
}
```

## Navigation model

The plugin defines screens. The host owns the current navigation stack.

Example `NavigationState`:

```json
{
  "pluginId": "retro.cluster",
  "stack": [
    { "screen": "home", "paramsJson": null },
    { "screen": "tpms_detail", "paramsJson": "{}" }
  ]
}
```

The current screen is the last item in `stack`.

The plugin navigates by emitting a `HostAction` with metadata containing a `PluginAction` JSON payload.

## Actions

The plugin can emit actions to the host through Remote Compose `HostAction`.

Current host action id:

```kotlin
PluginContract.HostActionIds.PLUGIN_ACTION // 1000
```

Example: push TPMS detail screen.

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
                    SerializerMoshi.moshiKotlin.toJson(
                        PluginAction::class.java,
                        PluginAction(
                            action = PluginActions.PUSH,
                            screen = "tpms_detail",
                            paramsJson = "{}"
                        )
                    )
                )
            )
        )
)
```

Supported actions:

| Action | Meaning |
|---|---|
| `PUSH` | Push a plugin-defined screen onto the host-owned stack. |
| `REPLACE` | Replace the current stack entry. |
| `POP` | Pop the current stack entry if possible. |
| `EMIT_EVENT` | Send a custom event to the host. |

## Renderer flow

`AAPluginRenderer.render(...)` receives a `PluginRenderRequest`, reads host-provided data, checks the current screen from `request.navigationState`, captures a Remote Compose document, and returns `captured.bytes`.

Simplified flow:

```kotlin
suspend fun render(request: PluginRenderRequest, action: PluginAction? = null): ByteArray {
    val speed = readEnvelopeData<VehicleSpeedData>(request.data[PluginDataKeys.VEHICLE_SPEED.value])
    val currentScreen = request.navigationState.stack.lastOrNull()?.screen ?: "home"

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
                RemoteText("Speed: ${speed?.speedKph?.roundToInt() ?: "--"}")
            }
        }
    }

    return captured.bytes
}
```

## Service lifecycle note

Do not initialize the renderer with `applicationContext` in a property initializer. Initialize it in `onCreate()`:

```kotlin
class AAProxyPluginService : Service() {
    private lateinit var renderer: AAPluginRenderer

    override fun onCreate() {
        super.onCreate()
        renderer = AAPluginRenderer(applicationContext)
    }
}
```

Using `applicationContext` too early can crash because the `Service` context may not be attached yet.

## Build notes

The app module must enable AIDL:

```groovy
android {
    buildFeatures {
        aidl true
        compose true
    }
}
```

This sample uses AndroidX Remote Compose alpha artifacts and Compose BOM. The repository currently contains an app module, AIDL source set, Kotlin source set, and Android manifest.

## ProGuard / R8

For release builds, keep the plugin service, contract models, AIDL stubs, Moshi adapters, and Remote Compose classes:

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

## Development checklist

1. Install the plugin APK.
2. Make sure the service is exported and has the `PLUGIN_AA` intent filter.
3. Make sure host app manifest has a `<queries>` entry for `com.github.deadknight.aaproxycompanion.PLUGIN_AA`.
4. Select the plugin package in the host app.
5. Host binds to `IAAProxyPlugin`.
6. Host calls `getManifestJson()`.
7. Host sends `PluginRenderRequest` to `render(...)`.
8. Plugin returns Remote Compose document bytes.
9. Host displays the bytes in `RemoteComposePlayer`.
10. Plugin emits `HostAction(1000, ...)` for navigation/actions.

## Troubleshooting

### Plugin is not discovered

Check the plugin manifest service declaration and the host app `<queries>` block.

### `render(...)` is not called

Check that the host successfully bound to the service and that `getManifestJson()` succeeds.

### White screen

Log the returned byte array size. Empty bytes usually mean plugin-side parsing or rendering failed.

### Service crashes on startup

Initialize renderer in `onCreate()`, not in a property initializer.

### UI scale is wrong

Use `request.densityDpi` in `RemoteCreationDisplayInfo`. Do not try to scale only at playback time.
