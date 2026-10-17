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

# OkHttp selects the JDK's TLS provider on Windows. These adapters are optional
# probes shipped in the same jar, not dependencies of the selected JDK path.
-dontwarn okhttp3.internal.platform.Android10Platform
-dontwarn okhttp3.internal.platform.AndroidPlatform
-dontwarn okhttp3.internal.platform.android.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.openjsse.**

# ProGuard cannot resolve MethodHandle.invokeExact's signature-polymorphic
# short/float calls in ORT's optional FP16 conversions against JDK 17 stubs.
# Screenshot OCR uses float32; preserve the class and suppress only this probe.
-dontwarn ai.onnxruntime.platform.Fp16Conversions
