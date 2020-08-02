package com.dinhhuy258.vintellij.idea

import com.dinhhuy258.vintellij.exceptions.VIException
import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiJavaFileImpl
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClassWithBuiltinMapping
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
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

        fun findPackageForPsiClass(psiClass: PsiClass): String? {
            return psiClass.getJavaClassDescriptor()?.findPackage()?.fqName?.asString()
        }

        fun getVirtualFile(fileName: String): VirtualFile {
            if (PathUtils.isVimJarFilePath(fileName)) {
                return VirtualFileManager.getInstance().refreshAndFindFileByUrl(PathUtils.toIntellijJarFilePath(fileName))
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
            val application = ApplicationManager.getApplication()
            val psiFileRef = Ref<PsiFile>()

            application.runReadAction {
                psiFileRef.set(virtualFile.toPsiFile(project))
            }

            return psiFileRef.get() ?: throw VIException("Cannot find the PsiFile for $fileName.")
        }

        fun getPsiClass(psiElement: PsiElement): PsiClass? {
            return when (psiElement) {
                is PsiClass -> {
                    psiElement
                }
                is KtClassOrObject -> {
                    psiElement.toLightClassWithBuiltinMapping()
                }
                else -> {
                    null
                }
            }
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

        fun getClassName(psiFile: PsiFile): String? {
            return when (psiFile) {
                is KtFile -> {
                    psiFile.classes[0].getKotlinFqName()?.asString()
                }
                is PsiJavaFileImpl -> {
                    psiFile.classes[0].getKotlinFqName()?.asString()
                }
                else -> {
                    null
                }
            }
        }
    }
}
