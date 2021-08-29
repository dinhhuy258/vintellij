package com.dinhhuy258.vintellij.buffer

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.openapi.vfs.newvfs.RefreshQueue

class BufferSynchronization(private val client: VintellijLanguageClient) {
    fun performSync(project: Project, finishRunnable: Runnable) {
        val files = mutableListOf<VirtualFile>()
        runWriteAction {
            val file =
                VirtualFileManager.getInstance().findFileByUrl(project.baseDir.url) as? NewVirtualFile
                    ?: return@runWriteAction
            files.add(file)
            if (file.isDirectory) {
                file.markDirtyRecursively()
            } else {
                file.markDirty()
            }
        }

        RefreshQueue.getInstance().refresh(true, true, finishRunnable, *files.toTypedArray())
    }

    fun onDocumentChanged(path: String, startLine: Int, endLine: Int, lines: List<String>) {
        client.syncBuffer(VintellijSyncBuffer(path, startLine, endLine, lines))
    }
}
