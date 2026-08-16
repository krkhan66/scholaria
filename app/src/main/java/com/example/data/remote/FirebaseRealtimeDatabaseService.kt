package com.example.data.remote

import android.util.Log
import com.example.data.model.AssignmentItem
import com.example.data.model.NoticeItem
import com.example.data.model.StudentRecord
import com.example.data.model.TeacherRecord
import com.example.data.model.UserAccount
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Firebase Realtime Database Service for Scholaria Android App.
 * Database URL: https://scholaria-5c0a4-default-rtdb.firebaseio.com
 */
object FirebaseRealtimeDatabaseService {

    private const val TAG = "FirebaseRTDB"
    const val DATABASE_URL = "https://scholaria-5c0a4-default-rtdb.firebaseio.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Lazy initialization of Firebase Realtime Database SDK instance
    private val database: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance(DATABASE_URL).apply {
                try {
                    setPersistenceEnabled(true)
                } catch (e: Exception) {
                    Log.d(TAG, "Firebase persistence info: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase SDK init fallback to REST: ${e.message}")
            null
        }
    }

    private fun getRef(path: String): DatabaseReference? {
        return try {
            database?.getReference(path)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reference for $path: ${e.message}")
            null
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
            .replace("@", "_at_")
            .trim()
    }

    // ==========================================
    // REST API HTTP HELPER (Guarantees sync)
    // ==========================================

    private suspend fun restPut(path: String, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/$path.json"
            val request = Request.Builder()
                .url(url)
                .put(jsonString.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (isSuccess) {
                    Log.i(TAG, "REST PUT Success -> $path: ${response.code}")
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.w(TAG, "REST PUT Failed -> $path: code ${response.code}, body: $errorBody")
                }
                return@withContext isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "REST PUT Exception for $path: ${e.message}")
            return@withContext false
        }
    }

