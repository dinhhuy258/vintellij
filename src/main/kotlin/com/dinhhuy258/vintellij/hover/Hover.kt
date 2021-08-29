package com.dinhhuy258.vintellij.hover

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.runReadAction
import com.dinhhuy258.vintellij.utils.stripHtml
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.Ref
import org.apache.commons.lang3.StringUtils.isBlank
import org.eclipse.lsp4j.MarkedString
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.jsonrpc.messages.Either

fun getHoverDoc(buffer: Buffer?, position: Position): List<Either<String, MarkedString>> {
    if (buffer == null) {
        return emptyList()
    }

    val hoverDocRef = Ref<String>()
    invokeAndWait {
        runReadAction {
            val psiFile = buffer.psiFile
            val editor = buffer.editor.editor
            val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

            val psiReference = psiFile.findReferenceAt(offset) ?: return@runReadAction
            val psiElement = psiReference.resolve() ?: return@runReadAction
            val originalElement = psiReference.element
            val documentationManager = DocumentationManager.getProviderFromElement(originalElement)
            val hoverDoc = documentationManager.generateHoverDoc(psiElement, originalElement)
            hoverDocRef.set(hoverDoc)
        }
    }
    val hoverDoc = hoverDocRef.get()
    if (isBlank(hoverDoc)) {
        return emptyList()
    }

    val strippedHoverDoc = stripHtml(hoverDoc)
    val docs = strippedHoverDoc.split("\\r?\\n")

    return docs.map {
        Either.forLeft(it)
    }
}
