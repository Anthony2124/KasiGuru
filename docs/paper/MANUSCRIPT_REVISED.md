<!-- Revised proposal manuscript. Chapters follow the supplied table of contents.
     Evaluation is planned. Bracketed details require researcher confirmation.
     The original Word manuscript and chapter drafts are preserved. -->

# Chapter 1

THE PROBLEM AND ITS BACKGROUND

## Rationale

Language connects people with the knowledge, experiences, and cultural practices of their community. Preserving a language therefore involves more than storing its words; it also involves making those words accessible to people who wish to learn and use them. In Casiguran, Aurora, Kasiguranin provides an important focus for local language documentation and learning. This study approaches its preservation through the development of a mobile application that organizes documented vocabulary and supports regular beginner practice.

Access to learner-oriented materials and opportunities for practice motivates the project. Community consultations will examine these needs alongside participants' use of Kasiguranin, Tagalog, and English. The study will use requirements consultations to clarify learners' experiences and priorities rather than assume that the community has already lost particular linguistic features.

Supnet's (2016) grammatical sketch provides a scholarly reference for the project. Such documentation can inform the organization of a lexical database and the selection of linguistic information for learners. Its use in a mobile application nevertheless requires careful adaptation: linguistic descriptions must be represented accurately, vocabulary must retain its source, and new material must be reviewed by knowledgeable speakers. The application will not generate Kasiguranin words, translations, or narratives to fill gaps in the available material.

Mobile learning offers a practical way to make supplementary activities available on devices learners already use. Short lessons, vocabulary lookup, review sessions, and educational games can provide opportunities for practice outside formal instruction. An offline-first design is particularly relevant to the project's intended use because it allows downloaded learning content and locally recorded progress to remain accessible when a network connection is unavailable.

KasiGuru combines these functions in an Android application supported by an administrative web portal. Its learning design includes vocabulary activities, spaced repetition, and game elements such as experience points, achievements, and progress indicators. These mechanisms are intended to encourage continued practice. Their inclusion does not establish that the application improves language proficiency or prevents language loss; those outcomes would require evidence beyond the planned software quality and acceptability evaluation.

This study will develop and evaluate KasiGuru as a supplementary resource for students and beginner learners of Kasiguranin. It will document the development process, assess selected software quality characteristics, and describe user acceptability and satisfaction. Its contribution will be a functioning learning resource and a documented approach to organizing community-sourced linguistic material, together with evaluation findings that can guide further improvement.

## Literature Review

### Language Documentation and Community Learning

Headland (2003) discusses threats affecting small Philippine Negrito language communities, drawing attention to the relationship between language survival and the wider circumstances of speakers. His study concerns those communities specifically; it should not be treated as evidence that Kasiguranin has the same vitality status. For the present project, it provides context for the importance of language documentation and for the need to distinguish general preservation concerns from locally established findings.

Hermes and King (2013) examine multimedia technology and family language learning in an Ojibwe revitalization context. Their work connects the use of digital materials with interactions among learners and family members. The implication for KasiGuru is that a mobile resource should support opportunities to engage with speakers and community knowledge. Application use will be treated as supplementary practice, while linguistic validation will remain a responsibility of knowledgeable human contributors.

Supnet (2016) is retained from the original manuscript as the project's principal Kasiguranin grammatical reference. The researchers will check the original thesis when adapting linguistic descriptions and will keep a record of the material used. Community review will complement documentary sources, especially where a vocabulary entry, pronunciation, example sentence, or aspectual form requires clarification.

### Mobile-Assisted Language Learning

Kukulska-Hulme and Shield (2008) review mobile-assisted language learning, including the movement from content delivery toward interaction and collaboration. Their discussion supports consideration of how learners use mobile devices in their own environments. KasiGuru applies this perspective through activities that learners can revisit and through vocabulary lookup that can accompany independent practice. The study will evaluate whether respondents find these functions usable and relevant; it will not infer learning effectiveness from the presence of mobile delivery alone.

### Gamification

