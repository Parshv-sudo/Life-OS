package com.example

import com.example.brain.AiBrain
import com.example.connectors.DedupEngine
import com.example.connectors.EmailConnector
import com.example.connectors.VolpConnector
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification of architectural laws and deterministic AI Brain behavior:
 * - "Force planning, not execution" boundary
 * - Strict read-only nature of connectors
 * - Total data integrity across all nodes
 */
class ExampleUnitTest {

    @Test
    fun test_state_machine_transition_laws() {
        // Law: unseen cannot become completed without passing through planning
        assertFalse(AssignmentStatus.UNSEEN.canTransitionTo(AssignmentStatus.COMPLETED))
        assertTrue(AssignmentStatus.UNSEEN.canTransitionTo(AssignmentStatus.ACKNOWLEDGED))
        assertTrue(AssignmentStatus.ACKNOWLEDGED.canTransitionTo(AssignmentStatus.PLANNED))
        assertTrue(AssignmentStatus.PLANNED.canTransitionTo(AssignmentStatus.IN_PROGRESS))
        assertTrue(AssignmentStatus.IN_PROGRESS.canTransitionTo(AssignmentStatus.COMPLETED))

        // Law: completed state is terminal (no silent reopening)
        assertFalse(AssignmentStatus.COMPLETED.canTransitionTo(AssignmentStatus.PLANNED))
        assertFalse(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.PLANNED))

        // Law: Action cannot execute without being approved
        assertTrue(ActionStatus.APPROVED.canTransitionTo(ActionStatus.EXECUTED))
        assertFalse(ActionStatus.FIRED.canTransitionTo(ActionStatus.APPROVED))

        // Law: Rule lock exception cannot skip cooldown
        assertFalse(ExceptionStatus.REQUESTED.canTransitionTo(ExceptionStatus.GRANTED))
        assertTrue(ExceptionStatus.REQUESTED.canTransitionTo(ExceptionStatus.COOLING_DOWN))
        assertTrue(ExceptionStatus.COOLING_DOWN.canTransitionTo(ExceptionStatus.GRANTED))
    }

    @Test
    fun test_urgency_classification_and_force_planning_boundary() {
        val now = System.currentTimeMillis()
        val urgentDeadline = now + (16 * 3600 * 1000L) // in 16h
        val importantDeadline = now + (48 * 3600 * 1000L) // in 48h
        val normalDeadline = now + (96 * 3600 * 1000L) // in 96h

        assertEquals(UrgencyLevel.URGENT, AiBrain.classifyUrgency(urgentDeadline, now))
        assertEquals(UrgencyLevel.IMPORTANT, AiBrain.classifyUrgency(importantDeadline, now))
        assertEquals(UrgencyLevel.NORMAL, AiBrain.classifyUrgency(normalDeadline, now))

        val urgentAssignment = AssignmentEntity(
            id = "assign_urgent_1",
            courseId = "cs301",
            title = "DAA HW",
            description = "Problem set",
            dueAt = urgentDeadline,
            firstSeenAt = now,
            lastSeenAt = now
        )

        // With no task planned, it MUST trigger the commitment check prompt (FR-051)
        assertTrue(AiBrain.requiresCommitmentCheck(urgentAssignment, null, now))

        // Once a planned task exists, commitment check is satisfied (FR-052)
        val plannedTask = TaskEntity(
            id = "task_1",
            linkedAssignmentId = "assign_urgent_1",
            linkedProposalId = null,
            courseId = "cs301",
            title = "Study DAA",
            plannedMinutes = 60,
            scheduledStart = now + 3600000L,
            schedulingMode = SchedulingMode.EXPLICIT,
            status = TaskStatus.PLANNED,
            date = "2026-09-04"
        )
        assertFalse(AiBrain.requiresCommitmentCheck(urgentAssignment, plannedTask, now))
    }

    @Test
    fun test_strict_read_only_connectors() {
        val volp = VolpConnector()
        val email = EmailConnector()

        assertTrue("VOLP must be read-only", volp.isReadOnly)
        assertTrue("Email must be read-only", email.isReadOnly)
        assertEquals("VOLP poll budget is 2/day", 2, volp.getMaxPollsPerDay())
    }

    @Test
    fun test_conservative_deduplication() {
        val now = System.currentTimeMillis()
        val existing = listOf(
            AssignmentEntity(
                id = "assign_os_1",
                courseId = "cs305",
                title = "OS Milestone",
                description = "Kernel",
                dueAt = now + (24 * 3600 * 1000L),
                firstSeenAt = now,
                lastSeenAt = now
            )
        )

        // Matching course and deadline within 24 hours -> auto-linked
        val closeResult = DedupEngine.evaluateMatch("cs305", now + (25 * 3600 * 1000L), existing)
        assertTrue(closeResult.isDuplicate)
        assertEquals("assign_os_1", closeResult.matchedAssignmentId)
        assertFalse(closeResult.requiresUserConfirmation)

        // Ambiguous match (between 24h and 48h difference) -> surfaces to user instead of silent auto-merge (FR-041)
        val ambiguousResult = DedupEngine.evaluateMatch("cs305", now + (60 * 3600 * 1000L), existing)
        assertFalse(ambiguousResult.isDuplicate)
        assertTrue(ambiguousResult.requiresUserConfirmation)
    }

    @Test
    fun test_slot_selection_excludes_busy_blocks_and_avoids_rejected_buckets() {
        val now = System.currentTimeMillis()
        val dueAt = now + (24 * 3600 * 1000L)

        // Seed a busy task in the next hour
        val busyTasks = listOf(
            TaskEntity(
                id = "task_busy",
                linkedAssignmentId = null,
                linkedProposalId = null,
                courseId = "cs310",
                title = "Lecture Block",
                plannedMinutes = 60,
                scheduledStart = now + (3600 * 1000L),
                schedulingMode = SchedulingMode.EXPLICIT,
                status = TaskStatus.PLANNED,
                date = "2026-09-04"
            )
        )

        val result = AiBrain.selectSlot(
            courseId = "cs301",
            courseName = "Algorithms",
            dueAt = dueAt,
            baselineDurationMinutes = 60,
            rollingAvgDeltaMinutes = 15.0f,
            existingTasks = busyTasks,
            now = now
        )

        // Duration should be adjusted by rolling average delta (+15m -> 75m)
        assertEquals(75, result.durationMinutes)
        // Must not conflict with the busy block [now + 1h, now + 2h]
        val busyStart = busyTasks[0].scheduledStart
        val busyEnd = busyStart + (60 * 60 * 1000L)
        assertFalse(
            "Selected slot must not overlap busy window",
            result.slotStart < busyEnd && (result.slotStart + result.durationMinutes * 60000L) > busyStart
        )
    }
}
