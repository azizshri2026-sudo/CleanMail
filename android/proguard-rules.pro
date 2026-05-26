# JavaMail
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class jakarta.mail.** { *; }
-dontwarn com.sun.mail.**
-dontwarn javax.mail.**
-dontwarn jakarta.mail.**

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# SQLDelight
-keep class cleanmail.db.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# CleanMail models — never obfuscate data classes used in serialization
-keep class cleanmail.models.** { *; }