Deterding et al. (2011) define gamification in terms of applying game design elements in contexts outside games. In KasiGuru, this approach appears in experience points, achievements, streaks, level progression, and feedback associated with learning activities. These features organize and reward participation without replacing the language content itself.

Hamari et al. (2014) review empirical studies of gamification and report that outcomes depend on the context and the users involved. This finding supports evaluating the application's motivational features with its intended respondents instead of assuming that rewards will have the same effect in every learning setting. The acceptability instrument will therefore include perceived motivation and willingness to continue using the application, while keeping these perceptions separate from measured language acquisition.

### Spaced Repetition

Cepeda et al. (2006) synthesize research on distributed practice in verbal recall. Their findings support spacing learning opportunities over time and show that the relationship between practice intervals and retention depends on the intended retention period. This provides a basis for including scheduled vocabulary review in a learning application.

The SuperMemo method uses performance-sensitive scheduling to determine review intervals (SuperMemo, n.d.). KasiGuru implements an SM-2-based scheduler locally, with support for relearning and lapse tracking. The scheduler determines when review items become due from the learner's recorded responses. It is a design mechanism for organizing practice; the planned evaluation will assess its operation and usability rather than claim that its educational effect has already been demonstrated in the Kasiguranin context.

### Software Quality Evaluation

ISO/IEC 25010:2011 defines a software product quality model with eight characteristics. This study retains the six characteristics specified in the original research questions: functional suitability, performance efficiency, usability, reliability, security, and maintainability. Compatibility and portability are outside the selected evaluation criteria. The researchers will identify the adopted edition explicitly because the 2011 standard has been withdrawn and superseded; retaining it is a bounded methodological choice, not a claim that it is the current edition (International Organization for Standardization, 2011).

The questionnaire will be adapted to observable features and supported by relevant testing records. User ratings will describe experience with the application. Technical assessments will also draw on practitioner review and evidence from the system. Favorable survey ratings alone will not establish security, maintainability, or conformity with every requirement of the standard.

## Synthesis

The reviewed literature supports a design that connects documentation, accessible practice, and community participation. Research on language-related multimedia suggests that digital materials can support interactions with speakers. Mobile-assisted learning provides a basis for making activities available in learners' everyday environments. Gamification offers mechanisms that may support participation, while distributed practice provides a basis for scheduling repeated vocabulary review.

These findings do not establish that the same results will occur in Casiguran. The studies concern different languages, users, and research settings. KasiGuru will therefore apply their design implications cautiously and evaluate the resulting application with its intended users. The project will retain source attribution and human review as safeguards for linguistic accuracy.

The gap addressed by the study is the need to organize the project's documented Kasiguranin material into a usable beginner learning resource. It is not necessary to claim that no other learning materials or applications exist to establish this need. The proposed contribution is the integration of a local lexical database, structured practice, scheduled review, gamification, and moderated content management in one Android system.

Software quality and user acceptability provide the appropriate initial evaluation targets. Evidence about long-term retention, changes in language use, or intergenerational transmission will require further research. The conceptual framework and methodology that follow translate this bounded contribution into development activities and a planned evaluation.

## Conceptual Framework

The study adopts the Input–Process–Output (IPO) model with a feedback path. This framework relates the resources and requirements available to the researchers to the development activities and the outputs to be evaluated. Figure 1 illustrates these relationships. Its outputs represent the intended results of the study; evaluation findings and fully validated narrative content have not yet been produced.

**Input.** Inputs include Supnet's (2016) grammatical sketch; sourced vocabulary and community contributions; learner and educator requirements; the six adopted ISO/IEC 25010:2011 characteristics; and the Android platform and connectivity conditions. Narrative material and pronunciation recordings will be included only when available, authorized, and reviewed. Unconfirmed content will not be presented as part of the evaluated learning corpus.

**Process.** The development process follows the Phased Development Approach through requirements, analysis, design, coding/implementation, testing, deployment, and maintenance. It includes corpus organization, lesson and game implementation, SM-2-based review scheduling, gamification, local data storage, synchronization, and administrative content management. Evaluation will follow a defined period of use and will assess the selected software quality characteristics and user acceptability.

