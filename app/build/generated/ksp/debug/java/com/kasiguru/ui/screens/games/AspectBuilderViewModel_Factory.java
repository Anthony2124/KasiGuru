package com.kasiguru.ui.screens.games;

import com.kasiguru.data.repository.GameRepository;
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
public final class AspectBuilderViewModel_Factory implements Factory<AspectBuilderViewModel> {
  private final Provider<VocabularyRepository> vocabularyRepositoryProvider;

  private final Provider<GameRepository> gameRepositoryProvider;

  public AspectBuilderViewModel_Factory(Provider<VocabularyRepository> vocabularyRepositoryProvider,
      Provider<GameRepository> gameRepositoryProvider) {
    this.vocabularyRepositoryProvider = vocabularyRepositoryProvider;
    this.gameRepositoryProvider = gameRepositoryProvider;
  }

  @Override
  public AspectBuilderViewModel get() {
    return newInstance(vocabularyRepositoryProvider.get(), gameRepositoryProvider.get());
  }

  public static AspectBuilderViewModel_Factory create(
      Provider<VocabularyRepository> vocabularyRepositoryProvider,
      Provider<GameRepository> gameRepositoryProvider) {
    return new AspectBuilderViewModel_Factory(vocabularyRepositoryProvider, gameRepositoryProvider);
  }

  public static AspectBuilderViewModel newInstance(VocabularyRepository vocabularyRepository,
      GameRepository gameRepository) {
    return new AspectBuilderViewModel(vocabularyRepository, gameRepository);
  }
}
