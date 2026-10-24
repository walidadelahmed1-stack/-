package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("neon_nile_progress_v1", Context.MODE_PRIVATE)

    private val _progress = MutableStateFlow(loadProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private fun loadProgress(): PlayerProgress {
        val starsJson = prefs.getString("stars", "{}") ?: "{}"
        val scoresJson = prefs.getString("scores", "{}") ?: "{}"
        val solvedSet = prefs.getStringSet("solved", emptySet()) ?: emptySet()
        val storiesSet = prefs.getStringSet("storiesSeen", emptySet()) ?: emptySet()
        val puzzleScoresJson = prefs.getString("puzzle_scores_v1", "{}") ?: "{}"

        val starsMap = mutableMapOf<String, Int>()
        try {
            val obj = JSONObject(starsJson)
            for (key in obj.keys()) {
                starsMap[key] = obj.getInt(key)
            }
        } catch (_: Exception) {}

        val scoresMap = mutableMapOf<String, Int>()
        try {
            val obj = JSONObject(scoresJson)
            for (key in obj.keys()) {
                scoresMap[key] = obj.getInt(key)
            }
        } catch (_: Exception) {}

        val storiesIntSet = storiesSet.mapNotNull { it.toIntOrNull() }.toSet()

        val puzzleScoresMap = mutableMapOf<String, PuzzleScoreRecord>()
        try {
            val obj = JSONObject(puzzleScoresJson)
            for (key in obj.keys()) {
                val pObj = obj.getJSONObject(key)
                puzzleScoresMap[key] = PuzzleScoreRecord(
                    levelId = key,
                    highScore = pObj.optInt("highScore", 0),
                    bestTimeSeconds = pObj.optLong("bestTimeSeconds", 0L),
                    fewestMoves = pObj.optInt("fewestMoves", 0),
                    stars = pObj.optInt("stars", 3),
                    timestamp = pObj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {}

        return PlayerProgress(
            stars = starsMap,
            scores = scoresMap,
            solvedPuzzles = solvedSet,
            storiesSeen = storiesIntSet,
            puzzleScores = puzzleScoresMap
        )
    }

    private fun saveProgress(p: PlayerProgress) {
        val starsObj = JSONObject()
        p.stars.forEach { (k, v) -> starsObj.put(k, v) }

        val scoresObj = JSONObject()
        p.scores.forEach { (k, v) -> scoresObj.put(k, v) }

        val puzzleScoresObj = JSONObject()
        p.puzzleScores.forEach { (k, rec) ->
            val pObj = JSONObject().apply {
                put("highScore", rec.highScore)
                put("bestTimeSeconds", rec.bestTimeSeconds)
                put("fewestMoves", rec.fewestMoves)
                put("stars", rec.stars)
                put("timestamp", rec.timestamp)
            }
            puzzleScoresObj.put(k, pObj)
        }

        prefs.edit()
            .putString("stars", starsObj.toString())
            .putString("scores", scoresObj.toString())
            .putStringSet("solved", p.solvedPuzzles)
            .putStringSet("storiesSeen", p.storiesSeen.map { it.toString() }.toSet())
            .putString("puzzle_scores_v1", puzzleScoresObj.toString())
            .apply()

        _progress.value = p
    }

    fun recordLevelCompletion(levelId: String, stars: Int, score: Int) {
        val current = _progress.value
        val oldStars = current.stars[levelId] ?: 0
        val oldScore = current.scores[levelId] ?: 0

        val newStars = current.stars.toMutableMap().apply { put(levelId, maxOf(oldStars, stars)) }
        val newScores = current.scores.toMutableMap().apply { put(levelId, maxOf(oldScore, score)) }

        val updated = current.copy(stars = newStars, scores = newScores)
        saveProgress(updated)
    }

    fun recordPuzzleScore(levelId: String, result: PuzzleScoreResult): PuzzleScoreRecord {
        val current = _progress.value
        val existing = current.puzzleScores[levelId]

        val newHighScore = maxOf(existing?.highScore ?: 0, result.score)
        val newBestTime = if (existing != null && existing.bestTimeSeconds > 0) {
            minOf(existing.bestTimeSeconds, result.timeSeconds)
        } else {
            result.timeSeconds
        }
        val newFewestMoves = if (existing != null && existing.fewestMoves > 0) {
            minOf(existing.fewestMoves, result.moves)
        } else {
            result.moves
        }
        val newStars = maxOf(existing?.stars ?: 0, result.stars)

        val record = PuzzleScoreRecord(
            levelId = levelId,
            highScore = newHighScore,
            bestTimeSeconds = newBestTime,
            fewestMoves = newFewestMoves,
            stars = newStars,
            timestamp = System.currentTimeMillis()
        )

        val updatedPuzzleScores = current.puzzleScores.toMutableMap().apply {
            put(levelId, record)
        }
        val updatedSolved = current.solvedPuzzles.toMutableSet().apply {
            add(levelId)
        }

        val updated = current.copy(
            puzzleScores = updatedPuzzleScores,
            solvedPuzzles = updatedSolved
        )
        saveProgress(updated)
        return record
    }

    fun recordPuzzleSolved(levelId: String) {
        val current = _progress.value
        val newSolved = current.solvedPuzzles.toMutableSet().apply { add(levelId) }
        val updated = current.copy(solvedPuzzles = newSolved)
        saveProgress(updated)
    }

    fun markStorySeen(chapterId: Int) {
        val current = _progress.value
        val newStories = current.storiesSeen.toMutableSet().apply { add(chapterId) }
        val updated = current.copy(storiesSeen = newStories)
        saveProgress(updated)
    }

    fun mergeWithRemote(remote: PlayerProgress): PlayerProgress {
        val local = _progress.value

        val mergedStars = (local.stars.keys + remote.stars.keys).associateWith { key ->
            maxOf(local.stars[key] ?: 0, remote.stars[key] ?: 0)
        }

        val mergedScores = (local.scores.keys + remote.scores.keys).associateWith { key ->
            maxOf(local.scores[key] ?: 0, remote.scores[key] ?: 0)
        }

        val mergedSolved = local.solvedPuzzles + remote.solvedPuzzles
        val mergedStories = local.storiesSeen + remote.storiesSeen

        val mergedPuzzleScores = (local.puzzleScores.keys + remote.puzzleScores.keys).associateWith { key ->
            val l = local.puzzleScores[key]
            val r = remote.puzzleScores[key]
            when {
                l == null && r != null -> r
                l != null && r == null -> l
                l != null && r != null -> PuzzleScoreRecord(
                    levelId = key,
                    highScore = maxOf(l.highScore, r.highScore),
                    bestTimeSeconds = if (l.bestTimeSeconds > 0 && r.bestTimeSeconds > 0) {
                        minOf(l.bestTimeSeconds, r.bestTimeSeconds)
                    } else maxOf(l.bestTimeSeconds, r.bestTimeSeconds),
                    fewestMoves = if (l.fewestMoves > 0 && r.fewestMoves > 0) {
                        minOf(l.fewestMoves, r.fewestMoves)
                    } else maxOf(l.fewestMoves, r.fewestMoves),
                    stars = maxOf(l.stars, r.stars),
                    timestamp = maxOf(l.timestamp, r.timestamp)
                )
                else -> PuzzleScoreRecord(levelId = key, highScore = 0, bestTimeSeconds = 0, fewestMoves = 0)
            }
        }

        val merged = PlayerProgress(
            stars = mergedStars,
            scores = mergedScores,
            solvedPuzzles = mergedSolved,
            storiesSeen = mergedStories,
            puzzleScores = mergedPuzzleScores
        )
        saveProgress(merged)
        return merged
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
        _progress.value = PlayerProgress()
    }


    // Chapters Definition
    private val chapterTemplates = listOf(
        ChapterDef(
            title = "بوابة الرمل",
            hook = "قاهرة ٢٠٧٧: تحت الرمل تستيقظ بوابة فرعونية.",
            lines = listOf(
                "قاهرة، ٢٠٧٧. المطر يلمع على الأسفلت كما يلمع الذهب على جدار مقبرة قديمة.",
                "تحت سوق النيون، فتح العمال بوابة لم تُفتح منذ ثلاثة آلاف عام.",
                "خرجت منها الأحجار: قرص الشمس، مفتاح الحياة، الجعران… تنبض بضوء أزرق بين كابلات الألياف.",
                "أنا نور، حارسة الأرشيف الفرعوني. عليّ أن أطابق الأحجار وأعيد شحن البوابة قبل أن يبتلع «الصمت» شبكة المدينة."
            ),
            levelNames = listOf("بوابة الرمل", "رصيف الكابلات", "أول ضوء", "همس الجعران", "مفتاح الحياة", "لوح الحارس")
        ),
        ChapterDef(
            title = "سوق النيون",
            hook = "الذاكرة تُباع بالجرام، والأحجار تشتري الضوء.",
            lines = listOf(
                "في السوق يبيع الناس الذاكرة بالجرام، ويهمسون عن بوابة تشرب الكهرباء.",
                "كل صفّ من الأحجار أطابقه يوقظ جزءًا من الخريطة القديمة.",
                "قال التاجر العجوز: البوابة لا تُفتح بالقوة، بل بالنمط الصحيح.",
                "إن أحكمت الترتيب، ستكشف شفرة النيون التي تحمي القاهرة من الانهيار."
            ),
            levelNames = listOf("بساط السوق", "شفرة التاجر", "ممر الفوانيس", "حجر مقابل حجر", "سرّ النيون", "لوح القناع")
        ),
        ChapterDef(
            title = "مقبرة السيرفرات",
            hook = "تابوت حجري واحد يضم ألف سيرفر.",
            lines = listOf(
                "تحت المدينة، صفوف من السيرفرات داخل تابوت حجري واحد.",
                "اللغز هنا ليس في الحجر، بل في الترتيب: من يخطئ في النمط يعيد من البداية.",
                "وراء كل جدار عين تراقب: نظام قديم يستيقظ ببطء.",
                "أحجار رع المشحونة بالطاقة هي السلاح الوحيد لتهدئة السيرفرات المتمردة."
            ),
            levelNames = listOf("السلّم النازل", "تابوت السيرفرات", "عين المراقبة", "نبض البيانات", "باب الأرقام", "لوح الذاكرة")
        ),
        ChapterDef(
            title = "قصر رع الأخير",
            hook = "زجاج أسود وحجر رملي يطفوان فوق النيل.",
            lines = listOf(
                "آخر بوابة: قصر من زجاج أسود وحجر رملي، يطفو فوق النيل.",
                "إن وصلتِ، يعود الضوء إلى الشبكة… وإن تأخرتِ، يصمت كل شيء.",
                "أحجار رع الأخيرة في انتظارك، والمدينة تترقب الشرارة النهائية.",
                "أنتِ الآن الحارسة الوحيدة لسرّ الخلود الرقمي."
            ),
            levelNames = listOf("مرآة النيل", "قاعة الأعمدة", "قلب الزجاج", "آخر شرارة", "سماء رع", "لوح الخلود")
        )
    )

    private val extraTitles = listOf(
        "مرصد الأنوبيس", "قناة الضوء", "برج المسلات", "سراديب حورس",
        "ميناء الأقمار", "مصنع الجعران", "مكتبة الطين", "جسر الكواكب",
        "محرقة الأصفار", "واحة الشفرات", "معبد البايت", "مرآة إيزيس"
    )

    private val extraHooks = listOf(
        "الرمل يتحوّل إلى زجاج، والزجاج يحفظ الأسماء.",
        "خطوط الليزر ترسم خريطة لمقبرة لم تُفتح بعد.",
        "تحت الأرض، نبض بطيء يعدّ الحركات.",
        "كل حجر تطابقه يوقظ حرفًا من كتاب قديم.",
        "النيون ينبض باللون الذهبي: البوابة تقترب."
    )

    private val worldConfigs = listOf(
        WorldConfig(rows = 7, cols = 7, gemTypes = 5, moves = 26, perMove = 78),
        WorldConfig(rows = 7, cols = 7, gemTypes = 5, moves = 25, perMove = 78),
        WorldConfig(rows = 8, cols = 8, gemTypes = 6, moves = 26, perMove = 66),
        WorldConfig(rows = 8, cols = 8, gemTypes = 6, moves = 27, perMove = 66)
    )

    fun getChapter(rawChapterId: Int): Chapter {
        val chapterId = rawChapterId.coerceIn(1, MAX_CHAPTERS)
        val worldIdx = (chapterId - 1) % worldConfigs.size
        val config = worldConfigs[worldIdx]

        val title: String
        val hook: String
        val lines: List<String>
        val levelNames: List<String>

        if (chapterId <= chapterTemplates.size) {
            val def = chapterTemplates[chapterId - 1]
            title = def.title
            hook = def.hook
            lines = def.lines
            levelNames = def.levelNames
        } else {
            val extraIdx = chapterId - chapterTemplates.size - 1
            val baseTitle = extraTitles[extraIdx % extraTitles.size]
            val cycle = (extraIdx / extraTitles.size) + 1
            title = if (cycle == 1) baseTitle else "$baseTitle · ${toArabicNumerals(cycle)}"
            hook = extraHooks[extraIdx % extraHooks.size]
            lines = listOf(
                "طبقة أعمق من وادي النيون تنكشف أمامك في الفصل ${toArabicNumerals(chapterId)}.",
                hook,
                "طابقي أحجار رع وأثبتي براعتك في فك الشفرات الفرعونية قبل أن يبتلعها الصمت."
            )
            levelNames = listOf("المدخل", "الممر", "القاعة", "القلب", "القمة", "لوح الختام")
        }

        val levels = (1..6).map { idx ->
            val isBoss = idx == 6
            val puzzleKind = when {
                isBoss -> PuzzleKind.TILE
                idx % 2 == 0 -> PuzzleKind.SEQUENCE
                else -> PuzzleKind.RIDDLE
            }
            val baseTarget = config.moves * config.perMove
            val target = ((baseTarget * (0.45 + (idx - 1) * 0.03)) / 10).toInt() * 10

            GameLevel(
                id = "$chapterId-$idx",
                chapterId = chapterId,
                index = idx,
                name = levelNames[idx - 1],
                rows = config.rows,
                cols = config.cols,
                gemTypeCount = config.gemTypes,
                moves = config.moves,
                targetScore = target,
                isBoss = isBoss,
                puzzleKind = puzzleKind
            )
        }

        return Chapter(
            id = chapterId,
            tag = "الفصل ${toArabicNumerals(chapterId)}",
            title = title,
            hook = hook,
            lines = lines,
            levels = levels
        )
    }

    fun getAllInitialChapters(): List<Chapter> {
        return (1..chapterTemplates.size).map { getChapter(it) }
    }

    fun getLevel(levelId: String): GameLevel? {
        val parts = levelId.split("-")
        if (parts.size != 2) return null
        val chapterId = parts[0].toIntOrNull() ?: return null
        val levelIndex = parts[1].toIntOrNull() ?: return null
        if (levelIndex !in 1..6) return null
        val chapter = getChapter(chapterId)
        return chapter.levels.getOrNull(levelIndex - 1)
    }

    fun getNextLevelId(currentLevelId: String): String? {
        val parts = currentLevelId.split("-")
        if (parts.size != 2) return null
        val chapterId = parts[0].toIntOrNull() ?: return null
        val levelIndex = parts[1].toIntOrNull() ?: return null
        return if (levelIndex < 6) {
            "$chapterId-${levelIndex + 1}"
        } else if (chapterId < MAX_CHAPTERS) {
            "${chapterId + 1}-1"
        } else {
            null
        }
    }

    fun getPreviousLevelId(currentLevelId: String): String? {
        val parts = currentLevelId.split("-")
        if (parts.size != 2) return null
        val chapterId = parts[0].toIntOrNull() ?: return null
        val levelIndex = parts[1].toIntOrNull() ?: return null
        return if (levelIndex > 1) {
            "$chapterId-${levelIndex - 1}"
        } else if (chapterId > 1) {
            "${chapterId - 1}-6"
        } else {
            null
        }
    }

    fun isLevelUnlocked(levelId: String, progress: PlayerProgress): Boolean {
        if (levelId == "1-1") return true
        val prev = getPreviousLevelId(levelId) ?: return false
        val prevStars = progress.getStarsFor(prev)
        val prevPuzzleSolved = progress.isPuzzleSolved(prev)
        return prevStars > 0 && prevPuzzleSolved
    }

    fun getFrontierLevelId(progress: PlayerProgress): String {
        val completed = progress.stars.filter { it.value > 0 }.keys
        if (completed.isEmpty()) return "1-1"

        // Find the latest completed level
        var latestChapter = 1
        var latestIndex = 1
        for (lvl in completed) {
            val parts = lvl.split("-")
            if (parts.size == 2) {
                val c = parts[0].toIntOrNull() ?: 1
                val i = parts[1].toIntOrNull() ?: 1
                if (c > latestChapter || (c == latestChapter && i > latestIndex)) {
                    latestChapter = c
                    latestIndex = i
                }
            }
        }

        val next = getNextLevelId("$latestChapter-$latestIndex")
        return if (next != null && isLevelUnlocked(next, progress)) next else "$latestChapter-$latestIndex"
    }

    // Riddles Database
    private val riddles = listOf(
        RiddleDef(
            prompt = "أحرس الملوك منذ ثلاثة آلاف عام، وأحمل ألف غرفة، ولا أُبصر. من أنا؟",
            options = listOf("المصباح", "الهرم", "المفتاح"),
            answer = 1,
            explain = "الهرم: مقبرة الملوك الشامخة، بلا عين واحدة."
        ),
        RiddleDef(
            prompt = "كلما أخذت منها كبرت. ما هي؟",
            options = listOf("الحفرة", "الظل", "الصدى"),
            answer = 0,
            explain = "الحفرة: تكبر وتتسع كلما أزلت منها تراباً."
        ),
        RiddleDef(
            prompt = "يمشي بلا قدمين، ويبكي بلا عينين. ما هو؟",
            options = listOf("الحجر", "المفتاح", "السحاب"),
            answer = 2,
            explain = "السحاب: يسير مع الريح ويمطر كما يبكي."
        ),
        RiddleDef(
            prompt = "له أسنان ولا يعضّ. ما هو؟",
            options = listOf("الجعران", "المشط", "الشمس"),
            answer = 1,
            explain = "المشط: له أسنان منتظمة ولا يعض."
        ),
        RiddleDef(
            prompt = "مرآة لا تُظهر وجهك، بل تحفظ تاريخك، وتعيش في السماء. ما هي؟",
            options = listOf("السحابة الرقمية", "البوابة", "الفانوس"),
            answer = 0,
            explain = "السحابة الرقمية (Cloud): تحفظ بياناتك وذكرياتك في الفضاء السيبراني."
        ),
        RiddleDef(
            prompt = "يُكسر بمجرد أن تنطق باسمه. ما هو؟",
            options = listOf("الزجاج", "القفل", "الصمت"),
            answer = 2,
            explain = "الصمت: بمجرد أن تتكلم وتلفظ اسمه، ينكسر."
        ),
        RiddleDef(
            prompt = "ابن أبيك وأمك، وليس بأخيك. من هو؟",
            options = listOf("ابن عمك", "أنت", "جارك"),
            answer = 1,
            explain = "أنت: ابن أبيك وأمك ولست بأخيك بل أنت ذاتك."
        ),
        RiddleDef(
            prompt = "بيت من زجاج بلا باب، فيه ألف سرّ ورمز. ما هو؟",
            options = listOf("المرآة", "الخريطة", "الشاشة"),
            answer = 2,
            explain = "الشاشة: نافذة رقمية زجاجية تحمل ملايين الأسرار والمعلومات."
        ),
        RiddleDef(
            prompt = "أول شرارة تُشعل ألف شمعة، وما زالت واحدة لا تنقص. ما هي؟",
            options = listOf("الجعران", "الفكرة", "المفتاح"),
            answer = 1,
            explain = "الفكرة والمعرفة: تلهم آلاف العقول دون أن تنقص شعلتها."
        ),
        RiddleDef(
            prompt = "كلما شاركتها زادت ولم تنقص. ما هي؟",
            options = listOf("المعرفة", "الذهب", "الماء"),
            answer = 0,
            explain = "المعرفة: تفيض وتتضاعف كلما تبادلتها مع الآخرين."
        ),
        RiddleDef(
            prompt = "له ألف عين ولا يرى، ويسهر الليل كله. ما هو؟",
            options = listOf("البومة", "السيرفر", "السماء"),
            answer = 2,
            explain = "السماء المرصعة بنجوم الليل: آلاف الأعين المتلألئة."
        ),
        RiddleDef(
            prompt = "يكبر ولا يصغر، ويمضي ولا يعود. ما هو؟",
            options = listOf("الظل", "النهر", "العمر"),
            answer = 2,
            explain = "العمر والزمن: يمضي إلى الأمام ولا يرجع إلى الوراء."
        )
    )

    // Sequence Puzzles Database
    private val sequencePuzzles = listOf(
        SequenceDef(
            terms = listOf("٣", "٦", "٩", "١٢"),
            options = listOf("١٥", "١٤", "١٦"),
            answer = 0,
            explain = "النمط: مضاعفات الرقم ٣ (زيادة ٣ في كل خطوة)."
        ),
        SequenceDef(
            terms = listOf("٢", "٤", "٨", "١٦"),
            options = listOf("٢٤", "٣٢", "٦٤"),
            answer = 1,
            explain = "النمط: يتضاعف الرقم بالضرب في ٢ كل خطوة."
        ),
        SequenceDef(
            terms = listOf("١", "١", "٢", "٣", "٥", "٨"),
            options = listOf("١١", "١٢", "١٣"),
            answer = 2,
            explain = "متتالية فيبوناتشي: كل رقم هو مجموع الرقمين السابقين له (٥ + ٨ = ١٣)."
        ),
        SequenceDef(
            terms = listOf("٢٠", "١٧", "١٤"),
            options = listOf("١١", "١٢", "١٠"),
            answer = 0,
            explain = "النمط: نقصان ٣ درجات في كل خطوة."
        ),
        SequenceDef(
            terms = listOf("١", "٤", "٩", "١٦"),
            options = listOf("٢٠", "٢٥", "٣٦"),
            answer = 1,
            explain = "مربعات الأعداد: ١²=١، ٢²=٤، ٣²=٩، ٤²=١٦، ٥²=٢٥."
        ),
        SequenceDef(
            terms = listOf("٣", "٩", "٢٧"),
            options = listOf("٥٤", "٧٢", "٨١"),
            answer = 2,
            explain = "النمط: قوى الرقم ٣ (الضرب في ٣ كل مرة: ٢٧ × ٣ = ٨١)."
        ),
        SequenceDef(
            terms = listOf("٦٤", "٣٢", "١٦", "٨"),
            options = listOf("٤", "٦", "٢"),
            answer = 0,
            explain = "النمط: التنصيف والقسمة على ٢ في كل خطوة (٨ ÷ ٢ = ٤)."
        ),
        SequenceDef(
            terms = listOf("٥", "١٠", "٢٠", "٤٠"),
            options = listOf("٦٠", "٨٠", "١٠٠"),
            answer = 1,
            explain = "النمط: مضاعفة الرقم بالضرب في ٢ (٤٠ × ٢ = ٨٠)."
        )
    )

    // Tile sliding puzzles
    private val tileConfigs = listOf(
        TileDef(name = "لوح الحارس", labels = listOf("١", "٢", "٣", "٤", "٥", "٦", "٧", "٨"), shuffles = 14),
        TileDef(name = "لوح القناع", labels = listOf("△", "◇", "□", "○", "▲", "◆", "■", "●"), shuffles = 22),
        TileDef(name = "لوح الذاكرة", labels = listOf("★", "☆", "☥", "✧", "✦", "◉", "◈", "△"), shuffles = 28),
        TileDef(name = "لوح الخلود", labels = listOf("☥", "★", "◇", "✧", "◆", "○", "■", "△"), shuffles = 34)
    )

    fun getPuzzleForLevel(level: GameLevel): GamePuzzle {
        return when (level.puzzleKind) {
            PuzzleKind.TILE -> {
                val tileDef = tileConfigs[(level.chapterId - 1) % tileConfigs.size]
                val shuffledBoard = generateShuffledTileBoard(tileDef.shuffles)
                TilePuzzle(
                    id = level.id,
                    name = tileDef.name,
                    labels = tileDef.labels,
                    shuffles = tileDef.shuffles,
                    initialBoard = shuffledBoard
                )
            }
            PuzzleKind.SEQUENCE -> {
                val idx = (level.chapterId * 31 + level.index * 7) % sequencePuzzles.size
                val def = sequencePuzzles[idx]
                SequencePuzzle(
                    id = level.id,
                    terms = def.terms,
                    options = def.options,
                    answerIndex = def.answer,
                    explanation = def.explain
                )
            }
            PuzzleKind.RIDDLE -> {
                val idx = (level.chapterId * 31 + level.index * 7) % riddles.size
                val def = riddles[idx]
                RiddlePuzzle(
                    id = level.id,
                    prompt = def.prompt,
                    options = def.options,
                    answerIndex = def.answer,
                    explanation = def.explain
                )
            }
        }
    }

    private fun generateShuffledTileBoard(shuffles: Int): List<Int> {
        val target = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
        var board = target.toMutableList()
        var emptyPos = 8
        var prevPos = -1

        for (i in 0 until shuffles) {
            val neighbors = getTileNeighbors(emptyPos).filter { it != prevPos }
            val chosen = neighbors.random()
            prevPos = emptyPos
            // Swap emptyPos and chosen
            board[emptyPos] = board[chosen]
            board[chosen] = 0
            emptyPos = chosen
        }

        // If somehow ended up solved, swap one valid move
        if (board == target) {
            val neighbors = getTileNeighbors(emptyPos)
            val chosen = neighbors.first()
            board[emptyPos] = board[chosen]
            board[chosen] = 0
        }

        return board
    }

    fun getTileNeighbors(index: Int): List<Int> {
        val row = index / 3
        val col = index % 3
        val list = mutableListOf<Int>()
        if (row > 0) list.add(index - 3)
        if (row < 2) list.add(index + 3)
        if (col > 0) list.add(index - 1)
        if (col < 2) list.add(index + 1)
        return list
    }

    companion object {
        const val MAX_CHAPTERS = 5200
        const val TOTAL_STAGES = MAX_CHAPTERS * 6

        fun toArabicNumerals(text: String): String {
            val arabicDigits = listOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            return text.map { char ->
                if (char in '0'..'9') arabicDigits[char - '0'] else char
            }.joinToString("")
        }

        fun toArabicNumerals(num: Number): String {
            return toArabicNumerals(num.toString())
        }

        fun toArabicNumerals(num: Int): String {
            return toArabicNumerals(num.toString())
        }
    }

    private data class ChapterDef(
        val title: String,
        val hook: String,
        val lines: List<String>,
        val levelNames: List<String>
    )

    private data class WorldConfig(
        val rows: Int,
        val cols: Int,
        val gemTypes: Int,
        val moves: Int,
        val perMove: Int
    )

    private data class RiddleDef(
        val prompt: String,
        val options: List<String>,
        val answer: Int,
        val explain: String
    )

    private data class SequenceDef(
        val terms: List<String>,
        val options: List<String>,
        val answer: Int,
        val explain: String
    )

    private data class TileDef(
        val name: String,
        val labels: List<String>,
        val shuffles: Int
    )
}