**Output.** Outputs will include the KasiGuru Android application, its supporting administrative portal, the versioned lexical corpus available in the evaluation build, and the resulting evaluation report. A narrative collection is an intended extension and will be counted as an evaluated output only if validated material is enabled in the selected build. The report will identify unavailable features and explain the resulting limits of the findings.

**Feedback.** Prototype observations and evaluation recommendations will inform corrections and revised requirements. Community contributions will return to the content review process before entering the shared corpus. A submission will not become authoritative linguistic material solely because it has been entered through the application.

[FIGURE_1]

## Research Problem

The study addresses the need for an accessible supplementary resource for beginner learning and documentation of Kasiguranin. It aims to develop and evaluate KasiGuru while distinguishing the quality of the software from broader outcomes such as language proficiency or preservation over time.

Specifically, the study seeks to answer the following questions:

1. How may KasiGuru be developed using the Phased Development Approach in terms of requirements, analysis, design, coding/implementation, testing, deployment, and maintenance?
2. How may KasiGuru be evaluated using the selected ISO/IEC 25010:2011 characteristics of functional suitability, performance efficiency, usability, reliability, security, and maintainability?
3. What will be the level of acceptability and user satisfaction among participating students and beginner learners of Kasiguranin?

### Objectives of the Study

The general objective is to develop and evaluate KasiGuru as a gamified Android application for supplementary beginner learning and digital documentation of Kasiguranin.

The specific objectives are to document the seven development phases; implement the vocabulary, lesson, review, game, progress, and administrative functions included in the evaluation build; evaluate the six adopted software quality characteristics; and determine respondents' reported acceptability and satisfaction. Any cultural narrative or pronunciation material will be included subject to availability, permission, and linguistic review.

## Scope and Delimitations

The study covers the development and planned evaluation of KasiGuru for beginner learners in Casiguran, Aurora. The mobile application targets Android 8.0 (API 26) and above. The supporting administrative portal serves authorized content managers and is part of the system architecture; the project does not provide an equivalent web or iOS learner application.

The evaluated functions will be drawn from the selected build and will include dictionary lookup, categorized vocabulary, lessons, scheduled review, educational games, progress records, and gamification where available. Local content and progress will support offline learning after installation and content preparation. Authentication, shared rankings, remote content changes, submissions, and synchronization may require connectivity. Offline-first operation will not be described as meaning that every service works without the internet.

The current project includes eight game modules: Word Match, Reverse Match, Fill in the Blank, Word Recall, Sentence Order, Aspect Builder, Word Search, and Word Wheel. Module existence does not establish readiness for participant use. Stories are currently disabled, and Aspect Builder is marked as coming soon. Neither will be rated as a completed learning feature unless it becomes available with reviewed content before the evaluation build is finalized.

The scope includes documenting a versioned inventory of the corpus and enabled features immediately before data collection. The final inventory will state the vocabulary count, available recordings, example sentences, and relevant content gaps. This inventory will establish the content and functions actually available to respondents during the evaluation.

### Delimitations

The study is delimited to beginner vocabulary and supported foundational activities. Advanced linguistic analysis, automated translation, speech recognition, and automatically generated Kasiguranin content are excluded. Community submissions will require review before publication. The application will supplement learning and will not replace speaker-led instruction.

Evaluation will use a purposively selected local sample and the six specified characteristics of the 2011 software quality model. User acceptability will be assessed through reported experience after guided use. The design will not test a causal hypothesis, compare the application with a control group, or measure long-term proficiency, language retention, or changes in intergenerational transmission.

### Limitations

The study may be constrained by the availability of community reviewers, recordings, validated sentences, and complete aspectual forms. Device differences, connectivity, and the duration of participant use may also affect the evaluation. The actual constraints encountered will be documented after data collection rather than described in advance as completed findings. Results will apply most directly to the respondents, build, content, and conditions studied.

## Significance of the Study

**Students and beginner learners.** KasiGuru may provide a convenient supplementary resource for vocabulary lookup and repeated practice. Its local learning functions can support use when connectivity is intermittent. Evaluation will help determine whether the intended learners find these functions accessible and useful.

