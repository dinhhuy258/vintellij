package com.dinhhuy258.plugins.idea

import com.dinhhuy258.plugins.exceptions.VIException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
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
            val application = ApplicationManager.getApplication()
            val file = File(FileUtil.toSystemDependentName(fileName))
            if (!file.exists()) {
                throw VIException("File not found: $fileName.")
            }
            val virtualFileRef = Ref<VirtualFile>()
            application.invokeAndWait {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                        ?: throw VIException("Virtual file not found: $fileName!!!")
                virtualFileRef.set(virtualFile)
            }

            return virtualFileRef.get()
        }

        fun getPsiFile(fileName: String): PsiFile {
            val application = ApplicationManager.getApplication()
            val project = getProject()
            val virtualFile = getVirtualFile(fileName)
            val psiFileRef = Ref<PsiFile>()
            application.runReadAction {
                val psiManager = PsiManager.getInstance(project)
                val psiFile = psiManager.findFile(virtualFile) ?: throw VIException("Cannot find the PsiFile for $fileName.")
                psiFileRef.set(psiFile)
            }

            return psiFileRef.get()
        }
    }
}
