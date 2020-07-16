package com.dinhhuy258.plugins.idea.files

import com.dinhhuy258.plugins.exceptions.FileNotFoundException
import com.dinhhuy258.plugins.exceptions.ProjectNotFoundException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File

class FileFinder {
    fun getPsiFile(fileName: String): PsiFile {
        val application = ApplicationManager.getApplication()
        val projects = ProjectManager.getInstance().openProjects
        if (projects.isEmpty()) {
            throw ProjectNotFoundException()
        }
        val project = projects[0]
        val file = File(FileUtil.toSystemIndependentName(fileName))
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $fileName!!!")
        }
        val virtualFileRef = Ref<VirtualFile>()
        application.invokeAndWait {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: throw FileNotFoundException("File not found: $fileName!!!")
            virtualFileRef.set(virtualFile)
        }
        val virtualFile = virtualFileRef.get()
        val psiFileRef = Ref<PsiFile>()
        application.runReadAction {
            val psiManager = PsiManager.getInstance(project!!)
            val psiFile = psiManager.findFile(virtualFile) ?: throw FileNotFoundException("Cannot find the PsiFile for $fileName!!!")
            psiFileRef.set(psiFile)
        }

        return psiFileRef.get()
    }
}