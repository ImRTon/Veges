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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE estimates ADD COLUMN estimatorApprovedOn TEXT NOT NULL DEFAULT '1970-01-01'",
        )
        db.execSQL(
            "UPDATE estimates SET estimatorApprovedOn = calibrationCutoff",
        )
        db.execSQL(
            "ALTER TABLE estimates ADD COLUMN formula TEXT NOT NULL DEFAULT 'CALIBRATED_MODEL'",
        )
    }
}
