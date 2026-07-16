package com.polyglotai.android.data

import android.content.Context
import kotlinx.serialization.json.Json

/** A full language a learner can start (basePack == null), for the language picker. */
data class LanguageOption(val id: String, val name: String)

/**
 * Reads bundled pack content from assets/packs (synced from repo-root /packs at build time).
 * Parsing is lenient (ignoreUnknownKeys) so the models can lag the JSON without breaking.
 */
class PackRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun listPackIds(): List<String> =
        context.assets.list("packs")?.toList().orEmpty()

    fun manifest(packId: String): PackManifest =
        json.decodeFromString(readAsset("packs/$packId/manifest.json"))

    /** Full languages only (no basePack) — micro-packs are reached from inside a language later. */
    fun fullLanguages(): List<LanguageOption> =
        listPackIds().mapNotNull { id ->
            runCatching { manifest(id) }.getOrNull()
        }.filter { it.basePack == null }
            .map { LanguageOption(it.id, it.name) }
            .sortedBy { it.name }

    fun vocabulary(packId: String): List<VocabularyItem> =
        loadList(packId, manifest(packId).contents.vocabulary)

    fun grammar(packId: String): List<GrammarItem> =
        loadList(packId, manifest(packId).contents.grammar)

    fun slang(packId: String): List<SlangItem> =
        loadList(packId, manifest(packId).contents.slang)

    private inline fun <reified T> loadList(packId: String, files: List<String>): List<T> =
        files.flatMap { rel ->
            runCatching { json.decodeFromString<List<T>>(readAsset("packs/$packId/$rel")) }.getOrDefault(emptyList())
        }
}
