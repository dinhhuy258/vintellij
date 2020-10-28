package com.dinhhuy258.vintellij.lsp.navigation

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.codeInsight.navigation.GotoImplementationHandler
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Comparing
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.psi.KtDeclaration

@Synchronized
fun goToImplementation(buffer: SyncBuffer?, position: Position): List<Location> {
    val locations = ArrayList<Location>()

    if (buffer != null) {
        ApplicationManager.getApplication().invokeAndWait {
            buffer.moveCaretToPosition(position.line, position.character)
            val editor = buffer.editor
            val gotoImplementationHandler = GotoImplementationHandler()
            val gotoData = gotoImplementationHandler.getSourceAndTargetElements(editor.editor, buffer.psiFile)
            gotoData?.targets?.forEach { target ->
                val location = getLocation(target)
                if (location != null) {
                    locations.add(location)
                }
            }
        }
    }

    return locations
}

private fun getLocation(psiElement: PsiElement): Location? {
    val psiFile = psiElement.containingFile
    val virtualFile = psiFile.virtualFile
    if (virtualFile.fileType is JavaClassFileType && psiElement !is KtDeclaration) {
        val project = IdeaUtils.getProject()
        val sourceFile = JavaEditorFileSwapper.findSourceFile(project, virtualFile)
        if (sourceFile != null) {
            // Recalculate the text offset for source file
            val member = PsiTreeUtil.getParentOfType(psiElement, PsiMember::class.java, false)
            val position = Position(0, 0)
            var range = Range(position, position)
            if (member != null) {
                val navigationElement = member.originalElement.navigationElement
                if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                    val content = navigationElement.containingFile.text
                    range = getRange(content, navigationElement.textOffset)
                }
            }

            return Location(PathUtils.toVimJarFilePath(sourceFile.path), range)
        }

        return null
    } else if ((virtualFile.fileType is KotlinBuiltInFileType || virtualFile.fileType is JavaClassFileType) && psiElement is KtDeclaration) {
        val sourceFile = SourceNavigationHelper.getNavigationElement(psiElement).containingFile.virtualFile
        if (sourceFile != null) {
            // Recalculate the text offset for source file
            val position = Position(0, 0)
            var range = Range(position, position)
            val navigationElement = psiElement.navigationElement
            if (navigationElement != null && Comparing.equal(navigationElement.containingFile.virtualFile, sourceFile)) {
                val content = navigationElement.containingFile.text
                range = getRange(content, navigationElement.textOffset)
            }
            return Location(PathUtils.toVimJarFilePath(sourceFile.path), range)
        }

        return null
    }

    if (PathUtils.isIntellijJarFile(virtualFile.path)) {
        return Location(PathUtils.toVimJarFilePath(virtualFile.path), getRange(psiFile.text, psiElement.textOffset))
    }

    return Location(virtualFile.path, getRange(psiFile.text, psiElement.textOffset))
}

fun getRange(content: String, offset: Int): Range {
    val position = getPosition(content, offset)
    return Range(position, position)
}

fun getPosition(content: String, offset: Int): Position {
    val reader = content.reader()
    var line = 0
    var char = 0

    var find = 0
    while (find < offset) {
        val nextChar = reader.read()

        if (nextChar == -1) {
            throw RuntimeException("Reached end of file before reaching offset $offset")
        }

        find++
        char++

        if (nextChar.toChar() == '\n') {
            line++
            char = 0
        }
    }

    return Position(line, char)
}
