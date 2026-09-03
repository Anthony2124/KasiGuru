# Chapter I

> **KasiGuru: A Gamified Mobile Learning Application for the Preservation and Learning of the
> Kasiguranin Language**
> Bihasa, Erickson · Cordial, Anthony T. · Miras, Adrian Rhoman V. · Ruidera, Owen
> Aurora State College of Technology, School of Information Technology · May 2026
>
> Restructured to the format: Rationale · Literature Review · Synthesis · Conceptual Framework ·
> Research Problem · Scope and Delimitations · Significance of the Study · Definition of Terms.
>
> Word formatting: Times New Roman 12, double-spaced, justified, 0.5" first-line indent.

---

## RATIONALE

Language is not merely a system for exchanging information; it is the vessel through which a
community carries its history, its relationship with its environment, and its understanding of
itself. When a language falls out of daily use, what is lost is not only vocabulary but the
categories of thought encoded in it — the distinctions its speakers considered worth naming. UNESCO
has repeatedly warned that a substantial share of the world's languages will cease to be spoken
within this century, and that most of those at risk are community languages without official status,
without a standardized orthography, and without a body of learner-facing material capable of drawing
new speakers in.

The Philippines, with more than a hundred and eighty living languages, is among the countries where
this pressure is most concentrated. Headland (2003) identified thirty Philippine languages already
in advanced stages of endangerment and observed that the mechanism of loss is rarely dramatic. It is
gradual and domestic: a generation of parents, acting reasonably and out of concern for their
children's prospects, chooses to raise them in the language of school, employment, and media rather
than the language of the household.

In the municipality of Casiguran, Aurora, the coastal lowland population speaks **Kasiguranin**, a
non-Negrito Austronesian language sharing an approximately 88% cognacy rate with the neighbouring
indigenous Casiguran Dumagat (Agta). Despite that close lexical relationship, Kasiguranin is
structurally distinct from Agta, and distinct from Tagalog as well. For most of its history the
geography of Casiguran did the work of preservation: the Sierra Madre range separated the
municipality from the Central Luzon plain, and the resulting isolation allowed the language to retain
a predicate-initial syntax, a six-vowel system, word-final glottal stops, contrastive vowel length,
and a morphological system in which verbs inflect for **aspect** — whether an action is begun,
ongoing, completed, or contemplated — rather than for chronological tense.

That isolation has ended. Road infrastructure, mobile connectivity, migration for work and study, and
Tagalog-medium schooling and broadcast media have integrated Casiguran into national linguistic
domains within a single generation. On the Expanded Graded Intergenerational Disruption Scale
(EGIDS), Kasiguranin is classified at **Status 6a (Vigorous)** — it is still learned at home and used
across generations. This study therefore does not claim that Kasiguranin is dying. It claims
something more specific and more actionable.

The observable change is not abandonment but **structural erosion**. Younger speakers increasingly
retain Kasiguranin vocabulary while substituting Tagalog grammatical patterns, and the aspectual verb
system — the feature that most clearly marks Kasiguranin as a language rather than a variety of
Tagalog — is among the first things to go. A speaker who has the words but not the aspect system is,
functionally, speaking Tagalog with a Kasiguranin lexicon. This is precisely the point at which a
"vigorous" classification becomes misleading, because vitality measured by counting speakers does not
detect a language being hollowed out from the inside.

Two conditions accelerate this. First, the absence of documentation accessible to the community has
produced a persistent local belief that Kasiguranin is a *dialect* of Tagalog rather than a language
in its own right — a framing that quietly licenses its replacement, because no one mourns the loss of
an accent. Second, transmission remains almost entirely oral, and oral transmission alone does not
compete for the attention of a generation whose reading, writing, and leisure are mediated by a
phone.

