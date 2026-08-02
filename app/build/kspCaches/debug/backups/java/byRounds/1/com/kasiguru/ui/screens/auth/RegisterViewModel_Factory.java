package com.kasiguru.ui.screens.auth;

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
public final class RegisterViewModel_Factory implements Factory<RegisterViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public RegisterViewModel_Factory(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public RegisterViewModel get() {
    return newInstance(userProgressRepositoryProvider.get());
  }

  public static RegisterViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new RegisterViewModel_Factory(userProgressRepositoryProvider);
  }

  public static RegisterViewModel newInstance(UserProgressRepository userProgressRepository) {
    return new RegisterViewModel(userProgressRepository);
  }
}
