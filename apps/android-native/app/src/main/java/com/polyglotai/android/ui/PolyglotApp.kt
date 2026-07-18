package com.polyglotai.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polyglotai.android.AppContainer
import com.polyglotai.android.data.GrammarItem
import com.polyglotai.android.data.LanguageOption
import com.polyglotai.android.data.SlangItem
import com.polyglotai.android.data.VocabularyItem
import com.polyglotai.android.ui.theme.AppTheme
import com.polyglotai.android.ui.theme.Eyebrow
import com.polyglotai.android.ui.theme.FlatRow
import com.polyglotai.android.ui.theme.HeroBox
import com.polyglotai.android.ui.theme.LocalDisplayFamily
import com.polyglotai.android.ui.theme.LocalPolyColors
import com.polyglotai.android.ui.theme.Pack
import com.polyglotai.android.ui.theme.PackVariant
import com.polyglotai.android.ui.theme.variantsForPack
import com.polyglotai.android.ui.theme.PlexMono
import com.polyglotai.android.ui.theme.PlexSans
import com.polyglotai.android.ui.theme.PolyTextField
import com.polyglotai.android.ui.theme.PrimaryButton
import com.polyglotai.android.ui.theme.SecondaryButton
import com.polyglotai.android.ui.theme.SectionHead
import com.polyglotai.android.ui.theme.TagCard
import com.polyglotai.android.ui.theme.TextureHero
import com.polyglotai.android.ui.theme.TextureWord
import com.polyglotai.android.ui.theme.packForId
import com.polyglotai.android.ui.theme.seigaiha
import com.polyglotai.android.data.ai.ChatMessage
import com.polyglotai.android.data.db.ReviewItem
import com.polyglotai.android.domain.DashboardStats
import com.polyglotai.android.domain.Streak
import com.polyglotai.android.domain.ai.AiCorrection
import com.polyglotai.android.domain.ai.AiExample
import com.polyglotai.android.domain.ai.AiTranslation
import com.polyglotai.android.domain.ai.NeedsAccessCode
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Picker : Screen
    data object Account : Screen
    /** [returnTo] lets Settings come back to wherever it was opened from (Picker or Dashboard). */
    data class Settings(val returnTo: Screen = Picker) : Screen
    data class Dashboard(val packId: String, val packName: String) : Screen
    data class Review(val packId: String, val packName: String) : Screen
    data class Library(val packId: String, val packName: String) : Screen
    data class Tutor(val packId: String, val packName: String) : Screen
    data class Conversation(val packId: String, val packName: String) : Screen
    data class Pronunciation(val packId: String, val packName: String) : Screen
    data class Interpreter(val packId: String, val packName: String) : Screen
    data class Drill(val packId: String, val packName: String) : Screen
    data class Mastery(val packId: String, val packName: String) : Screen
}

