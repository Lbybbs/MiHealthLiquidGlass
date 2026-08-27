package com.mihealth.liquidglass

import android.app.Activity
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * libxposed API 102 entry point for the 小米运动健康 liquid-glass bottom bar.
 *
 * Loaded by LSPosed into the `com.mi.health` process (see scope.list). We hook
 * the framework's [android.app.Instrumentation.callActivityOnResume] so we can
 * catch every Activity resume *without depending on any app-private class*.
 * When the resumed Activity happens to own the main tab layout
 * (`main_activity_main`), we inject the liquid-glass backdrop under the native
 * Material `TabLayout` so content scrolls beneath it and is refracted.
 */
class MiHealthModule : XposedModule() {

    companion object {
        private const val TAG = "MiHealthLiquidGlass"
        const val TARGET_PKG = "com.mi.health"

        @Volatile
        var appClassLoader: ClassLoader? = null
            private set
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "module loaded in ${param.processName} (api $apiVersion)")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != TARGET_PKG) return
        appClassLoader = param.classLoader
        try {
            hookActivityResume()
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "failed to hook Activity resume", t)
        }
    }

    /**
     * Hooks the framework method that runs for every Activity's onResume, so the
     * injector runs regardless of which base Activity 小米运动健康 uses.
     */
    private fun hookActivityResume() {
        val instrument = Class.forName("android.app.Instrumentation", false, null)
        val callActivityOnResume =
            instrument.getDeclaredMethod("callActivityOnResume", Activity::class.java)

        hook(callActivityOnResume).intercept { chain ->
            val activity = if (chain.args.isNotEmpty()) chain.args[0] as? Activity else null
            if (activity != null) {
                try {
                    GlassBarInjector.maybeInject(activity)
                } catch (t: Throwable) {
                    log(Log.ERROR, TAG, "inject failed on ${activity.javaClass.name}", t)
                }
            }
            chain.proceed()
        }

        log(Log.INFO, TAG, "Instrumentation.callActivityOnResume hooked")
    }
}
