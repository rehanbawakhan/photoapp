package com.photoapp.ui.map;

import com.photoapp.data.repository.PhotoRepository;
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
    "cast",
    "deprecation"
})
public final class PhotosMapViewModel_Factory implements Factory<PhotosMapViewModel> {
  private final Provider<PhotoRepository> repositoryProvider;

  public PhotosMapViewModel_Factory(Provider<PhotoRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public PhotosMapViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static PhotosMapViewModel_Factory create(Provider<PhotoRepository> repositoryProvider) {
    return new PhotosMapViewModel_Factory(repositoryProvider);
  }

  public static PhotosMapViewModel newInstance(PhotoRepository repository) {
    return new PhotosMapViewModel(repository);
  }
}
