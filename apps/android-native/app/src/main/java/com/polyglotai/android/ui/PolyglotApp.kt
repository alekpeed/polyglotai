package com.polyglotai.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polyglotai.android.AppContainer
import com.polyglotai.android.data.GrammarItem
import com.polyglotai.android.data.LanguageOption
import com.polyglotai.android.data.SlangItem
import com.polyglotai.android.data.VocabularyItem
import com.polyglotai.android.data.ai.ChatMessage
import com.polyglotai.android.data.db.ReviewItem
import com.polyglotai.android.domain.DashboardStats
import com.polyglotai.android.domain.ai.AiCorrection
import com.polyglotai.android.domain.ai.AiExample
import com.polyglotai.android.domain.ai.NeedsAccessCode
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Picker : Screen
    data class Dashboard(val packId: String, val packName: String) : Screen
    data class Review(val packId: String, val packName: String) : Screen
    data class Library(val packId: String, val packName: String) : Screen
    data class Tutor(val packId: String, val packName: String) : Screen
    data class Conversation(val packId: String, val packName: String) : Screen
    data class Pronunciation(val packId: String, val packName: String) : Screen
}

@Composable
fun PolyglotApp(container: AppContainer, modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<Screen>(Screen.Picker) }

    when (val s = screen) {
        is Screen.Picker -> PickerScreen(container, modifier) { opt ->
            screen = Screen.Dashboard(opt.id, opt.name)
        }
        is Screen.Dashboard -> DashboardScreen(
            container, modifier, s.packId, s.packName,
            onReview = { screen = Screen.Review(s.packId, s.packName) },
            onLibrary = { screen = Screen.Library(s.packId, s.packName) },
            onTutor = { screen = Screen.Tutor(s.packId, s.packName) },
            onConversation = { screen = Screen.Conversation(s.packId, s.packName) },
            onPronunciation = { screen = Screen.Pronunciation(s.packId, s.packName) },
            onBack = { screen = Screen.Picker },
        )
        is Screen.Review -> ReviewScreen(
            container, modifier, s.packId,
            onDone = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
        is Screen.Library -> LibraryScreen(
            container, modifier, s.packId, s.packName,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
        is Screen.Tutor -> TutorScreen(
            container, modifier, s.packName,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
        is Screen.Conversation -> ConversationScreen(
            container, modifier, s.packName,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
        is Screen.Pronunciation -> PronunciationScreen(
            container, modifier, s.packId, s.packName,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
    }
}

@Composable
private fun PickerScreen(container: AppContainer, modifier: Modifier, onPick: (LanguageOption) -> Unit) {
    var langs by remember { mutableStateOf<List<LanguageOption>?>(null) }
    LaunchedEffect(Unit) {
        langs = runCatching { container.packs.fullLanguages() }.getOrDefault(emptyList())
    }

    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose a language", style = MaterialTheme.typography.headlineMedium)
        val list = langs
        when {
            list == null -> Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            list.isEmpty() -> Text("No language packs found.", style = MaterialTheme.typography.bodyMedium)
            else -> list.forEach { opt ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(opt.name, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { onPick(opt) }) { Text("Start") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    container: AppContainer,
    modifier: Modifier,
    packId: String,
    packName: String,
    onReview: () -> Unit,
    onLibrary: () -> Unit,
    onTutor: () -> Unit,
    onConversation: () -> Unit,
    onPronunciation: () -> Unit,
    onBack: () -> Unit,
) {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    LaunchedEffect(packId) {
        container.learning.seedPack(packId)
        stats = container.learning.dashboard(packId)
    }

    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(packName, style = MaterialTheme.typography.headlineMedium)
        val d = stats
        if (d == null) {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Due now", style = MaterialTheme.typography.labelLarge)
                    Text("${d.dueCount}", style = MaterialTheme.typography.displaySmall)
                    Text("${d.totalCards} cards total", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onReview, enabled = d.dueCount > 0) {
                        Text(if (d.dueCount > 0) "Start review" else "Nothing due")
                    }
                }
            }
            OutlinedButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) { Text("Browse library") }
            OutlinedButton(onClick = onTutor, modifier = Modifier.fillMaxWidth()) { Text("AI Tutor") }
            OutlinedButton(onClick = onConversation, modifier = Modifier.fillMaxWidth()) { Text("Conversation") }
            OutlinedButton(onClick = onPronunciation, modifier = Modifier.fillMaxWidth()) { Text("Pronunciation") }
            TextButton(onClick = onBack) { Text("Switch language") }
        }
    }
}

@Composable
private fun ReviewScreen(container: AppContainer, modifier: Modifier, packId: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<ReviewItem>?>(null) }
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reviewed by remember { mutableStateOf(0) }

    LaunchedEffect(packId) {
        queue = container.learning.listDue(packId)
    }

    val q = queue
    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            q == null -> Text("Loading review…", style = MaterialTheme.typography.bodyMedium)
            index >= q.size -> {
                Text("Session complete", style = MaterialTheme.typography.headlineMedium)
                Text("$reviewed reviewed", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onDone) { Text("Back to dashboard") }
            }
            else -> {
                val card = q[index]
                Text("${q.size - index} left", style = MaterialTheme.typography.labelMedium)
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(card.front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        card.reading?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (revealed) {
                            Spacer(Modifier.height(4.dp))
                            Text(card.back, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (!revealed) {
                    Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Show answer") }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Again", 2 to "Hard", 3 to "Good", 4 to "Easy").forEach { (rating, label) ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch { container.learning.grade(card, rating) }
                                    reviewed += 1
                                    index += 1
                                    revealed = false
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(label) }
                        }
                    }
                }
                TextButton(onClick = onDone) { Text("Stop for now") }
            }
        }
    }
}

