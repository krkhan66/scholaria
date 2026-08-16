package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentProfile

@Composable
fun AttendanceScreen(
    studentProfile: StudentProfile?,
    onCheckIn: () -> Unit
) {
    var showExcuseDialog by remember { mutableStateOf(false) }
    var checkedInToday by remember { mutableStateOf(false) }

    val subjectsAttendance = remember {
        listOf(
            SubjectAttendance("Mathematics", 98, 49, 50),
            SubjectAttendance("Physics", 92, 46, 50),
            SubjectAttendance("Chemistry", 96, 48, 50),
            SubjectAttendance("English Literature", 94, 47, 50),
            SubjectAttendance("History & Civics", 90, 45, 50),
            SubjectAttendance("Computer Science", 100, 50, 50)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attendance Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Overall Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${studentProfile?.attendancePercent ?: 95}%",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (studentProfile?.attendancePercent ?: 95) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("114", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Days Present", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("6", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Days Absent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!checkedInToday) {
                                    checkedInToday = true
                                    onCheckIn()
                                }
                            },
                            enabled = !checkedInToday,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_in_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (checkedInToday) "Checked In Today" else "Check In Today")
                        }

                        OutlinedButton(
                            onClick = { showExcuseDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Submit Leave")
                        }
                    }
                }
            }
        }

        // Subject Breakdown Title
        item {
            Text(
                text = "Subject-Wise Attendance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(subjectsAttendance) { subject ->
            SubjectAttendanceRow(subject)
        }

        // Monthly Calendar Preview Title
        item {
            Text(
                text = "August 2026 Attendance Calendar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            AttendanceCalendarGrid()
        }
    }

    if (showExcuseDialog) {
        LeaveApplicationDialog(onDismiss = { showExcuseDialog = false })
    }
}

@Composable
fun SubjectAttendanceRow(subject: SubjectAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${subject.percent}% (${subject.attended}/${subject.total})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (subject.percent >= 90) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            LinearProgressIndicator(
                progress = { subject.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (subject.percent >= 90) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceCalendarGrid() {
    val daysInMonth = (1..31).toList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayHeader ->
                    Text(text = dayHeader, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                daysInMonth.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { dayNum ->
                            val statusColor = when {
                                dayNum in listOf(2, 9, 16, 23, 30) -> Color(0xFFEAB308) // Sunday/Holiday
                                dayNum in listOf(5, 18) -> MaterialTheme.colorScheme.error // Absent
                                else -> MaterialTheme.colorScheme.primary // Present
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .border(1.dp, statusColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor
                                )
                            }
                        }
                        // Fill missing spaces in last week
                        if (week.size < 7) {
                            repeat(7 - week.size) {
                                Spacer(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaveApplicationDialog(onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (submitted) "Application Sent" else "Submit Leave Request", fontWeight = FontWeight.Bold) },
        text = {
            if (submitted) {
                Text("Your leave request has been submitted to Class Teacher Mr. Sharma for approval.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reason for absence / leave:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("e.g. Doctor's appointment, Fever, Family function...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (submitted) {
                Button(onClick = onDismiss) { Text("OK") }
            } else {
                Button(
                    onClick = { submitted = true },
                    enabled = reason.isNotBlank()
                ) {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            if (!submitted) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

data class SubjectAttendance(
    val name: String,
    val percent: Int,
    val attended: Int,
    val total: Int
)
