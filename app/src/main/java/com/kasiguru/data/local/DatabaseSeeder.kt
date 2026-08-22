package com.kasiguru.data.local

import com.kasiguru.data.local.entity.*
import com.kasiguru.util.Constants

/**
 * Complete Kasiguranin Linguistic Corpus DatabaseSeeder.
 * Extracted directly from UP Thesis: A Grammatical Sketch of Kasiguranin (Supnet, 2016).
 * Contains 394 vocabulary entries.
 */
object DatabaseSeeder {

    fun getInitialGameLevels(): List<GameLevelEntity> {
        val levels = mutableListOf<GameLevelEntity>()
        val gameTypes = listOf("word_match", "reverse_match", "fill_blank", "audio_quiz", "aspect_builder", "sentence_order")
        
        for (game in gameTypes) {
            for (levelNum in 1..30) {
                val difficulty = when (levelNum) {
                    in 1..10 -> "Easy"
                    in 11..20 -> "Medium"
                    else -> "Hard"
                }
                val questionsCount = when (difficulty) {
                    "Easy" -> 5
                    "Medium" -> 10
                    else -> 15
                }
                levels.add(
                    GameLevelEntity(
                        gameType = game,
                        levelNumber = levelNum,
                        difficulty = difficulty,
                        isUnlocked = levelNum == 1, // Only level 1 is unlocked initially
                        questionsCount = questionsCount
                    )
                )
            }
        }
        return levels
    }

