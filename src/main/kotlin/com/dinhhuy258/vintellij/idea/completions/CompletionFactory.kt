package com.dinhhuy258.vintellij.idea.completions

import com.dinhhuy258.vintellij.exceptions.VIException
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.idea.KotlinLanguage

class CompletionFactory {
    companion object {
        fun createCompletion(language: Language,
                             onSuggest: (word: String, kind: CompletionKind, menu: String) -> Unit) = when (language) {
            KotlinLanguage.INSTANCE -> KtCompletion(onSuggest)
            JavaLanguage.INSTANCE -> JavaCompletion(onSuggest)
            else -> throw VIException("Language not support: " + language.displayName)
        }
    }
}
