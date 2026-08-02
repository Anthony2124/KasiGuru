package com.kasiguru.ui.screens.flashcards;

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
public final class FlashcardViewModel_Factory implements Factory<FlashcardViewModel> {
  private final Provider<VocabularyRepository> vocabularyRepositoryProvider;

  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public FlashcardViewModel_Factory(Provider<VocabularyRepository> vocabularyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.vocabularyRepositoryProvider = vocabularyRepositoryProvider;
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public FlashcardViewModel get() {
    return newInstance(vocabularyRepositoryProvider.get(), userProgressRepositoryProvider.get());
  }

  public static FlashcardViewModel_Factory create(
      Provider<VocabularyRepository> vocabularyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new FlashcardViewModel_Factory(vocabularyRepositoryProvider, userProgressRepositoryProvider);
  }

  public static FlashcardViewModel newInstance(VocabularyRepository vocabularyRepository,
      UserProgressRepository userProgressRepository) {
    return new FlashcardViewModel(vocabularyRepository, userProgressRepository);
  }
}
