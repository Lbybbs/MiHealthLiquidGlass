# libxposed module entry classes must survive R8.
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
-keep class com.mihealth.liquidglass.MiHealthModule { public <init>(); }
-keep class com.example.liquidglass.** { *; }
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
