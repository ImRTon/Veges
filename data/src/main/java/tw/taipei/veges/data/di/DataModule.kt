package tw.taipei.veges.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.local.MIGRATION_1_2
import tw.taipei.veges.data.local.MIGRATION_2_3

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideVegesDatabase(@ApplicationContext context: Context): VegesDatabase =
        Room.databaseBuilder(context, VegesDatabase::class.java, "veges.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
