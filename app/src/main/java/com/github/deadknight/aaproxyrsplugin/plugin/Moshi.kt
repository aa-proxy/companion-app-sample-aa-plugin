import com.github.deadknight.aaproxyrsplugin.plugin.adapter.BigDecimalAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.BigIntegerAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.ByteArrayAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.LocalDateAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.LocalDateTimeAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.OffsetDateTimeAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.URIAdapter
import com.github.deadknight.aaproxyrsplugin.plugin.adapter.UUIDAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Date

object Serializer {
    @JvmStatic
    val moshiBuilder: Moshi.Builder = Moshi.Builder()
        .add(OffsetDateTimeAdapter())
        .add(LocalDateTimeAdapter())
        .add(LocalDateAdapter())
        .add(UUIDAdapter())
        .add(ByteArrayAdapter())
        .add(URIAdapter())
        .add(BigDecimalAdapter())
        .add(BigIntegerAdapter())

    @JvmStatic
    val moshi: Moshi by lazy {
        moshiBuilder.build()
    }
}

object SerializerMoshi {
    @JvmStatic
    val moshiKotlin: Moshi by lazy {
        Serializer.moshiBuilder
            //.add(StringNullFactory)
            .add(KotlinJsonAdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .build()
    }
}

fun <T> Moshi.fromJson(clazz: Class<T>, json: String): T {
    return this.adapter(clazz).fromJson(json) ?: clazz.newInstance()
}

fun <T> Moshi.toJson(clazz: Class<T>, data: T): String {
    return this.adapter(clazz).toJson(data)
}