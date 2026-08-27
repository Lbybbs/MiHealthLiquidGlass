package com.mihealth.liquidglass

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView

/**
 * Finds the main tab layout inside 小米运动健康 and turns its bottom bar into a
 * real liquid-glass surface that refracts the content scrolling behind it.
 *
 * Target layout (res/layout/main_activity_main.xml, ids resolved by name so the
 * module survives resource re-numbering across app updates):
 *
 * ```
 * LinearLayout (vertical)                       <- app root, child of android.R.id.content
 *   FrameLayout main_fl_content   (weight=1)    <- content area (RN screens render here)
 *   View        divider                         <- thin line (hidden behind the bar)
 *   FrameLayout main_fl_bottom_container        <- bottom bar
 *     Material TabLayout main_tl_bottom         <- native tabs (kept for real tab switching)
 * ```
 *
 * The surgery moves the bottom container out of the vertical LinearLayout and
 * overlays it on the activity content frame (bottom gravity), so the content
 * area now fills the whole screen and scrolls *underneath* the bar. A
 * [LiquidGlassView] is inserted at index 0 of the bottom container, sampling
 * [main_fl_content], with the native TabLayout drawn on top (index 1). The
 * native tab-switching logic is untouched.
 */
object GlassBarInjector {

    private const val TAG = "MiHealthLiquidGlass"

    /** Guard so we only inject once per Activity instance. */
    private val injected = java.util.WeakHashMap<Activity, Boolean>()

    /** True once any glass bar has been applied (used by diagnostics). */
    @Volatile
    var installed: Boolean = false
        private set

    fun maybeInject(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (injected.containsKey(activity)) return
        try {
            val content = find<FrameLayout>(activity, "main_fl_content") ?: return
            val bottom = find<FrameLayout>(activity, "main_fl_bottom_container") ?: return
            // The native tab host must be present, otherwise this isn't the main screen.
            if (find<ViewGroup>(activity, "main_tl_bottom") == null) return

            injectGlass(activity, content, bottom)
            injected[activity] = true
            installed = true
            log(Log.INFO, TAG, "liquid-glass bottom bar injected on ${activity.javaClass.name}")
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "maybeInject failed", t)
        }
    }

    private fun injectGlass(activity: Activity, content: FrameLayout, bottom: FrameLayout) {
        if (Build.VERSION.SDK_INT < 33) {
            log(Log.WARN, TAG, "API<33: AGSL lens unavailable, skipping full refraction")
            return
        }
        val container = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        // 1) Make the bottom bar float over the content (content scrolls behind it).
        if (bottom.parent !== container) {
            (bottom.parent as? ViewGroup)?.removeView(bottom)
            container.addView(
                bottom,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )
        }

        // 2) Drop the opaque bar background so the glass shows through.
        bottom.setBackgroundColor(Color.TRANSPARENT)

        // 3) Insert the LiquidGlassView beneath the native TabLayout (glass = child 0,
        //    TabLayout stays at the top and keeps receiving its own touch events).
        val glass = LiquidGlassView(activity, null, 0)
        val density = activity.resources.displayMetrics.density
        glass.enableDynamicBackground = true
        glass.backdropSource = content          // refract the RN content under the bar
        glass.material = GlassMaterial.REGULAR  // readability-first material
        glass.cornerRadius = 999f               // pill / full-bleed bar
        glass.refractionHeight = 60f * density
        glass.bevelWidth = 16f * density
        glass.dispersionStrength = 0.12f
        glass.enableSensorHighlight = true      // specular follows device tilt
        glass.enableAdaptiveTint = true         // tint follows backdrop luminance
        glass.isClickable = false               // don't swallow taps outside the tabs

        bottom.addView(
            glass,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // 4) The content area already fills the freed space (weight=1 in the app's
        //    LinearLayout, which is match_parent in the content frame).
        // TODO(device-test): add bottom padding to the content ScrollView so the
        //    last items can scroll above the bar; and tune material/refraction to taste.
        // TODO(device-test): on API < 33 fall back to a plain frost tinted bar.
    }

    /** Resolve a view id by name from the target app's resources (version-robust). */
    private fun <T : View> find(activity: Activity, name: String): T? {
        val id = activity.resources.getIdentifier(name, "id", activity.packageName)
        if (id == 0) return null
        @Suppress("UNCHECKED_CAST")
        return activity.findViewById(id)
    }

    private fun log(level: Int, msg: String) = Log.println(level, TAG, msg)
}
