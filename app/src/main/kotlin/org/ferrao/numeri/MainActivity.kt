package org.ferrao.numeri

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringArrayResource
import org.ferrao.numeri.ui.GameScreen
import org.ferrao.numeri.ui.HelpScreen
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private val game: GameViewModel by viewModels()

    private lateinit var soundPool: SoundPool
    private var eraseSound = 0
    private var successSound = 0

    private lateinit var sensorManager: SensorManager
    private var facingDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        eraseSound = soundPool.load(this, R.raw.erase, 1)
        successSound = soundPool.load(this, R.raw.success, 1)
        sensorManager = getSystemService(SensorManager::class.java)

        setContent {
            LaunchedEffect(Unit) {
                game.sounds.collect {
                    soundPool.play(if (it == Sound.ERASE) eraseSound else successSound, 1f, 1f, 1, 0, 1f)
                }
            }
            BackHandler(enabled = game.showHelp) { game.closeHelp() }

            // A ajuda entra por cima e sai por baixo, como no original.
            AnimatedContent(
                targetState = game.showHelp,
                transitionSpec = {
                    if (targetState) {
                        slideInVertically(tween(450)) { -it } togetherWith slideOutVertically(tween(450)) { it }
                    } else {
                        slideInVertically(tween(450)) { it } togetherWith slideOutVertically(tween(450)) { -it }
                    }
                },
                label = "help",
            ) { help ->
                if (help) {
                    HelpScreen(stringArrayResource(R.array.help_text).toList(), onPlay = game::closeHelp)
                } else {
                    GameScreen(
                        board = game.board,
                        revision = game.revision,
                        showHidden = game.showHidden,
                        celebrating = game.celebrating,
                        solvedCount = game.solvedCount,
                        busy = game.animating,
                        waitingForNext = game.waitingForNext,
                        onCellDown = game::onCellDown,
                        onCellEnter = game::onCellEnter,
                        onRelease = game::onRelease,
                        onPrev = game::onPrev,
                        onNext = game::onNext,
                        onHelp = game::openHelp,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }

    /** Limiares do original (em g); no Android o eixo z aponta para fora do ecrã, por isso o sinal troca. */
    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values.map { it / SensorManager.GRAVITY_EARTH }
        val length = sqrt(x * x + y * y + z * z)
        if (length !in 0.8f..1.4f) return
        if (!facingDown && z < -0.8f) facingDown = true
        else if (facingDown && z > -0.7f) facingDown = false
        else return
        game.setFaceDown(facingDown)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}
