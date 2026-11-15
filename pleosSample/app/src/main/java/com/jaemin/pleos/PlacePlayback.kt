package com.jaemin.pleos

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.listener.EventListener

/** Activity-scoped location polling. All client callbacks run on the main thread. */
class PlacePlayback(context: Context, private val location: (Double, Double) -> Unit,
                    private val error: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val navi = NaviHelper(context.applicationContext)
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private var active = false
    private var closed = false
    private var connected = false
    private var ready = false
    private var completion: (() -> Unit)? = null
    private val timeout = Runnable { cancelSpeech(); error("음성 안내 시간이 초과됐어요.") }
    private val poll = object : Runnable {
        override fun run() {
            if (!active || closed) return
            if (connected) runCatching { navi.getCurrentLocationInfo() }
                .onFailure { error("현재 위치를 확인할 수 없어요.") }
            handler.postDelayed(this, 2000)
        }
    }
    private val navigation = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            handler.post { if (active && !closed) location(info.latitude, info.longitude) }
        }
        override fun onError(errorCode: NaviErrorCode) {
            handler.post { if (active && !closed) error("위치 조회 실패: $errorCode") }
        }
    }
    private val speech = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onStop() = Unit
        override fun onDone() {
            handler.post {
                handler.removeCallbacks(timeout)
                val callback = completion
                completion = null
                if (active && !closed) callback?.invoke()
            }
        }
        override fun onError(errorType: ErrorType) {
            handler.post { cancelSpeech(); if (active && !closed) error("음성 안내 실패: $errorType") }
        }
    }
    init {
        runCatching {
            navi.initialize(onConnected = {
                handler.post { if (!closed) { connected = true; navi.addListener(navigation) } }
            }, onFailed = { handler.post { if (!closed) error("내비게이션 연결 실패") } })
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    handler.post {
                        if (!closed) {
                            tts.addEventListener(speech)
                            tts.setLocale(java.util.Locale.KOREA)
                            ready = true
                        }
                    }
                }
                override fun onFailure(errorType: ErrorType) {
                    handler.post { if (!closed) error("TTS 연결 실패: $errorType") }
                }
            })
        }.onFailure { error("차량 SDK 초기화 실패: ${it.message}") }
    }
    fun start() { active = true; handler.removeCallbacks(poll); handler.post(poll) }
    fun stop() { active = false; handler.removeCallbacks(poll); cancelSpeech() }
    fun announce(text: String, done: () -> Unit): Boolean {
        if (!ready || !active || closed || completion != null) return false
        completion = done
        return runCatching {
            tts.speak(text, true)
            handler.postDelayed(timeout, 20000)
            true
        }.getOrElse { completion = null; false }
    }
    fun cancelSpeech() {
        completion = null
        handler.removeCallbacks(timeout)
        runCatching { tts.stop() }
    }
    fun release() {
        stop()
        closed = true
        runCatching { navi.removeListener(navigation) }
        runCatching { navi.release() }
        runCatching { tts.removeEventListener(speech) }
        runCatching { tts.release() }
        handler.removeCallbacksAndMessages(null)
    }
}

internal fun CarNote.isWithin(latitude: Double, longitude: Double): Boolean {
    val targetLat = lat ?: return false
    val targetLng = lng ?: return false
    val radius = radiusM ?: return false
    if (triggerType != "PLACE" || radius <= 0 ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0 ||
        targetLat !in -90.0..90.0 || targetLng !in -180.0..180.0) return false
    val result = FloatArray(1)
    Location.distanceBetween(latitude, longitude, targetLat, targetLng, result)
    return result[0] <= radius
}
