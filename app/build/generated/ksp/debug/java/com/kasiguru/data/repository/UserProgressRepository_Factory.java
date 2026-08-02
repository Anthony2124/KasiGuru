package com.kasiguru.data.repository;

import com.kasiguru.data.local.dao.AchievementDao;
import com.kasiguru.data.local.dao.UserProgressDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class UserProgressRepository_Factory implements Factory<UserProgressRepository> {
  private final Provider<UserProgressDao> userProgressDaoProvider;

  private final Provider<AchievementDao> achievementDaoProvider;

  public UserProgressRepository_Factory(Provider<UserProgressDao> userProgressDaoProvider,
      Provider<AchievementDao> achievementDaoProvider) {
    this.userProgressDaoProvider = userProgressDaoProvider;
    this.achievementDaoProvider = achievementDaoProvider;
  }

  @Override
  public UserProgressRepository get() {
    return newInstance(userProgressDaoProvider.get(), achievementDaoProvider.get());
  }

  public static UserProgressRepository_Factory create(
      Provider<UserProgressDao> userProgressDaoProvider,
      Provider<AchievementDao> achievementDaoProvider) {
    return new UserProgressRepository_Factory(userProgressDaoProvider, achievementDaoProvider);
  }

  public static UserProgressRepository newInstance(UserProgressDao userProgressDao,
      AchievementDao achievementDao) {
    return new UserProgressRepository(userProgressDao, achievementDao);
  }
}
