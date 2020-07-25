package com.dinhhuy258.vintellij.idea.completions

import com.google.gson.annotations.SerializedName

enum class CompletionKind {
    @SerializedName("F") FUNCTION,
    @SerializedName("V") VARIABLE,
    @SerializedName("T") TYPE,
    @SerializedName("K") KEYWORD,
    @SerializedName("U") UNKNOWN
}
