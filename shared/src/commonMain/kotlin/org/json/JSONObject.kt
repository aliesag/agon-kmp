package org.json

import kotlinx.serialization.json.*

/**
 * KMP-compatible shim for Android's org.json.JSONObject.
 * Backed by kotlinx.serialization.json. API surface mirrors what AppViewModel uses.
 */
class JSONObject {
    val map: MutableMap<String, Any?> = LinkedHashMap()

    constructor()

    constructor(json: String) { parseInto(json) }

    private fun parseInto(json: String) {
        val el = try { Json.parseToJsonElement(json) } catch (_: Exception) { return }
        val obj = el as? JsonObject ?: return
        for ((k, v) in obj) map[k] = unwrap(v)
    }

    private fun unwrap(v: JsonElement): Any? = when (v) {
        is JsonObject -> JSONObject().also { jo -> v.forEach { (k, vv) -> jo.put(k, unwrap(vv)) } }
        is JsonArray -> JSONArray().also { ja -> v.forEach { ja.put(unwrap(it)) } }
        is JsonPrimitive -> when {
            v.isString -> v.content
            v.booleanOrNull != null -> v.booleanOrNull
            v.intOrNull != null -> v.intOrNull
            v.longOrNull != null -> v.longOrNull
            v.doubleOrNull != null -> v.doubleOrNull
            else -> v.content
        }
        JsonNull -> null
    }

    fun put(key: String, value: Any?): JSONObject { map[key] = value; return this }
    fun put(key: String, value: Int): JSONObject { map[key] = value; return this }
    fun put(key: String, value: Long): JSONObject { map[key] = value; return this }
    fun put(key: String, value: Boolean): JSONObject { map[key] = value; return this }

    fun getInt(key: String): Int = (map[key] as? Number)?.toInt() ?: 0
    fun getLong(key: String): Long = (map[key] as? Number)?.toLong() ?: 0L
    fun getString(key: String): String = map[key]?.toString() ?: ""
    fun getBoolean(key: String): Boolean = map[key] as? Boolean ?: false

    fun optInt(key: String, default: Int = 0): Int = (map[key] as? Number)?.toInt() ?: default
    fun optLong(key: String, default: Long = 0L): Long = (map[key] as? Number)?.toLong() ?: default
    fun optString(key: String, default: String = ""): String = map[key]?.toString() ?: default
    fun optString(key: String, default: String?): String? = map[key]?.toString() ?: default
    fun optBoolean(key: String, default: Boolean = false): Boolean = map[key] as? Boolean ?: default
    fun optDouble(key: String, default: Double = 0.0): Double = (map[key] as? Number)?.toDouble() ?: default

    fun has(key: String): Boolean = map.containsKey(key)
    fun keys(): Iterator<String> = map.keys.iterator()
    fun opt(key: String): Any? = map[key]

    fun optJSONObject(key: String): JSONObject? = map[key] as? JSONObject
    fun optJSONArray(key: String): JSONArray? = map[key] as? JSONArray

    fun toJsonElement(): JsonElement = toElement(map.values.toList(), map.keys.toList())
    private fun toElement(values: List<Any?>, keys: List<String>): JsonObject {
        val b = buildJsonObject { }
        val sb = StringBuilder()
        val inner = buildJsonObject {
            for (i in keys.indices) {
                put(keys[i], anyToElement(values[i]))
            }
        }
        return inner
    }
    private fun anyToElement(v: Any?): JsonElement = when (v) {
        null -> JsonNull
        is JSONObject -> v.toJsonElement()
        is JSONArray -> v.toJsonElement()
        is String -> JsonPrimitive(v)
        is Int -> JsonPrimitive(v)
        is Long -> JsonPrimitive(v)
        is Double -> JsonPrimitive(v)
        is Boolean -> JsonPrimitive(v)
        else -> JsonPrimitive(v.toString())
    }

    override fun toString(): String = toJsonElement().toString()
}
