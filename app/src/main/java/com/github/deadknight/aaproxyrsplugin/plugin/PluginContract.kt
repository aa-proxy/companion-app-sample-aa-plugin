package com.github.deadknight.aaproxyrsplugin.plugin

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = false)
enum class PluginSystemTheme(val value: String) {
  @Json(name = "light") LIGHT("light"),
  @Json(name = "dark") DARK("dark"),
}

@JsonClass(generateAdapter = false)
enum class PluginDataKeys(val value: String) {
  @Json(name = "VEHICLE_SPEED") VEHICLE_SPEED("VEHICLE_SPEED"),
  @Json(name = "VEHICLE_ODOMETER") VEHICLE_ODOMETER("VEHICLE_ODOMETER"),
  @Json(name = "VEHICLE_TPMS") VEHICLE_TPMS("VEHICLE_TPMS"),
  @Json(name = "VEHICLE_TRIP") VEHICLE_TRIP("VEHICLE_TRIP"),
  @Json(name = "VEHICLE_BATTERY") VEHICLE_BATTERY("VEHICLE_BATTERY"),
  @Json(name = "VEHICLE_DRIVE_STATE") VEHICLE_DRIVE_STATE("VEHICLE_DRIVE_STATE"),
  @Json(name = "EVENT_TOPIC_DATA") EVENT_TOPIC_DATA("EVENT_TOPIC_DATA"),
  @Json(name = "EVENT_SUBSCRIBE_TOPIC_RESULT")
  EVENT_SUBSCRIBE_TOPIC_RESULT("EVENT_SUBSCRIBE_TOPIC_RESULT"),
  @Json(name = "EVENT_UNSUBSCRIBE_TOPIC_RESULT")
  EVENT_UNSUBSCRIBE_TOPIC_RESULT("EVENT_UNSUBSCRIBE_TOPIC_RESULT"),
  @Json(name = "SYSTEM_THEME") SYSTEM_THEME("SYSTEM_THEME"),
  @Json(name = "HOST_NAVIGATION_STATE") HOST_NAVIGATION_STATE("HOST_NAVIGATION_STATE"),
}

@JsonClass(generateAdapter = false)
enum class PluginActions(val value: String) {
  @Json(name = "PUSH") PUSH("PUSH"),
  @Json(name = "REPLACE") REPLACE("REPLACE"),
  @Json(name = "POP") POP("POP"),
  @Json(name = "SUBSCRIBE_WS_TOPIC") SUBSCRIBE_WS_TOPIC("SUBSCRIBE_WS_TOPIC"),
  @Json(name = "UNSUBSCRIBE_WS_TOPIC") UNSUBSCRIBE_WS_TOPIC("UNSUBSCRIBE_WS_TOPIC"),
  @Json(name = "SCRIPT_EVENT") SCRIPT_EVENT("SCRIPT_EVENT"),
  @Json(name = "CONSUME_DATA") CONSUME_DATA("CONSUME_DATA"),
}

object PluginContract {
  object HostActionIds {
    const val PLUGIN_ACTION = 1000
  }
}

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginManifest(
    val pluginId: String,
    val version: Int,
    val displayName: String,
    val initialScreen: String,
    val screens: List<String>,
    val requestedPluginDataKeys: List<PluginDataKeys>,
    val customDataKeys: List<String> = emptyList(),
    val supportedActions: List<PluginActions> = emptyList(),
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class NavigationState(val pluginId: String, val stack: List<NavigationEntry>) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class NavigationEntry(val screen: String, val paramsJson: String? = null) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginAction(
    val action: PluginActions,
    val screen: String? = null,
    val paramsJson: String? = null,
    val name: String? = null,
    val payloadJson: String? = null,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class HostDataEntry(val key: String, val json: String) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class JsonEnvelope(val version: Int, val timestamp: Long, val dataJson: String) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleSpeedData(val speedKph: Float? = null) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleOdometerData(val totalKm: Float? = null, val tripKm: Float? = null) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class TpmsWheelData(val pressureKpa: Float? = null, val tempC: Float? = null) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleTpmsData(
    val fl: TpmsWheelData? = null,
    val fr: TpmsWheelData? = null,
    val rl: TpmsWheelData? = null,
    val rr: TpmsWheelData? = null,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleTripData(
    val tripKm: Float? = null,
    val tripDurationSec: Long? = null,
    val averageSpeedKph: Float? = null,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleBatteryData(
    val batteryPercent: Float? = null,
    val batteryKwh: Float? = null,
    val charging: Boolean? = null,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleRangeData(val estimatedKm: Float? = null, val estimatedMi: Float? = null) :
    Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleTemperatureData(val interiorC: Float? = null, val exteriorC: Float? = null) :
    Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleDriveStateData(
    val gear: String? = null,
    val ignitionOn: Boolean? = null,
    val moving: Boolean? = null,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class EventSubscribeTopicResultdata(val uuid: String, val topic: String) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class EventUnsubscribeTopicResultdata(val uuid: String, val topic: String) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class EventTopicData(val uuid: String, val topic: String, val payload: String) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class SystemTimeData(val epochMillis: Long, val formatted: String? = null) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class SystemThemeData(val theme: String) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class SystemUnitsData(
    val distanceUnit: String,
    val speedUnit: String,
    val pressureUnit: String,
    val temperatureUnit: String,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginRenderRequest(
    val pluginId: String,
    val navigationState: NavigationState,
    val data: Map<String, String>,
    val config: Map<String, String> = emptyMap(),
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginRenderResponse(
    val documentBytes: ByteArray,
    val warnings: List<String> = emptyList(),
) : Parcelable
