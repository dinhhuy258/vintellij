package com.dinhhuy258.vintellij.commands

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.buffer.BufferManager
import com.dinhhuy258.vintellij.formatting.optimizeImport
import com.dinhhuy258.vintellij.notifications.VintellijEventType
import com.dinhhuy258.vintellij.notifications.VintellijNotification
import com.dinhhuy258.vintellij.utils.uriToPath
import com.google.gson.JsonPrimitive
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.eclipse.lsp4j.ExecuteCommandParams

class ImportOptimizationCommand(client: VintellijLanguageClient, bufferManager: BufferManager) :
    Command(client, bufferManager) {
    override fun executeInternal(params: ExecuteCommandParams) {
        val uri = (params.arguments[0] as JsonPrimitive).asString
        val path = uriToPath(uri)

        optimizeImport(buffer = bufferManager.loadBuffer(path)) {
            val syncedBuffer = bufferManager.loadBuffer(path)!!
            syncedBuffer.psiFile.virtualFile.refresh(true, true)
            FileDocumentManager.getInstance().saveDocument(syncedBuffer.document)

            client.sendNotification(VintellijNotification(VintellijEventType.REFRESH_FILE))
        }
    }

    override fun isValidCommand(params: ExecuteCommandParams): Boolean {
        if (params.arguments.size < 1) {
            return false
        }

        val firstArgument = params.arguments[0]

        return firstArgument is JsonPrimitive && firstArgument.isString
    }
}
