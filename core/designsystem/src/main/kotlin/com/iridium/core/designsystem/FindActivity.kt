package com.iridium.core.designsystem

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Walks the context wrapper chain to the hosting [FragmentActivity].
 * A direct `as? FragmentActivity` cast silently breaks when the context
 * is wrapped (themed, tinted, or Compose-view contexts all wrap).
 */
fun Context.findActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
