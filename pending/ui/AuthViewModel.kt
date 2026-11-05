package com.agon.app.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    object Guest : AuthState()
    data class VerificationSent(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isGuest = MutableStateFlow<Boolean>(false)
    val isGuest: StateFlow<Boolean> = _isGuest

    // Preserved registration input data so user won't retype them if returning from email verification
    var savedRegisterName: String = ""
    var savedRegisterEmail: String = ""
    var savedRegisterPassword: String = ""
    var savedRegisterUsername: String = ""
    var savedRegisterAge: String = ""
    
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    init {
        // If firebase is configured and user is logged in
        auth?.currentUser?.let { user ->
            _isGuest.value = false
            if (!user.isEmailVerified && user.providerData.any { it.providerId == "password" }) {
                _authState.value = AuthState.VerificationSent(user)
            } else {
                _authState.value = AuthState.Success(user)
            }
            viewModelScope.launch {
                try {
                    user.reload().await()
                    val reloadedUser = auth.currentUser
                    if (reloadedUser?.isEmailVerified == true) {
                        _authState.value = AuthState.Success(reloadedUser)
                    } else if (reloadedUser != null && reloadedUser.providerData.any { it.providerId == "password" }) {
                        _authState.value = AuthState.VerificationSent(reloadedUser)
                    }
                } catch (e: Exception) {
                    // Ignore transient network errors during initialization
                }
            }
        }
    }

    fun continueAsGuest() {
        _isGuest.value = true
        _authState.value = AuthState.Guest
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun login(email: String, pass: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add google-services.json in settings.")
            return
        }
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("يرجى ملء جميع الحقول")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null && !user.isEmailVerified && user.providerData.any { it.providerId == "password" }) {
                    _authState.value = AuthState.VerificationSent(user)
                } else {
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error("لا يوجد حساب بهذا البريد الإلكتروني")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _authState.value = AuthState.Error("كلمة المرور غير صحيحة")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "حدث خطأ أثناء تسجيل الدخول")
            }
        }
    }

    fun register(email: String, pass: String, name: String, username: String, age: String, onStoreExtra: (String, String) -> Unit) {
        savedRegisterName = name
        savedRegisterEmail = email
        savedRegisterPassword = pass
        savedRegisterUsername = username
        savedRegisterAge = age

        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add google-services.json in settings.")
            return
        }
        if (email.isBlank() || pass.isBlank() || name.isBlank() || username.isBlank() || age.isBlank()) {
            _authState.value = AuthState.Error("يرجى ملء جميع الحقول")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                result.user?.let { user ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    user.sendEmailVerification().await()
                    onStoreExtra(username, age)
                    _authState.value = AuthState.VerificationSent(user)
                } ?: run {
                    _authState.value = AuthState.Error("حدث خطأ غير معروف")
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error("هذا البريد الإلكتروني مسجل بالفعل، يرجى تسجيل الدخول")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "حدث خطأ أثناء إنشاء الحساب")
            }
        }
    }

    fun resetPassword(email: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add google-services.json in settings.")
            return
        }
        if (email.isBlank()) {
            _authState.value = AuthState.Error("يرجى إدخال البريد الإلكتروني")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Error("تم إرسال رابط إعادة تعيين كلمة المرور، يرجى التحقق من بريدك")
            } catch (e: FirebaseAuthInvalidUserException) {
                _authState.value = AuthState.Error("لا يوجد حساب مسجل بهذا البريد")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "حدث خطأ")
            }
        }
    }

    fun loginWithGoogle(context: Context, webClientId: String) {
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase is not configured. Please add google-services.json in settings.")
            return
        }
        if (webClientId.isBlank()) {
             _authState.value = AuthState.Error("إعدادات تسجيل جوجل غير مكتملة (Web Client ID مفقود)")
             return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    _authState.value = AuthState.Success(authResult.user)
                } else {
                    _authState.value = AuthState.Error("فشل تسجيل الدخول بجوجل")
                }
            } catch (e: GetCredentialException) {
                _authState.value = AuthState.Error("تم إلغاء تسجيل الدخول أو حدث خطأ: ${e.message}")
            } catch (e: GoogleIdTokenParsingException) {
                _authState.value = AuthState.Error("خطأ في بيانات جوجل")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("خطأ: ${e.message}")
            }
        }
    }
    
    fun checkEmailVerification(silent: Boolean = false) {
        viewModelScope.launch {
            try {
                auth?.currentUser?.reload()?.await()
                if (auth?.currentUser?.isEmailVerified == true) {
                    _authState.value = AuthState.Success(auth.currentUser)
                } else {
                    if (!silent) {
                        _authState.value = AuthState.Error("لم يتم التحقق من البريد الإلكتروني بعد")
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _authState.value = AuthState.Error(e.message ?: "حدث خطأ غير معروف")
                }
            }
        }
    }
    
    fun logout() {
        auth?.signOut()
        _isGuest.value = false
        _authState.value = AuthState.Idle
    }
}
