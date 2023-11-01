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

-optimizationpasses 5
-dontpreverify
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic
-keepattributes *Annotation*

-verbose

-renamesourcefileattribute SourceFile

-keepattributes Exceptions
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*
-keepattributes Signature

-dontwarn java8.util.**
-dontwarn jnr.posix.**
-dontwarn com.kenai.**

#-keep class org.bouncycastle.**
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper

# Web3j
#-keep class org.web3j.** { *; }

-keep class * extends org.web3j.abi.TypeReference
-keep class * extends org.web3j.abi.datatypes.Type

#-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**

-dontwarn org.webrtc.voiceengine.WebRtcAudioTrack
-keep class org.webrtc.** { *; }

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** e(...);
}
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
