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

# Capacitor Core & Android 브릿지 보호
-keep class com.getcapacitor.** { *; }
-keep class org.apache.cordova.** { *; }

# 자바스크립트 인터페이스 및 리플렉션 속성 유지
-keepattributes EnclosingMethod,InnerClasses,Signature,AnnotationDefault,*Annotation*,JavascriptInterface

# 커스텀 MainActivity 내의 내부 클래스 및 메소드 보호
-keep class com.is2rang.tetroid.MainActivity$** { *; }
