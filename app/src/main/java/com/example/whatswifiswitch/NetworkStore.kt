package com.example.whatswifiswitch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Stores the list of saved WiFi networks (SSID + password) as JSON in
 * SharedPreferences. Simple CRUD - no database needed for a handful of
 * networks.
 */
object NetworkStore {

    fun getAll(context: Context): List<WifiNetwork> {
        val json = prefs(context).getString(Prefs.NETWORKS_KEY, null) ?: return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<WifiNetwork>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                WifiNetwork(
                    id = obj.getString("id"),
                    ssid = obj.getString("ssid"),
                    password = obj.getString("password")
                )
            )
        }
        return result
    }

    fun add(context: Context, ssid: String, password: String) {
        val networks = getAll(context).toMutableList()
        networks.add(WifiNetwork(id = UUID.randomUUID().toString(), ssid = ssid, password = password))
        saveAll(context, networks)
    }

    fun update(context: Context, id: String, ssid: String, password: String) {
        val networks = getAll(context).map {
            if (it.id == id) it.copy(ssid = ssid, password = password) else it
        }
        saveAll(context, networks)
    }

    fun delete(context: Context, id: String) {
        val networks = getAll(context).filterNot { it.id == id }
        saveAll(context, networks)
    }

    private fun saveAll(context: Context, networks: List<WifiNetwork>) {
        val array = JSONArray()
        networks.forEach { network ->
            val obj = JSONObject()
            obj.put("id", network.id)
            obj.put("ssid", network.ssid)
            obj.put("password", network.password)
            array.put(obj)
        }
        prefs(context).edit().putString(Prefs.NETWORKS_KEY, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
}
