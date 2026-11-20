package org.kenjinx.android.controllers

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject
import org.kenjinx.android.GamePadButtonInputId

/**
 * Persistent custom physical-controller button mappings.
 * Each Android input descriptor owns an independent mapping.
 */
object ControllerButtonMappings {
    private const val PREF_KEY = "kenjinx_controller_button_mappings_v1"

    private val lock = Any()

    private val allowedTargets = setOf(
        GamePadButtonInputId.A,
        GamePadButtonInputId.B,
        GamePadButtonInputId.X,
        GamePadButtonInputId.Y,
        GamePadButtonInputId.LeftStickButton,
        GamePadButtonInputId.RightStickButton,
        GamePadButtonInputId.LeftShoulder,
        GamePadButtonInputId.RightShoulder,
        GamePadButtonInputId.LeftTrigger,
        GamePadButtonInputId.RightTrigger,
        GamePadButtonInputId.DpadUp,
        GamePadButtonInputId.DpadDown,
        GamePadButtonInputId.DpadLeft,
        GamePadButtonInputId.DpadRight,
        GamePadButtonInputId.Minus,
        GamePadButtonInputId.Plus
    )

    private fun readAll(context: Context): JSONObject {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(PREF_KEY, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun getMapping(
        context: Context,
        descriptor: String,
        keyCode: Int
    ): GamePadButtonInputId? {
        if (descriptor.isBlank() || keyCode <= 0) return null

        return synchronized(lock) {
            val all = readAll(context)
            val device = all.optJSONObject(descriptor) ?: return@synchronized null
            val name = device.optString(keyCode.toString(), "")
            val target = runCatching {
                GamePadButtonInputId.valueOf(name)
            }.getOrNull()

            target?.takeIf { it in allowedTargets }
        }
    }

    fun getMappings(
        context: Context,
        descriptor: String
    ): Map<Int, GamePadButtonInputId> {
        if (descriptor.isBlank()) return emptyMap()

        return synchronized(lock) {
            val device = readAll(context).optJSONObject(descriptor)
                ?: return@synchronized emptyMap()

            buildMap {
                val keys = device.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val keyCode = key.toIntOrNull() ?: continue
                    val target = runCatching {
                        GamePadButtonInputId.valueOf(device.optString(key))
                    }.getOrNull() ?: continue

                    if (target in allowedTargets) {
                        put(keyCode, target)
                    }
                }
            }
        }
    }

    fun setMapping(
        context: Context,
        descriptor: String,
        keyCode: Int,
        target: GamePadButtonInputId
    ): Boolean {
        if (descriptor.isBlank() || keyCode <= 0 || target !in allowedTargets) {
            return false
        }

        return synchronized(lock) {
            try {
                val all = readAll(context)
                val device = all.optJSONObject(descriptor) ?: JSONObject()

                device.put(keyCode.toString(), target.name)
                all.put(descriptor, device)

                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(PREF_KEY, all.toString())
                    .commit()
            } catch (_: Exception) {
                false
            }
        }
    }

    fun removeMapping(
        context: Context,
        descriptor: String,
        keyCode: Int
    ): Boolean {
        if (descriptor.isBlank() || keyCode <= 0) return false

        return synchronized(lock) {
            try {
                val all = readAll(context)
                val device = all.optJSONObject(descriptor)
                    ?: return@synchronized true

                device.remove(keyCode.toString())

                if (device.length() == 0) {
                    all.remove(descriptor)
                } else {
                    all.put(descriptor, device)
                }

                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(PREF_KEY, all.toString())
                    .commit()
            } catch (_: Exception) {
                false
            }
        }
    }

    fun clearProfile(context: Context, descriptor: String): Boolean {
        if (descriptor.isBlank()) return false

        return synchronized(lock) {
            try {
                val all = readAll(context)
                all.remove(descriptor)

                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(PREF_KEY, all.toString())
                    .commit()
            } catch (_: Exception) {
                false
            }
        }
    }
}
