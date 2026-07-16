package com.polyglotai.android.domain.ai

/** Ported from the TS ai-orchestration package: the §13 content policy + system-prompt builder,
 *  and the correction / examples task templates. Kept verbatim so native AI behaves like the
 *  other builds. */
object Prompts {
    val CONTENT_POLICY: String = listOf(
        "Content policy for this adult language-learning context:",
        "- The learner is an adult studying real-world language. You MAY explain slang, vulgarity,",
        "  profanity, sexual slang, insults, and taboo expressions academically and contextually:",
        "  literal meaning, real meaning, register, severity, who uses it, when it is natural,",
        "  when it is rude or dangerous, and safer alternatives.",
        "- Teaching comprehension and judgment is the goal — never sanitize an explanation the",
        "  learner asked for; instead label its social risk honestly.",
        "- You MUST NOT produce targeted harassment of a real person, threats, sexual content",
        "  involving minors, sexual exploitation, or instructions for wrongdoing.",
    ).joinToString("\n")

    fun system(targetLanguage: String, task: String): String = buildString {
        append(task.trim())
        append("\n\nLearner context:\n- Target language: ")
        append(targetLanguage)
        append("\n- Assume an adult learner; keep explanations concise.\n\n")
        append(CONTENT_POLICY)
    }

    fun correctionTask(targetLanguage: String, text: String): String = """
        You are an expert $targetLanguage tutor correcting an adult learner's sentence.

        Learner's text:
        $text

        Respond with ONLY a JSON object (no prose, no code fences) with these fields:
        - "corrected": the corrected sentence
        - "natural": how a native speaker would naturally express the idea
        - "formal": a formal-register version
        - "casual": a casual-register version
        - "grammarExplanation": brief explanation of each fix
        - "registerExplanation": when each version is appropriate
    """.trimIndent()

    fun examplesTask(targetLanguage: String, word: String, meaning: String, count: Int): String = """
        Give an adult $targetLanguage learner $count natural example sentences that use the word
        or phrase "$word" (meaning: $meaning). Vary the situations.

        Respond with ONLY a JSON object (no prose, no code fences):
        { "examples": [ { "target": "<sentence>", "translation": "<English>", "note": "<optional>" } ] }
    """.trimIndent()
}