private enum class LibTab(val label: String) { VOCAB("Vocabulary"), GRAMMAR("Grammar"), SLANG("Slang") }

private fun stripMd(s: String): String = s.replace("**", "").replace("*", "").replace("`", "")

@Composable
private fun LibraryScreen(
    container: AppContainer,
    modifier: Modifier,
    packId: String,
    packName: String,
    onBack: () -> Unit,
) {
    var vocab by remember { mutableStateOf<List<VocabularyItem>?>(null) }
    var grammar by remember { mutableStateOf<List<GrammarItem>?>(null) }
    var slang by remember { mutableStateOf<List<SlangItem>?>(null) }
    var tab by remember { mutableStateOf(LibTab.VOCAB) }

    LaunchedEffect(packId) {
        vocab = runCatching { container.packs.vocabulary(packId) }.getOrDefault(emptyList())
        grammar = runCatching { container.packs.grammar(packId) }.getOrDefault(emptyList())
        slang = runCatching { container.packs.slang(packId) }.getOrDefault(emptyList())
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("$packName · Library", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibTab.entries.forEach { t ->
                if (t == tab) {
                    Button(onClick = { tab = t }, modifier = Modifier.weight(1f)) { Text(t.label) }
                } else {
                    OutlinedButton(onClick = { tab = t }, modifier = Modifier.weight(1f)) { Text(t.label) }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            when (tab) {
                LibTab.VOCAB -> items(vocab.orEmpty()) { v ->
                    VocabLibraryRow(container, packName, v)
                }
                LibTab.GRAMMAR -> items(grammar.orEmpty()) { g ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(g.title, style = MaterialTheme.typography.titleMedium)
                            Text(stripMd(g.explanationMd), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                LibTab.SLANG -> items(slang.orEmpty()) { s ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.phrase, style = MaterialTheme.typography.titleMedium)
                            s.natural?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            listOfNotNull(s.register, s.severity?.let { "severity $it" }).joinToString(" · ").ifBlank { null }?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun VocabLibraryRow(container: AppContainer, packName: String, v: VocabularyItem) {
    val scope = rememberCoroutineScope()
    var examples by remember { mutableStateOf<List<AiExample>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(v.lemma, style = MaterialTheme.typography.titleMedium)
            listOfNotNull(v.reading, v.romaji).joinToString(" · ").ifBlank { null }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Text(v.translation, style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        try {
                            examples = container.ai.examples(packName, v.lemma, v.translation)
                        } catch (e: NeedsAccessCode) {
                            error = "Open AI Tutor once to enter the access code."
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't generate examples."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) { Text(if (busy) "Generating…" else if (examples == null) "✨ Examples" else "↻ Examples") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            examples?.forEach { ex ->
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(ex.target, style = MaterialTheme.typography.bodyMedium)
                    Text(ex.translation, style = MaterialTheme.typography.bodySmall)
                    ex.note?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun ConversationScreen(container: AppContainer, modifier: Modifier, packName: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(container.ai.isConnected) }
    var accessCode by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Conversation · $packName", style = MaterialTheme.typography.headlineSmall)
        if (!connected) {
            Text("Enter the access code to enable AI features.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = accessCode,
                onValueChange = { accessCode = it },
                label = { Text("Access code") },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                enabled = !busy && accessCode.isNotBlank(),
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        val ok = runCatching { container.ai.connect(accessCode.trim()) }.getOrDefault(false)
                        busy = false
                        if (ok) connected = true else error = "That code didn't work."
                    }
                },
            ) { Text(if (busy) "Connecting…" else "Connect") }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (m.role == "user") "You" else packName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(m.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Say something…") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = !busy && input.isNotBlank(),
                    onClick = {
                        val userText = input.trim()
                        input = ""
                        val history = messages.toList()
                        messages.add(ChatMessage("user", userText))
                        busy = true; error = null
                        scope.launch {
                            try {
                                val reply = container.ai.converse(packName, "casual everyday chat", history, userText)
                                messages.add(ChatMessage("assistant", reply))
                            } catch (e: NeedsAccessCode) {
                                connected = false
                            } catch (e: Exception) {
                                error = e.message ?: "Something went wrong."
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) { Text(if (busy) "…" else "Send") }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun CorrectionField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TutorScreen(container: AppContainer, modifier: Modifier, packName: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(container.ai.isConnected) }
    var accessCode by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<AiCorrection?>(null) }

    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("AI Tutor", style = MaterialTheme.typography.headlineMedium)
        if (!connected) {
            Text("Enter the access code to enable AI features.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = accessCode,
                onValueChange = { accessCode = it },
                label = { Text("Access code") },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                enabled = !busy && accessCode.isNotBlank(),
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        val ok = runCatching { container.ai.connect(accessCode.trim()) }.getOrDefault(false)
                        busy = false
                        if (ok) connected = true else error = "That code didn't work."
                    }
                },
            ) { Text(if (busy) "Connecting…" else "Connect") }
        } else {
            Text("Write a sentence in $packName; get it corrected.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(packName) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                enabled = !busy && text.isNotBlank(),
                onClick = {
                    busy = true; error = null; result = null
                    scope.launch {
                        try {
                            result = container.ai.correct(packName, text.trim())
                        } catch (e: NeedsAccessCode) {
                            connected = false
                        } catch (e: Exception) {
                            error = e.message ?: "Something went wrong."
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Correcting…" else "Correct it") }

            result?.let { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CorrectionField("Corrected", c.corrected)
                        c.natural?.let { CorrectionField("Natural", it) }
                        c.formal?.let { CorrectionField("Formal", it) }
                        c.casual?.let { CorrectionField("Casual", it) }
                        c.grammarExplanation?.let { CorrectionField("Grammar", it) }
                        c.registerExplanation?.let { CorrectionField("Register", it) }
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
