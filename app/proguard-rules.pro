# KasiGuru release ProGuard/R8 rules.

# Firebase Firestore maps documents to these model classes via reflection
# (toObjects/toObject). R8 must keep their names and fields or the mapping
# silently breaks in release builds.
-keep class com.kasiguru.data.local.entity.** { *; }
-keep class com.kasiguru.data.remote.model.** { *; }

# Firebase Firestore / Auth / Messaging / Crashlytics ship their own
# consumer rules; no extra keeps needed for them.
