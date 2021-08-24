package com.dinhhuy258.vintellij.lsp.hover

import com.dinhhuy258.vintellij.lsp.Buffer
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.application.ApplicationManager
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
    ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runReadAction {
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

// TODO: Move this method to utils class
fun stripHtml(input: String): String {
    return input.replace("(<br[ /]*>|</?PRE>)".toRegex(), "\n")
            .replace("<li>".toRegex(), "\n - ")
            .replace("<style .*style>".toRegex(), "")
            .replace("<[^>]+>".toRegex(), "")
            .replace("&nbsp;".toRegex(), " ")
            .replace("&amp;".toRegex(), "&")
            .replace("&lt;".toRegex(), "<")
            .replace("&gt;".toRegex(), ">")
            .trim { it <= ' ' }
}
