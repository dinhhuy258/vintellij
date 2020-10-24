package com.dinhhuy258.vintellij.lsp.buffer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.File

class Buffer(private val path: String, private val project: Project) {
    private val psiFile: PsiFile

    private val fileEditorManager: FileEditorManager

    private val document: Document

    private var _editor: VintellijEditor? = null
    val editor: VintellijEditor
        get() {
            ApplicationManager.getApplication().assertIsDispatchThread()
            val backed = _editor
            if (backed == null || backed.isDisposed) {
                _editor = createVintellijEditor()
            }
            return _editor!!
        }

    init {
        val application = ApplicationManager.getApplication()
        var foundPsiFile: PsiFile? = null
        application.runReadAction {
            val files = FilenameIndex.getFilesByName(project, File(path).name, GlobalSearchScope.allScope(project))
            foundPsiFile = files.find {
                it.virtualFile.canonicalPath == path
            }

            return@runReadAction
        }
        if (foundPsiFile == null) {
            throw BufferNotFoundException()
        }
        psiFile = foundPsiFile as PsiFile
        fileEditorManager = FileEditorManager.getInstance(project)
        document = PsiDocumentManager.getInstance(project).getDocument(psiFile!!) ?: throw BufferNotFoundException()

        navigate()
    }

    fun release() {
        fileEditorManager.closeFile(psiFile.virtualFile)
    }

    fun moveCaretToPosition(row: Int, col: Int) {
        val caret = editor.editor.caretModel.currentCaret
        caret.moveToLogicalPosition(LogicalPosition(row, col))
    }

    fun getPsiFile(): PsiFile {
        return psiFile
    }

    fun getDocument(): Document {
        return document
    }

    fun getProject(): Project {
        return project
    }

    fun navigate() {
        val selectedFiles = fileEditorManager.selectedFiles
        if (selectedFiles.isEmpty() || selectedFiles.first() != psiFile.virtualFile) {
            OpenFileDescriptor(project, psiFile.virtualFile).navigate(false)
        }
    }

    private fun createVintellijEditor(): VintellijEditor {
        val fileEditors = fileEditorManager.openFile(psiFile.virtualFile, false, true)
        val fileEditor = fileEditors.firstOrNull { it is TextEditor && it.editor is EditorEx }
                ?: throw BufferNotFoundException("FileEditorManger cannot open a TextEditor.")

        return VintellijEditor((fileEditor as TextEditor).editor as EditorEx)
    }
}
