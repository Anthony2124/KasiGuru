package com.kasiguru.util;

import com.kasiguru.data.local.dao.SyncQueueDao;
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
public final class OfflineSyncManager_Factory implements Factory<OfflineSyncManager> {
  private final Provider<SyncQueueDao> syncQueueDaoProvider;

  public OfflineSyncManager_Factory(Provider<SyncQueueDao> syncQueueDaoProvider) {
    this.syncQueueDaoProvider = syncQueueDaoProvider;
  }

  @Override
  public OfflineSyncManager get() {
    return newInstance(syncQueueDaoProvider.get());
  }

  public static OfflineSyncManager_Factory create(Provider<SyncQueueDao> syncQueueDaoProvider) {
    return new OfflineSyncManager_Factory(syncQueueDaoProvider);
  }

  public static OfflineSyncManager newInstance(SyncQueueDao syncQueueDao) {
    return new OfflineSyncManager(syncQueueDao);
  }
}
