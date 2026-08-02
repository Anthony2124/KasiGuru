package com.kasiguru.util;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AcousticPlayerManager_Factory implements Factory<AcousticPlayerManager> {
  private final Provider<Context> contextProvider;

  public AcousticPlayerManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AcousticPlayerManager get() {
    return newInstance(contextProvider.get());
  }

  public static AcousticPlayerManager_Factory create(Provider<Context> contextProvider) {
    return new AcousticPlayerManager_Factory(contextProvider);
  }

  public static AcousticPlayerManager newInstance(Context context) {
    return new AcousticPlayerManager(context);
  }
}
