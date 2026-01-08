package com.example.local_ai_agent.nlp

object SimpleTokenizer{
    private const val MAX_TOKENS=128

    fun tokenize(text: String):String{
        return text
        .lowercase()
        .trim()
        .split(Regex("\\s+"))
        .take(MAX_TOKENS)
        .joinToString(" ")
    }
}