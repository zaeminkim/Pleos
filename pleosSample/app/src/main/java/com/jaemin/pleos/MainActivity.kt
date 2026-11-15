package com.jaemin.pleos

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import java.io.File
import java.time.Instant

class MainActivity : AppCompatActivity() {
    private val api = GilnoteApi()
    private lateinit var store: CarStore
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var connect: Button
    private lateinit var qr: ImageView
    private lateinit var code: TextView
    private val notes = linkedMapOf<Long, CarNote>()
    private val prefs by lazy { getSharedPreferences("played_queue", MODE_PRIVATE) }
    private var player: MediaPlayer? = null
    private var playingId: Long? = null
    private var expiryJob: Job? = null
    private var placePlayback: PlacePlayback? = null
    private val attemptedPlaces = mutableSetOf<Long>()
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as android.media.AudioManager }
    private val audioFocus by lazy {
        android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { change -> if (change < 0) stopPlayback() }
            .build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = CarStore(this)
        buildScreen()
        placePlayback = PlacePlayback(this, { lat, lng ->
            if (playingId == null) {
                val candidate = notes.values.firstOrNull {
                    it.id !in attemptedPlaces && !prefs.contains(it.id.toString()) &&
                        it.isWithin(lat, lng) && audioFile(it.id).length() > 0
                }
                if (candidate != null) {
                    attemptedPlaces.add(candidate.id)
                    play(candidate)
                }
            }
        }, { message -> stopPlayback(); status.text = message })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        status.text = "쪽지 확인 중…"
                        val token = withContext(Dispatchers.IO) {
                            store.carToken ?: api.register("우리 가족 차").also { store.carToken = it }
                        }
                        connect.isEnabled = true
                        refresh(token)
                        status.text = "받은 쪽지 ${notes.size}개 · 20초마다 확인"
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        status.text = "연결 실패 · 20초 후 재시도: ${e.message}"
                    }
                    delay(20_000)
                }
            }
        }
    }

    private fun audioFile(id: Long) = File(filesDir, "note_$id")

    private suspend fun refresh(token: String) {
        // 완료 기록은 서버 확인 전까지 디스크에 남겨 재시작 후에도 재전송한다.
        for ((id, timestamp) in prefs.all) {
            withContext(Dispatchers.IO) { api.played(token, id.toLong(), timestamp as String) }
            prefs.edit().remove(id).commit()
            audioFile(id.toLong()).delete()
        }
        val fetched = withContext(Dispatchers.IO) { api.notes(token) }
        notes.keys.retainAll(fetched.map { it.id }.toSet())
        for (note in fetched) {
            if (prefs.contains(note.id.toString())) continue
            withContext(Dispatchers.IO) {
                val file = audioFile(note.id)
                if (!file.exists() || file.length() == 0L) api.downloadAudio(token, note, file)
            }
            if (!prefs.contains(note.id.toString())) notes[note.id] = note
        }
        showNotes()
    }

    private fun connectFamily() {
        val token = store.carToken ?: return
        connect.isEnabled = false
        lifecycleScope.launch {
            try {
                val pair = withContext(Dispatchers.IO) { api.pairCode(token) }
                val expires = Instant.parse(pair.expiresAt)
                qr.setImageBitmap(qrBitmap("${Config.WEB_BASE}/join.html?code=${pair.code}"))
                expiryJob?.cancel()
                expiryJob = lifecycleScope.launch {
                    while (true) {
                        val seconds = (expires.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
                        code.text = "${pair.code} · 남은 시간 ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                        if (seconds == 0L) break
                        delay(1000)
                    }
                    qr.setImageDrawable(null)
                    code.text = "연결 코드 만료 · 가족 연결을 다시 눌러주세요."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status.text = "연결 코드 발급 실패: ${e.message}"
            } finally {
                connect.isEnabled = true
            }
        }
    }

    private var selectedId: Long? = null
    private var detailOpen = false
    private var familyOpen = false
    private lateinit var content: LinearLayout
    private val ink = android.graphics.Color.parseColor("#131417")
    private val teal = android.graphics.Color.parseColor("#123D35")
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun surface(color: String, radius: Int = 28) = android.graphics.drawable.GradientDrawable().apply {
        setColor(android.graphics.Color.parseColor(color))
        cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float = 28f, light: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(if (light) android.graphics.Color.WHITE else ink)
        setPadding(0, dp(8), 0, dp(8))
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun action(value: String, onClick: () -> Unit) = Button(this).apply {
        text = value
        textSize = 24f
        isAllCaps = false
        setTextColor(ink)
        background = surface("#E9EBEC")
        minHeight = dp(80)
        setPadding(dp(24), dp(12), dp(24), dp(12))
        setOnClickListener { onClick() }
    }
    private fun buildScreen() {
        val root = column().apply {
            setPadding(dp(40), dp(32), dp(40), dp(32))
            setBackgroundColor(android.graphics.Color.parseColor("#F5F6F7"))
        }
        val header = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val title = label("길쪽지", 36f).apply { setTypeface(typeface, android.graphics.Typeface.BOLD) }
        header.addView(title, LinearLayout.LayoutParams(0, dp(100), 1f))
        connect = action("가족 연결") {
            familyOpen = true
            showNotes()
            connectFamily()
        }.apply { isEnabled = store.carToken != null }
        header.addView(connect)
        root.addView(header)
        status = label("쪽지 확인 중…", 20f)
        root.addView(status)
        content = column()
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        list = column()
        qr = ImageView(this).apply { contentDescription = "가족 연결 QR 코드" }
        code = label("연결 코드를 발급하고 있어요", 28f)
        setContentView(root)
        showNotes()
    }
    private fun condition(note: CarNote): String = if (note.triggerType == "PLACE") {
        "장소 쪽지 · ${note.placeLabel ?: "지정한 장소"}"
    } else {
        "동승자 쪽지 · ${note.seat ?: "지정한 좌석"}"
    }
    private fun showNotes() {
        content.removeAllViews()
        if (familyOpen) {
            content.addView(action("뒤로") { familyOpen = false; showNotes() })
            content.addView(label("가족 연결", 40f))
            content.addView(label("휴대폰 카메라로 QR을 찍으세요", 30f))
            (qr.parent as? android.view.ViewGroup)?.removeView(qr)
            (code.parent as? android.view.ViewGroup)?.removeView(code)
            content.addView(qr, LinearLayout.LayoutParams(dp(300), dp(300)).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL })
            content.addView(code)
            content.addView(action("새 코드 받기") { if (connect.isEnabled) connectFamily() })
            return
        }
        val wide = resources.configuration.screenWidthDp >= 1100
        val row = LinearLayout(this).apply { orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        content.addView(row)
        list = column()
        if (wide || !detailOpen) {
            row.addView(list, if (wide) LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(40) } else LinearLayout.LayoutParams(-1, -2))
            list.addView(label("받은 쪽지 ${notes.size}개", 40f))
            if (notes.isEmpty()) list.addView(label("아직 받은 쪽지가 없어요", 28f))
            for (note in notes.values) {
                val item = action("${note.from}의 쪽지\n${condition(note)}") {
                    selectedId = note.id
                    detailOpen = true
                    showNotes()
                }
                item.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                item.background = surface("#FFFFFF", 26)
                list.addView(item, LinearLayout.LayoutParams(-1, dp(136)).apply { bottomMargin = dp(24) })
            }
        }
        val note = notes[playingId] ?: notes[selectedId] ?: notes.values.firstOrNull()
        if (note != null && (wide || detailOpen)) {
            val card = column().apply {
                setPadding(dp(48), dp(48), dp(48), dp(48))
                background = surface("#123D35", 38)
                minimumHeight = dp(650)
            }
            row.addView(card, if (wide) LinearLayout.LayoutParams(0, -2, 1f) else LinearLayout.LayoutParams(-1, -2))
            if (!wide) card.addView(action("뒤로") { detailOpen = false; showNotes() })
            card.addView(label("${note.from}의 쪽지", 48f, true))
            card.addView(label(condition(note), 28f, true))
            card.addView(label(if (playingId != null) "재생 중" else "정차 중에 음성 쪽지를 들을 수 있어요.", 24f, true))
            card.addView(action(if (playingId != null) "재생 중지" else "쪽지 듣기") {
                if (playingId != null) stopPlayback() else play(note)
            })
        }
    }

    private fun play(note: CarNote) {
        if (playingId != null) return
        playingId = note.id
        selectedId = note.id
        detailOpen = true
        showNotes()
        val started = placePlayback?.announce("${note.from}의 쪽지가 도착했어요.") {
            if (playingId == note.id) playRecording(note)
        } == true
        if (!started) {
            stopPlayback()
            attemptedPlaces.remove(note.id)
            status.text = "음성 안내 준비 중이에요. 잠시 후 다시 시도해주세요."
        }
    }

    private fun playRecording(note: CarNote) {
        if (audioManager.requestAudioFocus(audioFocus) != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopPlayback()
            status.text = "다른 음성 안내가 끝난 뒤 다시 재생해주세요."
            return
        }
        playingId = note.id
        showNotes()
        val media = MediaPlayer()
        player = media
        try {
            media.setDataSource(audioFile(note.id).absolutePath)
            media.setOnPreparedListener { it.start() }
            media.setOnErrorListener { _, _, _ ->
                stopPlayback()
                status.text = "음성 재생 실패 · 다시 시도해주세요."
                true
            }
            media.setOnCompletionListener {
                val saved = prefs.edit().putString(note.id.toString(), Instant.now().toString()).commit()
                if (saved) notes.remove(note.id)
                stopPlayback()
                status.text = if (saved) "들었어요 · 다음 동기화 때 서버에 전달합니다." else "완료 기록 저장 실패 · 다시 시도해주세요."
            }
            media.prepareAsync()
        } catch (e: Exception) {
            stopPlayback()
            status.text = "재생 실패: ${e.message}"
        }
    }

    private fun stopPlayback() {
        placePlayback?.cancelSpeech()
        audioManager.abandonAudioFocusRequest(audioFocus)
        player?.release()
        player = null
        playingId = null
        showNotes()
    }

    override fun onStart() {
        super.onStart()
        placePlayback?.start()
    }

    override fun onDestroy() {
        placePlayback?.release()
        super.onDestroy()
    }

    override fun onStop() {
        placePlayback?.stop()
        stopPlayback()
        super.onStop()
    }
}
