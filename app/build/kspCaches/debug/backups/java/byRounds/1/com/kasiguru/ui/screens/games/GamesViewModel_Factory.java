package com.kasiguru.ui.screens.games;

import com.kasiguru.data.repository.GameRepository;
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
public final class GamesViewModel_Factory implements Factory<GamesViewModel> {
  private final Provider<GameRepository> gameRepositoryProvider;

  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public GamesViewModel_Factory(Provider<GameRepository> gameRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.gameRepositoryProvider = gameRepositoryProvider;
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public GamesViewModel get() {
    return newInstance(gameRepositoryProvider.get(), userProgressRepositoryProvider.get());
  }

  public static GamesViewModel_Factory create(Provider<GameRepository> gameRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new GamesViewModel_Factory(gameRepositoryProvider, userProgressRepositoryProvider);
  }

  public static GamesViewModel newInstance(GameRepository gameRepository,
      UserProgressRepository userProgressRepository) {
    return new GamesViewModel(gameRepository, userProgressRepository);
  }
}
