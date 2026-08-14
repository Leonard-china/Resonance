# Reliability is more important than a smaller desktop package. JavaFX loads its
# Windows toolkit and media implementation through reflection and native code;
# ProGuard cannot infer those entry points and otherwise removes QuantumToolkit.
-dontshrink
-dontoptimize
-dontobfuscate

# Preserve runtime metadata used by Kotlin, Compose, JavaFX, and jaudiotagger.
-keepattributes *
