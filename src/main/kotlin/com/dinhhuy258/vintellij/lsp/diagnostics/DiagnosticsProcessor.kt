package com.dinhhuy258.vintellij.lsp.diagnostics

import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.comrade.buffer.SyncBufferManagerListener
import com.dinhhuy258.vintellij.utils.getURIForFile
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import java.util.IdentityHashMap
import kotlin.collections.HashSet
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.services.LanguageClient

class DiagnosticsProcessor() : SyncBufferManagerListener, DaemonCodeAnalyzer.DaemonListener, ProjectManagerListener {
    companion object {
        private const val DEBOUNCE_TIME = 200L
    }

    private var isStarted: Boolean = false

    private val jobsMap = IdentityHashMap<SyncBuffer, Deferred<Unit>>()

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

    override fun bufferCreated(syncBuffer: SyncBuffer) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (syncBuffer.project != project) {
            return
        }

        jobsMap[syncBuffer]?.cancel()
        jobsMap[syncBuffer] = createJobAsync(syncBuffer)
    }

    override fun bufferReleased(syncBuffer: SyncBuffer) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (syncBuffer.project != project) {
            return
        }

        jobsMap.remove(syncBuffer)?.cancel()
    }

    private fun createJobAsync(syncBuffer: SyncBuffer): Deferred<Unit> {
        return ComradeScope.async {
            delay(DEBOUNCE_TIME)
            doLint(syncBuffer)
        }
    }

    private fun doLint(buffer: SyncBuffer) {
        if (!isStarted || buffer.isReleased) {
            return
        }

        val highlights = try {
            getHighlights(buffer)
        } catch (e: Throwable) {
            emptyList()
        }

        reportDiagnostics(buffer, highlights)
    }

    private fun reportDiagnostics(buffer: SyncBuffer, highlightInfos: List<HighlightInfo>) {
        val errorRows = HashSet<Int>()
        val diagnostics = highlightInfos.mapNotNull {
            it.toDiagnostic(buffer.document)
        }.sortedBy {
            it.severity
        }.filter {
            if (it.severity == DiagnosticSeverity.Error) {
                errorRows.add(it.range.start.line)
                true
            } else {
                !errorRows.contains(it.range.start.line)
            }
        }

        client.publishDiagnostics(PublishDiagnosticsParams(getURIForFile(buffer.psiFile), diagnostics))
    }
}
