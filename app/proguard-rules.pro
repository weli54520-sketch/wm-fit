# نحتفظ بأسماء الحقول اللي نقرأها ونكتبها كـ JSON
-keepclassmembers class com.wm.fit.core.** { *; }

# ===== المرحلة 4 =====
# مكتبة Anthropic تقرأ وتكتب JSON بـ Jackson (بالانعكاس)، فنحتفظ بأسماء كلاساتها
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
# كلاسات اختيارية ما نستخدمها (للسيرفرات)، نتجاهل تحذيراتها
-dontwarn com.fasterxml.jackson.databind.ext.**
-dontwarn java.beans.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn java.lang.management.**
-dontwarn io.opentelemetry.**
# Android ما فيه AnnotatedType. تستخدمه بس ميزة "المخرجات المنظمة" في مكتبة Anthropic
# (توليد JSON Schema)، واحنا ما نستخدمها
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn java.lang.reflect.Annotated*
-dontwarn com.github.victools.jsonschema.**
