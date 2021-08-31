package com.dinhhuy258.vintellij.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.ProjectFrameHelper
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

val VINTELLIJ_WIN_FOCUS: Key<Boolean> = Key.create("vintellij_win_focus")

object VintellijWindowFocusListener : WindowFocusListener {
    override fun windowGainedFocus(e: WindowEvent) {
        val project = getProject(e) ?: return
        project.putUserData(VINTELLIJ_WIN_FOCUS, true)
    }

    override fun windowLostFocus(e: WindowEvent) {
        val project = getProject(e) ?: return
        project.putUserData(VINTELLIJ_WIN_FOCUS, false)
    }

    private fun getProject(e: WindowEvent): Project? {
        return WindowManager.getInstance().allProjectFrames
            .filterIsInstance(ProjectFrameHelper::class.java)
            .firstOrNull { it.frame === e.component }?.project
    }
}
