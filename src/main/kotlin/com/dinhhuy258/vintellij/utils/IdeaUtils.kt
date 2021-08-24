package com.dinhhuy258.vintellij.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import java.io.File
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFunction

class ResourceNotFoundException(message: String) : Exception(message)

class IdeaUtils {
    companion object {
        fun getProject(): Project {
            val projects = ProjectManager.getInstance().openProjects
            if (projects.isEmpty()) {
                throw ResourceNotFoundException("Project not found. Please open a project on intellij.")
            }

            return projects[0]
        }

        fun getPsiMethod(psiElement: PsiElement): PsiMethod? {
            return when (psiElement) {
                is KtFunction -> {
                    psiElement.getRepresentativeLightMethod()
                }
                is PsiMethod -> {
                    psiElement
                }
                else -> {
                    null
                }
            }
        }

        fun invokeOnMainAndWait(exceptionHandler: ((Throwable) -> Unit)? = null, runnable: () -> Unit) {
            var throwable: Throwable? = null
            ApplicationManager.getApplication().invokeAndWait {
                try {
                    runnable.invoke()
                } catch (t: Throwable) {
                    throwable = t
                }
            }
            val toThrow = throwable ?: return
            if (exceptionHandler == null) {
                throw toThrow
            } else {
                exceptionHandler.invoke(toThrow)
            }
        }

        private fun getVirtualFile(fileName: String): VirtualFile {
            if (PathUtils.isVimJarFilePath(fileName)) {
                return VirtualFileManager.getInstance().refreshAndFindFileByUrl(PathUtils.toIntellijJarFilePath(fileName))
                    ?: throw ResourceNotFoundException("Virtual file not found: $fileName.")
            }

            val application = ApplicationManager.getApplication()
            val file = File(FileUtil.toSystemDependentName(fileName))
            if (!file.exists()) {
                throw ResourceNotFoundException("File not found: $fileName.")
            }
            val virtualFileRef = Ref<VirtualFile>()
            application.invokeAndWait {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    ?: throw ResourceNotFoundException("Virtual file not found: $fileName.")
                virtualFileRef.set(virtualFile)
            }

            return virtualFileRef.get()
        }
    }
}
