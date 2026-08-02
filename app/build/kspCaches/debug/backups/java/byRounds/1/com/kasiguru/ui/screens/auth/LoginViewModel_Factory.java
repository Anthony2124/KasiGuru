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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public LoginViewModel_Factory(Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(userProgressRepositoryProvider.get());
  }

  public static LoginViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new LoginViewModel_Factory(userProgressRepositoryProvider);
  }

  public static LoginViewModel newInstance(UserProgressRepository userProgressRepository) {
    return new LoginViewModel(userProgressRepository);
  }
}
