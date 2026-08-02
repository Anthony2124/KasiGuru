package com.kasiguru.di;

import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.UserProgressDao;
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
public final class DatabaseModule_ProvideUserProgressDaoFactory implements Factory<UserProgressDao> {
  private final Provider<KasiGuruDatabase> databaseProvider;

  public DatabaseModule_ProvideUserProgressDaoFactory(Provider<KasiGuruDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public UserProgressDao get() {
    return provideUserProgressDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideUserProgressDaoFactory create(
      Provider<KasiGuruDatabase> databaseProvider) {
    return new DatabaseModule_ProvideUserProgressDaoFactory(databaseProvider);
  }

  public static UserProgressDao provideUserProgressDao(KasiGuruDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideUserProgressDao(database));
  }
}
