package com.kasiguru.di;

import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.VocabularyDao;
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
public final class DatabaseModule_ProvideVocabularyDaoFactory implements Factory<VocabularyDao> {
  private final Provider<KasiGuruDatabase> databaseProvider;

  public DatabaseModule_ProvideVocabularyDaoFactory(Provider<KasiGuruDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public VocabularyDao get() {
    return provideVocabularyDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideVocabularyDaoFactory create(
      Provider<KasiGuruDatabase> databaseProvider) {
    return new DatabaseModule_ProvideVocabularyDaoFactory(databaseProvider);
  }

  public static VocabularyDao provideVocabularyDao(KasiGuruDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideVocabularyDao(database));
  }
}
