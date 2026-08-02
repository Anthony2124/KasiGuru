package com.kasiguru.ui.screens.home;

import com.kasiguru.data.repository.UserProgressRepository;
import com.kasiguru.data.repository.VocabularyRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  private final Provider<VocabularyRepository> vocabularyRepositoryProvider;

  public HomeViewModel_Factory(Provider<UserProgressRepository> userProgressRepositoryProvider,
      Provider<VocabularyRepository> vocabularyRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
    this.vocabularyRepositoryProvider = vocabularyRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(userProgressRepositoryProvider.get(), vocabularyRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider,
      Provider<VocabularyRepository> vocabularyRepositoryProvider) {
    return new HomeViewModel_Factory(userProgressRepositoryProvider, vocabularyRepositoryProvider);
  }

  public static HomeViewModel newInstance(UserProgressRepository userProgressRepository,
      VocabularyRepository vocabularyRepository) {
    return new HomeViewModel(userProgressRepository, vocabularyRepository);
  }
}
