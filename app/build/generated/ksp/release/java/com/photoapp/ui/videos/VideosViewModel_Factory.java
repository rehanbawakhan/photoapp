package com.photoapp.ui.videos;

import android.content.Context;
import com.photoapp.data.repository.PhotoRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class VideosViewModel_Factory implements Factory<VideosViewModel> {
  private final Provider<PhotoRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public VideosViewModel_Factory(Provider<PhotoRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public VideosViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static VideosViewModel_Factory create(Provider<PhotoRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new VideosViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static VideosViewModel newInstance(PhotoRepository repository, Context context) {
    return new VideosViewModel(repository, context);
  }
}
