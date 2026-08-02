package com.kasiguru.ui.screens.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<UserProgressRepository> userProgressRepositoryProvider;

  public HomeViewModel_Factory(Provider<UserProgressRepository> userProgressRepositoryProvider) {
    this.userProgressRepositoryProvider = userProgressRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(userProgressRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<UserProgressRepository> userProgressRepositoryProvider) {
    return new HomeViewModel_Factory(userProgressRepositoryProvider);
  }

  public static HomeViewModel newInstance(UserProgressRepository userProgressRepository) {
    return new HomeViewModel(userProgressRepository);
  }
}
