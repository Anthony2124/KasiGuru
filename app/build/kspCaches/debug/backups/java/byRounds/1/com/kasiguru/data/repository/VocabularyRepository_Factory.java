package com.kasiguru.data.repository;

import com.kasiguru.data.local.dao.VocabularyDao;
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
public final class VocabularyRepository_Factory implements Factory<VocabularyRepository> {
  private final Provider<VocabularyDao> vocabularyDaoProvider;

  public VocabularyRepository_Factory(Provider<VocabularyDao> vocabularyDaoProvider) {
    this.vocabularyDaoProvider = vocabularyDaoProvider;
  }

  @Override
  public VocabularyRepository get() {
    return newInstance(vocabularyDaoProvider.get());
  }

  public static VocabularyRepository_Factory create(Provider<VocabularyDao> vocabularyDaoProvider) {
    return new VocabularyRepository_Factory(vocabularyDaoProvider);
  }

  public static VocabularyRepository newInstance(VocabularyDao vocabularyDao) {
    return new VocabularyRepository(vocabularyDao);
  }
}
