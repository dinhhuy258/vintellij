package com.dinhhuy258.vintellij.commands

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.documents.generateDoc
import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.dinhhuy258.vintellij.utils.uriToPath
import com.google.gson.JsonPrimitive
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.Position

class DocGenerationCommand(client: VintellijLanguageClient, bufferManager: BufferManager) : Command(client, bufferManager) {
    override fun executeInternal(params: ExecuteCommandParams) {
        val uri = (params.arguments[0] as JsonPrimitive).asString
        val path = uriToPath(uri)
        val row = (params.arguments[1] as JsonPrimitive).asInt
        val col = (params.arguments[2] as JsonPrimitive).asInt

        val doc = generateDoc(bufferManager.loadBuffer(path), Position(row - 1, col))
        if (doc != null) {
            val lines = doc.split("\n").filter { line ->
                line != ""
            }.mapIndexed { idx: Int, line: String ->
                if (idx == 0) {
                    line
                } else {
                    " $line"
                }
            }
            client.syncBuffer(
                VintellijSyncBuffer(path, row - 1, row + lines.size, lines, false)
            )
        }
    }

    override fun isValidCommand(params: ExecuteCommandParams): Boolean {
        if (params.arguments.size < 3) {
            return false
        }

        val firstArgument = params.arguments[0]
        val secondArgument = params.arguments[1]
        val thirdArgument = params.arguments[2]

        return firstArgument is JsonPrimitive && firstArgument.isString &&
            secondArgument is JsonPrimitive && secondArgument.isNumber &&
            thirdArgument is JsonPrimitive && thirdArgument.isNumber
    }
}
