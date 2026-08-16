package com.example.data.repository

import com.example.data.local.ScholariaDao
import com.example.data.model.AssignmentItem
import com.example.data.model.ChatMessageItem
import com.example.data.model.ClassFeeStructure
import com.example.data.model.ClassScheduleItem
import com.example.data.model.NoticeItem
import com.example.data.model.SchoolInfo
import com.example.data.model.StudentRecord
import com.example.data.model.StudentProfile
import com.example.data.model.TeacherRecord
import com.example.data.model.TopPerformerItem
import com.example.data.model.UserAccount
import com.example.data.remote.FirebaseRealtimeDatabaseService
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow

class ScholariaRepository(private val dao: ScholariaDao) {

    val studentProfile: Flow<StudentProfile?> = dao.getStudentProfile()
    val classes: Flow<List<ClassScheduleItem>> = dao.getAllClasses()
    val assignments: Flow<List<AssignmentItem>> = dao.getAllAssignments()
    val notices: Flow<List<NoticeItem>> = dao.getAllNotices()
    val topPerformers: Flow<List<TopPerformerItem>> = dao.getTopPerformers()
    val chatMessages: Flow<List<ChatMessageItem>> = dao.getChatMessages()

    val allStudents: Flow<List<StudentRecord>> = dao.getAllStudents()
    val allTeachers: Flow<List<TeacherRecord>> = dao.getAllTeachers()
    val classFeeStructures: Flow<List<ClassFeeStructure>> = dao.getClassFeeStructures()
    val schoolInfo: Flow<SchoolInfo?> = dao.getSchoolInfo()

    suspend fun syncWithFirebase() {
        try {
            // Sync School Name from Firebase Realtime Database
            val firebaseSchoolName = FirebaseRealtimeDatabaseService.getSchoolName()
            if (!firebaseSchoolName.isNullOrBlank()) {
                dao.insertSchoolInfo(SchoolInfo(id = 1, schoolName = firebaseSchoolName.trim()))
            }
        } catch (e: Exception) {
            // Silently ignore sync errors on offline
        }
    }

    suspend fun updateSchoolName(newName: String) {
        if (newName.isNotBlank()) {
            val cleanName = newName.trim()
            dao.insertSchoolInfo(SchoolInfo(id = 1, schoolName = cleanName))
            try {
                FirebaseRealtimeDatabaseService.saveSchoolName(cleanName)
            } catch (e: Exception) {
                // Keep local state saved if offline
            }
        }
    }

    suspend fun updateProfileForUser(user: UserAccount) {
        val isTeacher = user.role.equals("Teacher", ignoreCase = true)
        val grade = if (isTeacher) "Faculty Staff" else "Grade 10 - A"
        val rollNo = if (isTeacher) "FAC-881" else "SCH-1042"
        dao.insertStudentProfile(
            StudentProfile(
                id = 1,
                name = user.name.ifBlank { "User" },
                grade = grade,
                rollNo = rollNo,
                attendancePercent = 96,
                gpa = if (isTeacher) "N/A" else "3.92",
                gradeTerm = if (isTeacher) "Teacher" else "A+"
            )
        )
    }

    suspend fun login(email: String, password: String, collegeCode: String = ""): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()

        // 1. Try Firebase Realtime Database
        try {
            val remoteResult = FirebaseRealtimeDatabaseService.handleAppLogin(cleanEmail, cleanPass, collegeCode)
            if (remoteResult.isSuccess) {
                val remoteUser = remoteResult.getOrThrow()
                // Cache user locally
                val localUser = dao.findUserByEmail(cleanEmail)
                if (localUser != null) {
                    dao.updateUser(localUser.copy(
                        name = remoteUser.name,
                        role = remoteUser.role,
                        collegeCode = remoteUser.collegeCode,
                        status = remoteUser.status,
                        passwordHash = cleanPass
                    ))
                } else {
                    dao.insertUser(remoteUser)
                }
                return Result.success(remoteUser)
            } else {
                val errorMsg = remoteResult.exceptionOrNull()?.message ?: ""
                // If it's a specific approval or college code error, propagate it
                if (errorMsg.contains("approved", ignoreCase = true) || errorMsg.contains("College Code", ignoreCase = true)) {
                    return Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            // Proceed to local check
        }

        // 2. Check Local Database
        val localUser = dao.findUserByEmail(cleanEmail)
        if (localUser != null) {
            if (localUser.passwordHash.isNotBlank() && localUser.passwordHash != cleanPass) {
                return Result.failure(Exception("Invalid email or password"))
            }

            // Check Status
            if (localUser.status != "Approved") {
                return Result.failure(Exception("Your account is not approved by the Principal yet."))
            }

            // Check College Code
            if (collegeCode.isNotBlank() && localUser.collegeCode.isNotBlank() &&
                !localUser.collegeCode.equals(collegeCode.trim(), ignoreCase = true)
            ) {
                return Result.failure(Exception("Invalid College Code!"))
            }

            return Result.success(localUser)
        }

        return Result.failure(Exception("Invalid email or password or account not found."))
    }

    suspend fun signupUser(
        name: String,
        email: String,
        password: String,
        role: String,
        collegeCode: String
    ): Result<String> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        val cleanPass = password.trim()
        val cleanRole = role.trim()
        val cleanCollegeCode = collegeCode.trim().ifBlank { "SCH-2024" }

        val existing = dao.findUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        // Save into local Room DB with status 'Pending'
        val userAccount = UserAccount(
            email = cleanEmail,
            passwordHash = cleanPass,
            name = cleanName,
            role = cleanRole,
            collegeCode = cleanCollegeCode,
            status = "Pending"
        )
        dao.insertUser(userAccount)

        if (cleanRole.equals("Teacher", ignoreCase = true)) {
            val teacherRecord = TeacherRecord(
                name = cleanName,
                subject = "General Faculty",
                email = cleanEmail,
                qualification = "B.Ed",
                experience = "3 years"
            )
            dao.insertTeacher(teacherRecord)
        } else {
            val studentRecord = StudentRecord(
                name = cleanName,
                rollNo = (1000 + (10..99).random()).toString(),
                classGrade = "Grade 10 - A",
                email = cleanEmail,
                guardian = "Parent/Guardian",
                annualFee = 52000,
                paidFee = 0,
                feeStatus = "Pending"
            )
            dao.insertStudent(studentRecord)
        }

        // 2. Save in Firebase Database with status 'Pending'
        try {
            FirebaseRealtimeDatabaseService.handleAppSignup(
                name = cleanName,
                email = cleanEmail,
                password = cleanPass,
                role = cleanRole,
                collegeCode = cleanCollegeCode
            )
        } catch (e: Exception) {
            // Ignore offline network issues
        }

        return Result.success("Registration Successful! Please wait for the Principal to approve your account.")
    }