    fun getInitialVocabulary(): List<VocabularyEntity> = listOf(
        VocabularyEntity(
            kasiguranin = "apak",
            tagalog = "daras",
            english = "adze",
            rootForm = "apak",
            category = "Occupations & Tools",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁaː.pak"
        ),
        VocabularyEntity(
            kasiguranin = "buhay",
            tagalog = "buhay",
            english = "alive",
            rootForm = "buhay",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈhaj"
        ),
        VocabularyEntity(
            kasiguranin = "kanga",
            tagalog = "galit",
            english = "anger",
            rootForm = "kanga",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈkaː.ŋaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "hayop",
            tagalog = "hayop",
            english = "animal",
            rootForm = "hayop",
            category = "Animals & Wildlife",
            phoneticVowelLength = true,
            ipaNotation = "ˈhaː.jɔp"
        ),
        VocabularyEntity(
            kasiguranin = "bukong bokong",
            tagalog = "bukung-bukong",
            english = "ankle",
            rootForm = "bukong",
            category = "Body Parts & Health",
            phoneticVowelLength = true,
            ipaNotation = "bʊ.kɔŋ.ˈbɔː.kɔŋ"
        ),
        VocabularyEntity(
            kasiguranin = "singët",
            tagalog = "langgam",
            english = "ant",
            rootForm = "singët",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "si.ˈŋət"
        ),
        VocabularyEntity(
            kasiguranin = "braso",
            tagalog = "bisig",
            english = "arm",
            rootForm = "braso",
            category = "Body Parts & Health",
            phoneticVowelLength = true,
            ipaNotation = "ˈbraː.sɔ"
        ),
        VocabularyEntity(
            kasiguranin = "kili-kile",
            tagalog = "kili-kili",
            english = "armpit",
            rootForm = "kilikile",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ki.li.ki.ˈlɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "abo",
            tagalog = "abo",
            english = "ashes",
            rootForm = "abo",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈbɔ"
        ),
        VocabularyEntity(
            kasiguranin = "lukag",
            tagalog = "gising",
            english = "awake",
            rootForm = "lukag",
            category = "Body Parts & Health",
            ipaNotation = "lʊ.ˈkag"
        ),
        VocabularyEntity(
            kasiguranin = "adëg",
            tagalog = "likod",
            english = "back",
            rootForm = "adëg",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈdəg"
        ),
        VocabularyEntity(
            kasiguranin = "madukës",
            tagalog = "masama",
            english = "bad",
            rootForm = "madukës",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "ma.du.ˈkəs"
        ),
        VocabularyEntity(
            kasiguranin = "pokpok",
            tagalog = "kalbo",
            english = "bald",
            rootForm = "pokpok",
            category = "Body Parts & Health",
            ipaNotation = "ˈpɔk.pɔk"
        ),
        VocabularyEntity(
            kasiguranin = "kawayan",
            tagalog = "kawayan",
            english = "bamboo",
            rootForm = "kawayan",
            category = "Nature & Environment",
            ipaNotation = "ka.wa.ˈjan"
        ),
        VocabularyEntity(
            kasiguranin = "kulet ng kayo",
            tagalog = "balat ng kahoy",
            english = "bark",
            rootForm = "kulet",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "kʊ.ˈlet naŋ ka.ˈjɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tiis tiis",
            tagalog = "suffer",
            english = "bear,",
            rootForm = "tiis",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "ti.ˈɁis"
        ),
        VocabularyEntity(
            kasiguranin = "umeng",
            tagalog = "bigote",
            english = "mustache",
            rootForm = "umeng",
            category = "Body Parts & Health",
            ipaNotation = "bal.ˈbas"
        ),
        VocabularyEntity(
            kasiguranin = "maganda",
            tagalog = "maganda",
            english = "beautiful",
            rootForm = "maganda",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈgan.da"
        ),
        VocabularyEntity(
            kasiguranin = "tiyan",
            tagalog = "tiyan",
            english = "belly",
            rootForm = "tiyan",
            category = "Body Parts & Health",
            ipaNotation = "ti.ˈjan"
        ),
        VocabularyEntity(
            kasiguranin = "dikkël",
            tagalog = "Malaki",
            english = "big",
            rootForm = "dikkël",
            category = "Colors & Shapes",
            phoneticGlottal = true,
            ipaNotation = "dik.ˈkəl"
        ),
        VocabularyEntity(
            kasiguranin = "apdu",
            tagalog = "apdu",
            english = "bile",
            rootForm = "apdu",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ˈɁap.dʊ"
        ),
        VocabularyEntity(
            kasiguranin = "ibon",
            tagalog = "ibon",
            english = "bird",
            rootForm = "ibon",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁiː.bɔn"
        ),
        VocabularyEntity(
            kasiguranin = "mapet",
            tagalog = "mapait",
            english = "bitter",
            rootForm = "mapet",
            category = "Food & Dining",
            ipaNotation = "ma.ˈpɛt"
        ),
        VocabularyEntity(
            kasiguranin = "mangitet",
            tagalog = "maitim",
            english = "black",
            rootForm = "mangitet",
            category = "Colors & Shapes",
            ipaNotation = "ma.ŋi.ˈtɛt"
        ),
        VocabularyEntity(
            kasiguranin = "tadëm",
            tagalog = "talim",
            english = "blade/sharpness",
            rootForm = "tadëm",
            category = "Occupations & Tools",
            phoneticGlottal = true,
            ipaNotation = "ta.ˈdəm"
        ),
        VocabularyEntity(
            kasiguranin = "burëk",
            tagalog = "bulag",
            english = "blind",
            rootForm = "burëk",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bʊ.ˈrək"
        ),
        VocabularyEntity(
            kasiguranin = "digi",
            tagalog = "dugo",
            english = "blood",
            rootForm = "digi",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "di.ˈgiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "bëggi",
            tagalog = "katawan",
            english = "body",
            rootForm = "bëggi",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bəg.ˈgiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tulang",
            tagalog = "tinik",
            english = "fishbone",
            rootForm = "",
            category = "Animals & Wildlife",
            ipaNotation = "tʊ.ˈlaŋ"
        ),
        VocabularyEntity(
            kasiguranin = "utëk",
            tagalog = "utak",
            english = "brain",
            rootForm = "utëk",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈtək"
        ),
        VocabularyEntity(
            kasiguranin = "sanga",
            tagalog = "sanga",
            english = "branch",
            rootForm = "sanga",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "sa.ˈŋaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "suso",
            tagalog = "suso",
            english = "breast",
            rootForm = "suso",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "sʊ.ˈsɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "mademlag",
            tagalog = "maliwanag",
            english = "bright",
            rootForm = "mademlag",
            category = "Colors & Shapes",
            ipaNotation = "ma.ˈdɛm.lag"
        ),
        VocabularyEntity(
            kasiguranin = "bayaw",
            tagalog = "bayaw",
            english = "brother-in-law",
            rootForm = "bayaw",
            category = "Family & People",
            ipaNotation = "ba.ˈjaw"
        ),
        VocabularyEntity(
            kasiguranin = "bëdbëd",
            tagalog = "bigkis",
            english = "bundle",
            rootForm = "bëdbëd",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "bəd.ˈbəd"
        ),
        VocabularyEntity(
            kasiguranin = "paruparo",
            tagalog = "paruparo",
            english = "butterfly",
            rootForm = "paruparo",
            category = "Animals & Wildlife",
            ipaNotation = "pa.rʊ.pa.ˈrɔ"
        ),
        VocabularyEntity(
            kasiguranin = "bule",
            tagalog = "puwit, puwitan",
            english = "buttocks",
            rootForm = "bule",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bʊ.ˈleɁ"
        ),
        VocabularyEntity(
            kasiguranin = "dakëp",
            tagalog = "apprehend",
            english = "catch,",
            rootForm = "dakëp",
            category = "Greetings & Essentials",
            ipaNotation = "da.ˈkəp"
        ),
        VocabularyEntity(
            kasiguranin = "biro",
            tagalog = "uling",
            english = "charcoal",
            rootForm = "biro",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "bi.ˈrɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "padingel",
            tagalog = "pisngi",
            english = "cheek",
            rootForm = "padingel",
            category = "Body Parts & Health",
            ipaNotation = "pa.di.ˈŋel"
        ),
        VocabularyEntity(
            kasiguranin = "rakaw",
            tagalog = "dibdib",
            english = "chest",
            rootForm = "rakaw",
            category = "Body Parts & Health",
            ipaNotation = "ra.ˈkaw"
        ),
        VocabularyEntity(
            kasiguranin = "sepsep",
            tagalog = "sisiw",
            english = "chick",
            rootForm = "sepsep",
            category = "Animals & Wildlife",
            ipaNotation = "ˈsep.sep"
        ),
        VocabularyEntity(
            kasiguranin = "manok",
            tagalog = "manok",
            english = "chicken",
            rootForm = "manok",
            category = "Animals & Wildlife",
            ipaNotation = "ma.ˈnɔk"
        ),
        VocabularyEntity(
            kasiguranin = "pinuno",
            tagalog = "pinuno",
            english = "chief",
            rootForm = "pinuno",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "pi.nʊ.nʊɁ"
        ),
        VocabularyEntity(
            kasiguranin = "anak anak",
            tagalog = "(young)",
            english = "child",
            rootForm = "anak",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈnak"
        ),
        VocabularyEntity(
            kasiguranin = "timed",
            tagalog = "baba",
            english = "chin",
            rootForm = "timed",
            category = "Body Parts & Health",
            ipaNotation = "ti.ˈmed"
        ),
        VocabularyEntity(
            kasiguranin = "malinis",
            tagalog = "malinis",
            english = "clean",
            rootForm = "malinis",
            category = "House & Daily Life",
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈliː.nis"
        ),
        VocabularyEntity(
            kasiguranin = "ulap",
            tagalog = "ulap",
            english = "cloud",
            rootForm = "ulap",
            category = "Weather & Climate",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁʊː.lap"
        ),
        VocabularyEntity(
            kasiguranin = "ipës",
            tagalog = "ipis",
            english = "cockroach",
            rootForm = "ipës",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "Ɂi.ˈpəs"
        ),
        VocabularyEntity(
            kasiguranin = "niyog",
            tagalog = "niyog",
            english = "coconut",
            rootForm = "niyog",
            category = "Food & Dining",
            ipaNotation = "ni.ˈjɔg"
        ),
        VocabularyEntity(
            kasiguranin = "korkoran",
            tagalog = "kudkuran",
            english = "coconut",
            rootForm = "korkoran",
            category = "Food & Dining",
            phoneticVowelLength = true,
            ipaNotation = "kɔr.ˈkɔː.ran"
        ),
        VocabularyEntity(
            kasiguranin = "gata gata",
            tagalog = "milk",
            english = "coconut",
            rootForm = "gata",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "ga.ˈtaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "madëgnen",
            tagalog = "maginaw",
            english = "cold",
            rootForm = "madëgnen",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "ma.dəg.ˈnɛn"
        ),
        VocabularyEntity(
            kasiguranin = "bangkay",
            tagalog = "bangkay",
            english = "corpse",
            rootForm = "bangkay",
            category = "Body Parts & Health",
            ipaNotation = "baŋ.ˈkaj"
        ),
        VocabularyEntity(
            kasiguranin = "pensan",
            tagalog = "pinsan",
            english = "cousin",
            rootForm = "pensan",
            category = "Family & People",
            ipaNotation = "ˈpɛn.san"
        ),
        VocabularyEntity(
            kasiguranin = "buwaya",
            tagalog = "buwaya",
            english = "crocodile",
            rootForm = "buwaya",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "bʊ.wa.ˈyaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "wakwak",
            tagalog = "uwak",
            english = "crow",
            rootForm = "wakwak",
            category = "Animals & Wildlife",
            ipaNotation = "wak.ˈwak"
        ),
        VocabularyEntity(
            kasiguranin = "kulot kulot",
            tagalog = "hair",
            english = "curly",
            rootForm = "kulot",
            category = "Body Parts & Health",
            ipaNotation = "kʊ.ˈlɔt"
        ),
        VocabularyEntity(
            kasiguranin = "madeklëm",
            tagalog = "madilim",
            english = "dark",
            rootForm = "madeklëm",
            category = "Colors & Shapes",
            phoneticGlottal = true,
            ipaNotation = "ma.dik.ˈləm"
        ),
        VocabularyEntity(
            kasiguranin = "or 24 hrs) araw (also, sun) aldew",
            tagalog = "(12",
            english = "day",
            rootForm = "or",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "ˈɁal.dɛw"
        ),
        VocabularyEntity(
            kasiguranin = "umaga",
            tagalog = "umaga",
            english = "daytime",
            rootForm = "umaga",
            category = "Numbers & Time",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂʊ.ˈmaː.ga"
        ),
        VocabularyEntity(
            kasiguranin = "utang",
            tagalog = "utang",
            english = "debt",
            rootForm = "utang",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈtaŋ"
        ),
        VocabularyEntity(
            kasiguranin = "madisalad (hukay)",
            tagalog = "malalim",
            english = "deep",
            rootForm = "madisalad",
            category = "Colors & Shapes",
            phoneticVowelLength = true,
            ipaNotation = "maː.di.ˈsaː.lad"
        ),
        VocabularyEntity(
            kasiguranin = "ogsa",
            tagalog = "usa",
            english = "deer",
            rootForm = "ogsa",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "ˈɁɔg.saɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tibag",
            tagalog = "giba",
            english = "demolish",
            rootForm = "tibag",
            category = "House & Daily Life",
            ipaNotation = "ti.ˈbag"
        ),
        VocabularyEntity(
            kasiguranin = "hamog",
            tagalog = "hamog",
            english = "dew",
            rootForm = "hamog",
            category = "Weather & Climate",
            ipaNotation = "ha.ˈmɔg"
        ),
        VocabularyEntity(
            kasiguranin = "malëgga/marupet",
            tagalog = "marumi",
            english = "dirty",
            rootForm = "malëgga/marupet",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "ma.ləg.ˈgaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "aso",
            tagalog = "aso",
            english = "dog",
            rootForm = "aso",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈsɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "pinto",
            tagalog = "pinto",
            english = "door",
            rootForm = "pinto",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "ˈpin.tɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "didëbba",
            tagalog = "baba",
            english = "downward",
            rootForm = "didëbba",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "pa.di.dəb.ˈbaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "taginëp",
            tagalog = "panaginip",
            english = "dream",
            rootForm = "taginëp",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ta.ˈgiː.nəp"
        ),
        VocabularyEntity(
            kasiguranin = "tuyo tuyo",
            tagalog = "(substance)",
            english = "dry",
            rootForm = "tuyo",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "tʊ.ˈyɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "mangudël",
            tagalog = "mapurol",
            english = "dull",
            rootForm = "mangudël",
            category = "Occupations & Tools",
            phoneticGlottal = true,
            ipaNotation = "ma.ŋʊ.ˈdəl"
        ),
        VocabularyEntity(
            kasiguranin = "bulol",
            tagalog = "pipi",
            english = "deaf",
            rootForm = "bulol",
            category = "Body Parts & Health",
            ipaNotation = "bʊ.ˈlɔl"
        ),
        VocabularyEntity(
            kasiguranin = "alikabok",
            tagalog = "alikabok",
            english = "dust",
            rootForm = "alikabok",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.li.ka.ˈbɔk"
        ),
        VocabularyEntity(
            kasiguranin = "bëng-bëng",
            tagalog = "tainga",
            english = "ear",
            rootForm = "bngbng",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bəŋ.ˈbəŋ"
        ),
        VocabularyEntity(
            kasiguranin = "luta",
            tagalog = "lupa",
            english = "earth",
            rootForm = "luta",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "lʊ.ˈtaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tule",
            tagalog = "tutuli",
            english = "earwax",
            rootForm = "tule",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "tʊ.ˈleɁ"
        ),
        VocabularyEntity(
            kasiguranin = "bunay",
            tagalog = "itlog",
            english = "egg",
            rootForm = "bunay",
            category = "Food & Dining",
            ipaNotation = "bʊ.ˈnaj"
        ),
        VocabularyEntity(
            kasiguranin = "talung",
            tagalog = "talong",
            english = "eggplant",
            rootForm = "talung",
            category = "Food & Dining",
            ipaNotation = "ta.ˈluŋ"
        ),
        VocabularyEntity(
            kasiguranin = "walo",
            tagalog = "walo",
            english = "eight",
            rootForm = "walo",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "wa.ˈlɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "siko",
            tagalog = "siko",
            english = "elbow",
            rootForm = "siko",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "si.ˈkɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "coal baga baga",
            tagalog = "hot",
            english = "ember,",
            rootForm = "coal",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "baː.ˈga"
        ),
        VocabularyEntity(
            kasiguranin = "ëttog",
            tagalog = "latug",
            english = "erection",
            rootForm = "ëttog",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂət.ˈtɔg"
        ),
        VocabularyEntity(
            kasiguranin = "gibi",
            tagalog = "gabi",
            english = "evening",
            rootForm = "gibi",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "gi.ˈbiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "ëttay",
            tagalog = "dumi",
            english = "excrement",
            rootForm = "ëttay",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂət.ˈtay"
        ),
        VocabularyEntity(
            kasiguranin = "mata",
            tagalog = "mata",
            english = "eye",
            rootForm = "mata",
            category = "Body Parts & Health",
            ipaNotation = "ma.ˈta"
        ),
        VocabularyEntity(
            kasiguranin = "kiray",
            tagalog = "kilay",
            english = "eyebrow",
            rootForm = "kiray",
            category = "Body Parts & Health",
            ipaNotation = "ki.ˈraj"
        ),
        VocabularyEntity(
            kasiguranin = "rupa",
            tagalog = "mukha",
            english = "face",
            rootForm = "rupa",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "rʊ.ˈpaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "adayo",
            tagalog = "malayo",
            english = "far",
            rootForm = "adayo",
            category = "Numbers & Time",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂa.ˈdaː.jɔ"
        ),
        VocabularyEntity(
            kasiguranin = "hagut",
            tagalog = "mabilis",
            english = "fast",
            rootForm = "hagut",
            category = "Numbers & Time",
            ipaNotation = "ma.bi.ˈlis"
        ),
        VocabularyEntity(
            kasiguranin = "tabi",
            tagalog = "taba",
            english = "fat",
            rootForm = "tabi",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ta.ˈbiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tatay",
            tagalog = "ama",
            english = "father",
            rootForm = "tatay",
            category = "Family & People",
            phoneticVowelLength = true,
            ipaNotation = "ˈtaː.taj"
        ),
        VocabularyEntity(
            kasiguranin = "kudal",
            tagalog = "bakod",
            english = "fence",
            rootForm = "kudal",
            category = "House & Daily Life",
            ipaNotation = "kʊ.ˈdal"
        ),
        VocabularyEntity(
            kasiguranin = "sabaddit",
            tagalog = "kaunti",
            english = "few",
            rootForm = "sabaddit",
            category = "Numbers & Time",
            phoneticVowelLength = true,
            ipaNotation = "ˈsaː.bad.dit"
        ),
        VocabularyEntity(
            kasiguranin = "palekpek",
            tagalog = "palaypay",
            english = "fin",
            rootForm = "palekpek",
            category = "Animals & Wildlife",
            ipaNotation = "pa.lɛk.ˈpɛk"
        ),
        VocabularyEntity(
            kasiguranin = "guramët",
            tagalog = "daliri",
            english = "finger",
            rootForm = "guramët",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "gʊ.ra.ˈmət"
        ),
        VocabularyEntity(
            kasiguranin = "kuko",
            tagalog = "kuko",
            english = "fingernail",
            rootForm = "kuko",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "kʊ.ˈkɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "apoy",
            tagalog = "apoy",
            english = "fire",
            rootForm = "apoy",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈpɔj"
        ),
        VocabularyEntity(
            kasiguranin = "damo",
            tagalog = "una",
            english = "first",
            rootForm = "damo",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "da.ˈmɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "sida",
            tagalog = "isda",
            english = "fish",
            rootForm = "sida",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "si.ˈdaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "lima",
            tagalog = "lima",
            english = "five",
            rootForm = "lima",
            category = "Numbers & Time",
            ipaNotation = "li.ˈmaʔ"
        ),
        VocabularyEntity(
            kasiguranin = "ëttot",
            tagalog = "utot",
            english = "flatulence",
            rootForm = "ëttot",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂət.ˈtɔt"
        ),
        VocabularyEntity(
            kasiguranin = "baha",
            tagalog = "baha",
            english = "flood",
            rootForm = "baha",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "ba.ˈhaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "bulaklak",
            tagalog = "bulaklak",
            english = "flower",
            rootForm = "bulaklak",
            category = "Nature & Environment",
            ipaNotation = "bʊ.lak.ˈlak"
        ),
        VocabularyEntity(
            kasiguranin = "bula",
            tagalog = "bula",
            english = "foam",
            rootForm = "bula",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "bʊ.ˈlaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "alapok",
            tagalog = "ulop",
            english = "fog",
            rootForm = "alapok",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.la.ˈpɔk"
        ),
        VocabularyEntity(
            kasiguranin = "bësset",
            tagalog = "paa",
            english = "foot",
            rootForm = "bësset",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bəs.ˈsɛt"
        ),
        VocabularyEntity(
            kasiguranin = "muding",
            tagalog = "noo",
            english = "forehead",
            rootForm = "muding",
            category = "Body Parts & Health",
            ipaNotation = "mʊ.ˈdiŋ"
        ),
        VocabularyEntity(
            kasiguranin = "mabuyok",
            tagalog = "mabaho",
            english = "foul-smelling",
            rootForm = "mabuyok",
            category = "Emotions & Feelings",
            ipaNotation = "ma.bʊ.ˈjɔk"
        ),
        VocabularyEntity(
            kasiguranin = "appat",
            tagalog = "apat",
            english = "four",
            rootForm = "appat",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "Ɂəp.ˈpat"
        ),
        VocabularyEntity(
            kasiguranin = "masrob",
            tagalog = "mabango",
            english = "fragrant",
            rootForm = "masrob",
            category = "Emotions & Feelings",
            ipaNotation = "mas.ˈrɔb"
        ),
        VocabularyEntity(
            kasiguranin = "tukak",
            tagalog = "palaka",
            english = "frog",
            rootForm = "tukak",
            category = "Animals & Wildlife",
            ipaNotation = "tu.ˈkak"
        ),
        VocabularyEntity(
            kasiguranin = "bassog",
            tagalog = "busog",
            english = "full after eating",
            rootForm = "bassog",
            category = "Body Parts & Health",
            ipaNotation = "bəs.ˈsɔg"
        ),
        VocabularyEntity(
            kasiguranin = "putat",
            tagalog = "puno",
            english = "full",
            rootForm = "putat",
            category = "House & Daily Life",
            ipaNotation = "pʊ.ˈtat"
        ),
        VocabularyEntity(
            kasiguranin = "dutdut",
            tagalog = "balahibo",
            english = "fur",
            rootForm = "dutdut",
            category = "Animals & Wildlife",
            ipaNotation = "dʊt.ˈdʊt"
        ),
        VocabularyEntity(
            kasiguranin = "pagmulaan",
            tagalog = "halamanan",
            english = "garden",
            rootForm = "pagmulaan",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "pag.mʊ.la.ˈɁan"
        ),
        VocabularyEntity(
            kasiguranin = "asang",
            tagalog = "hasang",
            english = "gills",
            rootForm = "asang",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈsaŋ"
        ),
        VocabularyEntity(
            kasiguranin = "laya",
            tagalog = "luya",
            english = "ginger",
            rootForm = "laya",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "la.ˈjaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "diyos",
            tagalog = "bathala",
            english = "god",
            rootForm = "diyos",
            category = "Family & People",
            ipaNotation = "ˈʤɔs"
        ),
        VocabularyEntity(
            kasiguranin = "ginto",
            tagalog = "ginto",
            english = "gold",
            rootForm = "ginto",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "gin.ˈtɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "maigi",
            tagalog = "mabuti",
            english = "good",
            rootForm = "maigi",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈɁiː.giɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kaku na",
            tagalog = "paalam",
            english = "goodbye",
            rootForm = "kaku",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈkaː.ku ˈnaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "lamon",
            tagalog = "damo",
            english = "grass",
            rootForm = "lamon",
            category = "Nature & Environment",
            ipaNotation = "la.ˈmɔn"
        ),
        VocabularyEntity(
            kasiguranin = "uban",
            tagalog = "puting buhok",
            english = "gray",
            rootForm = "uban",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈban"
        ),
        VocabularyEntity(
            kasiguranin = "bituka",
            tagalog = "laman-loob",
            english = "guts",
            rootForm = "bituka",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bi.tʊ.ˈkaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "buhok",
            tagalog = "buhok",
            english = "hair",
            rootForm = "buhok",
            category = "Body Parts & Health",
            ipaNotation = "bʊ.ˈhɔk"
        ),
        VocabularyEntity(
            kasiguranin = "lima",
            tagalog = "kamay",
            english = "hand",
            rootForm = "lima",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "li.ˈmaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "maktog",
            tagalog = "matigas",
            english = "hard",
            rootForm = "maktog",
            category = "House & Daily Life",
            ipaNotation = "ˈmak.tɔg"
        ),
        VocabularyEntity(
            kasiguranin = "ulo",
            tagalog = "ulo",
            english = "head",
            rootForm = "ulo",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "u.ˈlɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "matabi",
            tagalog = "malusog",
            english = "healthy",
            rootForm = "matabi",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ma.ta.ˈbiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "puso",
            tagalog = "puso",
            english = "heart",
            rootForm = "puso",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "pʊ.ˈsɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tonok",
            tagalog = "taib",
            english = "high",
            rootForm = "taog",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "ˈtɔː.nɔk"
        ),
        VocabularyEntity(
            kasiguranin = "mainit",
            tagalog = "mainit",
            english = "hot",
            rootForm = "mainit",
            category = "Weather & Climate",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈɁiː.nit"
        ),
        VocabularyEntity(
            kasiguranin = "balay",
            tagalog = "bahay",
            english = "house",
            rootForm = "balay",
            category = "House & Daily Life",
            ipaNotation = "ba.ˈlaj"
        ),
        VocabularyEntity(
            kasiguranin = "paanu",
            tagalog = "paano",
            english = "how",
            rootForm = "paanu",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "pa.ˈɁa.nʊ"
        ),
        VocabularyEntity(
            kasiguranin = "sangan",
            tagalog = "ilan",
            english = "how much",
            rootForm = "sangan",
            category = "Numbers & Time",
            phoneticVowelLength = true,
            ipaNotation = "ˈsaː.ŋan"
        ),
        VocabularyEntity(
            kasiguranin = "alëp",
            tagalog = "gutom",
            english = "hungry",
            rootForm = "alëp",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈləp"
        ),
        VocabularyEntity(
            kasiguranin = "letratu",
            tagalog = "larawan",
            english = "image",
            rootForm = "letratu",
            category = "House & Daily Life",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "lɛ.ˈtraː.tuɁ"
        ),
        VocabularyEntity(
            kasiguranin = "bituka",
            tagalog = "bituka",
            english = "intestines",
            rootForm = "bituka",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bi.tʊ.ˈkaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "isla",
            tagalog = "pulo",
            english = "island",
            rootForm = "isla",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "Ɂis.ˈlaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "katël",
            tagalog = "kati",
            english = "itch",
            rootForm = "katël",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ka.ˈtəl"
        ),
        VocabularyEntity(
            kasiguranin = "panga",
            tagalog = "panga",
            english = "jaw",
            rootForm = "panga",
            category = "Body Parts & Health",
            ipaNotation = "pa.ˈŋa"
        ),
        VocabularyEntity(
            kasiguranin = "ammo",
            tagalog = "halik",
            english = "kiss",
            rootForm = "ammo",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "Ɂəm.ˈmɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "dimodyan",
            tagalog = "huli",
            english = "last",
            rootForm = "dimodyan",
            category = "Numbers & Time",
            ipaNotation = "di.ˈmɔ.ʤan"
        ),
        VocabularyEntity(
            kasiguranin = "dipos",
            tagalog = "bunso",
            english = "lastborn",
            rootForm = "dipos",
            category = "Family & People",
            ipaNotation = "di.ˈpɔs"
        ),
        VocabularyEntity(
            kasiguranin = "duun",
            tagalog = "dahon",
            english = "leaf",
            rootForm = "duun",
            category = "Nature & Environment",
            ipaNotation = "dʊ.ˈʊn"
        ),
        VocabularyEntity(
            kasiguranin = "turog",
            tagalog = "tulo",
            english = "leak,",
            rootForm = "turog",
            category = "House & Daily Life",
            ipaNotation = "tʊ.ˈrɔg"
        ),
        VocabularyEntity(
            kasiguranin = "kaliwa kariwe",
            tagalog = "(hand)",
            english = "left",
            rootForm = "kaliwa",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "ka.ri.ˈwɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "binti",
            tagalog = "binti",
            english = "leg",
            rootForm = "binti",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bin.ˈtiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kasinungalingan kabulean",
            tagalog = "(falsehood)",
            english = "lie",
            rootForm = "kasinungalingan",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "kaː.bʊ. ˈlɛː. Ɂan"
        ),
        VocabularyEntity(
            kasiguranin = "malagen",
            tagalog = "magaan",
            english = "light",
            rootForm = "malagen",
            category = "House & Daily Life",
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈlaː.gɛn"
        ),
        VocabularyEntity(
            kasiguranin = "sëllet",
            tagalog = "kidlat",
            english = "lightning",
            rootForm = "sëllet",
            category = "Weather & Climate",
            ipaNotation = "kid.ˈlat"
        ),
        VocabularyEntity(
            kasiguranin = "labi",
            tagalog = "labi",
            english = "lip",
            rootForm = "labi",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈlaː.biɁ"
        ),
        VocabularyEntity(
            kasiguranin = "agtay",
            tagalog = "atay",
            english = "liver",
            rootForm = "agtay",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂag.ˈtaj"
        ),
        VocabularyEntity(
            kasiguranin = "atakdug",
            tagalog = "mahaba",
            english = "long",
            rootForm = "atakdug",
            category = "Colors & Shapes",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈtak.dʊg"
        ),
        VocabularyEntity(
            kasiguranin = "malawa",
            tagalog = "maluwang",
            english = "loose",
            rootForm = "malawa",
            category = "Colors & Shapes",
            phoneticGlottal = true,
            ipaNotation = "ma.la.ˈwaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kuto",
            tagalog = "kuto",
            english = "louse",
            rootForm = "kuto",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "kʊ.ˈtɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "gayuma amaya",
            tagalog = "charm",
            english = "love",
            rootForm = "gayuma",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ma.ˈyaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "baga",
            tagalog = "baga",
            english = "lungs",
            rootForm = "baga",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈbaː.gaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "lalakke",
            tagalog = "lalake",
            english = "man",
            rootForm = "lalakke",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "lə.lək.ˈkɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "gulpi",
            tagalog = "marami",
            english = "many",
            rootForm = "gulpi",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "gʊl.ˈpiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "abëk",
            tagalog = "banig",
            english = "mat",
            rootForm = "abëk",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈbək"
        ),
        VocabularyEntity(
            kasiguranin = "gamot",
            tagalog = "gamot",
            english = "medicine",
            rootForm = "gamot",
            category = "Body Parts & Health",
            ipaNotation = "ga.ˈmɔt"
        ),
        VocabularyEntity(
            kasiguranin = "tunaw",
            tagalog = "tunaw",
            english = "melt",
            rootForm = "tunaw",
            category = "Nature & Environment",
            ipaNotation = "tʊ.ˈnaw"
        ),
        VocabularyEntity(
            kasiguranin = "ditëngnga",
            tagalog = "gitna",
            english = "middle",
            rootForm = "ditëngnga",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "di.ˈtəŋ.ŋa"
        ),
        VocabularyEntity(
            kasiguranin = "gatas",
            tagalog = "gatas",
            english = "milk",
            rootForm = "gatas",
            category = "Food & Dining",
            phoneticVowelLength = true,
            ipaNotation = "ˈgaː.tas"
        ),
        VocabularyEntity(
            kasiguranin = "bulan",
            tagalog = "buwan",
            english = "moon",
            rootForm = "bulan",
            category = "Numbers & Time"
        ),
        VocabularyEntity(
            kasiguranin = "lamok",
            tagalog = "lamok",
            english = "mosquito",
            rootForm = "lamok",
            category = "Animals & Wildlife",
            ipaNotation = "la.ˈmɔk"
        ),
        VocabularyEntity(
            kasiguranin = "lumot",
            tagalog = "lumot",
            english = "moss",
            rootForm = "lumot",
            category = "Nature & Environment",
            ipaNotation = "lʊ.ˈmɔt"
        ),
        VocabularyEntity(
            kasiguranin = "buked",
            tagalog = "bundok",
            english = "mountain",
            rootForm = "buked",
            category = "Nature & Environment",
            ipaNotation = "bʊ.ˈkɛd"
        ),
        VocabularyEntity(
            kasiguranin = "nguso",
            tagalog = "bibig",
            english = "mouth",
            rootForm = "nguso",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ŋʊ.ˈsɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "putik",
            tagalog = "putik",
            english = "mud",
            rootForm = "putik",
            category = "Nature & Environment",
            phoneticVowelLength = true,
            ipaNotation = "ˈpʊː.tik"
        ),
        VocabularyEntity(
            kasiguranin = "kuko",
            tagalog = "kuko",
            english = "nail",
            rootForm = "kuko",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "kʊ.ˈkɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "ngaran",
            tagalog = "pangalan",
            english = "name",
            rootForm = "ngaran",
            category = "Family & People",
            ipaNotation = "ŋa.ˈran"
        ),
        VocabularyEntity(
            kasiguranin = "tëngngëd",
            tagalog = "batok",
            english = "nape",
            rootForm = "tngngd",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "təŋ.ˈŋəd"
        ),
        VocabularyEntity(
            kasiguranin = "makitid",
            tagalog = "makitid",
            english = "narrow",
            rootForm = "makitid",
            category = "Colors & Shapes",
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈkiː.tid"
        ),
        VocabularyEntity(
            kasiguranin = "pusëd",
            tagalog = "pusod",
            english = "navel",
            rootForm = "pusëd",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "pʊ.ˈsəd"
        ),
        VocabularyEntity(
            kasiguranin = "adene",
            tagalog = "malapit",
            english = "near",
            rootForm = "adene",
            category = "Numbers & Time",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂa.ˈdɛː.nɛ"
        ),
        VocabularyEntity(
            kasiguranin = "alleg",
            tagalog = "leeg",
            english = "neck",
            rootForm = "alleg",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂal.ˈlɛg"
        ),
        VocabularyEntity(
            kasiguranin = "kuwentas",
            tagalog = "kuwintas",
            english = "necklace",
            rootForm = "kuwentas",
            category = "House & Daily Life",
            ipaNotation = "kʊ.ˈwɛn.tas"
        ),
        VocabularyEntity(
            kasiguranin = "digum",
            tagalog = "karayom",
            english = "needle",
            rootForm = "digum",
            category = "House & Daily Life",
            ipaNotation = "di.ˈgʊm"
        ),
        VocabularyEntity(
            kasiguranin = "lubun",
            tagalog = "pugad",
            english = "nest",
            rootForm = "lubun",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈlʊː.bʊn"
        ),
        VocabularyEntity(
            kasiguranin = "rambat",
            tagalog = "lambat",
            english = "net",
            rootForm = "rambat",
            category = "Occupations & Tools",
            ipaNotation = "ram.ˈbat"
        ),
        VocabularyEntity(
            kasiguranin = "bigu",
            tagalog = "bago",
            english = "new",
            rootForm = "bigu",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "bi.ˈgʊɁ"
        ),
        VocabularyEntity(
            kasiguranin = "gibi",
            tagalog = "gabi",
            english = "night",
            rootForm = "gibi",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "gi.ˈbiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "syam",
            tagalog = "siyam",
            english = "nine",
            rootForm = "syam",
            category = "Numbers & Time",
            ipaNotation = "ʃam"
        ),
        VocabularyEntity(
            kasiguranin = "ahëy",
            tagalog = "di ko alam",
            english = "i don't know",
            rootForm = "wala",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "wa.ˈlaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "addung",
            tagalog = "ilong",
            english = "nose",
            rootForm = "addung",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂad.ˈdʊŋ"
        ),
        VocabularyEntity(
            kasiguranin = "hindi",
            tagalog = "hindi",
            english = "not",
            rootForm = "hindi",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "hin.ˈdiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "ngayon",
            tagalog = "ngayon",
            english = "now",
            rootForm = "ngayon",
            category = "Numbers & Time"
        ),
        VocabularyEntity(
            kasiguranin = "pugita",
            tagalog = "pugita",
            english = "octopus",
            rootForm = "pugita",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "pʊ.gi.ˈtaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "madalas",
            tagalog = "madalas",
            english = "often",
            rootForm = "madalas",
            category = "Numbers & Time",
            ipaNotation = "ma.da.ˈlas"
        ),
        VocabularyEntity(
            kasiguranin = "luma",
            tagalog = "luma",
            english = "old",
            rootForm = "luma",
            category = "Numbers & Time",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈlʊː.maɁ"
        ),
        VocabularyEntity(
            kasiguranin = "mensan",
            tagalog = "minsan",
            english = "once",
            rootForm = "mensan",
            category = "Numbers & Time",
            ipaNotation = "ˈmɛn.san"
        ),
        VocabularyEntity(
            kasiguranin = "essa",
            tagalog = "isa",
            english = "one",
            rootForm = "essa",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "Ɂɛs.ˈsaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "ulila",
            tagalog = "ulila",
            english = "orphan",
            rootForm = "ulila",
            category = "Family & People",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂʊ.ˈliː.laɁ"
        ),
        VocabularyEntity(
            kasiguranin = "iba iba",
            tagalog = "different",
            english = "other,",
            rootForm = "iba",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "Ɂi.ˈba"
        ),
        VocabularyEntity(
            kasiguranin = "abeng",
            tagalog = "bangka",
            english = "boat",
            rootForm = "abeng",
            category = "Occupations & Tools",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈbɛŋ"
        ),
        VocabularyEntity(
            kasiguranin = "katig pakaway",
            tagalog = "float",
            english = "outrigger",
            rootForm = "katig",
            category = "Greetings & Essentials",
            ipaNotation = "pa.ka.ˈwaj"
        ),
        VocabularyEntity(
            kasiguranin = "duun",
            tagalog = "doon",
            english = "over there",
            rootForm = "duun",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "dʊ.ˈɁʊn"
        ),
        VocabularyEntity(
            kasiguranin = "sagwan",
            tagalog = "sagwan",
            english = "paddle",
            rootForm = "sagwan",
            category = "Occupations & Tools",
            ipaNotation = "sag. ˈwan"
        ),
        VocabularyEntity(
            kasiguranin = "saket",
            tagalog = "sakit",
            english = "pain",
            rootForm = "saket",
            category = "Emotions & Feelings",
            ipaNotation = "sa.ˈkɛt"
        ),
        VocabularyEntity(
            kasiguranin = "palad palad",
            tagalog = "(hand)",
            english = "palm",
            rootForm = "palad",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "ˈpaː.lad"
        ),
        VocabularyEntity(
            kasiguranin = "buto/bungaw",
            tagalog = "ari ng lalake",
            english = "penis",
            rootForm = "buto/bungaw",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bʊ.ˈtɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tolay",
            tagalog = "tao",
            english = "person",
            rootForm = "tolay",
            category = "Family & People",
            phoneticVowelLength = true,
            ipaNotation = "ˈtɔː.laj"
        ),
        VocabularyEntity(
            kasiguranin = "babuy",
            tagalog = "baboy",
            english = "pig",
            rootForm = "babuy",
            category = "Animals & Wildlife",
            ipaNotation = "ba.ˈbʊj"
        ),
        VocabularyEntity(
            kasiguranin = "punganan",
            tagalog = "unan",
            english = "pillow",
            rootForm = "punganan",
            category = "House & Daily Life",
            ipaNotation = "pʊ.ŋa.ˈnan"
        ),
        VocabularyEntity(
            kasiguranin = "halaman",
            tagalog = "halaman",
            english = "plant",
            rootForm = "halaman",
            category = "Nature & Environment",
            phoneticVowelLength = true,
            ipaNotation = "ha.ˈlaː.man"
        ),
        VocabularyEntity(
            kasiguranin = "hand",
            tagalog = "with",
            english = "press",
            rootForm = "hand",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "nana",
            tagalog = "nana",
            english = "pus",
            rootForm = "nana",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "na.ˈnaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kuyëng",
            tagalog = "daga",
            english = "rat",
            rootForm = "kuyëng",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "kʊ.ˈjəŋ"
        ),
        VocabularyEntity(
            kasiguranin = "madideg",
            tagalog = "pula",
            english = "red",
            rootForm = "madideg",
            category = "Colors & Shapes",
            ipaNotation = "ma.di.ˈdɛg"
        ),
        VocabularyEntity(
            kasiguranin = "takgeng",
            tagalog = "tadyang",
            english = "rib",
            rootForm = "takgeng",
            category = "Body Parts & Health",
            ipaNotation = "ˈtak.gɛŋ"
        ),
        VocabularyEntity(
            kasiguranin = "tama tama",
            tagalog = "(correct)",
            english = "right",
            rootForm = "tama",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈtaː.maɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kanan kanan",
            tagalog = "(hand)",
            english = "right",
            rootForm = "kanan",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "ˈkaː.nan"
        ),
        VocabularyEntity(
            kasiguranin = "banlaw",
            tagalog = "banlaw",
            english = "rinse",
            rootForm = "banlaw",
            category = "House & Daily Life",
            ipaNotation = "ban.ˈlaw"
        ),
        VocabularyEntity(
            kasiguranin = "bulos",
            tagalog = "ilog",
            english = "river",
            rootForm = "bulos",
            category = "Nature & Environment",
            ipaNotation = "bʊ.ˈlɔs"
        ),
        VocabularyEntity(
            kasiguranin = "dalan",
            tagalog = "daan",
            english = "road",
            rootForm = "dalan",
            category = "Nature & Environment",
            ipaNotation = "da.ˈlan"
        ),
        VocabularyEntity(
            kasiguranin = "atëp",
            tagalog = "bubong",
            english = "roof",
            rootForm = "atp",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈtəp"
        ),
        VocabularyEntity(
            kasiguranin = "ugat",
            tagalog = "ugat",
            english = "root",
            rootForm = "ugat",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈgat"
        ),
        VocabularyEntity(
            kasiguranin = "igut",
            tagalog = "tali",
            english = "tiie",
            rootForm = "igut",
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈbid"
        ),
        VocabularyEntity(
            kasiguranin = "gabuk",
            tagalog = "marupok",
            english = "fragile",
            rootForm = "gabuk",
            category = "House & Daily Life",
            ipaNotation = "ga.ˈbʊk"
        ),
        VocabularyEntity(
            kasiguranin = "masapgët",
            tagalog = "magaspang",
            english = "rough",
            rootForm = "masapgët",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "ma. ˈsap.gət"
        ),
        VocabularyEntity(
            kasiguranin = "asen",
            tagalog = "asin",
            english = "salt",
            rootForm = "asen",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈsɛn"
        ),
        VocabularyEntity(
            kasiguranin = "maasen",
            tagalog = "maalat",
            english = "salty",
            rootForm = "maasen",
            category = "Food & Dining",
            phoneticVowelLength = true,
            ipaNotation = "maː.ˈsɛn"
        ),
        VocabularyEntity(
            kasiguranin = "pareho",
            tagalog = "tulad",
            english = "same",
            rootForm = "pareho",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "pa.ˈrɛː.hɔ"
        ),
        VocabularyEntity(
            kasiguranin = "baybay",
            tagalog = "buhangin",
            english = "sand",
            rootForm = "baybay",
            category = "Nature & Environment",
            ipaNotation = "baj.ˈbaj"
        ),
        VocabularyEntity(
            kasiguranin = "diget",
            tagalog = "dagat",
            english = "sea",
            rootForm = "dagat",
            category = "Nature & Environment",
            ipaNotation = "di.ˈgɛt"
        ),
        VocabularyEntity(
            kasiguranin = "bukël",
            tagalog = "buto",
            english = "seed",
            rootForm = "bukël",
            category = "Body Parts & Health",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kaguman",
            tagalog = "kasama",
            english = "companion",
            rootForm = "kaguman",
            category = "Family & People",
            phoneticVowelLength = true,
            ipaNotation = "ˈkaː.gʊ.man"
        ),
        VocabularyEntity(
            kasiguranin = "pitu",
            tagalog = "pito",
            english = "seven",
            rootForm = "pitu",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "pi.ˈtʊɁ"
        ),
        VocabularyEntity(
            kasiguranin = "aninu",
            tagalog = "anino",
            english = "shadow",
            rootForm = "aninu",
            category = "Nature & Environment",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂa.ˈnɛː.nɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "madidëbbaw",
            tagalog = "mababaw",
            english = "shallow",
            rootForm = "madidëbbaw",
            category = "Nature & Environment",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "maː.di.dəb.ˈbaw"
        ),
        VocabularyEntity(
            kasiguranin = "digdig",
            tagalog = "gilid",
            english = "side",
            rootForm = "digdig",
            category = "House & Daily Life",
            ipaNotation = "dig.ˈdig ng di.ˈgɛt"
        ),
        VocabularyEntity(
            kasiguranin = "baddit",
            tagalog = "maliit",
            english = "short",
            rootForm = "baddit",
            category = "Numbers & Time",
            ipaNotation = "bad.ˈdit"
        ),
        VocabularyEntity(
            kasiguranin = "abaga",
            tagalog = "balikat",
            english = "shoulder",
            rootForm = "abaga",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ba.ˈgaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "udeng",
            tagalog = "hipon",
            english = "shrimp",
            rootForm = "udeng",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈdɛŋ"
        ),
        VocabularyEntity(
            kasiguranin = "kapatiyaka",
            tagalog = "kapatid",
            english = "sibling",
            rootForm = "kapatiyaka",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "ka.pa.ʧa.ˈkaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "pase",
            tagalog = "paso",
            english = "singe",
            rootForm = "pase",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "pa.ˈsɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "ënnëm",
            tagalog = "anim",
            english = "six",
            rootForm = "ënnëm",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "Ɂən.ˈnəm"
        ),
        VocabularyEntity(
            kasiguranin = "balat kulet",
            tagalog = "(person)",
            english = "skin",
            rootForm = "balat",
            category = "Body Parts & Health",
            ipaNotation = "kʊ.ˈlet"
        ),
        VocabularyEntity(
            kasiguranin = "alila",
            tagalog = "alipin",
            english = "slave",
            rootForm = "alila",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂa.ˈliː.laɁ"
        ),
        VocabularyEntity(
            kasiguranin = "magtongka",
            tagalog = "inaantok",
            english = "sleepy",
            rootForm = "magtongka",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "mag.ˈtɔŋ.kaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "mabagal",
            tagalog = "mabagal",
            english = "slow",
            rootForm = "mabagal",
            category = "Numbers & Time",
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈbaː.gal"
        ),
        VocabularyEntity(
            kasiguranin = "baddit",
            tagalog = "maliit",
            english = "small",
            rootForm = "baddit",
            category = "Colors & Shapes",
            ipaNotation = "bad.ˈdit"
        ),
        VocabularyEntity(
            kasiguranin = "asok",
            tagalog = "usok",
            english = "smoke",
            rootForm = "asok",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈsɔk"
        ),
        VocabularyEntity(
            kasiguranin = "makinis",
            tagalog = "makinis",
            english = "smooth",
            rootForm = "makinis",
            category = "House & Daily Life",
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈkiː.nis"
        ),
        VocabularyEntity(
            kasiguranin = "ulag",
            tagalog = "ahas",
            english = "snake",
            rootForm = "ulag",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈlag"
        ),
        VocabularyEntity(
            kasiguranin = "abben",
            tagalog = "bahing",
            english = "sneeze",
            rootForm = "abben",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂəb.ˈbɛn"
        ),
        VocabularyEntity(
            kasiguranin = "malammen",
            tagalog = "malambot",
            english = "soft",
            rootForm = "malammen",
            category = "House & Daily Life",
            ipaNotation = "ma.lam.ˈmɛn"
        ),
        VocabularyEntity(
            kasiguranin = "sangan",
            tagalog = "ilan",
            english = "some",
            rootForm = "sangan",
            category = "Numbers & Time",
            phoneticVowelLength = true,
            ipaNotation = "ˈsaː.ŋan"
        ),
        VocabularyEntity(
            kasiguranin = "kaluluwa",
            tagalog = "kaluluwa",
            english = "soul",
            rootForm = "kaluluwa",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "ˈkaː.lu.lu.wa"
        ),
        VocabularyEntity(
            kasiguranin = "malasëm",
            tagalog = "maasim",
            english = "sour",
            rootForm = "malasëm",
            category = "Food & Dining",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈlaː.səm"
        ),
        VocabularyEntity(
            kasiguranin = "abagat",
            tagalog = "habagat",
            english = "southwest monsoon",
            rootForm = "abagat",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ba.ˈgat"
        ),
        VocabularyEntity(
            kasiguranin = "kulapnët",
            tagalog = "paniki",
            english = "bat",
            rootForm = "kulapnët",
            category = "Animals & Wildlife",
            ipaNotation = "ku.lap.ˈnɛt"
        ),
        VocabularyEntity(
            kasiguranin = "gagamba",
            tagalog = "gagamba",
            english = "spider",
            rootForm = "gagamba",
            category = "Animals & Wildlife",
            ipaNotation = "ga.ˈgam.ba"
        ),
        VocabularyEntity(
            kasiguranin = "laway",
            tagalog = "laway",
            english = "saliva",
            rootForm = "laway",
            category = "Body Parts & Health",
            phoneticVowelLength = true,
            ipaNotation = "ˈlaː.waj"
        ),
        VocabularyEntity(
            kasiguranin = "pusit",
            tagalog = "pusit",
            english = "squid",
            rootForm = "pusit",
            category = "Animals & Wildlife",
            ipaNotation = "pʊ.ˈsit"
        ),
        VocabularyEntity(
            kasiguranin = "agdenan",
            tagalog = "hagdan",
            english = "stairs",
            rootForm = "agdenan",
            category = "House & Daily Life",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂag.ˈdɛː.nan"
        ),
        VocabularyEntity(
            kasiguranin = "taknëg",
            tagalog = "tayo",
            english = "stand",
            rootForm = "taknëg",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "tak.ˈnəg"
        ),
        VocabularyEntity(
            kasiguranin = "bitoin",
            tagalog = "bituin",
            english = "star",
            rootForm = "bitoin",
            category = "Nature & Environment",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "bi.ˈtɔː.Ɂin"
        ),
        VocabularyEntity(
            kasiguranin = "wood) patpat patpat",
            tagalog = "(of",
            english = "stick",
            rootForm = "wood",
            category = "Greetings & Essentials",
            ipaNotation = "pat.ˈpat"
        ),
        VocabularyEntity(
            kasiguranin = "tiyan",
            tagalog = "tiyan",
            english = "stomach",
            rootForm = "tiyan",
            category = "Body Parts & Health",
            ipaNotation = "ʧan"
        ),
        VocabularyEntity(
            kasiguranin = "bato",
            tagalog = "bato",
            english = "stone",
            rootForm = "bato",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "ba.ˈtɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kamalig kamalig",
            tagalog = "(food)",
            english = "storehouse",
            rootForm = "kamalig",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "ka.ˈmaː.lig"
        ),
        VocabularyEntity(
            kasiguranin = "matuwid diretso",
            tagalog = "tuwid,",
            english = "straight",
            rootForm = "matuwid",
            category = "Colors & Shapes",
            ipaNotation = "di.ˈrɛt.sɔ"
        ),
        VocabularyEntity(
            kasiguranin = "annat",
            tagalog = "unat",
            english = "stretch",
            rootForm = "annat",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂən.ˈnat"
        ),
        VocabularyEntity(
            kasiguranin = "mabigsëk",
            tagalog = "malakas",
            english = "strong",
            rootForm = "mabigsëk",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ma.big.ˈsək"
        ),
        VocabularyEntity(
            kasiguranin = "sëpsëp",
            tagalog = "sipsip",
            english = "suck",
            rootForm = "sëpsëp",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "səp.ˈsəp"
        ),
        VocabularyEntity(
            kasiguranin = "talad",
            tagalog = "tubo",
            english = "sugarcane",
            rootForm = "talad",
            category = "Food & Dining",
            ipaNotation = "ta.ˈlad"
        ),
        VocabularyEntity(
            kasiguranin = "aldew",
            tagalog = "araw",
            english = "sun",
            rootForm = "aldew",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "ˈɁal.dɛw"
        ),
        VocabularyEntity(
            kasiguranin = "matam-is",
            tagalog = "matamis",
            english = "sweet",
            rootForm = "matamis",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "ma.ˈtam.Ɂis"
        ),
        VocabularyEntity(
            kasiguranin = "baga",
            tagalog = "maga",
            english = "swollen",
            rootForm = "baga",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ba.ˈgaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "ipos",
            tagalog = "buntot",
            english = "tail",
            rootForm = "ipos",
            category = "Animals & Wildlife",
            phoneticGlottal = true,
            ipaNotation = "Ɂi.ˈpɔs"
        ),
        VocabularyEntity(
            kasiguranin = "malangkaw",
            tagalog = "matangkad",
            english = "tall",
            rootForm = "malangkaw",
            category = "Body Parts & Health",
            ipaNotation = "ma.laŋ.ˈkaw"
        ),
        VocabularyEntity(
            kasiguranin = "sanget",
            tagalog = "iyak",
            english = "cry",
            rootForm = "sanget",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈlʊː.haɁ"
        ),
        VocabularyEntity(
            kasiguranin = "sapulo",
            tagalog = "sampu",
            english = "ten",
            rootForm = "sapulo",
            category = "Numbers & Time",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈsaː.pʊ.lɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "mabagël",
            tagalog = "makapal",
            english = "thick",
            rootForm = "mabagël",
            category = "Occupations & Tools",
            phoneticGlottal = true,
            ipaNotation = "ma.ba.ˈgəl"
        ),
        VocabularyEntity(
            kasiguranin = "lape",
            tagalog = "hita",
            english = "thigh",
            rootForm = "lape",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "la.ˈpɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "malapes",
            tagalog = "manipis",
            english = "thin",
            rootForm = "malapes",
            category = "Body Parts & Health",
            phoneticVowelLength = true,
            ipaNotation = "ma.ˈlaː.pɛs"
        ),
        VocabularyEntity(
            kasiguranin = "maniwang",
            tagalog = "payat",
            english = "thin",
            rootForm = "payat",
            category = "Body Parts & Health",
            ipaNotation = "ma.ni.ˈwaŋ"
        ),
        VocabularyEntity(
            kasiguranin = "uwaw",
            tagalog = "uhaw",
            english = "thirsty",
            rootForm = "uwaw",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈwaw"
        ),
        VocabularyEntity(
            kasiguranin = "tëllo",
            tagalog = "tatlo",
            english = "three",
            rootForm = "tllo",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "təl.ˈlɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "bukraw",
            tagalog = "lalamunan",
            english = "throat",
            rootForm = "bukraw",
            category = "Body Parts & Health",
            ipaNotation = "bʊk.ˈraw"
        ),
        VocabularyEntity(
            kasiguranin = "këddur",
            tagalog = "kulog",
            english = "thunder",
            rootForm = "këddur",
            category = "Weather & Climate",
            ipaNotation = "kad.ˈdʊr"
        ),
        VocabularyEntity(
            kasiguranin = "masikip",
            tagalog = "masikip",
            english = "tight",
            rootForm = "masikip",
            category = "House & Daily Life",
            ipaNotation = "ma.si.ˈkip"
        ),
        VocabularyEntity(
            kasiguranin = "pakeligip",
            tagalog = "tanong",
            english = "ask",
            rootForm = "pakeligip",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "pa.ˈkɛː.li.gip"
        ),
        VocabularyEntity(
            kasiguranin = "lukag",
            tagalog = "gising",
            english = "wake",
            rootForm = "lukag",
            category = "House & Daily Life",
            ipaNotation = "lʊ.ˈkag"
        ),
        VocabularyEntity(
            kasiguranin = "kanga",
            tagalog = "galit",
            english = "angry",
            rootForm = "kanga",
            category = "Emotions & Feelings",
            ipaNotation = "ˈka.ŋa"
        ),
        VocabularyEntity(
            kasiguranin = "enak",
            tagalog = "anak",
            english = "child",
            rootForm = "enak",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "ˈɁɛ.nak"
        ),
        VocabularyEntity(
            kasiguranin = "yabat",
            tagalog = "palo",
            english = "strike",
            rootForm = "yabat",
            category = "Family & People",
            phoneticVowelLength = true,
            ipaNotation = "ˈjaː.bat"
        ),
        VocabularyEntity(
            kasiguranin = "tëggeb",
            tagalog = "dighay",
            english = "burp",
            rootForm = "dighay",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "təg.ˈgɛb"
        ),
        VocabularyEntity(
            kasiguranin = "këtteb",
            tagalog = "kagat",
            english = "bite",
            rootForm = "kattëb",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "kət.ˈtɛb"
        ),
        VocabularyEntity(
            kasiguranin = "sabyog",
            tagalog = "ihip",
            english = "blow",
            rootForm = "sabyog",
            category = "Weather & Climate",
            ipaNotation = "sab.ˈjɔg"
        ),
        VocabularyEntity(
            kasiguranin = "labbut",
            tagalog = "kulo",
            english = "boil",
            rootForm = "labbut",
            category = "House & Daily Life",
            ipaNotation = "ləb.ˈbʊt"
        ),
        VocabularyEntity(
            kasiguranin = "putel",
            tagalog = "putol",
            english = "break",
            rootForm = "putel",
            category = "Nature & Environment",
            ipaNotation = "pʊ.ˈtɛl"
        ),
        VocabularyEntity(
            kasiguranin = "angës",
            tagalog = "hinga",
            english = "breath",
            rootForm = "angës",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈŋəs"
        ),
        VocabularyEntity(
            kasiguranin = "tawed",
            tagalog = "dala",
            english = "bring",
            rootForm = "tawed",
            category = "House & Daily Life",
            ipaNotation = "ta.ˈwɛd"
        ),
        VocabularyEntity(
            kasiguranin = "tutod",
            tagalog = "sunog",
            english = "burn",
            rootForm = "tutod",
            category = "House & Daily Life",
            ipaNotation = "tʊ.ˈtɔd"
        ),
        VocabularyEntity(
            kasiguranin = "kotkot",
            tagalog = "baon",
            english = "bury",
            rootForm = "kotkot",
            category = "Nature & Environment",
            ipaNotation = "ˈkɔt.kɔt"
        ),
        VocabularyEntity(
            kasiguranin = "lëbbëng",
            tagalog = "libing",
            english = "bury the dead",
            rootForm = "lëbbëng",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "ləb.ˈbəng"
        ),
        VocabularyEntity(
            kasiguranin = "bugtong",
            tagalog = "bili",
            english = "buy",
            rootForm = "bugtong",
            category = "House & Daily Life",
            ipaNotation = "bʊg.ˈtɔŋ"
        ),
        VocabularyEntity(
            kasiguranin = "dulaw",
            tagalog = "tawag",
            english = "called",
            rootForm = "dulaw",
            category = "Greetings & Essentials",
            ipaNotation = "dʊ.ˈlaw"
        ),
        VocabularyEntity(
            kasiguranin = "betbet",
            tagalog = "dala",
            english = "carry",
            rootForm = "betbet",
            category = "House & Daily Life",
            ipaNotation = "ˈbɛt.bɛt"
        ),
        VocabularyEntity(
            kasiguranin = "pile",
            tagalog = "pili",
            english = "choose",
            rootForm = "pile",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "pi.ˈlɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "demët",
            tagalog = "dating",
            english = "come",
            rootForm = "demët",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈdeː.mət"
        ),
        VocabularyEntity(
            kasiguranin = "ikkër",
            tagalog = "ubo",
            english = "cough",
            rootForm = " ikkër",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂik.ˈkər"
        ),
        VocabularyEntity(
            kasiguranin = "bilang",
            tagalog = "bilang",
            english = "count",
            rootForm = "bilang",
            category = "Numbers & Time",
            phoneticVowelLength = true,
            ipaNotation = "ˈbiː.laŋ"
        ),
        VocabularyEntity(
            kasiguranin = "këttol",
            tagalog = "putol",
            english = "cut",
            rootForm = "këttol",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "kət.ˈtɔl"
        ),
        VocabularyEntity(
            kasiguranin = "sayaw",
            tagalog = "sayaw",
            english = "dance",
            rootForm = "sayaw",
            category = "Occupations & Tools",
            ipaNotation = "sa.ˈjaw"
        ),
        VocabularyEntity(
            kasiguranin = "gustu",
            tagalog = "gusto",
            english = "want",
            rootForm = "gustu",
            category = "Greetings & Essentials",
            ipaNotation = "gʊs.ˈtʊ"
        ),
        VocabularyEntity(
            kasiguranin = "gamet",
            tagalog = "gawa",
            english = "do",
            rootForm = "gamet",
            category = "Occupations & Tools",
            ipaNotation = "ga.ˈmɛt"
        ),
        VocabularyEntity(
            kasiguranin = "godgod",
            tagalog = "kaladkad",
            english = "drag",
            rootForm = "godgod",
            category = "House & Daily Life",
            ipaNotation = "ˈgɔd.gɔd"
        ),
        VocabularyEntity(
            kasiguranin = "inom inom",
            tagalog = "drink",
            english = "to",
            rootForm = "inom",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "Ɂi.ˈnɔm"
        ),
        VocabularyEntity(
            kasiguranin = "limës",
            tagalog = "lunod",
            english = "drown",
            rootForm = "limës",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "li.ˈməs"
        ),
        VocabularyEntity(
            kasiguranin = "kuman",
            tagalog = "kain",
            english = "eat",
            rootForm = "kumain",
            category = "Food & Dining",
            ipaNotation = "kʊ.ˈman"
        ),
        VocabularyEntity(
            kasiguranin = "tapduk",
            tagalog = "hulog",
            english = "fall",
            rootForm = "tapduk",
            category = "House & Daily Life",
            ipaNotation = "tap.ˈdʊk"
        ),
        VocabularyEntity(
            kasiguranin = "anteng",
            tagalog = "takot",
            english = "scared",
            rootForm = "anteng",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "Ɂan.ˈtɛŋ"
        ),
        VocabularyEntity(
            kasiguranin = "laban laban",
            tagalog = "fight",
            english = "to",
            rootForm = "laban",
            category = "Greetings & Essentials",
            phoneticVowelLength = true,
            ipaNotation = "ˈlaː.ban"
        ),
        VocabularyEntity(
            kasiguranin = "aryok",
            tagalog = "hanap",
            english = "find",
            rootForm = "aryok",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂar.ˈjɔk"
        ),
        VocabularyEntity(
            kasiguranin = "latak",
            tagalog = "lutang",
            english = "float",
            rootForm = "latak",
            category = "Nature & Environment",
            ipaNotation = "la.ˈtak"
        ),
        VocabularyEntity(
            kasiguranin = "agus",
            tagalog = "agos",
            english = "flow",
            rootForm = "agus",
            category = "Nature & Environment",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁaː.gʊs"
        ),
        VocabularyEntity(
            kasiguranin = "egbër",
            tagalog = "lipad",
            english = "fly",
            rootForm = "egbër",
            category = "Nature & Environment",
            phoneticGlottal = true,
            ipaNotation = "ˈɁɛg.bər"
        ),
        VocabularyEntity(
            kasiguranin = "lipon",
            tagalog = "limot",
            english = "forget",
            rootForm = "lipon",
            category = "Emotions & Feelings",
            ipaNotation = "li.ˈpɔn"
        ),
        VocabularyEntity(
            kasiguranin = "attëd",
            tagalog = "bigay",
            english = "give",
            rootForm = "attëd",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈtəd"
        ),
        VocabularyEntity(
            kasiguranin = "angay",
            tagalog = "punta",
            english = "go",
            rootForm = "angay",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈŋaj"
        ),
        VocabularyEntity(
            kasiguranin = "ogsad",
            tagalog = "baba",
            english = "down",
            rootForm = "ogsad",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "Ɂɔg.ˈsad"
        ),
        VocabularyEntity(
            kasiguranin = "sëddëp",
            tagalog = "pasok",
            english = "in",
            rootForm = "sëddëp",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "səd.ˈdəp"
        ),
        VocabularyEntity(
            kasiguranin = "luwas",
            tagalog = "labas",
            english = "out",
            rootForm = "luwas",
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈwas"
        ),
        VocabularyEntity(
            kasiguranin = "sangkay",
            tagalog = "akyat",
            english = "climb",
            rootForm = "sangkay",
            category = "Nature & Environment",
            ipaNotation = "saŋ.ˈkaj"
        ),
        VocabularyEntity(
            kasiguranin = "saneg",
            tagalog = "kinig",
            english = "hear",
            rootForm = "kinig",
            category = "Body Parts & Health",
            phoneticVowelLength = true,
            ipaNotation = "ˈsaː.nɛg"
        ),
        VocabularyEntity(
            kasiguranin = "kabit",
            tagalog = "hawak",
            english = "hold",
            rootForm = "kabit",
            category = "Body Parts & Health",
            ipaNotation = "ka.ˈbit"
        ),
        VocabularyEntity(
            kasiguranin = "lugso",
            tagalog = "talon",
            english = "jump",
            rootForm = "lugso",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "lʊk.ˈsɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "buno",
            tagalog = "patay",
            english = "kll",
            rootForm = "buno",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "bʊ.ˈnɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "tukoy",
            tagalog = "alam",
            english = "know",
            rootForm = "tukoy",
            category = "Emotions & Feelings",
            ipaNotation = "tʊ.ˈkɔj"
        ),
        VocabularyEntity(
            kasiguranin = "oled",
            tagalog = "higa",
            english = "lay",
            rootForm = "oled",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁɔː.lɛd"
        ),
        VocabularyEntity(
            kasiguranin = "ileng",
            tagalog = "tingin",
            english = "look",
            rootForm = "ileng",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂi.ˈlɛŋ"
        ),
        VocabularyEntity(
            kasiguranin = "usek",
            tagalog = "laro",
            english = "play",
            rootForm = "usek",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈsɛk"
        ),
        VocabularyEntity(
            kasiguranin = "botbot",
            tagalog = "hila",
            english = "pull",
            rootForm = "botbot",
            category = "Occupations & Tools",
            ipaNotation = "bɔt.ˈbɔt"
        ),
        VocabularyEntity(
            kasiguranin = "toglad",
            tagalog = "tulak",
            english = "push",
            rootForm = "toglad",
            category = "Occupations & Tools",
            ipaNotation = "tɔg.ˈlad"
        ),
        VocabularyEntity(
            kasiguranin = "datton",
            tagalog = "lagay",
            english = "put",
            rootForm = "datton",
            category = "House & Daily Life",
            ipaNotation = "dət.ˈtɔn"
        ),
        VocabularyEntity(
            kasiguranin = "dima",
            tagalog = "away",
            english = "fight",
            rootForm = "dima",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "di.ˈmaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "uden",
            tagalog = "ulan",
            english = "rain",
            rootForm = "uden",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈdɛn"
        ),
        VocabularyEntity(
            kasiguranin = "sole",
            tagalog = "balik",
            english = "return",
            rootForm = "sole",
            category = "House & Daily Life",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈsɔː.lɛɁ"
        ),
        VocabularyEntity(
            kasiguranin = "kuskus",
            tagalog = "kuskos",
            english = "rub",
            rootForm = "kuskus",
            category = "House & Daily Life",
            ipaNotation = "kʊs.ˈkʊs"
        ),
        VocabularyEntity(
            kasiguranin = "ginan",
            tagalog = "takbo",
            english = "run",
            rootForm = "ginan",
            category = "House & Daily Life",
            ipaNotation = "gi.ˈnan"
        ),
        VocabularyEntity(
            kasiguranin = "kagi",
            tagalog = "sabi",
            english = "say",
            rootForm = "kagi",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "ka.ˈgiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "gusgus",
            tagalog = "kamot",
            english = "scratch",
            rootForm = "gusgus",
            category = "Body Parts & Health",
            ipaNotation = "gʊs.ˈgʊs"
        ),
        VocabularyEntity(
            kasiguranin = "keta",
            tagalog = "kita",
            english = "see",
            rootForm = "keta",
            category = "Body Parts & Health",
            phoneticVowelLength = true,
            ipaNotation = "ˈkeː.ta"
        ),
        VocabularyEntity(
            kasiguranin = "darop",
            tagalog = "tahi",
            english = "sew",
            rootForm = "darop",
            category = "Occupations & Tools",
            ipaNotation = "da.ˈrɔp"
        ),
        VocabularyEntity(
            kasiguranin = "karyaw",
            tagalog = "sigaw",
            english = "shout",
            rootForm = "karyaw",
            category = "Body Parts & Health",
            ipaNotation = "kar.ˈyaw"
        ),
        VocabularyEntity(
            kasiguranin = "ipeta",
            tagalog = "pakita",
            english = "show",
            rootForm = "ipeta",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "Ɂi.ˈpɛː.ta"
        ),
        VocabularyEntity(
            kasiguranin = "puropor",
            tagalog = "ambon",
            english = "shower",
            rootForm = "puropor",
            category = "Weather & Climate",
            phoneticVowelLength = true,
            ipaNotation = "pʊ.ˈrɔː.pɔr"
        ),
        VocabularyEntity(
            kasiguranin = "sarëm",
            tagalog = "lubog",
            english = "sink",
            rootForm = "sarëm",
            category = "House & Daily Life",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈsaː.rəm"
        ),
        VocabularyEntity(
            kasiguranin = "etnod",
            tagalog = "upo",
            english = "sit",
            rootForm = "etnod",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂɛt.ˈnɔd"
        ),
        VocabularyEntity(
            kasiguranin = "tidug",
            tagalog = "tulog",
            english = "sleep",
            rootForm = "tidug",
            category = "Body Parts & Health",
            ipaNotation = "ti.ˈdʊg"
        ),
        VocabularyEntity(
            kasiguranin = "arob",
            tagalog = "amoy",
            english = "smell",
            rootForm = "arob",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈrɔb"
        ),
        VocabularyEntity(
            kasiguranin = "kagi",
            tagalog = "salita",
            english = "speak",
            rootForm = "kagi",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "ka.ˈgiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "loktab",
            tagalog = "dura",
            english = "spit",
            rootForm = "loktab",
            category = "Body Parts & Health",
            ipaNotation = "lɔk.ˈtab"
        ),
        VocabularyEntity(
            kasiguranin = "pëkka",
            tagalog = "hati",
            english = "split",
            rootForm = "pëkka",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "pək.ˈkaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "pëkkël",
            tagalog = "piga",
            english = "squeeze",
            rootForm = "piga",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "pək.ˈkəl"
        ),
        VocabularyEntity(
            kasiguranin = "takaw",
            tagalog = "nakaw",
            english = "steal",
            rootForm = "takaw",
            category = "Family & People",
            ipaNotation = "ta.ˈkaw"
        ),
        VocabularyEntity(
            kasiguranin = "tël-lën",
            tagalog = "lunok",
            english = "swallow",
            rootForm = "tël-lën",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "təl.ˈlən"
        ),
        VocabularyEntity(
            kasiguranin = "ibut",
            tagalog = "itapon",
            english = "throw",
            rootForm = "ibut",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂi.ˈbʊt"
        ),
        VocabularyEntity(
            kasiguranin = "ota",
            tagalog = "suka",
            english = "vommit",
            rootForm = "ota",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁɔː.taɁ"
        ),
        VocabularyEntity(
            kasiguranin = "rempës",
            tagalog = "hugas",
            english = "wash",
            rootForm = "rempës",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "ˈrɛm.pəs"
        ),
        VocabularyEntity(
            kasiguranin = "ladi",
            tagalog = "habi",
            english = "weave",
            rootForm = "ladi",
            category = "Occupations & Tools",
            phoneticGlottal = true,
            ipaNotation = "la.ˈdiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "niilaw",
            tagalog = "bukas",
            english = "tomorrow",
            rootForm = "niilaw",
            category = "Numbers & Time",
            ipaNotation = "ni.i.ˈlaw"
        ),
        VocabularyEntity(
            kasiguranin = "ngipën",
            tagalog = "ngipin",
            english = "tooth",
            rootForm = "ngipën",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ŋi.ˈpən"
        ),
        VocabularyEntity(
            kasiguranin = "ungot",
            tagalog = "sulo",
            english = "torch,",
            rootForm = "sulo",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈŋɔt"
        ),
        VocabularyEntity(
            kasiguranin = "ponan",
            tagalog = "punong-kahoy",
            english = "tree",
            rootForm = "ponan",
            category = "Nature & Environment",
            phoneticVowelLength = true,
            ipaNotation = "ˈpɔː.nan"
        ),
        VocabularyEntity(
            kasiguranin = "bakokol",
            tagalog = "pagong",
            english = "turtle",
            rootForm = "bakokol",
            category = "Animals & Wildlife",
            phoneticVowelLength = true,
            ipaNotation = "ba.ˈkɔː.kɔl"
        ),
        VocabularyEntity(
            kasiguranin = "duwa",
            tagalog = "dalawa",
            english = "two",
            rootForm = "duwa",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "dʊ.ˈwaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "madukës",
            tagalog = "pangit",
            english = "ugly",
            rootForm = "madukës",
            category = "Emotions & Feelings",
            phoneticGlottal = true,
            ipaNotation = "ma.dʊ.ˈkəs"
        ),
        VocabularyEntity(
            kasiguranin = "badu",
            tagalog = "baro",
            english = "garment",
            rootForm = "badu",
            category = "House & Daily Life",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈbaː.dʊɁ"
        ),
        VocabularyEntity(
            kasiguranin = "disono",
            tagalog = "taas",
            english = "up",
            rootForm = "disono",
            category = "House & Daily Life",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "pa.di.ˈsɔː.nɔɁ"
        ),
        VocabularyEntity(
            kasiguranin = "esbu",
            tagalog = "ihi",
            english = "urine",
            rootForm = "esbu",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "ˈɁɛs.bʊ"
        ),
        VocabularyEntity(
            kasiguranin = "ubët",
            tagalog = "ari ng babae",
            english = "vagina",
            rootForm = "ubët",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈbət"
        ),
        VocabularyEntity(
            kasiguranin = "atong",
            tagalog = "gulay",
            english = "vegetables",
            rootForm = "atong",
            category = "Food & Dining",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈtɔŋ"
        ),
        VocabularyEntity(
            kasiguranin = "gera",
            tagalog = "digma",
            english = "war",
            rootForm = "gera",
            category = "Occupations & Tools",
            phoneticVowelLength = true,
            ipaNotation = "ˈgɛː.ra"
        ),
        VocabularyEntity(
            kasiguranin = "danom",
            tagalog = "tubig",
            english = "water",
            rootForm = "danom",
            category = "Nature & Environment",
            ipaNotation = "da.ˈnɔm"
        ),
        VocabularyEntity(
            kasiguranin = "dappog",
            tagalog = "kalabaw",
            english = "water buffalo",
            rootForm = "dappog",
            category = "Animals & Wildlife",
            ipaNotation = "dap.ˈpɔg"
        ),
        VocabularyEntity(
            kasiguranin = "alun",
            tagalog = "alon",
            english = "wave",
            rootForm = "alun",
            category = "Weather & Climate",
            phoneticGlottal = true,
            phoneticVowelLength = true,
            ipaNotation = "ˈɁaː.lɔn"
        ),
        VocabularyEntity(
            kasiguranin = "basa",
            tagalog = "basa",
            english = "wet",
            rootForm = "basa",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "ba.ˈsaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "anu",
            tagalog = "ano",
            english = "what",
            rootForm = "ano",
            category = "Greetings & Essentials",
            phoneticGlottal = true,
            ipaNotation = "Ɂa.ˈnɔ"
        ),
        VocabularyEntity(
            kasiguranin = "gulong",
            tagalog = "gulong",
            english = "wheel",
            rootForm = "gulong",
            category = "Occupations & Tools",
            ipaNotation = "gu.ˈlɔŋ"
        ),
        VocabularyEntity(
            kasiguranin = "kelan",
            tagalog = "kailan",
            english = "when",
            rootForm = "kelan",
            category = "Numbers & Time",
            phoneticVowelLength = true,
            ipaNotation = "ˈkɛː.lan"
        ),
        VocabularyEntity(
            kasiguranin = "mapudew",
            tagalog = "puti",
            english = "white",
            rootForm = "mapudew",
            category = "Colors & Shapes",
            ipaNotation = "ma.pu.ˈdɛw"
        ),
        VocabularyEntity(
            kasiguranin = "malawa",
            tagalog = "malawak",
            english = "wide",
            rootForm = "malawa",
            category = "Numbers & Time",
            phoneticGlottal = true,
            ipaNotation = "ma.la.ˈwaɁ"
        ),
        VocabularyEntity(
            kasiguranin = "parës",
            tagalog = "hangin",
            english = "wind",
            rootForm = "parës",
            category = "Weather & Climate",
            phoneticGlottal = true,
            ipaNotation = "pa.ˈrəs"
        ),
        VocabularyEntity(
            kasiguranin = "babbi",
            tagalog = "babae",
            english = "female",
            rootForm = "babae",
            category = "Family & People",
            phoneticGlottal = true,
            ipaNotation = "bəb.ˈbiɁ"
        ),
        VocabularyEntity(
            kasiguranin = "mali",
            tagalog = "mali",
            english = "wrong",
            rootForm = "mali",
            category = "House & Daily Life",
            phoneticGlottal = true,
            ipaNotation = "ma.ˈliɁ"
        ),
        VocabularyEntity(
            kasiguranin = "uwab",
            tagalog = "hikab",
            english = "yawn",
            rootForm = "uwab",
            category = "Body Parts & Health",
            phoneticGlottal = true,
            ipaNotation = "Ɂʊ.ˈwab"
        )
    )

