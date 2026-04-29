package com.github.deadknight.aaproxyrsplugin

import SerializerMoshi
import android.content.Context
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.ui.graphics.Color
import com.github.deadknight.aaproxyrsplugin.plugin.PluginAction
import com.github.deadknight.aaproxyrsplugin.plugin.PluginActions
import com.github.deadknight.aaproxyrsplugin.plugin.PluginContract
import com.github.deadknight.aaproxyrsplugin.plugin.PluginDataKeys
import com.github.deadknight.aaproxyrsplugin.plugin.PluginJson
import com.github.deadknight.aaproxyrsplugin.plugin.PluginRenderRequest
import com.github.deadknight.aaproxyrsplugin.plugin.SystemThemeData
import com.github.deadknight.aaproxyrsplugin.plugin.VehicleOdometerData
import com.github.deadknight.aaproxyrsplugin.plugin.VehicleSpeedData
import com.github.deadknight.aaproxyrsplugin.plugin.VehicleTpmsData
import toJson
import kotlin.math.roundToInt

class AAPluginRenderer(
    private val context: Context
) {
    suspend fun render(
        request: PluginRenderRequest,
        action: PluginAction? = null
    ): ByteArray {

        val speed = PluginJson.decode<VehicleSpeedData>(
            request.data[PluginDataKeys.VEHICLE_SPEED.value]
        )
        val odometer = PluginJson.decode<VehicleOdometerData>(
            request.data[PluginDataKeys.VEHICLE_ODOMETER.value]
        )
        val tpms = PluginJson.decode<VehicleTpmsData>(
            request.data[PluginDataKeys.VEHICLE_TPMS.value]
        )
        val theme = PluginJson.decode<SystemThemeData>(
            request.data[PluginDataKeys.SYSTEM_THEME.value]
        )

        val currentScreen = request.navigationState.stack.lastOrNull()?.screen ?: "home"
        val background = if (theme?.theme == "light") Color(0xFFF5F7FA) else Color(0xFF1E293B)

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
                    .background(background)
            ) {
                RemoteColumn(
                    modifier = RemoteModifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.rdp, vertical = 24.rdp)
                ) {
                    RemoteText("PLUGIN: ${request.pluginId}")
                    RemoteText("SCREEN: $currentScreen")

                    when (currentScreen) {
                        "home" -> {
                            RemoteText("Speed: ${speed?.speedKph?.roundToInt() ?: "--"}")
                            RemoteText("Odometer: ${odometer?.totalKm ?: "--"} km")
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
                        }

                        "tpms_detail" -> {
                            RemoteText("TPMS")
                            RemoteText("FL: ${tpms?.fl?.pressureKpa ?: "--"}")
                            RemoteText("FR: ${tpms?.fr?.pressureKpa ?: "--"}")
                            RemoteText("RL: ${tpms?.rl?.pressureKpa ?: "--"}")
                            RemoteText("RR: ${tpms?.rr?.pressureKpa ?: "--"}")
                            RemoteText(
                                "BACK",
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
                                                        action = PluginActions.POP
                                                    )
                                                )
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        return captured.bytes
    }
}