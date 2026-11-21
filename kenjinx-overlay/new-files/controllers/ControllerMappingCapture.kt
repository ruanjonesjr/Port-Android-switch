package org.kenjinx.android.controllers

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import org.kenjinx.android.GamePadButtonInputId

object ControllerMappingCapture {
    @Volatile private var pending: GamePadButtonInputId? = null
    @Volatile var listener: ((String, Int, GamePadButtonInputId) -> Unit)? = null

    private val capturedKeys = mutableSetOf<Pair<Int, Int>>()

    @Synchronized
    fun begin(target: GamePadButtonInputId) {
        pending = target
    }

    @Synchronized
    fun cancel() {
        pending = null
    }

    @Synchronized
    fun handle(context: Context, event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (!device.isGamepad) return false

        val identity = device.descriptor to event.keyCode

        if (event.action == KeyEvent.ACTION_UP) {
            return capturedKeys.remove(device.id to event.keyCode)
        }

        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) {
            return false
        }

        val target = pending ?: return false
        if (event.keyCode <= 0 || event.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false
        }

        if (ControllerButtonMappings.setMapping(
                context,
                identity.first,
                identity.second,
                target
            )
        ) {
            capturedKeys.add(device.id to event.keyCode)
            pending = null
            listener?.invoke(identity.first, identity.second, target)
            return true
        }

        return false
    }
}
