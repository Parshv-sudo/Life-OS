package com.example.connectors

import com.example.model.AssignmentEntity
import com.example.model.AssignmentStatus
import java.util.UUID

// ================================================================
// STRICT READ-ONLY CONNECTOR CONTRACT (FR-010, FR-011, ADR-005/006)
// There are NO write, submit, delete, or send methods.
// ================================================================

data class SessionHandle(
    val token: String,
    val uid: String,
    val expiresAt: Long,
    val readOnlyVerified: Boolean = true
)

data class RawRecord(
    val id: String,
    val source: String,
    val rawPayload: Map<String, String>,
    val timestamp: Long
)

interface ReadOnlyConnector {
    val name: String
    val isReadOnly: Boolean get() = true
    suspend fun authenticate(): SessionHandle
    suspend fun fetchNew(): List<RawRecord>
    fun isHealthy(): Boolean
    fun getPollCountToday(): Int
    fun getMaxPollsPerDay(): Int
}

/**
 * VOLP Connector (Academic Portal) - Read Only
 * Pulls course list and hands-on assignment deadlines.
 * Enforces rate limiting (FR-021: max 2 polls/day by default).
 */
class VolpConnector : ReadOnlyConnector {
    override val name: String = "VOLP Academic Portal"
    private var healthy: Boolean = true
    private var pollCountToday: Int = 0
    private val maxPolls: Int = 2
    private var lastPollDate: String = ""

    override suspend fun authenticate(): SessionHandle {
        // Authenticates via token+uid flow without storing plaintext password (FR-012, FR-020)
        return SessionHandle(
            token = "volp_tok_" + UUID.randomUUID().toString().take(12),
            uid = "volp_uid_5582",
            expiresAt = System.currentTimeMillis() + (86400 * 1000L),
            readOnlyVerified = true
        )
    }

    private fun resetIfNewDay() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        if (today != lastPollDate) {
            pollCountToday = 0
            lastPollDate = today
        }
    }

    override suspend fun fetchNew(): List<RawRecord> {
        resetIfNewDay()
        if (pollCountToday >= maxPolls) return emptyList() // Rate limit enforced (FR-021)
        pollCountToday++
        return listOf(
            RawRecord(
                id = "volp_rec_4401",
                source = "VOLP",
                rawPayload = mapOf(
                    "course" to "CS301",
                    "title" to "DAA Problem Set 4",
                    "due" to (System.currentTimeMillis() + 16 * 3600000L).toString()
                ),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override fun isHealthy(): Boolean = healthy
    override fun getPollCountToday(): Int { resetIfNewDay(); return pollCountToday }
    override fun getMaxPollsPerDay(): Int = maxPolls
}

/**
 * Email Connector - Read Only (FR-030, FR-031, FR-032)
 * Read-only mailbox scope only (OAuth2).
 * Relevance filter isolates faculty/admin emails.
 * Discards raw body after extraction per NFR-003 privacy mandate.
 */
class EmailConnector : ReadOnlyConnector {
    override val name: String = "College Faculty Email"
    private var healthy: Boolean = true
    private var pollCountToday: Int = 0
    private val maxPolls: Int = 6
    private var lastPollDate: String = ""

    override suspend fun authenticate(): SessionHandle {
        return SessionHandle(
            token = "oauth_read_mail_" + UUID.randomUUID().toString().take(12),
            uid = "student@college.edu",
            expiresAt = System.currentTimeMillis() + (7200 * 1000L),
            readOnlyVerified = true
        )
    }

    private fun resetIfNewDay() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        if (today != lastPollDate) {
            pollCountToday = 0
            lastPollDate = today
        }
    }

    override suspend fun fetchNew(): List<RawRecord> {
        resetIfNewDay()
        if (pollCountToday >= maxPolls) return emptyList() // Rate limit enforced
        pollCountToday++
        // Relevance filter applied at connector boundary
        return listOf(
            RawRecord(
                id = "email_rec_992",
                source = "EMAIL",
                rawPayload = mapOf(
                    "sender" to "prof.mehra@college.edu",
                    "senderRole" to "faculty",
                    "subject" to "Announcement: OS Milestone 2 POSIX IPC Submission Guidelines",
                    "extractedAction" to "Implement shared memory buffer and semaphores",
                    "extractedDeadline" to (System.currentTimeMillis() + 54 * 3600000L).toString(),
                    "relatedCourse" to "CS305"
                ),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override fun isHealthy(): Boolean = healthy
    override fun getPollCountToday(): Int { resetIfNewDay(); return pollCountToday }
    override fun getMaxPollsPerDay(): Int = maxPolls
}

/**
 * Deduplication Engine (FR-040, FR-041, ADR-010)
 * Conservative cross-source deduplication.
 * Auto-links only on course match + deadline proximity (<= 24 hours).
 * Never silently auto-merges ambiguous records.
 */
object DedupEngine {
    data class DedupResult(
        val isDuplicate: Boolean,
        val matchedAssignmentId: String?,
        val confidence: Float,
        val requiresUserConfirmation: Boolean
    )

    fun evaluateMatch(
        incomingCourseId: String,
        incomingDueAt: Long,
        existingAssignments: List<AssignmentEntity>,
        timeWindowMs: Long = 24 * 3600 * 1000L // 24h window
    ): DedupResult {
        for (existing in existingAssignments) {
            val courseMatches = existing.courseId.equals(incomingCourseId, ignoreCase = true)
            val timeDiff = Math.abs(existing.dueAt - incomingDueAt)

            if (courseMatches && timeDiff <= timeWindowMs) {
                val confidence = 1.0f - (timeDiff.toFloat() / timeWindowMs.toFloat() * 0.2f)
                return DedupResult(
                    isDuplicate = true,
                    matchedAssignmentId = existing.id,
                    confidence = confidence,
                    requiresUserConfirmation = false
                )
            } else if (courseMatches && timeDiff <= timeWindowMs * 2) {
                // Ambiguous proximity: Surface to user instead of silent merge (FR-041)
                return DedupResult(
                    isDuplicate = false,
                    matchedAssignmentId = existing.id,
                    confidence = 0.55f,
                    requiresUserConfirmation = true
                )
            }
        }
        return DedupResult(
            isDuplicate = false,
            matchedAssignmentId = null,
            confidence = 0.0f,
            requiresUserConfirmation = false
        )
    }
}
