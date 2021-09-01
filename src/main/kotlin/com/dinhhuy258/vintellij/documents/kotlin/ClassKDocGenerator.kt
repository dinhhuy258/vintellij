package com.dinhhuy258.vintellij.documents.kotlin

import org.jetbrains.kotlin.psi.KtClassOrObject

internal class ClassKDocGenerator(private val element: KtClassOrObject) :
    KDocGenerator {

    override fun generate(): String {
        val builder = StringBuilder()
        val name = nameToPhrase(element.name ?: "Class")
        builder.appendLine("/**")
            .appendLine("* $name")
            .appendLine("*")

        if (element.typeParameters.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(params = element.typeParameters))
        }

        val (properties, parameters) = element.primaryConstructor?.valueParameters?.partition {
            it.hasValOrVar()
        } ?: Pair(emptyList(), emptyList())

        if (properties.isNotEmpty()) {
            builder.appendLine(toParamsKdoc(keyword = "@property", params = properties))
        }

        if (parameters.isNotEmpty()) {
            builder.appendLine("* @constructor")
                .appendLine("*")
                .appendLine(toParamsKdoc(params = parameters))
        } else {
            builder.appendLine("* @constructor Create empty $name")
        }
        builder.appendLine("*/")
        return builder.toString()
    }
}
