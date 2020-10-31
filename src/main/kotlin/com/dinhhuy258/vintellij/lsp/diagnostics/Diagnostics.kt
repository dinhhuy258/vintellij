package com.dinhhuy258.vintellij.lsp.diagnostics

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.lsp.utils.offsetToPosition
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Ref
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Range

fun getHighlights(buffer: SyncBuffer): List<HighlightInfo> {
    val highlightsRef = Ref<List<HighlightInfo>>()
    ApplicationManager.getApplication().runReadAction {
        val highlights = mutableListOf<HighlightInfo>()
        DaemonCodeAnalyzerEx.processHighlights(
                buffer.document,
                buffer.project,
                HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING,
                0,
                buffer.document.getLineEndOffset(buffer.document.lineCount - 1)
        ) { highlightInfo ->
            highlights.add(highlightInfo)
            true
        }
        highlights.filter {
            val document = buffer.document
            it.startOffset > 0 && it.startOffset <= document.textLength && it.endOffset > 0 && it.endOffset <= document.textLength
        }

        highlightsRef.set(highlights)
    }

    return highlightsRef.get()
}

fun HighlightInfo.toDiagnostic(document: Document): Diagnostic? {
    if (this.description == null) {
        return null
    }

    val description = this.description
    val start = offsetToPosition(document, this.getStartOffset())
    val end = offsetToPosition(document, this.getEndOffset())
    var code: String? = null
    if (this.quickFixActionMarkers != null) {
        for (actionPair in this.quickFixActionMarkers) {
            if (actionPair.first.action.text == "Import") {
                code = "Import"
                break
            }
        }
    }

    return Diagnostic(Range(start, end), description, this.diagnosticSeverity(), "vintellij", code)
}

private fun HighlightInfo.diagnosticSeverity() =
    when (this.severity) {
        HighlightSeverity.INFORMATION -> DiagnosticSeverity.Information
        HighlightSeverity.WARNING -> DiagnosticSeverity.Warning
        HighlightSeverity.ERROR -> DiagnosticSeverity.Error
        else -> DiagnosticSeverity.Hint
    }
