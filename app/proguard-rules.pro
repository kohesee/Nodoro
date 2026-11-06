# Nothing Glyph SDK talks to a system service through AIDL/reflection; keep it intact.
-keep class com.nothing.** { *; }
-dontwarn com.nothing.**
# Keep line numbers for readable Play Console crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile