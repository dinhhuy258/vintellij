package com.dinhhuy258.vintellij.lsp.utils

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.codeEditor.JavaEditorFileSwapper
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.psi.KtDeclaration

fun Position.toOffset(doc: Document) = doc.getLineStartOffset(this.line) + this.character

fun Range.toTextRange(document: Document) =
    TextRange(
            this.start.toOffset(document),
            this.end.toOffset(document)
    )

fun offsetToPosition(document: Document, offset: Int): Position {
    if (offset == -1) {
        return Position(0, 0)
    }
    val line = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(line)
    val column = offset - lineStartOffset
    return Position(line, column)
}

fun getLocation(psiElement: PsiElement): Location? {
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
    val position = offsetToPosition(content, offset)
    return Range(position, position)
}

fun offsetToPosition(content: String, offset: Int): Position {
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
