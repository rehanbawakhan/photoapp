# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil
-keep class coil.** { *; }

# Compose
-dontwarn androidx.compose.**
