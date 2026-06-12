package com.photoapp.data.repository;

import com.photoapp.data.local.PhotoDao;
import com.photoapp.data.media.MediaStoreManager;
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
    "cast",
    "deprecation"
})
public final class PhotoRepositoryImpl_Factory implements Factory<PhotoRepositoryImpl> {
  private final Provider<PhotoDao> photoDaoProvider;

  private final Provider<MediaStoreManager> mediaStoreManagerProvider;

  public PhotoRepositoryImpl_Factory(Provider<PhotoDao> photoDaoProvider,
      Provider<MediaStoreManager> mediaStoreManagerProvider) {
    this.photoDaoProvider = photoDaoProvider;
    this.mediaStoreManagerProvider = mediaStoreManagerProvider;
  }

  @Override
  public PhotoRepositoryImpl get() {
    return newInstance(photoDaoProvider.get(), mediaStoreManagerProvider.get());
  }

  public static PhotoRepositoryImpl_Factory create(Provider<PhotoDao> photoDaoProvider,
      Provider<MediaStoreManager> mediaStoreManagerProvider) {
    return new PhotoRepositoryImpl_Factory(photoDaoProvider, mediaStoreManagerProvider);
  }

  public static PhotoRepositoryImpl newInstance(PhotoDao photoDao,
      MediaStoreManager mediaStoreManager) {
    return new PhotoRepositoryImpl(photoDao, mediaStoreManager);
  }
}
