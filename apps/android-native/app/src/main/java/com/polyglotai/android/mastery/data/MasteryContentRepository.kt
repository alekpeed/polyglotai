package com.polyglotai.android.mastery.data

import android.content.Context
import com.polyglotai.android.mastery.model.MasteryLesson
import kotlinx.serialization.json.Json

/**
 * Loads authored mastery lessons bundled under assets/mastery. This is the Content Repository seam
 * (spec §3.3) — Milestone 1 reads local JSON; later milestones can back it with generated/imported
 * content behind the same API without touching the engine.
 */
class MasteryContentRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val dir = "mastery"

    fun listLessonFiles(): List<String> =
        context.assets.list(dir)?.filter { it.endsWith(".json") }?.sorted().orEmpty()

    fun loadLesson(file: String): MasteryLesson {
        val text = context.assets.open("$dir/$file").bufferedReader().use { it.readText() }
        return json.decodeFromString(MasteryLesson.serializer(), text)
    }

    /** First bundled lesson, or null if none are installed. */
    fun firstLesson(): MasteryLesson? = listLessonFiles().firstOrNull()?.let { runCatching { loadLesson(it) }.getOrNull() }
}
