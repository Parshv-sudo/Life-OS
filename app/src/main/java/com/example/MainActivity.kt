package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BossViewModel
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.CommitmentEnforcementDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class BossScreen(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Daily Plan", Icons.Default.Dashboard, "nav_tab_dashboard"),
    COMMITMENTS("AI Brain", Icons.Default.Psychology, "nav_tab_commitments"),
    CONNECTORS("Connectors", Icons.Default.Hub, "nav_tab_connectors"),
    RULE_LOCKS("Rule Locks", Icons.Default.LockClock, "nav_tab_rules"),
    INTEGRITY("Integrity", Icons.Default.VerifiedUser, "nav_tab_integrity")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BossApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossApp(viewModel: BossViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(BossScreen.DASHBOARD) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = ElectricSky.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Boss Shield",
                                    tint = ElectricSky,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "BOSS // LIFE-OS",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Discipline Over Motivation • Strict Read-Only Connectors",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = SlateCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldVerified)
                            )
                            Text(
                                text = uiState.nodeIntegrityHash.take(6),
                                color = EmeraldVerified,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                contentColor = TextSecondary,
                tonalElevation = 8.dp
            ) {
                BossScreen.values().forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBg,
                            selectedTextColor = ElectricSky,
                            indicatorColor = ElectricSky,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == BossScreen.DASHBOARD || currentScreen == BossScreen.COMMITMENTS) {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    containerColor = ElectricSky,
                    contentColor = ObsidianBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Planned Task"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                BossScreen.DASHBOARD -> DailyDashboardScreen(
                    state = uiState,
                    onToggleChore = { choreId, completed -> viewModel.toggleChore(choreId, completed) },
                    onCompleteTask = { taskId, actualMins -> viewModel.completeTask(taskId, actualMins) },
                    onAddTaskClick = { showAddTaskDialog = true }
                )
                BossScreen.COMMITMENTS -> CommitmentsScreen(
                    state = uiState,
                    onAcceptProposal = { proposal -> viewModel.acceptProposal(proposal) },
                    onRejectProposal = { proposal, reason -> viewModel.rejectProposal(proposal, reason) },
                    onModifyProposal = { proposal, newStart, newDuration ->
                        viewModel.modifyProposal(proposal, newStart, newDuration)
                    }
                )
                BossScreen.CONNECTORS -> ConnectorsScreen(
                    state = uiState,
                    onManualPollClick = { viewModel.pollConnectors() }
                )
                BossScreen.RULE_LOCKS -> RuleLocksScreen(
                    state = uiState,
                    onRequestException = { ruleId, reason, duration ->
                        viewModel.requestRuleException(ruleId, reason, duration)
                    }
                )
                BossScreen.INTEGRITY -> NodeIntegrityScreen(
                    state = uiState
                )
            }

            // CRITICAL LAW: Force planning, not execution
            // If an urgent uncommitted assignment exists, surface the non-dismissible commitment prompt
            uiState.urgentCommitmentAssignment?.let { urgentAssignment ->
                CommitmentEnforcementDialog(
                    assignment = urgentAssignment,
                    onCommitPlan = { startTime, duration, title ->
                        viewModel.resolveUrgentCommitment(urgentAssignment.id, startTime, duration, title)
                    }
                )
            }

            if (showAddTaskDialog) {
                AddTaskDialog(
                    courses = uiState.courses,
                    onDismiss = { showAddTaskDialog = false },
                    onSaveTask = { title, courseId, mode, startTime, duration ->
                        viewModel.createManualTask(title, courseId, mode, startTime, duration)
                        showAddTaskDialog = false
                    }
                )
            }
        }
    }
}
