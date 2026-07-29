package tw.taipei.veges.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaxonomyConceptEntity::class,
        TaxonomyAliasEntity::class,
        OfficialVariantEntity::class,
        SourceObservationEntity::class,
        SourceDayStateEntity::class,
        SyncRunEntity::class,
        ModelMetadataEntity::class,
        EstimateEntity::class,
        TrackedConceptEntity::class,
        AlertRuleEntity::class,
        NotificationEventEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class VegesDatabase : RoomDatabase() {
    abstract fun taxonomyDao(): TaxonomyDao

    abstract fun sourceDao(): SourceDao

    abstract fun estimateDao(): EstimateDao

    abstract fun alertDao(): AlertDao

    abstract fun trackingDao(): TrackingDao
}
