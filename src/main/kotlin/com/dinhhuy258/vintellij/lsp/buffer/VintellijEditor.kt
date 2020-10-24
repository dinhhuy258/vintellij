package com.dinhhuy258.vintellij.lsp.buffer

import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException
import javax.swing.JComponent

@Suppress("CanBeParameter")
class NotSupportedByUIDelegateException(val editor: EditorEx, val methodName: String) :
        NotSupportedException("$methodName is not supported by EditorDelegate for ${editor.virtualFile.name}")

class VintellijEditor(val editor: EditorEx) : Editor by editor {

    override fun getComponent(): JComponent {
        throw NotSupportedByUIDelegateException(editor, "getComponent")
    }

    override fun getContentComponent(): JComponent {
        throw NotSupportedByUIDelegateException(editor, "getContentComponent")
    }

    override fun getCaretModel(): CaretModel {
        throw NotSupportedByUIDelegateException(editor, "getCaretModel")
    }
}
