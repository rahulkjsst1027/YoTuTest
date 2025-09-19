package com.youtubeapis.flash

class FlashlightPatterns(private val flashlight: FlashlightHelper) {

    // 1. Normal Blink
    fun blink() {
        flashlight.startPattern(listOf(500L, 500L))
    }

    // 2. Fast Blink
    fun fastBlink() {
        flashlight.startPattern(listOf(100L, 100L))
    }

    // 3. Slow Blink
    fun slowBlink() {
        flashlight.startPattern(listOf(1000L, 1000L))
    }

    // 4. SOS (... --- ...)
    fun sos() {
        val sosPattern = listOf(
            200L, 200L, 200L, 200L, 200L, 600L,
            600L, 200L, 600L, 200L, 600L, 600L,
            200L, 200L, 200L, 200L, 200L, 600L
        )
        flashlight.startPattern(sosPattern)
    }

    // 5. Strobe Light
    fun strobe() {
        flashlight.startPattern(listOf(50L, 50L))
    }

    // 6. Police Light
    fun police() {
        val policePattern = listOf(150L, 150L, 400L, 200L)
        flashlight.startPattern(policePattern)
    }

    // 7. Heartbeat (double pulse)
    fun heartbeat() {
        val pattern = listOf(100L, 100L, 100L, 400L)
        flashlight.startPattern(pattern)
    }

    // 8. Breathing Effect
    fun breathing() {
        val pattern = mutableListOf<Long>()
        for (i in 100..500 step 100) {
            pattern.add(i.toLong())
            pattern.add(i.toLong())
        }
        for (i in 500 downTo 100 step 100) {
            pattern.add(i.toLong())
            pattern.add(i.toLong())
        }
        flashlight.startPattern(pattern)
    }

    // 9. Random Blink
    fun randomBlink() {
        val pattern = List(20) { (100..1000).random().toLong() }
        flashlight.startPattern(pattern)
    }

    // 10. Emergency Beacon
    fun emergency() {
        val pattern = listOf(1000L, 1000L, 100L, 3000L)
        flashlight.startPattern(pattern)
    }

    // 11. Countdown Timer Blink
    fun countdown(seconds: Int) {
        val pattern = MutableList(seconds * 2) { 500L }
        flashlight.startPattern(pattern)
    }

    // 12. Custom Morse Code
    fun morseCode(message: String) {
        val pattern = mutableListOf<Long>()
        message.uppercase().forEach { c ->
            when (c) {
                'A' -> pattern.addAll(listOf(200L, 200L, 600L, 600L))
                'B' -> pattern.addAll(listOf(600L, 200L, 200L, 200L, 200L, 200L, 200L, 200L))
                'C' -> pattern.addAll(listOf(600L, 200L, 200L, 200L, 600L, 200L, 200L, 200L))
                'S' -> pattern.addAll(listOf(200L, 200L, 200L, 200L, 200L, 600L))
                'O' -> pattern.addAll(listOf(600L, 200L, 600L, 200L, 600L, 600L))
                ' ' -> pattern.add(1000L)
            }
        }
        flashlight.startPattern(pattern)
    }

    fun stop() {
        flashlight.stopPattern()
    }

    fun on() {
        flashlight.setFlash(true)
    }
}
