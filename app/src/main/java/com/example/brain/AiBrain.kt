package com.example.brain

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Deterministic AI Brain Core (ADR-004, ai-brain.md)
 * All urgency, slot selection, and proposal logic is purely deterministic,
 * highly explainable, and testable without LLM non-determinism.
 */
object AiBrain {

    // Thresholds
    const val URGENT_THRESHOLD_HOURS = 36L
    const val IMPORTANT_THRESHOLD_HOURS = 72L
    const val REJECTION_OVERRIDE_LIMIT = 2

    data class BusyBlock(
        val start: Long,
        val end: Long,
        val title: String
    )

    data class SlotSelectionResult(
        val slotStart: Long,
        val durationMinutes: Int,
        val reasoning: String
    )

    /**
     * Evaluates urgency purely deterministically (FR-050)
     */
    fun classifyUrgency(dueAt: Long, now: Long = System.currentTimeMillis()): UrgencyLevel {
        val hoursRemaining = (dueAt - now) / (1000 * 3600L)
        return when {
            hoursRemaining <= URGENT_THRESHOLD_HOURS -> UrgencyLevel.URGENT
            hoursRemaining <= IMPORTANT_THRESHOLD_HOURS -> UrgencyLevel.IMPORTANT
            else -> UrgencyLevel.NORMAL
        }
    }

    /**
     * Checks if assignment requires blocking commitment check (FR-051)
     * "Force planning, never execution"
     */
    fun requiresCommitmentCheck(
        assignment: AssignmentEntity,
        linkedTask: TaskEntity?,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (assignment.dismissed) return false
        if (assignment.userStatus == AssignmentStatus.COMPLETED) return false
        if (linkedTask != null && (linkedTask.status == TaskStatus.PLANNED || linkedTask.status == TaskStatus.IN_PROGRESS)) {
            return false
        }
        val urgency = classifyUrgency(assignment.dueAt, now)
        return urgency == UrgencyLevel.URGENT
    }

    /**
     * Deterministic slot selection (FR-060, FR-061, FR-062, FR-063)
     */
    fun selectSlot(
        courseId: String,
        courseName: String,
        dueAt: Long,
        baselineDurationMinutes: Int = 60,
        rollingAvgDeltaMinutes: Float = 0.0f,
        rejectedBuckets: Set<String> = emptySet(),
        existingTasks: List<TaskEntity> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): SlotSelectionResult {
        // Adjust duration using learned rolling delta (FR-090)
        val adjustedMinutes = (baselineDurationMinutes + rollingAvgDeltaMinutes).toInt().coerceAtLeast(30)

        // Build busy blocks
        val busyBlocks = existingTasks.map { task ->
            BusyBlock(task.scheduledStart, task.scheduledStart + (task.plannedMinutes * 60 * 1000L), task.title)
        }.sortedBy { it.start }

        // Start search window 1 hour from now
        val searchStart = now + (3600 * 1000L)
        val durationMs = adjustedMinutes * 60 * 1000L

        var candidateStart = searchStart
        var foundSlot = false

        // Search in 30-min increments
        val incrementMs = 30 * 60 * 1000L
        val maxSearch = now + (24 * 3600 * 1000L) // up to 24h out

        while (candidateStart + durationMs < dueAt && candidateStart < maxSearch) {
            val candidateEnd = candidateStart + durationMs

            // Check overlap with busy blocks (FR-061)
            val overlaps = busyBlocks.any { block ->
                candidateStart < block.end && candidateEnd > block.start
            }

            // Check time bucket against rejection patterns (FR-062)
            val cal = Calendar.getInstance().apply { timeInMillis = candidateStart }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val bucket = when (hour) {
                in 6..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..21 -> "evening"
                else -> "night"
            }

            if (!overlaps && !rejectedBuckets.contains(bucket)) {
                foundSlot = true
                break
            }
            candidateStart += incrementMs
        }

        val finalStart = if (foundSlot) candidateStart else searchStart
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(finalStart))

        val deltaNote = if (rollingAvgDeltaMinutes != 0.0f) {
            val sign = if (rollingAvgDeltaMinutes > 0) "+" else ""
            " ($sign${rollingAvgDeltaMinutes.toInt()}m adjusted from your rolling average)"
        } else ""

        val bucketNote = if (rejectedBuckets.isNotEmpty()) {
            "; avoids ${rejectedBuckets.joinToString()} based on past rejections"
        } else ""

        val reasoning = "Calculated ${adjustedMinutes}m duration$deltaNote. Selected an open calendar window at $formattedTime before due date$bucketNote."

        return SlotSelectionResult(
            slotStart = finalStart,
            durationMinutes = adjustedMinutes,
            reasoning = reasoning
        )
    }
}
