package com.dinhhuy258.vintellij.lsp.quickfix.imports

import com.intellij.psi.PsiElement

interface ImportSuggester {
    fun collectSuggestions(element: PsiElement): List<String>
}