    suspend fun approveUserStatus(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        dao.updateUserStatus(cleanEmail, "Approved")
        return try {
            FirebaseRealtimeDatabaseService.approveUserStatus(cleanEmail)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun registerStudent(
        name: String,
        email: String,
        phone: String,
        password: String,
        classGrade: String,
        paidFee: Int,
        rollNo: String,
        guardian: String,
        annualFee: Int,
        collegeCode: String = "SCH-2024"
    ): Result<String> {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.findUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        val studentRecord = StudentRecord(
            name = name,
            rollNo = if (rollNo.isBlank()) (1000 + (10..99).random()).toString() else rollNo,
            classGrade = classGrade,
            email = cleanEmail,
            phone = phone,
            guardian = guardian,
            annualFee = annualFee,
            paidFee = paidFee,
            feeStatus = if (paidFee >= annualFee) "Paid" else if (paidFee > 0) "Partial" else "Pending"
        )
        val studentId = dao.insertStudent(studentRecord)

        val userAccount = UserAccount(
            email = cleanEmail,
            passwordHash = password,
            name = name,
            role = "Student",
            collegeCode = collegeCode,
            status = "Pending",
            phone = phone,
            studentId = studentId
        )
        dao.insertUser(userAccount)

        try {
            FirebaseRealtimeDatabaseService.handleAppSignup(
                name = name,
                email = cleanEmail,
                password = password,
                role = "Student",
                collegeCode = collegeCode
            )
            FirebaseRealtimeDatabaseService.saveStudent(studentRecord, userAccount)
        } catch (e: Exception) {
            // Ignore offline network error
        }

        return Result.success("Registration Successful! Please wait for the Principal to approve your account.")
    }

    suspend fun registerTeacher(
        name: String,
        email: String,
        phone: String,
        password: String,
        subject: String,
        qualification: String,
        experience: String,
        role: String = "Teacher",
        collegeCode: String = "SCH-2024"
    ): Result<String> {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.findUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        val teacherRecord = TeacherRecord(
            name = name,
            subject = subject,
            email = cleanEmail,
            phone = phone,
            qualification = qualification,
            experience = experience
        )
        val teacherId = dao.insertTeacher(teacherRecord)

        val userAccount = UserAccount(
            email = cleanEmail,
            passwordHash = password,
            name = name,
            role = role,
            collegeCode = collegeCode,
            status = "Pending",
            phone = phone,
            teacherId = teacherId
        )
        dao.insertUser(userAccount)

        try {
            FirebaseRealtimeDatabaseService.handleAppSignup(
                name = name,
                email = cleanEmail,
                password = password,
                role = role,
                collegeCode = collegeCode
            )
            FirebaseRealtimeDatabaseService.saveTeacher(teacherRecord, userAccount)
        } catch (e: Exception) {
            // Ignore offline network error
        }

        return Result.success("Registration Successful! Please wait for the Principal to approve your account.")
    }

    suspend fun toggleAssignmentCompleted(assignment: AssignmentItem) {
        dao.updateAssignment(assignment.copy(isCompleted = !assignment.isCompleted))
    }

    suspend fun addAssignment(title: String, subject: String, dueDate: String, description: String) {
        val item = AssignmentItem(
            title = title,
            subject = subject,
            dueDate = dueDate,
            description = description,
            isCompleted = false
        )
        dao.insertAssignment(item)
        try {
            FirebaseRealtimeDatabaseService.addAssignment(item)
        } catch (e: Exception) {
            // Ignore offline error
        }
    }

    suspend fun updateAttendance(newAttendance: Int) {
        dao.insertStudentProfile(
            StudentProfile(
                id = 1,
                name = "Aryan K.",
                grade = "Grade 10-A",
                rollNo = "1024",
                attendancePercent = newAttendance,
                gpa = "3.9",
                gradeTerm = "A+"
            )
        )
    }

    suspend fun sendChatMessage(userText: String) {
        // Save user message
        dao.insertChatMessage(ChatMessageItem(sender = "user", text = userText))

        // Get AI response
        val aiResponse = GeminiService.getTutorResponse(userText)

        // Save AI response
        dao.insertChatMessage(ChatMessageItem(sender = "ai_tutor", text = aiResponse))
    }

    suspend fun clearChat() {
        dao.clearChatMessages()
        dao.insertChatMessage(
            ChatMessageItem(
                sender = "ai_tutor",
                text = "Chat cleared! How can I assist with your studies today?"
            )
        )
    }
}

