package com.dinhhuy258.vintellij.buffer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
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
import org.eclipse.lsp4j.Position
import java.io.File

class BufferNotInProjectException(path: String, msg: String) :
    Exception("'$path' cannot be found in any opened projects.\n$msg")

// TODO: Refactor setText, replaceText methods
class Buffer(val project: Project, val path: String) {
    internal val psiFile: PsiFile
    internal val document: Document
    private var _editor: EditorDelegate? = null
    private lateinit var documentChangedListener: DocumentChangedListener
    val editor: EditorDelegate
        get() {
            ApplicationManager.getApplication().assertIsDispatchThread()
            val backed = _editor
            if (backed == null || backed.isDisposed) {
                _editor = createEditorDelegate()
            }
            return _editor!!
        }
    private val fileEditorManager: FileEditorManager

    init {
        psiFile = locateFile(path)
            ?: throw BufferNotInProjectException(path, "'locateFile' cannot locate the corresponding document.")

        document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: throw BufferNotInProjectException(path, "'PsiDocumentManager' cannot locate the corresponding document.")

        fileEditorManager = FileEditorManager.getInstance(project)
    }

    /**
     * Navigate to the editor of the buffer in the IDE without requesting focus.
     * So ideally the contents in both IDE and nvim should be synced from time to time.
     */
    fun navigate() {
        val selectedFiles = fileEditorManager.selectedFiles
        if (selectedFiles.isEmpty() || selectedFiles.first() != psiFile.virtualFile) {
            OpenFileDescriptor(project, psiFile.virtualFile).navigate(false)
        }
    }

    internal fun setDocumentChangedListener(documentChangedListener: DocumentChangedListener) {
        this.documentChangedListener = documentChangedListener
        document.addDocumentListener(documentChangedListener)
    }

    internal fun release() {
        document.removeDocumentListener(documentChangedListener)
        fileEditorManager.closeFile(psiFile.virtualFile)
    }

    fun moveCaretToPosition(row: Int, col: Int) {
        val caret = editor.editor.caretModel.currentCaret
        caret.moveToLogicalPosition(LogicalPosition(row, col))
    }

    internal fun replaceText(startPosition: Position, endPosition: Position, text: CharSequence) {
        onVimDocumentChange {
            ApplicationManager.getApplication().runWriteAction {
                WriteCommandAction.writeCommandAction(project)
                    .run<Throwable> {
                        val editor = this.editor.editor
                        val startOffset =
                            editor.logicalPositionToOffset(LogicalPosition(startPosition.line, startPosition.character))
                        val endOffset =
                            editor.logicalPositionToOffset(LogicalPosition(endPosition.line, endPosition.character))

                        document.replaceString(startOffset, endOffset, text)
                    }
            }
        }
    }

    internal fun insertText(position: Position, text: CharSequence) {
        onVimDocumentChange {
            ApplicationManager.getApplication().runWriteAction {
                WriteCommandAction.writeCommandAction(project)
                    .run<Throwable> {
                        val editor = this.editor.editor
                        val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

                        document.insertString(offset, text)
                    }
            }
        }
    }

    fun onVimDocumentChange(runnable: () -> Unit) {
        documentChangedListener.isChangedByVim = true
        try {
            runnable()
        } finally {
            documentChangedListener.isChangedByVim = false
        }

    }

    private fun locateFile(path: String): PsiFile? {
        var psiFile: PsiFile? = null
        ApplicationManager.getApplication().runReadAction {
            val files = FilenameIndex.getFilesByName(
                project, File(path).name, GlobalSearchScope.allScope(project)
            )
            psiFile = files.find {
                it.virtualFile.canonicalPath == path
            }
        }

        return psiFile
    }

    private fun createEditorDelegate(): EditorDelegate {
        val fileEditors = fileEditorManager.openFile(psiFile.virtualFile, false, true)
        val fileEditor = fileEditors.firstOrNull { it is TextEditor && it.editor is EditorEx }
            ?: throw BufferNotInProjectException(path, "FileEditorManger cannot open a TextEditor.")
        return EditorDelegate((fileEditor as TextEditor).editor as EditorEx)
    }
}
