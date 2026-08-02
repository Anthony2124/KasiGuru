package com.kasiguru.data.repository;

import com.kasiguru.data.local.dao.StoryDao;
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
public final class StoryRepository_Factory implements Factory<StoryRepository> {
  private final Provider<StoryDao> storyDaoProvider;

  public StoryRepository_Factory(Provider<StoryDao> storyDaoProvider) {
    this.storyDaoProvider = storyDaoProvider;
  }

  @Override
  public StoryRepository get() {
    return newInstance(storyDaoProvider.get());
  }

  public static StoryRepository_Factory create(Provider<StoryDao> storyDaoProvider) {
    return new StoryRepository_Factory(storyDaoProvider);
  }

  public static StoryRepository newInstance(StoryDao storyDao) {
    return new StoryRepository(storyDao);
  }
}