**Educators.** The organized vocabulary and activities may support supplementary instruction and learner practice. Teachers' feedback can identify unclear content and interaction patterns that need revision. The study will not claim alignment with a mandated curriculum unless that alignment is separately established.

**Community speakers and culture bearers.** The project provides a way to organize contributed linguistic material with review and attribution. Contributors can help correct entries and determine whether material is appropriate for public use. Preservation value depends on accurate documentation and continued community participation.

**Local cultural workers.** A reviewed digital corpus may support subsequent documentation and educational initiatives. Availability for reuse will remain subject to the permissions and restrictions attached to source material.

**Researchers and developers.** The documented architecture, development process, and evaluation may provide a reference for similar applications using local language resources. Future studies can extend the work through fuller content validation, pronunciation recordings, and research on learning outcomes over time.

## Definition of Terms

**Acceptability.** In this study, respondents' reported judgments about the application's usefulness, cultural relevance, and suitability for continued use, measured through the planned questionnaire.

**Aspect.** A grammatical category concerned with how an action or event unfolds. In the application, aspectual fields store documented forms where those forms have been supplied and reviewed; complete coverage is not assumed.

**Content validation.** Review of research instruments or linguistic material for relevance, clarity, and accuracy by appropriately qualified reviewers. Instrument review and linguistic review are separate activities in this study.

**Gamification.** The application of game design elements to activities outside games (Deterding et al., 2011). Operationally, it refers to the rewards and progress mechanisms associated with KasiGuru learning activities.

**Input–Process–Output model.** The framework connecting the study's resources and requirements, development activities, and intended outputs, with a feedback path for revisions and reviewed contributions.

**ISO/IEC 25010:2011.** The adopted edition of the software quality model. The study evaluates six selected product quality characteristics and does not claim full certification or use of the current edition.

**Kasiguranin.** The language documented and taught through the reviewed learning material used by this project in Casiguran, Aurora. The manuscript uses “language” consistently with its existing title and source reference.

**Lexical corpus.** The collection of sourced vocabulary entries and associated linguistic information prepared for use in the application. The evaluated corpus will be identified by its version and documented inventory.

**Mobile-assisted language learning.** Language learning supported by mobile devices. In this study, it refers to the supplementary vocabulary and practice activities delivered through KasiGuru.

**Offline-first.** A design in which locally available content and progress support core learning activities without continuous connectivity, while specified remote services require network access.

**Phased Development Approach.** The project's organization of software development into requirements, analysis, design, coding/implementation, testing, deployment, and maintenance, with revisions between phases when needed.

**SM-2-based review.** Vocabulary review scheduling that uses recorded learner responses to update review intervals and item difficulty through the application's SM-2 implementation.

**User satisfaction.** Respondents' reported experience of using the application, including ease, perceived value, and willingness to continue, as measured after the planned period of use.

# Chapter 2

RESEARCH METHODOLOGY

This chapter describes the research design, study setting, intended respondents, instruments, data gathering procedure, analysis, and ethical safeguards. Development descriptions refer to the current software where verifiable. Participant recruitment, instrument validation, data collection, and evaluation remain planned activities and are described accordingly.

## Research Design

The study will use a descriptive–developmental design. The developmental component concerns constructing and refining KasiGuru through the Phased Development Approach. The descriptive component will summarize software quality assessments and user acceptability after a defined period of use. The design will not establish that application use causes improvement in language proficiency.

Research Question 1 will be addressed through documented development activities and their outputs. Research Question 2 will be addressed through a technical evaluation informed by the six selected software quality characteristics and supported by testing evidence. Research Question 3 will be addressed through user responses on acceptability and satisfaction. Prototype feedback will guide revisions but will be distinguished from the final survey dataset.

### Project Design and Development

**Requirements.** The researchers will consolidate learner and educator requirements through consultations, review documentary sources, and identify the linguistic content available for the application. Requirement records will state the source of each need and whether a proposed feature depends on further content collection.

