package com.kasiguru.di;

import android.content.Context;
import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.AchievementDao;
import com.kasiguru.data.local.dao.StoryDao;
import com.kasiguru.data.local.dao.UserProgressDao;
import com.kasiguru.data.local.dao.VocabularyDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DatabaseModule_ProvideDatabaseFactory implements Factory<KasiGuruDatabase> {
  private final Provider<Context> contextProvider;

  private final Provider<VocabularyDao> vocabularyDaoProvider;

  private final Provider<StoryDao> storyDaoProvider;

  private final Provider<UserProgressDao> userProgressDaoProvider;

  private final Provider<AchievementDao> achievementDaoProvider;

  public DatabaseModule_ProvideDatabaseFactory(Provider<Context> contextProvider,
      Provider<VocabularyDao> vocabularyDaoProvider, Provider<StoryDao> storyDaoProvider,
      Provider<UserProgressDao> userProgressDaoProvider,
      Provider<AchievementDao> achievementDaoProvider) {
    this.contextProvider = contextProvider;
    this.vocabularyDaoProvider = vocabularyDaoProvider;
    this.storyDaoProvider = storyDaoProvider;
    this.userProgressDaoProvider = userProgressDaoProvider;
    this.achievementDaoProvider = achievementDaoProvider;
  }

  @Override
  public KasiGuruDatabase get() {
    return provideDatabase(contextProvider.get(), vocabularyDaoProvider, storyDaoProvider, userProgressDaoProvider, achievementDaoProvider);
  }

  public static DatabaseModule_ProvideDatabaseFactory create(Provider<Context> contextProvider,
      Provider<VocabularyDao> vocabularyDaoProvider, Provider<StoryDao> storyDaoProvider,
      Provider<UserProgressDao> userProgressDaoProvider,
      Provider<AchievementDao> achievementDaoProvider) {
    return new DatabaseModule_ProvideDatabaseFactory(contextProvider, vocabularyDaoProvider, storyDaoProvider, userProgressDaoProvider, achievementDaoProvider);
  }

  public static KasiGuruDatabase provideDatabase(Context context,
      Provider<VocabularyDao> vocabularyDaoProvider, Provider<StoryDao> storyDaoProvider,
      Provider<UserProgressDao> userProgressDaoProvider,
      Provider<AchievementDao> achievementDaoProvider) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDatabase(context, vocabularyDaoProvider, storyDaoProvider, userProgressDaoProvider, achievementDaoProvider));
  }
}
