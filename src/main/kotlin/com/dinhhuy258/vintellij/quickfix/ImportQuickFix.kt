package com.dinhhuy258.vintellij.quickfix

import com.dinhhuy258.vintellij.quickfix.imports.ImportSuggesterFactory
import com.dinhhuy258.vintellij.buffer.Buffer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.Ref
import org.eclipse.lsp4j.Position

fun getImportCandidates(buffer: Buffer?, position: Position): List<String> {
    if (buffer == null) {
        return emptyList()
    }

    val candidatesRef = Ref<List<String>>()
    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runReadAction {
            val psiFile = buffer.psiFile
            val editor = buffer.editor.editor
            val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))
            val element = psiFile.findElementAt(offset)?.parent ?: return@runReadAction

            val importSuggester = ImportSuggesterFactory.createImportSuggester(psiFile.language)
            candidatesRef.set(importSuggester.collectSuggestions(element))
        }
    }

    return candidatesRef.get()
}
