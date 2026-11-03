package com.jaemin.pleos

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue

class MainActivity : Activity() {

    private var vehicle: Vehicle? = null
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 결과를 표시할 글자 영역
        resultText = TextView(this).apply {
            text = "도어 위치 조회 버튼을 눌러주세요."
            textSize = 22f
        }

        // 조회 버튼
        val queryButton = Button(this).apply {
            text = "도어 위치 조회"
            isEnabled = false
            setOnClickListener {
                queryDoorPosition()
            }
        }

        // 글자와 버튼을 세로로 배치
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
            addView(resultText)
            addView(queryButton)
        }

        setContentView(layout)

        // Vehicle SDK 초기화
        try {
            val sdk = Vehicle(this)
            vehicle = sdk
            sdk.initialize()
            queryButton.isEnabled = true
        } catch (e: Exception) {
            resultText.text = "SDK 초기화 실패: ${e.message}"
            Log.e("PleosTest", "SDK 초기화 실패", e)
        }
    }

    private fun queryDoorPosition() {
        val sdk = vehicle ?: return

        resultText.text = "조회 중..."

        try {
            val door = sdk.getDoor()

            door.getCurrentDoorPosition(
                area = DoorArea(DoorValue.ROW_1_LEFT),

                onComplete = { pos: Int? ->
                    Log.d("PleosTest", "도어 위치: $pos")

                    runOnUiThread {
                        resultText.text = if (pos != null) {
                            "앞줄 왼쪽 도어 위치 값: $pos"
                        } else {
                            "응답은 받았지만 도어 위치 값이 없습니다."
                        }
                    }
                },

                onFailed = { e: Exception ->
                    Log.e("PleosTest", "도어 위치 조회 실패", e)

                    runOnUiThread {
                        resultText.text = "조회 실패: ${e.message}"
                    }
                }
            )
        } catch (e: Exception) {
            resultText.text = "호출 실패: ${e.message}"
            Log.e("PleosTest", "API 호출 실패", e)
        }
    }

    override fun onDestroy() {
        vehicle?.release()
        vehicle = null
        super.onDestroy()
    }
}