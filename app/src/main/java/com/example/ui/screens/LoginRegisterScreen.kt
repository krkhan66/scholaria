package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassFeeStructure

enum class AuthScreenState {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    schoolName: String = "Scholaria",
    classFeeStructures: List<ClassFeeStructure>,
    authError: String?,
    registrationSuccessMessage: String? = null,
    resetSuccessMessage: String? = null,
    resetErrorMessage: String? = null,
    isResetSending: Boolean = false,
    isAuthenticating: Boolean = false,
    onLogin: (String, String, String) -> Unit,
    onRegisterUser: (name: String, email: String, password: String, role: String, collegeCode: String) -> Unit,
    onRegisterStudent: (
        name: String,
        email: String,
        phone: String,
        password: String,
        classGrade: String,
        paidFee: Int,
        rollNo: String,
        guardian: String,
        annualFee: Int,
        collegeCode: String
    ) -> Unit,
    onRegisterTeacher: (
        name: String,
        email: String,
        phone: String,
        password: String,
        subject: String,
        qualification: String,
        experience: String,
        role: String,
        collegeCode: String
    ) -> Unit,
    onSendResetLink: (collegeCode: String, email: String) -> Unit = { _, _ -> },
    onClearError: () -> Unit,
    onClearRegistrationSuccess: () -> Unit = {},
    onClearResetMessages: () -> Unit = {},
    onApproveUser: (email: String) -> Unit = {}
) {
    var authScreenState by remember { mutableStateOf(AuthScreenState.LOGIN) }
    var registerRoleTab by remember { mutableIntStateOf(0) } // 0: Student, 1: Teacher/Staff

    if (authScreenState == AuthScreenState.FORGOT_PASSWORD) {
        ForgotPasswordScreen(
            errorMessage = resetErrorMessage,
            successMessage = resetSuccessMessage,
            isSending = isResetSending,
            onSendResetLink = { collegeCode, email ->
                onSendResetLink(collegeCode, email)
            },
            onBackToLogin = {
                authScreenState = AuthScreenState.LOGIN
                onClearResetMessages()
                onClearError()
            },
            onClearMessages = {
                onClearResetMessages()
            }
        )
        return
    }

    val isRegisterMode = authScreenState == AuthScreenState.REGISTER

    // Login Form States
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginCollegeCode by remember { mutableStateOf("SCH-2024") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Student Register Form States
    var regStudentName by remember { mutableStateOf("") }
    var regStudentEmail by remember { mutableStateOf("") }
    var regStudentPhone by remember { mutableStateOf("") }
    var regStudentPassword by remember { mutableStateOf("") }
    var regStudentCollegeCode by remember { mutableStateOf("SCH-2024") }
    var regStudentRollNo by remember { mutableStateOf("") }
    var regStudentGuardian by remember { mutableStateOf("") }
    var selectedGradeLevel by remember { mutableStateOf("10th Grade") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }
    var selectedClassName by remember { mutableStateOf("Grade 10 - A") }
    var regPaidFeeText by remember { mutableStateOf("52000") }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    // Teacher Register Form States
    var regTeacherName by remember { mutableStateOf("") }
    var regTeacherEmail by remember { mutableStateOf("") }
    var regTeacherPhone by remember { mutableStateOf("") }
    var regTeacherPassword by remember { mutableStateOf("") }
    var regTeacherCollegeCode by remember { mutableStateOf("SCH-2024") }
    var regTeacherSubject by remember { mutableStateOf("Mathematics") }
    var regTeacherQualification by remember { mutableStateOf("M.Sc, B.Ed") }
    var regTeacherExperience by remember { mutableStateOf("5 years") }
    var regTeacherRole by remember { mutableStateOf("Teacher") }

    val currentClassStructure = remember(selectedClassName, classFeeStructures) {
        classFeeStructures.find { it.className == selectedClassName }
            ?: ClassFeeStructure(
                className = "Grade 10 - A",
                grade = "10",
                section = "A",
                teacherName = "Anita Desai",
                annualFee = 52000,
                feeFeatures = "Smart Classes, Adv Library, Science Lab, Career Counseling"
            )
    }

    LaunchedEffect(selectedClassName) {
        regPaidFeeText = currentClassStructure.annualFee.toString()
    }

    // Colors matching exact Tailwind M3 specification
    val darkBgColor = Color(0xFF0D1C2E)
    val primaryColor = Color(0xFF005C55)
    val primaryContainerColor = Color(0xFF0F766E)
    val onPrimaryContainerColor = Color(0xFFA3FAEF)
    val cardBgColor = Color(0xFFEAF1FF)
    val textPrimary = Color(0xFF0D1C2E)
    val textSecondary = Color(0xFF3E4947)
    val outlineVariant = Color(0xFFBDC9C6)
    val inputBgColor = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBgColor)
            .testTag("login_register_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (!isRegisterMode) {
                // =========================================================
                // LOGIN SCREEN DESIGN (EXACT MATCH TO HTML SPEC)
                // =========================================================

                // Logo Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(primaryContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Scholaria Logo",
                            tint = onPrimaryContainerColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = schoolName,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "The smart way to manage your academic journey.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD5E3FC),
                        textAlign = TextAlign.Center
                    )
                }

                // Login Card Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Ambient decorative top-right glow
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 30.dp, y = (-30).dp)
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(primaryContainerColor.copy(alpha = 0.25f))
                        )

                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = textPrimary
                            )

                            Text(
                                text = "Please enter your details to sign in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Demo Credentials Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = loginEmail == "aryan@scholaria.com",
                                    onClick = {
                                        loginEmail = "aryan@scholaria.com"
                                        loginPassword = "123456"
                                        loginCollegeCode = "SCH-2024"
                                        onClearError()
                                        onClearRegistrationSuccess()
                                    },
                                    label = { Text("Student Demo", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = loginEmail == "teacher@school.com",
                                    onClick = {
                                        loginEmail = "teacher@school.com"
                                        loginPassword = "teacher123"
                                        loginCollegeCode = "SCH-2024"
                                        onClearError()
                                        onClearRegistrationSuccess()
                                    },
                                    label = { Text("Teacher Demo", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Registration Success Banner (Green)
                            AnimatedVisibility(
                                visible = registrationSuccessMessage != null,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF81C784))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = registrationSuccessMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = Color(0xFF1B5E20),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = onClearRegistrationSuccess,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Error Banner (Red)
                            AnimatedVisibility(
                                visible = authError != null,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = authError ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // If approval pending, provide quick button for testing/principal approve
                                        if (authError?.contains("Principal", ignoreCase = true) == true && loginEmail.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = {
                                                    onApproveUser(loginEmail.trim())
                                                    onLogin(loginEmail.trim(), loginPassword, loginCollegeCode.trim())
                                                },
                                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF005C55)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Principal Quick Approve & Login",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Email Field
                            Text(
                                text = "Email or Student ID",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = {
                                    loginEmail = it
                                    onClearError()
                                },
                                placeholder = { Text("e.g. student@school.edu", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = outlineVariant,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // College Code Field
                            Text(
                                text = "College Code",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = loginCollegeCode,
                                onValueChange = {
                                    loginCollegeCode = it
                                    onClearError()
                                },
                                placeholder = { Text("e.g. SCH-2024", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.School,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_college_code_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = outlineVariant,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Field Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary
                                )
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            authScreenState = AuthScreenState.FORGOT_PASSWORD
                                            onClearError()
                                            onClearResetMessages()
                                        }
                                        .testTag("login_forgot_password_button")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    loginPassword = it
                                    onClearError()
                                },
                                placeholder = { Text("••••••••", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isLoginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = outlineVariant,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Remember Me Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                )
                                Text(
                                    text = "Remember me for 30 days",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Login Button
                            Button(
                                onClick = { onLogin(loginEmail.trim(), loginPassword, loginCollegeCode.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_submit_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = outlineVariant)
                                Text(
                                    text = "  Or continue with  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = outlineVariant)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Social Logins (Google & Apple)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        loginEmail = "aryan@scholaria.com"
                                        loginPassword = "123456"
                                        loginCollegeCode = "SCH-2024"
                                        onLogin(loginEmail, loginPassword, loginCollegeCode)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = inputBgColor)
                                ) {
                                    GoogleIcon(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Google", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        loginEmail = "teacher@school.com"
                                        loginPassword = "teacher123"
                                        loginCollegeCode = "SCH-2024"
                                        onLogin(loginEmail, loginPassword, loginCollegeCode)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = inputBgColor)
                                ) {
                                    AppleIcon(
                                        color = textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Apple", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Register prompt link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Don't have an account? ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                                Text(
                                    text = "Register here",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    ),
                                    modifier = Modifier.clickable {
                                        authScreenState = AuthScreenState.REGISTER
                                        onClearError()
                                        onClearResetMessages()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Secure Login Portal • Powered by $schoolName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                // =========================================================
                // REGISTER SCREEN DESIGN (EXACT MATCH TO HTML SPEC)
                // =========================================================

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Top Accent Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(primaryColor)
                        )

                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Header with school icon
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(primaryContainerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = onPrimaryContainerColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (registerRoleTab == 0) "Create Student Account" else "Create Staff Account",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = textPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Join $schoolName to manage your classes and grades.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Role Switcher Tab
                            TabRow(
                                selectedTabIndex = registerRoleTab,
                                containerColor = inputBgColor,
                                contentColor = primaryColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, outlineVariant, RoundedCornerShape(10.dp))
                            ) {
                                Tab(
                                    selected = registerRoleTab == 0,
                                    onClick = { 
                                        registerRoleTab = 0
                                        onClearError()
                                    },
                                    text = { Text("Student", fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = registerRoleTab == 1,
                                    onClick = { 
                                        registerRoleTab = 1
                                        onClearError()
                                    },
                                    text = { Text("Teacher / Staff", fontWeight = FontWeight.Bold) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Register Error Banner
                            AnimatedVisibility(
                                visible = authError != null,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = authError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            if (registerRoleTab == 0) {
                                // --- STUDENT REGISTER FORM ---

                                // Full Name
                                Text(
                                    text = "Full Name",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regStudentName,
                                    onValueChange = { 
                                        regStudentName = it
                                        onClearError()
                                    },
                                    placeholder = { Text("Jane Doe", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Student ID / Email
                                Text(
                                    text = "Student ID / Email",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regStudentEmail,
                                    onValueChange = { 
                                        regStudentEmail = it
                                        onClearError()
                                    },
                                    placeholder = { Text("ID or school email", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Grade Level Dropdown
                                Text(
                                    text = "Grade Level",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = classDropdownExpanded,
                                    onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedGradeLevel,
                                        onValueChange = {},
                                        readOnly = true,
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = primaryColor) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = inputBgColor,
                                            unfocusedContainerColor = inputBgColor,
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = outlineVariant,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = classDropdownExpanded,
                                        onDismissRequest = { classDropdownExpanded = false }
                                    ) {
                                        listOf("9th Grade", "10th Grade", "11th Grade", "12th Grade").forEach { grade ->
                                            DropdownMenuItem(
                                                text = { Text(grade, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedGradeLevel = grade
                                                    classDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // College Code
                                Text(
                                    text = "College Code",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regStudentCollegeCode,
                                    onValueChange = {
                                        regStudentCollegeCode = it
                                        onClearError()
                                    },
                                    placeholder = { Text("e.g. SCH-2024", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("register_student_college_code_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Password Field
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regStudentPassword,
                                    onValueChange = { 
                                        regStudentPassword = it
                                        onClearError()
                                    },
                                    placeholder = { Text("••••••••", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = primaryColor) },
                                    trailingIcon = {
                                        IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isRegPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = Color.Gray
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("register_student_password_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val paidFee = regPaidFeeText.toIntOrNull() ?: 52000
                                        val generatedEmail = "student${(1000..9999).random()}@school.edu"
                                        val emailToUse = regStudentEmail.ifBlank { generatedEmail }
                                        val nameToUse = regStudentName.ifBlank { "New Student" }
                                        val passToUse = regStudentPassword.ifBlank { "123456" }
                                        val codeToUse = regStudentCollegeCode.ifBlank { "SCH-2024" }

                                        loginEmail = emailToUse
                                        loginPassword = passToUse
                                        loginCollegeCode = codeToUse

                                        onRegisterStudent(
                                            nameToUse,
                                            emailToUse,
                                            regStudentPhone.ifBlank { "+155501928" },
                                            passToUse,
                                            selectedGradeLevel,
                                            paidFee,
                                            regStudentRollNo,
                                            regStudentGuardian,
                                            currentClassStructure.annualFee,
                                            codeToUse
                                        )
                                        // Switch to login tab so the user sees the approval message
                                        authScreenState = AuthScreenState.LOGIN
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("register_student_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text(
                                        text = "Register Student",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                // --- TEACHER / STAFF REGISTER FORM ---

                                Text(
                                    text = "Full Name",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regTeacherName,
                                    onValueChange = { 
                                        regTeacherName = it
                                        onClearError()
                                    },
                                    placeholder = { Text("Professor Smith", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("register_teacher_name_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Email Address",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regTeacherEmail,
                                    onValueChange = { 
                                        regTeacherEmail = it
                                        onClearError()
                                    },
                                    placeholder = { Text("teacher@school.com", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth().testTag("register_teacher_email_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Subject / Qualification",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regTeacherSubject,
                                    onValueChange = { 
                                        regTeacherSubject = it
                                        onClearError()
                                    },
                                    placeholder = { Text("Mathematics", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // College Code Field
                                Text(
                                    text = "College Code",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regTeacherCollegeCode,
                                    onValueChange = {
                                        regTeacherCollegeCode = it
                                        onClearError()
                                    },
                                    placeholder = { Text("e.g. SCH-2024", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("register_teacher_college_code_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = regTeacherPassword,
                                    onValueChange = { 
                                        regTeacherPassword = it
                                        onClearError()
                                    },
                                    placeholder = { Text("••••••••", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = primaryColor) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("register_teacher_password_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBgColor,
                                        unfocusedContainerColor = inputBgColor,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = outlineVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val generatedTeacherEmail = "teacher${(1000..9999).random()}@school.com"
                                        val emailToUse = regTeacherEmail.ifBlank { generatedTeacherEmail }
                                        val nameToUse = regTeacherName.ifBlank { "Teacher Smith" }
                                        val passToUse = regTeacherPassword.ifBlank { "teacher123" }
                                        val codeToUse = regTeacherCollegeCode.ifBlank { "SCH-2024" }

                                        loginEmail = emailToUse
                                        loginPassword = passToUse
                                        loginCollegeCode = codeToUse

                                        onRegisterTeacher(
                                            nameToUse,
                                            emailToUse,
                                            regTeacherPhone.ifBlank { "+155501988" },
                                            passToUse,
                                            regTeacherSubject,
                                            regTeacherQualification,
                                            regTeacherExperience,
                                            regTeacherRole,
                                            codeToUse
                                        )
                                        // Switch to login tab so the user sees the approval message
                                        authScreenState = AuthScreenState.LOGIN
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("register_teacher_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text(
                                        text = "Register Staff",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Back to Login Prompt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Already have an account? ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                                Text(
                                    text = "Login",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    ),
                                    modifier = Modifier.clickable {
                                        authScreenState = AuthScreenState.LOGIN
                                        onClearError()
                                        onClearResetMessages()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Simple multi-color G icon drawing
        drawArc(
            color = Color(0xFFEA4335), // Red
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = Color(0xFFFBBC05), // Yellow
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = Color(0xFF34A853), // Green
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = Color(0xFF4285F4), // Blue
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = true
        )
    }
}

@Composable
fun AppleIcon(color: Color = Color.Black, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhoneIphone,
            contentDescription = "Apple",
            tint = color,
            modifier = Modifier.fillMaxSize()
        )
    }
}
