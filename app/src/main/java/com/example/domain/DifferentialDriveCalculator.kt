package com.example.domain

import com.example.model.MotorCommandFormat
import com.example.model.MotorOutput
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Computes differential-drive left and right motor speeds from virtual joystick coordinates.
 *
 * Coordinate convention:
 * - [joystickX] in [-1f, 1f]: negative = left, positive = right.
 * - [joystickY] in [-1f, 1f]: positive = UP (forward), negative = DOWN (reverse).
 */
object DifferentialDriveCalculator {

    fun calculateMotors(
        joystickX: Float,
        joystickY: Float,
        maxPwm: Int = 255,
        deadzone: Float = 0.08f,
        speedScale: Float = 1.0f,
        invertLeft: Boolean = false,
        invertRight: Boolean = false
    ): MotorOutput {
        val clampedX = joystickX.coerceIn(-1f, 1f)
        val clampedY = joystickY.coerceIn(-1f, 1f)
        val rawMagnitude = hypot(clampedX, clampedY).coerceAtMost(1f)

        if (rawMagnitude < deadzone) {
            return MotorOutput(
                leftSpeed = 0,
                rightSpeed = 0,
                normalizedX = 0f,
                normalizedY = 0f,
                magnitude = 0f,
                directionLabel = "DETENIDO"
            )
        }

        // Rescale outside deadzone for smooth low-speed control
        val effectiveMag = ((rawMagnitude - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
        val scaleFactor = if (rawMagnitude > 0f) effectiveMag / rawMagnitude else 0f
        val forward = clampedY * scaleFactor
        val turn = clampedX * scaleFactor

        // Arcade differential drive mixing:
        // Forward (+Y) -> both motors positive
        // Backward (-Y) -> both motors negative
        // Right (+X) -> left faster/forward, right slower/reverse -> turns vehicle right
        // Left (-X) -> right faster/forward, left slower/reverse -> turns vehicle left
        var leftMix = forward + turn
        var rightMix = forward - turn

        val maxAbs = max(abs(leftMix), abs(rightMix))
        if (maxAbs > 1f) {
            leftMix /= maxAbs
            rightMix /= maxAbs
        }

        val effectiveMax = (maxPwm * speedScale.coerceIn(0.1f, 1f)).roundToInt()
        var leftPwm = (leftMix * effectiveMax).roundToInt().coerceIn(-maxPwm, maxPwm)
        var rightPwm = (rightMix * effectiveMax).roundToInt().coerceIn(-maxPwm, maxPwm)

        if (invertLeft) leftPwm = -leftPwm
        if (invertRight) rightPwm = -rightPwm

        val directionLabel = resolveDirectionLabel(forward, turn)

        return MotorOutput(
            leftSpeed = leftPwm,
            rightSpeed = rightPwm,
            normalizedX = clampedX,
            normalizedY = clampedY,
            magnitude = effectiveMag,
            directionLabel = directionLabel
        )
    }

    fun formatCommand(
        leftSpeed: Int,
        rightSpeed: Int,
        format: MotorCommandFormat
    ): String = when (format) {
        MotorCommandFormat.LABELED -> "L:$leftSpeed,R:$rightSpeed"
        MotorCommandFormat.CSV -> "$leftSpeed,$rightSpeed"
        MotorCommandFormat.MOTOR_TAG -> "M1:$leftSpeed;M2:$rightSpeed"
        MotorCommandFormat.JSON -> "{\"L\":$leftSpeed,\"R\":$rightSpeed}"
    }

    private fun resolveDirectionLabel(forward: Float, turn: Float): String {
        val absF = abs(forward)
        val absT = abs(turn)
        return when {
            absF < 0.15f && absT < 0.15f -> "DETENIDO"
            absF >= absT * 1.8f -> if (forward > 0f) "AVANCE" else "REVERSA"
            absT >= absF * 1.8f -> if (turn > 0f) "GIRO DER" else "GIRO IZQ"
            forward > 0f && turn > 0f -> "AVANCE DER"
            forward > 0f && turn < 0f -> "AVANCE IZQ"
            forward < 0f && turn > 0f -> "REVERSA DER"
            else -> "REVERSA IZQ"
        }
    }
}
