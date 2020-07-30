package com.dinhhuy258.vintellij.idea.completions

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class JavaCompletion(onSuggest: (item: String, word: String, kind: CompletionKind, menu: String) -> Unit): AbstractCompletion(onSuggest) {
   override fun doCompletion(psiFile: PsiFile, offset: Int) {
      val application = ApplicationManager.getApplication()
      val project = IdeaUtils.getProject()
      val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return

      val basicCompletionHandler = VICodeCompletionHandler(CompletionType.BASIC, onSuggest)
      val classNameCompleteHandler = VICodeCompletionHandler(CompletionType.CLASS_NAME, onSuggest)

      application.invokeAndWait {
         val editor = EditorFactory.getInstance().createEditor(document, project)

         if (editor != null) {
            editor.caretModel.moveToOffset(offset)
            CommandProcessor.getInstance().executeCommand(project, {
               basicCompletionHandler.invokeCompletion(project, editor)
               classNameCompleteHandler.invokeCompletion(project, editor)
            }, null, null)
         }
      }
   }
}
