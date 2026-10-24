package com.epfl.esl.simongame.wear

import org.json.JSONArray
import org.json.JSONObject

object WearJson {

    fun encodeStartCapture(req: StartCaptureRequest): ByteArray {
        val o = JSONObject()
        o.put("turnId", req.turnId)
        o.put("durationMs", req.durationMs)
        o.put("moveName", req.moveName)
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeStartCapture(bytes: ByteArray): StartCaptureRequest {
        val o = JSONObject(bytes.toString(Charsets.UTF_8))
        return StartCaptureRequest(
            turnId = o.getLong("turnId"),
            durationMs = o.getLong("durationMs"),
            moveName = o.optString("moveName", "")
        )
    }

    fun encodeGameState(state: GameState): ByteArray {
        val o = JSONObject()
        o.put("phase", state.phase)
        o.put("moveName", state.moveName)
        o.put("round", state.round)
        o.put("score", state.score)
        o.put("timeLeftSec", state.timeLeftSec)
        o.put("isRecording", state.isRecording)
        if (state.lastResultCorrect != null) {
            o.put("lastResultCorrect", state.lastResultCorrect)
        }
        o.put("retryCount", state.retryCount)
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeGameState(bytes: ByteArray): GameState {
        val o = JSONObject(bytes.toString(Charsets.UTF_8))
        return GameState(
            phase = o.getString("phase"),
            moveName = o.getString("moveName"),
            round = o.getInt("round"),
            score = o.getInt("score"),
            timeLeftSec = o.getInt("timeLeftSec"),
            isRecording = o.getBoolean("isRecording"),
            lastResultCorrect = if (o.has("lastResultCorrect")) o.getBoolean("lastResultCorrect") else null,
            retryCount = o.optInt("retryCount", 0)
        )
    }

    fun encodeMotionWindow(w: MotionWindow): ByteArray {
        val o = JSONObject()
        o.put("turnId", w.turnId)
        o.put("accel", encodeSamples(w.accel))
        o.put("gyro", encodeSamples(w.gyro))
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeMotionWindow(bytes: ByteArray): MotionWindow {
        val o = JSONObject(bytes.toString(Charsets.UTF_8))
        return MotionWindow(
            turnId = o.getLong("turnId"),
            accel = decodeSamples(o.getJSONArray("accel")),
            gyro = decodeSamples(o.getJSONArray("gyro"))
        )
    }

    private fun encodeSamples(samples: List<Vec3Sample>): JSONArray {
        val arr = JSONArray()
        for (s in samples) {
            val o = JSONObject()
            o.put("tMs", s.tMs)
            o.put("x", s.x)
            o.put("y", s.y)
            o.put("z", s.z)
            arr.put(o)
        }
        return arr
    }

    private fun decodeSamples(arr: JSONArray): List<Vec3Sample> {
        val out = ArrayList<Vec3Sample>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Vec3Sample(
                    tMs = o.getLong("tMs"),
                    x = o.getDouble("x").toFloat(),
                    y = o.getDouble("y").toFloat(),
                    z = o.getDouble("z").toFloat()
                )
            )
        }
        return out
    }
}
