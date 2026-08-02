package com.kasiguru.ui.screens.stories;

import com.kasiguru.data.repository.StoryRepository;
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
public final class StoriesViewModel_Factory implements Factory<StoriesViewModel> {
  private final Provider<StoryRepository> storyRepositoryProvider;

  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public StoriesViewModel_Factory(Provider<StoryRepository> storyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.storyRepositoryProvider = storyRepositoryProvider;
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public StoriesViewModel get() {
    return newInstance(storyRepositoryProvider.get(), userProgressRepositoryProvider.get());
  }

  public static StoriesViewModel_Factory create(Provider<StoryRepository> storyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new StoriesViewModel_Factory(storyRepositoryProvider, userProgressRepositoryProvider);
  }

  public static StoriesViewModel newInstance(StoryRepository storyRepository,
      UserProgressRepository userProgressRepository) {
    return new StoriesViewModel(storyRepository, userProgressRepository);
  }
}
