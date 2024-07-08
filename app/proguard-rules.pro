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

#-dontwarn pub.devrel.easypermissions.*

-repackageclasses ''
-allowaccessmodification
-keepattributes *Annotation*
-keepattributes InnerClasses
-optimizationpasses 3

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class com.google.** {*;}
-dontwarn java.lang.invoke.**

#To maintain custom components names that are used on layouts XML:
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

#Maintain enums
-keepclassmembers enum * { *; }

#To keep parcelable classes (to serialize - deserialize objects to sent through Intents)
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

#Keep the R
-keepclassmembers class **.R$* {
    public static <fields>;
}

#dagger
-dontwarn com.google.errorprone.annotations.*

#picasso
-dontwarn com.squareup.okhttp.**

#joda
-dontwarn org.joda**
-keep class org.joda.** { *; }
-keep interface org.joda.** { *; }

#org.apache
-dontwarn org.apache.**
-keep class org.apache.** { *; }
-keep interface org.apache.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Okio
-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**

# retrofit2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Room persistense library
-keep public class * extends androidx.**
-keep public class androidx.** {*;}
-keepclassmembers class androidx.** {*;}

# Lifecycler library
-keep public class * extends androidx.lifecycle.**
-keep public class androidx.lifecycle.** {*;}
-keepclasseswithmembers class androidx.lifecycle.** {*;}

-keep public class * extends net.maxsmr.core.domain.entities.feature.DoNotObfuscate

# kotlinx.serialization
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault


# Huawei
-keep class com.huawei.agconnect.**{*;}
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}
-keep class * extends com.huawei.hms.core.aidl.IMessageEntity{ *; }
-keep public class com.huawei.location.nlp.network.** {*; }
-keep class com.huawei.wisesecurity.ucs.**{*;}

#Common rules
-ignorewarnings
-repackageclasses
-verbose
-keepparameternames
-keepattributes Signature,*Annotation*,EnclosingMethod,SourceFile,LineNumberTable
-keepclassmembers,includedescriptorclasses class * { native <methods>; }
-keepclasseswithmembernames,includedescriptorclasses class * {native <methods>;}
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify

#Google and Java rules
-dontwarn android.support.**
-dontwarn java.lang.**
-dontwarn org.codehaus.**
-dontwarn com.google.**
-dontwarn java.nio.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-keep class com.google.** { *; }
-dontwarn com.google.**
-keep class java.** {*;}
-dontwarn java.**
-keep class android.** {*;}
-dontwarn android.**
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class * extends android.app.Activity
-keep class androidx.core.app.CoreComponentFactory { *; }
-keepnames class com.google.vr.ndk.** { *; }
-keepnames class com.google.vr.sdk.** { *; }
-keep class com.google.vr.sdk.* { *; }
-keepclassmembers class com.google.vr.sdk.* { *; }
-keep class com.google.vr.sdk.widgets.pano.VrPanoramaView { *; }
-keepclasseswithmembernames,allowoptimization class com.google.common.logging.nano.Vr$VREvent$SdkConfigurationParams** {*;}
-keep class com.google.vr.cardboard.UsedByNative
-keep @com.google.vr.cardboard.UsedByNative class *
-keepclassmembers class * {@com.google.vr.cardboard.UsedByNative *;}
-keep @com.google.vr.cardboard.annotations.UsedByNative class *
-keepclassmembers class * { @com.google.vr.cardboard.annotations.UsedByNative *;}
-keep class com.google.vr.cardboard.annotations.UsedByReflection
-keep @com.google.vr.cardboard.annotations.UsedByReflection class *
-keepclassmembers class * {@com.google.vr.cardboard.annotations.UsedByReflection *;}
-keepclasseswithmembernames,includedescriptorclasses class com.google.ar.** {native <methods>;}
-keep public class com.google.ar.core.** {*;}
-keep class com.google.ar.core.annotations.UsedByNative
-keep @com.google.ar.core.annotations.UsedByNative class *
-keepclassmembers class * {@com.google.ar.core.annotations.UsedByNative *;}
-keep class com.google.ar.core.annotations.UsedByReflection
-keep @com.google.ar.core.annotations.UsedByReflection class *
-keepclassmembers class * {@com.google.ar.core.annotations.UsedByReflection *;}
-keep class com.google.vr.dynamite.client.IObjectWrapper { *; }
-keep class com.google.vr.dynamite.client.ILoadedInstanceCreator { *; }
-keep class com.google.vr.dynamite.client.INativeLibraryLoader { *; }
-keep class com.google.vr.dynamite.client.UsedByNative
-keep class com.google.android.play.core.** { *; }
-keep @com.google.vr.dynamite.client.UsedByNative class *
-keepclassmembers class * {@com.google.vr.dynamite.client.UsedByNative *;}
-keep class com.google.vr.dynamite.client.UsedByReflection
-keep @com.google.vr.dynamite.client.UsedByReflection class *
-keepclassmembers class * {@com.google.vr.dynamite.client.UsedByReflection *;}
-keep class com.google.android.gms.* { *; }
-keepclassmembers class com.google.android.gms.* { *; }
-keep class com.google.android.gms.location.LocationResult { *; }
-keepclassmembers class com.google.android.gms.location.LocationResult { *; }
-dontwarn com.google.android.gms.**
-keepclassmembers class * implements android.arch.lifecycle.LifecycleObserver {<init>(...);}
-keepclassmembers class * extends android.arch.lifecycle.ViewModel {<init>(...);}
-keepclassmembers class android.arch.lifecycle.Lifecycle$State { *; }
-keepclassmembers class android.arch.lifecycle.Lifecycle$Event { *; }
-keepclassmembers class * {@android.arch.lifecycle.OnLifecycleEvent *;}
-keepclassmembers class * implements android.arch.lifecycle.LifecycleObserver {<init>(...);}
-keep class * implements android.arch.lifecycle.LifecycleObserver {<init>(...);}
-keepclassmembers class android.arch.** { *; }
-keep class android.arch.** { *; }
-dontwarn android.arch.**
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**
-keep class com.google.firebase.**
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**
-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }
-keep class com.makeramen.roundedimageview.** { *; }
-keep class com.makeramen.roundedimageview.RoundedTransformationBuilder { *; }
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keepclassmembers class **.R$* {public static <fields>;}