package com.kasiguru.di;

import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.AchievementDao;
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
public final class DatabaseModule_ProvideAchievementDaoFactory implements Factory<AchievementDao> {
  private final Provider<KasiGuruDatabase> databaseProvider;

  public DatabaseModule_ProvideAchievementDaoFactory(Provider<KasiGuruDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public AchievementDao get() {
    return provideAchievementDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideAchievementDaoFactory create(
      Provider<KasiGuruDatabase> databaseProvider) {
    return new DatabaseModule_ProvideAchievementDaoFactory(databaseProvider);
  }

  public static AchievementDao provideAchievementDao(KasiGuruDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAchievementDao(database));
  }
}
