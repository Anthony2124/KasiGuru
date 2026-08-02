package com.kasiguru.ui.screens.stories;

import androidx.lifecycle.SavedStateHandle;
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
public final class StoryReaderViewModel_Factory implements Factory<StoryReaderViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<StoryRepository> storyRepositoryProvider;

  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public StoryReaderViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<StoryRepository> storyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.storyRepositoryProvider = storyRepositoryProvider;
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public StoryReaderViewModel get() {
    return newInstance(savedStateHandleProvider.get(), storyRepositoryProvider.get(), userProgressRepositoryProvider.get());
  }

  public static StoryReaderViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<StoryRepository> storyRepositoryProvider,
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new StoryReaderViewModel_Factory(savedStateHandleProvider, storyRepositoryProvider, userProgressRepositoryProvider);
  }

  public static StoryReaderViewModel newInstance(SavedStateHandle savedStateHandle,
      StoryRepository storyRepository, UserProgressRepository userProgressRepository) {
    return new StoryReaderViewModel(savedStateHandle, storyRepository, userProgressRepository);
  }
}
