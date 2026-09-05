package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brain.AiBrain
import com.example.model.AssignmentStatus
import com.example.model.ProposalEntity
import com.example.model.UrgencyLevel
import com.example.ui.BossUiState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommitmentsScreen(
    state: BossUiState,
    onAcceptProposal: (ProposalEntity) -> Unit,
    onRejectProposal: (ProposalEntity, reason: String) -> Unit,
    onModifyProposal: (ProposalEntity, newStart: Long, newDuration: Int) -> Unit
) {
    val timeFormat = SimpleDateFormat("EEE, h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    var modifyingProposal by remember { mutableStateOf<ProposalEntity?>(null) }
    var modifyDuration by remember { mutableStateOf(60) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Pending AI Brain Proposals (Principle 3: Propose, don't decide)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI BRAIN PROPOSALS (PROPOSE, DON'T DECIDE)",
                        color = ElectricSky,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${state.pendingProposals.size} Pending",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.pendingProposals.isEmpty()) {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = EmeraldVerified,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All AI Proposals Reviewed",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Brain will generate slots at next checkpoint or as new assignments appear.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        items(state.pendingProposals) { proposal ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElectricSky.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ElectricSkyVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = ElectricSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "DETERMINISTIC PROPOSAL",
                                    color = ElectricSky,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "${proposal.suggestedMinutes} min slot",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = proposal.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Suggested Slot: ${timeFormat.format(Date(proposal.suggestedStart))}",
                        color = ElectricSky,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transparent Reasoning Banner (FR-063)
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "TRANSPARENT REASONING (FR-063):",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = proposal.reasoning,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Accept, Modify, Reject Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAcceptProposal(proposal) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricSky),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("accept_proposal_${proposal.id}")
                        ) {
                            Text("Accept Plan", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                modifyingProposal = proposal
                                modifyDuration = proposal.suggestedMinutes
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modify_proposal_${proposal.id}")
                        ) {
                            Text("Modify", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onRejectProposal(proposal, "Time slot not optimal") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonUrgent),
                            border = BorderStroke(1.dp, CrimsonUrgent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("reject_proposal_${proposal.id}")
                        ) {
                            Text("Reject", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 2: All Obligations by Urgency Tier
        item {
            Text(
                text = "COURSE ASSIGNMENTS (ACADEMIC OBLIGATIONS)",
                color = ElectricSky,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        items(state.assignments) { assignment ->
            val urgency = AiBrain.classifyUrgency(assignment.dueAt)
            val urgencyColor = when (urgency) {
                UrgencyLevel.URGENT -> CrimsonUrgent
                UrgencyLevel.IMPORTANT -> AmberGold
                UrgencyLevel.NORMAL -> EmeraldVerified
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = urgencyColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = urgency.name,
                                color = urgencyColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "Due: ${dateFormat.format(Date(assignment.dueAt))}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = assignment.title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = assignment.description,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SlateCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Course: ${assignment.courseId.uppercase()}",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = SlateCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Status: ${assignment.userStatus}",
                                color = if (assignment.userStatus == AssignmentStatus.PLANNED) EmeraldVerified else TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modify Modal
    modifyingProposal?.let { proposal ->
        AlertDialog(
            onDismissRequest = { modifyingProposal = null },
            containerColor = SlateSurface,
            title = { Text("Modify Proposal Parameters", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Adjust planned duration for ${proposal.title}:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(45, 60, 90, 120).forEach { mins ->
                            FilterChip(
                                selected = modifyDuration == mins,
                                onClick = { modifyDuration = mins },
                                label = { Text("${mins}m", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberGoldDark,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = SlateCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Text(
                        text = "Note: Modifying does not count as a slot rejection for learning purposes (FR-070).",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModifyProposal(proposal, proposal.suggestedStart, modifyDuration)
                        modifyingProposal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricSky)
                ) {
                    Text("Apply & Arm", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { modifyingProposal = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
