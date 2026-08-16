package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentProfile
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    studentProfile: StudentProfile?,
    userRole: String = "Student",
    schoolName: String = "Scholaria",
    onUpdateSchoolName: (String) -> Unit = {},
    initialSection: Int = 0,
    onSignOut: () -> Unit
) {
    var showEditSchoolDialog by remember { mutableStateOf(false) }
    var editedSchoolName by remember(schoolName) { mutableStateOf(schoolName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("profile_screen_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (studentProfile?.name?.take(1) ?: "S").uppercase(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = studentProfile?.name ?: "Student User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = userRole,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryFixed.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = schoolName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTeal
                    )
                }
            }
        }

        // Academic & Identity Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Academic Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                ProfileInfoRow(
                    icon = Icons.Default.School,
                    label = "Grade & Section",
                    value = studentProfile?.grade ?: "Grade 10 - A"
                )

                ProfileInfoRow(
                    icon = Icons.Default.Badge,
                    label = "Roll Number",
                    value = studentProfile?.rollNo ?: "SCH-1042"
                )

                ProfileInfoRow(
                    icon = Icons.Default.EventAvailable,
                    label = "Overall Attendance",
                    value = "${studentProfile?.attendancePercent ?: 95}%"
                )

                ProfileInfoRow(
                    icon = Icons.Default.Grade,
                    label = "Cumulative GPA",
                    value = "${studentProfile?.gpa ?: "3.92"} (${studentProfile?.gradeTerm ?: "A+"})"
                )
            }
        }

        // Institution Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Institution Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("School / College Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(schoolName, fontSize = 12.sp, color = TextSecondary)
                    }
                    FilledTonalButton(
                        onClick = { showEditSchoolDialog = true },
                        modifier = Modifier.testTag("edit_school_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        }

        // Logout Button
        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showEditSchoolDialog) {
        AlertDialog(
            onDismissRequest = { showEditSchoolDialog = false },
            title = { Text("Update School Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedSchoolName,
                    onValueChange = { editedSchoolName = it },
                    label = { Text("School Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedSchoolName.isNotBlank()) {
                            onUpdateSchoolName(editedSchoolName.trim())
                        }
                        showEditSchoolDialog = false
                    },
                    modifier = Modifier.testTag("save_school_name_button")
                ) {
                    Text("Save & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSchoolDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}
