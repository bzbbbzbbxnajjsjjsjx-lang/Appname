package com.example.androidapp.theme.motion

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * User motion preference indicating whether animations should be normal or reduced.
 */
enum class MotionPreference {
    NORMAL,
    REDUCED
}

/**
 * Interface isolating reduced-motion resolution from UI components.
 * Allows testing or swapping system detection without modifying UI layers.
 */
fun interface MotionPreferenceResolver {
    fun resolve(context: Context): MotionPreference
}

/**
 * Default system motion preference resolver:
 * 1. Checks official platform [ValueAnimator.areAnimatorsEnabled] (API 26+).
 * 2. Checks [ValueAnimator.getDurationScale] == 0f (API 33+).
 * 3. Fallbacks to [Settings.Global.ANIMATOR_DURATION_SCALE] == 0f.
 * 4. Gracefully defaults to [MotionPreference.NORMAL] if any security/system query fails.
 */
object SystemMotionPreferenceResolver : MotionPreferenceResolver {
    override fun resolve(context: Context): MotionPreference {
        return try {
            // 1. Official platform animator status check (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!ValueAnimator.areAnimatorsEnabled()) {
                    return MotionPreference.REDUCED
                }
            }

            // 2. Direct system duration scale check (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ValueAnimator.getDurationScale() == 0f) {
                    return MotionPreference.REDUCED
                }
            }

            // 3. Fallback check on global ANIMATOR_DURATION_SCALE setting
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            if (scale == 0f) MotionPreference.REDUCED else MotionPreference.NORMAL
        } catch (_: Throwable) {
            MotionPreference.NORMAL
        }
    }
}

/**
 * CompositionLocal providing the current [MotionPreference].
 */
val LocalMotionPreference = staticCompositionLocalOf<MotionPreference> {
    MotionPreference.NORMAL
}
