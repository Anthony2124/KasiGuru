package com.kasiguru.ui.screens.profile;

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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public ProfileViewModel_Factory(Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(userProgressRepositoryProvider.get());
  }

  public static ProfileViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new ProfileViewModel_Factory(userProgressRepositoryProvider);
  }

  public static ProfileViewModel newInstance(UserProgressRepository userProgressRepository) {
    return new ProfileViewModel(userProgressRepository);
  }
}
