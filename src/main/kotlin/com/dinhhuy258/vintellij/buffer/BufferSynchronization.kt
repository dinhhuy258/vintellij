package com.dinhhuy258.vintellij.buffer

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.openapi.vfs.newvfs.RefreshQueue

class BufferSynchronization {
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
}
