package com.dinhhuy258.vintellij.utils

import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.psi.KtDeclaration

class PathUtils private constructor() {
    companion object {
        const val INTELLIJ_PATH_PREFIX = "jar://"
        private const val VIM_FILE_PATH_PREFIX = "file://"
        private const val VIM_ZIPFILE_PATH_PREFIX = "zipfile://"
        private const val INTELLIJ_JAR_SEPARATOR = ".jar!/"
        private const val VIM_JAR_SEPARATOR = ".jar::"
        private const val INTELLIJ_ZIP_SEPARATOR = ".zip!/"
        private const val VIM_ZIP_SEPARATOR = ".zip::"

        fun isVimJarFilePath(filePath: String): Boolean = filePath.startsWith(VIM_ZIPFILE_PATH_PREFIX)

        fun toIntellijJarFilePath(filePath: String): String {
            val path = filePath.replaceFirst(VIM_ZIPFILE_PATH_PREFIX, INTELLIJ_PATH_PREFIX)

            return if (path.contains(VIM_JAR_SEPARATOR)) {
                path.replaceFirst(VIM_JAR_SEPARATOR, INTELLIJ_JAR_SEPARATOR)
            } else {
                path.replaceFirst(VIM_ZIP_SEPARATOR, INTELLIJ_ZIP_SEPARATOR)
            }
        }

        fun getPathWithOffsetFromVirtualFileAndPsiElement(virtualFile: VirtualFile, psiElement: PsiElement): Pair<String, Int>? {
            if (virtualFile.fileType is JavaClassFileType && psiElement !is KtDeclaration) {
                val project = IdeaUtils.getProject()
                val sourceFile = JavaEditorFileSwapper.findSourceFile(project, virtualFile)
                if (sourceFile != null) {
                    // Recalculate the text offset for source file
                    val member = PsiTreeUtil.getParentOfType(psiElement, PsiMember::class.java, false)
                    var textOffset = 0
                    if (member != null) {
                        val navigationElement = member.originalElement.navigationElement
                        if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                            textOffset = navigationElement.textOffset
                        }
                    }
                    return Pair(toVimJarFilePath(sourceFile.path), textOffset)
                }

                return null
            } else if ((virtualFile.fileType is KotlinBuiltInFileType || virtualFile.fileType is JavaClassFileType) && psiElement is KtDeclaration) {
                val sourceFile = SourceNavigationHelper.getNavigationElement(psiElement).containingFile.virtualFile
                if (sourceFile != null) {
                    // Recalculate the text offset for source file
                    var textOffset = 0
                    val navigationElement = psiElement.navigationElement
                    if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                        textOffset = navigationElement.textOffset
                    }
                    return Pair(toVimJarFilePath(sourceFile.path), textOffset)
                }

                return null
            }

            if (isIntellijJarFile(virtualFile.path)) {
                return Pair(toVimJarFilePath(virtualFile.path), psiElement.textOffset)
            }
            return Pair(virtualFile.path, psiElement.textOffset)
        }

        fun isIntellijJarFile(filePath: String) = filePath.contains(INTELLIJ_JAR_SEPARATOR) || filePath.contains(INTELLIJ_ZIP_SEPARATOR)

        fun toVimJarFilePath(filePath: String): String {
            val path = if (filePath.contains(INTELLIJ_JAR_SEPARATOR)) {
                filePath.replaceFirst(INTELLIJ_JAR_SEPARATOR, VIM_JAR_SEPARATOR)
            } else {
                filePath.replaceFirst(INTELLIJ_ZIP_SEPARATOR, VIM_ZIP_SEPARATOR)
            }

            return "$VIM_ZIPFILE_PATH_PREFIX$path"
        }

        fun toVimFilePath(filePath: String): String {
           return "$VIM_FILE_PATH_PREFIX$filePath"
        }
    }
}
