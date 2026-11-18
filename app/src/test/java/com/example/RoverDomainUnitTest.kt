package com.example

import com.example.domain.DifferentialDriveCalculator
import com.example.domain.Esp32TelemetryParser
import com.example.model.Esp32TelemetryState
import com.example.model.MotorCommandFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoverDomainUnitTest {

    @Test
    fun joystickUpMovesBothMotorsForward() {
        val out = DifferentialDriveCalculator.calculateMotors(joystickX = 0f, joystickY = 1f, maxPwm = 255)
        assertEquals(255, out.leftSpeed)
        assertEquals(255, out.rightSpeed)
        assertEquals("AVANCE", out.directionLabel)
    }

    @Test
    fun joystickDownMovesBothMotorsBackward() {
        val out = DifferentialDriveCalculator.calculateMotors(joystickX = 0f, joystickY = -1f, maxPwm = 255)
        assertEquals(-255, out.leftSpeed)
        assertEquals(-255, out.rightSpeed)
        assertEquals("REVERSA", out.directionLabel)
    }

    @Test
    fun joystickRightTurnsRight() {
        val out = DifferentialDriveCalculator.calculateMotors(joystickX = 1f, joystickY = 0f, maxPwm = 255)
        assertEquals(255, out.leftSpeed)
        assertEquals(-255, out.rightSpeed)
        assertEquals("GIRO DER", out.directionLabel)
    }

    @Test
    fun joystickLeftTurnsLeft() {
        val out = DifferentialDriveCalculator.calculateMotors(joystickX = -1f, joystickY = 0f, maxPwm = 255)
        assertEquals(-255, out.leftSpeed)
        assertEquals(255, out.rightSpeed)
        assertEquals("GIRO IZQ", out.directionLabel)
    }

    @Test
    fun joystickInsideDeadzoneReturnsZero() {
        val out = DifferentialDriveCalculator.calculateMotors(joystickX = 0.03f, joystickY = -0.04f, deadzone = 0.08f)
        assertEquals(0, out.leftSpeed)
        assertEquals(0, out.rightSpeed)
        assertEquals("DETENIDO", out.directionLabel)
    }

    @Test
    fun commandFormattingMatchesSelectedProtocol() {
        assertEquals("L:150,R:-90", DifferentialDriveCalculator.formatCommand(150, -90, MotorCommandFormat.LABELED))
        assertEquals("150,-90", DifferentialDriveCalculator.formatCommand(150, -90, MotorCommandFormat.CSV))
        assertEquals("M1:150;M2:-90", DifferentialDriveCalculator.formatCommand(150, -90, MotorCommandFormat.MOTOR_TAG))
        assertEquals("{\"L\":150,\"R\":-90}", DifferentialDriveCalculator.formatCommand(150, -90, MotorCommandFormat.JSON))
    }

    @Test
    fun esp32ParserParsesKeyValueTelemetry() {
        val initial = Esp32TelemetryState()
        val res = Esp32TelemetryParser.parseLine(
            "BAT:87,V:11.9,L:180,R:175,DIST:32.5,TEMP:33.4,RSSI:-59,OBS:0",
            initial
        )
        assertFalse(res.isDebugMessage)
        assertEquals(87, res.updatedState.batteryPercent)
        assertEquals(11.9f, res.updatedState.batteryVoltage)
        assertEquals(180, res.updatedState.reportedLeftSpeed)
        assertEquals(175, res.updatedState.reportedRightSpeed)
        assertEquals(32.5f, res.updatedState.distanceCm)
        assertEquals(33.4f, res.updatedState.temperatureC)
        assertEquals(-59, res.updatedState.rssiDbm)
        assertEquals(false, res.updatedState.obstacleDetected)
    }

    @Test
    fun esp32ParserCapturesDebugLines() {
        val res = Esp32TelemetryParser.parseLine("[DBG] Driver L298N inicializado", Esp32TelemetryState())
        assertTrue(res.isDebugMessage)
        assertEquals("[DBG] Driver L298N inicializado", res.debugText)
        assertEquals(1L, res.updatedState.packetsReceived)
    }
}
