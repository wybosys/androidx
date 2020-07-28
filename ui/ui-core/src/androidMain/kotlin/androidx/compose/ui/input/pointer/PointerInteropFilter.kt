/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.input.pointer

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.milliseconds
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.PointerEvent
import androidx.compose.ui.platform.PointerEventPass
import androidx.compose.ui.platform.PointerInputChange
import androidx.compose.ui.platform.anyChangeConsumed
import androidx.compose.ui.platform.changedToDownIgnoreConsumed
import androidx.compose.ui.platform.changedToUpIgnoreConsumed
import androidx.compose.ui.platform.consumeAllChanges
import androidx.compose.ui.viewinterop.AndroidViewHolder

/**
 * A special PointerInputModifier that provides access to the underlying [MotionEvent]s originally
 * dispatched to Compose.
 *
 * While the main intent of this Modifier is to allow arbitrary code to access the original
 * [MotionEvent] dispatched to Compose, for completeness, analogs are provided to allow arbitrary
 * code to interact with the system as if it were an Android View.
 *
 * This includes 2 APIs,
 *
 * 1. [onTouchEvent] has a Boolean return type which is akin to the return type of
 * [View.onTouchEvent]. If the provided [onTouchEvent] returns true, it will continue to receive
 * the event stream (unless the event stream has been intercepted) and if it returns false, it will
 * not.
 *
 * 2. [requestDisallowInterceptTouchEvent] is a lambda that you can optionally provide so that
 * you can later call it (yes, in this case, you call the lambda that you provided) which is akin
 * to calling [ViewParent.requestDisallowInterceptTouchEvent]. When this is called, any
 * associated ancestors in the tree that abide by the contract will act accordingly and will not
 * intercept the even stream.
 *
 * @see [View.onTouchEvent]
 * @see [ViewParent.requestDisallowInterceptTouchEvent]
 */
fun Modifier.pointerInteropFilter(
    requestDisallowInterceptTouchEvent: (RequestDisallowInterceptTouchEvent)? = null,
    onTouchEvent: (MotionEvent) -> Boolean
): Modifier = composed {
    val filter = remember { PointerInteropFilter() }
    filter.onTouchEvent = onTouchEvent
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    filter
}

/**
 * Function that can be passed to [pointerInteropFilter] and then later invoked which provides an
 * analog to [ViewParent.requestDisallowInterceptTouchEvent].
 */
class RequestDisallowInterceptTouchEvent : (Boolean) -> Unit {
    internal var pointerInteropFilter: PointerInteropFilter? = null

    override fun invoke(disallowIntercept: Boolean) {
        pointerInteropFilter?.disallowIntercept = disallowIntercept
    }
}

/**
 * Similar to [pointerInteropFilter] above, but connects directly to an [AndroidViewHolder] for
 * more seamless interop with Android.
 */
internal fun Modifier.pointerInteropFilter(view: AndroidViewHolder<out View>): Modifier {
    val filter = PointerInteropFilter()
    filter.onTouchEvent = { motionEvent ->
        view.dispatchTouchEvent(motionEvent)
    }
    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    view.onRequestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    return this.then(filter)
}

/**
 * The stateful part of pointerInteropFilter that manages the interop with Android.
 *
 * The intent of this PointerInputModifier is to allow Android Views and PointerInputModifiers to
 * interact seamlessly despite the differences in the 2 systems. Below is a detailed explanation
 * for how the interop is accomplished.
 *
 * When the type of event is not a movement event, we dispatch to the Android View as soon as
 * possible (during [PointerEventPass.InitialDown]) so that the Android View can react to down
 * and up events before Compose PointerInputModifiers normally would.
 *
 * When the type of event is a movement event, we dispatch to the Android View during
 * [PointerEventPass.PostDown] to allow Compose PointerInputModifiers to react to movement first,
 * which mimics a parent [ViewGroup] intercepting the event stream.
 *
 * Whenever we are about to call [onTouchEvent], we check to see if anything in Compose
 * consumed any aspect of the pointer input changes, and if they did, we intercept the stream and
 * dispatch ACTION_CANCEL to the Android View if they have already returned true for a call to
 * View#dispatchTouchEvent(...).
 *
 * If we do call [onTouchEvent], and it returns true, we consume all of the changes so that
 * nothing in Compose also responds.
 *
 * If the [requestDisallowInterceptTouchEvent] is provided and called with true, we simply dispatch move
 * events during [PointerEventPass.InitialDown] so that normal PointerInputModifiers don't get a
 * chance to consume first.  Note:  This does mean that it is possible for a Compose
 * PointerInputModifier to "intercept" even after requestDisallowInterceptTouchEvent has been
 * called because consumption can occur during [PointerEventPass.InitialDown].  This may seem
 * like a flaw, but in reality, any PointerInputModifier that consumes that aggressively would
 * likely only do so after some consumption already occurred on a later pass, and this ability to
 * do so is on par with a [ViewGroup]'s ability to override [ViewGroup.dispatchTouchEvent]
 * instead of overriding the more usual [ViewGroup.onTouchEvent] and [ViewGroup
 * .onInterceptTouchEvent].
 *
 * If [requestDisallowInterceptTouchEvent] is later called with false (the Android equivalent of
 * calling [ViewParent.requestDisallowInterceptTouchEvent] is exceedingly rare), we revert back to
 * the normal behavior.
 *
 * If all pointers go up on the pointer interop filter, parents will be set to be allowed to
 * intercept when new pointers go down. [requestDisallowInterceptTouchEvent] must be called again to
 * change that state.
 */
