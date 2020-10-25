package com.dinhhuy258.vintellij.comrade

import com.dinhhuy258.vintellij.comrade.core.NvimInstanceManager
import com.dinhhuy258.vintellij.comrade.insight.InsightProcessor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

val ComradeScope = ComradeNeovimPlugin.instance.coroutineScope

@State(name = "ComradeNeovim",
        storages = [Storage(file = "\$APP_CONFIG\$/comrade_neovim_settings.xml")])
class ComradeNeovimPlugin : BaseComponent, Disposable {
    companion object {
        val instance: ComradeNeovimPlugin by lazy {
            ApplicationManager.getApplication().getComponent(ComradeNeovimPlugin::class.java)
        }
    }

    private lateinit var msgBusConnection: MessageBusConnection
    private lateinit var job: Job
    // Retain a reference to make sure the singleton get initialized
    @Suppress("unused")

    val coroutineScope by lazy { CoroutineScope(job + Dispatchers.Default) }

    private val projectManagerListener = object : ProjectManagerListener {
        override fun projectOpened(project: Project) {
            NvimInstanceManager.refresh()
            // Start the singleton InsightProcessor here to avoid cyclic initialization
            InsightProcessor.start()
        }

        override fun projectClosing(project: Project) {
            NvimInstanceManager.cleanUp(project)
        }
    }

    override fun initComponent() {
        job = Job()
        Disposer.register(this, NvimInstanceManager)
        msgBusConnection = ApplicationManager.getApplication().messageBus.connect(this)
        msgBusConnection.subscribe(ProjectManager.TOPIC, projectManagerListener)
    }

    override fun disposeComponent() {
        Disposer.dispose(this)
    }

    override fun dispose() {
        job.cancel()
        super.disposeComponent()
    }
}
