# 🎮 Master Catalog — Gamified Language App Design Elements

A comprehensive reference catalog of every **customizable visual and interactive element** for KasiGuru, organized across 6 core categories. Tailored for story-based lessons, vocabulary lexicons, pronunciation audio, mini-games, and offline progress tracking.

---

## 👤 1. Identity & Progression Elements

| Element Name | Visual / Interactive Specification | Purpose in User Experience | Where in User Flow |
|---|---|---|---|
| **Avatar Profile Ring** | 54×54dp circle with glowing gradient border (`HeroCardStart`) | Represents learner identity and avatar customization. | Top Header Bar (*HomeScreen*, *ProfileScreen*) |
| **Level Progress Bar** | Horizontal rounded indicator with linear progress filling | Visualizes journey from Level 1 (*Novice Explorer*) to Level 5 (*Kasiguranin Legend*). | *HomeScreen* Bento Card, *ProfileScreen* |
| **XP Counter Badge** | Floating pill with trophy icon (`Iconsax.Cup`) & total XP text | Displays earned experience points in real time. | Top Header Bar, *Daily Quests Card*, *ProfileScreen* |
| **Daily Streak Flame** | Glowing orange flame pill (`Iconsax.Flash`) with day count | Motivates daily habit loop & streak retention. | Top Header Bar, *Streak Dialog*, *Quests Card* |
| **Offline Sync Pill** | Green/Gray status pill (`Room SQLite v11 Sync`) | Reassures user that all progress is saved locally offline. | *SettingsScreen*, *Profile Header* |

---

## 🏅 2. Rewards & Achievement System

| Element Name | Visual / Interactive Specification | Purpose in User Experience | Where in User Flow |
|---|---|---|---|
| **Achievement Badges** | 256×256px vector icons (Grayscale when locked, Gold when unlocked) | Rewards milestone accomplishments (e.g. 50 Words, 7-Day Streak). | *AchievementsScreen*, *Profile Screen* |
| **Level Up Celebration Modal** | Full-screen dialog with confetti particle effects & rank title | Celebrates major progression milestones. | Post-quiz or post-game completion |
| **Reward Claim Toast** | Floating snackbar with "+50 XP Unlocked!" banner | Immediate positive reinforcement for completing daily quests. | Bottom overlay on task completion |
| **Streak Multiplier Chip** | 2x / 3x Bonus multiplier badge | Encourages long-term streak retention with XP boosts. | *Game Result Screen*, *Leaderboard* |

---

## 🎮 3. Gamification Mechanics & Leaderboard

| Element Name | Visual / Interactive Specification | Purpose in User Experience | Where in User Flow |
|---|---|---|---|
| **Top 3 Podium Cards** | Gold 👑 (Rank #1), Silver 🥈 (#2), Bronze 🥉 (#3) podiums | Visual distinction for top daily/all-time learners. | Top of *LeaderboardScreen* |
| **Sticky "My Rank" Bar** | Elevated bottom bar highlighting user's live position & XP | Ensures user always knows their standing without scrolling. | Bottom of *LeaderboardScreen* |
| **Micro-Quiz Modal** | 1-question verification sheet (*"Prove You Know It!"*) | Strict mechanics preventing accidental or lazy checkmarking. | Tapping word checkmark in *CategoryDetailScreen* |
| **Pre-Game Rules Card** | Modal bottom sheet with game objectives & XP reward rate | Prepares user before starting Match or Fill-in-Blank games. | Tapping a game tile in *GameHubScreen* |

---

## 📚 4. Content & Lexicon Icons

| Element Name | Visual / Interactive Specification | Purpose in User Experience | Where in User Flow |
|---|---|---|---|
| **Category Vector Cards** | Cultural artwork (Carabao 🐃, Nipa Hut 🏠, Mango 🥭, Pana 🏹) | Differentiates vocabulary domains with native imagery. | 2-Column Bento Grid (*VocabularyScreen*) |
| **Audio Pronunciation Waveform** | 48×48dp circular button with speaker icon (`Iconsax.VolumeHigh`) | Triggers native voice recording audio playback. | *Vocabulary Card*, *Flashcard Screen* |
| **Phonetic Feature Chips** | Soft pills for Glottal Stop `ʔ`, Long Vowel `ː`, & IPA `[aː]` | Educates learners on unique Dumagat/Agta phonetics. | Expanded *Vocabulary Detail Card* |
| **Verb Aspect Pills** | Color-coded tags (Neutral, Past, Present, Future) | Explains complex Kasiguranin verb inflections. | Grammar tab & *Aspect Builder Game* |
| **Flashcard Flip Cue** | Animated 3D card rotater with swipe arrows | Intuitive affordance for flipping flashcards. | *FlashcardDeckScreen* |

---

## 📖 5. Narrative & Cultural Storytelling Elements

| Element Name | Visual / Interactive Specification | Purpose in User Experience | Where in User Flow |
|---|---|---|---|
| **Storybook Cover Card** | Sky Blue card with cultural book illustration & reading time | Invites users to immerse in Casiguran Agta folklore. | *StoryListScreen*, *Home Bento Grid* |
| **Sentence Audio Highlight** | Soft yellow background glow on active sentence | Synchronizes audio listening with reading comprehension. | *StoryReaderScreen* |
| **Cultural Trivia Card** | Gold-accented callout banner with heritage context | Deepens cultural appreciation of Casiguran Agta traditions. | Interspersed in stories & cultural screen |
| **Chapter Seal Badge** | Stamp-style completion emblem | Marks finished story chapters. | End of *StoryReaderScreen* |

---

## ⚡ 6. Micro-Interaction & Audio-Visual Feedback

| Element Name | Visual / Interactive Specification | Purpose in User Experience | Where in User Flow |
|---|---|---|---|
| **Haptic Tactile Vibrations** | Short click (success), double pulse (error) | Tactile confirmation of user inputs. | Every button tap & quiz selection |
| **Correct Answer Glow** | Green border pulse (`#22C55E`) + checkmark icon | Clear positive feedback during mini-games. | *Game Screens*, *Micro-Quiz Modal* |
| **Incorrect Shake Animation** | Horizontal card shake + Red border (`#EF4444`) | Immediate error feedback without harsh penalties. | *Game Screens*, *Micro-Quiz Modal* |
| **Floating XP Animated Text** | Floating `+50 XP` text rising and fading out | Delights user upon earning points. | Top of screen post-action |
| **Unread Badge Indicator** | 8dp solid primary blue dot | Draws attention to unread notifications or new stories. | *Top Header Bell*, *Notification Center* |