internal class PointerInteropFilter : PointerInputModifier {

    lateinit var onTouchEvent: (MotionEvent) -> Boolean

    var requestDisallowInterceptTouchEvent: RequestDisallowInterceptTouchEvent? = null
        set(value) {
            field?.pointerInteropFilter = null
            field = value
            field?.pointerInteropFilter = this
        }
    internal var disallowIntercept = false

    /**
     * The 3 possible states
     */
    private enum class DispatchToViewState {
        /**
         * We have yet to dispatch a new event stream to the child Android View.
         */
        Unknown,
        /**
         * We have dispatched to the child Android View and it wants to continue to receive
         * events for the current event stream.
         */
        Dispatching,
        /**
         * We intercepted the event stream, or the Android View no longer wanted to receive
         * events for the current event stream.
         */
        NotDispatching
    }

    override val pointerInputFilter =
        object : PointerInputFilter() {

            private var state = DispatchToViewState.Unknown

            override fun onPointerInput(
                changes: List<PointerInputChange>,
                pass: PointerEventPass,
                bounds: IntSize
            ): List<PointerInputChange> {
                // No implementation as onPointerEvent is overridden.
                // The super method will eventually be removed so this is just temporary.
                throw NotImplementedError("This method is temporary and should never be called")
            }

            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize
            ): List<PointerInputChange> {
                @Suppress("NAME_SHADOWING")
                var changes = pointerEvent.changes

                // If we were told to disallow intercept, or if the event was a down or up event,
                // we dispatch to Android as early as possible.  If the event is a move event and
                // we can still intercept, we dispatch to Android after we have a chance to
                // intercept due to movement.
                val dispatchDuringInitialTunnel = disallowIntercept ||
                        changes.fastAny {
                            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                        }

                if (state !== DispatchToViewState.NotDispatching) {
                    if (pass == PointerEventPass.InitialDown && dispatchDuringInitialTunnel) {
                        changes = dispatchToView(pointerEvent)
                    }
                    if (pass == PointerEventPass.PostDown && !dispatchDuringInitialTunnel) {
                        changes = dispatchToView(pointerEvent)
                    }
                }
                if (pass == PointerEventPass.PostDown) {
                    // If all of the changes were up changes, then the "event stream" has ended
                    // and we reset.
                    if (changes.all { it.changedToUpIgnoreConsumed() }) {
                        reset()
                    }
                }
                return changes
            }

            override fun onCancel() {
                // If we are still dispatching to the Android View, we have to send them a
                // cancel event, otherwise, we should not.
                if (state === DispatchToViewState.Dispatching) {
                    emptyCancelMotionEventScope(
                        SystemClock.uptimeMillis().milliseconds
                    ) { motionEvent ->
                        onTouchEvent(motionEvent)
                    }
                    reset()
                }
            }

            /**
             * Resets all of our state to be ready for a "new event stream".
             */
            private fun reset() {
                state = DispatchToViewState.Unknown
                disallowIntercept = false
            }

            /**
             * Dispatches to the Android View.
             *
             * Also consumes aspects of [pointerEvent] and updates our [state] accordingly.
             *
             * Will dispatch ACTION_CANCEL if any aspect of [pointerEvent] has been consumed and
             * update our [state] accordingly.
             *
             * @param pointerEvent The change to dispatch.
             * @return The resulting changes (fully consumed or untouched).
             */
            private fun dispatchToView(pointerEvent: PointerEvent):
                List<PointerInputChange> {

                var changes = pointerEvent.changes

                if (changes.fastAny { it.anyChangeConsumed() }) {
                    // We should no longer dispatch to the Android View.
                    if (state === DispatchToViewState.Dispatching) {
                        // If we were dispatching, send ACTION_CANCEL.
                        pointerEvent.toCancelMotionEventScope(
                            this.layoutCoordinates.localToRoot(Offset.Zero)
                        ) { motionEvent ->
                            onTouchEvent(motionEvent)
                        }
                    }
                    state = DispatchToViewState.NotDispatching
                } else {
                    // Dispatch and update our state with the result.
                    pointerEvent.toMotionEventScope(
                        this.layoutCoordinates.localToRoot(Offset.Zero)
                    ) { motionEvent ->
                        state = if (onTouchEvent(motionEvent)) {
                            DispatchToViewState.Dispatching
                        } else {
                            DispatchToViewState.NotDispatching
                        }
                    }
                    if (state === DispatchToViewState.Dispatching) {
                        // If the Android View claimed the event, consume all changes.
                        changes = changes.map { it.consumeAllChanges() }
                    }
                }
                return changes
            }
        }
}