package com.agon.app.data

import com.agon.app.BuildConfig
import com.agon.app.Http
import com.agon.app.ui.AuthRepository
import com.agon.app.ui.AuthUser
import com.russhwolf.settings.Settings
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Firebase Auth over the Identity Toolkit REST API (same backend the Android SDK talks to).
 * Pure commonMain: no Firebase iOS SDK / SPM package needed.
 * Needs BuildConfig.FIREBASE_API_KEY = "Web API Key" from Firebase console -> Project settings.
 */
class FirebaseAuthRest(private val store: Settings = Settings()) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val base = "https://identitytoolkit.googleapis.com/v1/accounts"
    private val key get() = BuildConfig.FIREBASE_API_KEY

    private object K {
        const val UID = "fb_uid"; const val EMAIL = "fb_email"; const val NAME = "fb_name"
        const val ID_TOKEN = "fb_id_token"; const val REFRESH = "fb_refresh"
        const val VERIFIED = "fb_verified"; const val PWD = "fb_pwd_provider"
    }

    override val currentUser: AuthUser?
        get() {
            val uid = store.getStringOrNull(K.UID) ?: return null
            return AuthUser(
                uid = uid,
                email = store.getString(K.EMAIL, ""),
                displayName = store.getStringOrNull(K.NAME),
                isEmailVerified = store.getBoolean(K.VERIFIED, false),
                isPasswordProvider = store.getBoolean(K.PWD, true)
            )
        }

    private class FbError(val code: String) : Exception(code)

    private suspend fun call(endpoint: String, body: JsonObject): JsonObject {
        if (key.isBlank()) throw IllegalStateException("FIREBASE_API_KEY غير مضبوط (BuildConfig.kt)")
        val res = Http.client.post("$base:$endpoint?key=$key") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = res.bodyAsText()
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        if (!res.status.isSuccess()) {
            val code = obj?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
            throw FbError(code)
        }
        return obj ?: JsonObject(emptyMap())
    }

    private fun mapError(e: Throwable): Exception {
        val c = (e as? FbError)?.code ?: return Exception(e.message ?: "حدث خطأ")
        return Exception(
            when {
                c.startsWith("EMAIL_NOT_FOUND") || c.startsWith("USER_NOT_FOUND") -> "لا يوجد حساب بهذا البريد الإلكتروني"
                c.startsWith("INVALID_PASSWORD") || c.startsWith("INVALID_LOGIN_CREDENTIALS") -> "كلمة المرور غير صحيحة"
                c.startsWith("EMAIL_EXISTS") -> "هذا البريد الإلكتروني مسجل بالفعل، يرجى تسجيل الدخول"
                c.startsWith("WEAK_PASSWORD") -> "كلمة المرور ضعيفة (6 أحرف على الأقل)"
                c.startsWith("INVALID_EMAIL") -> "صيغة البريد الإلكتروني غير صحيحة"
                c.startsWith("TOO_MANY_ATTEMPTS") -> "محاولات كثيرة، حاول لاحقاً"
                c.startsWith("USER_DISABLED") -> "هذا الحساب معطل"
                else -> "حدث خطأ: $c"
            }
        )
    }

    private fun persist(o: JsonObject, fallbackEmail: String, name: String?, verified: Boolean?): AuthUser {
        val uid = o["localId"]!!.jsonPrimitive.content
        val email = o["email"]?.jsonPrimitive?.contentOrNull ?: fallbackEmail
        val dn = name ?: o["displayName"]?.jsonPrimitive?.contentOrNull
        store.putString(K.UID, uid)
        store.putString(K.EMAIL, email)
        if (dn != null) store.putString(K.NAME, dn)
        o["idToken"]?.jsonPrimitive?.contentOrNull?.let { store.putString(K.ID_TOKEN, it) }
        o["refreshToken"]?.jsonPrimitive?.contentOrNull?.let { store.putString(K.REFRESH, it) }
        if (verified != null) store.putBoolean(K.VERIFIED, verified)
        store.putBoolean(K.PWD, true)
        return currentUser!!
    }

    private suspend fun lookup(idToken: String): JsonObject? = try {
        call("lookup", buildJsonObject { put("idToken", idToken) })["users"]?.jsonArray?.firstOrNull()?.jsonObject
    } catch (_: Throwable) { null }

    override suspend fun signInEmail(email: String, pass: String): AuthUser = try {
        val o = call("signInWithPassword", buildJsonObject {
            put("email", email); put("password", pass); put("returnSecureToken", true)
        })
        val verified = lookup(o["idToken"]!!.jsonPrimitive.content)?.get("emailVerified")?.jsonPrimitive?.booleanOrNull ?: false
        persist(o, email, null, verified)
    } catch (e: Throwable) { throw mapError(e) }

    override suspend fun createEmail(email: String, pass: String, displayName: String): AuthUser = try {
        val o = call("signUp", buildJsonObject {
            put("email", email); put("password", pass); put("returnSecureToken", true)
        })
        val token = o["idToken"]!!.jsonPrimitive.content
        // same as UserProfileChangeRequest.setDisplayName
        val up = call("update", buildJsonObject {
            put("idToken", token); put("displayName", displayName); put("returnSecureToken", true)
        })
        val merged = JsonObject(o + up.filterKeys { it == "idToken" || it == "refreshToken" })
        persist(merged, email, displayName, false)
    } catch (e: Throwable) { throw mapError(e) }

    override suspend fun sendVerification(user: AuthUser) {
        try {
            val t = store.getStringOrNull(K.ID_TOKEN) ?: return
            call("sendOobCode", buildJsonObject { put("requestType", "VERIFY_EMAIL"); put("idToken", t) })
        } catch (e: Throwable) { throw mapError(e) }
    }

    override suspend fun sendPasswordReset(email: String) {
        try {
            call("sendOobCode", buildJsonObject { put("requestType", "PASSWORD_RESET"); put("email", email) })
        } catch (e: Throwable) { throw mapError(e) }
    }

    /** idToken lives 1h: refresh it before lookup when needed. */
    private suspend fun freshToken(): String? {
        val refresh = store.getStringOrNull(K.REFRESH) ?: return store.getStringOrNull(K.ID_TOKEN)
        return try {
            val res = Http.client.post("https://securetoken.googleapis.com/v1/token?key=$key") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("grant_type=refresh_token&refresh_token=$refresh")
            }
            if (!res.status.isSuccess()) return store.getStringOrNull(K.ID_TOKEN)
            val o = json.parseToJsonElement(res.bodyAsText()).jsonObject
            o["id_token"]?.jsonPrimitive?.contentOrNull?.also { store.putString(K.ID_TOKEN, it) }
            o["refresh_token"]?.jsonPrimitive?.contentOrNull?.let { store.putString(K.REFRESH, it) }
            store.getStringOrNull(K.ID_TOKEN)
        } catch (_: Throwable) { store.getStringOrNull(K.ID_TOKEN) }
    }

    override suspend fun reload(user: AuthUser): AuthUser {
        val t = freshToken() ?: return user
        val u = lookup(t) ?: return user
        store.putBoolean(K.VERIFIED, u["emailVerified"]?.jsonPrimitive?.booleanOrNull ?: false)
        u["displayName"]?.jsonPrimitive?.contentOrNull?.let { store.putString(K.NAME, it) }
        val providers = (u["providerUserInfo"] as? JsonArray)
            ?.map { it.jsonObject["providerId"]?.jsonPrimitive?.contentOrNull } ?: emptyList()
        store.putBoolean(K.PWD, providers.isEmpty() || "password" in providers)
        return currentUser ?: user
    }

    override fun signOut() {
        listOf(K.UID, K.EMAIL, K.NAME, K.ID_TOKEN, K.REFRESH, K.VERIFIED, K.PWD).forEach { store.remove(it) }
    }
}
