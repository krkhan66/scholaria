package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserAccount::class,
        StudentRecord::class,
        TeacherRecord::class,
        StudentProfile::class,
        ClassFeeStructure::class,
        ClassScheduleItem::class,
        AssignmentItem::class,
        NoticeItem::class,
        TopPerformerItem::class,
        ChatMessageItem::class,
        SchoolInfo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ScholariaDatabase : RoomDatabase() {

    abstract fun scholariaDao(): ScholariaDao

    companion object {
        @Volatile
        private var INSTANCE: ScholariaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): ScholariaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScholariaDatabase::class.java,
                    "scholaria_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): ScholariaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScholariaDatabase::class.java,
                    "scholaria_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(CoroutineScope(Dispatchers.IO)))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.scholariaDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: ScholariaDao) {
            // Default School Info
            dao.insertSchoolInfo(
                SchoolInfo(
                    id = 1,
                    schoolName = "Scholaria International Academy",
                    collegeCode = "SCH-2024",
                    contactEmail = "admissions@scholaria.edu",
                    contactPhone = "+1 (800) 555-0199",
                    address = "742 Evergreen Terrace, Academic District"
                )
            )

            // Default Student Profile
            dao.insertStudentProfile(
                StudentProfile(
                    id = 1,
                    name = "Alex Mercer",
                    grade = "Grade 10 - A",
                    rollNo = "SCH-1042",
                    attendancePercent = 95,
                    gpa = "3.92",
                    gradeTerm = "A+",
                    avatarUrl = ""
                )
            )

            // Default Demo Account
            dao.insertUser(
                UserAccount(
                    email = "student@scholaria.edu",
                    passwordHash = "password123",
                    name = "Alex Mercer",
                    role = "Student",
                    phone = "+1 555-0192",
                    studentId = 1
                )
            )

            // Class Fee Structures
            dao.insertFeeStructures(
                listOf(
                    ClassFeeStructure(1, "Grade 9 - Standard", "9", "A", "Dr. Robert Stone", 48000, "Includes STEM Lab & Digital Library"),
                    ClassFeeStructure(2, "Grade 10 - Honors", "10", "A", "Prof. Sarah Connor", 52000, "Includes AP Lab & AI Tools"),
                    ClassFeeStructure(3, "Grade 11 - Science Major", "11", "B", "Dr. Alan Turing", 58000, "Includes Physics & Robotics Lab"),
                    ClassFeeStructure(4, "Grade 12 - Advanced Placement", "12", "A", "Prof. Marie Curie", 62000, "Includes Research Lab & College Prep")
                )
            )

            // Schedule
            dao.insertClasses(
                listOf(
                    ClassScheduleItem(1, "Advanced Mathematics", "Dr. Stone", "08:30 AM - 09:45 AM", "Room 302", "Mon"),
                    ClassScheduleItem(2, "Quantum Physics", "Prof. Connor", "10:00 AM - 11:15 AM", "Lab B", "Mon"),
                    ClassScheduleItem(3, "World History", "Mr. Davis", "11:30 AM - 12:45 PM", "Room 105", "Mon"),
                    ClassScheduleItem(4, "Computer Science & AI", "Ms. Vance", "01:30 PM - 02:45 PM", "Lab 4", "Mon"),
                    ClassScheduleItem(5, "Literature & Rhetoric", "Mrs. Gable", "08:30 AM - 09:45 AM", "Room 204", "Tue"),
                    ClassScheduleItem(6, "Organic Chemistry", "Dr. Patel", "10:00 AM - 11:15 AM", "Chem Lab", "Tue"),
                    ClassScheduleItem(7, "Physical Education", "Coach Miller", "01:30 PM - 02:45 PM", "Gym A", "Tue")
                )
            )

            // Assignments / Homework
            dao.insertAssignments(
                listOf(
                    AssignmentItem(1, "Calculus Problem Set #4", "Advanced Mathematics", "Due Tomorrow, 11:59 PM", false, "Solve problems 1-15 in Chapter 4 on derivatives and integrals."),
                    AssignmentItem(2, "Thermodynamics Lab Report", "Quantum Physics", "Due in 3 Days", false, "Complete thermal expansion experiment analysis and graph plotting."),
                    AssignmentItem(3, "AI Ethics Essay", "Computer Science", "Due Friday", true, "Write a 1000-word essay on responsible AI in education."),
                    AssignmentItem(4, "Civil War Essay Outline", "World History", "Completed", true, "Submit preliminary bibliography and thesis statement.")
                )
            )

            // Notices
            dao.insertNotices(
                listOf(
                    NoticeItem(1, "Annual Science Fair 2026", "Oct 24, 2026", "Registrations are now open for inter-school STEM projects. Grand prize includes a research grant.", "Academic", "HIGH", "Science Dept"),
                    NoticeItem(2, "Midterm Examination Timetable", "Oct 20, 2026", "Midterm exams schedule for Grades 9-12 has been published. Download the hall ticket from profile.", "Exams", "URGENT", "Examination Board"),
                    NoticeItem(3, "Inter-House Sports Championship", "Oct 18, 2026", "Track & field trials begin this Wednesday afternoon at the school stadium.", "Sports", "NORMAL", "Sports Dept")
                )
            )

            // Top Performers
            dao.insertTopPerformers(
                listOf(
                    TopPerformerItem(1, "Alex Mercer", "Grade 10-A", "GPA 3.92 (98%)", ""),
                    TopPerformerItem(2, "Sophia Chen", "Grade 10-B", "GPA 3.90 (97%)", ""),
                    TopPerformerItem(3, "Marcus Vance", "Grade 10-A", "GPA 3.88 (96%)", ""),
                    TopPerformerItem(4, "Elena Rostova", "Grade 10-C", "GPA 3.85 (95%)", "")
                )
            )

            // Initial AI Tutor Message
            dao.insertChatMessage(
                ChatMessageItem(
                    1,
                    "ai_tutor",
                    "Hello! I am your Scholaria AI Study Tutor. Ask me any question about Mathematics, Science, History, or Literature!",
                    System.currentTimeMillis()
                )
            )
        }
    }
}
