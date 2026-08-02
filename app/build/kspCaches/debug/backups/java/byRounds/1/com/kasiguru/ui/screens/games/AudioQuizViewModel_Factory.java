package com.kasiguru.ui.screens.games;

import com.kasiguru.data.repository.GameRepository;
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
public final class AudioQuizViewModel_Factory implements Factory<AudioQuizViewModel> {
  private final Provider<VocabularyRepository> vocabularyRepositoryProvider;

  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  private final Provider<GameRepository> gameRepositoryProvider;

  public AudioQuizViewModel_Factory(Provider<VocabularyRepository> vocabularyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider,
      Provider<GameRepository> gameRepositoryProvider) {
    this.vocabularyRepositoryProvider = vocabularyRepositoryProvider;
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
    this.gameRepositoryProvider = gameRepositoryProvider;
  }

  @Override
  public AudioQuizViewModel get() {
    return newInstance(vocabularyRepositoryProvider.get(), userProgressRepositoryProvider.get(), gameRepositoryProvider.get());
  }

  public static AudioQuizViewModel_Factory create(
      Provider<VocabularyRepository> vocabularyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider,
      Provider<GameRepository> gameRepositoryProvider) {
    return new AudioQuizViewModel_Factory(vocabularyRepositoryProvider, userProgressRepositoryProvider, gameRepositoryProvider);
  }

  public static AudioQuizViewModel newInstance(VocabularyRepository vocabularyRepository,
      UserProgressRepository userProgressRepository, GameRepository gameRepository) {
    return new AudioQuizViewModel(vocabularyRepository, userProgressRepository, gameRepository);
  }
}
