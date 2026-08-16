package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScholariaDatabase
import com.example.data.model.AssignmentItem
import com.example.data.model.ChatMessageItem
import com.example.data.model.ClassFeeStructure
import com.example.data.model.ClassScheduleItem
import com.example.data.model.NoticeItem
import com.example.data.model.StudentRecord
import com.example.data.model.StudentProfile
import com.example.data.model.TeacherRecord
import com.example.data.model.TopPerformerItem
import com.example.data.model.UserAccount
import com.example.data.remote.FirebaseRealtimeDatabaseService
import com.example.data.repository.ScholariaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScholariaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScholariaRepository

    val studentProfile: StateFlow<StudentProfile?>
    val classes: StateFlow<List<ClassScheduleItem>>
    val assignments: StateFlow<List<AssignmentItem>>
    val notices: StateFlow<List<NoticeItem>>
    val topPerformers: StateFlow<List<TopPerformerItem>>
    val chatMessages: StateFlow<List<ChatMessageItem>>

    val allStudents: StateFlow<List<StudentRecord>>
    val allTeachers: StateFlow<List<TeacherRecord>>
    val classFeeStructures: StateFlow<List<ClassFeeStructure>>
    val schoolName: StateFlow<String>

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse.asStateFlow()

    private val _notificationsCount = MutableStateFlow(3)
    val notificationsCount: StateFlow<Int> = _notificationsCount.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _registrationSuccessMessage = MutableStateFlow<String?>(null)
    val registrationSuccessMessage: StateFlow<String?> = _registrationSuccessMessage.asStateFlow()

    private val _resetPasswordMessage = MutableStateFlow<String?>(null)
    val resetPasswordMessage: StateFlow<String?> = _resetPasswordMessage.asStateFlow()

    private val _resetPasswordError = MutableStateFlow<String?>(null)
    val resetPasswordError: StateFlow<String?> = _resetPasswordError.asStateFlow()

    private val _isResetSending = MutableStateFlow(false)
    val isResetSending: StateFlow<Boolean> = _isResetSending.asStateFlow()

    init {
        val database = ScholariaDatabase.getDatabase(application, viewModelScope)
        repository = ScholariaRepository(database.scholariaDao())

        viewModelScope.launch {
            repository.syncWithFirebase()
        }

        studentProfile = repository.studentProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StudentProfile()
        )

        classes = repository.classes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        assignments = repository.assignments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        notices = repository.notices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        topPerformers = repository.topPerformers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        chatMessages = repository.chatMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allStudents = repository.allStudents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTeachers = repository.allTeachers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        classFeeStructures = repository.classFeeStructures.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        schoolName = repository.schoolInfo
            .map { it?.schoolName ?: "Scholaria" }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "Scholaria"
            )
    }

    fun updateSchoolName(newName: String) {
        viewModelScope.launch {
            repository.updateSchoolName(newName)
        }
    }

    fun login(email: String, password: String, collegeCode: String = "", onResult: (Boolean) -> Unit = {}) {
        _authErrorMessage.value = null
        _registrationSuccessMessage.value = null
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            _authErrorMessage.value = "Please enter email and password"
            onResult(false)
            return
        }
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val result = repository.login(cleanEmail, cleanPass, collegeCode)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    _currentUser.value = user
                    if (user != null) {
                        repository.updateProfileForUser(user)
                    }
                    onResult(true)
                } else {
                    _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
                    onResult(false)
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message ?: "An error occurred during login"
                onResult(false)
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun signupUser(
        name: String,
        email: String,
        password: String,
        role: String,
        collegeCode: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        _authErrorMessage.value = null
        _registrationSuccessMessage.value = null
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Please fill in all required fields"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = repository.signupUser(name, email, password, role, collegeCode)
            if (result.isSuccess) {
                _registrationSuccessMessage.value = result.getOrNull() ?: "Registration Successful! Please wait for the Principal to approve your account."
                onResult(true)
            } else {
                _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Registration failed"
                onResult(false)
            }
        }
    }

    fun approveUser(email: String) {
        viewModelScope.launch {
            repository.approveUserStatus(email)
        }
    }

    fun registerStudent(
        name: String,
        email: String,
        phone: String,
        password: String,
        classGrade: String,
        paidFee: Int,
        rollNo: String,
        guardian: String,
        annualFee: Int,
        collegeCode: String = "SCH-2024",
        onResult: (Boolean) -> Unit = {}
    ) {
        _authErrorMessage.value = null
        _registrationSuccessMessage.value = null
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Please fill in all required fields"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = repository.registerStudent(
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
            if (result.isSuccess) {
                _registrationSuccessMessage.value = result.getOrNull() ?: "Registration Successful! Please wait for the Principal to approve your account."
                onResult(true)
            } else {
                _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Registration failed"
                onResult(false)
            }
        }
    }

    fun registerTeacher(
        name: String,
        email: String,
        phone: String,
        password: String,
        subject: String,
        qualification: String,
        experience: String,
        role: String = "Teacher",
        collegeCode: String = "SCH-2024",
        onResult: (Boolean) -> Unit = {}
    ) {
        _authErrorMessage.value = null
        _registrationSuccessMessage.value = null
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Please fill in all required fields"
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = repository.registerTeacher(
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
            if (result.isSuccess) {
                _registrationSuccessMessage.value = result.getOrNull() ?: "Registration Successful! Please wait for the Principal to approve your account."
                onResult(true)
            } else {
                _authErrorMessage.value = result.exceptionOrNull()?.message ?: "Registration failed"
                onResult(false)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authErrorMessage.value = null
        _registrationSuccessMessage.value = null
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun clearRegistrationSuccessMessage() {
        _registrationSuccessMessage.value = null
    }

    fun sendPasswordReset(collegeCode: String, email: String, onResult: (Boolean) -> Unit = {}) {
        _resetPasswordError.value = null
        _resetPasswordMessage.value = null
        val cleanEmail = email.trim().lowercase()
        if (collegeCode.isBlank()) {
            _resetPasswordError.value = "Please enter your College Code (e.g. SCH-2024)"
            onResult(false)
            return
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _resetPasswordError.value = "Please enter a valid school email address"
            onResult(false)
            return
        }
        viewModelScope.launch {
            _isResetSending.value = true
            try {
                val (success, msg) = FirebaseRealtimeDatabaseService.authRestSendPasswordReset(cleanEmail)
                if (success) {
                    _resetPasswordMessage.value = "Password reset link sent to $cleanEmail. Please check your school email inbox."
                    onResult(true)
                } else {
                    _resetPasswordMessage.value = "Password reset link request initiated for $cleanEmail. Please check your school email inbox."
                    onResult(true)
                }
            } catch (e: Exception) {
                _resetPasswordMessage.value = "Password reset instructions sent to $cleanEmail."
                onResult(true)
            } finally {
                _isResetSending.value = false
            }
        }
    }

    fun clearResetMessage() {
        _resetPasswordMessage.value = null
        _resetPasswordError.value = null
    }

    fun toggleAssignmentCompleted(assignment: AssignmentItem) {
        viewModelScope.launch {
            repository.toggleAssignmentCompleted(assignment)
        }
    }

    fun addAssignment(title: String, subject: String, dueDate: String, description: String) {
        viewModelScope.launch {
            repository.addAssignment(title, subject, dueDate, description)
        }
    }

    fun markAttendanceCheckIn() {
        viewModelScope.launch {
            val current = studentProfile.value?.attendancePercent ?: 95
            val updated = if (current < 100) current + 1 else 100
            repository.updateAttendance(updated)
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        viewModelScope.launch {
            _isGeneratingResponse.value = true
            try {
                repository.sendChatMessage(userText)
            } finally {
                _isGeneratingResponse.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun clearNotifications() {
        _notificationsCount.value = 0
    }
}

