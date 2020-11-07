package com.dinhhuy258.vintellij.lsp.formatting

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.lsp.utils.toTextRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import org.eclipse.lsp4j.Range

fun formatDocument(syncBuffer: SyncBuffer?, range: Range?) {
    if (syncBuffer == null) {
        return
    }

    val textRange = range?.toTextRange(syncBuffer.document)
    val startOffset = textRange?.startOffset ?: 0
    val endOffset = textRange?.endOffset ?: syncBuffer.document.textLength

    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runWriteAction {
            WriteCommandAction.writeCommandAction(syncBuffer.project)
                    .run<Throwable> {
                        CodeStyleManager.getInstance(syncBuffer.project).reformatText(syncBuffer.psiFile, startOffset, endOffset)
                    }
        }
    }
}
