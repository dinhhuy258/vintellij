package com.dinhhuy258.vintellij.buffer

import com.dinhhuy258.vintellij.VintellijLanguageClient
import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import java.util.concurrent.atomic.AtomicInteger

class BufferSynchronization(private val client: VintellijLanguageClient) {
    private val pendingChanges = AtomicInteger(0)

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

    fun hasPendingChanges(): Boolean {
        if (pendingChanges.get() <= 0) {
            return false
        }

        pendingChanges.decrementAndGet()

        return true
    }

    fun onDocumentChanged(path: String, startLine: Int, endLine: Int, lines: List<String>) {
        pendingChanges.incrementAndGet()

        client.syncBuffer(VintellijSyncBuffer(path, startLine, endLine, lines))
    }
}
