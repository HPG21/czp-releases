# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# DataStore - защищаем от обфускации
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# Дополнительные правила для DataStore
-keep class androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.preferences.core.PreferencesKey { *; }
-keep class androidx.datastore.preferences.core.MutablePreferences { *; }
-keep class androidx.datastore.preferences.core.edit { *; }
-keep class androidx.datastore.preferences.core.stringPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.intPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.booleanPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.floatPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.longPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.doublePreferencesKey { *; }
-keep class androidx.datastore.preferences.core.stringSetPreferencesKey { *; }

# Gson - защищаем от обфускации
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep your data classes - защищаем модели данных
-keep class com.depotect.czp.SalaryCalculation { *; }
-keep class com.depotect.czp.ThemeMode { *; }
-keep class com.depotect.czp.Screen { *; }

# Keep LocalDate adapter
-keep class com.depotect.czp.LocalDateAdapter { *; }

# Keep UUID
-keep class java.util.UUID { *; }

# Keep LocalDate
-keep class java.time.LocalDate { *; }

# DataStore preferences keys
-keepclassmembers class * {
    @androidx.datastore.preferences.core.PreferencesKey <fields>;
}

# Prevent R8 from removing DataStore files
-keep class androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.preferences.core.PreferencesKey { *; }

# Дополнительные правила для сохранения данных
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses

# Защищаем весь класс MainActivity от обфускации
-keep class com.depotect.czp.MainActivity { *; }