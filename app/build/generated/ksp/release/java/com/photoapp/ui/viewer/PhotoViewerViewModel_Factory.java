package com.photoapp.ui.viewer;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
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
public final class PhotoViewerViewModel_Factory implements Factory<PhotoViewerViewModel> {
  private final Provider<PhotoRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public PhotoViewerViewModel_Factory(Provider<PhotoRepository> repositoryProvider,
      Provider<Context> contextProvider, Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public PhotoViewerViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get(), savedStateHandleProvider.get());
  }

  public static PhotoViewerViewModel_Factory create(Provider<PhotoRepository> repositoryProvider,
      Provider<Context> contextProvider, Provider<SavedStateHandle> savedStateHandleProvider) {
    return new PhotoViewerViewModel_Factory(repositoryProvider, contextProvider, savedStateHandleProvider);
  }

  public static PhotoViewerViewModel newInstance(PhotoRepository repository, Context context,
      SavedStateHandle savedStateHandle) {
    return new PhotoViewerViewModel(repository, context, savedStateHandle);
  }
}
