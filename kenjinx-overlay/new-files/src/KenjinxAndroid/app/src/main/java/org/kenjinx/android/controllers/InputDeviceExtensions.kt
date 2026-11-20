package org.kenjinx.android.controllers

import android.view.InputDevice

val InputDevice.isGamepad: Boolean
    get() =
        (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
        (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
