package com.photoapp.data.security;

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
    "cast",
    "deprecation"
})
public final class HiddenSecurityManager_Factory implements Factory<HiddenSecurityManager> {
  private final Provider<Context> contextProvider;

  public HiddenSecurityManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HiddenSecurityManager get() {
    return newInstance(contextProvider.get());
  }

  public static HiddenSecurityManager_Factory create(Provider<Context> contextProvider) {
    return new HiddenSecurityManager_Factory(contextProvider);
  }

  public static HiddenSecurityManager newInstance(Context context) {
    return new HiddenSecurityManager(context);
  }
}
