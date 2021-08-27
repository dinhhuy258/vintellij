package com.dinhhuy258.vintellij.diagnostics

import com.dinhhuy258.vintellij.VintellijScope
import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.buffer.BufferEventListener
import com.dinhhuy258.vintellij.utils.normalizeUri
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import java.util.IdentityHashMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.services.LanguageClient

class DiagnosticsProcessor : BufferEventListener, DaemonCodeAnalyzer.DaemonListener, ProjectManagerListener {
    companion object {
        private const val DEBOUNCE_TIME = 200L
    }

    private var isStarted: Boolean = false

    private val jobsMap = IdentityHashMap<Buffer, Deferred<Unit>>()

    private lateinit var project: Project

    private lateinit var client: LanguageClient

    fun start(project: Project, client: LanguageClient) {
        if (!isStarted) {
            this.project = project
            this.client = client
            isStarted = true
        }
    }

    override fun daemonFinished(fileEditors: Collection<FileEditor>) {
        fileEditors.forEach { editor ->
            val syncBuf = jobsMap.keys.firstOrNull { it.psiFile.virtualFile === editor.file }
            if (syncBuf != null) {
                jobsMap[syncBuf]?.cancel()
                jobsMap[syncBuf] = createJobAsync(syncBuf)
            }
        }
    }

    override fun bufferCreated(buffer: Buffer) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (buffer.project != project) {
            return
        }

        jobsMap[buffer]?.cancel()
        jobsMap[buffer] = createJobAsync(buffer)
    }

    override fun bufferReleased(buffer: Buffer) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (buffer.project != project) {
            return
        }

        jobsMap.remove(buffer)?.cancel()
    }

    private fun createJobAsync(buffer: Buffer): Deferred<Unit> {
        return VintellijScope.async {
            delay(DEBOUNCE_TIME)
            doLint(buffer)
        }
    }

    private fun doLint(buffer: Buffer) {
        if (!isStarted) {
            return
        }

        val diagnostics = try {
            getDiagnostics(buffer)
        } catch (e: Throwable) {
            emptyList()
        }

        reportDiagnostics(buffer, diagnostics)
    }

    private fun reportDiagnostics(buffer: Buffer, diagnostics: List<Diagnostic>) {
        client.publishDiagnostics(PublishDiagnosticsParams(normalizeUri(buffer.psiFile.virtualFile.url), diagnostics))
    }
}
