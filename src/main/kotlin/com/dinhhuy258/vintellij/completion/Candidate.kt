package com.dinhhuy258.vintellij.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation

class Candidate(lookupElement: LookupElement) : LookupElementPresentation() {
    init {
        lookupElement.renderElement(this)
    }

    // This should be added as candidate
    val valuable: Boolean get() {
        // When isTypedGrayed is true, for at least IDEA, it is the snippet completion.
        return itemText != null && itemText!!.isNotBlank() && !isTypeGrayed
    }

    override fun isReal(): Boolean {
        return false
    }
}