Kasiguranin is not, however, undocumented. Supnet (2016) produced a grammatical sketch of the
language as an undergraduate thesis at the University of the Philippines, Diliman, and that work
remains the primary structural description available. But it is an unpublished academic document
written for linguists: not discoverable by a student in Casiguran, not organized for learning, and
not designed to be returned to daily. The gap is therefore not a gap in *knowledge* but a gap in
**access and form**. The description exists; nothing has translated it into something a learner can
use.

**KasiGuru** addresses that gap. It is an offline-first Android application that converts the
existing scholarly description of Kasiguranin, together with vocabulary elicited from community
speakers, into a structured lexical database, illustrated regional storytelling, spaced-repetition
review, and a set of educational mini-games. Its purpose is threefold: to make the language
learnable by the generation least likely to acquire it at home, to correct the dialect-versus-language
misconception through visible structural evidence, and to establish a durable digital record of a
language that presently exists in one unpublished sketch and in the memory of its speakers.

---

## LITERATURE REVIEW

### Language Endangerment and the Mechanics of Language Shift

Fishman (1991) established the framework that still governs the field with the Graded
Intergenerational Disruption Scale (GIDS), whose central argument is that the decisive variable in
language survival is not the number of speakers but whether the language is transmitted to children
in the home. A language with many adult speakers and no child speakers is further along the path to
extinction than a smaller language still acquired at home. Lewis and Simons (2010) expanded this into
the Expanded GIDS (EGIDS), the thirteen-point scale now used by *Ethnologue*, which introduced the
intermediate statuses — including 6a (Vigorous) and 6b (Threatened) — that allow a language to be
described as still in use but no longer secure.

Crystal (2000) and the UNESCO Ad Hoc Expert Group on Endangered Languages (Brenzinger et al., 2003)
converge on the observation that language shift is typically driven not by prohibition but by
economic and educational incentive, and that communities frequently do not perceive the shift until
transmission has already broken. Headland (2003) documented this pattern specifically for the
Philippines, where regional languages compete with a national language that carries institutional and
economic advantage.

A recurring finding across this literature is that documentation alone does not arrest shift.
Archived grammars and word lists preserve a record for scholarship, but they do not return the
language to use. Revitalization requires that documentation be converted into a form the community
can act on.

### Digital Technology in Language Revitalization

Galla (2016) surveys the function of digital technology in indigenous language revitalization and
argues that technology is most effective when it is community-controlled and integrated into existing
social practice, rather than introduced as a substitute for transmission. Hermes and King (2013),
reporting on Ojibwe multimedia language learning within families, found that digital tools supported
learning most reliably where they created occasions for interaction rather than replacing them.

The literature also identifies a persistent risk: technology projects for endangered languages
frequently produce artifacts that serve researchers rather than speakers — searchable corpora,
archival recordings, and reference grammars that the community itself cannot use. The design
implication drawn consistently is that a revitalization tool must be usable by a non-specialist on
the device that non-specialist already owns.

### Mobile-Assisted Language Learning

Mobile-assisted language learning (MALL) developed from computer-assisted language learning as mobile
device ownership outpaced computer ownership, particularly in developing contexts. Kukulska-Hulme and
Shield (2008) characterize its distinctive advantage as continuity: learning distributed across short,
frequent sessions in the learner's own environment rather than concentrated in a classroom.
Stockwell and Hubbard (2013) propose design principles for the modality, emphasizing that mobile
learning activities must be short enough to complete in the intervals in which phones are actually
used.

Burston (2015), in a meta-analysis of two decades of MALL implementations, reports generally positive
learning outcomes but notes that most studies are short in duration and small in sample, and that
vocabulary acquisition is the skill for which the evidence is strongest. This is directly relevant
here: vocabulary is precisely the domain in which a beginner-level application for an
under-documented language can make a defensible claim.

For a language with no formal curriculum and no allocated instructional hours, the MALL argument is
not one of convenience but of feasibility. There is no classroom period for Kasiguranin to occupy.

### Gamification in Instructional Design

