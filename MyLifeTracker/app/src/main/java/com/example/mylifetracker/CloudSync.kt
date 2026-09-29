package com.example.mylifetracker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object FirebaseConfig {
    val apiKey: String get() = BuildConfig.FIREBASE_API_KEY.trim()
    val databaseUrl: String get() = BuildConfig.FIREBASE_DATABASE_URL.trim().trimEnd('/')
    val isConfigured: Boolean get() = apiKey.isNotBlank() && databaseUrl.startsWith("https://")
}

data class AuthSession(
    val uid: String,
    val email: String,
    val idToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
)

class AuthSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("cloud_auth", Context.MODE_PRIVATE)

    fun load(): AuthSession? {
        val uid = prefs.getString("uid", null) ?: return null
        val email = prefs.getString("email", "").orEmpty()
        val idToken = prefs.getString("idToken", null) ?: return null
        val refreshToken = prefs.getString("refreshToken", null) ?: return null
        val expiresAt = prefs.getLong("expiresAt", 0L)
        return AuthSession(uid, email, idToken, refreshToken, expiresAt)
    }

    fun save(session: AuthSession) {
        prefs.edit()
            .putString("uid", session.uid)
            .putString("email", session.email)
            .putString("idToken", session.idToken)
            .putString("refreshToken", session.refreshToken)
            .putLong("expiresAt", session.expiresAtMillis)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}

class FirebaseRestClient(private val sessionStore: AuthSessionStore) {
    suspend fun signIn(email: String, password: String): Result<AuthSession> = authRequest(
        endpoint = "accounts:signInWithPassword",
        email = email,
        password = password
    )

    suspend fun signUp(email: String, password: String): Result<AuthSession> = authRequest(
        endpoint = "accounts:signUp",
        email = email,
        password = password
    )

    private suspend fun authRequest(endpoint: String, email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            ensureConfigured()
            val url = "https://identitytoolkit.googleapis.com/v1/$endpoint?key=${encode(FirebaseConfig.apiKey)}"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("returnSecureToken", true)
            }.toString()
            val body = request("POST", url, payload, "application/json; charset=utf-8")
            val json = JSONObject(body)
            AuthSession(
                uid = json.getString("localId"),
                email = json.optString("email", email.trim()),
                idToken = json.getString("idToken"),
                refreshToken = json.getString("refreshToken"),
                expiresAtMillis = System.currentTimeMillis() + json.optLong("expiresIn", 3600L) * 1000L
            ).also(sessionStore::save)
        }
    }

    suspend fun validSession(): AuthSession? = withContext(Dispatchers.IO) {
        val current = sessionStore.load() ?: return@withContext null
        if (current.expiresAtMillis > System.currentTimeMillis() + 90_000L) return@withContext current
        refresh(current).getOrNull()
    }

    private suspend fun refresh(current: AuthSession): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            ensureConfigured()
            val body = "grant_type=refresh_token&refresh_token=${encode(current.refreshToken)}"
            val response = request(
                method = "POST",
                urlString = "https://securetoken.googleapis.com/v1/token?key=${encode(FirebaseConfig.apiKey)}",
                body = body,
                contentType = "application/x-www-form-urlencoded"
            )
            val json = JSONObject(response)
            AuthSession(
                uid = json.optString("user_id", current.uid),
                email = current.email,
                idToken = json.getString("id_token"),
                refreshToken = json.optString("refresh_token", current.refreshToken),
                expiresAtMillis = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L
            ).also(sessionStore::save)
        }
    }

    suspend fun downloadSnapshot(): Result<TrackerSnapshot?> = withContext(Dispatchers.IO) {
        runCatching {
            ensureConfigured()
            val session = validSession() ?: error("로그인이 필요해요.")
            val url = userDataUrl(session)
            val raw = request("GET", url, null, null)
            TrackerSnapshotJson.decode(raw)
        }
    }

    suspend fun uploadSnapshot(snapshot: TrackerSnapshot): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureConfigured()
            val session = validSession() ?: error("로그인이 필요해요.")
            request("PUT", userDataUrl(session), TrackerSnapshotJson.encode(snapshot), "application/json; charset=utf-8")
            Unit
        }
    }

    private fun userDataUrl(session: AuthSession): String =
        "${FirebaseConfig.databaseUrl}/users/${encode(session.uid)}/tracker.json?auth=${encode(session.idToken)}"

    private fun ensureConfigured() {
        if (!FirebaseConfig.isConfigured) error("클라우드 설정이 아직 연결되지 않았어요.")
    }

    private fun request(method: String, urlString: String, body: String?, contentType: String?): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            doInput = true
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType ?: "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException(readableFirebaseError(text, code))
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun readableFirebaseError(raw: String, httpCode: Int): String {
        val code = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
        return when {
            code.contains("EMAIL_EXISTS") -> "이미 가입된 이메일이에요."
            code.contains("INVALID_LOGIN_CREDENTIALS") || code.contains("INVALID_PASSWORD") || code.contains("EMAIL_NOT_FOUND") -> "이메일 또는 비밀번호를 확인해 주세요."
            code.contains("WEAK_PASSWORD") -> "비밀번호가 너무 짧아요. 6자 이상으로 입력해 주세요."
            code.contains("OPERATION_NOT_ALLOWED") -> "Firebase에서 이메일/비밀번호 로그인을 먼저 활성화해 주세요."
            code.contains("PERMISSION_DENIED") -> "데이터베이스 권한 설정을 확인해 주세요."
            code.contains("TOO_MANY_ATTEMPTS") -> "요청이 너무 많아요. 잠시 뒤 다시 시도해 주세요."
            code.isNotBlank() -> code.replace('_', ' ')
            else -> "클라우드 요청에 실패했어요. (HTTP $httpCode)"
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
