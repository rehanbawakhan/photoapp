package com.photoapp.data.repository;

import android.content.Context;
import com.photoapp.data.local.PhotoDao;
import com.photoapp.data.media.MediaStoreManager;
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
public final class PhotoRepositoryImpl_Factory implements Factory<PhotoRepositoryImpl> {
  private final Provider<PhotoDao> photoDaoProvider;

  private final Provider<MediaStoreManager> mediaStoreManagerProvider;

  private final Provider<Context> contextProvider;

  public PhotoRepositoryImpl_Factory(Provider<PhotoDao> photoDaoProvider,
      Provider<MediaStoreManager> mediaStoreManagerProvider, Provider<Context> contextProvider) {
    this.photoDaoProvider = photoDaoProvider;
    this.mediaStoreManagerProvider = mediaStoreManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public PhotoRepositoryImpl get() {
    return newInstance(photoDaoProvider.get(), mediaStoreManagerProvider.get(), contextProvider.get());
  }

  public static PhotoRepositoryImpl_Factory create(Provider<PhotoDao> photoDaoProvider,
      Provider<MediaStoreManager> mediaStoreManagerProvider, Provider<Context> contextProvider) {
    return new PhotoRepositoryImpl_Factory(photoDaoProvider, mediaStoreManagerProvider, contextProvider);
  }

  public static PhotoRepositoryImpl newInstance(PhotoDao photoDao,
      MediaStoreManager mediaStoreManager, Context context) {
    return new PhotoRepositoryImpl(photoDao, mediaStoreManager, context);
  }
}
