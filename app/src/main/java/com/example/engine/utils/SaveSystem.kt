package com.example.engine.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaveSystem(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("engine_save_data", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val key = "cyber_key_0x9A".toByteArray()

    var lifetimeActivationState: Boolean = false
        private set
    var totalFiringCount: Int = 0
        private set
    var highScore: Int = 0
        private set
    var totalElapsedRuntime: Float = 0f
        private set

    init {
        loadData()
        if (!lifetimeActivationState) {
            lifetimeActivationState = true
            saveDataAsync()
        }
    }

    private fun loadData() {
        lifetimeActivationState = decryptBoolean(prefs.getString("activation", null), false)
        totalFiringCount = decryptInt(prefs.getString("firingCount", null), 0)
        highScore = decryptInt(prefs.getString("highScore", null), 0)
        totalElapsedRuntime = decryptFloat(prefs.getString("runtime", null), 0f)
    }

    fun updateMetrics(firingCount: Int, score: Int, runtime: Float) {
        var changed = false
        if (firingCount > totalFiringCount) {
            totalFiringCount = firingCount
            changed = true
        }
        if (score > highScore) {
            highScore = score
            changed = true
        }
        if (runtime > totalElapsedRuntime) {
            totalElapsedRuntime = runtime
            changed = true
        }
        if (changed) {
            saveDataAsync()
        }
    }

    private fun saveDataAsync() {
        scope.launch {
            prefs.edit()
                .putString("activation", encryptBoolean(lifetimeActivationState))
                .putString("firingCount", encryptInt(totalFiringCount))
                .putString("highScore", encryptInt(highScore))
                .putString("runtime", encryptFloat(totalElapsedRuntime))
                .apply()
        }
    }

    private fun encrypt(data: String): String {
        val bytes = data.toByteArray()
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decrypt(data: String?): String? {
        if (data == null) return null
        return try {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            for (i in bytes.indices) {
                bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
            String(bytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun encryptBoolean(value: Boolean) = encrypt(value.toString())
    private fun decryptBoolean(data: String?, default: Boolean): Boolean {
        val dec = decrypt(data) ?: return default
        return dec.toBooleanStrictOrNull() ?: default
    }

    private fun encryptInt(value: Int) = encrypt(value.toString())
    private fun decryptInt(data: String?, default: Int): Int {
        val dec = decrypt(data) ?: return default
        return dec.toIntOrNull() ?: default
    }

    private fun encryptFloat(value: Float) = encrypt(value.toString())
    private fun decryptFloat(data: String?, default: Float): Float {
        val dec = decrypt(data) ?: return default
        return dec.toFloatOrNull() ?: default
    }
}
