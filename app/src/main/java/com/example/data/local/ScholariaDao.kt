package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScholariaDao {

    // User Accounts
    @Query("SELECT * FROM user_accounts WHERE email = :email AND passwordHash = :password LIMIT 1")
    suspend fun authenticateUser(email: String, password: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun findUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("UPDATE user_accounts SET status = :status WHERE email = :email")
    suspend fun updateUserStatus(email: String, status: String)

    // Students
    @Query("SELECT * FROM students ORDER BY id DESC")
    fun getAllStudents(): Flow<List<StudentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentRecord>)

    // Teachers
    @Query("SELECT * FROM teachers ORDER BY id DESC")
    fun getAllTeachers(): Flow<List<TeacherRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<TeacherRecord>)

    // Student Profile
    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    fun getStudentProfile(): Flow<StudentProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentProfile(profile: StudentProfile)

    // Classes Schedule
    @Query("SELECT * FROM classes_schedule")
    fun getAllClasses(): Flow<List<ClassScheduleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassScheduleItem>)

    // Assignments / Homework
    @Query("SELECT * FROM assignments ORDER BY id DESC")
    fun getAllAssignments(): Flow<List<AssignmentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentItem>)

    @Update
    suspend fun updateAssignment(assignment: AssignmentItem)

    // Notices
    @Query("SELECT * FROM notices ORDER BY id DESC")
    fun getAllNotices(): Flow<List<NoticeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeItem>)

    // Top Performers
    @Query("SELECT * FROM top_performers")
    fun getTopPerformers(): Flow<List<TopPerformerItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopPerformers(performers: List<TopPerformerItem>)

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessageItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageItem): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // Class Fee Structures
    @Query("SELECT * FROM class_fee_structures")
    fun getClassFeeStructures(): Flow<List<ClassFeeStructure>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeStructures(structures: List<ClassFeeStructure>)

    // School Info
    @Query("SELECT * FROM school_info WHERE id = 1 LIMIT 1")
    fun getSchoolInfo(): Flow<SchoolInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolInfo(info: SchoolInfo)
}