@Composable
fun PolyglotApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
    appTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {},
    onPackChange: (Pack) -> Unit = {},
    pack: Pack = Pack.DEFAULT,
    packVariant: PackVariant = PackVariant.DEFAULT,
    onVariantChange: (PackVariant) -> Unit = {},
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Picker) }
    var onboarded by remember { mutableStateOf(container.settings.onboarded) }

    if (!onboarded) {
        OnboardingScreen(modifier, onDone = {
            container.settings.onboarded = true
            onboarded = true
        })
        return
    }

    // System back navigates within the app instead of closing it: sub-screens fall back to the
    // dashboard, top-level screens to the picker. Disabled on the picker so back exits normally.
    BackHandler(enabled = screen != Screen.Picker) {
        screen = when (val s = screen) {
            is Screen.Picker -> Screen.Picker
            is Screen.Account -> Screen.Picker
            is Screen.Settings -> s.returnTo
            is Screen.Dashboard -> {
                onPackChange(Pack.DEFAULT)
                Screen.Picker
            }
            is Screen.Review -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Library -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Tutor -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Conversation -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Pronunciation -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Interpreter -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Drill -> Screen.Dashboard(s.packId, s.packName)
            is Screen.Mastery -> Screen.Dashboard(s.packId, s.packName)
        }
    }

    Crossfade(targetState = screen, label = "screen") { s ->
      when (s) {
        is Screen.Picker -> PickerScreen(
            container, modifier,
            onPick = { opt ->
                onPackChange(packForId(opt.id))
                screen = Screen.Dashboard(opt.id, opt.name)
            },
            onAccount = { screen = Screen.Account },
            onSettings = { screen = Screen.Settings() },
        )
        is Screen.Account -> AccountScreen(
            container, modifier,
            onBack = { screen = Screen.Picker },
        )
        is Screen.Settings -> SettingsScreen(
            container, modifier, appTheme, onThemeChange,
            pack = pack, packVariant = packVariant, onVariantChange = onVariantChange,
            onBack = { screen = s.returnTo },
        )
        is Screen.Dashboard -> DashboardScreen(
            container, modifier, s.packId, s.packName,
            onReview = { screen = Screen.Review(s.packId, s.packName) },
            onLibrary = { screen = Screen.Library(s.packId, s.packName) },
            onTutor = { screen = Screen.Tutor(s.packId, s.packName) },
            onConversation = { screen = Screen.Conversation(s.packId, s.packName) },
            onPronunciation = { screen = Screen.Pronunciation(s.packId, s.packName) },
            onInterpreter = { screen = Screen.Interpreter(s.packId, s.packName) },
            onDrill = { screen = Screen.Drill(s.packId, s.packName) },
            onMastery = { screen = Screen.Mastery(s.packId, s.packName) },
            onSettings = { screen = Screen.Settings(returnTo = s) },
            onBack = {
                onPackChange(Pack.DEFAULT)
                screen = Screen.Picker
            },
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
        is Screen.Interpreter -> InterpreterScreen(
            container, modifier, s.packName,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
        is Screen.Drill -> DrillScreen(
            container, modifier, s.packId, s.packName,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
        is Screen.Mastery -> com.polyglotai.android.mastery.ui.MasteryScreen(
            container.mastery, modifier,
            onBack = { screen = Screen.Dashboard(s.packId, s.packName) },
        )
      }
    }
}

@Composable
private fun PickerScreen(
    container: AppContainer,
    modifier: Modifier,
    onPick: (LanguageOption) -> Unit,
    onAccount: () -> Unit,
    onSettings: () -> Unit,
) {
    var langs by remember { mutableStateOf<List<LanguageOption>?>(null) }
    LaunchedEffect(Unit) {
        langs = runCatching { container.packs.fullLanguages() }.getOrDefault(emptyList())
    }

    val poly = LocalPolyColors.current
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // .onboard-hero — full-bleed, no rounding, running straight into the paper section below.
        TextureHero(
            eyebrow = "PolyglotAI",
            headline = "Which\nlanguage?",
            body = "Every language gets its own progress, review queue, and pace — pick up where you left off, or start something new.",
            modifier = Modifier.fillMaxWidth(),
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LinkLabel("Settings", onSettings)
                    LinkLabel(if (container.account.isSignedIn) "Account" else "Sign in", onAccount)
                }
            }
            SectionHead("Start learning")
            val list = langs
            when {
                list == null -> Text("Loading…", style = MaterialTheme.typography.bodyMedium, color = poly.inkSoft)
                list.isEmpty() -> Text("No language packs found.", style = MaterialTheme.typography.bodyMedium, color = poly.inkSoft)
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { opt ->
                        FlatRow(
                            onClick = { onPick(opt) },
                            name = opt.name,
                            sub = opt.id,
                            isNew = true,
                        )
                    }
                }
            }
        }
    }
}

