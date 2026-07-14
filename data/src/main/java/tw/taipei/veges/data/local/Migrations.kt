package tw.taipei.veges.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE estimates ADD COLUMN sourceDatesJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE estimates ADD COLUMN pairedCalibrationPeriods INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE estimates ADD COLUMN disclosureShortTag TEXT NOT NULL DEFAULT '估算'")
        db.execSQL("ALTER TABLE estimates ADD COLUMN disclosureFullLabel TEXT NOT NULL DEFAULT 'Taipei retail reference estimate'")
    }
}
