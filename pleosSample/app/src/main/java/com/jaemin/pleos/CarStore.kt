package com.jaemin.pleos

import android.content.Context

// 앱을 껐다 켜도 남는 작은 저장소. 차 토큰을 보관
class CarStore(context: Context) {
    private val prefs = context.getSharedPreferences("gilnote", Context.MODE_PRIVATE)

    var carToken: String?
        get() = prefs.getString("carToken", null)
        set(value) { prefs.edit().putString("carToken", value).apply() }
}

