# We're open source, so dont obfuscate
-dontobfuscate

# Optimize multiple times
-optimizationpasses 5

# Don't obfuscate causes the gradle build to fail after optimization step
# !code/allocation/variable is needed to prevent it
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-allowaccessmodification

# Keep important stacktrace
-keepattributes SourceFile,LineNumberTable

# Needed just for androidx.preference.PreferenceManager
-renamesourcefileattribute SourceFile

# Shizuku UserService: instantiated reflectively inside the Shizuku process
-keep class io.mdp43140.ltecleaner.FileService { <init>(); }
-keep class io.mdp43140.ltecleaner.IFileService { *; }
-keep class io.mdp43140.ltecleaner.IFileService$Stub { *; }
