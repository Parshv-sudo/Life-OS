package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionStatus
import com.example.model.TaskStatus
import com.example.ui.BossUiState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyDashboardScreen(
    state: BossUiState,
    onToggleChore: (choreId: String, completed: Boolean) -> Unit,
    onCompleteTask: (taskId: String, actualMins: Int) -> Unit,
    onAddTaskClick: () -> Unit
) {
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Node Status Banner
        item {
            Surface(
                color = SlateCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(EmeraldVerified)
                        )
                        Column {
                            Text(
                                text = "NODE: ANDROID-LOCAL",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Integrity Hash: ${state.nodeIntegrityHash} • Strict Read-Only Connectors",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Surface(
                        color = EmeraldVerified.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            color = EmeraldVerified,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Section 1: Fixed Daily Chores (FR-001, FR-002)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MORNING CHORE SEQUENCE (FR-001)",
                        color = ElectricSky,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Self-Reported Only (FR-002)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.chores.forEach { chore ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (chore.completed) SlateCard.copy(alpha = 0.4f) else SlateCard)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = if (chore.completed) EmeraldVerified.copy(alpha = 0.2f) else SlateBorder,
                                        shape = CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${chore.sequenceOrder}",
                                                color = if (chore.completed) EmeraldVerified else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = chore.title,
                                        color = if (chore.completed) TextMuted else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (chore.completed) FontWeight.Normal else FontWeight.Medium
                                    )
                                }
                                Checkbox(
                                    checked = chore.completed,
                                    onCheckedChange = { checked ->
                                        onToggleChore(chore.choreId, checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldVerified,
                                        uncheckedColor = SlateBorder,
                                        checkmarkColor = ObsidianBg
                                    ),
                                    modifier = Modifier.testTag("chore_checkbox_${chore.choreId}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Planned Commitments Timeline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S PLANNED COMMITMENTS",
                    color = ElectricSky,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${state.allTasks.count { it.status == TaskStatus.PLANNED }} Pending",
                    color = AmberGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (state.allTasks.isEmpty()) {
            item {
                Surface(
                    color = SlateSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No active tasks planned for today",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Accept an AI Brain proposal or tap '+' to schedule a session.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(state.allTasks) { task ->
                val isCompleted = task.status == TaskStatus.COMPLETED
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isCompleted) EmeraldVerified.copy(alpha = 0.3f) else SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = if (isCompleted) EmeraldVerified else ElectricSky,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = timeFormatter.format(Date(task.scheduledStart)),
                                    color = if (isCompleted) EmeraldVerified else ElectricSky,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    color = AmberGoldDark.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${task.plannedMinutes}m",
                                        color = AmberGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!isCompleted) {
                                Button(
                                    onClick = { onCompleteTask(task.id, task.plannedMinutes) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("complete_task_${task.id}")
                                ) {
                                    Text("Complete", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    color = EmeraldVerified.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "DONE (${task.actualMinutes ?: task.plannedMinutes}m)",
                                        color = EmeraldVerified,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = task.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Course: ${task.courseId.uppercase()} • Mode: ${task.schedulingMode}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Section 3: OS Execution Layer Actions (FR-071, FR-072)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ARMED OS ACTIONS (EXECUTION LAYER)",
                    color = ElectricSky,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (state.allActions.isEmpty()) {
                    Text(
                        text = "No OS actions armed. Approved proposals generate actions automatically.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    state.allActions.take(3).forEach { action ->
                        Surface(
                            color = SlateSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldVerified,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = action.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${action.executedOn} • ${action.type}",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Surface(
                                    color = ElectricSkyVariant.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ARMED",
                                        color = ElectricSky,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
