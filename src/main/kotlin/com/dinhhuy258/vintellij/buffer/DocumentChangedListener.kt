package com.dinhhuy258.vintellij.buffer

import com.dinhhuy258.vintellij.listeners.VINTELLIJ_WIN_FOCUS
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

class DocumentChangedListener(
    private val project: Project,
    private val path: String,
    private val onDocumentChanged: (String, Int, Int, List<String>) -> Unit
) : DocumentListener {

    internal var isChangedByVim = false

    private var startLine: Int = 0

    private var endLine: Int = 0

    override fun beforeDocumentChange(event: DocumentEvent) {
        if (!shouldNotifyVim()) {
            return
        }

        startLine = event.document.getLineNumber(event.offset)
        endLine = event.document.getLineNumber(event.offset + event.oldLength) + 1
    }

    override fun documentChanged(event: DocumentEvent) {
        if (!shouldNotifyVim()) {
            return
        }
        val document = event.document

        val afterEndLine = document.getLineNumber(event.offset + event.newLength)
        val lines =
            document.getText(TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(afterEndLine)))
                .split('\n')

        onDocumentChanged(path, startLine, endLine, lines)
    }

    private fun shouldNotifyVim(): Boolean {
        val winFocus = project.getUserData(VINTELLIJ_WIN_FOCUS)

        return !isChangedByVim && winFocus == true
    }
}
