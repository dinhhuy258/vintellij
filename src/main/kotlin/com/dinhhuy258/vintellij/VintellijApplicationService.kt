package com.dinhhuy258.vintellij

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager

object VintellijApplicationService {
    private val balloonGroup = NotificationGroup("vintellij", NotificationDisplayType.BALLOON, true)

    val instance: VintellijApplicationService
        get() = ServiceManager.getService(VintellijApplicationService::class.java)

    fun showBalloon(msg: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            val notification = balloonGroup.createNotification(msg, type)
            Notifications.Bus.notify(notification)
        }
    }
}