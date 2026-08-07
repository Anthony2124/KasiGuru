-- ============================================================================
-- KASIGURANIN LANGUAGE LEARNING ECOSYSTEM - DATABASE SCHEMA & MIGRATION SCRIPT
-- PostgreSQL Relational Schema with Normalized Conjugations & NULL Sanitization
-- ============================================================================

-- 1. Create normalized 'vocabulary' table for base words and categories
CREATE TABLE IF NOT EXISTS vocabulary (
    id SERIAL PRIMARY KEY,
    kasiguranin_word VARCHAR(255) NOT NULL,
    filipino_translation VARCHAR(255),
    english_translation VARCHAR(255),
    root_word VARCHAR(255),
    part_of_speech VARCHAR(100),
    category VARCHAR(100),
    audio_file VARCHAR(255),
    sample_sentence TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create normalized 'conjugations' table linked with ON DELETE CASCADE
CREATE TABLE IF NOT EXISTS conjugations (
    id SERIAL PRIMARY KEY,
    vocabulary_id INT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    conjugated_form VARCHAR(255) NOT NULL,
    tense VARCHAR(50) NOT NULL, -- 'perfective', 'imperfective', 'contemplative'
    affix_type VARCHAR(50),      -- 'Prefix', 'Infix', 'Suffix', 'Circumfix'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup by vocabulary_id and tense
CREATE INDEX IF NOT EXISTS idx_conjugations_vocab_id ON conjugations(vocabulary_id);
CREATE INDEX IF NOT EXISTS idx_vocabulary_category ON vocabulary(category);

-- ============================================================================
-- 3. Sample Migration & Insertion Script for "abben" with Strict NULL Handling
-- ============================================================================

DO $$ 
DECLARE
    new_vocab_id INT;
BEGIN
    -- Insert base vocabulary entry (strict NULLs instead of 'nan' or empty strings)
    INSERT INTO vocabulary (
        kasiguranin_word, 
        filipino_translation, 
        english_translation, 
        root_word, 
        part_of_speech, 
        category, 
        audio_file, 
        sample_sentence
    ) 
    VALUES (
        'abben', 
        NULL, 
        NULL, 
        'abben', 
        'Verb', 
        'Body Parts & Health', 
        NULL, 
        NULL
    ) 
    RETURNING id INTO new_vocab_id;

    -- Insert distinct conjugation rows linked to the parent vocabulary record

    -- Perfective Form 1 ("inumabben" / "Infix")
    INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type)
    VALUES (new_vocab_id, 'inumabben', 'perfective', 'Infix');

    -- Perfective Form 2 ("nagabben" / "Prefix")
    INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type)
    VALUES (new_vocab_id, 'nagabben', 'perfective', 'Prefix');

    -- Imperfective Form 1 ("magaabenën" / "Circumfix")
    INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type)
    VALUES (new_vocab_id, 'magaabenën', 'imperfective', 'Circumfix');

    -- Contemplative Form 1 ("magaabben" / "Prefix")
    INSERT INTO conjugations (vocabulary_id, conjugated_form, tense, affix_type)
    VALUES (new_vocab_id, 'magaabben', 'contemplative', 'Prefix');

END $$;
