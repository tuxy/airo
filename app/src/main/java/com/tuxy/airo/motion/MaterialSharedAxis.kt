package com.tuxy.airo.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

private const val ProgressThreshold = 0.35f

private val Int.ForOutgoing: Int
    get() = (this * ProgressThreshold).toInt()

private val Int.ForIncoming: Int
    get() = this - this.ForOutgoing

/*
* Transitions taken from Read you's repository
* */

/**
 * [materialSharedAxisXIn] allows to switch between two layouts that have a navigational relationship.
 * For example, a layout could navigate to a detail page and back.
 *
 * @param initialOffsetX the starting offset X of the slide-in animation.
 * @param durationMillis the duration of the animation.
 */
fun materialSharedAxisXIn(
    initialOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = MotionConstants.DEFAULT_MOTION_DURATION,
): EnterTransition = slideInHorizontally(
    animationSpec = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    ),
    initialOffsetX = initialOffsetX
) + fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis.ForIncoming,
        delayMillis = durationMillis.ForOutgoing,
        easing = LinearOutSlowInEasing
    )
) +
scaleIn(
    initialScale = 0.9f,
    animationSpec = tween(300)
)

/**
 * [materialSharedAxisXOut] allows an [ExitTransition] to be created using a shared X-axis.
 *
 * @param targetOffsetX defines the end x position of the content. This should typically be a
 * negative value for items that are moving towards the start/left, and positive for items that
 * are moving towards the end/right.
 * @param durationMillis the duration of the transition.
 */
fun materialSharedAxisXOut(
    targetOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = MotionConstants.DEFAULT_MOTION_DURATION,
): ExitTransition = slideOutHorizontally(
    animationSpec = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    ),
    targetOffsetX = targetOffsetX
) + fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis.ForOutgoing,
        delayMillis = 0,
        easing = FastOutLinearInEasing
    )
) +
scaleOut(
    targetScale = 0.9f,
    animationSpec = tween(300)
)


/**
 * Unused functions... Could be useful, but currently the app functions smoothly without.
 */

fun materialSharedAxisYIn(
    initialOffsetY: (fullWidth: Int) -> Int,
    durationMillis: Int = MotionConstants.DEFAULT_MOTION_DURATION,
): EnterTransition = slideInVertically(
    animationSpec = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    ),
    initialOffsetY = initialOffsetY
) + fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis.ForIncoming,
        delayMillis = durationMillis.ForOutgoing,
        easing = LinearOutSlowInEasing
    )
)

fun materialSharedAxisYOut(
    targetOffsetY: (fullWidth: Int) -> Int,
    durationMillis: Int = MotionConstants.DEFAULT_MOTION_DURATION,
): ExitTransition = slideOutVertically(
    animationSpec = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing
    ),
    targetOffsetY = targetOffsetY
) + fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis.ForOutgoing,
        delayMillis = 0,
        easing = FastOutLinearInEasing
    )
)