**Analysis.** Requirements will be translated into learning tasks, system modules, data needs, and evaluation criteria. Analysis will consider device capability, offline access, connectivity-dependent services, and the distinction between completed functions and planned extensions.

**Design.** The researchers will document the data model, navigation, interface organization, review scheduling, rewards, and synchronization behavior. The design will specify how unavailable linguistic content is handled so that incomplete material is not substituted with invented language data.

**Coding/Implementation.** The current Android application uses Kotlin and Jetpack Compose, with local storage managed through Room. Repositories connect local data with learning and interface functions. An SM-2-based scheduler supports review, and the web portal supports content management and moderation. Further changes will be recorded under version control.

**Testing.** The researchers will conduct relevant unit, database migration, and system tests and retain their results. System testing will include installation, dictionary lookup, lesson completion, due review, enabled games, progress persistence, and behavior when connectivity is lost. Prototype usability sessions will identify interaction problems before the main evaluation. Existing test files will not be treated as evidence that a check passed unless a result is recorded.

**Deployment.** A signed Android package will be selected for evaluation and distributed through authorized installation or download arrangements. Its version, release date, content inventory, and enabled modules will be recorded. The study will use that identified build unless an essential correction requires a documented replacement.

**Maintenance.** Defects and recommendations will be entered into an improvement record. Corrective work will be prioritized, retested, and distinguished from the behavior of the build already evaluated. New community content will pass through review before publication.

### System Architecture and Modules

The system comprises an Android client, supporting Firebase services, and an administrative web portal. The mobile client contains interface, learning logic, and data components. Room stores locally available content and learning records. Firebase supports functions such as account services and synchronization when connectivity is available. The administrative portal manages authorized content changes and submitted material.

Learning records will be saved locally before synchronization so that interruptions do not require the learner to repeat a completed activity merely to recover local progress. Remote updates and cross-device behavior will be included in system testing where applicable. Claims about these behaviors will be supported by the recorded results for the evaluated build.

Core modules include vocabulary lookup, categorized lessons, scheduled review, enabled educational games, progress and achievements, and account settings. Administrative functions include content editing and submission moderation. Stories and Aspect Builder will remain outside completed-feature ratings while unavailable. The researchers will disclose this distinction during orientation and in the final feature inventory.

### Design Considerations

The design will prioritize clear navigation, readable text, understandable feedback, and manageable activity length. Accessibility checks will consider text enlargement, contrast, and interactive control labels. Device and connectivity tests will examine the conditions relevant to the intended respondents. Linguistic accuracy will remain dependent on source checking and community review, while personal data collection will be limited to what the application and research require.

## Locale of the Study

The study will be conducted in Casiguran, Aurora, Philippines, where the researchers intend to consult community speakers and recruit students and beginner learners of Kasiguranin. Participant activities will take place at [PARTNER SCHOOL OR INSTITUTION], subject to permission and availability. The evaluation period will be [START DATE–END DATE].

The researchers will describe the venue, installation arrangements, available devices, and connectivity conditions when the site is confirmed. Development work may occur outside the participant venue. Site selection reflects the project's target community and access to relevant contributors; it does not make the sample representative of all Kasiguranin learners.

## Respondents

The planned evaluation will involve learner-respondents and IT practitioner-respondents. Linguistic contributors and content validators will have a separate role. Their review will establish content readiness and will not be combined with questionnaire scores unless they also meet the criteria for a clearly identified evaluation group.

**Learner-respondents.** The researchers will purposively recruit [TARGET NUMBER OF LEARNER-RESPONDENTS] students or beginner learners who reside or study in the intended community, can understand the language of the questionnaire, and can use a compatible Android device with any required assistance. Beginner status and prior exposure will be recorded using the approved eligibility questions. Participants may use their own devices or devices provided for the study.

**IT practitioner-respondents.** The study will invite [TARGET NUMBER OF IT PRACTITIONER-RESPONDENTS] practitioners or instructors with [SPECIFIED EXPERIENCE OR QUALIFICATION]. They will examine the relevant functions and testing documentation before completing the technical instrument. Technical criteria will not be assigned to respondents who have no reasonable basis for judging them.