Deterding, Dixon, Khaled and Nacke (2011) provide the definition the field has settled on:
gamification is the use of game design elements in non-game contexts, distinguished from serious games
by being a layer over an existing activity rather than a game in itself. Kapp (2012) develops the
instructional application, arguing that game mechanics address motivation and persistence rather than
comprehension.

Hamari, Koivisto and Sarsa (2014), reviewing empirical studies, report that gamification generally
produces positive effects but that those effects are contingent on context and on the users involved,
and that studies frequently measure engagement rather than learning. Werbach and Hunter (2012)
organize the mechanics themselves into points, badges, leaderboards, levels, and challenges.

The consistent finding is that gamification addresses **attrition, not difficulty**. This matters for
the present study because the failure mode of self-directed language learning is not that learners
find the material too hard; it is that they stop returning.

### Spaced Repetition and Retention

Ebbinghaus (1885/1913) established that forgetting follows a predictable curve and that review timed
against that curve produces retention more efficiently than massed repetition. Cepeda, Pashler, Vul,
Wixted and Rohrer (2006), synthesizing the subsequent literature, confirm the distributed-practice
effect across a large body of verbal recall studies.

Woźniak and Gorzelańczyk (1994) operationalized this as the **SM-2 algorithm**, which assigns each
item an easiness factor and an interval, lengthening the interval after a correct recall and
collapsing it after a failure, so that each item is presented at approximately the moment the learner
is predicted to be about to forget it. SM-2 remains the basis of widely used flashcard systems and is
computationally light enough to run entirely on-device.

Spaced repetition is what separates recognition from retention. A learner who can select the right
answer from four options has not necessarily acquired the word; a learner who recalls it correctly
after progressively longer intervals has.

### Related Systems

Several existing applications establish the design vocabulary this project draws on, and each
illustrates a limitation the present study must address.

**Duolingo** demonstrates gamified progression at scale — streaks, experience points, levels, and
leagues — and is the reference point most learners bring to a language app. Its coverage, however,
is restricted to languages with large learner markets; among Philippine languages it offers Tagalog
only, and its content pipeline presupposes a standardized orthography and a large corpus of existing
material.

**Memrise** combines spaced repetition with user-generated content, showing that community
contribution can extend a corpus. **Anki** implements SM-2 directly and is the most rigorous of the
consumer tools, but it provides no pedagogical scaffolding, no cultural context, and a setup burden
that assumes a motivated, technically confident user. **Drops** demonstrates short-session visual
vocabulary practice but deliberately avoids grammar entirely.

Across these systems three gaps are consistent. None supports any under-documented Philippine
regional language. None is architected offline-first for genuinely intermittent connectivity; all
assume a working network for content delivery. And none combines *documentation* with *learning* —
they teach from a corpus that already exists rather than producing one. No application for
Kasiguranin exists in any form.

### Software Quality Evaluation

ISO/IEC 25010:2011 defines the product quality model used in this study, decomposing software quality
into eight characteristics, of which this evaluation adopts six: functional suitability, performance
efficiency, usability, reliability, security, and maintainability. The standard is widely used in
Philippine capstone evaluation because it provides criteria that can be operationalized into a survey
instrument administered to both users and IT practitioners. A 2023 revision of the standard
restructures several characteristics; this study evaluates against the 2011 model, and the
implications of that choice are acknowledged in the Delimitations.

---

## SYNTHESIS

The reviewed literature converges on five propositions that together define the problem space of this
study.

**First**, language survival is determined by intergenerational transmission rather than by speaker
count (Fishman, 1991; Lewis & Simons, 2010). A language rated Vigorous can nonetheless be losing the
structural features that constitute its distinctiveness, and a vitality measure that counts speakers
will not detect this. Kasiguranin's EGIDS 6a status is therefore not evidence that intervention is
unnecessary; it is evidence that intervention is still possible.

