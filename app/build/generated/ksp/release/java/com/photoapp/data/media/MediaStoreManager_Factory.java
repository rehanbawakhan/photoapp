package com.photoapp.data.media;

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
public final class MediaStoreManager_Factory implements Factory<MediaStoreManager> {
  private final Provider<Context> contextProvider;

  public MediaStoreManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MediaStoreManager get() {
    return newInstance(contextProvider.get());
  }

  public static MediaStoreManager_Factory create(Provider<Context> contextProvider) {
    return new MediaStoreManager_Factory(contextProvider);
  }

  public static MediaStoreManager newInstance(Context context) {
    return new MediaStoreManager(context);
  }
}
