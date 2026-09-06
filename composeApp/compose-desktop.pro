# Reliability is more important than a smaller desktop package. JavaFX loads its
# Windows toolkit and media implementation through reflection and native code;
# ProGuard cannot infer those entry points and otherwise removes QuantumToolkit.
-dontshrink
-dontoptimize
-dontobfuscate

# Preserve runtime metadata used by Kotlin, Compose, JavaFX, and jaudiotagger.
-keepattributes *

# SQLite probes for the optional SLF4J facade at runtime and falls back when it
# is absent. The application does not bundle SLF4J, so do not let this optional
# logging integration block Windows release packaging.
-dontwarn org.slf4j.**
