package com.dinhhuy258.vintellij.formatting

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.offsetToPosition
import com.dinhhuy258.vintellij.utils.runWriteAction
import com.dinhhuy258.vintellij.utils.toTextRange
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit

fun formatDocument(buffer: Buffer?, range: Range?): List<TextEdit> {
    if (buffer == null) {
        return emptyList()
    }

    val textRange = range?.toTextRange(buffer.document)
    val startOffset = textRange?.startOffset ?: 0
    val endOffset = textRange?.endOffset ?: buffer.document.textLength

    val textEdits = ArrayList<TextEdit>()

    invokeAndWait {
        runWriteAction {
            val beforeFormatting = buffer.document.text

            WriteCommandAction.writeCommandAction(buffer.project)
                .run<Throwable> {
                    CodeStyleManager.getInstance(buffer.project)
                        .reformatText(buffer.psiFile, startOffset, endOffset)
                }

            buffer.psiFile.virtualFile.refresh(true, true)

            val formattingText = buffer.document.text
            val formattingRange = Range(
                offsetToPosition(buffer.document, 0),
                offsetToPosition(buffer.document, buffer.document.textLength)
            )

            buffer.document.setText(beforeFormatting)

            textEdits.add(
                TextEdit(
                    formattingRange,
                    formattingText
                )
            )
        }
    }

    return textEdits
}
