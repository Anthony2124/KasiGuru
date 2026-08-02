package com.kasiguru.ui.screens.games;

import com.kasiguru.data.repository.GameRepository;
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
public final class SentenceOrderViewModel_Factory implements Factory<SentenceOrderViewModel> {
  private final Provider<GameRepository> gameRepositoryProvider;

  public SentenceOrderViewModel_Factory(Provider<GameRepository> gameRepositoryProvider) {
    this.gameRepositoryProvider = gameRepositoryProvider;
  }

  @Override
  public SentenceOrderViewModel get() {
    return newInstance(gameRepositoryProvider.get());
  }

  public static SentenceOrderViewModel_Factory create(
      Provider<GameRepository> gameRepositoryProvider) {
    return new SentenceOrderViewModel_Factory(gameRepositoryProvider);
  }

  public static SentenceOrderViewModel newInstance(GameRepository gameRepository) {
    return new SentenceOrderViewModel(gameRepository);
  }
}
