package com.kasiguru.di;

import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.GameScoreDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideGameScoreDaoFactory implements Factory<GameScoreDao> {
  private final Provider<KasiGuruDatabase> databaseProvider;

  public DatabaseModule_ProvideGameScoreDaoFactory(Provider<KasiGuruDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public GameScoreDao get() {
    return provideGameScoreDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideGameScoreDaoFactory create(
      Provider<KasiGuruDatabase> databaseProvider) {
    return new DatabaseModule_ProvideGameScoreDaoFactory(databaseProvider);
  }

  public static GameScoreDao provideGameScoreDao(KasiGuruDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGameScoreDao(database));
  }
}
