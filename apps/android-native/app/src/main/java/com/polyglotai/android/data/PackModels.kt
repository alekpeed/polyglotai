package com.polyglotai.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Content models mirroring the JSON in repo-root /packs (the same files the TS app loads). Only
 * the fields the native app currently uses are declared; ignoreUnknownKeys handles the rest, so
 * these can stay lean and grow as more of the app is ported.
 */

@Serializable
data class PackManifest(
    val id: String,
    val name: String,
    val languageCode: String,
    val basePack: String? = null,
    val contents: PackContents = PackContents(),
)

@Serializable
data class PackContents(
    val vocabulary: List<String> = emptyList(),
    val grammar: List<String> = emptyList(),
    val slang: List<String> = emptyList(),
    val dialogues: List<String> = emptyList(),
)

@Serializable
data class VocabularyItem(
    val key: String,
    val lemma: String,
    val translation: String,
    val reading: String? = null,
    val romaji: String? = null,
    @SerialName("partOfSpeech") val partOfSpeech: String? = null,
    val register: String? = null,
    val cefr: String? = null,
    val tags: List<String> = emptyList(),
)
