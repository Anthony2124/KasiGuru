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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public SplashViewModel_Factory(Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(userProgressRepositoryProvider.get());
  }

  public static SplashViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new SplashViewModel_Factory(userProgressRepositoryProvider);
  }

  public static SplashViewModel newInstance(UserProgressRepository userProgressRepository) {
    return new SplashViewModel(userProgressRepository);
  }
}
