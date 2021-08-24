package com.dinhhuy258.vintellij.symbol

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.containerName
import com.dinhhuy258.vintellij.utils.symbolKind
import com.dinhhuy258.vintellij.utils.toRange
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.java.JavaStructureViewBuilderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Key
import com.intellij.pom.Navigatable
import java.beans.PropertyChangeListener
import java.util.*
import javax.swing.JComponent
import kotlin.collections.HashMap
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.structureView.KotlinStructureViewFactory

private class VintellijFileEditor(private val editor: Editor) : TextEditor {
    private val userDataMap = HashMap<Any, Any?>()

    override fun getEditor(): Editor {
        return this.editor
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        return userDataMap[key] as T
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        userDataMap[key] = value
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun getComponent(): JComponent {
        TODO("Not yet implemented")
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun setState(state: FileEditorState) {
        TODO("Not yet implemented")
    }

    override fun isModified(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        TODO("Not yet implemented")
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        TODO("Not yet implemented")
    }

    override fun canNavigateTo(navigatable: Navigatable): Boolean {
        TODO("Not yet implemented")
    }

    override fun navigateTo(navigatable: Navigatable) {
        TODO("Not yet implemented")
    }
}

fun getDocumentSymbols(buffer: Buffer?, documentUri: String): List<Either<SymbolInformation, DocumentSymbol>> {
    if (buffer == null) {
        return emptyList()
    }

    val symbols = mutableListOf<Either<SymbolInformation, DocumentSymbol>>()
    val psiFile = buffer.psiFile
    if (psiFile.language != KotlinLanguage.INSTANCE && psiFile.language != JavaLanguage.INSTANCE) {
        return symbols
    }
    val document = buffer.document

    ApplicationManager.getApplication().invokeAndWait {
        val structureViewFactory = if (psiFile.language == KotlinLanguage.INSTANCE) {
            KotlinStructureViewFactory()
        } else {
            JavaStructureViewBuilderFactory()
        }
        val structureViewBuilder = structureViewFactory.getStructureViewBuilder(buffer.psiFile) ?: return@invokeAndWait
        val fileEditor = VintellijFileEditor(buffer.editor.editor)
        val structureView = structureViewBuilder.createStructureView(fileEditor, buffer.project)
        val treeModel = structureView.treeModel
        val root = treeModel.root
        val treeElements = LinkedList<TreeElement>()
        treeElements.addAll(root.children)

        while (!treeElements.isEmpty()) {
            val treeElement = treeElements.pop()
            if (treeElement is PsiTreeElementBase<*>) {
                val psiTreeElement = treeElement as PsiTreeElementBase<*>
                val element = psiTreeElement.element
                if (element != null) {
                    symbols.add(Either.forLeft(
                            SymbolInformation(
                                    psiTreeElement.presentation.presentableText,
                                    element.symbolKind(),
                                    Location(documentUri, element.toRange(document)),
                                    element.containerName()
                            )
                    ))
                }
            }

            treeElements.addAll(treeElement.children)
        }
    }

    return symbols
}
