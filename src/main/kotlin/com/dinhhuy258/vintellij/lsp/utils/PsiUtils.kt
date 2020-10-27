package com.dinhhuy258.vintellij.lsp.utils

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun Position.toOffset(doc: Document) = doc.getLineStartOffset(this.line) + this.character

fun Range.toTextRange(document: Document) =
    TextRange(
            this.start.toOffset(document),
            this.end.toOffset(document)
    )

fun offsetToPosition(document: Document, offset: Int): Position {
    if (offset == -1) {
        return Position(0, 0)
    }
    val line = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(line)
    val column = offset - lineStartOffset
    return Position(line, column)
}
