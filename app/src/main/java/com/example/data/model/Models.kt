package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    STUDENT,
    TEACHER,
    STAFF,
    PARENT
}

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String = "",
    val passwordHash: String = "",
    val name: String = "",
    val role: String = "Student",
    val collegeCode: String = "SCH-2024",
    val status: String = "Approved",
    val phone: String = "",
    val studentId: Long? = null,
    val teacherId: Long? = null
)

@Entity(tableName = "students")
data class StudentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val rollNo: String = "",
    val classGrade: String = "Grade 10 - A",
    val email: String = "",
    val phone: String = "",
    val guardian: String = "",
    val annualFee: Int = 52000,
    val paidFee: Int = 0,
    val feeStatus: String = "Pending"
)

@Entity(tableName = "teachers")
data class TeacherRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val subject: String = "",
    val email: String = "",
    val phone: String = "",
    val qualification: String = "",
    val experience: String = ""
)

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex Mercer",
    val grade: String = "Grade 10 - A",
    val rollNo: String = "SCH-1042",
    val attendancePercent: Int = 95,
    val gpa: String = "3.92",
    val gradeTerm: String = "A+",
    val avatarUrl: String = ""
)

@Entity(tableName = "class_fee_structures")
data class ClassFeeStructure(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String = "",
    val grade: String = "",
    val section: String = "",
    val teacherName: String = "",
    val annualFee: Int = 52000,
    val feeFeatures: String = ""
)

@Entity(tableName = "classes_schedule")
data class ClassScheduleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String = "",
    val teacher: String = "",
    val timeSlot: String = "",
    val room: String = "",
    val dayOfWeek: String = "Mon"
)

@Entity(tableName = "assignments")
data class AssignmentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "notices")
data class NoticeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val dateText: String = "",
    val description: String = "",
    val category: String = "General",
    val priority: String = "NORMAL",
    val author: String = "Principal's Office"
)

@Entity(tableName = "top_performers")
data class TopPerformerItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val grade: String = "",
    val scoreText: String = "",
    val avatarUrl: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessageItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String = "user", // "user" or "ai_tutor"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "school_info")
data class SchoolInfo(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "Scholaria International Academy",
    val collegeCode: String = "SCH-2024",
    val contactEmail: String = "admin@scholaria.edu",
    val contactPhone: String = "+1 (800) 555-0199",
    val address: String = "742 Evergreen Terrace, Academic District"
)
