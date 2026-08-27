package com.mihealth.liquidglass

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Minimal launcher activity so the module APK is installable and openable.
 * The real work happens inside 小米运动健康's process; this screen is only a
 * placeholder module info page.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply {
            text = "MiHealth 液态玻璃底栏\n\n" +
                "模块已启用。\n作用域：com.mi.health\n" +
                "请在 LSPosed 中开启本模块并强制停止小米运动健康后重开。\n\n" +
                "效果：整块液态玻璃底栏（折射下方滚动内容，保留原生 Tab）。"
            setPadding(48, 48, 48, 48)
            textSize = 15f
        }
        val layout = LinearLayout(this).apply {
            addView(text)
            setPadding(24, 24, 24, 24)
        }
        setContentView(layout)
    }
}
