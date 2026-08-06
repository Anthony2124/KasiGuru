package com.kasiguru.data.local

import com.kasiguru.data.local.entity.*
import com.kasiguru.util.Constants

/**
 * Complete Kasiguranin Linguistic Corpus DatabaseSeeder.
 * Extracted directly from UP Thesis: A Grammatical Sketch of Kasiguranin (Supnet, 2016).
 * Contains 394 vocabulary entries.
 */
object DatabaseSeeder {

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
        val story1Pages = """[
            {"pageNumber":1,"kasiguranin":"Ha baybayin ng Casiguran, may isáng batang babae na ang pangalan ay Si Maring.","tagalog":"Sa baybayin ng Casiguran, may isang batang babae na ang pangalan ay Si Maring.","english":"On the shore of Casiguran, there was a young girl named Maring.","illustrationDesc":"A young girl standing on a beautiful beach at sunrise, with the Sierra Madre mountains in the background"},
            {"pageNumber":2,"kasiguranin":"Araw-araw, lumálakad si Maring papuntá ha dagat para mangisdâ kasama ing kanyang ama.","tagalog":"Araw-araw, lumalakad si Maring papunta sa dagat para mangisda kasama ang kanyang ama.","english":"Every day, Maring walked to the sea to fish with her father.","illustrationDesc":"Maring walking along a path with her father carrying fishing nets, coconut trees lining the way"},
            {"pageNumber":3,"kasiguranin":"Isáng aldaw, nakatagpô siya ng magandáng kabibe ha dalampasigan.","tagalog":"Isang araw, nakatagpo siya ng magandang kabibe sa dalampasigan.","english":"One day, she found a beautiful shell on the beach.","illustrationDesc":"A glowing, colorful seashell on the sand with gentle waves"},
            {"pageNumber":4,"kasiguranin":"'Ina,' sabi niya, 'tingnan mo itong kabibe! Parang kinúlay ng aldaw at dagat!'","tagalog":"'Ina,' sabi niya, 'tingnan mo itong kabibe! Parang kinulayan ng araw at dagat!'","english":"'Mother,' she said, 'look at this shell! It seems painted by the sun and sea!'","illustrationDesc":"Maring showing the shell to her mother inside their nipa hut"},
            {"pageNumber":5,"kasiguranin":"Mula noon, iningatan ni Maring ing kabibe bilang aláala ng kagandahan ng Casiguran.","tagalog":"Mula noon, iningatan ni Maring ang kabibe bilang alaala ng kagandahan ng Casiguran.","english":"From then on, Maring kept the shell as a reminder of Casiguran's beauty.","illustrationDesc":"Maring holding the shell against a stunning sunset over the Casiguran bay"}
        ]"""

        val story2Pages = """[
            {"pageNumber":1,"kasiguranin":"Noong unang panahon, may isáng matandáng mangingisdâ na tinatawag na Mang Tasyo.","tagalog":"Noong unang panahon, may isang matandang mangingisda na tinatawag na Mang Tasyo.","english":"Long ago, there was an old fisherman called Mang Tasyo.","illustrationDesc":"An old fisherman sitting in a wooden boat, mending his nets at dawn"},
            {"pageNumber":2,"kasiguranin":"Kilala siya ha buóng Casiguran dahil ha kanyang karunungan tungkol ha dagat.","tagalog":"Kilala siya sa buong Casiguran dahil sa kanyang karunungan tungkol sa dagat.","english":"He was known throughout Casiguran for his wisdom about the sea.","illustrationDesc":"Villagers gathered around Mang Tasyo as he tells stories by a campfire"},
            {"pageNumber":3,"kasiguranin":"'Pakinggan niyo ing dagat,' sabi ni Mang Tasyo. 'Itó ay nagsásalitâ kung makikinig ka.'","tagalog":"'Pakinggan niyo ang dagat,' sabi ni Mang Tasyo. 'Ito ay nagsasalita kung makikinig ka.'","english":"'Listen to the sea,' said Mang Tasyo. 'It speaks if you listen.'","illustrationDesc":"Mang Tasyo pointing at the ocean, moonlight reflecting off the waves"},
            {"pageNumber":4,"kasiguranin":"Tinuruan niya ing mga kabataan kung paano basáhin ing hangin, ing alon, at ing mga bituin.","tagalog":"Tinuruan niya ang mga kabataan kung paano basahin ang hangin, ang alon, at ang mga bituin.","english":"He taught the youth how to read the wind, the waves, and the stars.","illustrationDesc":"Young people learning from Mang Tasyo on a boat, stars visible above"}
        ]"""

