package com.dinhhuy258.vintellij.lsp.formatting

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager

fun formatDocument(syncBuffer: SyncBuffer?) {
    if (syncBuffer == null) {
        return
    }

    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runWriteAction {
            WriteCommandAction.writeCommandAction(syncBuffer.project)
                    .run<Throwable> {
                        CodeStyleManager.getInstance(syncBuffer.project).reformatText(syncBuffer.psiFile, 0, syncBuffer.document.textLength)
                    }
        }
    }
}
