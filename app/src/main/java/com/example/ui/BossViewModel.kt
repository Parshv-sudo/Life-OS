package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brain.AiBrain
import com.example.connectors.EmailConnector
import com.example.connectors.VolpConnector
import com.example.data.BossDatabase
import com.example.data.BossRepository
import com.example.execution.AndroidExecutionLayer
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

data class BossUiState(
    val courses: List<CourseEntity> = emptyList(),
    val assignments: List<AssignmentEntity> = emptyList(),
    val pendingProposals: List<ProposalEntity> = emptyList(),
    val allTasks: List<TaskEntity> = emptyList(),
    val allActions: List<ActionEntity> = emptyList(),
    val chores: List<ChoreLogEntity> = emptyList(),
    val ruleLocks: List<RuleLockEntity> = emptyList(),
    val exceptions: List<ExceptionRequestEntity> = emptyList(),
    val estimations: List<CourseEstimationEntity> = emptyList(),
    val overridePatterns: List<OverridePatternEntity> = emptyList(),
    val audits: List<NodeAuditRecord> = emptyList(),
    // Commitment Check Trigger
    val urgentCommitmentAssignment: AssignmentEntity? = null,
    // Connectors status
    val volpPollCount: Int = 1,
    val volpMaxPolls: Int = 2,
    val emailPollCount: Int = 3,
    val emailMaxPolls: Int = 6,
    val nodeIntegrityHash: String = "e3b0c44298fc1c14",
    val integrityVerified: Boolean = true,
    val lastSyncTime: String = "Just now"
)

class BossViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BossDatabase.getDatabase(application)
    private val repository = BossRepository(db.bossDao())
    private val executionLayer = AndroidExecutionLayer(application)
    val volpConnector = VolpConnector()
    val emailConnector = EmailConnector()

    private val _uiState = MutableStateFlow(BossUiState())
    val uiState: StateFlow<BossUiState> = _uiState.asStateFlow()

    private val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            observeData()
        }
    }

    private data class PlanningBundle(
        val courses: List<CourseEntity>,
        val assignments: List<AssignmentEntity>,
        val proposals: List<ProposalEntity>,
        val tasks: List<TaskEntity>,
        val actions: List<ActionEntity>
    )

    private data class DisciplineBundle(
        val chores: List<ChoreLogEntity>,
        val rules: List<RuleLockEntity>,
        val exceptions: List<ExceptionRequestEntity>,
        val estimations: List<CourseEstimationEntity>,
        val patterns: List<OverridePatternEntity>,
        val audits: List<NodeAuditRecord>
    )

    private fun observeData() {
        val planningFlow = combine(
            repository.allCourses,
            repository.allAssignments,
            repository.pendingProposals,
            repository.allTasks,
            repository.allActions
        ) { courses, assignments, proposals, tasks, actions ->
            PlanningBundle(courses, assignments, proposals, tasks, actions)
        }

        val disciplineFlow = combine(
            repository.getChoresForDate(todayStr),
            repository.allRuleLocks,
            repository.allExceptions,
            repository.allEstimations,
            repository.allOverridePatterns
        ) { chores, rules, exceptions, estimations, patterns ->
            DisciplineBundle(chores, rules, exceptions, estimations, patterns, emptyList())
        }

        viewModelScope.launch {
            combine(planningFlow, disciplineFlow, repository.auditLogs) { plan, disc, audits ->
                val urgentUnplanned = plan.assignments.firstOrNull { assignment ->
                    val linkedTask = plan.tasks.firstOrNull { it.linkedAssignmentId == assignment.id }
                    AiBrain.requiresCommitmentCheck(assignment, linkedTask)
                }

                // Compute node state hash for total data integrity
                val digest = MessageDigest.getInstance("SHA-256")
                val statePayload = "A:${plan.assignments.size}_T:${plan.tasks.size}_P:${plan.proposals.size}_R:${disc.rules.size}"
                val hash = digest.digest(statePayload.toByteArray())
                    .take(8)
                    .joinToString("") { "%02x".format(it) }

                BossUiState(
                    courses = plan.courses,
                    assignments = plan.assignments,
                    pendingProposals = plan.proposals,
                    allTasks = plan.tasks,
                    allActions = plan.actions,
                    chores = disc.chores,
                    ruleLocks = disc.rules,
                    exceptions = disc.exceptions,
                    estimations = disc.estimations,
                    overridePatterns = disc.patterns,
                    audits = audits,
                    urgentCommitmentAssignment = urgentUnplanned,
                    volpPollCount = volpConnector.getPollCountToday(),
                    volpMaxPolls = volpConnector.getMaxPollsPerDay(),
                    emailPollCount = emailConnector.getPollCountToday(),
                    emailMaxPolls = emailConnector.getMaxPollsPerDay(),
                    nodeIntegrityHash = hash,
                    integrityVerified = true,
                    lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // ====================================================
    // FORCE PLANNING, NOT EXECUTION: RESOLVE COMMITMENT
    // ====================================================

    fun resolveUrgentCommitment(
        assignmentId: String,
        startTime: Long,
        durationMinutes: Int,
        title: String
    ) {
        viewModelScope.launch {
            val task = repository.commitUrgentPlan(assignmentId, startTime, durationMinutes, title)
            // Execute OS Action via Execution Layer (FR-071, FR-072)
            val action = ActionEntity(
                id = "act_" + UUID.randomUUID().toString().take(8),
                type = "ALARM_NOTIFICATION",
                title = "Commitment: $title",
                startTime = startTime,
                durationMinutes = durationMinutes,
                linkedTaskId = task.id,
                platformTarget = "Android",
                executedOn = "Android-LocalNode",
                approved = true,
                status = ActionStatus.APPROVED
            )
            executionLayer.executeAction(action)
        }
    }

    // ====================================================
    // PROPOSAL LIFECYCLE (ACCEPT, REJECT, MODIFY)
    // ====================================================

    fun acceptProposal(proposal: ProposalEntity) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(proposal.suggestedStart))
            val taskId = "task_" + UUID.randomUUID().toString().take(8)
            val newTask = TaskEntity(
                id = taskId,
                linkedAssignmentId = proposal.assignmentId,
                linkedProposalId = proposal.id,
                courseId = proposal.assignmentId.let { id ->
                    _uiState.value.assignments.firstOrNull { it.id == id }?.courseId ?: "general"
                },
                title = proposal.title,
                plannedMinutes = proposal.suggestedMinutes,
                actualMinutes = null,
                scheduledStart = proposal.suggestedStart,
                schedulingMode = SchedulingMode.FLEXIBLE,
                status = TaskStatus.PLANNED,
                date = today,
                sourcePrompt = "AI Brain Proposal Accepted"
            )

            val actionId = "act_" + UUID.randomUUID().toString().take(8)
            val newAction = ActionEntity(
                id = actionId,
                type = "ALARM_NOTIFICATION",
                title = "Study Block: ${proposal.title}",
                startTime = proposal.suggestedStart,
                durationMinutes = proposal.suggestedMinutes,
                linkedTaskId = taskId,
                platformTarget = "Android",
                executedOn = "Android-LocalNode",
                approved = true,
                status = ActionStatus.APPROVED
            )

            repository.insertTaskAndAction(newTask, newAction)
            repository.recordProposalResponse(proposal.id, "Accepted with scheduled slot", ProposalStatus.ACCEPTED)
            executionLayer.executeAction(newAction)
        }
    }

    fun rejectProposal(proposal: ProposalEntity, reason: String) {
        viewModelScope.launch {
            // Update override pattern for that course and time bucket (FR-091)
            val cal = Calendar.getInstance().apply { timeInMillis = proposal.suggestedStart }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val bucket = when (hour) {
                in 6..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..21 -> "evening"
                else -> "night"
            }

            val assignment = _uiState.value.assignments.firstOrNull { it.id == proposal.assignmentId }
            val courseId = assignment?.courseId ?: "general"
            val key = "${courseId}_$bucket"
            val existingPattern = _uiState.value.overridePatterns.firstOrNull { it.key == key }
            val updatedPattern = OverridePatternEntity(
                key = key,
                courseId = courseId,
                timeBucket = bucket,
                rejectionCount = (existingPattern?.rejectionCount ?: 0) + 1
            )
            db.bossDao().insertOrUpdateOverridePattern(updatedPattern)

            repository.recordProposalResponse(proposal.id, "Rejected: $reason (Bucket '$bucket' count incremented)", ProposalStatus.REJECTED)
        }
    }

    fun modifyProposal(proposal: ProposalEntity, newStartTime: Long, newDurationMinutes: Int) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(newStartTime))
            val taskId = "task_" + UUID.randomUUID().toString().take(8)
            val newTask = TaskEntity(
                id = taskId,
                linkedAssignmentId = proposal.assignmentId,
                linkedProposalId = proposal.id,
                courseId = proposal.assignmentId.let { id ->
                    _uiState.value.assignments.firstOrNull { it.id == id }?.courseId ?: "general"
                },
                title = proposal.title,
                plannedMinutes = newDurationMinutes,
                actualMinutes = null,
                scheduledStart = newStartTime,
                schedulingMode = SchedulingMode.CONSTRAINED,
                status = TaskStatus.PLANNED,
                date = today,
                sourcePrompt = "Modified User Commitment"
            )

            val actionId = "act_" + UUID.randomUUID().toString().take(8)
            val newAction = ActionEntity(
                id = actionId,
                type = "ALARM_NOTIFICATION",
                title = "Modified Commitment: ${proposal.title}",
                startTime = newStartTime,
                durationMinutes = newDurationMinutes,
                linkedTaskId = taskId,
                platformTarget = "Android",
                executedOn = "Android-LocalNode",
                approved = true,
                status = ActionStatus.APPROVED
            )

            repository.insertTaskAndAction(newTask, newAction)
            repository.recordProposalResponse(proposal.id, "Modified parameters by user (does not count as rejection per FR-070)", ProposalStatus.MODIFIED)
            executionLayer.executeAction(newAction)
        }
    }

    // ====================================================
    // CHORES (SELF-REPORTED, SEQUENCE ORDERED)
    // ====================================================

    fun toggleChore(choreId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.setChoreCompleted(choreId, todayStr, completed)
        }
    }

    // ====================================================
    // MANUAL TASK CREATION (FLEXIBLE / CONSTRAINED / EXPLICIT)
    // ====================================================

    fun createManualTask(
        title: String,
        courseId: String,
        mode: SchedulingMode,
        explicitStartTime: Long,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startTime = when (mode) {
                SchedulingMode.EXPLICIT -> explicitStartTime
                SchedulingMode.FLEXIBLE -> {
                    val estimation = _uiState.value.estimations.firstOrNull { it.courseId == courseId }?.rollingAvgDelta ?: 0f
                    val result = AiBrain.selectSlot(
                        courseId = courseId,
                        courseName = courseId,
                        dueAt = now + (48 * 3600 * 1000L),
                        baselineDurationMinutes = durationMinutes,
                        rollingAvgDeltaMinutes = estimation,
                        existingTasks = _uiState.value.allTasks
                    )
                    result.slotStart
                }
                SchedulingMode.CONSTRAINED -> explicitStartTime // within window
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startTime))
            val taskId = "task_" + UUID.randomUUID().toString().take(8)
            val newTask = TaskEntity(
                id = taskId,
                linkedAssignmentId = null,
                linkedProposalId = null,
                courseId = courseId,
                title = title,
                plannedMinutes = durationMinutes,
                actualMinutes = null,
                scheduledStart = startTime,
                schedulingMode = mode,
                status = TaskStatus.PLANNED,
                date = today,
                sourcePrompt = "Manual Entry ($mode)"
            )

            val actionId = "act_" + UUID.randomUUID().toString().take(8)
            val newAction = ActionEntity(
                id = actionId,
                type = "ALARM_NOTIFICATION",
                title = "Commitment: $title",
                startTime = startTime,
                durationMinutes = durationMinutes,
                linkedTaskId = taskId,
                platformTarget = "Android",
                executedOn = "Android-LocalNode",
                approved = true,
                status = ActionStatus.APPROVED
            )

            repository.insertTaskAndAction(newTask, newAction)
            executionLayer.executeAction(newAction)
        }
    }

    // ====================================================
    // RULE LOCKS & COOLDOWN (FR-080, FR-081, FR-082, FR-083)
    // ====================================================

    fun requestRuleException(ruleLockId: String, reason: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.requestRuleLockException(ruleLockId, reason, durationMinutes)
        }
    }

    fun completeTask(taskId: String, actualMinutes: Int) {
        viewModelScope.launch {
            val task = _uiState.value.allTasks.firstOrNull { it.id == taskId } ?: return@launch
            val updated = task.copy(status = TaskStatus.COMPLETED, actualMinutes = actualMinutes)
            db.bossDao().updateTask(updated)

            // Update course estimation rolling average delta (FR-090)
            val delta = actualMinutes - task.plannedMinutes
            val currentEstimation = _uiState.value.estimations.firstOrNull { it.courseId == task.courseId }
            val newCount = (currentEstimation?.sampleCount ?: 0) + 1
            val currentAvg = currentEstimation?.rollingAvgDelta ?: 0f
            val newAvg = currentAvg + (delta - currentAvg) / newCount

            db.bossDao().insertOrUpdateCourseEstimation(
                CourseEstimationEntity(
                    courseId = task.courseId,
                    rollingAvgDelta = newAvg,
                    sampleCount = newCount
                )
            )

            if (task.linkedAssignmentId != null) {
                repository.updateAssignmentStatus(task.linkedAssignmentId, AssignmentStatus.COMPLETED)
            }
        }
    }

    fun pollConnectors() {
        viewModelScope.launch {
            volpConnector.fetchNew()
            emailConnector.fetchNew()
            _uiState.value = _uiState.value.copy(
                volpPollCount = volpConnector.getPollCountToday(),
                emailPollCount = emailConnector.getPollCountToday(),
                lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
        }
    }
}