**Content validators.** [NUMBER AND QUALIFICATIONS OF CONTENT VALIDATORS] will be invited to examine the linguistic material selected for evaluation. Review will cover source traceability, meaning, spelling or notation, and the accuracy of any recordings, examples, and aspectual forms included. Review status and unresolved issues will be documented.

The sample sizes and recruitment plan will be finalized with the research adviser before collection. They will be justified by the study's descriptive purpose, available population, and feasibility rather than by an unsupported claim that thirty respondents automatically make an analysis valid. The final report will state the numbers invited, participating, withdrawing, and included in each analysis. Minors will participate only under the approved consent and assent arrangements.

## Data Gathering Instrument

The researchers will use a requirements interview guide, a task observation sheet, and a researcher-made questionnaire. Each instrument will identify its purpose and the respondent group expected to answer it.

**Requirements interview guide.** Consultation questions will address existing learning practices, difficult or confusing content, preferred activities, device access, and expectations of the application. Notes will distinguish participants' reports from researchers' interpretations.

**Task observation sheet.** Prototype sessions will use a consistent set of tasks, such as locating a word, completing a lesson, finishing a scheduled review, playing an enabled game, and locating progress information. The sheet will record task completion, assistance, errors, and comments. Unavailable modules will not be presented as normal task requirements.

**Questionnaire Part I: Respondent profile.** The form will collect only the information needed to describe the sample, such as age band, learner status, prior exposure to Kasiguranin, and device-use experience. Identifying details will be kept separate from responses where required for consent or follow-up.

**Questionnaire Part II: Software quality.** IT respondents will answer statements mapped to functional suitability, performance efficiency, usability, reliability, security, and maintainability. Learner-respondents will answer an appropriate subset concerning functions they have used, ease of use, and experienced reliability. Practitioner judgments concerning security and maintainability will be supported by evidence rather than inferred from interface impressions.

**Questionnaire Part III: Acceptability and satisfaction.** Learners will rate perceived usefulness, ease of learning the interface, motivation to continue, cultural relevance, satisfaction, and intended continued use. These responses will describe perceived experience rather than measured language learning. An open-ended item will invite suggestions and problems encountered.

Items will use a four-point agreement scale: 4 for Strongly Agree, 3 for Agree, 2 for Disagree, and 1 for Strongly Disagree. “Not observed/not applicable” will be recorded separately and excluded from an item's numeric denominator. It will not be coded as disagreement. The distribution of valid responses and excluded responses will be reported.

Table 1 presents the proposed interpretation of mean agreement scores. The same agreement labels will be used across the questionnaire so that subjective ratings are not relabeled as objective certification or proof of software excellence.

Table 1. Proposed interpretation of mean agreement scores.

| Mean score | Interpretation |
| --- | --- |
| 3.26–4.00 | Strongly Agree |
| 2.51–3.25 | Agree |
| 1.76–2.50 | Disagree |
| 1.00–1.75 | Strongly Disagree |

Means will be classified after rounding to two decimal places. The researchers will also present response frequencies so that the ordinal response pattern remains visible. The interpretation scheme will be confirmed with the adviser before administration.

### Instrument Validation and Pilot Testing

The interview guide, tasks, and questionnaire will be reviewed by [NUMBER AND QUALIFICATIONS OF INSTRUMENT REVIEWERS] for relevance, clarity, and alignment with the research questions. Revisions will be recorded. This instrument review will be distinguished from the validation of Kasiguranin content.

The revised questionnaire will be piloted with [PILOT SAMPLE SIZE] eligible individuals who will not enter the main dataset. Internal consistency will be examined for multi-item sections intended to measure the same construct, where the pilot data permit a meaningful estimate. Any reported Cronbach's alpha will identify its section and sample; no coefficient is available at this proposal stage. A favorable coefficient will not substitute for content review, and items will not be removed solely to inflate reliability.

## Data Gathering Procedure

