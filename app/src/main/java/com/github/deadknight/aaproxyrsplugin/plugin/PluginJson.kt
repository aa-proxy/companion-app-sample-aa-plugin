package com.github.deadknight.aaproxyrsplugin.plugin

object PluginJson {
    inline fun <reified T> encode(value: T): String {
        return SerializerMoshi.moshiKotlin.adapter(T::class.java).toJson(value)
    }

    inline fun <reified T> decode(json: String?): T? {
        return try {
            SerializerMoshi.moshiKotlin.adapter(T::class.java).fromJson(json!!)
        } catch (ex: Exception) {
            null
        }
    }
}