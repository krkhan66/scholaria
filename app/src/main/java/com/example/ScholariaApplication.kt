package com.example

import android.app.Application
import android.util.Log
import com.example.data.remote.FirebaseRealtimeDatabaseService
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class ScholariaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(FirebaseRealtimeDatabaseService.API_KEY)
                    .setApplicationId(FirebaseRealtimeDatabaseService.MOBILE_APP_ID)
                    .setDatabaseUrl(FirebaseRealtimeDatabaseService.DATABASE_URL)
                    .setProjectId(FirebaseRealtimeDatabaseService.PROJECT_ID)
                    .setStorageBucket(FirebaseRealtimeDatabaseService.STORAGE_BUCKET)
                    .setGcmSenderId(FirebaseRealtimeDatabaseService.MESSAGING_SENDER_ID)
                    .build()

                FirebaseApp.initializeApp(this, options)
                Log.i("ScholariaApp", "Firebase explicitly initialized with FirebaseOptions successfully.")
            } else {
                Log.i("ScholariaApp", "Firebase default app already initialized: ${FirebaseApp.getInstance().name}")
            }
        } catch (e: Exception) {
            Log.e("ScholariaApp", "Firebase initialization error: ${e.message}", e)
        }
    }
}
