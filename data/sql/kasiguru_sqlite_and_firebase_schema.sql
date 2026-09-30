-- ============================================================================
-- KASIGURU ECOSYSTEM SCHEMA: SQLITE (ROOM DB) & FIREBASE FIRESTORE STRUCTURE
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. SQLITE SCHEMA (Android Room Local Database)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS vocabulary (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    kasiguranin_word TEXT NOT NULL,
    filipino_translation TEXT DEFAULT NULL,
    english_translation TEXT DEFAULT NULL,
    root_word TEXT DEFAULT NULL,
    part_of_speech TEXT DEFAULT NULL,
    category TEXT DEFAULT NULL,
    audio_file TEXT DEFAULT NULL,
    sample_sentence TEXT DEFAULT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS conjugations (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    vocabulary_id INTEGER NOT NULL,
    conjugated_form TEXT NOT NULL,
    tense TEXT NOT NULL,
    affix_type TEXT DEFAULT NULL,
    FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conjugations_vocab_id ON conjugations(vocabulary_id);
CREATE INDEX IF NOT EXISTS idx_vocabulary_category ON vocabulary(category);

-- Sample SQLite Insertion for "abben" with strict NULLs (no 'nan' strings)
INSERT INTO vocabulary (kasiguranin_word, filipino_translation, english_translation, root_word, part_of_speech, category, audio_file, sample_sentence, created_at)
VALUES ('abben', NULL, NULL, 'abben', 'Verb', 'Body Parts & Health', NULL, NULL, 1770520000000);

-- Get last inserted ID in SQLite
-- Assuming vocabulary id = 1 for sample insertion:
INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type) VALUES (1, 'inumabben', 'perfective', 'Infix');
INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type) VALUES (1, 'nagabben', 'perfective', 'Prefix');
INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type) VALUES (1, 'magaabenën', 'imperfective', 'Circumfix');
INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type) VALUES (1, 'magaabben', 'contemplative', 'Prefix');


-- ----------------------------------------------------------------------------
-- 2. FIREBASE CLOUD FIRESTORE STRUCTURE (Web Portal & Live Sync Engine)
-- Collection: "vocabulary"
-- Document ID: Auto-generated or custom slug
-- ----------------------------------------------------------------------------
/*
  {
    "kasiguranin": "abben",
    "filipino": null,
    "english": null,
    "rootWord": "abben",
    "partOfSpeech": "Verb",
    "category": "Body Parts & Health",
    "audioFile": null,
    "sampleSentence": null,
    "createdAt": 1770520000000,
    "conjugations": [
      {
        "conjugatedForm": "inumabben",
        "tense": "perfective",
        "affixType": "Infix"
      },
      {
        "conjugatedForm": "nagabben",
        "tense": "perfective",
        "affixType": "Prefix"
      },
      {
        "conjugatedForm": "magaabenën",
        "tense": "imperfective",
        "affixType": "Circumfix"
      },
      {
        "conjugatedForm": "magaabben",
        "tense": "contemplative",
        "affixType": "Prefix"
      }
    ]
  }
*/
