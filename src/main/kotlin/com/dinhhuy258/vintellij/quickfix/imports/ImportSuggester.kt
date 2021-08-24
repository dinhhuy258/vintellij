package com.dinhhuy258.vintellij.quickfix.imports

import com.intellij.psi.PsiElement

interface ImportSuggester {
    fun collectSuggestions(element: PsiElement): List<String>
}
