package com.dinhhuy258.vintellij.commands

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.google.gson.JsonPrimitive
import com.intellij.openapi.util.TextRange
import org.eclipse.lsp4j.ExecuteCommandParams

class ImportCommand(client: VintellijLanguageClient, bufferManager: BufferManager) : Command(client, bufferManager) {
    override fun executeInternal(params: ExecuteCommandParams) {
        val classToImport = (params.arguments[0] as JsonPrimitive).asString
        val path = (params.arguments[1] as JsonPrimitive).asString

        val buffer = bufferManager.loadBuffer(path)
        if (buffer != null) {
            val document = buffer.document
            val lineCount = document.lineCount

            var lastImportLine = 0
            // Currently, I assume that there is no file has no more than 50 import line
            val maxImportSize = 50
            // Collect all imports line in the current document
            for (line in 0 until (lineCount - 1).coerceAtMost(maxImportSize)) {
                val startOffset = document.getLineStartOffset(line)
                val endOffset = document.getLineEndOffset(line)
                val lineText = document.getText(TextRange(startOffset, endOffset)).trim()

                if (!lineText.startsWith("import ")) {
                    continue
                }

                if (lineText == classToImport) {
                    // Ignore if the class was already imported
                    return
                }

                lastImportLine = line
            }

            client.syncBuffer(
                VintellijSyncBuffer(path, lastImportLine + 1, 2, listOf(classToImport), false)
            )
        }
    }

    override fun isValidCommand(params: ExecuteCommandParams): Boolean {
        if (params.arguments.size < 2) {
            return false
        }

        val firstArgument = params.arguments[0]
        val secondArgument = params.arguments[1]

        return firstArgument is JsonPrimitive && firstArgument.isString && secondArgument is JsonPrimitive && secondArgument.isString
    }
}
