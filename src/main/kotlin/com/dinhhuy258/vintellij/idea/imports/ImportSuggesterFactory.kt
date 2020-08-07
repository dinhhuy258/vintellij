package com.dinhhuy258.vintellij.idea.imports

import com.dinhhuy258.vintellij.exceptions.VIException
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.idea.KotlinLanguage

class ImportSuggesterFactory {
    companion object {
        fun createImportSuggester(
            language: Language
        ) = when (language) {
            KotlinLanguage.INSTANCE -> KtImportSuggester()
            JavaLanguage.INSTANCE -> JavaImportSuggester()
            else -> throw VIException("Language not support: " + language.displayName)
        }
    }
}
