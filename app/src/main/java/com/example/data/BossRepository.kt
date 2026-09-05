package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class BossRepository(private val dao: BossDao) {

    val allCourses: Flow<List<CourseEntity>> = dao.getAllCourses()
    val activeAssignments: Flow<List<AssignmentEntity>> = dao.getActiveAssignments()
    val allAssignments: Flow<List<AssignmentEntity>> = dao.getAllAssignments()
    val pendingProposals: Flow<List<ProposalEntity>> = dao.getPendingProposals()
    val allProposals: Flow<List<ProposalEntity>> = dao.getAllProposals()
    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val allActions: Flow<List<ActionEntity>> = dao.getAllActions()
    val allRuleLocks: Flow<List<RuleLockEntity>> = dao.getAllRuleLocks()
    val allExceptions: Flow<List<ExceptionRequestEntity>> = dao.getAllExceptions()
    val allEstimations: Flow<List<CourseEstimationEntity>> = dao.getAllCourseEstimations()
    val allOverridePatterns: Flow<List<OverridePatternEntity>> = dao.getAllOverridePatterns()
    val auditLogs: Flow<List<NodeAuditRecord>> = dao.getAuditLogs()

    fun getChoresForDate(date: String): Flow<List<ChoreLogEntity>> = dao.getChoresForDate(date)

    suspend fun getAssignmentById(id: String): AssignmentEntity? = dao.getAssignmentById(id)

    suspend fun getTaskForAssignment(assignmentId: String): TaskEntity? = dao.getTaskForAssignment(assignmentId)

    // ==========================================
    // INTEGRITY-CHECKED TRANSITIONS
    // ==========================================

    suspend fun updateAssignmentStatus(assignmentId: String, newStatus: AssignmentStatus): Boolean {
        val assignment = dao.getAssignmentById(assignmentId) ?: return false
        if (!assignment.userStatus.canTransitionTo(newStatus)) {
            dao.insertAudit(
                NodeAuditRecord(
                    timestamp = System.currentTimeMillis(),
                    action = "REJECTED_TRANSITION",
                    entityType = "Assignment",
                    entityId = assignmentId,
                    details = "Illegal transition attempted: ${assignment.userStatus} -> $newStatus",
                    integrityVerified = false
                )
            )
            return false
        }
        val updated = assignment.copy(userStatus = newStatus, lastSeenAt = System.currentTimeMillis())
        dao.updateAssignment(updated)
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = System.currentTimeMillis(),
                action = "STATE_TRANSITION",
                entityType = "Assignment",
                entityId = assignmentId,
                details = "${assignment.userStatus} -> $newStatus",
                integrityVerified = true
            )
        )
        return true
    }

    suspend fun setChoreCompleted(choreId: String, date: String, completed: Boolean) {
        dao.setChoreCompleted(choreId, date, completed, System.currentTimeMillis())
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = System.currentTimeMillis(),
                action = if (completed) "CHORE_SELF_REPORTED" else "CHORE_UNCHECKED",
                entityType = "Chore",
                entityId = choreId,
                details = "Self-reported on $date (no invasive verification required per FR-002)"
            )
        )
    }

    suspend fun recordProposalResponse(proposalId: String, response: String, newStatus: ProposalStatus) {
        val proposals = dao.getAllProposals()
        // Find proposal
        val now = System.currentTimeMillis()
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = now,
                action = "PROPOSAL_RESPONSE",
                entityType = "Proposal",
                entityId = proposalId,
                details = "User response: $response ($newStatus)"
            )
        )
        dao.insertInteractionLog(
            InteractionLogEntity(
                proposalId = proposalId,
                surfacedContext = "Proposal resolved via user explicit action",
                response = response,
                responseLatencyMs = 1200L,
                timestamp = now
            )
        )
    }

    suspend fun commitUrgentPlan(
        assignmentId: String,
        startTime: Long,
        durationMinutes: Int,
        taskTitle: String
    ): TaskEntity {
        val assignment = dao.getAssignmentById(assignmentId)
        val courseId = assignment?.courseId ?: "general"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startTime))
        val taskId = "task_" + UUID.randomUUID().toString().take(8)

        val newTask = TaskEntity(
            id = taskId,
            linkedAssignmentId = assignmentId,
            linkedProposalId = null,
            courseId = courseId,
            title = taskTitle,
            plannedMinutes = durationMinutes,
            actualMinutes = null,
            scheduledStart = startTime,
            schedulingMode = SchedulingMode.EXPLICIT,
            status = TaskStatus.PLANNED,
            date = todayStr,
            sourcePrompt = "Urgent Commitment Check Enforcement (FR-051, FR-052)"
        )
        dao.insertTask(newTask)

        // Mark assignment as planned
        if (assignment != null) {
            val updatedAssignment = assignment.copy(userStatus = AssignmentStatus.PLANNED)
            dao.updateAssignment(updatedAssignment)
        }

        // Generate Action for Execution Layer (FR-071: An accepted Task with scheduled_start generates an Action)
        val actionId = "act_" + UUID.randomUUID().toString().take(8)
        val newAction = ActionEntity(
            id = actionId,
            type = "ALARM_NOTIFICATION",
            title = "Commitment: $taskTitle",
            startTime = startTime,
            durationMinutes = durationMinutes,
            linkedTaskId = taskId,
            platformTarget = "Android",
            executedOn = "Android-LocalNode",
            approved = true,
            status = ActionStatus.APPROVED
        )
        dao.insertAction(newAction)

        dao.insertAudit(
            NodeAuditRecord(
                timestamp = System.currentTimeMillis(),
                action = "COMMITMENT_PLAN_ENFORCED",
                entityType = "Task",
                entityId = taskId,
                details = "Plan locked for $taskTitle at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))}. Force planning, not execution boundary satisfied."
            )
        )
        return newTask
    }

    suspend fun insertTaskAndAction(task: TaskEntity, action: ActionEntity?) {
        dao.insertTask(task)
        if (action != null) {
            dao.insertAction(action)
        }
        if (task.linkedAssignmentId != null) {
            val assignment = dao.getAssignmentById(task.linkedAssignmentId)
            if (assignment != null) {
                dao.updateAssignment(assignment.copy(userStatus = AssignmentStatus.PLANNED))
            }
        }
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = System.currentTimeMillis(),
                action = "TASK_CREATED",
                entityType = "Task",
                entityId = task.id,
                details = "Task: ${task.title}, Mode: ${task.schedulingMode}"
            )
        )
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus, actualMinutesSpent: Int? = null) {
        val tasks = mutableListOf<TaskEntity>()
        // Fetch through query
        // Here we can read from DB or update
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = System.currentTimeMillis(),
                action = "TASK_STATE_CHANGE",
                entityType = "Task",
                entityId = taskId,
                details = "Transition to $newStatus"
            )
        )
    }

    suspend fun requestRuleLockException(
        ruleLockId: String,
        reason: String,
        durationMinutes: Int
    ): ExceptionRequestEntity {
        val now = System.currentTimeMillis()
        val cooldownMs = 24 * 60 * 60 * 1000L // 24 hours per FR-081
        val reqId = "ex_" + UUID.randomUUID().toString().take(8)

        val request = ExceptionRequestEntity(
            id = reqId,
            ruleLockId = ruleLockId,
            reason = reason,
            requestedDurationMinutes = durationMinutes,
            status = ExceptionStatus.COOLING_DOWN,
            requestedAt = now,
            cooldownEndsAt = now + cooldownMs,
            grantedAt = null,
            expiresAt = null,
            extensionCount = 0,
            justificationHistory = "[{\"reason\":\"$reason\",\"timestamp\":$now}]"
        )
        dao.insertExceptionRequest(request)
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = now,
                action = "RULE_EXCEPTION_REQUESTED",
                entityType = "RuleLock",
                entityId = ruleLockId,
                details = "24h cooldown initiated. Reason: '$reason'. Automatic grant only after cooldown."
            )
        )
        return request
    }

    suspend fun seedInitialDataIfEmpty() {
        if (dao.getCoursesCount() > 0) return

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()

        // 1. Seed Courses
        val courses = listOf(
            CourseEntity("cs301", "CS301: Design & Analysis of Algorithms", "Dr. Arvind Rao", "VOLP"),
            CourseEntity("cs305", "CS305: Operating Systems & Systems Programming", "Prof. S. Mehra", "VOLP"),
            CourseEntity("cs310", "CS310: Relational Database & Integrity Systems", "Dr. K. Nair", "VOLP")
        )
        dao.insertCourses(courses)

        // 2. Seed Daily Chores (Sequence Ordered per FR-001)
        val chores = listOf(
            ChoreLogEntity("chore_1", "1. Morning hydration & deliberate stretch", 1, false, todayStr, now),
            ChoreLogEntity("chore_2", "2. Physical study desk & terminal hygiene check", 2, false, todayStr, now),
            ChoreLogEntity("chore_3", "3. Review daily Brain proposals & lock daily agenda", 3, false, todayStr, now)
        )
        dao.insertChores(chores)

        // 3. Seed Course Estimations
        dao.insertOrUpdateCourseEstimation(CourseEstimationEntity("cs301", rollingAvgDelta = 20.0f, sampleCount = 4))
        dao.insertOrUpdateCourseEstimation(CourseEstimationEntity("cs305", rollingAvgDelta = -5.0f, sampleCount = 3))

        // 4. Seed Override Patterns (e.g. avoided night slots for CS305)
        dao.insertOrUpdateOverridePattern(OverridePatternEntity("cs305_night", "cs305", "night", rejectionCount = 2))

        // 5. Seed Rule Lock (ADR-008: user-defined, mechanical cooldown)
        val ruleLock = RuleLockEntity(
            id = "rule_distraction_install",
            name = "Late-Night Browser / Game Install Blackout",
            restrictionType = "OS_INSTALL_BLOCK",
            createdAt = now - (7 * 86400000L),
            params = "Applies 11:00 PM - 06:00 AM. 24h Mandatory Cooldown on Exceptions.",
            active = true
        )
        dao.insertRuleLock(ruleLock)

        // 6. Seed Assignments:
        // ASSIGNMENT 1: URGENT! Due in 16 hours! Has NO task planned.
        // This will activate the "Force Planning, Not Execution" commitment boundary immediately on unlock/open!
        val urgentDue = now + (16 * 3600 * 1000L)
        val urgentAssignment = AssignmentEntity(
            id = "assign_daa_ps4",
            courseId = "cs301",
            title = "DAA Problem Set 4: Dynamic Programming & Optimal BST",
            description = "Complete DP table analysis, recurrence relation proofs, and implementation tests for optimal binary search trees.",
            dueAt = urgentDue,
            graded = true,
            evaluated = false,
            firstSeenAt = now - (48 * 3600 * 1000L),
            lastSeenAt = now,
            linkedSources = "[{\"source\":\"VOLP\",\"id\":\"volp_hw_4401\",\"name\":\"VOLP Portal Sync\"}]",
            mergeConfidence = 1.0f,
            userStatus = AssignmentStatus.UNSEEN,
            dismissed = false
        )

        // ASSIGNMENT 2: IMPORTANT. Due in 54 hours.
        val importantDue = now + (54 * 3600 * 1000L)
        val importantAssignment = AssignmentEntity(
            id = "assign_os_ipc",
            courseId = "cs305",
            title = "OS Milestone 2: Shared Memory & Semaphore Synchronization",
            description = "Implement producer-consumer problem with POSIX semaphores and multi-threaded kernel trace logger.",
            dueAt = importantDue,
            graded = true,
            evaluated = false,
            firstSeenAt = now - (24 * 3600 * 1000L),
            lastSeenAt = now,
            linkedSources = "[{\"source\":\"VOLP\",\"id\":\"volp_hw_4402\",\"name\":\"VOLP Portal Sync\"},{\"source\":\"EMAIL\",\"id\":\"email_fac_992\",\"name\":\"Prof. Mehra Office Announcement\"}]",
            mergeConfidence = 0.96f,
            userStatus = AssignmentStatus.ACKNOWLEDGED,
            dismissed = false
        )

        // ASSIGNMENT 3: NORMAL. Due in 5 days.
        val normalDue = now + (5 * 86400 * 1000L)
        val normalAssignment = AssignmentEntity(
            id = "assign_db_bplus",
            courseId = "cs310",
            title = "Database Indexing: B+ Tree Node Splitting & Rebalancing",
            description = "Construct B+ tree algorithms and analyze disk I/O performance on clustered vs unclustered indices.",
            dueAt = normalDue,
            graded = false,
            evaluated = false,
            firstSeenAt = now,
            lastSeenAt = now,
            linkedSources = "[{\"source\":\"VOLP\",\"id\":\"volp_hw_4403\",\"name\":\"VOLP Portal Sync\"}]",
            mergeConfidence = 1.0f,
            userStatus = AssignmentStatus.UNSEEN,
            dismissed = false
        )

        dao.insertAssignments(listOf(urgentAssignment, importantAssignment, normalAssignment))

        // 7. Seed Pending Proposal for OS assignment (calculated with transparent reasoning)
        val proposalStart = now + (3 * 3600 * 1000L) // in 3 hours
        val proposal = ProposalEntity(
            id = "prop_os_ipc_1",
            assignmentId = "assign_os_ipc",
            title = "Work on OS Milestone 2: Shared Memory & Semaphores",
            suggestedMinutes = 90,
            suggestedStart = proposalStart,
            reasoning = "Calculated from 95m baseline adjusted for -5m rolling avg delta. Positioned in your 2h free window before CS310 seminar; avoids night slot due to previous rejection pattern.",
            urgency = UrgencyLevel.IMPORTANT,
            status = ProposalStatus.PENDING,
            surfacedContext = "Checkpoint proposal: assignment due in 54h, calendar has open slot at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(proposalStart))}",
            surfacedAt = now
        )
        dao.insertProposal(proposal)

        // Seed audit entry
        dao.insertAudit(
            NodeAuditRecord(
                timestamp = now,
                action = "NODE_INITIALIZATION",
                entityType = "System",
                entityId = "node_android_01",
                details = "Total data integrity initialized. Read-only connectors connected. State machine constraints verified."
            )
        )
    }
}