    fun getInitialStories(): List<StoryEntity> {
        // Ten Tagalog folk tales, replacing the five Casiguran stories.
        //
        // Every page carries an empty `kasiguranin`. Kasiguranin is the endangered language this
        // project documents, and the source PDF contains none of it, so writing that layer here
        // would mean inventing primary data for a preservation thesis. Adrian authors it. Until
        // then StoryReaderScreen leads with Tagalog and hides the word chips and audio control,
        // and StoryCoverCard falls back to the Tagalog title, so the stories read as finished
        // rather than broken.

        val story1Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Magkaibigan sina Pagong at Matsing. Mabait at matulungin si Pagong, ngunit tuso at palabiro si Matsing. Isang araw, binigyan sila ng isang supot ng pansit.", "english": "Turtle and Monkey were friends. Turtle was kind and helpful, but Monkey was cunning and full of mischief. One day they were given a packet of noodles.", "illustrationDesc": "A turtle and a monkey meeting on a forest path, a paper packet between them"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Sinabi ni Matsing na nangangamoy panis ang pansit at siya muna ang titikim. Naubos niya ang lahat at walang natira para kay Pagong.", "english": "Monkey said the noodles smelled spoiled and that he should taste them first. He finished every strand, and nothing was left for Turtle.", "illustrationDesc": "A monkey eating from a packet while a turtle waits beside him"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Nakakita sila ng puno ng saging at pinaghatian ito. Kinuha ni Matsing ang bahaging may dahon; napunta kay Pagong ang bahaging may ugat.", "english": "They found a banana tree and split it between them. Monkey took the leafy top; the rooted half went to Turtle.", "illustrationDesc": "A banana tree divided in two, one half leafy and one half with roots"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Nalanta ang tanim ni Matsing. Namunga naman nang hitik ang kay Pagong. Nag-alok si Matsing na siya na ang aakyat at maghuhulog ng bunga.", "english": "Monkey's half withered. Turtle's grew heavy with fruit. Monkey offered to climb up and drop the bananas down.", "illustrationDesc": "A withered stalk beside a tall banana tree laden with fruit"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Kinain ni Matsing ang lahat sa itaas ng puno. Naglagay si Pagong ng tinik sa ilalim, at nasaktan si Matsing nang bumaba siya.", "english": "Monkey ate everything up in the tree. Turtle laid thorns below, and Monkey was hurt when at last he climbed down.", "illustrationDesc": "A monkey resting high in a banana tree while thorns are placed at its base"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Galit na itinapon ni Matsing si Pagong sa dagat, hindi alam na marunong lumangoy ang pagong. Mula noon, nagbago si Matsing.", "english": "In anger Monkey threw Turtle into the sea, not knowing that turtles can swim. From that day on, Monkey changed.", "illustrationDesc": "A turtle swimming easily in shallow sea water near a shore"}
        ]"""

        val story2Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Maganda ang panahon at maaga pa lamang ay gising na si Langgam. Naghanap siya ng pagkain at pinasan pauwi ang isang butil ng bigas.", "english": "The weather was fine, and Ant was awake early. He searched for food and carried a single grain of rice home on his back.", "illustrationDesc": "An ant carrying a grain of rice along a sunlit path"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Binati siya ni Tipaklong. Ang tanong nito, bakit wala siyang ginawa kundi mag-ipon ng pagkain habang maganda ang panahon.", "english": "Grasshopper greeted him and asked why he did nothing but gather food while the weather was fine.", "illustrationDesc": "A grasshopper resting on a tall blade of grass, calling out"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Inaya siya ni Tipaklong na lumukso at kumanta. Sagot ni Langgam, nag-iipon siya para may makain kapag sumama ang panahon.", "english": "Grasshopper invited him to leap and sing. Ant answered that he was storing food so there would be something to eat when the weather turned.", "illustrationDesc": "An ant walking past a dancing grasshopper toward a small burrow"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Dumating ang tag-ulan. Umuulan sa umaga, sa hapon at sa gabi. Ginaw na ginaw at gutom na gutom ang kawawang Tipaklong.", "english": "The rainy season came. It rained in the morning, in the afternoon and through the night. Poor Grasshopper was cold and very hungry.", "illustrationDesc": "Heavy rain falling on flattened grass under a dark sky"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Pinilit niyang marating ang bahay ni Langgam. Pinapasok siya, binigyan ng tuyong damit at pagkain, at magkasalo silang kumain.", "english": "He struggled to reach Ant's house. He was welcomed in, given dry clothes and food, and the two friends ate together.", "illustrationDesc": "Two friends sharing a warm meal inside a small dry shelter"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Mula noon, natuto si Tipaklong gumawa at mag-impok, at kasama na siya ni Langgam kapag maganda ang panahon.", "english": "From then on Grasshopper learned to work and to save, and he joined Ant whenever the weather was fine.", "illustrationDesc": "An ant and a grasshopper working side by side in sunshine"}
        ]"""

        val story3Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Pinagtawanan ng Kuneho ang Pagong dahil sa bagal nitong maglakad. Sinabi niyang walang mararating ang may ganoong kaikling mga paa.", "english": "Rabbit laughed at Turtle for how slowly he walked. He said that legs so short would never get anywhere.", "illustrationDesc": "A rabbit laughing at a turtle on a woodland path"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Hinamon ng Pagong ang Kuneho ng karera patungo sa tuktok ng bundok. Tuwang-tuwa ang mayabang na Kuneho sa hamon.", "english": "Turtle challenged Rabbit to a race to the mountaintop. The boastful Rabbit was delighted by the challenge.", "illustrationDesc": "A turtle pointing toward a distant mountain while a rabbit grins"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Nagtawag pa ng mga kaibigan ang Kuneho para manood. Si Matsing ang nagbilang: Isa, dalawa, tatlo, takbo!", "english": "Rabbit even called friends to watch. Monkey counted them off: one, two, three, go!", "illustrationDesc": "Forest animals gathered at a starting line"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Sa isang sandali ay nasa paanan na ng bundok ang Kuneho. Malayung-malayo pa ang Pagong, ngunit patuloy siya sa paglakad.", "english": "In moments Rabbit was already at the foot of the mountain. Turtle was far behind, but he kept walking.", "illustrationDesc": "A rabbit bounding ahead while a turtle plods steadily behind"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Dahil tiwala sa sarili, nagpasya ang Kuneho na maidlip muna sa kalagitnaan ng bundok. Nilampasan siya ng Pagong na mahimbing na natutulog.", "english": "Sure of himself, Rabbit decided to nap halfway up. Turtle passed him as he slept soundly.", "illustrationDesc": "A rabbit asleep under a tree as a turtle passes by"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Nang magising ang Kuneho, natanaw niya ang Pagong sa tuktok ng bundok. Naunahan na pala siya.", "english": "When Rabbit woke, he saw Turtle already at the summit. He had been overtaken after all.", "illustrationDesc": "A turtle standing at a mountain summit against the sky"}
        ]"""

        val story4Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Nakatuwaan ng isang daga na maglaro sa likod ng isang natutulog na leon, umaakyat at nagpapadausdos pababa.", "english": "A mouse amused himself playing on the back of a sleeping lion, climbing up and sliding down again.", "illustrationDesc": "A small mouse climbing the back of a sleeping lion"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Sa katuwaan ay hindi niya napansing nagising ang leon. Dinakma siya nito at hinawakan sa buntot.", "english": "In his fun he did not notice the lion wake. The lion seized him and held him by the tail.", "illustrationDesc": "A lion holding a small mouse by the tail"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Nagmakaawa ang daga. Sinabi niyang hindi sinasadya ang paggambala at wala siyang masamang hangarin.", "english": "The mouse begged. He said the disturbance was not intended and that he meant no harm.", "illustrationDesc": "A mouse pleading, held in a lion's paw"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Pinakawalan siya ng leon. Nangako ang daga na balang araw ay makakaganti rin siya sa kabutihan nito.", "english": "The lion let him go. The mouse promised that one day he would repay the kindness.", "illustrationDesc": "A lion releasing a mouse onto the forest floor"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Lumipas ang maraming araw. Nakita ng daga ang leon na nahuli sa lambat na bitag ng mga mangangaso.", "english": "Many days passed. The mouse found the lion caught in a net set as a trap by hunters.", "illustrationDesc": "A lion tangled in a hunting net among trees"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Nginatngat ng daga ang lubid hanggang maputol. Sabi ng leon, utang niya rito ang kanyang buhay.", "english": "The mouse gnawed the rope until it broke. The lion said he owed the mouse his life.", "illustrationDesc": "A mouse gnawing through rope as a lion watches"}
        ]"""

        val story5Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Isang magsasaka ang nais lumipat ng bayan. Inilulan niya ang kanyang mga gamit sa alagang kabayo at kalabaw.", "english": "A farmer wished to move to another town. He loaded his belongings onto his horse and his carabao.", "illustrationDesc": "A farmer loading bundles onto a horse and a carabao at dawn"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Makalipas ang ilang oras ay nanghina ang kalabaw dahil sa bigat ng kanyang pasan.", "english": "After some hours the carabao weakened under the weight of his load.", "illustrationDesc": "A carabao straining under a heavy load on a dusty road"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Nakiusap siya sa kabayo na pasanin ang ilan sa kanyang dala. Tumanggi ang kabayo at binilisan pa ang paglakad.", "english": "He asked the horse to carry some of his burden. The horse refused and walked faster still.", "illustrationDesc": "A horse walking ahead, leaving a carabao behind"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Tumindi ang init ng araw. Hindi nakayanan ng kalabaw ang bigat at siya ay pumanaw.", "english": "The heat of the sun grew fierce. The carabao could not bear the weight, and he died.", "illustrationDesc": "A dusty road under a harsh midday sun"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Inilipat ng magsasaka sa kabayo ang lahat ng gamit. Bulong nito sa sarili: kung tinulungan ko lang sana ang kasama ko.", "english": "The farmer moved every load onto the horse. He whispered to himself: if only I had helped my companion.", "illustrationDesc": "A horse carrying a doubled load alone"}
        ]"""

        val story6Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Nais ng pagong na makipagkaibigan sa kalabaw. Tumawa ang kalabaw at sinabing ayaw niya ng kaibigang maliit at makupad.", "english": "The turtle wished to befriend the carabao. The carabao laughed and said he wanted no friend so small and so slow.", "illustrationDesc": "A large carabao towering over a small turtle"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Napahiya ang pagong. Sinabi niyang mapang-api ang kalabaw at minamaliit nito ang kanyang kakayahan.", "english": "The turtle was shamed. He said the carabao was a bully who belittled what he could do.", "illustrationDesc": "A turtle standing its ground before a carabao"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Hinamon ng kalabaw ang pagong ng karera. Tinanggap ito ng pagong at itinakda ang laban kinabukasan.", "english": "The carabao challenged the turtle to a race. The turtle accepted and set the contest for the next day.", "illustrationDesc": "Two animals agreeing terms beside a river"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Kinausap ng pagong ang apat niyang kaibigan. Pinapuwesto niya ang mga ito sa tuktok ng bawat bundok.", "english": "The turtle spoke with four of his friends. He placed each of them at the summit of a different hill.", "illustrationDesc": "Four turtles waiting at the tops of five hills"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Sa bawat bundok na marating ng kalabaw ay may pagong na nauna sa kanya. Ganoon din sa ikalima.", "english": "At every hilltop the carabao reached, a turtle had arrived before him. So it went to the fifth.", "illustrationDesc": "A carabao arriving at a hilltop to find a turtle waiting"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Sa galit at kahihiyan ay sinipa ng kalabaw ang pagong. Matigas ang likod nito, kaya ang kalabaw ang napaiyak sa sakit.", "english": "In anger and shame the carabao kicked the turtle. Its shell was hard, and it was the carabao who cried out in pain.", "illustrationDesc": "A carabao recoiling in pain beside an unharmed turtle"}
        ]"""

        val story7Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Mayabang na inilatag ng Agila ang malapad niyang pakpak sa kaitaasan at hinamon ang Kalapati sa pabilisan ng paglipad.", "english": "The eagle spread his broad wings proudly on high and challenged the dove to a race.", "illustrationDesc": "An eagle with wings spread wide above a valley"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Naisip ng Kalapati na bigyan ng aral ang humahamon. Pumayag siya at tinanong kung kailan ito gaganapin.", "english": "The dove thought to teach the challenger a lesson. She agreed and asked when it would be held.", "illustrationDesc": "A white dove perched calmly on a branch"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Napansin ng Kalapati ang maitim na ulap sa kalawakan. Alam niyang ilang sandali na lamang ay uulan.", "english": "The dove noticed dark clouds gathering in the sky. She knew that rain was only moments away.", "illustrationDesc": "Dark storm clouds massing over green mountains"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Iminungkahi niyang may kagat-kagat silang dala. Magdadala siya ng tipak ng asin; ang Agila ay bungkos ng bulak.", "english": "She proposed that each carry something in their beak. She would take a block of salt; the eagle a bundle of cotton.", "illustrationDesc": "A block of salt and a bundle of cotton side by side"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Bumuhos ang ulan. Nabasa ang bulak at bumigat, habang natunaw ang asin at gumaan ang dala ng Kalapati.", "english": "The rain poured down. The cotton soaked and grew heavy, while the salt dissolved and the dove's load lightened.", "illustrationDesc": "An eagle struggling in rain while a dove flies freely"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Nanalo ang Kalapati. Mula noon ay hindi na nagyabang ang palalong Agila.", "english": "The dove won. From that day the proud eagle boasted no more.", "illustrationDesc": "A dove alighting on a green summit ahead of an eagle"}
        ]"""

        val story8Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Noong unang panahon, ang mga alitaptap ay maliliit na kulisap lamang na walang dalang apoy.", "english": "Long ago the fireflies were only small insects, and they carried no fire at all.", "illustrationDesc": "Small dark insects flying under a night sky"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Gabi lamang sila lumilipad, ngunit ayaw nila ng madilim na gabi. Kapag walang buwan ay nagtatago sila sa mga damo at bulaklak.", "english": "They flew only at night, yet they hated dark nights. When there was no moon they hid among the grass and the flowers.", "illustrationDesc": "Insects hiding among leaves and closed blossoms"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Tinanong sila ng punong sampaguita kung bakit sila natatakot. Sagot nila, hindi sa dilim kundi sa mga kabag na kumakain sa kanila.", "english": "A sampaguita tree asked why they were afraid. They answered that it was not the dark but the bats that ate them.", "illustrationDesc": "A flowering sampaguita tree at night"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Sabi ng mga kulisap, kapag maliwanag ang buwan ay hindi sila mahuli, dahil nasisilaw sa liwanag ang mga kabag.", "english": "The insects said that when the moon was bright they were not caught, because the light dazzled the bats.", "illustrationDesc": "Moonlight falling across a garden"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Tinuruan sila ng sampaguita: magdala ang bawat isa ng apoy at sabay-sabay lumabas. Matatakot ang mga kabag sa kanila.", "english": "The sampaguita taught them: let each one carry a fire and go out together. The bats would fear them.", "illustrationDesc": "A tree seeming to speak to a cloud of insects"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Ganoon nga ang ginawa nila. Para silang mga alipatong lumilipad, at hindi na sila malapitan. Sila ang tinatawag ngayong alitaptap.", "english": "So they did. They flew like drifting sparks, and none could come near them. These are what we now call fireflies.", "illustrationDesc": "Hundreds of glowing fireflies rising around a flowering tree"}
        ]"""

        val story9Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "Ubos na ang pagkain sa munting pulo ng Matsing. Gutom na gutom siya, at ang tanging pagkain ay nasa kabilang malaking pulo.", "english": "The food on Monkey's small island was gone. He was very hungry, and the only food lay on the large island across the water.", "illustrationDesc": "A monkey on a bare islet looking across a channel"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Mahirap ang pagtawid dahil maraming buaya sa tubig, gutom din tulad niya. Nagpasya pa rin siyang tumawid.", "english": "Crossing was hard, for many crocodiles swam there, as hungry as he was. Still he decided to cross.", "illustrationDesc": "Crocodiles drifting in a narrow channel of water"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Hinarang siya ni Buaya at hiningi ang kanyang atay. Sinabi ni Matsing na naiwan daw niya ito sa kabilang pampang.", "english": "A crocodile stopped him and demanded his liver. Monkey said he had left it behind on the other shore.", "illustrationDesc": "A monkey speaking to a crocodile in midwater"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Isinakay siya ni Buaya sa likod nito at itinawid. Pagdating sa pampang ay tumakbo si Matsing at tumawa. Sino ba ang nag-iiwan ng atay?", "english": "The crocodile carried him across on its back. On reaching the shore Monkey ran and laughed. Who ever leaves his liver behind?", "illustrationDesc": "A monkey leaping from a crocodile's back onto a shore"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Naghiganti si Buaya. Nagtago siya sa bahay ni Matsing, ngunit sumigaw ang matsing: kung may nasa loob, tumahimik; kung wala, humiyaw!", "english": "The crocodile sought revenge. He hid inside Monkey's house, but Monkey called out: if someone is inside, be silent; if no one waits, shout!", "illustrationDesc": "A house with a shadow waiting inside the doorway"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Humiyaw si Buaya at nabuko. Sa huli, nagkunwari siyang patay, ngunit umungol nang tanungin siya. Palaging nakaiwas ang tusong Matsing.", "english": "The crocodile shouted and gave himself away. At last he played dead, but groaned when questioned. The cunning monkey always escaped.", "illustrationDesc": "A crocodile lying still on mud while a monkey watches from a distance"}
        ]"""

        val story10Pages = """[
            {"pageNumber": 1, "kasiguranin": "", "tagalog": "May isang diwatang napakapangit. Ang mukha niya ay mapula at kulubot, at pilay siya kaya hingkod-hingkod kung lumakad.", "english": "There was an enchantress who was very ugly. Her face was red and wrinkled, and she was lame, so she limped as she walked.", "illustrationDesc": "A stooped figure walking with difficulty along a shore"},
            {"pageNumber": 2, "kasiguranin": "", "tagalog": "Ang bahay niya ay nasa magandang dalampasigan, may looban na tinamnan ng sari-saring halamang namumulaklak.", "english": "Her house stood on a beautiful shore, with a garden planted with all manner of flowering things.", "illustrationDesc": "A bright garden of many flowers beside the sea"},
            {"pageNumber": 3, "kasiguranin": "", "tagalog": "Dalawang maralitang bata, sina Malakas at Ligaya, ang naglibot at nakakita ng bahay ng diwata. Pumasok sila sa halamanan.", "english": "Two poor children, Malakas and Ligaya, wandered by and saw the enchantress's house. They stepped into the garden.", "illustrationDesc": "Two children opening a garden gate"},
            {"pageNumber": 4, "kasiguranin": "", "tagalog": "Namupol sila ng bulaklak at kumain ng bunga. Dumating ang diwata at tinanong kung bakit sila nangahas pumasok.", "english": "They picked flowers and ate the fruit. The enchantress came and asked why they had dared to enter.", "illustrationDesc": "Children startled among flowering plants"},
            {"pageNumber": 5, "kasiguranin": "", "tagalog": "Inamin nila ang kasalanan. Sila ay mga ulila, at handa silang magsilbi bilang kabayaran.", "english": "They admitted their fault. They were orphans, and were willing to serve as payment.", "illustrationDesc": "Two children standing with heads bowed"},
            {"pageNumber": 6, "kasiguranin": "", "tagalog": "Bumulong ang diwata ng mahiwagang salita at naging maganda siya. Sinabi niyang gagawin silang hardinero ng kanyang mga bulaklak.", "english": "The enchantress whispered a word of magic and became beautiful. She said she would make them gardeners of her flowers.", "illustrationDesc": "A radiant figure holding a star-tipped staff"},
            {"pageNumber": 7, "kasiguranin": "", "tagalog": "Dinantayan niya sila ng mahiwagang baston at sa isang iglap ay nagkapakpak sila. Sila ngayon ang mga paru-paro ng diwata.", "english": "She touched them with her magic staff and in an instant they had wings. They are now the butterflies of the enchantress.", "illustrationDesc": "Two butterflies with black, blue, green and orange wings over a garden"}
        ]"""

        return listOf(
            StoryEntity(
                id = 1,
                title = "Si Pagong at si Matsing",
                titleKasiguranin = "",
                description = "The Turtle and the Monkey. A patient turtle and a scheming monkey share a banana tree, and each learns what tricking a friend costs.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story1Pages,
                totalPages = 6,
                requiredXp = 0,
                isUnlocked = true
            ),
            StoryEntity(
                id = 2,
                title = "Si Langgam at si Tipaklong",
                titleKasiguranin = "",
                description = "The Ant and the Grasshopper. One friend stores food through the fine weather and the other sings through it, until the rains arrive.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story2Pages,
                totalPages = 6,
                requiredXp = 0,
                isUnlocked = true
            ),
            StoryEntity(
                id = 3,
                title = "Ang Kuneho at ang Pagong",
                titleKasiguranin = "",
                description = "The Rabbit and the Turtle. A rabbit mocks a turtle for being slow, then races him to the top of a mountain.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story3Pages,
                totalPages = 6,
                requiredXp = 30,
                isUnlocked = false
            ),
            StoryEntity(
                id = 4,
                title = "Ang Daga at ang Leon",
                titleKasiguranin = "",
                description = "The Mouse and the Lion. A mouse spares a sleeping lion's temper, and later repays the debt with its teeth.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story4Pages,
                totalPages = 6,
                requiredXp = 60,
                isUnlocked = false
            ),
            StoryEntity(
                id = 5,
                title = "Ang Pabula ng Kabayo at Kalabaw",
                titleKasiguranin = "",
                description = "The Fable of the Horse and the Carabao. A carabao asks a horse to share the load on a long journey, and the horse refuses.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story5Pages,
                totalPages = 5,
                requiredXp = 90,
                isUnlocked = false
            ),
            StoryEntity(
                id = 6,
                title = "Ang Pagong at ang Kalabaw",
                titleKasiguranin = "",
                description = "The Turtle and the Carabao. A carabao belittles a turtle's size, and the turtle answers with a race and four friends.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story6Pages,
                totalPages = 6,
                requiredXp = 130,
                isUnlocked = false
            ),
            StoryEntity(
                id = 7,
                title = "Ang Agila at ang Kalapati",
                titleKasiguranin = "",
                description = "The Eagle and the Dove. A boastful eagle challenges a dove to a race, and the dove chooses what each will carry.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story7Pages,
                totalPages = 6,
                requiredXp = 170,
                isUnlocked = false
            ),
            StoryEntity(
                id = 8,
                title = "Alamat ng mga Unang Alitaptap",
                titleKasiguranin = "",
                description = "Legend of the First Fireflies. Small insects hid from bats in the dark until a sampaguita tree taught them to carry fire.",
                category = "Legend",
                iconEmoji = "",
                pagesJson = story8Pages,
                totalPages = 6,
                requiredXp = 220,
                isUnlocked = false
            ),
            StoryEntity(
                id = 9,
                title = "Ang Buaya at ang Tusong Matsing",
                titleKasiguranin = "",
                description = "The Crocodile and the Cunning Monkey. A hungry monkey must cross water full of crocodiles, and talks his way past every trap.",
                category = "Fable",
                iconEmoji = "",
                pagesJson = story9Pages,
                totalPages = 6,
                requiredXp = 280,
                isUnlocked = false
            ),
            StoryEntity(
                id = 10,
                title = "Alamat ng Paru-paro",
                titleKasiguranin = "",
                description = "Legend of the Butterfly. Two orphans wander into an enchantress's garden and are changed by what they love.",
                category = "Legend",
                iconEmoji = "",
                pagesJson = story10Pages,
                totalPages = 7,
                requiredXp = 350,
                isUnlocked = false
            )
        )
    }

    fun getInitialAchievements(): List<AchievementEntity> = listOf(
        AchievementEntity(
            id = "level_1",
            metricType = "level",
            name = "Novice Explorer",
            description = "Began your Kasiguranin learning journey",
            iconEmoji = "🌱",
            category = "Level",
            requiredValue = 1,
            isUnlocked = true,
            xpReward = 10
        ),
        AchievementEntity(
            id = "level_2",
            metricType = "level",
            name = "Vocab Apprentice",
            description = "Reached Level 2 & unlocked Fill in the Blank",
            iconEmoji = "📚",
            category = "Level",
            requiredValue = 2,
            xpReward = 50
        ),
        AchievementEntity(
            id = "level_3",
            metricType = "level",
            name = "Linguistic Scholar",
            description = "Reached Level 3 & unlocked Audio Listening Quiz",
            iconEmoji = "🎧",
            category = "Level",
            requiredValue = 3,
            xpReward = 100
        ),
        AchievementEntity(
            id = "level_4",
            metricType = "level",
            name = "Grammar Specialist",
            description = "Reached Level 4 & unlocked Verb Aspect Builder",
            iconEmoji = "⚡",
            category = "Level",
            requiredValue = 4,
            xpReward = 150
        ),
        AchievementEntity(
            id = "level_5",
            metricType = "level",
            name = "Kasiguranin Legend",
            description = "Reached Level 5 & unlocked Sentence Construction",
            iconEmoji = "👑",
            category = "Level",
            requiredValue = 5,
            xpReward = 300
        ),
        AchievementEntity(
            id = Constants.Achievements.FIRST_WORD,
            metricType = "wordsLearned",
            name = "Unáng Salitâ",
            description = "Learn your first Kasiguranin word",
            iconEmoji = "🌟",
            category = "Progress",
            requiredValue = 1,
            xpReward = 20
        ),
        AchievementEntity(
            id = Constants.Achievements.TEN_WORDS,
            metricType = "wordsLearned",
            name = "Sampûng Salitâ",
            description = "Learn 10 Kasiguranin words",
            iconEmoji = "📖",
            category = "Progress",
            requiredValue = 10,
            xpReward = 50
        ),
        AchievementEntity(
            id = Constants.Achievements.FIFTY_WORDS,
            metricType = "wordsLearned",
            tier = "bronze",
            name = "Limampûng Salitâ",
            description = "Learn 50 Kasiguranin words",
            iconEmoji = "🎓",
            category = "Progress",
            requiredValue = 50,
            xpReward = 200
        ),
        AchievementEntity(
            id = Constants.Achievements.FIRST_STORY,
            metricType = "storiesCompleted",
            name = "Mambábasa",
            description = "Complete your first story",
            iconEmoji = "📕",
            category = "Progress",
            requiredValue = 1,
            xpReward = 50
        ),
        AchievementEntity(
            id = Constants.Achievements.FIRST_GAME,
            metricType = "gamesPlayed",
            name = "Mánlalaro",
            description = "Play your first mini-game",
            iconEmoji = "🎮",
            category = "Progress",
            requiredValue = 1,
            xpReward = 25
        ),
        AchievementEntity(
            id = Constants.Achievements.PERFECT_GAME,
            metricType = "perfectGame",
            name = "Perpekto!",
            description = "Get a perfect score in any mini-game",
            iconEmoji = "⭐",
            category = "Games",
            requiredValue = 1,
            xpReward = 100
        ),
        AchievementEntity(
            id = Constants.Achievements.THREE_DAY_STREAK,
            metricType = "streak",
            name = "Tatlong Aldaw",
            description = "Maintain a 3-day learning streak",
            iconEmoji = "🔥",
            category = "Streaks",
            requiredValue = 3,
            xpReward = 50
        ),
        AchievementEntity(
            id = Constants.Achievements.SEVEN_DAY_STREAK,
            metricType = "streak",
            tier = "bronze",
            name = "Isáng Linggo",
            description = "Maintain a 7-day learning streak",
            iconEmoji = "💪",
            category = "Streaks",
            requiredValue = 7,
            xpReward = 150
        ),
        AchievementEntity(
            id = Constants.Achievements.LEVEL_FIVE,
            metricType = "level",
            tier = "silver",
            name = "Sumusulong",
            description = "Reach Level 5",
            iconEmoji = "🏅",
            category = "Progress",
            requiredValue = 5,
            xpReward = 100
        ),
        AchievementEntity(
            id = Constants.Achievements.LEVEL_TEN,
            metricType = "level",
            tier = "gold",
            name = "Mæstro",
            description = "Reach Level 10 — Master of Kasiguranin!",
            iconEmoji = "👑",
            category = "Progress",
            requiredValue = 10,
            xpReward = 500
        ),
        // Previously defined in Constants and referenced by checkStoryAchievements, but never
        // actually seeded - tryUnlock silently no-ops on a missing id, so this could never
        // unlock. Fixed here as part of making unlock logic generic and metric-driven.
        AchievementEntity(
            id = Constants.Achievements.ALL_STORIES,
            metricType = "storiesCompleted",
            tier = "silver",
            name = "Tagapagsalaysay",
            description = "Complete 3 stories",
            iconEmoji = "📚",
            category = "Progress",
            requiredValue = 3,
            xpReward = 150
        ),

        // ── New achievements: submission-based ──────────────────────────────
        AchievementEntity(
            id = Constants.Achievements.FIRST_CONTRIBUTION,
            metricType = "submissionsMade",
            name = "First Contribution",
            description = "Submit your first word, story, or poem",
            iconEmoji = "✍️",
            category = "Contribution",
            requiredValue = 1,
            xpReward = 30
        ),
        AchievementEntity(
            id = Constants.Achievements.TRUSTED_VOICE,
            metricType = "submissionsApproved",
            tier = "bronze",
            name = "Trusted Voice",
            description = "Have 5 submissions approved into the dictionary or Stories",
            iconEmoji = "🗣️",
            category = "Contribution",
            requiredValue = 5,
            xpReward = 150
        ),
        AchievementEntity(
            id = Constants.Achievements.CORPUS_BUILDER,
            metricType = "submissionsApproved",
            tier = "gold",
            name = "Corpus Builder",
            description = "Have 25 submissions approved into the dictionary or Stories",
            iconEmoji = "🏛️",
            category = "Contribution",
            requiredValue = 25,
            xpReward = 500
        ),

        // ── New achievements: deeper streak/mastery tiers ───────────────────
        AchievementEntity(
            id = Constants.Achievements.MOON_CYCLE,
            metricType = "streak",
            tier = "silver",
            name = "Moon Cycle",
            description = "Maintain a 30-day learning streak",
            iconEmoji = "🌙",
            category = "Streaks",
            requiredValue = 30,
            xpReward = 300
        ),
        AchievementEntity(
            id = Constants.Achievements.CENTURION,
            metricType = "streak",
            tier = "gold",
            name = "Centurion",
            description = "Maintain a 100-day learning streak",
            iconEmoji = "🏛️",
            category = "Streaks",
            requiredValue = 100,
            xpReward = 1000
        ),
        // Tracked per category as each one is completed - see
        // UserProgressRepository.checkCategoryMastery, called after a word is learned.
        AchievementEntity(
            id = Constants.Achievements.CATEGORY_MASTER,
            metricType = "categoryMastered",
            tier = "gold",
            name = "Category Master",
            description = "Learn every word in one dictionary category",
            iconEmoji = "🎯",
            category = "Mastery",
            requiredValue = 1,
            xpReward = 250
        ),
        // Seeded so it appears in the Achievements list with real criteria; live tracking (a
        // perfect score across all six game modes in one sitting) needs cross-screen session
        // state this pass doesn't add yet - a follow-up, not a placeholder value.
        AchievementEntity(
            id = Constants.Achievements.PERFECT_SIX,
            metricType = "perfectSixInOneSitting",
            tier = "gold",
            name = "Perfect Six",
            description = "Score perfectly in all six game modes in one sitting",
            iconEmoji = "💯",
            category = "Games",
            requiredValue = 1,
            xpReward = 400
        ),

        // ── New achievements: social/leaderboard-based ──────────────────────
        // Tracked from the live weekly leaderboard - see
        // UserProgressRepository.checkWeeklyLeaderboardAchievement.
        AchievementEntity(
            id = Constants.Achievements.TOP_OF_THE_WEEK,
            metricType = "weeklyTopTen",
            tier = "silver",
            name = "Top of the Week",
            description = "Reach the top 10 of the weekly leaderboard",
            iconEmoji = "📈",
            category = "Social",
            requiredValue = 1,
            xpReward = 200
        ),
        AchievementEntity(
            id = Constants.Achievements.SIX_FOR_SIX,
            metricType = "gameModesPlayed",
            name = "Six for Six",
            description = "Play every one of the six mini-game modes at least once",
            iconEmoji = "🎲",
            category = "Games",
            requiredValue = 6,
            xpReward = 150
        )
    )

    fun getInitialUserProgress(): UserProgressEntity = UserProgressEntity(
        id = 1,
        userName = "Learner",
        totalXp = 0,
        level = 1,
        currentStreak = 0,
        longestStreak = 0,
        lastActiveDate = "",
        wordsLearned = 0,
        storiesCompleted = 0,
        gamesPlayed = 0,
        totalCorrectAnswers = 0,
        totalQuestionsAnswered = 0,
        lessonsCompleted = 0
    )

    fun getInitialLeaderboard(): List<com.kasiguru.data.local.entity.LeaderboardEntity> = listOf(
        com.kasiguru.data.local.entity.LeaderboardEntity(
            id = 101,
            name = "Ligaya Santos",
            totalXp = 850,
            currentStreak = 14,
            avatarIconId = 2,
            levelTitle = "Grammar Specialist",
            isCurrentUser = false
        ),
        com.kasiguru.data.local.entity.LeaderboardEntity(
            id = 102,
            name = "Juan dela Cruz",
            totalXp = 620,
            currentStreak = 9,
            avatarIconId = 3,
            levelTitle = "Linguistic Scholar",
            isCurrentUser = false
        ),
        com.kasiguru.data.local.entity.LeaderboardEntity(
            id = 103,
            name = "Marco Ramirez",
            totalXp = 490,
            currentStreak = 7,
            avatarIconId = 4,
            levelTitle = "Vocab Apprentice",
            isCurrentUser = false
        ),
        com.kasiguru.data.local.entity.LeaderboardEntity(
            id = 104,
            name = "Elena Alonzo",
            totalXp = 380,
            currentStreak = 5,
            avatarIconId = 1,
            levelTitle = "Vocab Apprentice",
            isCurrentUser = false
        ),
        com.kasiguru.data.local.entity.LeaderboardEntity(
            id = 105,
            name = "Bayani Pascual",
            totalXp = 210,
            currentStreak = 4,
            avatarIconId = 5,
            levelTitle = "Novice Explorer",
            isCurrentUser = false
        ),
        com.kasiguru.data.local.entity.LeaderboardEntity(
            id = 106,
            name = "Marisol Reyes",
            totalXp = 140,
            currentStreak = 2,
            avatarIconId = 2,
            levelTitle = "Novice Explorer",
            isCurrentUser = false
        )
    )

    fun getInitialNotifications(): List<com.kasiguru.data.local.entity.NotificationEntity> = listOf(
        com.kasiguru.data.local.entity.NotificationEntity(
            id = 1,
            title = "🔥 Keep Your 5-Day Streak Alive!",
            message = "You are only 3 words away from reaching your daily learning goal today.",
            timestamp = "10 mins ago",
            category = "Streak",
            isRead = false,
            deepLinkRoute = "vocabulary"
        ),
        com.kasiguru.data.local.entity.NotificationEntity(
            id = 2,
            title = "🌟 Word of the Day: Magandang Aldew",
            message = "Kasiguranin greeting for 'Good Day / Good Morning'. Learn its audio & usage!",
            timestamp = "2 hours ago",
            category = "WordOfDay",
            isRead = false,
            deepLinkRoute = "vocabulary"
        ),
        com.kasiguru.data.local.entity.NotificationEntity(
            id = 3,
            title = "🏆 Leaderboard Rank #2 Reclaimed!",
            message = "You climbed to Rank #2 on the Global Leaderboard with 850 total XP!",
            timestamp = "Yesterday",
            category = "Leaderboard",
            isRead = true,
            deepLinkRoute = "leaderboard"
        ),
        com.kasiguru.data.local.entity.NotificationEntity(
            id = 4,
            title = "🎓 Badge Unlocked: Linguistic Scholar!",
            message = "Congratulations! You reached Level 3 & unlocked the Linguistic Scholar badge.",
            timestamp = "2 days ago",
            category = "Achievement",
            isRead = true,
            deepLinkRoute = "achievements"
        )
    )
}
