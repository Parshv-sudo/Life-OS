package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AssignmentEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Blocking Commitment Prompt (FR-051, FR-052, FR-053, ADR-001)
 * Enforces "Force planning, not execution":
 * - Non-dismissible without committing a scheduled time.
 * - Does NOT force execution right now.
 * - Does NOT lock the device outside the app (scoped to app planning layer).
 */
@Composable
fun CommitmentEnforcementDialog(
    assignment: AssignmentEntity,
    onCommitPlan: (scheduledStartTime: Long, durationMinutes: Int, taskTitle: String) -> Unit
) {
    val now = System.currentTimeMillis()
    val timeFormatter = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
    val dueStr = timeFormatter.format(Date(assignment.dueAt))

    // Preset options
    var selectedOffsetHours by remember { mutableStateOf(2L) }
    var selectedDurationMinutes by remember { mutableStateOf(60) }

    val committedTime = now + (selectedOffsetHours * 3600 * 1000L)
    val committedTimeStr = SimpleDateFormat("h:mm a (today)", Locale.getDefault()).format(Date(committedTime))

    // Non-dismissible dialog (FR-052)
    Dialog(
        onDismissRequest = { /* Cannot be dismissed without committing a plan */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg.copy(alpha = 0.94f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CrimsonUrgent.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Header with Urgent Warning
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = CrimsonUrgent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Urgent Commitment Alert",
                                tint = CrimsonUrgent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "COMMITMENT CHECK REQUIRED",
                                color = CrimsonUrgent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Unplanned Urgent Obligation",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Assignment Title and Due Info
                    Surface(
                        color = SlateCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = assignment.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Due: $dueStr (Within 36h threshold)",
                                    color = AmberGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Doctrine Explanation (Principle 2)
                    Surface(
                        color = ElectricSky.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricSky.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ElectricSky,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = "Principle 2: Force planning, never execution. You are not forced to work now, but you must commit to WHEN you will execute this.",
                                color = ElectricSky,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Time Slot Presets
                    Text(
                        text = "SELECT COMMITTED START TIME:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1L to "In 1 hr",
                            3L to "In 3 hrs",
                            6L to "In 6 hrs",
                            10L to "Tonight"
                        ).forEach { (offset, label) ->
                            val isSelected = selectedOffsetHours == offset
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedOffsetHours = offset },
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Duration Selection
                    Text(
                        text = "COMMITTED WORK DURATION:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(45 to "45 min", 60 to "1 hour", 90 to "1.5 hrs", 120 to "2 hrs").forEach { (duration, label) ->
                            val isSelected = selectedDurationMinutes == duration
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDurationMinutes = duration },
                                label = { Text(label, fontSize = 12.sp) },
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Commitment Action Button
                    Button(
                        onClick = {
                            onCommitPlan(committedTime, selectedDurationMinutes, "Study: ${assignment.title}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("commit_plan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricSky),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = ObsidianBg)
                            Text(
                                text = "COMMIT PLAN FOR $committedTimeStr",
                                color = ObsidianBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