**Second**, documentation and revitalization are different activities (Crystal, 2000; Galla, 2016).
A grammatical sketch preserves knowledge for scholarship without returning the language to use. The
existence of Supnet's (2016) description of Kasiguranin therefore does not close the gap this study
addresses — it supplies the raw material for closing it.

**Third**, mobile learning is the appropriate modality for a language with no curricular allocation
(Kukulska-Hulme & Shield, 2008; Burston, 2015), and the evidence for MALL is strongest precisely in
vocabulary acquisition, which is the domain this study targets.

**Fourth**, gamification addresses persistence rather than comprehension (Deterding et al., 2011;
Kapp, 2012; Hamari et al., 2014). It is therefore the correct instrument for the actual failure mode
of self-directed study, but it is not by itself a learning mechanism.

**Fifth**, retention requires distributed practice (Ebbinghaus, 1885/1913; Cepeda et al., 2006), and
SM-2 (Woźniak & Gorzelańczyk, 1994) operationalizes this in a form light enough to run on-device
without a network.

The gap these propositions define is specific. Existing systems that gamify well do not serve
under-documented languages; systems that implement spaced repetition rigorously provide no scaffolding
or cultural context; and none is built for intermittent connectivity or produces the corpus it
teaches from. For Kasiguranin in particular, a structural description exists but no learner-facing
material of any kind does.

**KasiGuru is positioned in that gap.** It combines the three mechanisms the literature identifies as
individually necessary and jointly absent from existing systems — gamification for persistence,
spaced repetition for retention, and cultural narrative for context — over a corpus that the project
itself digitizes, in an architecture that assumes the network will not be there. The contribution is
therefore not the novelty of any single mechanism, all of which are established, but their
combination and their application to a language for which no such application has previously been
attempted.

---

## CONCEPTUAL FRAMEWORK

This study adopts the **Input–Process–Output (IPO)** model as its conceptual framework, extended with
a feedback path. The IPO model is appropriate here because the study is a development study: it
transforms identified resources and requirements into a software artifact and an evaluation of that
artifact.

**Input** comprises the raw material and the constraints. The linguistic material consists of
Supnet's (2016) grammatical sketch of Kasiguranin together with vocabulary and narratives elicited
from community speakers and culture bearers. The requirements material consists of the needs of
student and beginner learners and of educators in Casiguran. The evaluative material consists of the
ISO/IEC 25010 quality characteristics. The technical constraints consist of the target platform and
the connectivity conditions of the deployment site.

**Process** comprises the development methodology and the design mechanisms it applies. The study
follows the **Phased Development Approach** through requirements, analysis, design,
coding/implementation, testing, deployment, and maintenance. Within that methodology, four design
mechanisms operate on the input: digitization and semantic categorization of the lexicon;
scheduling of review by the SM-2 spaced-repetition algorithm; application of gamification mechanics
for persistence; and an offline-first data architecture with opportunistic synchronization. The
process concludes with evaluation against ISO/IEC 25010 and an acceptability survey administered to
the target users.

**Output** comprises the developed KasiGuru mobile application, the digitized Kasiguranin lexical
corpus and narrative collection as a preservation artifact in its own right, the resulting software
quality evaluation, and the measured level of user acceptability.

**The feedback path** is what distinguishes this framework from a linear development model. Two
returns operate continuously. Evaluation results and usability findings re-enter the process as
revised requirements, consistent with the iterative character of the Phased Development Approach.
More importantly, the application's moderated contribution pipeline returns *new linguistic data* to
the input: community members submit Kasiguranin words that, after expert review, enter the corpus.
The corpus is therefore not a fixed input consumed once but a living resource the community extends,
which is the mechanism by which the artifact continues to serve preservation after the study
concludes.

*(See Figure 1: Conceptual framework of the study.)*

