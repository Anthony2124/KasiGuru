package com.kasiguru.di;

import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.StoryDao;
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
public final class DatabaseModule_ProvideStoryDaoFactory implements Factory<StoryDao> {
  private final Provider<KasiGuruDatabase> databaseProvider;

  public DatabaseModule_ProvideStoryDaoFactory(Provider<KasiGuruDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public StoryDao get() {
    return provideStoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideStoryDaoFactory create(
      Provider<KasiGuruDatabase> databaseProvider) {
    return new DatabaseModule_ProvideStoryDaoFactory(databaseProvider);
  }

  public static StoryDao provideStoryDao(KasiGuruDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideStoryDao(database));
  }
}
