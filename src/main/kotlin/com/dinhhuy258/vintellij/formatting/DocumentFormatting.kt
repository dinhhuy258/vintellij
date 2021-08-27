package com.dinhhuy258.vintellij.formatting

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.toTextRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.eclipse.lsp4j.Range

fun formatDocument(buffer: Buffer?, range: Range?) {
    if (buffer == null) {
        return
    }

    val textRange = range?.toTextRange(buffer.document)
    val startOffset = textRange?.startOffset ?: 0
    val endOffset = textRange?.endOffset ?: buffer.document.textLength

    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runWriteAction {
            buffer.onVimDocumentChange {
                // Save before formatting
                FileDocumentManager.getInstance().saveDocument(buffer.document)

                WriteCommandAction.writeCommandAction(buffer.project)
                    .run<Throwable> {
                        CodeStyleManager.getInstance(buffer.project)
                            .reformatText(buffer.psiFile, startOffset, endOffset)
                    }

                buffer.psiFile.virtualFile.refresh(true, true)
                FileDocumentManager.getInstance().saveDocument(buffer.document)
            }
        }
    }
}
