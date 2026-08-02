package com.kasiguru.ui.screens.vocabulary;

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
public final class VocabularyViewModel_Factory implements Factory<VocabularyViewModel> {
  private final Provider<VocabularyRepository> vocabularyRepositoryProvider;

  public VocabularyViewModel_Factory(Provider<VocabularyRepository> vocabularyRepositoryProvider) {
    this.vocabularyRepositoryProvider = vocabularyRepositoryProvider;
  }

  @Override
  public VocabularyViewModel get() {
    return newInstance(vocabularyRepositoryProvider.get());
  }

  public static VocabularyViewModel_Factory create(
      Provider<VocabularyRepository> vocabularyRepositoryProvider) {
    return new VocabularyViewModel_Factory(vocabularyRepositoryProvider);
  }

  public static VocabularyViewModel newInstance(VocabularyRepository vocabularyRepository) {
    return new VocabularyViewModel(vocabularyRepository);
  }
}
