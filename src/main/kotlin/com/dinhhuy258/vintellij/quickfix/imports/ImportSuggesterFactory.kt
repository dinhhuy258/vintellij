package com.dinhhuy258.vintellij.quickfix.imports

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.idea.KotlinLanguage

class LanguageNotSupported(message: String) : Exception(message)

class ImportSuggesterFactory {
    companion object {
        fun createImportSuggester(
            language: Language
        ) = when (language) {
            KotlinLanguage.INSTANCE -> KtImportSuggester()
            JavaLanguage.INSTANCE -> JavaImportSuggester()
            else -> throw LanguageNotSupported("Language not support: " + language.displayName)
        }
    }
}