**Figure 1 described.** Three panels left to right. **Input** — Supnet (2016) grammatical sketch;
community informant elicitation; regional folk narratives; learner and educator requirements;
ISO/IEC 25010 criteria; Android platform and connectivity constraints. An arrow labelled *feeds*
leads to **Process** — Phased Development Approach; corpus digitization and categorization; SM-2
review scheduling; gamification mechanics; offline-first synchronization; moderated contribution
review; ISO/IEC 25010 and acceptability evaluation. An arrow labelled *yields* leads to **Output** —
the KasiGuru Android application; the digitized Kasiguranin corpus; evaluated software quality;
measured user acceptability. A return arrow runs from Output beneath both panels back to Input,
labelled *evaluation findings and reviewed community submissions*.

---

## RESEARCH PROBLEM

The Kasiguranin language is spoken across generations in Casiguran, Aurora, yet exists in no form its
own young speakers can learn from. Its only structural description is an unpublished academic
document; its transmission is entirely oral; and the absence of accessible documentation sustains a
local misconception that it is a dialect of Tagalog rather than a distinct language. No learning
application, in any form, exists for it.

This study therefore aims to develop and evaluate **KasiGuru**, a gamified mobile learning
application for the preservation and learning of the Kasiguranin language.

Specifically, it seeks to answer the following questions:

**1. How may the application be developed using the Phased Development Approach in terms of:**

- 1.1 Requirements;
- 1.2 Analysis;
- 1.3 Design;
- 1.4 Coding/Implementation;
- 1.5 Testing;
- 1.6 Deployment; and
- 1.7 Maintenance?

**2. How may the application be evaluated based on the ISO/IEC 25010 Software Quality Standards in
terms of:**

- 2.1 Functional Suitability;
- 2.2 Performance Efficiency;
- 2.3 Usability;
- 2.4 Reliability;
- 2.5 Security; and
- 2.6 Maintainability?

**3. What is the level of acceptability and user satisfaction of the application among students and
beginner learners of Kasiguranin?**

---

## SCOPE AND DELIMITATIONS

### Scope

The study covers the design, development, and evaluation of KasiGuru as a native Android application
for beginner-level learners of Kasiguranin. As delivered, the system consists of:

- **A lexical database of 1,202 entries covering 1,076 distinct headwords**, organized into twelve
  semantic categories — Body Parts & Health (197), Greetings & Essentials (194), House & Daily Life
  (179), Nature & Environment (116), Food & Dining (97), Animals & Wildlife (93), Numbers & Time
  (93), Emotions & Feelings (69), Family & People (53), Occupations & Tools (41), Colors & Shapes
  (39), and Weather & Climate (31). Each entry carries its Kasiguranin form, Tagalog and English
  equivalents, root form, semantic category, and definitional glosses in both Tagalog and English.
- **Phonetic documentation** in International Phonetic Alphabet notation, with explicit markers for
  word-final glottal stop and contrastive vowel length, on the subset of entries transcribed to date.
- **Interactive storytelling**: ten illustrated regional narratives with tap-to-define word lookup
  against the dictionary.
- **A lesson system** generating exercise sequences from the corpus by semantic unit, with
  answer-level feedback.
- **Spaced-repetition review** using SM-2, including relearning steps and lapse tracking.
- **Six educational mini-games** — Word Match, Reverse Match, Fill in the Blank, Word Recall,
  Sentence Order, and Aspect Builder — with star-based level progression.
- **A gamified progression system** of experience points, five progression levels, daily streaks,
  twenty-five achievements, and a public leaderboard.
- **An offline-first data layer** in which the on-device database is authoritative and all learning
  functions operate without connectivity.
- **A moderated community contribution pipeline** for learner-submitted vocabulary.
- **An administrative web portal** for corpus management, submission moderation, and release
  publishing.

The application targets Android 8.0 (API 26) and above and is distributed as a directly installed
Android package rather than through a public application store. Evaluation is conducted with student
and beginner-learner respondents in Casiguran, Aurora.

### Delimitations

The following boundaries were set deliberately by the researchers and define what the study does not
attempt.