/** A small IBM Plex Mono link — .shelf-head button / button.link styling for inline text actions. */
@Composable
private fun LinkLabel(text: String, onClick: () -> Unit) {
    val poly = LocalPolyColors.current
    Text(
        text,
        fontFamily = PlexMono,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = poly.indigo,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private data class ShelfItem(val tag: String, val title: String, val body: String)

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
    onInterpreter: () -> Unit,
    onDrill: () -> Unit,
    onMastery: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var streak by remember { mutableStateOf<Streak?>(null) }
    var counts by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var shelf by remember { mutableStateOf<List<ShelfItem>>(emptyList()) }
    var syncing by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(packId) {
        container.learning.seedPack(packId)
        stats = container.learning.dashboard(packId, container.settings.dailyGoal)
        streak = container.learning.streak(packId)
        val vocab = runCatching { container.packs.vocabulary(packId) }.getOrDefault(emptyList())
        val grammar = runCatching { container.packs.grammar(packId) }.getOrDefault(emptyList())
        val slang = runCatching { container.packs.slang(packId) }.getOrDefault(emptyList())
        counts = Triple(vocab.size, grammar.size, slang.size)
        shelf = buildList {
            grammar.firstOrNull()?.let { add(ShelfItem(it.cefr?.let { c -> "Grammar · $c" } ?: "Grammar", it.title, stripMd(it.explanationMd).take(120))) }
            vocab.firstOrNull()?.let { add(ShelfItem("Vocabulary", it.lemma, it.translation)) }
            slang.firstOrNull()?.let { add(ShelfItem(it.register?.let { r -> "Slang · $r" } ?: "Slang", it.phrase, it.natural ?: it.literal ?: "")) }
        }
    }

    val poly = LocalPolyColors.current
    Column(modifier.fillMaxSize().seigaiha(poly.seigaiha).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp)) {
        Eyebrow("Welcome back")
        Text(packName, fontFamily = LocalDisplayFamily.current, fontSize = 28.sp, color = poly.ink, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

        val d = stats
        if (d == null) {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium, color = poly.inkSoft)
        } else {
            // The signature hero: periwinkle fill (navy in dark), a gold Fraunces-style serif
            // numeral, and the asymmetric top-start curve — flat in the ja washi world.
            HeroBox(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("DUE NOW", fontFamily = PlexMono, fontSize = 11.sp, letterSpacing = 1.5.sp, color = poly.onFill.copy(alpha = 0.65f))
                    Text(
                        "${d.dueCount}",
                        fontFamily = LocalDisplayFamily.current,
                        fontWeight = FontWeight.Normal,
                        fontSize = 64.sp,
                        color = poly.gold,
                    )
                    Text(
                        if (d.dueCount == 0) "All caught up — check back later or explore the library."
                        else "Vocabulary, grammar, and register — ready whenever you are.",
                        fontFamily = PlexSans,
                        fontSize = 13.sp,
                        color = poly.onFill.copy(alpha = 0.78f),
                    )
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton(onClick = onReview, enabled = d.dueCount > 0) {
                        Text(if (d.dueCount > 0) "Start review →" else "Nothing due")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // .streak-card — a smaller asymmetric blob corner, day-of-week dots.
            val s = streak
            val streakShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = if (poly.flat) 8.dp else 44.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(streakShape)
                    .background(poly.surfaceRaised)
                    .border(1.dp, poly.line, streakShape)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Eyebrow("Streak")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${s?.days ?: 0}", fontFamily = LocalDisplayFamily.current, fontSize = 30.sp, color = poly.ink)
                    Text(
                        " day${if ((s?.days ?: 0) == 1) "" else "s"}",
                        fontFamily = PlexSans,
                        fontSize = 15.sp,
                        color = poly.inkSoft,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (s?.last7 ?: List(7) { false }).forEach { on ->
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (on) poly.goldFill else poly.surface),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // .daily-goal — flat bordered strip, thin progress bar.
            val goalMet = d.reviewsToday >= d.dailyGoal
            val goalShape = RoundedCornerShape(if (poly.flat) 0.dp else 6.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(goalShape)
                    .background(poly.surfaceRaised)
                    .border(1.dp, poly.line, goalShape)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Eyebrow("Today's goal")
                    Text("${d.reviewsToday} / ${d.dailyGoal}", fontFamily = PlexMono, fontSize = 15.sp, color = poly.ink)
                }
                LinearProgressIndicator(
                    progress = { if (d.dailyGoal == 0) 0f else (d.reviewsToday.toFloat() / d.dailyGoal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                    color = poly.indigoFill,
                    trackColor = poly.line,
                )
                if (goalMet) {
                    Text("Goal met — nice work ✓", fontFamily = PlexSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = poly.heat[0])
                }
            }

            Spacer(Modifier.height(18.dp))

            // .stat-strip — a hairline grid of counts pulled straight from the pack's own content.
            val cnt = counts
            if (cnt != null) {
                val (vocab, grammar, slang) = cnt
                Row(
                    Modifier.fillMaxWidth().background(poly.line).border(1.dp, poly.line),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    StatCell("$vocab", "vocabulary", Modifier.weight(1f))
                    StatCell("$grammar", "grammar", Modifier.weight(1f))
                    StatCell("$slang", "slang & register", Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
            }

            if (shelf.isNotEmpty()) {
                SectionHead("This week", linkText = "See library →", onLinkClick = onLibrary)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    shelf.forEachIndexed { i, item ->
                        TagCard(tag = item.tag, title = item.title, body = item.body, tagIndex = i)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            SectionHead("Practice")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TagCard("AI Tutor", "Get a full correction", "Write a sentence, get corrected/formal/casual/slang versions.", 0, onClick = onTutor)
                TagCard("Conversation", "Roleplay a scenario", "Café, job interview, first date — pick a scenario and go.", 1, onClick = onConversation)
                TagCard("Live Interpreter", "Interpret on the spot", "Two-way translation, either direction, with a register note.", 2, onClick = onInterpreter)
                TagCard("Sentence Mastery", "Build & speak, one take", "Word by word into a full sentence, strict restart on a miss.", 0, onClick = onMastery)
                TagCard("Quick Drill", "Fast multiple choice", "Vocabulary rounds with distractors — a faster loop than review.", 1, onClick = onDrill)
                TagCard("Pronunciation", "Record & score", "Say it back, get scored against the target.", 1, onClick = onPronunciation)
                TagCard("Settings", "Daily goal & appearance", "Review pace, theme, and account.", 2, onClick = onSettings)
            }

            Spacer(Modifier.height(20.dp))
            if (container.account.isSignedIn) {
                SecondaryButton(
                    onClick = {
                        syncing = true; syncMsg = null
                        scope.launch {
                            try {
                                val r = container.account.sync()
                                stats = container.learning.dashboard(packId)
                                syncMsg = "Synced · ${r.pushed} up, ${r.pulled} down"
                            } catch (e: Exception) {
                                syncMsg = e.message ?: "Sync failed."
                            } finally {
                                syncing = false
                            }
                        }
                    },
                    enabled = !syncing,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (syncing) "Syncing…" else "Sync now") }
                syncMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = poly.inkSoft, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(10.dp))
            }
            LinkLabel("Switch language", onBack)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    val poly = LocalPolyColors.current
    Column(
        modifier.background(poly.surfaceRaised).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontFamily = PlexMono, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = poly.ink)
        Text(label, fontFamily = PlexSans, fontSize = 11.sp, color = poly.inkSoft)
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
                LinearProgressIndicator(
                    progress = { if (q.isEmpty()) 0f else index.toFloat() / q.size },
                    modifier = Modifier.fillMaxWidth(),
                )
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
    var query by remember { mutableStateOf("") }

    LaunchedEffect(packId) {
        vocab = runCatching { container.packs.vocabulary(packId) }.getOrDefault(emptyList())
        grammar = runCatching { container.packs.grammar(packId) }.getOrDefault(emptyList())
        slang = runCatching { container.packs.slang(packId) }.getOrDefault(emptyList())
    }

    val q = query.trim()
    val vocabShown = vocab.orEmpty().filter {
        q.isBlank() || it.lemma.contains(q, true) || it.translation.contains(q, true) ||
            it.reading?.contains(q, true) == true || it.romaji?.contains(q, true) == true
    }
    val grammarShown = grammar.orEmpty().filter {
        q.isBlank() || it.title.contains(q, true) || it.explanationMd.contains(q, true)
    }
    val slangShown = slang.orEmpty().filter {
        q.isBlank() || it.phrase.contains(q, true) || it.natural?.contains(q, true) == true
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
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val shownCount = when (tab) {
            LibTab.VOCAB -> vocabShown.size
            LibTab.GRAMMAR -> grammarShown.size
            LibTab.SLANG -> slangShown.size
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            if (shownCount == 0) {
                item {
                    Text(
                        if (q.isBlank()) "Nothing here yet." else "No matches for \"$q\".",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            when (tab) {
                LibTab.VOCAB -> items(vocabShown) { v ->
                    VocabLibraryRow(container, packName, v)
                }
                LibTab.GRAMMAR -> items(grammarShown) { g ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(g.title, style = MaterialTheme.typography.titleMedium)
                            Text(stripMd(g.explanationMd), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                LibTab.SLANG -> items(slangShown) { s ->
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
private fun OnboardingScreen(modifier: Modifier, onDone: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Welcome to PolyglotAI", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Learn a language the way people actually speak it — slang and register included.",
            style = MaterialTheme.typography.bodyLarge,
        )
        listOf(
            "Spaced-repetition review" to "Cards come back right before you'd forget them.",
            "Quick drills" to "Fast multiple-choice rounds when you want a change of pace.",
            "AI tutor & conversation" to "Get corrections, chat, practice pronunciation, and interpret text.",
            "Sync across devices" to "Create an account to carry your progress anywhere.",
        ).forEach { (title, body) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        PrimaryButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

private data class DrillQuestion(
    val prompt: String,
    val reading: String?,
    val options: List<String>,
    val correct: Int,
)

@Composable
private fun DrillScreen(
    container: AppContainer,
    modifier: Modifier,
    packId: String,
    packName: String,
    onBack: () -> Unit,
) {
    var questions by remember { mutableStateOf<List<DrillQuestion>?>(null) }
    var index by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }

    LaunchedEffect(packId) {
        val vocab = runCatching { container.packs.vocabulary(packId) }.getOrDefault(emptyList())
            .filter { it.lemma.isNotBlank() && it.translation.isNotBlank() }
        val translations = vocab.map { it.translation }.distinct()
        questions = if (translations.size < 4) {
            emptyList()
        } else {
            vocab.shuffled().take(10).map { v ->
                val distractors = translations.filter { it != v.translation }.shuffled().take(3)
                val opts = (distractors + v.translation).shuffled()
                DrillQuestion(
                    prompt = v.lemma,
                    reading = listOfNotNull(v.reading, v.romaji).joinToString(" · ").ifBlank { null },
                    options = opts,
                    correct = opts.indexOf(v.translation),
                )
            }
        }
    }

    val qs = questions
    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Quick drill · $packName", style = MaterialTheme.typography.headlineSmall)
        when {
            qs == null -> Text("Building drill…", style = MaterialTheme.typography.bodyMedium)
            qs.isEmpty() -> Text("Not enough vocabulary for a drill yet.", style = MaterialTheme.typography.bodyMedium)
            index >= qs.size -> {
                Text("Drill complete", style = MaterialTheme.typography.headlineMedium)
                Text("$score / ${qs.size} correct", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { index = 0; selected = null; score = 0; questions = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Drill again") }
            }
            else -> {
                val q = qs[index]
                Text("${index + 1} of ${qs.size}", style = MaterialTheme.typography.labelMedium)
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(q.prompt, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        q.reading?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                q.options.forEachIndexed { i, opt ->
                    val answered = selected != null
                    val isCorrect = i == q.correct
                    val colors = when {
                        !answered -> ButtonDefaults.outlinedButtonColors()
                        isCorrect -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        i == selected -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        else -> ButtonDefaults.outlinedButtonColors()
                    }
                    OutlinedButton(
                        onClick = {
                            if (selected == null) {
                                selected = i
                                if (isCorrect) score += 1
                            }
                        },
                        enabled = !answered || isCorrect || i == selected,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(opt) }
                }
                if (selected != null) {
                    Button(
                        onClick = { index += 1; selected = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (index + 1 >= qs.size) "See results" else "Next") }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun InterpreterScreen(container: AppContainer, modifier: Modifier, packName: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(container.ai.isConnected) }
    var accessCode by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var toTarget by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<AiTranslation?>(null) }

    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Interpreter · $packName", style = MaterialTheme.typography.headlineSmall)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val toEnglish = !toTarget
                if (toTarget) {
                    Button(onClick = { toTarget = true; result = null }, modifier = Modifier.weight(1f)) { Text("English → $packName") }
                } else {
                    OutlinedButton(onClick = { toTarget = true; result = null }, modifier = Modifier.weight(1f)) { Text("English → $packName") }
                }
                if (toEnglish) {
                    Button(onClick = { toTarget = false; result = null }, modifier = Modifier.weight(1f)) { Text("$packName → English") }
                } else {
                    OutlinedButton(onClick = { toTarget = false; result = null }, modifier = Modifier.weight(1f)) { Text("$packName → English") }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (toTarget) "English" else packName) },
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
                            result = container.ai.interpret(packName, text.trim(), toTarget)
                        } catch (e: NeedsAccessCode) {
                            connected = false
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't interpret that."
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Interpreting…" else "Interpret") }
            result?.let { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (toTarget) packName else "English", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(r.translation, style = MaterialTheme.typography.titleMedium)
                        r.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun AccountScreen(container: AppContainer, modifier: Modifier, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(container.account.isSignedIn) }
    var email by remember { mutableStateOf(container.account.email ?: "") }
    var password by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val poly = LocalPolyColors.current
        HeroBox(Modifier.fillMaxWidth(), skyline = true) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "POLYGLOTAI · BRASIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = poly.onFill.copy(alpha = 0.6f),
                )
                Text(
                    "Fala aí.",
                    fontFamily = LocalDisplayFamily.current,
                    fontWeight = FontWeight.Normal,
                    fontSize = 34.sp,
                    color = poly.onFill,
                )
                Text(
                    "Your account, your progress — synced wherever you sign in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = poly.onFill.copy(alpha = 0.82f),
                )
            }
        }
        if (signedIn) {
            Text("Signed in as ${container.account.email ?: "your account"}.", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Your review progress syncs to this account. Sign in on another device to pick up where you left off.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = {
                    container.account.signOut()
                    signedIn = false
                    password = ""
                    notice = "Signed out. Your progress stays on this device."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        } else {
            Text(
                "Sign in to sync your review progress across devices. Without an account the app works fully on this device alone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthMode.entries.forEach { m ->
                    if (m == mode) {
                        Button(onClick = { mode = m; error = null }, modifier = Modifier.weight(1f)) { Text(m.label) }
                    } else {
                        OutlinedButton(onClick = { mode = m; error = null }, modifier = Modifier.weight(1f)) { Text(m.label) }
                    }
                }
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            PrimaryButton(
                enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                onClick = {
                    busy = true; error = null; notice = null
                    scope.launch {
                        try {
                            if (mode == AuthMode.SIGN_UP) {
                                container.account.signUp(email, password)
                            } else {
                                container.account.signIn(email, password)
                            }
                            signedIn = container.account.isSignedIn
                            if (signedIn) {
                                // First sync right away so the account and device converge.
                                val r = runCatching { container.account.sync() }.getOrNull()
                                notice = r?.let { "Synced · ${it.pushed} up, ${it.pulled} down" }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't sign in."
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Working…" else "${mode.label} →") }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

private enum class AuthMode(val label: String) { SIGN_IN("Sign in"), SIGN_UP("Create account") }

@Composable
private fun SettingsScreen(
    container: AppContainer,
    modifier: Modifier,
    appTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    pack: Pack,
    packVariant: PackVariant,
    onVariantChange: (PackVariant) -> Unit,
    onBack: () -> Unit,
) {
    var goal by remember { mutableStateOf(container.settings.dailyGoal) }
    val variants = variantsForPack(pack)

    Column(
        modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Daily goal", style = MaterialTheme.typography.labelLarge)
                Text("$goal cards a day", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { goal = (goal - 5).coerceAtLeast(5); container.settings.dailyGoal = goal },
                        modifier = Modifier.weight(1f),
                        enabled = goal > 5,
                    ) { Text("– 5") }
                    OutlinedButton(
                        onClick = { goal = (goal + 5).coerceAtMost(100); container.settings.dailyGoal = goal },
                        modifier = Modifier.weight(1f),
                        enabled = goal < 100,
                    ) { Text("+ 5") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Appearance", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppTheme.entries.forEach { m ->
                        if (m == appTheme) {
                            Button(
                                onClick = { onThemeChange(m) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) { Text(m.label, style = MaterialTheme.typography.labelMedium) }
                        } else {
                            OutlinedButton(
                                onClick = { onThemeChange(m) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) { Text(m.label, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
        }

        // Palette variant — only shown for worlds that offer more than one look (currently RU:
        // Gzhel / Hermitage). Applies immediately and is remembered per language.
        if (variants.size > 1) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme variant", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        variants.forEach { option ->
                            if (option.variant == packVariant) {
                                Button(
                                    onClick = { onVariantChange(option.variant) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                ) { Text(option.label, style = MaterialTheme.typography.labelMedium) }
                            } else {
                                OutlinedButton(
                                    onClick = { onVariantChange(option.variant) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                ) { Text(option.label, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
            }
        }

        Text(
            "PolyglotAI · native Android build. Content packs are bundled; AI features and cloud sync are optional.",
            style = MaterialTheme.typography.bodySmall,
        )
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