        val story3Pages = """[
            {"pageNumber":1,"kasiguranin":"Tuwing buwan ng Mayo, nagdíriwang ing bayan ng Casiguran ng kanilang pista.","tagalog":"Tuwing buwan ng Mayo, nagdiriwang ang bayan ng Casiguran ng kanilang pista.","english":"Every May, the town of Casiguran celebrates its fiesta.","illustrationDesc":"A vibrant town plaza decorated with colorful banners and lights"},
            {"pageNumber":2,"kasiguranin":"Naghahanda ing bawat pamilya ng masasarap na pagkain galing ha dagat at bukid.","tagalog":"Naghahanda ang bawat pamilya ng masasarap na pagkain galing sa dagat at bukid.","english":"Every family prepares delicious food from the sea and the fields.","illustrationDesc":"Tables full of Filipino food - grilled fish, rice, vegetables, with families cooking together"},
            {"pageNumber":3,"kasiguranin":"Sumásayaw ing mga tao ha kalye habang tumútugtog ing banda ng musika.","tagalog":"Sumasayaw ang mga tao sa kalye habang tumutugtog ang banda ng musika.","english":"People dance in the streets while the music band plays.","illustrationDesc":"Colorful street dancing with traditional Filipino costumes and a marching band"},
            {"pageNumber":4,"kasiguranin":"Ing pistá ay panahon ng pasasalamat at pagkakaisá ng buóng komunidad.","tagalog":"Ang pista ay panahon ng pasasalamat at pagkakaisa ng buong komunidad.","english":"The fiesta is a time of thanksgiving and unity for the whole community.","illustrationDesc":"The whole community gathered together, fireworks in the night sky over Casiguran bay"},
            {"pageNumber":5,"kasiguranin":"Sa ganitong paraan, nananatili ing tradisyon at wika ng Casiguran sa pusó ng bawat tao.","tagalog":"Sa ganitong paraan, nananatili ang tradisyon at wika ng Casiguran sa puso ng bawat tao.","english":"In this way, the traditions and language of Casiguran live on in every person's heart.","illustrationDesc":"A family walking home from the fiesta under a starlit sky, the mountains silhouetted behind them"}
        ]"""