Data gathering will follow the approved sequence below. The researchers will retain an administration log so that the final report can distinguish planned procedures from what actually occurred.

1. Secure permission from the partner institution and obtain any required research or ethics clearance. Finalize the venue, recruitment criteria, instruments, device arrangements, and participant information materials before recruitment.
2. Conduct requirements consultations and check documentary sources. Record needs, source permissions, and content review decisions. Resolve issues affecting participant tasks before the evaluation build is selected.
3. Conduct prototype sessions using the task observation sheet. Explain the think-aloud procedure where used, obtain permission for any recording, and log interaction problems. Revise and retest the application as needed.
4. Complete instrument review and pilot testing. Finalize the feature inventory, questionnaire routing, response labels, and analysis plan before the main administration.
5. Recruit eligible respondents and secure informed consent. Obtain guardian consent and participant assent for minors where required. Record eligibility and participation separately from survey answers.
6. Install the identified evaluation build and provide a consistent orientation. Explain supported offline activities, functions requiring connectivity, unavailable modules, and how participants can obtain help without being coached toward favorable ratings.
7. Allow learner-respondents to use the application for [DURATION OF GUIDED USE], following [MINIMUM ACTIVITY REQUIREMENTS]. Keep a record of assistance and relevant technical interruptions. Conduct the IT review using the feature checklist and testing evidence.
8. Administer the relevant questionnaire sections after use. Learners will answer the profile, applicable experience items, and acceptability section. IT respondents will complete their designated technical assessment. Open-ended feedback will be collected without prompting respondents to give positive answers.
9. Screen and encode the responses using participant codes. Apply the predefined rules for missing, inapplicable, and withdrawn responses, retain the original records, and proceed with the descriptive analysis.

If a critical defect requires a build replacement during collection, the researchers will record the change and identify which respondents used each version. Scores from materially different conditions will not be pooled without explanation. Prototype feedback, pilot responses, and main evaluation responses will remain separate datasets.

## Data Analysis Technique

The analysis will describe the respondents' assessments and observed usability issues. It will not test a causal effect on language learning or treat a purposive sample as a probability sample.

**Frequency and percentage.** Respondent characteristics and item responses will be summarized using counts and percentages. Each percentage will identify its denominator. Missing and inapplicable responses will be reported separately from valid agreement choices.

**Weighted mean.** For an item, the weighted mean will be computed as WM = Σ(f × w) / n, where f is the number selecting a response category, w is its assigned weight from 1 to 4, and n is the number of valid scored responses for that item. An item with no valid responses will be reported as unavailable rather than assigned a score of zero.

**Section summaries.** Respondent-level section scores will be the mean of the applicable scored items, subject to a predefined minimum completion rule of [MINIMUM COMPLETION RULE]. The researchers will report the mean and standard deviation of those respondent-level scores and the number contributing to each summary. Item means will be interpreted using Table 1. Response distributions will accompany mean summaries because the response options are ordinal.

**Group reporting.** Learner and IT assessments will be reported separately. Different questionnaires, item coverage, and expertise will not be collapsed into a single overall quality score. Where common items exist, results may be displayed side by side with their group sizes and administration conditions.

**Instrument consistency.** Any internal consistency analysis will be identified as a pilot or main-sample estimate and restricted to appropriate multi-item sections. The report will state the coefficient, section, sample size, and any item revisions. A reliability estimate will not be presented as evidence that the instrument measures actual language acquisition.

**Qualitative feedback.** Interview notes, task observations, and open-ended comments will be read repeatedly, coded, and grouped into descriptive themes such as navigation problems, content clarity, motivation, or technical interruptions. The researchers will maintain a record linking themes to anonymized excerpts and use these findings to explain and guide improvements. Counts of comments will not be treated as population prevalence.

The software used for encoding and analysis will be [SPREADSHEET OR STATISTICAL SOFTWARE AND VERSION]. The final report will disclose the valid sample for each analysis, deviations from the plan, and limitations caused by missing data or restricted participation. No inferential significance test is planned for the stated research questions.

## Ethical Concerns

