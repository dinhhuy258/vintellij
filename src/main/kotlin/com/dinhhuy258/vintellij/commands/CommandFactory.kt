package com.dinhhuy258.vintellij.commands

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.buffer.BufferManager

class CommandNotSupportedException(msg: String) : RuntimeException(msg)

class CommandFactory private constructor() {
    private object Holder {
        val INSTANCE = CommandFactory()
    }

    companion object {
        @JvmStatic
        fun getInstance(): CommandFactory {
            return Holder.INSTANCE
        }
    }

    fun createCommand(commandType: String, client: VintellijLanguageClient, bufferManager: BufferManager): Command {
        when (commandType) {
            // TODO: Modify command name following convention
            "importFix" -> return ImportCommand(client, bufferManager)
            "gen_doc" -> return DocGenerationCommand(client, bufferManager)
            "optimizeImport" -> return ImportOptimizationCommand(client, bufferManager)
        }

        throw CommandNotSupportedException("Command not supported $commandType")
    }
}