        return listOf(
            StoryEntity(
                id = 1,
                title = "The Shell of Casiguran",
                titleKasiguranin = "Ing Kabibe ng Casiguran",
                description = "A young girl discovers the beauty of her coastal hometown through a magical shell.",
                category = "Culture",
                iconEmoji = "🐚",
                pagesJson = story1Pages,
                totalPages = 5,
                requiredXp = 0,
                isUnlocked = true
            ),
            StoryEntity(
                id = 2,
                title = "The Wise Fisherman",
                titleKasiguranin = "Ing Marunong na Mangingisdâ",
                description = "Old Mang Tasyo shares his wisdom about the sea with the youth of Casiguran.",
                category = "Wisdom",
                iconEmoji = "🎣",
                pagesJson = story2Pages,
                totalPages = 4,
                requiredXp = 100,
                isUnlocked = false
            ),
            StoryEntity(
                id = 3,
                title = "The Town Fiesta",
                titleKasiguranin = "Ing Pistá ng Bayan",
                description = "Experience the joy and traditions of Casiguran's annual fiesta celebration.",
                category = "Traditions",
                iconEmoji = "🎉",
                pagesJson = story3Pages,
                totalPages = 5,
                requiredXp = 250,
                isUnlocked = false
            )
        )
    }

    fun getInitialAchievements(): List<AchievementEntity> = listOf(
        AchievementEntity(
            id = "level_1",
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
            name = "Vocab Apprentice",
            description = "Reached Level 2 & unlocked Fill in the Blank",
            iconEmoji = "📚",
            category = "Level",
            requiredValue = 2,
            xpReward = 50
        ),
        AchievementEntity(
            id = "level_3",
            name = "Linguistic Scholar",
            description = "Reached Level 3 & unlocked Audio Listening Quiz",
            iconEmoji = "🎧",
            category = "Level",
            requiredValue = 3,
            xpReward = 100
        ),
        AchievementEntity(
            id = "level_4",
            name = "Grammar Specialist",
            description = "Reached Level 4 & unlocked Verb Aspect Builder",
            iconEmoji = "⚡",
            category = "Level",
            requiredValue = 4,
            xpReward = 150
        ),
        AchievementEntity(
            id = "level_5",
            name = "Kasiguranin Legend",
            description = "Reached Level 5 & unlocked Sentence Construction",
            iconEmoji = "👑",
            category = "Level",
            requiredValue = 5,
            xpReward = 300
        ),
        AchievementEntity(
            id = Constants.Achievements.FIRST_WORD,
            name = "Unáng Salitâ",
            description = "Learn your first Kasiguranin word",
            iconEmoji = "🌟",
            category = "Progress",
            requiredValue = 1,
            xpReward = 20
        ),
        AchievementEntity(
            id = Constants.Achievements.TEN_WORDS,
            name = "Sampûng Salitâ",
            description = "Learn 10 Kasiguranin words",
            iconEmoji = "📖",
            category = "Progress",
            requiredValue = 10,
            xpReward = 50
        ),
        AchievementEntity(
            id = Constants.Achievements.FIFTY_WORDS,
            name = "Limampûng Salitâ",
            description = "Learn 50 Kasiguranin words",
            iconEmoji = "🎓",
            category = "Progress",
            requiredValue = 50,
            xpReward = 200
        ),
        AchievementEntity(
            id = Constants.Achievements.FIRST_STORY,
            name = "Mambábasa",
            description = "Complete your first story",
            iconEmoji = "📕",
            category = "Progress",
            requiredValue = 1,
            xpReward = 50
        ),
        AchievementEntity(
            id = Constants.Achievements.FIRST_GAME,
            name = "Mánlalaro",
            description = "Play your first mini-game",
            iconEmoji = "🎮",
            category = "Progress",
            requiredValue = 1,
            xpReward = 25
        ),
        AchievementEntity(
            id = Constants.Achievements.PERFECT_GAME,
            name = "Perpekto!",
            description = "Get a perfect score in any mini-game",
            iconEmoji = "⭐",
            category = "Games",
            requiredValue = 1,
            xpReward = 100
        ),
        AchievementEntity(
            id = Constants.Achievements.THREE_DAY_STREAK,
            name = "Tatlong Aldaw",
            description = "Maintain a 3-day learning streak",
            iconEmoji = "🔥",
            category = "Streaks",
            requiredValue = 3,
            xpReward = 50
        ),
        AchievementEntity(
            id = Constants.Achievements.SEVEN_DAY_STREAK,
            name = "Isáng Linggo",
            description = "Maintain a 7-day learning streak",
            iconEmoji = "💪",
            category = "Streaks",
            requiredValue = 7,
            xpReward = 150
        ),
        AchievementEntity(
            id = Constants.Achievements.LEVEL_FIVE,
            name = "Sumusulong",
            description = "Reach Level 5",
            iconEmoji = "🏅",
            category = "Progress",
            requiredValue = 5,
            xpReward = 100
        ),
        AchievementEntity(
            id = Constants.Achievements.LEVEL_TEN,
            name = "Mæstro",
            description = "Reach Level 10 — Master of Kasiguranin!",
            iconEmoji = "👑",
            category = "Progress",
            requiredValue = 10,
            xpReward = 500
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