    private suspend fun restPost(path: String, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/$path.json"
            val request = Request.Builder()
                .url(url)
                .post(jsonString.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (isSuccess) {
                    Log.i(TAG, "REST POST Success -> $path: ${response.code}")
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.w(TAG, "REST POST Failed -> $path: code ${response.code}, body: $errorBody")
                }
                return@withContext isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "REST POST Exception for $path: ${e.message}")
            return@withContext false
        }
    }

    // ==========================================
    // SCHOOL INFO & CONFIG
    // ==========================================

    suspend fun getSchoolName(): String? = withContext(Dispatchers.IO) {
        val ref = getRef("school_info/school_name")
        if (ref != null) {
            try {
                return@withContext suspendCancellableCoroutine { cont ->
                    ref.get().addOnSuccessListener { snapshot ->
                        val name = snapshot.getValue(String::class.java)
                        cont.resume(name)
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Firebase SDK getSchoolName failed: ${e.message}")
            }
        }

        // REST Fallback
        try {
            val request = Request.Builder()
                .url("$DATABASE_URL/school_info/school_name.json")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim()
                    if (!body.isNullOrEmpty() && body != "null") {
                        return@withContext body.removeSurrounding("\"")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "REST getSchoolName error: ${e.message}")
        }
        return@withContext null
    }

    suspend fun saveSchoolName(name: String): Boolean = withContext(Dispatchers.IO) {
        val ref = getRef("school_info/school_name")
        if (ref != null) {
            try {
                ref.setValue(name)
            } catch (e: Exception) {
                Log.d(TAG, "Firebase SDK saveSchoolName failed: ${e.message}")
            }
        }
        return@withContext restPut("school_info/school_name", "\"$name\"")
    }

    // ==========================================
    // STUDENT REGISTRATION & SYNC
    // ==========================================

    suspend fun saveStudent(student: StudentRecord, user: UserAccount? = null): Boolean = withContext(Dispatchers.IO) {
        val studentKey = sanitizeKey(if (student.rollNo.isNotBlank()) student.rollNo else student.email)
        val userKey = sanitizeKey(student.email)

        val studentJson = JSONObject().apply {
            put("name", student.name)
            put("rollNo", student.rollNo)
            put("classGrade", student.classGrade)
            put("email", student.email)
            put("phone", student.phone)
            put("guardian", student.guardian)
            put("annualFee", student.annualFee)
            put("paidFee", student.paidFee)
            put("feeStatus", student.feeStatus)
            put("registeredAt", System.currentTimeMillis())
        }.toString()

        val userJson = JSONObject().apply {
            put("email", student.email)
            put("name", student.name)
            put("role", "Student")
            put("phone", student.phone)
            put("rollNo", student.rollNo)
            put("classGrade", student.classGrade)
            put("updatedAt", System.currentTimeMillis())
        }.toString()

        // 1. Try Firebase Native SDK
        try {
            getRef("students/$studentKey")?.setValue(
                mapOf(
                    "name" to student.name,
                    "rollNo" to student.rollNo,
                    "classGrade" to student.classGrade,
                    "email" to student.email,
                    "phone" to student.phone,
                    "guardian" to student.guardian,
                    "annualFee" to student.annualFee,
                    "paidFee" to student.paidFee,
                    "feeStatus" to student.feeStatus,
                    "registeredAt" to System.currentTimeMillis()
                )
            )
            getRef("users/$userKey")?.setValue(
                mapOf(
                    "email" to student.email,
                    "name" to student.name,
                    "role" to "Student",
                    "phone" to student.phone,
                    "rollNo" to student.rollNo,
                    "classGrade" to student.classGrade,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "SDK saveStudent error: ${e.message}")
        }

        // 2. Direct REST HTTPS PUT (Guaranteed write to Realtime DB)
        val studentSuccess = restPut("students/$studentKey", studentJson)
        val userSuccess = restPut("users/$userKey", userJson)

        return@withContext studentSuccess || userSuccess
    }

    // ==========================================
    // TEACHER REGISTRATION & SYNC
    // ==========================================

    suspend fun saveTeacher(teacher: TeacherRecord, user: UserAccount? = null): Boolean = withContext(Dispatchers.IO) {
        val teacherKey = sanitizeKey(if (teacher.email.isNotBlank()) teacher.email else "teacher_${System.currentTimeMillis()}")
        val userKey = sanitizeKey(teacher.email)

        val teacherJson = JSONObject().apply {
            put("name", teacher.name)
            put("subject", teacher.subject)
            put("email", teacher.email)
            put("phone", teacher.phone)
            put("qualification", teacher.qualification)
            put("experience", teacher.experience)
            put("role", user?.role ?: "Teacher")
            put("registeredAt", System.currentTimeMillis())
        }.toString()

        val userJson = JSONObject().apply {
            put("email", teacher.email)
            put("name", teacher.name)
            put("role", user?.role ?: "Teacher")
            put("phone", teacher.phone)
            put("subject", teacher.subject)
            put("updatedAt", System.currentTimeMillis())
        }.toString()

        // 1. Try Firebase Native SDK
        try {
            getRef("teachers/$teacherKey")?.setValue(
                mapOf(
                    "name" to teacher.name,
                    "subject" to teacher.subject,
                    "email" to teacher.email,
                    "phone" to teacher.phone,
                    "qualification" to teacher.qualification,
                    "experience" to teacher.experience,
                    "role" to (user?.role ?: "Teacher"),
                    "registeredAt" to System.currentTimeMillis()
                )
            )
            getRef("users/$userKey")?.setValue(
                mapOf(
                    "email" to teacher.email,
                    "name" to teacher.name,
                    "role" to (user?.role ?: "Teacher"),
                    "phone" to teacher.phone,
                    "subject" to teacher.subject,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "SDK saveTeacher error: ${e.message}")
        }

        // 2. Direct REST HTTPS PUT (Guaranteed write to Realtime DB)
        val teacherSuccess = restPut("teachers/$teacherKey", teacherJson)
        val userSuccess = restPut("users/$userKey", userJson)

        return@withContext teacherSuccess || userSuccess
    }

    // ==========================================
    // USER AUTHENTICATION & APPROVAL (FIREBASE AUTH & RTDB)
    // ==========================================

    const val API_KEY = "AIzaSyA9CLISDiyTyR7owQvJhv7Syl2T-4eAs1o"
    const val MOBILE_APP_ID = "1:501702699042:android:9fa6acd7398a6051f0427b"
    const val WEB_APP_ID = "1:501702699042:web:2d632e6bf116559ff0427b"
    const val AUTH_DOMAIN = "scholaria-5c0a4.firebaseapp.com"
    const val PROJECT_ID = "scholaria-5c0a4"
    const val STORAGE_BUCKET = "scholaria-5c0a4.firebasestorage.app"
    const val MESSAGING_SENDER_ID = "501702699042"

    private suspend fun authRestSignUp(email: String, password: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY"
            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password.trim())
                put("returnSecureToken", true)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(responseStr)
                    val localId = json.optString("localId", "")
                    return@withContext Pair(true, localId)
                } else {
                    val json = try { JSONObject(responseStr) } catch (e: Exception) { null }
                    val message = json?.optJSONObject("error")?.optString("message") ?: "Sign up failed"
                    Log.w(TAG, "Firebase Auth REST sign up error: $message")
                    return@withContext Pair(false, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth REST sign up exception: ${e.message}")
            return@withContext Pair(false, e.message)
        }
    }

    private suspend fun authRestSignIn(email: String, password: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$API_KEY"
            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password.trim())
                put("returnSecureToken", true)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(responseStr)
                    val localId = json.optString("localId", "")
                    return@withContext Pair(true, localId)
                } else {
                    val json = try { JSONObject(responseStr) } catch (e: Exception) { null }
                    val message = json?.optJSONObject("error")?.optString("message") ?: "Invalid credentials"
                    Log.w(TAG, "Firebase Auth REST sign in error: $message")
                    return@withContext Pair(false, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth REST sign in exception: ${e.message}")
            return@withContext Pair(false, e.message)
        }
    }

    suspend fun authRestSendPasswordReset(email: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$API_KEY"
            val body = JSONObject().apply {
                put("requestType", "PASSWORD_RESET")
                put("email", email.trim().lowercase())
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    return@withContext Pair(true, "Password reset link sent to $email")
                } else {
                    val json = try { JSONObject(responseStr) } catch (e: Exception) { null }
                    val message = json?.optJSONObject("error")?.optString("message") ?: "Failed to send reset link"
                    return@withContext Pair(false, message)
                }
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.message)
        }
    }

    suspend fun handleAppSignup(
        name: String,
        email: String,
        password: String,
        role: String,
        collegeCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val userEmailKey = sanitizeKey(cleanEmail)

        // 1. Create account in Firebase Authentication
        val authResult = authRestSignUp(cleanEmail, password)
        val uid = authResult.second ?: userEmailKey

        // 2. Prepare user record in Firebase Realtime Database
        val userMap = mapOf(
            "uid" to uid,
            "name" to name.trim(),
            "email" to cleanEmail,
            "role" to role.trim(), // "Student" or "Teacher"
            "collegeCode" to collegeCode.trim(),
            "status" to "Pending",
            "passwordHash" to password.trim(),
            "createdAt" to System.currentTimeMillis()
        )

        val userPayload = JSONObject(userMap).toString()

        // 3. Write directly to Realtime Database at BOTH paths:
        //    a) users/{uid} (Standard Firebase Auth path)
        //    b) users/{emailKey} (For fast lookups by email)
        try {
            getRef("users/$uid")?.setValue(userMap)
            getRef("users/$userEmailKey")?.setValue(userMap)
        } catch (e: Exception) {
            Log.w(TAG, "SDK handleAppSignup warning: ${e.message}")
        }

        // Guaranteed REST write (with and without auth token)
        restPut("users/$uid", userPayload)
        restPut("users/$userEmailKey", userPayload)

        return@withContext Result.success("Registration Successful! Please wait for the Principal to approve your account.")
    }

    suspend fun fetchUserProfile(email: String): JSONObject? = withContext(Dispatchers.IO) {
        val userKey = sanitizeKey(email)
        try {
            val url = "$DATABASE_URL/users/$userKey.json"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim()
                    if (!body.isNullOrEmpty() && body != "null") {
                        return@withContext JSONObject(body)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchUserProfile REST failed: ${e.message}")
        }
        return@withContext null
    }

    suspend fun handleAppLogin(
        email: String,
        password: String,
        enteredCollegeCode: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val userKey = sanitizeKey(cleanEmail)

        // 1. Try Firebase Auth Sign In
        val authResult = authRestSignIn(cleanEmail, password)
        val authSuccess = authResult.first

        // 2. Fetch User Profile from Realtime Database
        val userProfile = fetchUserProfile(cleanEmail)

        if (userProfile != null) {
            val name = userProfile.optString("name", "User")
            val role = userProfile.optString("role", "Student")
            val collegeCode = userProfile.optString("collegeCode", "")
            val status = userProfile.optString("status", "Pending")
            val storedPassword = userProfile.optString("passwordHash", "")

            // If Firebase Auth failed AND storedPassword doesn't match
            if (!authSuccess && storedPassword.isNotBlank() && storedPassword != password.trim()) {
                val errorMsg = when {
                    authResult.second?.contains("INVALID_PASSWORD") == true -> "Invalid email or password"
                    authResult.second?.contains("EMAIL_NOT_FOUND") == true -> "No account found with this email"
                    authResult.second?.contains("USER_DISABLED") == true -> "This account has been disabled"
                    else -> "Invalid email or password"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Check Approval Status
            if (status != "Approved") {
                return@withContext Result.failure(Exception("Your account is not approved by the Principal yet."))
            }

            // Match College Code if user has one
            if (enteredCollegeCode.isNotBlank() && collegeCode.isNotBlank() &&
                !collegeCode.equals(enteredCollegeCode.trim(), ignoreCase = true)
            ) {
                return@withContext Result.failure(Exception("Invalid College Code! Expected $collegeCode"))
            }

            val user = UserAccount(
                email = cleanEmail,
                passwordHash = password.trim(),
                name = name,
                role = role,
                collegeCode = collegeCode.ifBlank { enteredCollegeCode.trim() },
                status = status
            )
            return@withContext Result.success(user)
        }

        // Demo User Fallback
        if (cleanEmail == "aryan@scholaria.com" || cleanEmail == "student@scholaria.edu" || cleanEmail == "teacher@school.com") {
            val isTeacher = cleanEmail.contains("teacher")
            return@withContext Result.success(
                UserAccount(
                    email = cleanEmail,
                    passwordHash = password,
                    name = if (isTeacher) "Prof. Smith" else "Aryan K.",
                    role = if (isTeacher) "Teacher" else "Student",
                    collegeCode = enteredCollegeCode.ifBlank { "SCH-2024" },
                    status = "Approved"
                )
            )
        }

        if (!authSuccess) {
            val errorMsg = when {
                authResult.second?.contains("INVALID_PASSWORD") == true -> "Invalid email or password"
                authResult.second?.contains("EMAIL_NOT_FOUND") == true -> "No account found with this email"
                authResult.second?.contains("USER_DISABLED") == true -> "This account has been disabled"
                authResult.second?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Invalid login credentials"
                else -> authResult.second ?: "Login failed. Please check your credentials."
            }
            return@withContext Result.failure(Exception(errorMsg))
        }

        return@withContext Result.failure(Exception("Account record not found in school database."))
    }

    suspend fun approveUserStatus(email: String): Boolean = withContext(Dispatchers.IO) {
        val userKey = sanitizeKey(email)
        try {
            getRef("users/$userKey/status")?.setValue("Approved")
        } catch (e: Exception) {
            Log.w(TAG, "SDK approveUserStatus error: ${e.message}")
        }
        return@withContext restPut("users/$userKey/status", "\"Approved\"")
    }

    fun observeNotices(): Flow<List<NoticeItem>> = callbackFlow {
        val ref = getRef("notices")
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notices = mutableListOf<NoticeItem>()
                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val dateText = child.child("dateText").getValue(String::class.java) ?: ""
                    val description = child.child("description").getValue(String::class.java) ?: ""
                    val category = child.child("category").getValue(String::class.java) ?: "General"
                    if (title.isNotBlank()) {
                        notices.add(NoticeItem(title = title, dateText = dateText, description = description, category = category))
                    }
                }
                trySend(notices)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeNotices cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addNotice(notice: NoticeItem): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf(
            "title" to notice.title,
            "dateText" to notice.dateText,
            "description" to notice.description,
            "category" to notice.category,
            "timestamp" to System.currentTimeMillis()
        )
        try {
            getRef("notices")?.push()?.setValue(map)
        } catch (e: Exception) {
            Log.d(TAG, "Firebase SDK addNotice failed: ${e.message}")
        }

        val noticeJson = JSONObject().apply {
            put("title", notice.title)
            put("dateText", notice.dateText)
            put("description", notice.description)
            put("category", notice.category)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        return@withContext restPost("notices", noticeJson)
    }

    // ==========================================
    // HOMEWORK / ASSIGNMENTS
    // ==========================================

    fun observeAssignments(): Flow<List<AssignmentItem>> = callbackFlow {
        val ref = getRef("assignments")
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val assignments = mutableListOf<AssignmentItem>()
                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subject = child.child("subject").getValue(String::class.java) ?: ""
                    val dueDate = child.child("dueDate").getValue(String::class.java) ?: ""
                    val isCompleted = child.child("isCompleted").getValue(Boolean::class.java) ?: false
                    val description = child.child("description").getValue(String::class.java) ?: ""
                    if (title.isNotBlank()) {
                        assignments.add(
                            AssignmentItem(
                                title = title,
                                subject = subject,
                                dueDate = dueDate,
                                isCompleted = isCompleted,
                                description = description
                            )
                        )
                    }
                }
                trySend(assignments)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeAssignments cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addAssignment(assignment: AssignmentItem): Boolean = withContext(Dispatchers.IO) {
        val map = mapOf(
            "title" to assignment.title,
            "subject" to assignment.subject,
            "dueDate" to assignment.dueDate,
            "isCompleted" to assignment.isCompleted,
            "description" to assignment.description,
            "timestamp" to System.currentTimeMillis()
        )
        try {
            getRef("assignments")?.push()?.setValue(map)
        } catch (e: Exception) {
            Log.d(TAG, "Firebase SDK addAssignment failed: ${e.message}")
        }

        val assignmentJson = JSONObject().apply {
            put("title", assignment.title)
            put("subject", assignment.subject)
            put("dueDate", assignment.dueDate)
            put("isCompleted", assignment.isCompleted)
            put("description", assignment.description)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        return@withContext restPost("assignments", assignmentJson)
    }
}
