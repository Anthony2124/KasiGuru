package com.kasiguru.ui.screens.achievements;

import com.kasiguru.data.repository.UserProgressRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AchievementsViewModel_Factory implements Factory<AchievementsViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public AchievementsViewModel_Factory(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public AchievementsViewModel get() {
    return newInstance(userProgressRepositoryProvider.get());
  }

  public static AchievementsViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new AchievementsViewModel_Factory(userProgressRepositoryProvider);
  }

  public static AchievementsViewModel newInstance(UserProgressRepository userProgressRepository) {
    return new AchievementsViewModel(userProgressRepository);
  }
}
