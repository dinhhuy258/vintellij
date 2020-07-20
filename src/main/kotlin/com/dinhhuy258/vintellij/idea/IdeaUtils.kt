package com.dinhhuy258.vintellij.idea

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.utils.UrlUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.io.File

class IdeaUtils {
    companion object {
        fun getProject(): Project {
            val projects = ProjectManager.getInstance().openProjects
            if (projects.isEmpty()) {
                throw VIException("Project not found. Please open a project on intellij.")
            }

            return projects[0]
        }

        fun getVirtualFile(fileName: String): VirtualFile {
            if (UrlUtils.isVimJarFilePath(fileName)) {
                return VirtualFileManager.getInstance().refreshAndFindFileByUrl(UrlUtils.toIntellijJarFilePath(fileName))
                        ?: throw VIException("Virtual file not found: $fileName.")
            }

            val application = ApplicationManager.getApplication()
            val file = File(FileUtil.toSystemDependentName(fileName))
            if (!file.exists()) {
                throw VIException("File not found: $fileName.")
            }
            val virtualFileRef = Ref<VirtualFile>()
            application.invokeAndWait {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                        ?: throw VIException("Virtual file not found: $fileName.")
                virtualFileRef.set(virtualFile)
            }

            return virtualFileRef.get()
        }

        fun getPsiFile(fileName: String): PsiFile {
            val project = getProject()
            val virtualFile = getVirtualFile(fileName)
            return virtualFile.toPsiFile(project) ?: throw VIException("Cannot find the PsiFile for $fileName.")
        }
    }
}
