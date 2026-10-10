# Preserve integrations that discover classes through reflection and native code.
-dontshrink
-dontoptimize
-dontobfuscate

# Preserve runtime metadata used by Kotlin, Compose, and jaudiotagger.
-keepattributes *

# SQLite probes for the optional SLF4J facade at runtime and falls back when it
# is absent. The application does not bundle SLF4J, so do not let this optional
# logging integration block Windows release packaging.
-dontwarn org.slf4j.**
