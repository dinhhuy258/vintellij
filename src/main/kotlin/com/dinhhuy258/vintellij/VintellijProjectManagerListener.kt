package com.dinhhuy258.vintellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class VintellijProjectManagerListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        project.getUserData(VINTELLIJ_CLIENT)
            ?.sendEventNotification(VintellijEventNotification(VintellijEventType.CLOSE_CONNECTION))
    }
}