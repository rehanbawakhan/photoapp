package com.photoapp.di;

import com.photoapp.data.local.PhotoDao;
import com.photoapp.data.local.PhotoDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidePhotoDaoFactory implements Factory<PhotoDao> {
  private final Provider<PhotoDatabase> databaseProvider;

  public DatabaseModule_ProvidePhotoDaoFactory(Provider<PhotoDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PhotoDao get() {
    return providePhotoDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePhotoDaoFactory create(
      Provider<PhotoDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePhotoDaoFactory(databaseProvider);
  }

  public static PhotoDao providePhotoDao(PhotoDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePhotoDao(database));
  }
}
