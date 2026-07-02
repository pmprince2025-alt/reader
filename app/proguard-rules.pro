# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.folio.core.database.** { *; }

# Keep serializable navigation routes
-keep class com.folio.app.* { *; }

# Keep PDF engine
-keep class com.folio.pdfengine.** { *; }