1. **Beginner-level pedagogy only.** The study is delimited to vocabulary, phonology, and basic
   sentence construction. Complex verbal focus subcategorization and advanced discourse structure are
   excluded, as they exceed what a beginner-level application can teach and what the available
   documentation supports.

2. **Aspectual verb data is reserved for future work.** The database schema provides fields for all
   four aspectual inflections and the Aspect Builder mini-game is implemented against them, but the
   aspect dataset is still being documented with language experts and is not populated in the
   evaluated build. The application handles this state explicitly and the game remains unavailable
   until the dataset is supplied. Documenting aspect requires elicitation work with native speakers
   beyond the timeframe of this study.

3. **The audio corpus is excluded from the evaluated build.** Playback infrastructure is implemented,
   but native-speaker recordings are not bundled; phonetic guidance is delivered through IPA
   transcription and phonological markers instead. Recording is constrained by the availability of
   native informants and of recording equipment.

4. **No automated language generation or processing.** The system includes no natural language
   processing, speech recognition, or machine translation, and does not generate, infer, or translate
   Kasiguranin content automatically. This exclusion is deliberate rather than incidental:
   automatically generated Kasiguranin would fabricate primary linguistic data for an
   under-documented language and would compromise the corpus as a preservation record.

5. **Content is bounded to expert-reviewed material.** Only vocabulary and narratives that have
   passed review are included; the corpus is not scraped or crowd-sourced without moderation.

6. **Android only.** iOS and web clients are outside the scope. The choice reflects device ownership
   at the deployment site.

7. **Evaluation against the 2011 edition of ISO/IEC 25010.** The standard was revised in 2023; this
   study adopts the 2011 model because it remains the edition in general use for capstone evaluation
   and because its six adopted characteristics map to an instrument that respondents can answer.

8. **No longitudinal measurement.** The study measures software quality and user acceptability. It
   does not measure long-term language acquisition outcomes or effects on intergenerational
   transmission, which would require a longitudinal design beyond the study's timeframe.

---

## SIGNIFICANCE OF THE STUDY

**Students and Beginner Learners.** The primary beneficiaries receive a free, offline, structured
means of learning Kasiguranin where none previously existed. For learners who already speak Tagalog,
the application makes the structural differences between the two languages explicit rather than
leaving them to be inferred, addressing directly the misconception that Kasiguranin is a Tagalog
dialect.

**Educators.** Teachers in Casiguran receive supplementary instructional material aligned with the
aims of Mother Tongue-Based Multilingual Education, for a language that currently has no published
learner-facing resources. The categorized lexicon and story collection are usable as classroom
material independently of the application's gamified layer.

**The Local Community and Culture Bearers.** The study produces a durable digital record of
Kasiguranin vocabulary and narrative that outlives any individual speaker, in a form the community
itself can read. The moderated contribution pipeline allows community members to submit words for
expert review, making the corpus a record the community extends rather than an artifact produced
about them.

**The Local Government and Cultural Workers.** The digitized corpus and story collection provide a
foundation for cultural programming, heritage documentation, and tourism material, and constitute
evidence of linguistic distinctiveness usable in advocacy for local language recognition.

**IT Researchers and Developers.** The study contributes a documented technical reference for
mobile-assisted language learning applied to a low-resource, under-documented language: an
offline-first architecture, an on-device spaced-repetition implementation over a community-sourced
corpus, and a moderated contribution pipeline. These are transferable to the many other Philippine
languages in a comparable position.

**Future Researchers.** The study establishes a baseline for subsequent work on Kasiguranin,
including the aspectual verb dataset, the audio corpus, and advanced-level pedagogy that this study
deliberately leaves as future work.

---

## DEFINITION OF TERMS

The following terms are defined conceptually and, where applicable, operationally as used in this
study.

**Achievement.** A named award granted for reaching a defined milestone. *Operationally*, one of
twenty-five records in the application that unlocks when its condition is met and grants experience
points.

