package tw.taipei.veges.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tw.taipei.veges.data.alerts.AndroidLocalNotificationPublisher
import tw.taipei.veges.data.alerts.AndroidNotificationPermissionState
import tw.taipei.veges.data.alerts.LocalNotificationPublisher
import tw.taipei.veges.data.alerts.NotificationPermissionState

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindNotificationPublisher(publisher: AndroidLocalNotificationPublisher): LocalNotificationPublisher

    @Binds
    abstract fun bindNotificationPermissionState(state: AndroidNotificationPermissionState): NotificationPermissionState
}
