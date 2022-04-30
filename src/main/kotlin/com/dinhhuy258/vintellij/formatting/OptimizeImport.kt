package com.dinhhuy258.vintellij.formatting

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.runWriteAction
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.command.WriteCommandAction

fun optimizeImport(buffer: Buffer?, onOptimizationDone: Runnable) {
    if (buffer == null) {
        return
    }

    invokeAndWait {
        runWriteAction {
            WriteCommandAction.writeCommandAction(buffer.project)
                .run<Throwable> {
                    val processor = OptimizeImportsProcessor(buffer.project, buffer.psiFile)
                    processor.setPostRunnable(onOptimizationDone)
                    processor.run()
                }
        }
    }
}
