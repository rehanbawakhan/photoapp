package com.photoapp.ui.hidden;

import com.photoapp.data.repository.PhotoRepository;
import com.photoapp.data.security.HiddenSecurityManager;
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
public final class HiddenViewModel_Factory implements Factory<HiddenViewModel> {
  private final Provider<PhotoRepository> repositoryProvider;

  private final Provider<HiddenSecurityManager> securityManagerProvider;

  public HiddenViewModel_Factory(Provider<PhotoRepository> repositoryProvider,
      Provider<HiddenSecurityManager> securityManagerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.securityManagerProvider = securityManagerProvider;
  }

  @Override
  public HiddenViewModel get() {
    return newInstance(repositoryProvider.get(), securityManagerProvider.get());
  }

  public static HiddenViewModel_Factory create(Provider<PhotoRepository> repositoryProvider,
      Provider<HiddenSecurityManager> securityManagerProvider) {
    return new HiddenViewModel_Factory(repositoryProvider, securityManagerProvider);
  }

  public static HiddenViewModel newInstance(PhotoRepository repository,
      HiddenSecurityManager securityManager) {
    return new HiddenViewModel(repository, securityManager);
  }
}
