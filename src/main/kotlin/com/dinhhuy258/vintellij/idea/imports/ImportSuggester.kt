package com.dinhhuy258.vintellij.idea.imports

import com.intellij.psi.PsiElement

interface ImportSuggester {
    fun collectSuggestions(element: PsiElement): List<String>
}
