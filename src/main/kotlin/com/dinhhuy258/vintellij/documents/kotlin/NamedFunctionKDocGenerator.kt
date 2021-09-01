package com.dinhhuy258.vintellij.documents.kotlin

import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNamedFunction

class NamedFunctionKDocGenerator(private val element: KtNamedFunction) :
    KDocGenerator {
    override fun generate(): String {

        val builder = StringBuilder()
        val nameToPhrase = nameToPhrase(element.name ?: "Function")
        builder.appendLine("/**")
            .appendLine("* $nameToPhrase")
            .appendLine("*")

        if (element.typeParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = element.typeParameters))
        }
        if (element.valueParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = element.valueParameters))
            element.valueParameters.forEach end@{
                if (it.typeReference != null && it.typeReference?.typeElement is KtFunctionType) {
                    builder.appendLine("* @receiver")
                    return@end
                }
            }
        }
        element.typeReference?.let {
            if (it.text != "Unit") {
                builder.appendLine("* @return")
            }
        }

        builder.appendLine("*/")
        return builder.toString()
    }
}
