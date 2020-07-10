package com.dinhhuy258.plugins.components

import com.intellij.openapi.components.ProjectComponent

class VIProjectComponent: ProjectComponent {
    override fun projectClosed() {
        super.projectClosed()
        println("Project closed")
    }

    override fun projectOpened() {
        super.projectOpened()
        println("Project opened")
    }
}