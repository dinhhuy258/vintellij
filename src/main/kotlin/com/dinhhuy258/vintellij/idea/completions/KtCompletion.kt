package com.dinhhuy258.vintellij.idea.completions

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

class KtCompletion(onSuggest: (item: String, word: String, kind: CompletionKind, menu: String) -> Unit): AbstractCompletion(onSuggest) {
   override fun doCompletion(psiFile: PsiFile, offset: Int) {
       val ktFile = psiFile as KtFile
   }
}
