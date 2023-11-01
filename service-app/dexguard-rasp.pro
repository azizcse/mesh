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

-dontwarn java8.util.**
-dontwarn jnr.posix.**
-dontwarn com.kenai.**

#-keep class org.bouncycastle.**
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper

# Web3j
-keep class org.** { *; }

-keep class * extends org.web3j.abi.TypeReference
-keep class * extends org.web3j.abi.datatypes.Type
-keep class com.intermeshnetworks.service.MainActivity {*;}

#RASP START
#-raspchecks *
-raspcontinueon *

-raspcallback class com.w3engineers.meshrnd.TeleMeshService {
    public static void raspCallback(com.guardsquare.dexguard.rasp.callback.DetectionReport);
}

-encryptclasses com.w3engineers.meshrnd.TeleMeshService$Delegate
-encryptclasses com.w3engineers.mesh.ui.TeleMeshServiceMainActivity$Delegate

#Debug sha256
-raspcertificatehash "65:99:3F:C9:07:E8:F5:23:EF:AE:9C:4F:12:88:CC:39:37:DA:F6:E0:1D:54:0B:B8:16:1E:A2:69:72:4C:39:0E"
#Release sha256
-raspcertificatehash "66:2A:B9:6A:5A:EC:48:11:17:89:4C:9F:79:01:A4:AE:E5:63:6A:3E:BA:02:9E:A2:2E:BE:62:79:83:B1:41:93"

#RASP END

#-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**
