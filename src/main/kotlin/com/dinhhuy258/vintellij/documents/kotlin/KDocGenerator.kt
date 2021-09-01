package com.dinhhuy258.vintellij.documents.kotlin

import com.intellij.psi.PsiNameIdentifierOwner
import java.util.regex.Pattern

interface KDocGenerator {

    companion object {
        const val LF = "\n"
    }

    fun generate(): String

    fun toParamsKdoc(keyword: String = "@param", params: List<PsiNameIdentifierOwner>): String =
        params.map { "$keyword [${it.name}]" }
            .joinToString(LF, transform = { "* $it" })

    fun StringBuilder.appendLine(text: String): StringBuilder = append(text).append(LF)

    fun nameToPhrase(name: String): String {
        val array = name.split(Pattern.compile("(?<=[a-zA-Z])(?=[A-Z])"))
        val builder = StringBuilder()
        array.forEach {
            builder.append(it.toLowerCase())
            builder.append(" ")
        }
        val phrase = builder.toString()
        return phrase.capitalize()
    }
}
