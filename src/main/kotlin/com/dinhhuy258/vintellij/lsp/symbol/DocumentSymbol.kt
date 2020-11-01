package com.dinhhuy258.vintellij.lsp.symbol

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.lsp.utils.symbolKind
import com.dinhhuy258.vintellij.lsp.utils.symbolName
import com.dinhhuy258.vintellij.lsp.utils.toRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.jsonrpc.messages.Either

private class DocumentSymbolVisitor(
    private val psiFile: PsiFile,
    private val onVisitElement: (PsiElement) -> Unit
) : PsiRecursiveElementVisitor() {
    fun visit() {
        visitElement(psiFile)
    }

    override fun visitElement(element: PsiElement) {
        onVisitElement(element)
        super.visitElement(element)
    }

    override fun visitFile(file: PsiFile) {
        throw UnsupportedOperationException()
    }
}

fun getDocumentSymbols(buffer: SyncBuffer?, documentUri: String): List<Either<SymbolInformation, DocumentSymbol>> {
    if (buffer == null) {
        return emptyList()
    }

    val symbols = mutableListOf<Either<SymbolInformation, DocumentSymbol>>()
    val document = buffer.document

    DocumentSymbolVisitor(buffer.psiFile) { element ->
        val kind = element.symbolKind()
        val name = element.symbolName()
        if (kind != null && name != null) {
            symbols.add(Either.forLeft(SymbolInformation(name, kind, Location(documentUri, element.toRange(document)), element.containerName())))
        }
    }.visit()

    return symbols
}

private fun PsiElement.containerName(): String? =
        generateSequence(parent, { it.parent })
                .firstOrNull { it.symbolKind() != null && it.symbolName() != null }
                ?.symbolName()
