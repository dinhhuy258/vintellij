package com.dinhhuy258.plugins.idea.imports

import org.jetbrains.kotlin.psi.KtSimpleNameExpression

interface ImportSuggester {
    fun collectSuggestions(element: KtSimpleNameExpression): List<String>
}
