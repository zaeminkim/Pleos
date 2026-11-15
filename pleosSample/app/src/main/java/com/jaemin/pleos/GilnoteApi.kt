package com.jaemin.pleos

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ApiException(val code: Int, body: String) : Exception("HTTP $code: $body")

data class PairCode(val code: String, val expiresAt: String)

data class CarNote(
    val id: Long,
    val from: String,
    val triggerType: String,   // PLACE 또는 SEAT
    val lat: Double?,
    val lng: Double?,
    val radiusM: Int?,
    val placeLabel: String?,
    val seat: String?,
    val audioUrl: String
)

class GilnoteApi(private val base: String = Config.API_BASE) {

    private val client = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    // 모든 요청 공통: ngrok 무료 경고 페이지 건너뛰기
    private fun request(path: String) = Request.Builder()
        .url(base + path)
        .header("ngrok-skip-browser-warning", "true")

    // 요청 보내고 본문 받기. 200번대가 아니면 예외
    private fun call(req: Request): String =
        client.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw ApiException(res.code, body)
            body
        }

    // API 1. 차 등록 → carToken
    fun register(name: String): String {
        val body = JSONObject().put("name", name).toString().toRequestBody(json)
        val res = call(request("/api/cars/register").post(body).build())
        return JSONObject(res).getString("carToken")
    }

    // API 2. 연결 코드 (6자리, 3분 만료)
    fun pairCode(carToken: String): PairCode {
        val req = request("/api/cars/pair-code")
            .header("X-Car-Token", carToken)
            .post(ByteArray(0).toRequestBody(null))
            .build()
        val o = JSONObject(call(req))
        return PairCode(o.getString("code"), o.getString("expiresAt"))
    }

    // API 6. 아직 안 들은 쪽지 목록 (서버가 "차에 도착"으로 바꿈)
    fun notes(carToken: String): List<CarNote> {
        val req = request("/api/cars/notes").header("X-Car-Token", carToken).build()
        val arr = JSONArray(call(req))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CarNote(
                id = o.getLong("noteId"),
                from = o.optString("from"),
                triggerType = o.getString("triggerType"),
                lat = if (o.isNull("lat")) null else o.getDouble("lat"),
                lng = if (o.isNull("lng")) null else o.getDouble("lng"),
                radiusM = if (o.isNull("radiusM")) null else o.getInt("radiusM"),
                placeLabel = if (o.isNull("placeLabel")) null else o.getString("placeLabel"),
                seat = if (o.isNull("seat")) null else o.getString("seat"),
                audioUrl = o.getString("audioUrl")
            )
        }
    }

    // API 7. 음성 파일 내려받아 저장
    fun downloadAudio(carToken: String, note: CarNote, saveTo: File) {
        val req = request(note.audioUrl).header("X-Car-Token", carToken).build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw ApiException(res.code, res.body?.string().orEmpty())
            val temporary = File(saveTo.parentFile, saveTo.name + ".part")
            try {
                val body = res.body ?: throw java.io.IOException("Empty audio response")
                temporary.outputStream().use { out -> body.byteStream().copyTo(out) }
                if (temporary.length() == 0L || !temporary.renameTo(saveTo)) {
                    throw java.io.IOException("Audio download could not be saved")
                }
            } finally {
                temporary.delete()
            }
        }
    }

    // API 8. 들었어요
    fun played(carToken: String, noteId: Long, playedAt: String) {
        val req = request("/api/cars/notes/$noteId/played")
            .header("X-Car-Token", carToken)
            .post(JSONObject().put("playedAt", playedAt).toString().toRequestBody(json))
            .build()
        call(req)
    }
}


