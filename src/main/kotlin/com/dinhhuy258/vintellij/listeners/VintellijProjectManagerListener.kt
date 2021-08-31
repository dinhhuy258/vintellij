package com.dinhhuy258.vintellij.listeners

import com.dinhhuy258.vintellij.VINTELLIJ_CLIENT
import com.dinhhuy258.vintellij.notifications.VintellijEventType
import com.dinhhuy258.vintellij.notifications.VintellijNotification
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class VintellijProjectManagerListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        project.getUserData(VINTELLIJ_CLIENT)
            ?.sendNotification(VintellijNotification(VintellijEventType.CLOSE_CONNECTION))
    }
}
