package com.github.deadknight.aaproxyrsplugin.plugin

import SerializerMoshi
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.github.deadknight.aaproxycompanion.plugin.aidl.IAAProxyPlugin
import com.github.deadknight.aaproxyrsplugin.AAPluginRenderer
import fromJson
import kotlinx.coroutines.runBlocking
import toJson

class AAProxyPluginService : Service() {

    private lateinit var renderer: AAPluginRenderer

    override fun onCreate() {
        super.onCreate()
        renderer = AAPluginRenderer(applicationContext)
    }

    private val binder = object : IAAProxyPlugin.Stub() {
        override fun getApiVersion(): Int = 1

        override fun getManifestJson(): String {
            return SerializerMoshi.moshiKotlin.toJson(PluginManifest::class.java,
                PluginManifest(
                    pluginId = "retro.cluster",
                    version = 1,
                    displayName = "Retro Cluster",
                    initialScreen = "home",
                    screens = listOf("home", "tpms_detail", "trip_stats"),
                    requestedPluginDataKeys = listOf(
                        PluginDataKeys.VEHICLE_SPEED,
                        PluginDataKeys.VEHICLE_ODOMETER,
                        PluginDataKeys.VEHICLE_TPMS
                    ),
                    customDataKeys = emptyList(),
                    supportedActions = listOf(
                        PluginActions.PUSH,
                        PluginActions.REPLACE,
                        PluginActions.POP,
                        PluginActions.SCRIPT_EVENT,
                        PluginActions.SUBSCRIBE_WS_TOPIC,
                        PluginActions.UNSUBSCRIBE_WS_TOPIC,
                    )
                )
            )
        }

        override fun render(renderRequestJson: String) = runBlocking {
            val request = SerializerMoshi.moshiKotlin.fromJson(
                PluginRenderRequest::class.java,
                renderRequestJson
            )

            renderer.render(request = request)
        }

        override fun onAction(actionJson: String, renderRequestJson: String) = runBlocking {
            val action = SerializerMoshi.moshiKotlin.fromJson(
                PluginAction::class.java,
                actionJson
            )

            val request = SerializerMoshi.moshiKotlin.fromJson(
                PluginRenderRequest::class.java,
                renderRequestJson
            )

            renderer.render(
                request = request,
                action = action
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}