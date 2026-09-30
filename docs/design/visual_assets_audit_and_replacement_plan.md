# 🎨 Master Visual Asset Audit & Custom Design Plan — KasiGuru

A comprehensive audit of every visual asset — badges, icons, character avatars, storybook illustrations, and UI graphics — across the **KasiGuru** codebase that currently uses a placeholder, default vector, or system emoji and requires custom-designed replacements.

---

## Executive Summary & Asset Scope

| Feature Area | Total Assets | Current Placeholder Status | Target Custom Replacement Type |
|---|:---:|---|---|
| **1. Identity, Avatars & Onboarding** | 16 | Generic `ic_profile_outline` & system emojis (`🐣`, `🌿`, etc.) | 9 Custom Resident Character Portraits, 6 Persona Badges, 1 Hero Mascot |
| **2. Progression & Rank System** | 6 | Emojis (`🌱`, `📚`, etc.) & generic 2D shield vectors | 5 Tiered 3D Level Rank Badges, 1 Dynamic Learning Map Node System |
| **3. Rewards, Achievements & Streaks** | 10 | System emojis (`🌟`, `📖`, `🔥`, etc.) & flat 2D flame vector | 9 Custom 3D Milestone Badges, 1 Expressive Streak Mascot |
| **4. Content & Lexicon (Categories)** | 12 | Monochrome Iconsax utility icons (`Iconsax.Book`, etc.) | 12 Culturally-grounded 3D Vector Category Emblems |
| **5. Mini-Games & Challenges** | 8 | Utility icons (`Iconsax.Edit`, `Flash`, etc.) & text stats | 6 Custom Mini-Game 3D Badges, 1 Gameboard Graphic, 1 Victory Trophy |
| **6. Narrative & Storytelling** | 23 | Plain text in colored box (`illustrationDesc`) & draft PNG | 22 Full-color Storybook Page Illustrations (5 Stories), 1 Stories Hero Banner |
| **7. Gamification & Leaderboard** | 4 | System emojis (`👑`, `🥈`, `🥉`, `👤`) in basic shapes | 3 Top-Podium 3D Medals/Crown, 1 Ranked Learner Avatar Ring |
| **8. App Branding & System** | 2 | Minimalist flat geometric 'K' vector paths | Premium 3D App Launcher Icon & Vector Splash Screen Logo |
| **TOTAL CUSTOM ASSETS** | **81** | — | — |

---

## 👦 1. Identity, Character Avatars & Onboarding

