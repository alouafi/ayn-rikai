# kotlinx.serialization and OkHttp ship their own consumer rules.

# ML Kit's consumer rules do not cover R8 full mode: class merging folds ML Kit's component
# factories into unrelated classes, and on-device OCR then fails with a NullPointerException.
-keep class com.google.mlkit.** { *; }