**Review and permissions.** The researchers will obtain the permissions and any ethics clearance required by [APPROVING COLLEGE, DEPARTMENT, OR COMMITTEE] before participant recruitment and collection. The manuscript will report approval details only after they have been issued.

**Consent and voluntary participation.** Participants will receive a clear explanation of the study's purpose, tasks, duration, expected data use, and contact details. Participation will be voluntary, and refusal will not affect grades, services, or standing. Guardian consent and participant assent will be obtained for minors where applicable. The consent material will specify any withdrawal deadline after which anonymized records can no longer be linked to an individual.

**Confidentiality and data handling.** Survey responses will use participant codes. Consent records and any contact details will be stored separately from the analysis dataset. Access will be limited to [AUTHORIZED PERSONS], and records will be retained for [RETENTION PERIOD] using [STORAGE AND DELETION ARRANGEMENTS]. Reports will omit identifying details and avoid quotations that could expose a participant in a small community.

**Application data and accounts.** Orientation and consent materials will explain the data produced by normal application use, including any account, synchronization, or public-ranking functions used during the study. The researchers will specify whether application activity records will enter the research dataset. Account registration will not be treated as research consent, and guest access will not be described as eliminating all identifiers. Any public display of a participant's profile will be explained and managed under the approved participation arrangements.

**Cultural and linguistic responsibility.** Contributors will be asked for permission to record, attribute, digitize, and publish their material. The researchers will respect restrictions on culturally sensitive information and distinguish permission to review content from permission to publish it. Linguistic corrections will be documented, and uncertain material will remain outside authoritative learning content until reviewed. No Kasiguranin material will be invented to complete a dataset.

**Research integrity.** Responses and observations will be reported as collected. The researchers will disclose unavailable features, unresolved content gaps, protocol changes, withdrawals, and limitations. Software quality and satisfaction findings will not be represented as proof of language preservation or educational effectiveness. Only after evaluation will the abstract and methodology be updated to report the actual sample, procedures, and results.

# REFERENCES

Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). Distributed practice in verbal recall tasks: A review and quantitative synthesis. Psychological Bulletin, 132(3), 354–380. https://doi.org/10.1037/0033-2909.132.3.354

Deterding, S., Dixon, D., Khaled, R., & Nacke, L. (2011). From game design elements to gamefulness: Defining “gamification.” In Proceedings of the 15th International Academic MindTrek Conference: Envisioning Future Media Environments (pp. 9–15). Association for Computing Machinery. https://doi.org/10.1145/2181037.2181040

Hamari, J., Koivisto, J., & Sarsa, H. (2014). Does gamification work? A literature review of empirical studies on gamification. In Proceedings of the 47th Hawaii International Conference on System Sciences (pp. 3025–3034). IEEE. https://doi.org/10.1109/HICSS.2014.377

Headland, T. N. (2003). Thirty endangered languages in the Philippines. Work Papers of the Summer Institute of Linguistics, University of North Dakota Session, 47, Article 1. https://doi.org/10.31356/silwp.vol47.01

Hermes, M., & King, K. A. (2013). Ojibwe language revitalization, multimedia technology, and family language learning. Language Learning & Technology, 17(1), 125–144. https://doi.org/10.64152/10125/24513

International Organization for Standardization. (2011). ISO/IEC 25010:2011: Systems and software engineering—Systems and software Quality Requirements and Evaluation (SQuaRE)—System and software quality models. https://www.iso.org/standard/35733.html

Kukulska-Hulme, A., & Shield, L. (2008). An overview of mobile assisted language learning: From content delivery to supported collaboration and interaction. ReCALL, 20(3), 271–289. https://doi.org/10.1017/S0958344008000335

SuperMemo. (n.d.). SuperMemo method. Retrieved October 3, 2026, from https://www.supermemo.com/en/supermemo-method

Supnet, C. P. E. (2016). A grammatical sketch of Kasiguranin [Unpublished undergraduate thesis]. University of the Philippines Diliman. [VERIFY AUTHOR INITIALS AND YEAR AGAINST ORIGINAL TITLE PAGE]