**Android Package (APK).** The file format in which an Android application is distributed and
installed. *Operationally*, the form in which KasiGuru is delivered, installed directly rather than
through a public application store.

**Aspect.** A grammatical category expressing the internal temporal structure of an action — whether
it is begun, ongoing, completed, or contemplated — as distinct from tense, which locates an action in
time. *Operationally*, the four inflectional forms (neutral, imperfective, perfective, contemplative)
for which the lexical database provides fields.

**Casiguran.** A first-class municipality in the province of Aurora, Philippines, situated on the
Pacific coast behind the Sierra Madre range. The site of this study.

**Cognacy Rate.** The proportion of vocabulary in two languages descended from a common ancestral
form, used as a measure of relatedness. Kasiguranin shares an approximately 88% cognacy rate with
Casiguran Dumagat (Agta).

**Delimitation.** A boundary the researchers set deliberately, distinguishing what the study chose
not to address from a limitation, which is a constraint outside the researchers' control.

**Expanded Graded Intergenerational Disruption Scale (EGIDS).** A thirteen-point scale for describing
language vitality by degree of disruption to intergenerational transmission (Lewis & Simons, 2010).
Kasiguranin is classified at Status 6a (Vigorous).

**Experience Points (XP).** A numeric measure of cumulative learning activity. *Operationally*, points
awarded for completed lessons, reviews, games, and stories, which determine the learner's progression
level.

**Gamification.** The use of game design elements in non-game contexts (Deterding et al., 2011).
*Operationally*, the experience point, level, streak, achievement, and leaderboard systems layered
over the application's learning activities.

**Glottal Stop.** A consonant produced by closing the vocal folds, phonemic in Kasiguranin and
frequently word-final. *Operationally*, a marked property of a dictionary entry, rendered in the
entry's IPA transcription.

**Input–Process–Output (IPO) Model.** A conceptual framework representing a system as resources
entering a transformation and producing results. Adopted as this study's conceptual framework, with
an added feedback path.

**International Phonetic Alphabet (IPA).** A standardized system of phonetic notation.
*Operationally*, the transcription attached to entries in the lexical database, used to convey
pronunciation in the absence of recorded audio.

**ISO/IEC 25010.** An international standard defining a software product quality model.
*Operationally*, the six characteristics against which the application is evaluated — functional
suitability, performance efficiency, usability, reliability, security, and maintainability.

**Kasiguranin.** A non-Negrito Austronesian language spoken by the coastal lowland population of
Casiguran, Aurora, characterized by predicate-initial syntax, a six-vowel system, aspectual verb
morphology, word-final glottal stops, and contrastive vowel length. The subject language of this
study.

**Lexical Database.** A structured, queryable collection of vocabulary entries. *Operationally*, the
on-device table of 1,202 Kasiguranin entries with their translations, root forms, categories, glosses,
and phonetic data.

**Mobile-Assisted Language Learning (MALL).** Language learning supported by mobile devices,
characterized by short sessions distributed across the learner's own environment
(Kukulska-Hulme & Shield, 2008).

**Offline-First.** An architecture in which the local device store is the authoritative source of
data and network access is optional. *Operationally*, all learning functions in KasiGuru operate with
no connection; synchronization to the cloud is opportunistic.

**Phased Development Approach.** A software development methodology proceeding through requirements,
analysis, design, coding/implementation, testing, deployment, and maintenance, permitting iteration
between phases. The methodology followed in this study.

**Root Form.** The base morphological form of a word before inflection. *Operationally*, a stored
field on each dictionary entry.

**SM-2 Algorithm.** A spaced-repetition scheduling algorithm that assigns each item an easiness
factor and review interval, lengthening the interval after a correct recall and shortening it after a
failure (Woźniak & Gorzelańczyk, 1994). *Operationally*, the algorithm scheduling vocabulary review
in KasiGuru.

**Spaced Repetition.** A learning technique in which review is distributed over increasing intervals
timed against the forgetting curve, producing durable retention more efficiently than massed
repetition.

