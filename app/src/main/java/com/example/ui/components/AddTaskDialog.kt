package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CourseEntity
import com.example.model.SchedulingMode
import com.example.ui.theme.*

@Composable
fun AddTaskDialog(
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onSaveTask: (title: String, courseId: String, mode: SchedulingMode, explicitStart: Long, duration: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "cs301") }
    var mode by remember { mutableStateOf(SchedulingMode.FLEXIBLE) }
    var durationMinutes by remember { mutableStateOf(60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        title = {
            Text(
                text = "New Planned Task",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description") },
                    placeholder = { Text("e.g. Study DAA Dynamic Programming") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricSky,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Mode Selection (FR-060)
                Text(
                    text = "SCHEDULING MODE (FR-060):",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        SchedulingMode.FLEXIBLE to "Flexible",
                        SchedulingMode.CONSTRAINED to "Windowed",
                        SchedulingMode.EXPLICIT to "Explicit"
                    ).forEach { (m, label) ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricSkyVariant,
                                selectedLabelColor = TextPrimary,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = when (mode) {
                        SchedulingMode.FLEXIBLE -> "AI Brain automatically locates the optimal non-conflicting calendar window."
                        SchedulingMode.CONSTRAINED -> "AI picks within your preferred afternoon/evening study block."
                        SchedulingMode.EXPLICIT -> "Exact scheduled start time committed immediately."
                    },
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                // Duration Selection
                Text(
                    text = "PLANNED DURATION:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins },
                            label = { Text("${mins}m", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGoldDark,
                                selectedLabelColor = TextPrimary,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val startTime = System.currentTimeMillis() + (2 * 3600 * 1000L)
                        onSaveTask(title, selectedCourseId, mode, startTime, durationMinutes)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricSky),
                modifier = Modifier.testTag("submit_task_button")
            ) {
                Text("Schedule & Arm", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
