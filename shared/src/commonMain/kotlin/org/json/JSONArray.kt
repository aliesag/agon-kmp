package org.json

import kotlinx.serialization.json.*

class JSONArray {
    val list: MutableList<Any?> = mutableListOf()
    constructor()
    constructor(json: String) {
        val el = try { Json.parseToJsonElement(json) } catch (_: Exception) { return }
        (el as? JsonArray)?.forEach { list.add(unwrap(it)) }
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
    fun put(v: Any?): JSONArray { list.add(v); return this }
    fun length(): Int = list.size
    fun optJSONObject(i: Int): JSONObject? = list.getOrNull(i) as? JSONObject
    fun optString(i: Int, default: String = ""): String = list.getOrNull(i)?.toString() ?: default
    fun getJSONObject(i: Int): JSONObject = list[i] as JSONObject
    fun toJsonElement(): JsonElement = buildJsonArray {
        for (v in list) add(anyToElement(v))
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
