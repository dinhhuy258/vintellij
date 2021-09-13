package com.dinhhuy258.vintellij.buffer

import com.dinhhuy258.vintellij.utils.runReadAction
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
import java.io.File
import org.eclipse.lsp4j.Position

class BufferNotInProjectException(path: String, msg: String) :
    Exception("'$path' cannot be found in any opened projects.\n$msg")

class Buffer(val project: Project, val path: String) {
    internal val psiFile: PsiFile
    internal val document: Document
    private var _editor: EditorDelegate? = null
    val editor: EditorDelegate
        get() {
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

    internal fun release() {
        fileEditorManager.closeFile(psiFile.virtualFile)
    }

    fun moveCaretToPosition(row: Int, col: Int) {
        val caret = editor.editor.caretModel.currentCaret
        caret.moveToLogicalPosition(LogicalPosition(row, col))
    }

    /**
     * Replaces the specified range of text in the document with the specified string.
     * This method MUST be called in the write action block
     */
    internal fun replaceText(startPosition: Position, endPosition: Position, text: CharSequence) {
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

    /**
     * Set text for the document.
     * This method MUST be called in the write action block
     */
    internal fun setText(text: String) {
        WriteCommandAction.writeCommandAction(project)
            .run<Throwable> {
                document.setText(text)
            }
    }

    /**
     * Deletes the specified range of text from the document.
     * This method MUST be called in the write action block
     */
    internal fun deleteText(startPosition: Position, endPosition: Position) {
        WriteCommandAction.writeCommandAction(project)
            .run<Throwable> {
                val editor = this.editor.editor
                val startOffset =
                    editor.logicalPositionToOffset(LogicalPosition(startPosition.line, startPosition.character))
                val endOffset =
                    editor.logicalPositionToOffset(LogicalPosition(endPosition.line, endPosition.character))

                document.deleteString(startOffset, endOffset)
            }
    }

    /**
     * Inserts the specified text at the specified offset in the document
     * This method MUST be called in the write action block
     */
    internal fun insertText(position: Position, text: CharSequence) {
        WriteCommandAction.writeCommandAction(project)
            .run<Throwable> {
                val editor = this.editor.editor
                val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

                document.insertString(offset, text)
            }
    }

    private fun locateFile(path: String): PsiFile? {
        var psiFile: PsiFile? = null
        runReadAction {
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
