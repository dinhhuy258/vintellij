package com.dinhhuy258.vintellij.lsp.buffer

import com.dinhhuy258.vintellij.lsp.utils.toTextRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.eclipse.lsp4j.TextDocumentContentChangeEvent

fun changeDocument(project: Project, buffer: Buffer, contentChanges: List<TextDocumentContentChangeEvent>) {
    val document = buffer.getDocument()
    val changes = contentChanges.sortedWith(compareBy({ it.range.start.line }, { it.range.start.character })).reversed()
    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        CommandProcessor.getInstance().executeCommand(project, {
            application.runWriteAction {
                changes.forEach { change ->
                    applyDocumentChange(document, change)
                }

                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
        }, "update-document", "", UndoConfirmationPolicy.REQUEST_CONFIRMATION)
    }
}

private fun applyDocumentChange(document: Document, change: TextDocumentContentChangeEvent) {
    if (change.range == null) {
        document.setText(change.text)
    } else {
        val textRange = change.range.toTextRange(document)
        document.replaceString(textRange.startOffset, textRange.endOffset, change.text)
    }
}
