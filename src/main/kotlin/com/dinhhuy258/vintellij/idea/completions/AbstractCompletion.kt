package com.dinhhuy258.vintellij.idea.completions

import com.intellij.psi.PsiFile

abstract class AbstractCompletion(protected val onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) {
    abstract fun doCompletion(psiFile: PsiFile, offset: Int, prefix: String)
}
