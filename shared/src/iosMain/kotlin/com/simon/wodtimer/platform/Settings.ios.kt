package com.simon.wodtimer.platform

import platform.Foundation.NSUserDefaults

actual object Settings {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getInt(key: String, default: Int): Int {
        if (defaults.objectForKey(key) == null) return default
        return defaults.integerForKey(key).toInt()
    }

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    actual fun getString(key: String): String? = defaults.stringForKey(key)

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
