package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ==========================================
// STATE MACHINES & INTEGRITY GUARDS
// ==========================================

enum class AssignmentStatus {
    UNSEEN,
    ACKNOWLEDGED,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    DISMISSED;

    fun canTransitionTo(target: AssignmentStatus): Boolean {
        if (this == target) return true
        if (this == COMPLETED) return false // Completed is terminal
        return when (this) {
            UNSEEN -> target == ACKNOWLEDGED || target == DISMISSED
            ACKNOWLEDGED -> target == PLANNED || target == DISMISSED
            PLANNED -> target == IN_PROGRESS || target == DISMISSED
            IN_PROGRESS -> target == COMPLETED || target == DISMISSED
            DISMISSED -> target == ACKNOWLEDGED // can be restored
            COMPLETED -> false
        }
    }
}

enum class TaskStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    MISSED;

    fun canTransitionTo(target: TaskStatus): Boolean {
        if (this == target) return true
        if (this == COMPLETED) return false // Completed is terminal
        return when (this) {
            PLANNED -> target == IN_PROGRESS || target == MISSED
            IN_PROGRESS -> target == COMPLETED || target == MISSED
            MISSED -> target == PLANNED // rescheduling retains history
            COMPLETED -> false
        }
    }
}

enum class ProposalStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    MODIFIED;

    fun canTransitionTo(target: ProposalStatus): Boolean {
        if (this == target) return true
        if (this != PENDING) return false // Terminal once resolved
        return true
    }
}

enum class ActionStatus {
    APPROVED,
    EXECUTED,
    FIRED,
    CANCELLED;

    fun canTransitionTo(target: ActionStatus): Boolean {
        if (this == target) return true
        if (this == FIRED || this == CANCELLED) return false
        return when (this) {
            APPROVED -> target == EXECUTED || target == CANCELLED
            EXECUTED -> target == FIRED || target == CANCELLED
            FIRED, CANCELLED -> false
        }
    }
}

enum class ExceptionStatus {
    REQUESTED,
    COOLING_DOWN,
    GRANTED,
    EXPIRED,
    RENEWED,
    CLOSED;

    fun canTransitionTo(target: ExceptionStatus): Boolean {
        if (this == target) return true
        return when (this) {
            REQUESTED -> target == COOLING_DOWN
            COOLING_DOWN -> target == GRANTED // Cooldown MUST elapse
            GRANTED -> target == EXPIRED
            EXPIRED -> target == RENEWED || target == CLOSED
            RENEWED -> target == COOLING_DOWN
            CLOSED -> false
        }
    }
}

enum class UrgencyLevel {
    NORMAL,     // > 72h - morning review queue
    IMPORTANT,  // <= 72h - checkpoint proposal
    URGENT      // <= 36h - triggers blocking commitment prompt if unplanned
}

enum class SchedulingMode {
    FLEXIBLE,    // AI picks optimal slot based on constraints
    CONSTRAINED, // User specifies a window, AI picks within it
    EXPLICIT     // User specifies exact start time
}

// ==========================================
// PERSISTED ROOM ENTITIES (Total Node Integrity)
// ==========================================

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val instructor: String,
    val source: String // e.g. "VOLP"
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val description: String,
    val dueAt: Long,
    val graded: Boolean = false,
    val evaluated: Boolean = false,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val linkedSources: String = "[]", // JSON array of SourceRef
    val mergeConfidence: Float = 1.0f,
    val userStatus: AssignmentStatus = AssignmentStatus.UNSEEN,
    val dismissed: Boolean = false
)

@Entity(tableName = "proposals")
data class ProposalEntity(
    @PrimaryKey val id: String,
    val assignmentId: String,
    val title: String,
    val suggestedMinutes: Int,
    val suggestedStart: Long,
    val reasoning: String,
    val urgency: UrgencyLevel,
    val status: ProposalStatus = ProposalStatus.PENDING,
    val surfacedContext: String,
    val surfacedAt: Long,
    val respondedAt: Long? = null,
    val rejectedSlotHistory: String = "[]"
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val linkedAssignmentId: String?,
    val linkedProposalId: String?,
    val courseId: String,
    val title: String,
    val plannedMinutes: Int,
    val actualMinutes: Int? = null,
    val scheduledStart: Long,
    val schedulingMode: SchedulingMode,
    val status: TaskStatus = TaskStatus.PLANNED,
    val date: String,
    val sourcePrompt: String = ""
)

@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey val id: String,
    val type: String, // "CALENDAR_EVENT" or "ALARM_NOTIFICATION"
    val title: String,
    val startTime: Long,
    val durationMinutes: Int,
    val linkedTaskId: String,
    val platformTarget: String = "Android",
    val executedOn: String = "Android-LocalNode",
    val approved: Boolean = false,
    val status: ActionStatus = ActionStatus.APPROVED
)

@Entity(tableName = "chore_logs")
data class ChoreLogEntity(
    @PrimaryKey val choreId: String,
    val title: String,
    val sequenceOrder: Int,
    val completed: Boolean,
    val date: String,
    val timestamp: Long
)

@Entity(tableName = "course_estimations")
data class CourseEstimationEntity(
    @PrimaryKey val courseId: String,
    val rollingAvgDelta: Float = 0.0f, // actual - planned difference in minutes
    val sampleCount: Int = 0
)

@Entity(tableName = "override_patterns")
data class OverridePatternEntity(
    @PrimaryKey val key: String, // "${courseId}_${timeBucket}" e.g. "cs301_morning"
    val courseId: String,
    val timeBucket: String, // "morning", "afternoon", "evening", "night"
    val rejectionCount: Int = 0
)

@Entity(tableName = "rule_locks")
data class RuleLockEntity(
    @PrimaryKey val id: String,
    val name: String,
    val restrictionType: String,
    val createdAt: Long,
    val params: String,
    val active: Boolean = true
)

@Entity(tableName = "exception_requests")
data class ExceptionRequestEntity(
    @PrimaryKey val id: String,
    val ruleLockId: String,
    val reason: String,
    val requestedDurationMinutes: Int,
    val status: ExceptionStatus = ExceptionStatus.REQUESTED,
    val requestedAt: Long,
    val cooldownEndsAt: Long,
    val grantedAt: Long? = null,
    val expiresAt: Long? = null,
    val extensionCount: Int = 0,
    val justificationHistory: String = "[]" // JSON array of past reasons
)

@Entity(tableName = "interaction_logs")
data class InteractionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalId: String,
    val surfacedContext: String,
    val response: String,
    val responseLatencyMs: Long,
    val timestamp: Long
)

@Entity(tableName = "node_audits")
data class NodeAuditRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val nodeName: String = "Android-Node-Primary",
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val integrityVerified: Boolean = true
)
