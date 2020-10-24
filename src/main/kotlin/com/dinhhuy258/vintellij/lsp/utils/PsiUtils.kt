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
