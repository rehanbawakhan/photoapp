package com.photoapp.ui.trash;

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
public final class TrashViewModel_Factory implements Factory<TrashViewModel> {
  private final Provider<PhotoRepository> repositoryProvider;

  public TrashViewModel_Factory(Provider<PhotoRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TrashViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TrashViewModel_Factory create(Provider<PhotoRepository> repositoryProvider) {
    return new TrashViewModel_Factory(repositoryProvider);
  }

  public static TrashViewModel newInstance(PhotoRepository repository) {
    return new TrashViewModel(repository);
  }
}
