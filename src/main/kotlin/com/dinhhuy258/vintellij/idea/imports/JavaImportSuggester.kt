package com.dinhhuy258.vintellij.idea.imports

import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName

class JavaImportSuggester : ImportSuggester {
    override fun collectSuggestions(element: PsiElement): List<String> {
        val reference = element.reference ?: return emptyList()
        if (reference !is PsiJavaCodeReferenceElement || reference.qualifier != null) {
            return emptyList()
        }

        val importCandidates = ArrayList<String>()
        val importClassFix = ImportClassFix(reference)
        importClassFix.classesToImport.forEach { psiClass ->
            val fqName = psiClass.getKotlinFqName() ?: return@forEach
            importCandidates.push("import ${fqName.asString()};")
        }

        return importCandidates
    }
}
