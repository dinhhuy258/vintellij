package com.dinhhuy258.vintellij.comrade.buffer

import com.dinhhuy258.vintellij.comrade.core.NvimInstance
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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.eclipse.lsp4j.Position
import java.io.File

class BufferNotInProjectException(path: String, msg: String) :
        Exception("'$path' cannot be found in any opened projects.\n$msg")

class SyncBuffer(
    val id: Int,
    val path: String,
    val nvimInstance: NvimInstance
) {

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

    val isSynchronizable: Boolean
    val project: Project
    val text get() = document.text
    var isReleased: Boolean = false
        private set

    internal lateinit var synchronizer: Synchronizer

    private val fileEditorManager: FileEditorManager

    init {
        val pair = locateFile(path)
                ?: throw BufferNotInProjectException(path, "'locateFile' cannot locate the corresponding document.")
        project = pair.first
        psiFile = pair.second

        isSynchronizable = (when (ApplicationManager.getApplication().isUnitTestMode) {
            true -> TempFileSystem.getInstance().findFileByPath(path)
            false -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(path))
        }) != null && psiFile.isWritable

        document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: throw BufferNotInProjectException(path, "'PsiDocumentManager' cannot locate the corresponding document.")

        fileEditorManager = FileEditorManager.getInstance(project)
    }

    private fun createEditorDelegate(): EditorDelegate {
        val fileEditors = fileEditorManager.openFile(psiFile.virtualFile, false, true)
        val fileEditor = fileEditors.firstOrNull { it is TextEditor && it.editor is EditorEx }
                ?: throw BufferNotInProjectException(path, "FileEditorManger cannot open a TextEditor.")
        return EditorDelegate((fileEditor as TextEditor).editor as EditorEx)
    }

    /**
     * Navigate to the editor of the buffer in the IDE without requesting focus.
     * So ideally the contents in both IDE and nvim should be synced from time to time.
     */
    fun navigate() {
        checkReleased()
        val selectedFiles = fileEditorManager.selectedFiles
        if (selectedFiles.isEmpty() || selectedFiles.first() != psiFile.virtualFile) {
            OpenFileDescriptor(project, psiFile.virtualFile).navigate(false)
        }
    }

    fun moveCaretToPosition(row: Int, col: Int) {
        checkReleased()
        val caret = editor.editor.caretModel.currentCaret
        caret.moveToLogicalPosition(LogicalPosition(row, col))
    }

    fun insertText(text: CharSequence, line: Int) {
        checkReleased()
        val offset = document.getLineStartOffset(line)
        ApplicationManager.getApplication().invokeAndWait {
            insertText(offset, text)
        }
    }

    internal fun setText(text: CharSequence) {
        checkReleased()
        ApplicationManager.getApplication().runWriteAction {
            document.setText(text)
        }
    }

    internal fun replaceText(startOffset: Int, endOffset: Int, text: CharSequence) {
        checkReleased()
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                    .run<Throwable> {
                        document.replaceString(startOffset, endOffset, text)
                    }
        }
    }

    internal fun replaceText(startPosition: Position, endPosition: Position, text: CharSequence) {
        checkReleased()
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
        checkReleased()
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                .run<Throwable> {
                    val editor = this.editor.editor
                    val offset = editor.logicalPositionToOffset(LogicalPosition(position.line, position.character))

                    document.insertString(offset, text)
                }
        }
    }


    internal fun insertText(offset: Int, text: CharSequence) {
        checkReleased()
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                    .run<Throwable> {
                        document.insertString(offset, text)
                    }
        }
    }

    internal fun deleteText(start: Int, end: Int) {
        checkReleased()
        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.writeCommandAction(project)
                    .run<Throwable> {
                        document.deleteString(start, end)
                    }
        }
    }

    internal fun attachSynchronizer(synchronizer: Synchronizer) {
        this.synchronizer = synchronizer
        document.addDocumentListener(synchronizer)
        synchronizer.initFromJetBrain()
    }

    /**
     * Use [SyncBufferManager.releaseBuffer] to dispose the [SyncBuffer].
     */
    internal fun release() {
        if (isReleased) return
        isReleased = true
        // FIXME: Do we need it to sync between Intellij and Vim?
//        document.removeDocumentListener(synchronizer)
        fileEditorManager.closeFile(psiFile.virtualFile)
    }

    private fun checkReleased() {
        if (isReleased) throw IllegalStateException("This SyncBuffer has been released already: $path")
    }

    override fun toString(): String {
        return "bufId: $id, $path"
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
