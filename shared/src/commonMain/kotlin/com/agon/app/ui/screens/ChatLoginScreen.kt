package com.agon.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.ui.AuthViewModel
import com.agon.app.ui.theme.NetflixRed
import com.agon.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatLoginScreen(
    vm: AppViewModel,
    authViewModel: AuthViewModel? = null,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existingName by vm.profileName.collectAsState()
    val existingEmail by vm.userEmail.collectAsState()

    var username by remember { 
        mutableStateOf(if (existingName.isNotBlank() && existingName != "ضيف") existingName else "") 
    }
    var email by remember { mutableStateOf(existingEmail) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val handleJoin = {
        val trimmedUser = username.trim()
        val trimmedEmail = email.trim()

        if (trimmedUser.isBlank()) {
            errorMessage = "يرجى كتابة اسم المستخدم للمتابعة"
        } else if (trimmedUser.length < 2) {
            errorMessage = "اسم المستخدم يجب أن يكون حرفين على الأقل"
        } else if (trimmedEmail.isBlank()) {
            errorMessage = "يرجى إدخال البريد الإلكتروني"
        } else if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(trimmedEmail)) {
            errorMessage = "صيغة البريد الإلكتروني غير صحيحة"
        } else {
            errorMessage = null
            vm.setUserAccount(trimmedUser, trimmedEmail)
            authViewModel?.continueAsGuest()
            onSuccess()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0C10),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "التسجيل للشات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0C10)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B0C10)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glow badge icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NetflixRed.copy(alpha = 0.35f),
                                    Color(0xFFE50914).copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.5.dp, NetflixRed.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = NetflixRed,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "الانضمام إلى مجتمع الدردشة",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "التطبيق مجاني بالكامل! يرجى إدخال اسمك وبريدك الإلكتروني فقط لتتمكن من التفاعل والمشاركة في الشات.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF9E9E9E),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Input Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF14151B)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Error message
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            errorMessage?.let { error ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE50914).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        color = Color(0xFFFF5252),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Username field
                        Text(
                            text = "اسم المستخدم",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = {
                                Text("اكتب اسمك أو لقبك المفضل", color = Color(0xFF6B7280), fontSize = 14.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NetflixRed)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E2029),
                                unfocusedContainerColor = Color(0xFF1E2029),
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = Color(0xFF2C2F3A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Email field
                        Text(
                            text = "البريد الإلكتروني",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = {
                                Text("name@example.com", color = Color(0xFF6B7280), fontSize = 14.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = NetflixRed)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    handleJoin()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E2029),
                                unfocusedContainerColor = Color(0xFF1E2029),
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = Color(0xFF2C2F3A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        // Submit Button
                        Button(
                            onClick = handleJoin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed
                            )
                        ) {
                            Text(
                                text = "دخول إلى الشات 💬",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Privacy/Free Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "لا توجد كلمات مرور مطلوبة - استمتع بالدردشة فوراً وبشكل مجاني",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBack) {
                    Text(
                        text = "العودة إلى الرئيسية",
                        color = Color(0xFFB0B3B8),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
