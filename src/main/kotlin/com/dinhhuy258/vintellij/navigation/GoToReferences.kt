package com.dinhhuy258.vintellij.navigation

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.IdeaUtils
import com.dinhhuy258.vintellij.utils.offsetToPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScopeBuilder
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.suggested.startOffset
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.psi.KtFunction

fun goToReferences(buffer: Buffer?, position: Position): List<Location> {
    if (buffer == null) {
        return emptyList()
    }

    val locationsRef = Ref<List<Location>>()

    val application = ApplicationManager.getApplication()
    application.invokeAndWait {
        application.runReadAction {
            val psiFile = buffer.psiFile
            val editor = buffer.editor.editor
            val project = buffer.project
            val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

            val psiElement = psiFile.findElementAt(offset)?.context ?: return@runReadAction
            val scope = ProjectScopeBuilder.getInstance(project).buildProjectScope()

            if (psiElement is KtFunction || psiElement is PsiMethod) {
                val psiMethod = getPsiMethod(psiElement) ?: return@runReadAction

                val locations = MethodReferencesSearch.search(psiMethod, scope, true).mapNotNull {
                    val referenceMethod = it.element.parent
                    getReference(referenceMethod) ?: return@mapNotNull null
                }
                locationsRef.set(locations)
            } else {
                ReferencesSearch.search(ReferencesSearch.SearchParameters(psiElement, scope, false)).mapNotNull {
                    val referenceElement = it.element.parent
                    getReference(referenceElement) ?: return@mapNotNull null
                }
            }
        }
    }

    return locationsRef.get()
}

private fun getReference(psiElement: PsiElement): Location? {
    val referenceFile = psiElement.containingFile ?: return null
    val referenceDocument = PsiDocumentManager.getInstance(psiElement.project).getDocument(referenceFile)
        ?: return null

    val filePath = referenceFile.virtualFile.path
    val offset = psiElement.startOffset

    val position = offsetToPosition(referenceDocument, offset)
    return Location(filePath, Range(position, position))
}

private fun getPsiMethod(psiElement: PsiElement): PsiMethod? {
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

