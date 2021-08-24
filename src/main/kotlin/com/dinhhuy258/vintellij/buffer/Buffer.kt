package com.dinhhuy258.vintellij.buffer

import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import java.io.File
import org.eclipse.lsp4j.Position

class BufferNotInProjectException(path: String, msg: String) :
    Exception("'$path' cannot be found in any opened projects.\n$msg")

// TODO: Refactor setText, replaceText methods
class Buffer(val path: String) {
    internal val psiFile: PsiFile
    internal val document: Document
    private var _editor: EditorDelegate? = null
    val editor: EditorDelegate
        get() {
            ApplicationManager.getApplication().assertIsDispatchThread()
            val backed = _editor
            if (backed == null || backed.isDisposed) {
                _editor = createEditorDelegate()
            }
            return _editor!!
        }
    val project: Project
    private val fileEditorManager: FileEditorManager

    init {
        val pair = locateFile(path)
            ?: throw BufferNotInProjectException(path, "'locateFile' cannot locate the corresponding document.")
        project = pair.first
        psiFile = pair.second

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

    internal fun replaceText(startPosition: Position, endPosition: Position, text: CharSequence) {
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                .run<Throwable> {
                    val editor = this.editor.editor
                    val startOffset = editor.logicalPositionToOffset(LogicalPosition(startPosition.line, startPosition.character))
                    val endOffset = editor.logicalPositionToOffset(LogicalPosition(endPosition.line, endPosition.character))

                    document.replaceString(startOffset, endOffset, text)
                }
        }
    }

    internal fun insertText(position: Position, text: CharSequence) {
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                .run<Throwable> {
                    val editor = this.editor.editor
                    val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

                    document.insertString(offset, text)
                }
        }
    }

    fun insertText(text: CharSequence, line: Int) {
        val offset = document.getLineStartOffset(line)
        ApplicationManager.getApplication().invokeAndWait {
            insertText(offset, text)
        }
    }

    private fun insertText(offset: Int, text: CharSequence) {
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                .run<Throwable> {
                    document.insertString(offset, text)
                }
        }
    }

    private fun locateFile(name: String): Pair<Project, PsiFile>? {
        val filePath = if (PathUtils.isVimJarFilePath(name)) {
            PathUtils.toIntellijJarFilePath(name).removePrefix(PathUtils.INTELLIJ_PATH_PREFIX)
        } else {
            name
        }

        var ret: Pair<Project, PsiFile>? = null
        ApplicationManager.getApplication().runReadAction {
            val projectManager = ProjectManager.getInstance()
            val projects = projectManager.openProjects
            projects.forEach { project ->
                val files = com.intellij.psi.search.FilenameIndex.getFilesByName(
                    project, File(name).name, GlobalSearchScope.allScope(project))
                val psiFile = files.find {
                    it.virtualFile.canonicalPath == filePath
                }
                if (psiFile != null) {
                    ret = project to psiFile
                    return@runReadAction
                }
            }
        }
        return ret
    }

    private fun createEditorDelegate(): EditorDelegate {
        val fileEditors = fileEditorManager.openFile(psiFile.virtualFile, false, true)
        val fileEditor = fileEditors.firstOrNull { it is TextEditor && it.editor is EditorEx }
            ?: throw BufferNotInProjectException(path, "FileEditorManger cannot open a TextEditor.")
        return EditorDelegate((fileEditor as TextEditor).editor as EditorEx)
    }
}
