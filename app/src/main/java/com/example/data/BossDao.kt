package com.example.data

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BossDao {

    // ================= Courses =================
    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCoursesCount(): Int

    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    // ================= Assignments =================
    @Query("SELECT * FROM assignments ORDER BY dueAt ASC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE id = :id LIMIT 1")
    suspend fun getAssignmentById(id: String): AssignmentEntity?

    @Query("SELECT * FROM assignments WHERE dismissed = 0 AND userStatus != 'COMPLETED' ORDER BY dueAt ASC")
    fun getActiveAssignments(): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>)

    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)

    // ================= Proposals =================
    @Query("SELECT * FROM proposals WHERE status = 'PENDING' ORDER BY suggestedStart ASC")
    fun getPendingProposals(): Flow<List<ProposalEntity>>

    @Query("SELECT * FROM proposals ORDER BY surfacedAt DESC")
    fun getAllProposals(): Flow<List<ProposalEntity>>

    @Query("SELECT * FROM proposals WHERE id = :id LIMIT 1")
    suspend fun getProposalById(id: String): ProposalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: ProposalEntity)

    @Update
    suspend fun updateProposal(proposal: ProposalEntity)

    // ================= Tasks =================
    @Query("SELECT * FROM tasks ORDER BY scheduledStart ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY scheduledStart ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE linkedAssignmentId = :assignmentId LIMIT 1")
    suspend fun getTaskForAssignment(assignmentId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    // ================= Actions =================
    @Query("SELECT * FROM actions ORDER BY startTime ASC")
    fun getAllActions(): Flow<List<ActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionEntity)

    @Update
    suspend fun updateAction(action: ActionEntity)

    // ================= Chores (Fixed Daily Sequence) =================
    @Query("SELECT * FROM chore_logs WHERE date = :date ORDER BY sequenceOrder ASC")
    fun getChoresForDate(date: String): Flow<List<ChoreLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChores(chores: List<ChoreLogEntity>)

    @Query("UPDATE chore_logs SET completed = :completed, timestamp = :timestamp WHERE choreId = :choreId AND date = :date")
    suspend fun setChoreCompleted(choreId: String, date: String, completed: Boolean, timestamp: Long)

    // ================= Rule Locks & Exceptions =================
    @Query("SELECT * FROM rule_locks ORDER BY createdAt ASC")
    fun getAllRuleLocks(): Flow<List<RuleLockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuleLock(ruleLock: RuleLockEntity)

    @Query("SELECT * FROM exception_requests ORDER BY requestedAt DESC")
    fun getAllExceptions(): Flow<List<ExceptionRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExceptionRequest(request: ExceptionRequestEntity)

    @Update
    suspend fun updateExceptionRequest(request: ExceptionRequestEntity)

    // ================= Learning & Estimation =================
    @Query("SELECT * FROM course_estimations WHERE courseId = :courseId LIMIT 1")
    suspend fun getCourseEstimation(courseId: String): CourseEstimationEntity?

    @Query("SELECT * FROM course_estimations")
    fun getAllCourseEstimations(): Flow<List<CourseEstimationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCourseEstimation(estimation: CourseEstimationEntity)

    @Query("SELECT * FROM override_patterns WHERE `key` = :key LIMIT 1")
    suspend fun getOverridePattern(key: String): OverridePatternEntity?

    @Query("SELECT * FROM override_patterns")
    fun getAllOverridePatterns(): Flow<List<OverridePatternEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOverridePattern(pattern: OverridePatternEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLog(log: InteractionLogEntity)

    // ================= Node Audit & Total Data Integrity =================
    @Query("SELECT * FROM node_audits ORDER BY timestamp DESC LIMIT 100")
    fun getAuditLogs(): Flow<List<NodeAuditRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: NodeAuditRecord)
}
