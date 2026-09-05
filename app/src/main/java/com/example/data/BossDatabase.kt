package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.*

class Converters {
    @TypeConverter
    fun fromAssignmentStatus(value: AssignmentStatus): String = value.name
    @TypeConverter
    fun toAssignmentStatus(value: String): AssignmentStatus = AssignmentStatus.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromProposalStatus(value: ProposalStatus): String = value.name
    @TypeConverter
    fun toProposalStatus(value: String): ProposalStatus = ProposalStatus.valueOf(value)

    @TypeConverter
    fun fromActionStatus(value: ActionStatus): String = value.name
    @TypeConverter
    fun toActionStatus(value: String): ActionStatus = ActionStatus.valueOf(value)

    @TypeConverter
    fun fromExceptionStatus(value: ExceptionStatus): String = value.name
    @TypeConverter
    fun toExceptionStatus(value: String): ExceptionStatus = ExceptionStatus.valueOf(value)

    @TypeConverter
    fun fromUrgencyLevel(value: UrgencyLevel): String = value.name
    @TypeConverter
    fun toUrgencyLevel(value: String): UrgencyLevel = UrgencyLevel.valueOf(value)

    @TypeConverter
    fun fromSchedulingMode(value: SchedulingMode): String = value.name
    @TypeConverter
    fun toSchedulingMode(value: String): SchedulingMode = SchedulingMode.valueOf(value)
}

@Database(
    entities = [
        CourseEntity::class,
        AssignmentEntity::class,
        ProposalEntity::class,
        TaskEntity::class,
        ActionEntity::class,
        ChoreLogEntity::class,
        CourseEstimationEntity::class,
        OverridePatternEntity::class,
        RuleLockEntity::class,
        ExceptionRequestEntity::class,
        InteractionLogEntity::class,
        NodeAuditRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BossDatabase : RoomDatabase() {
    abstract fun bossDao(): BossDao

    companion object {
        @Volatile
        private var INSTANCE: BossDatabase? = null

        fun getDatabase(context: Context): BossDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BossDatabase::class.java,
                    "boss_lifeos.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