### Resident Character Avatars (`CasiguranResident`)
* **Location**: [`app/src/main/java/com/kasiguru/ui/components/CasiguranAvatarPortrait.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/components/CasiguranAvatarPortrait.kt#L27-L37)
* **Current Placeholder**: All 9 residents point to `R.drawable.ic_profile_outline` (a monochrome blank user silhouette).
* **Target Size/Format**: `512 × 512 px` transparent PNG / Vector Drawable.

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `avatar_apo_dumagat` | Character Avatar | `ic_profile_outline` | **Apo Dumagat (Elder Guide)**: Respected Dumagat Agta elder with wisdom lines, traditional beaded necklace, woven headwear, and warm storytelling smile. |
| `avatar_mang_mateo` | Character Avatar | `ic_profile_outline` | **Mang Mateo (Fisherman)**: Seasoned coastal fisherman with straw hat (*salakot*), friendly smile, and weathered sea gear; guides coastal & marine vocab. |
| `avatar_maam_elena` | Character Avatar | `ic_profile_outline` | **Ma'am Elena (Teacher)**: Primary KasiGuru educator; warm teacher with glasses, holding a book, wearing a modern Filipiniana teacher uniform. |
| `avatar_juan` | Character Avatar | `ic_profile_outline` | **Juan (Student Learner)**: Enthusiastic young student character with a backpack, pencil in pocket, representing the learner's journey. |
| `avatar_nana_maria` | Character Avatar | `ic_profile_outline` | **Nana Maria (Agta Heritage Specialist)**: Indigenous Dumagat artisan woman wearing native woven sash (*habol*) and beaded earrings. |
| `avatar_casiguran_surfer` | Character Avatar | `ic_profile_outline` | **Casiguran Surfer**: Youthful energetic Pacific coast surfer with surfboard and tropical sun visor; hosts mini-games and action challenges. |
| `avatar_mang_ben` | Character Avatar | `ic_profile_outline` | **Mang Ben (Farmer)**: Sierra Madre highland farmer with bamboo rake and sun-shielding hat; guides flora, agriculture, and nature terms. |
| `avatar_ina_ligaya` | Character Avatar | `ic_profile_outline` | **Ina Ligaya (Mother/Homemaker)**: Welcoming village mother in traditional apron holding native clay pot; guides home and family vocabulary. |
| `avatar_kiko` | Character Avatar | `ic_profile_outline` | **Kiko (Musician/Audio Master)**: Cheerful youth with headphones and native bamboo flute (*tulali*); guides phonetics and audio listening quizzes. |

### Onboarding Persona Avatars & Hero Mascot
* **Location**: [`app/src/main/java/com/kasiguru/ui/screens/onboarding/OnboardingScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/onboarding/OnboardingScreen.kt#L730-L738), [`app/src/main/res/drawable/img_hero_student.png`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/drawable/img_hero_student.png)

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `avatar_persona_hatchling` | Avatar Icon | Emoji `🐣` in circle | **Beginner Hatchling**: Cute baby chick hatching from a painted native earthen shell; represents entry-level learner persona. |
| `avatar_persona_seedling` | Avatar Icon | Emoji `🌿` in circle | **Sierra Madre Seedling**: Sprouting tropical rainforest shoot with dew drop; represents growing vocabulary apprentice. |
| `avatar_persona_wave` | Avatar Icon | Emoji `🌊` in circle | **Pacific Coast Wave**: Stylized vibrant ocean wave crest with sun sparkle; represents dynamic explorer persona. |
| `avatar_persona_eagle` | Avatar Icon | Emoji `🦅` in circle | **Philippine Eagle**: Noble eagle head emblem with golden feathers; represents advanced linguistic scholar. |
| `avatar_persona_crown` | Avatar Icon | Emoji `👑` in circle | **Tribal Chief Crown**: Regal gold headdress with indigenous Dumagat weave patterns; represents master learner persona. |
| `avatar_persona_star` | Avatar Icon | Emoji `⭐` in circle | **Star Explorer**: Radiant 3D five-point gold star; represents high-achieving gamified learner. |
| `img_hero_student` | Mascot Illustration | Preliminary PNG (238 KB) | **Student Hero Character**: 3D Pixar-style full-body mascot of a young Casiguran student smiling warmly while holding an open notebook and wooden pencil, wearing subtle native woven patterns. Used on HomeScreen Hero Card. |

---

## 👑 2. Progression & Rank System

* **Code Locations**: 
  - [`app/src/main/java/com/kasiguru/util/gamification/GamificationEngine.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/util/gamification/GamificationEngine.kt#L39-L40)
  - [`app/src/main/java/com/kasiguru/ui/components/GamifiedBadge3D.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/components/GamifiedBadge3D.kt#L63-L69)
  - [`app/src/main/java/com/kasiguru/ui/components/LevelUpDialog.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/components/LevelUpDialog.kt#L56-L60)
  - [`app/src/main/java/com/kasiguru/ui/components/LearningPathNode.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/components/LearningPathNode.kt#L44-L48)
* **Target Size/Format**: `256 × 256 px` 3D isometric vector / PNG with transparency.

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `ic_badge_level_1` | Level Rank Badge | Emoji `🌱` / `ic_star_shield_2d` | **Level 1 (Novice Explorer)**: Glossy 3D badge featuring a green seedling sprouting from a carved wooden tribal shield with brass borders. |
| `ic_badge_level_2` | Level Rank Badge | Emoji `📚` / `ic_star_shield_2d` | **Level 2 (Vocab Apprentice)**: Glossy 3D badge featuring an open leather notebook with a silver feather quill pen and sapphire ribbon. |
| `ic_badge_level_3` | Level Rank Badge | Emoji `🎧` / `ic_star_shield_2d` | **Level 3 (Linguistic Scholar)**: Glossy 3D badge featuring sleek golden headphones resting over an ancient rolled parchment scroll. |
| `ic_badge_level_4` | Level Rank Badge | Emoji `⚡` / `ic_star_shield_2d` | **Level 4 (Grammar Specialist)**: Glossy 3D badge featuring an electric blue lightning bolt surging across an engraved grammar shield book. |
| `ic_badge_level_5` | Level Rank Badge | Emoji `👑` / `ic_crown_badge_2d` | **Level 5 (Kasiguranin Legend)**: Top-tier 3D golden royal crown badge with traditional Casiguran Agta geometric tribal inlays and glowing rubies. |
| `ic_path_node_checkpoint` | Map Node Graphic | Flat circle + `ic_tick_circle` / `ic_play_outline` / `ic_lock_outline` | **Learning Journey Trail Stepping Stones**: 3D floating island / stepping-stone checkpoint nodes with distinct active (glowing gold pulse), completed (bamboo checkmark flag), and locked (ancient stone padlock) states. |

---

## 🏅 3. Rewards, Achievements & Streaks

* **Code Locations**:
  - [`app/src/main/java/com/kasiguru/ui/screens/achievements/AchievementsScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/achievements/AchievementsScreen.kt#L216-L220)
  - [`app/src/main/java/com/kasiguru/data/local/DatabaseSeeder.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/data/local/DatabaseSeeder.kt#L3588-L3725)
  - [`app/src/main/java/com/kasiguru/ui/components/StreakDialog.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/components/StreakDialog.kt#L52-L58)
  - [`app/src/main/res/drawable/ic_streak_flame_2d.xml`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/drawable/ic_streak_flame_2d.xml)

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `ic_badge_first_word` | Milestone Badge | Emoji `🌟` in circle | **Unáng Salitâ (First Word Learned)**: 3D golden star medallion with radiant sun rays and an engraved speech bubble in the center. |
| `ic_badge_10_words` | Milestone Badge | Emoji `📖` in circle | **Sampûng Salitâ (10 Words Mastered)**: 3D open gilded textbook badge with the numeral "10" embossed on the book cover. |
| `ic_badge_50_words` | Milestone Badge | Emoji `🎓` / `🏆` in circle | **Limampûng Salitâ (50 Words Mastered)**: 3D graduation mortarboard cap with golden tassel resting upon a royal certificate scroll. |
| `ic_badge_streak_3` | Streak Badge | Emoji `🔥` in circle | **Tatlong Aldaw (3-Day Streak)**: 3D fiery orange flame badge with glowing ember particles and gold metallic border. |
| `ic_badge_streak_7` | Streak Badge | Emoji `💪` in circle | **Isáng Linggo (7-Day Streak)**: 3D polished golden muscular flexed arm badge wrapped in fiery lightning accents. |
| `ic_badge_first_story` | Milestone Badge | Emoji `📕` in circle | **Mambábasa (First Story Reader)**: 3D folklore storybook badge with glowing tribal seal and ribbon bookmark. |
| `ic_badge_first_game` | Milestone Badge | Emoji `🎮` in circle | **Mánlalaro (First Mini-Game)**: 3D arcade gamepad coin token with floating victory stars. |
| `ic_badge_perfect_game` | Milestone Badge | Emoji `⭐` in circle | **Perpekto! (100% Quiz Score)**: 3D triple-star gold medal with laurel wreath and diamond centerpiece. |
| `ic_badge_level_10_master` | Milestone Badge | Emoji `👑` in circle | **Mæstro (Level 10 Master)**: Grand master ceremonial Agta tribal chief headdress medallion with cascading feathers. |
| `img_mascot_streak_flame` | Streak Mascot Graphic | Flat 2D vector `ic_streak_flame_2d.xml` | **Expressive Streak Flame Character**: Vibrant 3D flame mascot with expressive joyful face and animated spark trail for streak counters and celebration dialogs. |

---

## 📚 4. Content & Lexicon (12 Vocabulary Categories)

* **Code Locations**:
  - [`app/src/main/java/com/kasiguru/ui/theme/CategoryMetaData.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/theme/CategoryMetaData.kt#L28-L126) (`customDrawableRes` currently all `null`)
  - [`app/src/main/java/com/kasiguru/ui/screens/vocabulary/VocabularyScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/vocabulary/VocabularyScreen.kt#L302-L316)
  - [`app/src/main/java/com/kasiguru/ui/screens/vocabulary/CategoryDetailScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/vocabulary/CategoryDetailScreen.kt#L171-L184)
* **Target Size/Format**: `256 × 256 px` 3D isometric vector / PNG with transparent background.

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `img_cat_greetings` | Category Icon | `Iconsax.BookBold` (utility book) | **Greetings & Essentials**: 3D illustration of two welcoming hands making a respectful gesture with a warm golden sunburst. |
| `img_cat_food` | Category Icon | `Iconsax.VolumeHighBold` (speaker icon) | **Food & Dining**: 3D illustration of a ripe golden Philippine Mango (*Manga*) beside a steaming bowl of rice and clay pot. |
| `img_cat_animals` | Category Icon | `Iconsax.FlashBold` (lightning bolt) | **Animals & Wildlife**: 3D illustration of a friendly Philippine Carabao (Water Buffalo) standing beside tropical forest leaves. |
| `img_cat_body` | Category Icon | `Iconsax.ProfileBold` (user outline) | **Body Parts & Health**: 3D illustration of a glowing heart and anatomical eye icon surrounded by healing herbal leaf accents. |
| `img_cat_numbers` | Category Icon | `Iconsax.Calendar` (calendar grid) | **Numbers & Time**: 3D illustration of an antique hourglass and sun dial with floating glowing numerals 1, 2, 3. |
| `img_cat_weather` | Category Icon | `Iconsax.Global` (globe outline) | **Weather & Climate**: 3D illustration of a bright tropical sun peeking from behind a fluffy rain cloud with rainbow mist. |
| `img_cat_emotions` | Category Icon | `Iconsax.StarBold` (outline star) | **Emotions & Feelings**: 3D illustration of expressive theater masks (Happy, Surprised, Loved) with floating pink heart particles. |
| `img_cat_house` | Category Icon | `Iconsax.HomeBold` (house outline) | **House & Daily Life**: 3D illustration of a traditional native Nipa Hut (*Bahay Kubo*) with bamboo thatch roof and earthen water jar. |
| `img_cat_nature` | Category Icon | `Iconsax.Teacher` (teacher icon) | **Nature & Environment**: 3D illustration of the green Sierra Madre mountain ridge meeting ocean waves under bright sunlight. |
| `img_cat_family` | Category Icon | `Iconsax.People` (generic people) | **Family & People**: 3D illustration of an indigenous Dumagat family (parents and child) wearing traditional woven sashes. |
| `img_cat_colors` | Category Icon | `Iconsax.Element4Bold` (4 squares) | **Colors & Shapes**: 3D illustration of an artist's wooden palette with vibrant paint splashes and geometric forms (Circle, Star, Triangle). |
| `img_cat_tools` | Category Icon | `Iconsax.SettingBold` (gear icon) | **Occupations & Tools**: 3D illustration of an indigenous Casiguran Agta hunting bow and arrow (*Pana*) with carved adze (*Apak*). |

---

## 🎮 5. Mini-Games & Interactive Challenges

* **Code Locations**:
  - [`app/src/main/java/com/kasiguru/ui/screens/games/GameHubScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/games/GameHubScreen.kt#L188-L282)
  - [`app/src/main/res/drawable/img_mini_games_board.png`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/drawable/img_mini_games_board.png)
  - [`app/src/main/java/com/kasiguru/ui/components/GameOverView.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/components/GameOverView.kt#L48-L78)
  - [`app/src/main/java/com/kasiguru/ui/screens/games/LevelSelectionScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/games/LevelSelectionScreen.kt#L190-L205)

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `img_mini_games_board` | Banner Graphic | Draft PNG (100 KB) | **Mini Games Hub Hero Banner**: Playful 3D isometric gameboard with wooden game tokens, glowing dice, and floating gold stars. Used on HomeScreen Bento Card. |
| `ic_game_word_match` | Mini-Game Tile Icon | `Iconsax.Element4Outline` (4 squares) | **Word Match Blitz**: 3D dual matching flip cards snapping together with a glowing linguistic energy arc. |
| `ic_game_fill_blank` | Mini-Game Tile Icon | `Iconsax.Edit` (monochrome pencil) | **Fill in the Blank**: 3D missing puzzle piece slipping smoothly into a glowing sentence slot. |
| `ic_game_audio_quiz` | Mini-Game Tile Icon | `Iconsax.VolumeHigh` (speaker icon) | **Audio Listening Quiz**: 3D golden headphones over expanding harmonic soundwaves and native wooden drum. |
| `ic_game_aspect_builder` | Mini-Game Tile Icon | `Iconsax.Flash` (lightning icon) | **Verb Aspect Inflection Builder**: 3D interlocking modular affix blocks (Neutral, Past, Present, Future) snapping together. |
| `ic_game_sentence_order` | Mini-Game Tile Icon | `Iconsax.Document` (generic file icon) | **Sentence Construction Order**: 3D sequenced wooden word stones aligning on a grammatical rail. |
| `ic_game_reverse_match` | Mini-Game Tile Icon | `Iconsax.RepeatOutline` (reload arrows) | **Reverse Match (Tagalog ⇄ Kasiguranin)**: 3D dual-directional bilingual translation mirrors reflecting across languages. |
| `ic_game_over_trophy` | Victory Graphic | Outline `Iconsax.Cup` + `StarBold` | **Game Victory Trophy & Stars**: Polished 3D gold victory cup accompanied by sculpted 3-star milestone medals (1-star, 2-star, 3-star earned variations). |

---

## 📖 6. Narrative & Storytelling (5 Stories, 22 Pages)

* **Code Locations**:
  - [`app/src/main/java/com/kasiguru/ui/screens/stories/StoryReaderScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/stories/StoryReaderScreen.kt#L116-L137) (currently renders `targetPage.illustrationDesc` in a plain box)
  - [`app/src/main/java/com/kasiguru/data/local/DatabaseSeeder.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/data/local/DatabaseSeeder.kt#L3486-L3585)
  - [`app/src/main/res/drawable/img_stories_books.png`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/drawable/img_stories_books.png)
* **Target Size/Format**: `1024 × 576 px` (16:9) or `800 × 600 px` (4:3) full-color illustrated scene artwork.

### Hub Hero Graphic
| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `img_stories_books` | Banner Graphic | Draft PNG (418 KB) | **Stories Hub Hero Banner**: Ancient magical open storybook with floating golden stars, glowing Casiguran motifs, and rainforest leaves. |

### Story 1: "The Shell of Casiguran" (*Ing Kabibe ng Casiguran*)
| Asset Identifier | Page | Current Placeholder Text | Functional Representation & Target Scene Design |
|---|:---:|---|---|
| `img_story_1_page_1` | 1 | "A young girl standing on a beautiful beach at sunrise..." | **Sunrise Shore**: Young Maring standing on the Casiguran shoreline looking out at calm morning waters with the misty Sierra Madre mountains in the background. |
| `img_story_1_page_2` | 2 | "Maring walking along a path with her father carrying fishing nets..." | **Morning Walk**: Maring walking hand-in-hand with her fisherman father along a coconut tree-lined sandy path carrying woven nets. |
| `img_story_1_page_3` | 3 | "A glowing, colorful seashell on the sand with gentle waves" | **The Discovery**: A close-up perspective of a radiant, iridescent sea shell resting on golden sand washed by gentle foamy surf. |
| `img_story_1_page_4` | 4 | "Maring showing the shell to her mother inside their nipa hut" | **Family Delight**: Inside a cozy bamboo Bahay Kubo, Maring excitedly holds up the shimmering shell for her smiling mother to see. |
| `img_story_1_page_5` | 5 | "Maring holding the shell against a stunning sunset over the Casiguran bay" | **Sunset Keepsake**: Maring holding her cherished shell against a breathtaking twilight sunset over Casiguran Bay. |

### Story 2: "Malakas and Maganda" (*Si Malakas at si Maganda*)
| Asset Identifier | Page | Current Placeholder Text | Functional Representation & Target Scene Design |
|---|:---:|---|---|
| `img_story_2_page_1` | 1 | "A magnificent Philippine eagle flying over an endless blue ocean" | **Primordial Sky**: A majestic Philippine Eagle with outstretched wings soaring beneath ancient sunlit clouds above an endless turquoise sea. |
| `img_story_2_page_2` | 2 | "A giant golden bamboo stalk floating in ocean waves" | **The Floating Bamboo**: The eagle pecking with curious force at a giant, glowing golden bamboo stalk drifting in ocean swells. |
| `img_story_2_page_3` | 3 | "A strong man and beautiful woman stepping out from split bamboo" | **The Emergence**: The giant bamboo splitting open with divine light as Malakas (strong man) and Maganda (graceful woman) step forth into the world. |
| `img_story_2_page_4` | 4 | "Malakas and Maganda walking along lush green Philippine island hills" | **First Ancestors**: Malakas and Maganda standing atop rolling green tropical island hills overlooking the Philippine archipelago in harmony. |

### Story 3: "Legend of the Pineapple" (*Alamat ng Pinya / Si Pinang*)
| Asset Identifier | Page | Current Placeholder Text | Functional Representation & Target Scene Design |
|---|:---:|---|---|
| `img_story_3_page_1` | 1 | "A young girl sitting comfortably while her mother cooks" | **Lazy Afternoons**: Young Pinang relaxing lazily on a bamboo bench with crossed arms while her hardworking mother stirs a pot over firewood. |
| `img_story_3_page_2` | 2 | "Pinang looking around carelessly in the kitchen" | **Missing Ladle**: Pinang barely looking, shrugging in the kitchen cupboards unable (and unwilling) to search for the cooking spoon. |
| `img_story_3_page_3` | 3 | "Mother looking worried while speaking to Pinang" | **Mother's Wish**: Mother looking exasperated yet pleading as she wishes aloud for Pinang to have a hundred eyes to see what is right in front of her. |
| `img_story_3_page_4` | 4 | "A golden pineapple growing in a garden under bright sunlight" | **The Pineapple Plant**: A mysterious, golden crown-topped fruit covered with dozens of tiny "eyes" growing in the sunlit garden plot. |

### Story 4: "The Wise Fisherman" (*Ing Marunong na Mangingisdâ*)
| Asset Identifier | Page | Current Placeholder Text | Functional Representation & Target Scene Design |
|---|:---:|---|---|
| `img_story_4_page_1` | 1 | "An old fisherman sitting in a wooden boat, mending his nets at dawn" | **Dawn on the Banca**: Elder Mang Tasyo seated inside his outrigger banca boat, patiently repairing cotton fishing nets in the early morning twilight. |
| `img_story_4_page_2` | 2 | "Villagers gathered around Mang Tasyo as he tells stories by a campfire" | **Campfire Lore**: Village children and apprentices seated in a semicircle on the sand listening intently to Mang Tasyo by a glowing beach campfire. |
| `img_story_4_page_3` | 3 | "Mang Tasyo pointing at the ocean, moonlight reflecting off the waves" | **Whispers of the Tide**: Mang Tasyo extending his hand toward the silver, moonlit waves, explaining how the currents speak to those who listen. |
| `img_story_4_page_4` | 4 | "Young people learning from Mang Tasyo on a boat, stars visible above" | **Celestial Navigation**: Mang Tasyo guiding young navigators in a boat, pointing upward to the starry constellations guiding their way home. |

### Story 5: "The Town Fiesta" (*Ing Pistá ng Bayan*)
| Asset Identifier | Page | Current Placeholder Text | Functional Representation & Target Scene Design |
|---|:---:|---|---|
| `img_story_5_page_1` | 1 | "A vibrant town plaza decorated with colorful banners and lights" | **Fiesta Eve Plaza**: Casiguran town plaza adorned with zigzagging colorful *banderitas* (streamers) and warm paper lanterns. |
| `img_story_5_page_2` | 2 | "Tables full of Filipino food - grilled fish, rice, vegetables..." | **The Grand Banquet**: Long wooden tables laden with native Casiguran delicacies, grilled coastal fish, banana-leaf rice, and tropical fruit punch. |
| `img_story_5_page_3` | 3 | "Colorful street dancing with traditional Filipino costumes..." | **Street Parade**: Vibrant street dancers in festive attire with smiling faces marching alongside a lively brass band. |
| `img_story_5_page_4` | 4 | "The whole community gathered together, fireworks in the night sky..." | **Night Fireworks**: The town gathered together cheering as brilliant multicolored fireworks illuminate the Casiguran sky above the bay. |
| `img_story_5_page_5` | 5 | "A family walking home from the fiesta under a starlit sky..." | **Peaceful Journey Home**: A family walking along a quiet country road under a starlit sky, heartened by community togetherness. |

---

## 🏆 7. Gamification & Leaderboard

* **Code Locations**:
  - [`app/src/main/java/com/kasiguru/ui/screens/leaderboard/LeaderboardScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/leaderboard/LeaderboardScreen.kt#L210-L213) (Gold Stat Card `👑` emoji)
  - [`app/src/main/java/com/kasiguru/ui/screens/leaderboard/LeaderboardScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/leaderboard/LeaderboardScreen.kt#L330-L368) (Top 3 Podium `👑`, `🥈`, `🥉` emojis)
  - [`app/src/main/java/com/kasiguru/ui/screens/leaderboard/LeaderboardScreen.kt`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/java/com/kasiguru/ui/screens/leaderboard/LeaderboardScreen.kt#L474-L476) (Ranked row `👤` emoji)

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `ic_crown_gold` | Podium Trophy Icon | Text emoji `👑` in yellow circle | **Rank #1 Gold Champion Crown**: Glossy 3D royal gold crown adorned with sparkling diamonds and gold aura for the #1 podium position. |
| `ic_medal_silver` | Podium Trophy Icon | Text emoji `🥈` in gray circle | **Rank #2 Silver Medal**: Sleek 3D silver medallion with navy blue ribbon and reflective chrome luster for the #2 podium position. |
| `ic_medal_bronze` | Podium Trophy Icon | Text emoji `🥉` in copper circle | **Rank #3 Bronze Medal**: Warm 3D polished bronze/copper medallion with scarlet red ribbon for the #3 podium position. |
| `ic_ranked_avatar_frame` | User Row Frame | Text emoji `👤` in gray circle | **Ranked User Portrait Ring**: Rounded avatar portrait frame dynamically displaying the user's selected resident persona or initials badge with rank pill overlay. |

---

## 📱 8. App Branding, Launcher & Splash

* **Code Locations**:
  - [`app/src/main/res/drawable/ic_launcher_foreground.xml`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/drawable/ic_launcher_foreground.xml)
  - [`app/src/main/res/drawable/ic_splash.xml`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/drawable/ic_splash.xml)
  - [`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`](file:///C:/KasiGuru/KasiGuru-main/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)

| Asset Identifier | Type | Current Placeholder | Functional Representation & Target Design |
|---|---|---|---|
| `ic_launcher_foreground` / `ic_launcher` | App Icon (Adaptive) | Flat vector letter 'K' with a yellow dot on teal square | **KasiGuru Master App Icon**: Premium 3D app icon featuring an open cultural storybook resting above ocean waves with a rising golden sun and stylized indigenous leaf emblem. |
| `ic_splash` | Splash Screen Brand Emblem | Flat vector letter 'K' with yellow dot | **KasiGuru Brand Identity Emblem**: High-resolution centered emblem combining the stylized book/sun brand mark with elegant typography for the Android 12+ Splash API. |

---

## Technical Integration Specification & Deliverable Checklist

When custom assets are created, they should be exported according to these specifications and integrated into the following directories:

```
app/src/main/res/drawable/
├── avatars/
│   ├── avatar_apo_dumagat.png (512×512)
│   ├── avatar_mang_mateo.png (512×512)
│   ├── avatar_maam_elena.png (512×512)
│   └── ... (all 9 resident characters + 6 personas)
├── badges/
│   ├── ic_badge_level_1.png (256×256)
│   ├── ic_badge_level_2.png (256×256)
│   ├── ic_badge_first_word.png (256×256)
│   └── ... (all 13 level & milestone badges)
├── categories/
│   ├── img_cat_greetings.png (256×256)
│   ├── img_cat_food.png (256×256)
│   ├── img_cat_animals.png (256×256)
│   └── ... (all 12 vocabulary categories)
├── games/
│   ├── ic_game_word_match.png (256×256)
│   ├── ic_game_fill_blank.png (256×256)
│   ├── ic_game_audio_quiz.png (256×256)
│   └── ... (all 6 game tiles + victory trophy)
├── stories/
│   ├── img_story_1_page_1.png (1024×576)
│   ├── ... (all 22 storybook scene illustrations)
└── branding/
    ├── ic_launcher_foreground.xml / png (108×108dp)
    └── ic_splash.xml / png (108×108dp)
```
