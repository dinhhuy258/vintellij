package com.dinhhuy258.vintellij.lsp.diagnostics

import com.dinhhuy258.vintellij.lsp.Buffer
import com.dinhhuy258.vintellij.lsp.hover.stripHtml
import com.dinhhuy258.vintellij.lsp.utils.offsetToPosition
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFix
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Ref
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Range

fun getDiagnostics(buffer: Buffer): List<Diagnostic> {
    val diagnosticsRef = Ref<List<Diagnostic>>()
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

        val errorRows = HashSet<Int>()
        val diagnostics = highlights.filter {
            val document = buffer.document
            it.startOffset > 0 && it.startOffset <= document.textLength && it.endOffset > 0 && it.endOffset <= document.textLength
        }.mapNotNull {
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

        diagnosticsRef.set(diagnostics)
    }

    return diagnosticsRef.get()
}

fun HighlightInfo.toDiagnostic(document: Document): Diagnostic? {
    if (this.description == null) {
        return null
    }

    var description = this.description
    if (description.isBlank() && this.toolTip != null) {
        description = stripHtml(this.toolTip!!)
    }
    val start = offsetToPosition(document, this.getStartOffset())
    val end = offsetToPosition(document, this.getEndOffset())
    var code: String? = null
    if (this.quickFixActionMarkers != null) {
        for (actionPair in this.quickFixActionMarkers) {
            try {
                val action = actionPair.first.action
                if (action is ImportClassFix || action.text == "Import") {
                    code = "Import"
                    break
                }
            } catch (e: Throwable) {
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
