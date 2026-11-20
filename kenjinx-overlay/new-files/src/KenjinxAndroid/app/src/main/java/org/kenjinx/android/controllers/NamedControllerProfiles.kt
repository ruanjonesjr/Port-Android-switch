package org.kenjinx.android.controllers

import android.content.Context
import androidx.preference.PreferenceManager
import org.kenjinx.android.GamePadButtonInputId
import org.json.JSONObject
import org.json.JSONArray

/**
 * Perfis nomeados de remapeamento.
 * Separado dos perfis de pareamento físico/JoyCon.
 */
object NamedControllerProfiles {
    private const val KEY = "kenjinx_named_controller_profiles"
    private const val ACTIVE = "kenjinx_active_controller_profile"

    fun names(context: Context): List<String> {
        val root = load(context)
        return (0 until root.length()).map { root.getJSONObject(root.names()!!.getString(it)).optString("name") }
            .filter { it.isNotBlank() }.sorted()
    }

    fun activeName(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context).getString(ACTIVE, "Padrão") ?: "Padrão"

    fun save(context: Context, name: String, descriptor: String, mappings: Map<Int, GamePadButtonInputId>) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Digite um nome para o perfil." }
        val root = load(context)
        val profile = JSONObject()
        profile.put("name", clean)
        profile.put("descriptor", descriptor)
        val map = JSONObject()
        mappings.forEach { (key, value) -> map.put(key.toString(), value.name) }
        profile.put("mappings", map)
        root.put(clean, profile)
        prefs(context).edit().putString(KEY, root.toString()).putString(ACTIVE, clean).apply()
    }

    fun loadMappings(context: Context, name: String): Map<Int, GamePadButtonInputId> {
        val profile = load(context).optJSONObject(name) ?: return emptyMap()
        val map = profile.optJSONObject("mappings") ?: return emptyMap()
        val result = mutableMapOf<Int, GamePadButtonInputId>()
        val keys = map.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val button = runCatching { GamePadButtonInputId.valueOf(map.getString(key)) }.getOrNull()
            if (button != null) key.toIntOrNull()?.let { result[it] = button }
        }
        return result
    }

    fun descriptorFor(context: Context, name: String): String {
        return load(context).optJSONObject(name)?.optString("descriptor", "") ?: ""
    }

    fun setActive(context: Context, name: String) {
        prefs(context).edit().putString(ACTIVE, name).apply()
    }

    fun delete(context: Context, name: String) {
        val root = load(context)
        root.remove(name)
        val p = prefs(context).edit().putString(KEY, root.toString())
        if (activeName(context) == name) p.putString(ACTIVE, "Padrão")
        p.apply()
    }

    private fun prefs(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun load(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY, "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }
}
