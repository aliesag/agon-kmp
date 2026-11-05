package com.agon.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Firebase-independent user snapshot. */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val isEmailVerified: Boolean = false,
    val isPasswordProvider: Boolean = true
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: AuthUser?) : AuthState()
    object Guest : AuthState()
    data class VerificationSent(val user: AuthUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Auth backend contract. Implementation must be provided per platform:
 *   iOS: Firebase iOS SDK (FirebaseAuth) — wiring in stage 4 (needs SPM/XcodeGen packages).
 *   Android: Firebase Android SDK (existing behaviour).
 * Until wired, DefaultAuthRepository returns a clear "not configured" error so UI shows the message.
 */
interface AuthRepository {
    val currentUser: AuthUser?
    suspend fun signInEmail(email: String, pass: String): AuthUser
    suspend fun createEmail(email: String, pass: String, displayName: String): AuthUser
    suspend fun sendVerification(user: AuthUser)
    suspend fun sendPasswordReset(email: String)
    suspend fun reload(user: AuthUser): AuthUser
    fun signOut()
}

/** KMP fallback: everything fails with a clear message; caller must plug a real backend. */
class DefaultAuthRepository : AuthRepository {
    override val currentUser: AuthUser? = null
    override suspend fun signInEmail(email: String, pass: String): AuthUser =
        throw IllegalStateException("Firebase iOS SDK not wired yet")
    override suspend fun createEmail(email: String, pass: String, displayName: String): AuthUser =
        throw IllegalStateException("Firebase iOS SDK not wired yet")
    override suspend fun sendVerification(user: AuthUser) {}
    override suspend fun sendPasswordReset(email: String) =
        throw IllegalStateException("Firebase iOS SDK not wired yet")
    override suspend fun reload(user: AuthUser): AuthUser = user
    override fun signOut() {}
}

class AuthViewModel(
    private val repo: AuthRepository = DefaultAuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest

    var savedRegisterName: String = ""
    var savedRegisterEmail: String = ""
    var savedRegisterPassword: String = ""
    var savedRegisterUsername: String = ""
    var savedRegisterAge: String = ""

    init {
        repo.currentUser?.let { u ->
            _isGuest.value = false
            if (!u.isEmailVerified && u.isPasswordProvider) {
                _authState.value = AuthState.VerificationSent(u)
            } else {
                _authState.value = AuthState.Success(u)
            }
            viewModelScope.launch {
                try {
                    val reloaded = repo.reload(u)
                    if (reloaded.isEmailVerified) _authState.value = AuthState.Success(reloaded)
                    else if (reloaded.isPasswordProvider) _authState.value = AuthState.VerificationSent(reloaded)
                } catch (_: Exception) { /* ignore transient */ }
            }
        }
    }

    fun continueAsGuest() {
        _isGuest.value = true
        _authState.value = AuthState.Guest
    }

    fun resetState() { _authState.value = AuthState.Idle }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("يرجى ملء جميع الحقول"); return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repo.signInEmail(email, pass)
                if (!user.isEmailVerified && user.isPasswordProvider) {
                    _authState.value = AuthState.VerificationSent(user)
                } else {
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "حدث خطأ أثناء تسجيل الدخول")
            }
        }
    }

    fun register(
        email: String, pass: String, name: String, username: String, age: String,
        onStoreExtra: (String, String) -> Unit
    ) {
        savedRegisterName = name
        savedRegisterEmail = email
        savedRegisterPassword = pass
        savedRegisterUsername = username
        savedRegisterAge = age

        if (email.isBlank() || pass.isBlank() || name.isBlank() || username.isBlank() || age.isBlank()) {
            _authState.value = AuthState.Error("يرجى ملء جميع الحقول"); return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repo.createEmail(email, pass, name)
                repo.sendVerification(user)
                onStoreExtra(username, age)
                _authState.value = AuthState.VerificationSent(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "حدث خطأ أثناء إنشاء الحساب")
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("يرجى إدخال البريد الإلكتروني"); return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                repo.sendPasswordReset(email)
                _authState.value = AuthState.Error("تم إرسال رابط إعادة تعيين كلمة المرور، يرجى التحقق من بريدك")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "حدث خطأ")
            }
        }
    }

    /** Google sign-in is a no-op until the iOS Google Sign-In SDK is wired. */
    fun loginWithGoogle() {
        _authState.value = AuthState.Error("تسجيل الدخول بجوجل غير متاح على iOS بعد")
    }

    fun checkEmailVerification(silent: Boolean = false) {
        viewModelScope.launch {
            val u = repo.currentUser ?: return@launch
            try {
                val reloaded = repo.reload(u)
                if (reloaded.isEmailVerified) _authState.value = AuthState.Success(reloaded)
                else if (!silent) _authState.value = AuthState.Error("لم يتم التحقق من البريد الإلكتروني بعد")
            } catch (e: Exception) {
                if (!silent) _authState.value = AuthState.Error(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }

    fun logout() {
        repo.signOut()
        _isGuest.value = false
        _authState.value = AuthState.Idle
    }
}
