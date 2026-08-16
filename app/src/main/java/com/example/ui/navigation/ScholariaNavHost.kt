package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.ScholariaTopAppBar
import com.example.ui.screens.*
import com.example.ui.viewmodel.ScholariaViewModel

sealed class Screen(val route: String, val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Classes : Screen("classes", "Classes", Icons.Filled.School, Icons.Outlined.School)
    object Attendance : Screen("attendance", "Attendance", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable)
    object Notices : Screen("notices", "Notices", Icons.Filled.Campaign, Icons.Outlined.Campaign)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    object AiTutor : Screen("ai_tutor", "AI Tutor", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
}

@Composable
fun ScholariaApp(viewModel: ScholariaViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val classFeeStructures by viewModel.classFeeStructures.collectAsStateWithLifecycle()
    val authError by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val registrationSuccessMessage by viewModel.registrationSuccessMessage.collectAsStateWithLifecycle()
    val resetSuccessMessage by viewModel.resetPasswordMessage.collectAsStateWithLifecycle()
    val resetErrorMessage by viewModel.resetPasswordError.collectAsStateWithLifecycle()
    val isResetSending by viewModel.isResetSending.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()

    if (currentUser == null) {
        LoginRegisterScreen(
            schoolName = schoolName,
            classFeeStructures = classFeeStructures,
            authError = authError,
            registrationSuccessMessage = registrationSuccessMessage,
            resetSuccessMessage = resetSuccessMessage,
            resetErrorMessage = resetErrorMessage,
            isResetSending = isResetSending,
            onLogin = { email, password, collegeCode ->
                viewModel.login(email, password, collegeCode)
            },
            onRegisterUser = { name, email, password, role, collegeCode ->
                viewModel.signupUser(name, email, password, role, collegeCode)
            },
            onRegisterStudent = { name, email, phone, password, classGrade, paidFee, rollNo, guardian, annualFee, collegeCode ->
                viewModel.registerStudent(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password,
                    classGrade = classGrade,
                    paidFee = paidFee,
                    rollNo = rollNo,
                    guardian = guardian,
                    annualFee = annualFee,
                    collegeCode = collegeCode
                )
            },
            onRegisterTeacher = { name, email, phone, password, subject, qualification, experience, role, collegeCode ->
                viewModel.registerTeacher(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password,
                    subject = subject,
                    qualification = qualification,
                    experience = experience,
                    role = role,
                    collegeCode = collegeCode
                )
            },
            onSendResetLink = { collegeCode, email ->
                viewModel.sendPasswordReset(collegeCode, email)
            },
            onClearError = {
                viewModel.clearAuthError()
            },
            onClearRegistrationSuccess = {
                viewModel.clearRegistrationSuccessMessage()
            },
            onClearResetMessages = {
                viewModel.clearResetMessage()
            },
            onApproveUser = { email ->
                viewModel.approveUser(email)
            }
        )
        return
    }

    val navController = rememberNavController()

    val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()
    val classesList by viewModel.classes.collectAsStateWithLifecycle()
    val assignmentsList by viewModel.assignments.collectAsStateWithLifecycle()
    val noticesList by viewModel.notices.collectAsStateWithLifecycle()
    val topPerformers by viewModel.topPerformers.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGeneratingResponse by viewModel.isGeneratingResponse.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.notificationsCount.collectAsStateWithLifecycle()


    var showNotificationsDialog by remember { mutableStateOf(false) }

    var classesInitialTab by remember { mutableIntStateOf(0) }
    var profileInitialSection by remember { mutableIntStateOf(0) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Classes,
        Screen.Attendance,
        Screen.Notices,
        Screen.Profile
    )

    fun navigateTo(route: String) {
        if (route == Screen.Home.route) {
            val popped = navController.popBackStack(Screen.Home.route, inclusive = false)
            if (!popped) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentRoute != Screen.AiTutor.route) {
                ScholariaTopAppBar(
                    studentProfile = studentProfile,
                    schoolName = schoolName,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onNotificationsClick = { showNotificationsDialog = true },
                    onProfileClick = {
                        profileInitialSection = 0
                        navigateTo(Screen.Profile.route)
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("scholaria_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navigateTo(screen.route)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.activeIcon else screen.inactiveIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    studentProfile = studentProfile,
                    classesList = classesList,
                    assignmentsList = assignmentsList,
                    noticesList = noticesList,
                    topPerformers = topPerformers,
                    assignmentsCount = assignmentsList.count { !it.isCompleted },
                    onNavigateToClasses = { tab ->
                        classesInitialTab = tab
                        navigateTo(Screen.Classes.route)
                    },
                    onNavigateToProfile = { section ->
                        profileInitialSection = section
                        navigateTo(Screen.Profile.route)
                    },
                    onNavigateToAiTutor = {
                        navigateTo(Screen.AiTutor.route)
                    },
                    onNavigateToNotices = {
                        navigateTo(Screen.Notices.route)
                    },
                    onNavigateToAttendance = {
                        navigateTo(Screen.Attendance.route)
                    }
                )
            }

            composable(Screen.Classes.route) {
                ClassesScreen(
                    classesList = classesList,
                    assignmentsList = assignmentsList,
                    initialTab = classesInitialTab,
                    onToggleAssignment = { assignment ->
                        viewModel.toggleAssignmentCompleted(assignment)
                    },
                    onAddAssignment = { title, subject, dueDate, desc ->
                        viewModel.addAssignment(title, subject, dueDate, desc)
                    }
                )
            }

            composable(Screen.Attendance.route) {
                AttendanceScreen(
                    studentProfile = studentProfile,
                    onCheckIn = {
                        viewModel.markAttendanceCheckIn()
                    }
                )
            }

            composable(Screen.Notices.route) {
                NoticesScreen(noticesList = noticesList)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    studentProfile = studentProfile,
                    userRole = currentUser?.role ?: "Student",
                    schoolName = schoolName,
                    onUpdateSchoolName = { newName ->
                        viewModel.updateSchoolName(newName)
                    },
                    initialSection = profileInitialSection,
                    onSignOut = { viewModel.logout() }
                )
            }

            composable(Screen.AiTutor.route) {
                AiTutorScreen(
                    schoolName = schoolName,
                    chatMessages = chatMessages,
                    isGenerating = isGeneratingResponse,
                    onSendMessage = { text ->
                        viewModel.sendChatMessage(text)
                    },
                    onClearChat = {
                        viewModel.clearChat()
                    },
                    onNavigateBack = {
                        navigateTo(Screen.Home.route)
                    }
                )
            }
        }
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            onDismiss = { showNotificationsDialog = false },
            onClearAll = {
                viewModel.clearNotifications()
            }
        )
    }
}
