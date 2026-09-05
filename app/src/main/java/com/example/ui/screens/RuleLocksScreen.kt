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
import com.example.model.ExceptionRequestEntity
import com.example.model.ExceptionStatus
import com.example.model.RuleLockEntity
import com.example.ui.BossUiState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RuleLocksScreen(
    state: BossUiState,
    onRequestException: (ruleLockId: String, reason: String, durationMinutes: Int) -> Unit
) {
    var requestingRule by remember { mutableStateOf<RuleLockEntity?>(null) }
    var exceptionReason by remember { mutableStateOf("") }
    var exceptionDurationHours by remember { mutableStateOf(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Principle 4 Banner
        item {
            Surface(
                color = SlateCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShieldMoon,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "USER-OWNED RULES, MECHANICALLY ENFORCED (ADR-008)",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rule locks are defined exclusively by you in clear-headed moments. The AI never infers or imposes restrictions independently (FR-080). Exceptions require a mandatory 24h cooldown to prevent impulsive setting changes.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Active Rules
        item {
            Text(
                text = "ACTIVE SELF-IMPOSED RULE LOCKS",
                color = ElectricSky,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        items(state.ruleLocks) { rule ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rule.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = EmeraldVerified.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE LOCK",
                                color = EmeraldVerified,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rule.params,
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            requestingRule = rule
                            exceptionReason = ""
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_exception_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Request Exception (24h Cooldown)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Active Exceptions & Cooldowns
        item {
            Text(
                text = "EXCEPTION COOLDOWN REQUESTS (FR-081, FR-083)",
                color = ElectricSky,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (state.exceptions.isEmpty()) {
            item {
                Surface(
                    color = SlateSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No active exception requests. System is running under full disciplined lock.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.exceptions) { ex ->
                val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                val cooldownEndsStr = timeFormat.format(Date(ex.cooldownEndsAt))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = AmberGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ex.status.name,
                                    color = AmberGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "24h Cooldown Active",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stated Reason: \"${ex.reason}\"",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unlock available at: $cooldownEndsStr (Requested for ${ex.requestedDurationMinutes} mins)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        // If extended 2+ times, show justification history per FR-083
                        if (ex.extensionCount >= 2) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = CrimsonUrgent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CrimsonUrgent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "JUSTIFICATION AUDIT (FR-083): Extended ${ex.extensionCount} times",
                                        color = CrimsonUrgent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Does the original justification still genuinely apply? Review past stated reasons before continuing.",
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Request Exception Dialog
    requestingRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { requestingRule = null },
            containerColor = SlateSurface,
            title = {
                Text(
                    text = "Request Exception to Rule Lock",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Rule: ${rule.name}",
                        color = AmberGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Per FR-081, requesting an exception initiates a mandatory 24-hour cooldown timer. The lock will only unlock after 24 hours to ensure you are acting with deliberate intent, not impulsivity.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = exceptionReason,
                        onValueChange = { exceptionReason = it },
                        label = { Text("Stated Justification Reason") },
                        placeholder = { Text("Why is this exception necessary?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exception_reason_input")
                    )

                    Text(
                        text = "Requested Duration: $exceptionDurationHours hours",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exceptionReason.isNotBlank()) {
                            onRequestException(rule.id, exceptionReason, exceptionDurationHours * 60)
                            requestingRule = null
                        }
                    },
                    enabled = exceptionReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                    modifier = Modifier.testTag("confirm_exception_request")
                ) {
                    Text("Initiate 24h Cooldown", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { requestingRule = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
