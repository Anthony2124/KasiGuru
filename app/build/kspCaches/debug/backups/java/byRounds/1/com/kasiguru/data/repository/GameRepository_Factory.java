package com.kasiguru.data.repository;

import com.kasiguru.data.local.dao.GameScoreDao;
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
public final class GameRepository_Factory implements Factory<GameRepository> {
  private final Provider<GameScoreDao> gameScoreDaoProvider;

  public GameRepository_Factory(Provider<GameScoreDao> gameScoreDaoProvider) {
    this.gameScoreDaoProvider = gameScoreDaoProvider;
  }

  @Override
  public GameRepository get() {
    return newInstance(gameScoreDaoProvider.get());
  }

  public static GameRepository_Factory create(Provider<GameScoreDao> gameScoreDaoProvider) {
    return new GameRepository_Factory(gameScoreDaoProvider);
  }

  public static GameRepository newInstance(GameScoreDao gameScoreDao) {
    return new GameRepository(gameScoreDao);
  }
}
