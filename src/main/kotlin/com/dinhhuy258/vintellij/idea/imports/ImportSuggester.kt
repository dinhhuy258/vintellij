package com.dinhhuy258.vintellij.idea.imports

import org.jetbrains.kotlin.psi.KtSimpleNameExpression

interface ImportSuggester {
    fun collectSuggestions(element: KtSimpleNameExpression): List<String>
}
