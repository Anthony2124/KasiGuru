package com.kasiguru.data.local

import com.kasiguru.data.local.entity.*
import com.kasiguru.util.Constants

/**
 * Complete Kasiguranin Linguistic Corpus DatabaseSeeder.
 * Extracted directly from UP Thesis: A Grammatical Sketch of Kasiguranin (Supnet, 2016).
 * Contains 487 vocabulary entries and 0 authentic sentences.
 */
object DatabaseSeeder {

    fun getInitialVocabulary(): List<VocabularyEntity> = listOf(
        VocabularyEntity(
            kasiguranin = "apak",
            tagalog = "daras",
            english = "adze",
            rootForm = "apak",
            category = "Occupations & Tools",
            ipaNotation = "ˈɁaː.pak",
            phoneticGlottal = true,
            phoneticVowelLength = true
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
            kasiguranin = "‘ttanan11",
            tagalog = "lahat",
            english = "all",
            rootForm = "ttanan11",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂət.ta.ˈnan",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "at",
            tagalog = "at",
            english = "and",
            rootForm = "at",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂat",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kanga",
            tagalog = "galit",
            english = "anger",
            rootForm = "kanga",
            category = "Emotions & Feelings",
            ipaNotation = "ˈkaː.ŋaɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "hayop",
            tagalog = "hayop",
            english = "animal",
            rootForm = "hayop",
            category = "Animals & Wildlife",
            ipaNotation = "ˈhaː.jɔp",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "bukong bokong",
            tagalog = "bukung-bukong",
            english = "ankle",
            rootForm = "bukong",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.kɔŋ.ˈbɔː.kɔŋ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "sing’t",
            tagalog = "langgam",
            english = "ant",
            rootForm = "singt",
            category = "Animals & Wildlife",
            ipaNotation = "si.ˈŋət",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "braso",
            tagalog = "bisig",
            english = "arm",
            rootForm = "braso",
            category = "Body Parts & Health",
            ipaNotation = "ˈbraː.sɔ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kili-kile",
            tagalog = "kili-kili",
            english = "armpit",
            rootForm = "kilikile",
            category = "Body Parts & Health",
            ipaNotation = "ki.li.ki.ˈlɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "b’ttek",
            tagalog = "palaso",
            english = "arrow",
            rootForm = "bttek",
            category = "Occupations & Tools",
            ipaNotation = "bət.ˈtɛk",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "abo",
            tagalog = "abo",
            english = "ashes",
            rootForm = "abo",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈbɔ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sa",
            tagalog = "sa",
            english = "at",
            rootForm = "sa",
            category = "Greetings & Essentials",
            ipaNotation = "sa"
        ),
        VocabularyEntity(
            kasiguranin = "lukag",
            tagalog = "gising",
            english = "awake",
            rootForm = "lukag",
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈkag"
        ),
        VocabularyEntity(
            kasiguranin = "ad’g",
            tagalog = "likod",
            english = "back",
            rootForm = "adg",
            category = "Body Parts & Health",
            ipaNotation = "Ɂa.ˈdəg",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "maduk’s",
            tagalog = "masama",
            english = "bad",
            rootForm = "maduks",
            category = "Emotions & Feelings",
            ipaNotation = "ma.du.ˈkəs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "pokpok",
            tagalog = "kalbo",
            english = "bald",
            rootForm = "pokpok",
            category = "Greetings & Essentials",
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
            kasiguranin = "balat ng kahoy kulet ng kayo",
            tagalog = "(tree)",
            english = "bark",
            rootForm = "balat",
            category = "General",
            ipaNotation = "kʊ.ˈlet naŋ ka.ˈjɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tiis tiis",
            tagalog = "suffer",
            english = "bear,",
            rootForm = "tiis",
            category = "Body Parts & Health",
            ipaNotation = "ti.ˈɁis",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "balbas",
            tagalog = "balbas",
            english = "beard",
            rootForm = "balbas",
            category = "Body Parts & Health",
            ipaNotation = "bal.ˈbas"
        ),
        VocabularyEntity(
            kasiguranin = "maganda",
            tagalog = "maganda",
            english = "beautiful",
            rootForm = "maganda",
            category = "Emotions & Feelings",
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
            kasiguranin = "dikk’l",
            tagalog = "Malaki",
            english = "big",
            rootForm = "dikkl",
            category = "Greetings & Essentials",
            ipaNotation = "dik.ˈkəl",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "apdu",
            tagalog = "apdu",
            english = "bile",
            rootForm = "apdu",
            category = "Body Parts & Health",
            ipaNotation = "ˈɁap.dʊ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ibon",
            tagalog = "ibon",
            english = "bird",
            rootForm = "ibon",
            category = "Animals & Wildlife",
            ipaNotation = "ˈɁiː.bɔn",
            phoneticGlottal = true,
            phoneticVowelLength = true
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
            kasiguranin = "maitim mangitet",
            tagalog = "itim,",
            english = "black",
            rootForm = "maitim",
            category = "Colors & Shapes",
            ipaNotation = "ma.ŋi.ˈtɛt"
        ),
        VocabularyEntity(
            kasiguranin = "their symbol for schwa or mid central vowel.",
            tagalog = "is",
            english = "The apostrophe",
            rootForm = "their",
            category = "Greetings & Essentials",
            ipaNotation = "’"
        ),
        VocabularyEntity(
            kasiguranin = "talim tad’m",
            tagalog = "sharpness",
            english = "blade/",
            rootForm = "talim",
            category = "Occupations & Tools",
            ipaNotation = "ta.ˈdəm",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bur’k",
            tagalog = "bulag",
            english = "blind",
            rootForm = "burk",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈrək",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "digi",
            tagalog = "dugo",
            english = "blood",
            rootForm = "digi",
            category = "Body Parts & Health",
            ipaNotation = "di.ˈgiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "b’ggi",
            tagalog = "katawan",
            english = "body",
            rootForm = "bggi",
            category = "Body Parts & Health",
            ipaNotation = "bəg.ˈgiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(also, seed) tulang",
            tagalog = "buto",
            english = "bone",
            rootForm = "also",
            category = "Numbers",
            ipaNotation = "tʊ.ˈlaŋ"
        ),
        VocabularyEntity(
            kasiguranin = "m.,",
            tagalog = "(young",
            english = "boy",
            rootForm = "m",
            category = "Family & People"
        ),
        VocabularyEntity(
            kasiguranin = "ut’k",
            tagalog = "utak",
            english = "brain",
            rootForm = "utk",
            category = "Body Parts & Health",
            ipaNotation = "Ɂʊ.ˈtək",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sanga",
            tagalog = "sanga",
            english = "branch",
            rootForm = "sanga",
            category = "Nature & Environment",
            ipaNotation = "sa.ˈŋaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "suso",
            tagalog = "suso",
            english = "breast",
            rootForm = "suso",
            category = "Body Parts & Health",
            ipaNotation = "sʊ.ˈsɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "mademlag",
            tagalog = "maliwanag",
            english = "bright",
            rootForm = "mademlag",
            category = "Emotions & Feelings",
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
            kasiguranin = "bigkis b’db’d",
            tagalog = "belt",
            english = "bundle,",
            rootForm = "bigkis",
            category = "Greetings & Essentials",
            ipaNotation = "bəd.ˈbəd",
            phoneticGlottal = true
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
            kasiguranin = "puwitan bule",
            tagalog = "puwit,",
            english = "buttocks",
            rootForm = "puwitan",
            category = "Body Parts & Health",
            ipaNotation = "bʊ.ˈleɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "dakip dakəp",
            tagalog = "apprehend",
            english = "catch,",
            rootForm = "dakip",
            category = "Greetings & Essentials",
            ipaNotation = "da.ˈkəp"
        ),
        VocabularyEntity(
            kasiguranin = "biro (niyog)",
            tagalog = "uling",
            english = "charcoal",
            rootForm = "biro",
            category = "General",
            ipaNotation = "bi.ˈrɔɁ",
            phoneticGlottal = true
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
            ipaNotation = "pi.nʊ.nʊɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "anak anak",
            tagalog = "(young)",
            english = "child",
            rootForm = "anak",
            category = "Family & People",
            ipaNotation = "Ɂa.ˈnak",
            phoneticGlottal = true
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
            category = "Emotions & Feelings",
            ipaNotation = "ma.ˈliː.nis",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ulap",
            tagalog = "ulap",
            english = "cloud",
            rootForm = "ulap",
            category = "Weather & Climate",
            ipaNotation = "ˈɁʊː.lap",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ip’s",
            tagalog = "ipis",
            english = "cockroach",
            rootForm = "ips",
            category = "Animals & Wildlife",
            ipaNotation = "Ɂi.ˈpəs",
            phoneticGlottal = true
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
            kasiguranin = "kudkuran korkoran",
            tagalog = "grater",
            english = "coconut",
            rootForm = "kudkuran",
            category = "Food & Dining",
            ipaNotation = "kɔr.ˈkɔː.ran",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "gata gata",
            tagalog = "milk",
            english = "coconut",
            rootForm = "gata",
            category = "Food & Dining",
            ipaNotation = "ga.ˈtaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "malamig malamig",
            tagalog = "(objects)",
            english = "cold",
            rootForm = "malamig",
            category = "General",
            ipaNotation = "ma.la.ˈmig"
        ),
        VocabularyEntity(
            kasiguranin = "maginaw mad’gnen",
            tagalog = "(weather)",
            english = "cold",
            rootForm = "maginaw",
            category = "General",
            ipaNotation = "ma.dəg.ˈnɛn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bangkay",
            tagalog = "bangkay",
            english = "corpse",
            rootForm = "bangkay",
            category = "Greetings & Essentials",
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
            ipaNotation = "bʊ.wa.ˈyaɁ",
            phoneticGlottal = true
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
            category = "Greetings & Essentials",
            ipaNotation = "kʊ.ˈlɔt"
        ),
        VocabularyEntity(
            kasiguranin = "madilim madikl’m",
            tagalog = "dim",
            english = "dark,",
            rootForm = "madilim",
            category = "Greetings & Essentials",
            ipaNotation = "ma.dik.ˈləm",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "or 24 hrs) araw (also, sun) aldew",
            tagalog = "(12",
            english = "day",
            rootForm = "or",
            category = "Nature & Environment",
            ipaNotation = "ˈɁal.dɛw",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "umaga",
            tagalog = "umaga",
            english = "daytime",
            rootForm = "umaga",
            category = "Numbers & Time",
            ipaNotation = "Ɂʊ.ˈmaː.ga",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "tul’ng",
            tagalog = "bingi",
            english = "deaf",
            rootForm = "tulng",
            category = "Greetings & Essentials",
            ipaNotation = "tʊ.ˈləŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "utang",
            tagalog = "utang",
            english = "debt",
            rootForm = "utang",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈtaŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "madisalad (hukay)",
            tagalog = "malalim",
            english = "deep",
            rootForm = "madisalad",
            category = "General",
            ipaNotation = "maː.di.ˈsaː.lad",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ogsa",
            tagalog = "usa",
            english = "deer",
            rootForm = "ogsa",
            category = "Animals & Wildlife",
            ipaNotation = "ˈɁɔg.saɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tibag",
            tagalog = "giba",
            english = "demolish",
            rootForm = "tibag",
            category = "Greetings & Essentials",
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
            kasiguranin = "mal’gga",
            tagalog = "marumi",
            english = "dirty",
            rootForm = "malgga",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ləg.ˈgaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "aso",
            tagalog = "aso",
            english = "dog",
            rootForm = "aso",
            category = "Animals & Wildlife",
            ipaNotation = "Ɂa.ˈsɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "pinto",
            tagalog = "pinto",
            english = "door",
            rootForm = "pinto",
            category = "Greetings & Essentials",
            ipaNotation = "ˈpin.tɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "padid’bba",
            tagalog = "pababa",
            english = "downward",
            rootForm = "padidbba",
            category = "Greetings & Essentials",
            ipaNotation = "pa.di.dəb.ˈbaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tagin’p",
            tagalog = "panaginip",
            english = "dream",
            rootForm = "taginp",
            category = "Greetings & Essentials",
            ipaNotation = "ta.ˈgiː.nəp",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "tuyo tuyo",
            tagalog = "(substance)",
            english = "dry",
            rootForm = "tuyo",
            category = "General",
            ipaNotation = "tʊ.ˈyɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "mapurol mangud’l",
            tagalog = "(knife)",
            english = "dull",
            rootForm = "mapurol",
            category = "General",
            ipaNotation = "ma.ŋʊ.ˈdəl",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "pipi bulol",
            tagalog = "(mute)",
            english = "dumb",
            rootForm = "pipi",
            category = "General",
            ipaNotation = "bʊ.ˈlɔl"
        ),
        VocabularyEntity(
            kasiguranin = "alikabok",
            tagalog = "alikabok",
            english = "dust",
            rootForm = "alikabok",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.li.ka.ˈbɔk",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "b’ng-b’ng",
            tagalog = "tainga",
            english = "ear",
            rootForm = "bngbng",
            category = "Body Parts & Health",
            ipaNotation = "bəŋ.ˈbəŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "lupa luta",
            tagalog = "(soil)",
            english = "earth",
            rootForm = "lupa",
            category = "Nature & Environment",
            ipaNotation = "lʊ.ˈtaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tule (solid)",
            tagalog = "tutuli",
            english = "earwax",
            rootForm = "tule",
            category = "Body Parts",
            ipaNotation = "tʊ.ˈleɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(freshwater)",
            tagalog = "igat",
            english = "eel",
            rootForm = "freshwater",
            category = "General"
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
            category = "Animals & Wildlife",
            ipaNotation = "ta.ˈluŋ"
        ),
        VocabularyEntity(
            kasiguranin = "walo",
            tagalog = "walo",
            english = "eight",
            rootForm = "walo",
            category = "Numbers & Time",
            ipaNotation = "wa.ˈlɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "siko",
            tagalog = "siko",
            english = "elbow",
            rootForm = "siko",
            category = "Body Parts & Health",
            ipaNotation = "si.ˈkɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "coal baga baga",
            tagalog = "hot",
            english = "ember,",
            rootForm = "coal",
            category = "Greetings & Essentials",
            ipaNotation = "baː.ˈga",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "‘ttog",
            tagalog = "latug",
            english = "erection",
            rootForm = "ttog",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂət.ˈtɔg",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "gibi",
            tagalog = "gabi",
            english = "evening",
            rootForm = "gibi",
            category = "Numbers & Time",
            ipaNotation = "gi.ˈbiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "‘ttay",
            tagalog = "dumi",
            english = "excrement",
            rootForm = "ttay",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂət.ˈtay",
            phoneticGlottal = true
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
            ipaNotation = "rʊ.ˈpaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "adayo",
            tagalog = "malayo",
            english = "far",
            rootForm = "adayo",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈdaː.jɔ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "mabilis",
            tagalog = "mabilis",
            english = "fast",
            rootForm = "mabilis",
            category = "Greetings & Essentials",
            ipaNotation = "ma.bi.ˈlis"
        ),
        VocabularyEntity(
            kasiguranin = "taba tabi",
            tagalog = "(substance)",
            english = "fat",
            rootForm = "taba",
            category = "General",
            ipaNotation = "ta.ˈbiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tatay",
            tagalog = "ama",
            english = "father",
            rootForm = "tatay",
            category = "Family & People",
            ipaNotation = "ˈtaː.taj",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "balahibo (fur,",
            tagalog = "(large)",
            english = "feather",
            rootForm = "balahibo",
            category = "Daily Activities"
        ),
        VocabularyEntity(
            kasiguranin = "kudal",
            tagalog = "bakod",
            english = "fence",
            rootForm = "kudal",
            category = "Greetings & Essentials",
            ipaNotation = "kʊ.ˈdal"
        ),
        VocabularyEntity(
            kasiguranin = "sabaddit",
            tagalog = "kaunti",
            english = "few",
            rootForm = "sabaddit",
            category = "Greetings & Essentials",
            ipaNotation = "ˈsaː.bad.dit",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "palekpek",
            tagalog = "palaypay",
            english = "fin",
            rootForm = "palekpek",
            category = "Greetings & Essentials",
            ipaNotation = "pa.lɛk.ˈpɛk"
        ),
        VocabularyEntity(
            kasiguranin = "guram’t",
            tagalog = "daliri",
            english = "finger",
            rootForm = "guramt",
            category = "Body Parts & Health",
            ipaNotation = "gʊ.ra.ˈmət",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kuko",
            tagalog = "kuko",
            english = "fingernail",
            rootForm = "kuko",
            category = "Body Parts & Health",
            ipaNotation = "kʊ.ˈkɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "apoy",
            tagalog = "apoy",
            english = "fire",
            rootForm = "apoy",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈpɔj",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "damo",
            tagalog = "una",
            english = "first",
            rootForm = "damo",
            category = "Greetings & Essentials",
            ipaNotation = "da.ˈmɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "damong anak",
            tagalog = "panganay",
            english = "firstborn",
            rootForm = "damong",
            category = "Greetings & Essentials",
            ipaNotation = "da.ˈmɔŋ Ɂa.ˈnak",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sida",
            tagalog = "isda",
            english = "fish",
            rootForm = "sida",
            category = "Animals & Wildlife",
            ipaNotation = "si.ˈdaɁ",
            phoneticGlottal = true
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
            kasiguranin = "‘ttot",
            tagalog = "utot",
            english = "flatulence",
            rootForm = "ttot",
            category = "Colors & Shapes",
            ipaNotation = "Ɂət.ˈtɔt",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "baha",
            tagalog = "baha",
            english = "flood",
            rootForm = "baha",
            category = "Greetings & Essentials",
            ipaNotation = "ba.ˈhaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bulaklak",
            tagalog = "bulaklak",
            english = "flower",
            rootForm = "bulaklak",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.lak.ˈlak"
        ),
        VocabularyEntity(
            kasiguranin = "insect) langaw (small) langaw",
            tagalog = "(the",
            english = "fly",
            rootForm = "insect",
            category = "Animals",
            ipaNotation = "la.ˈŋaw"
        ),
        VocabularyEntity(
            kasiguranin = "bula",
            tagalog = "bula",
            english = "foam",
            rootForm = "bula",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈlaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "alapok",
            tagalog = "ulop",
            english = "fog",
            rootForm = "alapok",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.la.ˈpɔk",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "b’sset",
            tagalog = "paa",
            english = "foot",
            rootForm = "bsset",
            category = "Body Parts & Health",
            ipaNotation = "bəs.ˈsɛt",
            phoneticGlottal = true
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
            category = "Greetings & Essentials",
            ipaNotation = "ma.bʊ.ˈjɔk"
        ),
        VocabularyEntity(
            kasiguranin = "appat",
            tagalog = "apat",
            english = "four",
            rootForm = "appat",
            category = "Numbers & Time",
            ipaNotation = "Ɂəp.ˈpat",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "masrob",
            tagalog = "mabango",
            english = "fragrant",
            rootForm = "masrob",
            category = "Animals & Wildlife",
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
            kasiguranin = "eating) busog bassog",
            tagalog = "(after",
            english = "full",
            rootForm = "eating",
            category = "General",
            ipaNotation = "bəs.ˈsɔg"
        ),
        VocabularyEntity(
            kasiguranin = "empty) puno putat",
            tagalog = "(not",
            english = "full",
            rootForm = "empty",
            category = "General",
            ipaNotation = "pʊ.ˈtat"
        ),
        VocabularyEntity(
            kasiguranin = "dutdut",
            tagalog = "balahibo",
            english = "fur",
            rootForm = "dutdut",
            category = "Greetings & Essentials",
            ipaNotation = "dʊt.ˈdʊt"
        ),
        VocabularyEntity(
            kasiguranin = "pagmulaan",
            tagalog = "halamanan",
            english = "garden",
            rootForm = "pagmulaan",
            category = "Greetings & Essentials",
            ipaNotation = "pag.mʊ.la.ˈɁan",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "asang",
            tagalog = "hasang",
            english = "gills",
            rootForm = "asang",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈsaŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "laya",
            tagalog = "luya",
            english = "ginger",
            rootForm = "laya",
            category = "Greetings & Essentials",
            ipaNotation = "la.ˈjaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "babae anak na babbi",
            tagalog = "batang",
            english = "girl",
            rootForm = "babae",
            category = "Family & People",
            ipaNotation = "Ɂa.ˈnak na bəb.ˈbiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "diyos",
            tagalog = "bathala",
            english = "god",
            rootForm = "diyos",
            category = "Greetings & Essentials",
            ipaNotation = "ˈʤɔs"
        ),
        VocabularyEntity(
            kasiguranin = "ginto",
            tagalog = "ginto",
            english = "gold",
            rootForm = "ginto",
            category = "Greetings & Essentials",
            ipaNotation = "gin.ˈtɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "maigi",
            tagalog = "mabuti",
            english = "good",
            rootForm = "maigi",
            category = "Emotions & Feelings",
            ipaNotation = "ma.ˈɁiː.giɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kaku na",
            tagalog = "paalam",
            english = "goodbye",
            rootForm = "kaku",
            category = "Emotions & Feelings",
            ipaNotation = "ˈkaː.ku ˈnaɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "lamon",
            tagalog = "damo",
            english = "grass",
            rootForm = "lamon",
            category = "Greetings & Essentials",
            ipaNotation = "la.ˈmɔn"
        ),
        VocabularyEntity(
            kasiguranin = "uban uban",
            tagalog = "hair",
            english = "gray",
            rootForm = "uban",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈban",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bituka",
            tagalog = "laman-loob",
            english = "guts",
            rootForm = "bituka",
            category = "Greetings & Essentials",
            ipaNotation = "bi.tʊ.ˈkaɁ",
            phoneticGlottal = true
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
            ipaNotation = "li.ˈmaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "maktog",
            tagalog = "matigas",
            english = "hard",
            rootForm = "maktog",
            category = "Greetings & Essentials",
            ipaNotation = "ˈmak.tɔg"
        ),
        VocabularyEntity(
            kasiguranin = "(he, she) siya",
            tagalog = "siya",
            english = "he",
            rootForm = "he",
            category = "General",
            ipaNotation = "si.ˈja"
        ),
        VocabularyEntity(
            kasiguranin = "ulo",
            tagalog = "ulo",
            english = "head",
            rootForm = "ulo",
            category = "Body Parts & Health",
            ipaNotation = "u.ˈlɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "matabi",
            tagalog = "malusog",
            english = "healthy",
            rootForm = "matabi",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ta.ˈbiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "puso",
            tagalog = "puso",
            english = "heart",
            rootForm = "puso",
            category = "Body Parts & Health",
            ipaNotation = "pʊ.ˈsɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "mad’gga",
            tagalog = "mabigat",
            english = "heavy",
            rootForm = "madgga",
            category = "Greetings & Essentials",
            ipaNotation = "ma.dəg.ˈgaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "dito",
            tagalog = "dito",
            english = "here",
            rootForm = "dito",
            category = "Greetings & Essentials",
            ipaNotation = "ˈdiː.tɔ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "taog tonok",
            tagalog = "tide",
            english = "high",
            rootForm = "taog",
            category = "Greetings & Essentials",
            ipaNotation = "ˈtɔː.nɔk",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "in",
            tagalog = "(esp.",
            english = "hole",
            rootForm = "in",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "mainit",
            tagalog = "mainit",
            english = "hot",
            rootForm = "mainit",
            category = "Weather & Climate",
            ipaNotation = "ma.ˈɁiː.nit",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "balay",
            tagalog = "bahay",
            english = "house",
            rootForm = "balay",
            category = "Greetings & Essentials",
            ipaNotation = "ba.ˈlaj"
        ),
        VocabularyEntity(
            kasiguranin = "paanu",
            tagalog = "paano",
            english = "how",
            rootForm = "paanu",
            category = "Greetings & Essentials",
            ipaNotation = "pa.ˈɁa.nʊ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ilan sangan",
            tagalog = "many?",
            english = "how",
            rootForm = "ilan",
            category = "Greetings & Essentials",
            ipaNotation = "ˈsaː.ŋan",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "magkano t’gsangan",
            tagalog = "much?",
            english = "how",
            rootForm = "magkano",
            category = "Greetings & Essentials",
            ipaNotation = "təg.ˈsaː.ŋan",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "al’p",
            tagalog = "gutom",
            english = "hungry",
            rootForm = "alp",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈləp",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(spouse) kabingang l’l’kke",
            tagalog = "asawa",
            english = "husband",
            rootForm = "spouse",
            category = "Family & People",
            ipaNotation = "ka.bi.ˈŋaŋ lə.lək.ˈkɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ako",
            tagalog = "ako",
            english = "I",
            rootForm = "ako",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈkɔ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "letratu",
            tagalog = "larawan",
            english = "image",
            rootForm = "letratu",
            category = "Greetings & Essentials",
            ipaNotation = "lɛ.ˈtraː.tuɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "bituka",
            tagalog = "bituka",
            english = "intestines",
            rootForm = "bituka",
            category = "Body Parts & Health",
            ipaNotation = "bi.tʊ.ˈkaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "isla",
            tagalog = "pulo",
            english = "island",
            rootForm = "isla",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂis.ˈlaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kat’l",
            tagalog = "kati",
            english = "itch",
            rootForm = "katl",
            category = "Greetings & Essentials",
            ipaNotation = "ka.ˈtəl",
            phoneticGlottal = true
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
            category = "Greetings & Essentials",
            ipaNotation = "Ɂəm.ˈmɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "‘ttod",
            tagalog = "tuhod",
            english = "knee",
            rootForm = "ttod",
            category = "Body Parts & Health",
            ipaNotation = "Ɂət.ˈtɔd",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "dimodyan",
            tagalog = "huli",
            english = "last",
            rootForm = "dimodyan",
            category = "Greetings & Essentials",
            ipaNotation = "di.ˈmɔ.ʤan"
        ),
        VocabularyEntity(
            kasiguranin = "dipos",
            tagalog = "bunso",
            english = "lastborn",
            rootForm = "dipos",
            category = "Greetings & Essentials",
            ipaNotation = "di.ˈpɔs"
        ),
        VocabularyEntity(
            kasiguranin = "mamaya",
            tagalog = "mamaya",
            english = "later",
            rootForm = "mamaya",
            category = "Greetings & Essentials",
            ipaNotation = "ˈmaː.ma.ja",
            phoneticVowelLength = true
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
            kasiguranin = "rain tulu turog",
            tagalog = "drip,",
            english = "leak,",
            rootForm = "rain",
            category = "Greetings & Essentials",
            ipaNotation = "tʊ.ˈrɔg"
        ),
        VocabularyEntity(
            kasiguranin = "kaliwa kariwe",
            tagalog = "(hand)",
            english = "left",
            rootForm = "kaliwa",
            category = "General",
            ipaNotation = "ka.ri.ˈwɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "binti",
            tagalog = "binti",
            english = "leg",
            rootForm = "binti",
            category = "Body Parts & Health",
            ipaNotation = "bin.ˈtiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kasinungalingan kabulean",
            tagalog = "(falsehood)",
            english = "lie",
            rootForm = "kasinungalingan",
            category = "General",
            ipaNotation = "kaː.bʊ. ˈlɛː. Ɂan",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "malagen",
            tagalog = "magaan",
            english = "light",
            rootForm = "malagen",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈlaː.gɛn",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kidlat",
            tagalog = "kidlat",
            english = "lightning",
            rootForm = "kidlat",
            category = "Weather & Climate",
            ipaNotation = "kid.ˈlat"
        ),
        VocabularyEntity(
            kasiguranin = "labi",
            tagalog = "labi",
            english = "lip",
            rootForm = "labi",
            category = "Body Parts & Health",
            ipaNotation = "ˈlaː.biɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "agtay",
            tagalog = "atay",
            english = "liver",
            rootForm = "agtay",
            category = "Body Parts & Health",
            ipaNotation = "Ɂag.ˈtaj",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "atakdug",
            tagalog = "mahaba",
            english = "long",
            rootForm = "atakdug",
            category = "Colors & Shapes",
            ipaNotation = "Ɂa.ˈtak.dʊg",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "malawa",
            tagalog = "maluwang",
            english = "loose",
            rootForm = "malawa",
            category = "Greetings & Essentials",
            ipaNotation = "ma.la.ˈwaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kuto",
            tagalog = "kuto",
            english = "louse",
            rootForm = "kuto",
            category = "Greetings & Essentials",
            ipaNotation = "kʊ.ˈtɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "gayuma amaya",
            tagalog = "charm",
            english = "love",
            rootForm = "gayuma",
            category = "Emotions & Feelings",
            ipaNotation = "Ɂa.ma.ˈyaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "baga",
            tagalog = "baga",
            english = "lungs",
            rootForm = "baga",
            category = "Body Parts & Health",
            ipaNotation = "ˈbaː.gaɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "lalaki lalakke",
            tagalog = "(male)",
            english = "man",
            rootForm = "lalaki",
            category = "Family & People",
            ipaNotation = "lə.lək.ˈkɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "gulpi",
            tagalog = "marami",
            english = "many",
            rootForm = "gulpi",
            category = "Family & People",
            ipaNotation = "gʊl.ˈpiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "floor) banig ab’k",
            tagalog = "(for",
            english = "mat",
            rootForm = "floor",
            category = "General",
            ipaNotation = "Ɂa.ˈbək",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "karne karne",
            tagalog = "(flesh)",
            english = "meat",
            rootForm = "karne",
            category = "Food & Drink",
            ipaNotation = "kar.ˈnɛ"
        ),
        VocabularyEntity(
            kasiguranin = "gamot",
            tagalog = "gamot",
            english = "medicine",
            rootForm = "gamot",
            category = "Greetings & Essentials",
            ipaNotation = "ga.ˈmɔt"
        ),
        VocabularyEntity(
            kasiguranin = "tunaw",
            tagalog = "tunaw",
            english = "melt",
            rootForm = "tunaw",
            category = "Greetings & Essentials",
            ipaNotation = "tʊ.ˈnaw"
        ),
        VocabularyEntity(
            kasiguranin = "dit’ngnga",
            tagalog = "gitna",
            english = "middle",
            rootForm = "ditngnga",
            category = "Greetings & Essentials",
            ipaNotation = "di.ˈtəŋ.ŋa",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "gatas",
            tagalog = "gatas",
            english = "milk",
            rootForm = "gatas",
            category = "Food & Dining",
            ipaNotation = "ˈgaː.tas",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "(also",
            tagalog = "buwan",
            english = "moon",
            rootForm = "also",
            category = "Weather & Climate"
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
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈmɔt"
        ),
        VocabularyEntity(
            kasiguranin = "nanay",
            tagalog = "nanay",
            english = "mother",
            rootForm = "nanay",
            category = "Family & People",
            ipaNotation = "ˈnaː.naj",
            phoneticVowelLength = true
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
            ipaNotation = "ŋʊ.ˈsɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "putik",
            tagalog = "putik",
            english = "mud",
            rootForm = "putik",
            category = "Greetings & Essentials",
            ipaNotation = "ˈpʊː.tik",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kuko",
            tagalog = "kuko",
            english = "nail",
            rootForm = "kuko",
            category = "Body Parts & Health",
            ipaNotation = "kʊ.ˈkɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ngaran",
            tagalog = "pangalan",
            english = "name",
            rootForm = "ngaran",
            category = "Greetings & Essentials",
            ipaNotation = "ŋa.ˈran"
        ),
        VocabularyEntity(
            kasiguranin = "t’ngng’d",
            tagalog = "batok",
            english = "nape",
            rootForm = "tngngd",
            category = "Greetings & Essentials",
            ipaNotation = "təŋ.ˈŋəd",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "makitid",
            tagalog = "makitid",
            english = "narrow",
            rootForm = "makitid",
            category = "Occupations & Tools",
            ipaNotation = "ma.ˈkiː.tid",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "pus’d",
            tagalog = "pusod",
            english = "navel",
            rootForm = "pusd",
            category = "Greetings & Essentials",
            ipaNotation = "pʊ.ˈsəd",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "adene",
            tagalog = "malapit",
            english = "near",
            rootForm = "adene",
            category = "Body Parts & Health",
            ipaNotation = "Ɂa.ˈdɛː.nɛ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "alleg",
            tagalog = "leeg",
            english = "neck",
            rootForm = "alleg",
            category = "Body Parts & Health",
            ipaNotation = "Ɂal.ˈlɛg",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kuwentas",
            tagalog = "kuwintas",
            english = "necklace",
            rootForm = "kuwentas",
            category = "Body Parts & Health",
            ipaNotation = "kʊ.ˈwɛn.tas"
        ),
        VocabularyEntity(
            kasiguranin = "digum",
            tagalog = "karayom",
            english = "needle",
            rootForm = "digum",
            category = "Greetings & Essentials",
            ipaNotation = "di.ˈgʊm"
        ),
        VocabularyEntity(
            kasiguranin = "bird’s) pugad lubun",
            tagalog = "(as",
            english = "nest",
            rootForm = "birds",
            category = "General",
            ipaNotation = "ˈlʊː.bʊn",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "lambat rambat",
            tagalog = "(fishing)",
            english = "net",
            rootForm = "lambat",
            category = "General",
            ipaNotation = "ram.ˈbat"
        ),
        VocabularyEntity(
            kasiguranin = "bigu",
            tagalog = "bago",
            english = "new",
            rootForm = "bigu",
            category = "Greetings & Essentials",
            ipaNotation = "bi.ˈgʊɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "gibi",
            tagalog = "gabi",
            english = "night",
            rootForm = "gibi",
            category = "Numbers & Time",
            ipaNotation = "gi.ˈbiɁ",
            phoneticGlottal = true
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
            kasiguranin = "wala",
            tagalog = "wala",
            english = "none",
            rootForm = "wala",
            category = "Numbers & Time",
            ipaNotation = "wa.ˈlaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "amihan amihan",
            tagalog = "wind",
            english = "northeast",
            rootForm = "amihan",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈmiː.han",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "addung",
            tagalog = "ilong",
            english = "nose",
            rootForm = "addung",
            category = "Body Parts & Health",
            ipaNotation = "Ɂad.ˈdʊŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "hindi",
            tagalog = "hindi",
            english = "not",
            rootForm = "hindi",
            category = "Greetings & Essentials",
            ipaNotation = "hin.ˈdiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ngayon",
            tagalog = "ngayon",
            english = "now",
            rootForm = "ngayon",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "pugita",
            tagalog = "pugita",
            english = "octopus",
            rootForm = "pugita",
            category = "Greetings & Essentials",
            ipaNotation = "pʊ.gi.ˈtaɁ",
            phoneticGlottal = true
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
            category = "Greetings & Essentials",
            ipaNotation = "ˈlʊː.maɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "mensan",
            tagalog = "minsan",
            english = "once",
            rootForm = "mensan",
            category = "Greetings & Essentials",
            ipaNotation = "ˈmɛn.san"
        ),
        VocabularyEntity(
            kasiguranin = "essa",
            tagalog = "isa",
            english = "one",
            rootForm = "essa",
            category = "Numbers & Time",
            ipaNotation = "Ɂɛs.ˈsaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "isang daan esang daan",
            tagalog = "hundred",
            english = "one",
            rootForm = "isang",
            category = "Numbers & Time",
            ipaNotation = "Ɂɛ.ˈsaŋ da.ˈɁan",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "isang libo esang libo",
            tagalog = "thousand",
            english = "one",
            rootForm = "isang",
            category = "Numbers & Time",
            ipaNotation = "Ɂɛ.ˈsaŋ ˈliː.bɔ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ulila",
            tagalog = "ulila",
            english = "orphan",
            rootForm = "ulila",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈliː.laɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "iba iba",
            tagalog = "different",
            english = "other,",
            rootForm = "iba",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈba",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bangka abeng",
            tagalog = "canoe",
            english = "outrigger",
            rootForm = "bangka",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈbɛŋ",
            phoneticGlottal = true
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
            kasiguranin = "(far) doon duun",
            tagalog = "there",
            english = "over",
            rootForm = "far",
            category = "General",
            ipaNotation = "dʊ.ˈɁʊn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sagwan sagwan",
            tagalog = "(canoe)",
            english = "paddle",
            rootForm = "sagwan",
            category = "General",
            ipaNotation = "sag. ˈwan"
        ),
        VocabularyEntity(
            kasiguranin = "saket",
            tagalog = "sakit",
            english = "pain",
            rootForm = "saket",
            category = "Greetings & Essentials",
            ipaNotation = "sa.ˈkɛt"
        ),
        VocabularyEntity(
            kasiguranin = "palad palad",
            tagalog = "(hand)",
            english = "palm",
            rootForm = "palad",
            category = "General",
            ipaNotation = "ˈpaː.lad",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ng lalaki buto",
            tagalog = "ari",
            english = "penis",
            rootForm = "ng",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈtɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(also human) tolay",
            tagalog = "tao",
            english = "person",
            rootForm = "also",
            category = "Family & People",
            ipaNotation = "ˈtɔː.laj",
            phoneticVowelLength = true
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
            category = "Greetings & Essentials",
            ipaNotation = "pʊ.ŋa.ˈnan"
        ),
        VocabularyEntity(
            kasiguranin = "halaman",
            tagalog = "halaman",
            english = "plant",
            rootForm = "halaman",
            category = "Animals & Wildlife",
            ipaNotation = "ha.ˈlaː.man",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "hand",
            tagalog = "with",
            english = "press",
            rootForm = "hand",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "tusok tusok",
            tagalog = "pierce",
            english = "prick,",
            rootForm = "tusok",
            category = "Greetings & Essentials",
            ipaNotation = "ˈtʊː.sɔk",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "nana",
            tagalog = "nana",
            english = "pus",
            rootForm = "nana",
            category = "Greetings & Essentials",
            ipaNotation = "na.ˈnaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kuy’ng",
            tagalog = "daga",
            english = "rat",
            rootForm = "kuyng",
            category = "Animals & Wildlife",
            ipaNotation = "kʊ.ˈjəŋ",
            phoneticGlottal = true
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
            category = "General",
            ipaNotation = "ˈtaː.maɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kanan kanan",
            tagalog = "(hand)",
            english = "right",
            rootForm = "kanan",
            category = "General",
            ipaNotation = "ˈkaː.nan",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "banlaw",
            tagalog = "banlaw",
            english = "rinse",
            rootForm = "banlaw",
            category = "Greetings & Essentials",
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
            category = "Greetings & Essentials",
            ipaNotation = "da.ˈlan"
        ),
        VocabularyEntity(
            kasiguranin = "boulder) bato bato",
            tagalog = "(or",
            english = "rock",
            rootForm = "boulder",
            category = "General",
            ipaNotation = "ba.ˈtɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "at’p",
            tagalog = "bubong",
            english = "roof",
            rootForm = "atp",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈtəp",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ugat",
            tagalog = "ugat",
            english = "root",
            rootForm = "ugat",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈgat",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "lubid",
            tagalog = "lubid",
            english = "rope",
            rootForm = "lubid",
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈbid"
        ),
        VocabularyEntity(
            kasiguranin = "fruit) sira buyok",
            tagalog = "(as",
            english = "rotten",
            rootForm = "fruit",
            category = "Numbers",
            ipaNotation = "bʊ.ˈjɔk"
        ),
        VocabularyEntity(
            kasiguranin = "bulok gabuk",
            tagalog = "(log)",
            english = "rotten",
            rootForm = "bulok",
            category = "Numbers",
            ipaNotation = "ga.ˈbʊk"
        ),
        VocabularyEntity(
            kasiguranin = "masapg’t",
            tagalog = "magaspang",
            english = "rough",
            rootForm = "masapgt",
            category = "Greetings & Essentials",
            ipaNotation = "ma. ˈsap.gət",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "asen",
            tagalog = "asin",
            english = "salt",
            rootForm = "asen",
            category = "Food & Dining",
            ipaNotation = "Ɂa.ˈsɛn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "maasen",
            tagalog = "maalat",
            english = "salty",
            rootForm = "maasen",
            category = "Food & Dining",
            ipaNotation = "maː.ˈsɛn",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "pareho",
            tagalog = "tulad",
            english = "same",
            rootForm = "pareho",
            category = "Greetings & Essentials",
            ipaNotation = "pa.ˈrɛː.hɔ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "baybay",
            tagalog = "buhangin",
            english = "sand",
            rootForm = "baybay",
            category = "Greetings & Essentials",
            ipaNotation = "baj.ˈbaj"
        ),
        VocabularyEntity(
            kasiguranin = "dagat diget",
            tagalog = "(ocean)",
            english = "sea",
            rootForm = "dagat",
            category = "Nature & Environment",
            ipaNotation = "di.ˈgɛt"
        ),
        VocabularyEntity(
            kasiguranin = "ikaduwa",
            tagalog = "ikalawa",
            english = "second",
            rootForm = "ikaduwa",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ka.ˈdʊː.waɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "(also, bone) buk’l",
            tagalog = "buto",
            english = "seed",
            rootForm = "also",
            category = "General",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kaguman",
            tagalog = "katulong",
            english = "servant",
            rootForm = "kaguman",
            category = "Animals & Wildlife",
            ipaNotation = "ˈkaː.gʊ.man",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "pitu",
            tagalog = "pito",
            english = "seven",
            rootForm = "pitu",
            category = "Numbers & Time",
            ipaNotation = "pi.ˈtʊɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "aneno",
            tagalog = "anino",
            english = "shadow",
            rootForm = "aneno",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈnɛː.nɔɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "madid’bbaw (hukay)",
            tagalog = "mababaw",
            english = "shallow",
            rootForm = "madidbbaw",
            category = "General",
            ipaNotation = "maː.di.dəb.ˈbaw",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "iyo",
            tagalog = "pating",
            english = "shark",
            rootForm = "iyo",
            category = "Animals & Wildlife",
            ipaNotation = "Ɂi.ˈyɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "matalim matad’m",
            tagalog = "(knife)",
            english = "sharp",
            rootForm = "matalim",
            category = "General",
            ipaNotation = "ma.ta.ˈdəm",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "digdig ng diget",
            tagalog = "tabing-dagat",
            english = "shore",
            rootForm = "digdig",
            category = "Greetings & Essentials",
            ipaNotation = "dig.ˈdig ng di.ˈgɛt"
        ),
        VocabularyEntity(
            kasiguranin = "baddit",
            tagalog = "maliit",
            english = "short",
            rootForm = "baddit",
            category = "Colors & Shapes",
            ipaNotation = "bad.ˈdit"
        ),
        VocabularyEntity(
            kasiguranin = "abaga",
            tagalog = "balikat",
            english = "shoulder",
            rootForm = "abaga",
            category = "Body Parts & Health",
            ipaNotation = "Ɂa.ba.ˈgaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "udeng (river)",
            tagalog = "hipon",
            english = "shrimp",
            rootForm = "udeng",
            category = "General",
            ipaNotation = "Ɂʊ.ˈdɛŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kapatid kapatiyaka",
            tagalog = "(m/f)",
            english = "sibling",
            rootForm = "kapatid",
            category = "Family & People",
            ipaNotation = "ka.pa.ʧa.ˈkaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "pase",
            tagalog = "paso",
            english = "singe",
            rootForm = "pase",
            category = "Greetings & Essentials",
            ipaNotation = "pa.ˈsɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "hipag",
            tagalog = "hipag",
            english = "sister-in-law",
            rootForm = "hipag",
            category = "Family & People",
            ipaNotation = "ˈhiː.pag",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "‘nn’m",
            tagalog = "anim",
            english = "six",
            rootForm = "nnm",
            category = "Numbers & Time",
            ipaNotation = "Ɂən.ˈnəm",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "balat kulet",
            tagalog = "(person)",
            english = "skin",
            rootForm = "balat",
            category = "General",
            ipaNotation = "kʊ.ˈlet"
        ),
        VocabularyEntity(
            kasiguranin = "bungo",
            tagalog = "bungo",
            english = "skull",
            rootForm = "bungo",
            category = "Body Parts & Health",
            ipaNotation = "bʊ.ˈŋɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "alila",
            tagalog = "alipin",
            english = "slave",
            rootForm = "alila",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈliː.laɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "magtongka",
            tagalog = "inaantok",
            english = "sleepy",
            rootForm = "magtongka",
            category = "Greetings & Essentials",
            ipaNotation = "mag.ˈtɔŋ.kaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "mabagal",
            tagalog = "mabagal",
            english = "slow",
            rootForm = "mabagal",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈbaː.gal",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "baddit",
            tagalog = "maliit",
            english = "small",
            rootForm = "baddit",
            category = "Greetings & Essentials",
            ipaNotation = "bad.ˈdit"
        ),
        VocabularyEntity(
            kasiguranin = "asok",
            tagalog = "usok",
            english = "smoke",
            rootForm = "asok",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈsɔk",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "makinis",
            tagalog = "makinis",
            english = "smooth",
            rootForm = "makinis",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈkiː.nis",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ulag",
            tagalog = "ahas",
            english = "snake",
            rootForm = "ulag",
            category = "Animals & Wildlife",
            ipaNotation = "Ɂʊ.ˈlag",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "abben",
            tagalog = "bahing",
            english = "sneeze",
            rootForm = "abben",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂəb.ˈbɛn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "malammen",
            tagalog = "malambot",
            english = "soft",
            rootForm = "malammen",
            category = "Greetings & Essentials",
            ipaNotation = "ma.lam.ˈmɛn"
        ),
        VocabularyEntity(
            kasiguranin = "talampakan",
            tagalog = "talampakan",
            english = "sole",
            rootForm = "talampakan",
            category = "Body Parts & Health",
            ipaNotation = "ta.lam.ˈpaː.kan",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "sangan",
            tagalog = "ilan",
            english = "some",
            rootForm = "sangan",
            category = "Greetings & Essentials",
            ipaNotation = "ˈsaː.ŋan",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kaluluwa",
            tagalog = "kaluluwa",
            english = "soul",
            rootForm = "kaluluwa",
            category = "Greetings & Essentials",
            ipaNotation = "ˈkaː.lu.lu.wa",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "malas’m",
            tagalog = "maasim",
            english = "sour",
            rootForm = "malasm",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈlaː.səm",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "habagat abagat",
            tagalog = "wind",
            english = "southwest",
            rootForm = "habagat",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ba.ˈgat",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sibat",
            tagalog = "sibat",
            english = "spear",
            rootForm = "sibat",
            category = "Body Parts & Health",
            ipaNotation = "si.ˈbat"
        ),
        VocabularyEntity(
            kasiguranin = "bats paniki kulapnet (bahay)",
            tagalog = "of",
            english = "species",
            rootForm = "bats",
            category = "General",
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
            english = "spittle(saliva)",
            rootForm = "laway",
            category = "General",
            ipaNotation = "ˈlaː.waj",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "pusit",
            tagalog = "pusit",
            english = "squid",
            rootForm = "pusit",
            category = "Greetings & Essentials",
            ipaNotation = "pʊ.ˈsit"
        ),
        VocabularyEntity(
            kasiguranin = "agdenan",
            tagalog = "hagdan",
            english = "stairs",
            rootForm = "agdenan",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂag.ˈdɛː.nan",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "stature tindig takn’g",
            tagalog = "up,",
            english = "stand",
            rootForm = "stature",
            category = "Greetings & Essentials",
            ipaNotation = "tak.ˈnəg",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bitoin",
            tagalog = "bituin",
            english = "star",
            rootForm = "bitoin",
            category = "Weather & Climate",
            ipaNotation = "bi.ˈtɔː.Ɂin",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "wood) patpat patpat",
            tagalog = "(of",
            english = "stick",
            rootForm = "wood",
            category = "General",
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
            category = "Numbers & Time",
            ipaNotation = "ba.ˈtɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kamalig kamalig",
            tagalog = "(food)",
            english = "storehouse",
            rootForm = "kamalig",
            category = "General",
            ipaNotation = "ka.ˈmaː.lig",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "matuwid diretso",
            tagalog = "tuwid,",
            english = "straight",
            rootForm = "matuwid",
            category = "Greetings & Essentials",
            ipaNotation = "di.ˈrɛt.sɔ"
        ),
        VocabularyEntity(
            kasiguranin = "annat",
            tagalog = "unat",
            english = "stretch",
            rootForm = "annat",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂən.ˈnat",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "mabigs’k",
            tagalog = "malakas",
            english = "strong",
            rootForm = "mabigsk",
            category = "Greetings & Essentials",
            ipaNotation = "ma.big.ˈsək",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "s’ps’p",
            tagalog = "sipsip",
            english = "suck",
            rootForm = "spsp",
            category = "Greetings & Essentials",
            ipaNotation = "səp.ˈsəp",
            phoneticGlottal = true
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
            kasiguranin = "(also, day) aldew",
            tagalog = "araw",
            english = "sun",
            rootForm = "also",
            category = "Nature & Environment",
            ipaNotation = "ˈɁal.dɛw",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "matam-is",
            tagalog = "matamis",
            english = "sweet",
            rootForm = "matamis",
            category = "Food & Dining",
            ipaNotation = "ma.ˈtam.Ɂis",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "baga",
            tagalog = "maga",
            english = "swollen",
            rootForm = "baga",
            category = "Greetings & Essentials",
            ipaNotation = "ba.ˈgaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ipos",
            tagalog = "buntot",
            english = "tail",
            rootForm = "ipos",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈpɔs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "malangkaw",
            tagalog = "matangkad",
            english = "tall",
            rootForm = "malangkaw",
            category = "Greetings & Essentials",
            ipaNotation = "ma.laŋ.ˈkaw"
        ),
        VocabularyEntity(
            kasiguranin = "crying) luha luha",
            tagalog = "(from",
            english = "tear",
            rootForm = "crying",
            category = "Body Parts",
            ipaNotation = "ˈlʊː.haɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "sapulo",
            tagalog = "sampu",
            english = "ten",
            rootForm = "sapulo",
            category = "Numbers & Time",
            ipaNotation = "ˈsaː.pʊ.lɔɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "anay",
            tagalog = "anay",
            english = "termites",
            rootForm = "anay",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈnaj",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bungaw",
            tagalog = "bayag",
            english = "testicle",
            rootForm = "bungaw",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈŋaw"
        ),
        VocabularyEntity(
            kasiguranin = "salamat salamat",
            tagalog = "you",
            english = "thank",
            rootForm = "salamat",
            category = "Greetings & Essentials",
            ipaNotation = "sa.ˈlaː.mat",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "iyon iyon",
            tagalog = "(far)",
            english = "that",
            rootForm = "iyon",
            category = "General",
            ipaNotation = "Ɂi.ˈjɔn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "iyan iyan",
            tagalog = "(near)",
            english = "that",
            rootForm = "iyan",
            category = "General",
            ipaNotation = "Ɂi.ˈjan",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "diyan diyan",
            tagalog = "(near)",
            english = "there",
            rootForm = "diyan",
            category = "General",
            ipaNotation = "ʤan"
        ),
        VocabularyEntity(
            kasiguranin = "sila",
            tagalog = "sila",
            english = "they",
            rootForm = "sila",
            category = "Greetings & Essentials",
            ipaNotation = "si.ˈla"
        ),
        VocabularyEntity(
            kasiguranin = "mabag’l",
            tagalog = "makapal",
            english = "thick",
            rootForm = "mabagl",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ba.ˈgəl",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "lape",
            tagalog = "hita",
            english = "thigh",
            rootForm = "lape",
            category = "Body Parts & Health",
            ipaNotation = "la.ˈpɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "malapes",
            tagalog = "manipis",
            english = "thin",
            rootForm = "malapes",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈlaː.pɛs",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "payat maniwang",
            tagalog = "(human)",
            english = "thin",
            rootForm = "payat",
            category = "General",
            ipaNotation = "ma.ni.ˈwaŋ"
        ),
        VocabularyEntity(
            kasiguranin = "ikatallo",
            tagalog = "ikatlo",
            english = "third",
            rootForm = "ikatallo",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ka.təl.ˈloɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "uwaw",
            tagalog = "uhaw",
            english = "thirsty",
            rootForm = "uwaw",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈwaw",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ito",
            tagalog = "ito",
            english = "this",
            rootForm = "ito",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈtɔ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(also,",
            tagalog = "tinik",
            english = "thorn",
            rootForm = "also",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "ikaw",
            tagalog = "ika",
            english = "thou/you",
            rootForm = "ikaw",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈkaw",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "t’llo",
            tagalog = "tatlo",
            english = "three",
            rootForm = "tllo",
            category = "Numbers & Time",
            ipaNotation = "təl.ˈlɔɁ",
            phoneticGlottal = true
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
            kasiguranin = "kaddur",
            tagalog = "kulog",
            english = "thunder",
            rootForm = "kaddur",
            category = "Weather & Climate",
            ipaNotation = "kad.ˈdʊr"
        ),
        VocabularyEntity(
            kasiguranin = "masikip",
            tagalog = "masikip",
            english = "tight",
            rootForm = "masikip",
            category = "Greetings & Essentials",
            ipaNotation = "ma.si.ˈkip"
        ),
        VocabularyEntity(
            kasiguranin = "tanong pakeligip",
            tagalog = "ask",
            english = "to",
            rootForm = "tanong",
            category = "Greetings & Essentials",
            ipaNotation = "pa.ˈkɛː.li.gip",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "gising lukag",
            tagalog = "awake",
            english = "to",
            rootForm = "gising",
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈkag"
        ),
        VocabularyEntity(
            kasiguranin = "angry galit kanga",
            tagalog = "be",
            english = "to",
            rootForm = "angry",
            category = "Greetings & Essentials",
            ipaNotation = "ˈka.ŋa"
        ),
        VocabularyEntity(
            kasiguranin = "(child) anak enak",
            tagalog = "bear",
            english = "to",
            rootForm = "child",
            category = "General",
            ipaNotation = "ˈɁɛ.nak",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(strike) palo yabat",
            tagalog = "beat",
            english = "to",
            rootForm = "strike",
            category = "General",
            ipaNotation = "ˈjaː.bat",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "dighay t’ggeb",
            tagalog = "belch",
            english = "to",
            rootForm = "dighay",
            category = "Greetings & Essentials",
            ipaNotation = "təg.ˈgɛb",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kagat k’tteb",
            tagalog = "bite",
            english = "to",
            rootForm = "kagat",
            category = "Greetings & Essentials",
            ipaNotation = "kət.ˈtɛb",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(wind) ihip sabyog",
            tagalog = "blow",
            english = "to",
            rootForm = "wind",
            category = "General",
            ipaNotation = "sab.ˈjɔg"
        ),
        VocabularyEntity(
            kasiguranin = "(intrans.) kulo labbut",
            tagalog = "boil",
            english = "to",
            rootForm = "intrans",
            category = "General",
            ipaNotation = "ləb.ˈbʊt"
        ),
        VocabularyEntity(
            kasiguranin = "(as stick) bali putel",
            tagalog = "break",
            english = "to",
            rootForm = "as",
            category = "General",
            ipaNotation = "pʊ.ˈtɛl"
        ),
        VocabularyEntity(
            kasiguranin = "hinga ang’s",
            tagalog = "breathe",
            english = "to",
            rootForm = "hinga",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈŋəs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "dala tawed",
            tagalog = "bring",
            english = "to",
            rootForm = "dala",
            category = "Greetings & Essentials",
            ipaNotation = "ta.ˈwɛd"
        ),
        VocabularyEntity(
            kasiguranin = "(by itself) sunog tutod",
            tagalog = "burn",
            english = "to",
            rootForm = "by",
            category = "General",
            ipaNotation = "tʊ.ˈtɔd"
        ),
        VocabularyEntity(
            kasiguranin = "baon kotkot",
            tagalog = "bury",
            english = "to",
            rootForm = "baon",
            category = "Greetings & Essentials",
            ipaNotation = "ˈkɔt.kɔt"
        ),
        VocabularyEntity(
            kasiguranin = "(the dead) libing l’bb’ng",
            tagalog = "bury",
            english = "to",
            rootForm = "the",
            category = "General",
            ipaNotation = "ləb.ˈbəng",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bili bugtong",
            tagalog = "buy",
            english = "to",
            rootForm = "bili",
            category = "Greetings & Essentials",
            ipaNotation = "bʊg.ˈtɔŋ"
        ),
        VocabularyEntity(
            kasiguranin = "tawag dulaw",
            tagalog = "call",
            english = "to",
            rootForm = "tawag",
            category = "Greetings & Essentials",
            ipaNotation = "dʊ.ˈlaw"
        ),
        VocabularyEntity(
            kasiguranin = "dala betbet",
            tagalog = "carry",
            english = "to",
            rootForm = "dala",
            category = "Greetings & Essentials",
            ipaNotation = "ˈbɛt.bɛt"
        ),
        VocabularyEntity(
            kasiguranin = "pili pile",
            tagalog = "choose",
            english = "to",
            rootForm = "pili",
            category = "Greetings & Essentials",
            ipaNotation = "pi.ˈlɛɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "linis remp’s",
            tagalog = "clean",
            english = "to",
            rootForm = "linis",
            category = "Greetings & Essentials",
            ipaNotation = "rɛm.ˈpəs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "dating dem’t",
            tagalog = "come",
            english = "to",
            rootForm = "dating",
            category = "Greetings & Essentials",
            ipaNotation = "ˈdeː.mət",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ubo ikk’r",
            tagalog = "cough",
            english = "to",
            rootForm = "ubo",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂik.ˈkər",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bilang bilang",
            tagalog = "count",
            english = "to",
            rootForm = "bilang",
            category = "Greetings & Essentials",
            ipaNotation = "ˈbiː.laŋ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "putol k’ttol",
            tagalog = "cut",
            english = "to",
            rootForm = "putol",
            category = "Greetings & Essentials",
            ipaNotation = "kət.ˈtɔl",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sayaw sayaw",
            tagalog = "dance",
            english = "to",
            rootForm = "sayaw",
            category = "Greetings & Essentials",
            ipaNotation = "sa.ˈjaw"
        ),
        VocabularyEntity(
            kasiguranin = "dumi, bawas, tae ‘ttay",
            tagalog = "defecate",
            english = "to",
            rootForm = "dumi",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂət.ˈtaj",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "nais gustu",
            tagalog = "desire",
            english = "to",
            rootForm = "nais",
            category = "Greetings & Essentials",
            ipaNotation = "gʊs.ˈtʊ"
        ),
        VocabularyEntity(
            kasiguranin = "patay patay",
            tagalog = "die",
            english = "to",
            rootForm = "patay",
            category = "Greetings & Essentials",
            ipaNotation = "pa.ˈtaj"
        ),
        VocabularyEntity(
            kasiguranin = "hukay kotkot",
            tagalog = "dig",
            english = "to",
            rootForm = "hukay",
            category = "Greetings & Essentials",
            ipaNotation = "ˈkɔt.kɔt"
        ),
        VocabularyEntity(
            kasiguranin = "gawa gamet",
            tagalog = "do",
            english = "to",
            rootForm = "gawa",
            category = "Greetings & Essentials",
            ipaNotation = "ga.ˈmɛt"
        ),
        VocabularyEntity(
            kasiguranin = "kaladkad godgod",
            tagalog = "drag",
            english = "to",
            rootForm = "kaladkad",
            category = "Greetings & Essentials",
            ipaNotation = "ˈgɔd.gɔd"
        ),
        VocabularyEntity(
            kasiguranin = "inom inom",
            tagalog = "drink",
            english = "to",
            rootForm = "inom",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈnɔm",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "lunod lim’s",
            tagalog = "drown",
            english = "to",
            rootForm = "lunod",
            category = "Greetings & Essentials",
            ipaNotation = "li.ˈməs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kain kuman",
            tagalog = "eat",
            english = "to",
            rootForm = "kain",
            category = "Greetings & Essentials",
            ipaNotation = "kʊ.ˈman"
        ),
        VocabularyEntity(
            kasiguranin = "(drop) hulog tapduk",
            tagalog = "fall",
            english = "to",
            rootForm = "drop",
            category = "General",
            ipaNotation = "tap.ˈdʊk"
        ),
        VocabularyEntity(
            kasiguranin = "takot anteng",
            tagalog = "fear",
            english = "to",
            rootForm = "takot",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂan.ˈtɛŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "laban laban",
            tagalog = "fight",
            english = "to",
            rootForm = "laban",
            category = "Greetings & Essentials",
            ipaNotation = "ˈlaː.ban",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "hanap aryok",
            tagalog = "find",
            english = "to",
            rootForm = "hanap",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂar.ˈjɔk",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "lutang latak",
            tagalog = "float",
            english = "to",
            rootForm = "lutang",
            category = "Greetings & Essentials",
            ipaNotation = "la.ˈtak"
        ),
        VocabularyEntity(
            kasiguranin = "agos agus",
            tagalog = "flow",
            english = "to",
            rootForm = "agos",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁaː.gʊs",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "lipad egb’r",
            tagalog = "fly",
            english = "to",
            rootForm = "lipad",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁɛg.bər",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "limot lipon",
            tagalog = "forget",
            english = "to",
            rootForm = "limot",
            category = "Greetings & Essentials",
            ipaNotation = "li.ˈpɔn"
        ),
        VocabularyEntity(
            kasiguranin = "bigay att’d",
            tagalog = "give",
            english = "to",
            rootForm = "bigay",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈtəd",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "punta angay",
            tagalog = "go",
            english = "to",
            rootForm = "punta",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈŋaj",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "down baba ogsad",
            tagalog = "go",
            english = "to",
            rootForm = "down",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂɔg.ˈsad",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "in pasok s’dd’p",
            tagalog = "go",
            english = "to",
            rootForm = "in",
            category = "Greetings & Essentials",
            ipaNotation = "səd.ˈdəp",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "out labas luwas",
            tagalog = "go",
            english = "to",
            rootForm = "out",
            category = "Greetings & Essentials",
            ipaNotation = "lʊ.ˈwas"
        ),
        VocabularyEntity(
            kasiguranin = "up akyat sangkay (bahay)",
            tagalog = "go",
            english = "to",
            rootForm = "up",
            category = "General",
            ipaNotation = "saŋ.ˈkaj"
        ),
        VocabularyEntity(
            kasiguranin = "on, hook",
            tagalog = "hang",
            english = "to",
            rootForm = "on",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "kinig saneg",
            tagalog = "hear",
            english = "to",
            rootForm = "kinig",
            category = "Greetings & Essentials",
            ipaNotation = "ˈsaː.nɛg",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "tama tama",
            tagalog = "hit",
            english = "to",
            rootForm = "tama",
            category = "Greetings & Essentials",
            ipaNotation = "ˈtaː.maɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "(in hand) hawak kabit",
            tagalog = "hold",
            english = "to",
            rootForm = "in",
            category = "General",
            ipaNotation = "ka.ˈbit"
        ),
        VocabularyEntity(
            kasiguranin = "(game) aso aso",
            tagalog = "hunt",
            english = "to",
            rootForm = "game",
            category = "General",
            ipaNotation = "ˈɁaː.so",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "(esp. up) talon lukso",
            tagalog = "jump",
            english = "to",
            rootForm = "esp",
            category = "General",
            ipaNotation = "lʊk.ˈsɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "patay buno",
            tagalog = "kill",
            english = "to",
            rootForm = "patay",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈnɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(facts) alam tukoy",
            tagalog = "know",
            english = "to",
            rootForm = "facts",
            category = "General",
            ipaNotation = "tʊ.ˈkɔj"
        ),
        VocabularyEntity(
            kasiguranin = "tawa tawa",
            tagalog = "laugh",
            english = "to",
            rootForm = "tawa",
            category = "Greetings & Essentials",
            ipaNotation = "ˈtaː.wa",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "(on side) higa oled",
            tagalog = "lie",
            english = "to",
            rootForm = "on",
            category = "General",
            ipaNotation = "ˈɁɔː.lɛd",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "buhay buhay",
            tagalog = "live",
            english = "to",
            rootForm = "buhay",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈhaj"
        ),
        VocabularyEntity(
            kasiguranin = "tingin ileng",
            tagalog = "look",
            english = "to",
            rootForm = "tingin",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈlɛŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ibig gusto",
            tagalog = "love",
            english = "to",
            rootForm = "ibig",
            category = "Greetings & Essentials",
            ipaNotation = "gʊs.ˈtʊ"
        ),
        VocabularyEntity(
            kasiguranin = "ungol ungol",
            tagalog = "moan",
            english = "to",
            rootForm = "ungol",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁʊː.ŋɔl",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "bukas bukas",
            tagalog = "open",
            english = "to",
            rootForm = "bukas",
            category = "Greetings & Essentials",
            ipaNotation = "bʊ.ˈkas"
        ),
        VocabularyEntity(
            kasiguranin = "laro usek",
            tagalog = "play",
            english = "to",
            rootForm = "laro",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈsɛk",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bayo bayo",
            tagalog = "pound",
            english = "to",
            rootForm = "bayo",
            category = "Greetings & Essentials",
            ipaNotation = "ba.ˈjɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "hila botbot (pataas)",
            tagalog = "pull",
            english = "to",
            rootForm = "hila",
            category = "General",
            ipaNotation = "bɔt.ˈbɔt"
        ),
        VocabularyEntity(
            kasiguranin = "tulak toglad",
            tagalog = "push",
            english = "to",
            rootForm = "tulak",
            category = "Greetings & Essentials",
            ipaNotation = "tɔg.ˈlad"
        ),
        VocabularyEntity(
            kasiguranin = "lagay datton",
            tagalog = "put",
            english = "to",
            rootForm = "lagay",
            category = "Greetings & Essentials",
            ipaNotation = "dət.ˈtɔn"
        ),
        VocabularyEntity(
            kasiguranin = "away dima",
            tagalog = "quarrel",
            english = "to",
            rootForm = "away",
            category = "Greetings & Essentials",
            ipaNotation = "di.ˈmaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ulan uden",
            tagalog = "rain",
            english = "to",
            rootForm = "ulan",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈdɛn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "balik sole",
            tagalog = "return",
            english = "to",
            rootForm = "balik",
            category = "Greetings & Essentials",
            ipaNotation = "ˈsɔː.lɛɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kuskos kuskus",
            tagalog = "rub",
            english = "to",
            rootForm = "kuskos",
            category = "Greetings & Essentials",
            ipaNotation = "kʊs.ˈkʊs"
        ),
        VocabularyEntity(
            kasiguranin = "takbo ginan",
            tagalog = "run",
            english = "to",
            rootForm = "takbo",
            category = "Greetings & Essentials",
            ipaNotation = "gi.ˈnan"
        ),
        VocabularyEntity(
            kasiguranin = "sabi kagi",
            tagalog = "say",
            english = "to",
            rootForm = "sabi",
            category = "Greetings & Essentials",
            ipaNotation = "ka.ˈgiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(itch) kamot gusgus",
            tagalog = "scratch",
            english = "to",
            rootForm = "itch",
            category = "General",
            ipaNotation = "gʊs.ˈgʊs"
        ),
        VocabularyEntity(
            kasiguranin = "kita keta",
            tagalog = "see",
            english = "to",
            rootForm = "kita",
            category = "Greetings & Essentials",
            ipaNotation = "ˈkeː.ta",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "bili ibugtong",
            tagalog = "sell",
            english = "to",
            rootForm = "bili",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.bʊg.ˈtɔŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tahi darop",
            tagalog = "sew",
            english = "to",
            rootForm = "tahi",
            category = "Greetings & Essentials",
            ipaNotation = "da.ˈrɔp"
        ),
        VocabularyEntity(
            kasiguranin = "sigaw karyaw",
            tagalog = "shout",
            english = "to",
            rootForm = "sigaw",
            category = "Greetings & Essentials",
            ipaNotation = "kar.ˈyaw"
        ),
        VocabularyEntity(
            kasiguranin = "pakita ipeta",
            tagalog = "show",
            english = "to",
            rootForm = "pakita",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈpɛː.ta",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "ambon puropor",
            tagalog = "shower",
            english = "to",
            rootForm = "ambon",
            category = "Greetings & Essentials",
            ipaNotation = "pʊ.ˈrɔː.pɔr",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "awit, kanta kanta",
            tagalog = "sing",
            english = "to",
            rootForm = "awit",
            category = "Greetings & Essentials",
            ipaNotation = "ˈkan.taɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(intrans.) lubog sar’m (sun)",
            tagalog = "sink",
            english = "to",
            rootForm = "intrans",
            category = "General",
            ipaNotation = "ˈsaː.rəm",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "upo etnod",
            tagalog = "sit",
            english = "to",
            rootForm = "upo",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂɛt.ˈnɔd",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tulog tidug",
            tagalog = "sleep",
            english = "to",
            rootForm = "tulog",
            category = "Greetings & Essentials",
            ipaNotation = "ti.ˈdʊg"
        ),
        VocabularyEntity(
            kasiguranin = "amoy arob",
            tagalog = "smell",
            english = "to",
            rootForm = "amoy",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈrɔb",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "salita kagi",
            tagalog = "speak",
            english = "to",
            rootForm = "salita",
            category = "Greetings & Essentials",
            ipaNotation = "ka.ˈgiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "dura loktab",
            tagalog = "spit",
            english = "to",
            rootForm = "dura",
            category = "Greetings & Essentials",
            ipaNotation = "lɔk.ˈtab"
        ),
        VocabularyEntity(
            kasiguranin = "hati p’kka",
            tagalog = "split",
            english = "to",
            rootForm = "hati",
            category = "Greetings & Essentials",
            ipaNotation = "pək.ˈkaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "piga p’kk’l",
            tagalog = "squeeze",
            english = "to",
            rootForm = "piga",
            category = "Greetings & Essentials",
            ipaNotation = "pək.ˈkəl",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "(or stick) saksak saksak",
            tagalog = "stab",
            english = "to",
            rootForm = "or",
            category = "General",
            ipaNotation = "sak.ˈsak"
        ),
        VocabularyEntity(
            kasiguranin = "tayo takn’g",
            tagalog = "stand",
            english = "to",
            rootForm = "tayo",
            category = "Greetings & Essentials",
            ipaNotation = "tak.ˈnəg",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "nakaw takaw",
            tagalog = "steal",
            english = "to",
            rootForm = "nakaw",
            category = "Greetings & Essentials",
            ipaNotation = "ta.ˈkaw"
        ),
        VocabularyEntity(
            kasiguranin = "(as leis) tuhog turok",
            tagalog = "string",
            english = "to",
            rootForm = "as",
            category = "General",
            ipaNotation = "tu.ˈrɔk"
        ),
        VocabularyEntity(
            kasiguranin = "sipsip s’ps’p",
            tagalog = "suck",
            english = "to",
            rootForm = "sipsip",
            category = "Greetings & Essentials",
            ipaNotation = "səp.ˈsəp",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "lunok t’l-l’n",
            tagalog = "swallow",
            english = "to",
            rootForm = "lunok",
            category = "Greetings & Essentials",
            ipaNotation = "təl.ˈlən",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "pawis pawis",
            tagalog = "sweat",
            english = "to",
            rootForm = "pawis",
            category = "Greetings & Essentials",
            ipaNotation = "ˈpaː.wis",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "maga baga",
            tagalog = "swell",
            english = "to",
            rootForm = "maga",
            category = "Greetings & Essentials",
            ipaNotation = "ba.ˈgaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "langoy langoy",
            tagalog = "swim",
            english = "to",
            rootForm = "langoy",
            category = "Greetings & Essentials",
            ipaNotation = "la.ˈŋɔj"
        ),
        VocabularyEntity(
            kasiguranin = "isip isip",
            tagalog = "think",
            english = "to",
            rootForm = "isip",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁiː.sip",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "tapon ibut",
            tagalog = "throw",
            english = "to",
            rootForm = "tapon",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈbʊt",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "tali igut",
            tagalog = "tie",
            english = "to",
            rootForm = "tali",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂi.ˈgʊt",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "suka ota",
            tagalog = "vomit",
            english = "to",
            rootForm = "suka",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁɔː.taɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "lakad lakad",
            tagalog = "walk",
            english = "to",
            rootForm = "lakad",
            category = "Greetings & Essentials",
            ipaNotation = "la.ˈkad"
        ),
        VocabularyEntity(
            kasiguranin = "hugas remp’s (general)",
            tagalog = "wash",
            english = "to",
            rootForm = "hugas",
            category = "General",
            ipaNotation = "ˈrɛm.pəs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "habi ladi",
            tagalog = "weave",
            english = "to",
            rootForm = "habi",
            category = "Greetings & Essentials",
            ipaNotation = "la.ˈdiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "pahid punas",
            tagalog = "wipe",
            english = "to",
            rootForm = "pahid",
            category = "Greetings & Essentials",
            ipaNotation = "ˈpʊː.nas",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "araw ngayong aldew",
            tagalog = "ngayong",
            english = "today",
            rootForm = "araw",
            category = "Numbers & Time",
            ipaNotation = "ŋa.ˈjɔŋ ˈɁal.dɛw",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sa paa guram’t sa basset",
            tagalog = "daliri",
            english = "toe",
            rootForm = "sa",
            category = "Body Parts & Health",
            ipaNotation = "gʊ.ra.ˈmət sa bas.ˈsɛt",
            phoneticGlottal = true
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
            kasiguranin = "ngipin (all teeth) ngip’n",
            tagalog = "(front)",
            english = "tooth",
            rootForm = "ngipin",
            category = "General",
            ipaNotation = "ŋi.ˈpən",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "sulo ungot",
            tagalog = "light",
            english = "torch,",
            rootForm = "sulo",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈŋɔt",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ponan",
            tagalog = "punong-kahoy",
            english = "tree",
            rootForm = "ponan",
            category = "Nature & Environment",
            ipaNotation = "ˈpɔː.nan",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "tree) puno baggi ng kayo",
            tagalog = "(of",
            english = "trunk",
            rootForm = "tree",
            category = "Daily Activities",
            ipaNotation = "bəg.ˈgiɁ naŋ ka.ˈjɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "bakokol",
            tagalog = "pagong",
            english = "turtle",
            rootForm = "bakokol",
            category = "Animals & Wildlife",
            ipaNotation = "ba.ˈkɔː.kɔl",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "kambal",
            tagalog = "kambal",
            english = "twins",
            rootForm = "kambal",
            category = "Greetings & Essentials",
            ipaNotation = "kam.ˈbal"
        ),
        VocabularyEntity(
            kasiguranin = "duwa",
            tagalog = "dalawa",
            english = "two",
            rootForm = "duwa",
            category = "Numbers & Time",
            ipaNotation = "dʊ.ˈwaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "maduk’s",
            tagalog = "pangit",
            english = "ugly",
            rootForm = "maduks",
            category = "Greetings & Essentials",
            ipaNotation = "ma.dʊ.ˈkəs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "baro badu",
            tagalog = "garment",
            english = "upper",
            rootForm = "baro",
            category = "Greetings & Essentials",
            ipaNotation = "ˈbaː.dʊɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "padisono",
            tagalog = "pataas",
            english = "upward",
            rootForm = "padisono",
            category = "Greetings & Essentials",
            ipaNotation = "pa.di.ˈsɔː.nɔɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "esbu",
            tagalog = "ihi",
            english = "urine",
            rootForm = "esbu",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁɛs.bʊ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ub’t",
            tagalog = "pekpek",
            english = "vagina",
            rootForm = "ubt",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈbət",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "atong",
            tagalog = "gulay",
            english = "vegetables",
            rootForm = "atong",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈtɔŋ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "boses",
            tagalog = "tinig",
            english = "voice",
            rootForm = "boses",
            category = "Greetings & Essentials",
            ipaNotation = "ˈbɔː.sɛs",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "gera",
            tagalog = "digma",
            english = "war",
            rootForm = "gera",
            category = "Greetings & Essentials",
            ipaNotation = "ˈgɛː.ra",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "mainit",
            tagalog = "mainit",
            english = "warm",
            rootForm = "mainit",
            category = "Body Parts & Health",
            ipaNotation = "ma.ˈɁiː.nit",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "danom",
            tagalog = "tubig",
            english = "water",
            rootForm = "danom",
            category = "Food & Dining",
            ipaNotation = "da.ˈnɔm"
        ),
        VocabularyEntity(
            kasiguranin = "kalabaw dappog",
            tagalog = "buffalo",
            english = "water",
            rootForm = "kalabaw",
            category = "Food & Dining",
            ipaNotation = "dap.ˈpɔg"
        ),
        VocabularyEntity(
            kasiguranin = "surf) alon alon",
            tagalog = "(as",
            english = "wave",
            rootForm = "surf",
            category = "General",
            ipaNotation = "ˈɁaː.lɔn",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "person,",
            tagalog = "(1st",
            english = "we",
            rootForm = "person",
            category = "Greetings & Essentials"
        ),
        VocabularyEntity(
            kasiguranin = "pl.) tayo tayo",
            tagalog = "(dual,",
            english = "we",
            rootForm = "pl",
            category = "General",
            ipaNotation = "ˈtaː.yɔ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "mahina",
            tagalog = "mahina",
            english = "weak",
            rootForm = "mahina",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈhiː.naɁ",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "basa",
            tagalog = "basa",
            english = "wet",
            rootForm = "basa",
            category = "Greetings & Essentials",
            ipaNotation = "ba.ˈsaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "ano",
            tagalog = "ano",
            english = "what",
            rootForm = "ano",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂa.ˈnɔ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "gulong",
            tagalog = "gulong",
            english = "wheel",
            rootForm = "gulong",
            category = "Body Parts & Health",
            ipaNotation = "gu.ˈlɔŋ"
        ),
        VocabularyEntity(
            kasiguranin = "kelan",
            tagalog = "kailan",
            english = "when",
            rootForm = "kelan",
            category = "Greetings & Essentials",
            ipaNotation = "ˈkɛː.lan",
            phoneticVowelLength = true
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
            kasiguranin = "sino",
            tagalog = "sino",
            english = "who",
            rootForm = "sino",
            category = "Greetings & Essentials",
            ipaNotation = "ˈsiː.nɔ",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "bakit",
            tagalog = "bakit",
            english = "why",
            rootForm = "bakit",
            category = "Greetings & Essentials",
            ipaNotation = "ˈbaː.kit",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "malawa",
            tagalog = "malawak",
            english = "wide",
            rootForm = "malawa",
            category = "Greetings & Essentials",
            ipaNotation = "ma.la.ˈwaɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kabingang babbi",
            tagalog = "asawa",
            english = "wife",
            rootForm = "kabingang",
            category = "Family & People",
            ipaNotation = "ka.bi.ˈŋaŋ bab.biɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "hangin par’s",
            tagalog = "(breeze)",
            english = "wind",
            rootForm = "hangin",
            category = "Nature & Environment",
            ipaNotation = "pa.ˈrəs",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "alak",
            tagalog = "alak",
            english = "wine",
            rootForm = "alak",
            category = "Greetings & Essentials",
            ipaNotation = "ˈɁaː.lak",
            phoneticGlottal = true,
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "pakpak",
            tagalog = "pakpak",
            english = "wing",
            rootForm = "pakpak",
            category = "Greetings & Essentials",
            ipaNotation = "pak.ˈpak"
        ),
        VocabularyEntity(
            kasiguranin = "kindat",
            tagalog = "kindat",
            english = "wink",
            rootForm = "kindat",
            category = "Greetings & Essentials",
            ipaNotation = "kin.ˈdat"
        ),
        VocabularyEntity(
            kasiguranin = "babae babbi",
            tagalog = "(female)",
            english = "woman",
            rootForm = "babae",
            category = "Family & People",
            ipaNotation = "bəb.ˈbiɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "mali",
            tagalog = "mali",
            english = "wrong",
            rootForm = "mali",
            category = "Greetings & Essentials",
            ipaNotation = "ma.ˈliɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "uwab",
            tagalog = "hikab",
            english = "yawn",
            rootForm = "uwab",
            category = "Greetings & Essentials",
            ipaNotation = "Ɂʊ.ˈwab",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kayo",
            tagalog = "kayo",
            english = "ye",
            rootForm = "kayo",
            category = "Greetings & Essentials",
            ipaNotation = "ka.ˈjɔɁ",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "taon",
            tagalog = "taon",
            english = "year",
            rootForm = "taon",
            category = "Body Parts & Health",
            ipaNotation = "ta.ˈɁɔn",
            phoneticGlottal = true
        ),
        VocabularyEntity(
            kasiguranin = "kahapon",
            tagalog = "kahapon",
            english = "yesterday",
            rootForm = "kahapon",
            category = "Numbers & Time",
            ipaNotation = "ka.ˈhaː.pɔn",
            phoneticVowelLength = true
        ),
        VocabularyEntity(
            kasiguranin = "anuman walang anuman",
            tagalog = "walang",
            english = "welcome",
            rootForm = "anuman",
            category = "Greetings & Essentials",
            ipaNotation = "wa.ˈlaŋ Ɂa.nu.ˈman",
            phoneticGlottal = true
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