**Streak.** A count of consecutive days on which the learner completed at least one activity.
*Operationally*, a motivational counter that increments daily and resets when a day passes with no
completed activity.

**Vowel Length.** Contrastive duration of a vowel, phonemic in Kasiguranin. *Operationally*, a marked
property of a dictionary entry, rendered in the entry's IPA transcription.

---

## REFERENCES

Brenzinger, M., Yamamoto, A., Aikawa, N., Koundiouba, D., Minasyan, A., Dwyer, A., Grinevald, C.,
Krauss, M., Miyaoka, O., Sakiyama, O., Smeets, R., & Zepeda, O. (2003). *Language vitality and
endangerment*. UNESCO Ad Hoc Expert Group on Endangered Languages.

Burston, J. (2015). Twenty years of MALL project implementation: A meta-analysis of learning
outcomes. *ReCALL, 27*(1), 4–20.

Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). Distributed practice in
verbal recall tasks: A review and quantitative synthesis. *Psychological Bulletin, 132*(3), 354–380.

Crystal, D. (2000). *Language death*. Cambridge University Press.

Deterding, S., Dixon, D., Khaled, R., & Nacke, L. (2011). From game design elements to gamefulness:
Defining "gamification." In *Proceedings of the 15th International Academic MindTrek Conference:
Envisioning Future Media Environments* (pp. 9–15). Association for Computing Machinery.

Ebbinghaus, H. (1913). *Memory: A contribution to experimental psychology* (H. A. Ruger & C. E.
Bussenius, Trans.). Teachers College, Columbia University. (Original work published 1885)

Fishman, J. A. (1991). *Reversing language shift: Theoretical and empirical foundations of assistance
to threatened languages*. Multilingual Matters.

Galla, C. K. (2016). Indigenous language revitalization, promotion, and education: Function of
digital technology. *Computer Assisted Language Learning, 29*(7), 1137–1151.

Hamari, J., Koivisto, J., & Sarsa, H. (2014). Does gamification work? A literature review of
empirical studies on gamification. In *Proceedings of the 47th Hawaii International Conference on
System Sciences* (pp. 3025–3034). IEEE.

Headland, T. N. (2003). Thirty endangered languages in the Philippines. *Work Papers of the Summer
Institute of Linguistics, University of North Dakota Session, 47*.

Hermes, M., & King, K. A. (2013). Ojibwe language revitalization, multimedia technology, and family
language learning. *Language Learning & Technology, 17*(1), 125–144.

International Organization for Standardization. (2011). *ISO/IEC 25010:2011 — Systems and software
engineering — Systems and Software Quality Requirements and Evaluation (SQuaRE) — System and software
quality models*.

Kapp, K. M. (2012). *The gamification of learning and instruction: Game-based methods and strategies
for training and education*. Pfeiffer.

Kukulska-Hulme, A., & Shield, L. (2008). An overview of mobile assisted language learning: From
content delivery to supported collaboration and interaction. *ReCALL, 20*(3), 271–289.

Lewis, M. P., & Simons, G. F. (2010). Assessing endangerment: Expanding Fishman's GIDS. *Revue
Roumaine de Linguistique, 55*(2), 103–120.

Stockwell, G., & Hubbard, P. (2013). *Some emerging principles for mobile-assisted language
learning*. The International Research Foundation for English Language Education.

Supnet, C. P. E. (2016). *A grammatical sketch of Kasiguranin* [Unpublished undergraduate thesis].
University of the Philippines, Diliman, Quezon City.

UNESCO. (2021). *World report of languages: Towards global language sustainability*.

Werbach, K., & Hunter, D. (2012). *For the win: How game thinking can revolutionize your business*.
Wharton Digital Press.

Woźniak, P. A., & Gorzelańczyk, E. J. (1994). Optimization of repetition spacing in the practice of
learning. *Acta Neurobiologiae Experimentalis, 54*(1), 59–62.
