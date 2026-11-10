package com.example.blockblastauto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class AutoService : AccessibilityService() {
 private var panel: LinearLayout? = null
 private lateinit var wm: WindowManager
 private val executor = Executors.newSingleThreadExecutor()
 private val handler = Handler(Looper.getMainLooper())
 private var status: TextView? = null
 private var toggle: Button? = null
 private var closed = false
 private var running = false
 private var busy = false
 private var failures = 0
 private var steps = 0
 private var pending: List<Solver.Move> = emptyList()
 private var frame: Vision.Frame? = null

 override fun onServiceConnected() {
  super.onServiceConnected()
  wm = getSystemService(WINDOW_SERVICE) as WindowManager
  closed = false
  val layout = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(8, 8, 8, 8)
   setBackgroundColor(0xE6222833.toInt())
  }
  status = TextView(this).apply {
   text = "BB · Apri Block Blast"
   setTextColor(-1)
   setPadding(8, 6, 8, 6)
  }
  layout.addView(status)
  val actions = LinearLayout(this)
  toggle = Button(this).apply {
   text = "Avvia"
   setOnClickListener {
    if (running) stop("In pausa") else {
     running = true
     failures = 0
     steps = 0
     pending = emptyList()
     text = "Ferma"
     status?.text = "Avvio riconoscimento…"
     analyze()
    }
   }
  }
  actions.addView(toggle)
  actions.addView(Button(this).apply {
   text = "✕"
   setOnClickListener { closeOverlay() }
  })
  layout.addView(actions)
  val params = WindowManager.LayoutParams(
   WindowManager.LayoutParams.WRAP_CONTENT,
   WindowManager.LayoutParams.WRAP_CONTENT,
   WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
   PixelFormat.TRANSLUCENT
  ).apply { gravity = Gravity.TOP or Gravity.END; x = 12; y = 100 }
  try { wm.addView(layout, params); panel = layout } catch (_: Exception) {}
 }

 private fun stop(message: String) {
  running = false
  pending = emptyList()
  toggle?.text = "Avvia"
  status?.text = message
 }

 private fun analyze() {
  if (!running || closed || busy) return
  if (Build.VERSION.SDK_INT < 30) { stop("Richiede Android 11+"); return }
  busy = true
  takeScreenshot(0, executor, object : TakeScreenshotCallback {
   override fun onSuccess(result: ScreenshotResult) {
    val hardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
    val bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
    result.hardwareBuffer.close()
    val detected = try { bitmap?.let { Vision.detect(it) } } catch (_: Exception) { null }
    bitmap?.recycle()
    handler.post {
     busy = false
     if (!running || closed) return@post
     if (detected == null) {
      failures++
      if (failures >= 3) stop("Lettura incerta: fermato, nessun trascinamento")
      else handler.postDelayed({ analyze() }, 900)
      return@post
     }
     failures = 0
     frame = detected
     val result = try { Solver.solve(detected.board, detected.pieces) } catch (_: Exception) { null }
     if (result == null || result.moves.isEmpty()) {
      stop("Nessuna mossa valida: fermato")
      return@post
     }
     pending = result.moves
     status?.text = "Mossa ${steps + 1} · ${pending.size} pezzi"
     executeNext()
    }
   }
   override fun onFailure(errorCode: Int) {
    handler.post {
     busy = false
     if (running && !closed) stop("Screenshot non disponibile ($errorCode)")
    }
   }
  })
 }

 private fun executeNext() {
  if (!running || closed) return
  val f = frame ?: run { stop("Dati non disponibili"); return }
  val move = pending.firstOrNull() ?: run { handler.postDelayed({ analyze() }, 800); return }
  val shape = f.pieces.getOrNull(move.piece) ?: run { stop("Pezzo mancante"); return }
  val tray = f.trays.getOrNull(move.piece) ?: run { stop("Posizione pezzo mancante"); return }
  // Drag the geometric center of the piece to the geometric center of its placement.
  val maxR = shape.maxOf { it.r }
  val maxC = shape.maxOf { it.c }
  val fromX = tray.first
  val fromY = tray.second
  val toX = f.gridX + (move.col + (maxC + 1f) / 2f) * f.step
  val toY = f.gridY + (move.row + (maxR + 1f) / 2f) * f.step
  val path = Path().apply {
   moveTo(fromX, fromY)
   lineTo(toX, toY)
  }
  val stroke = GestureDescription.StrokeDescription(path, 0, 550)
  val gesture = GestureDescription.Builder().addStroke(stroke).build()
  val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
   override fun onCompleted(gestureDescription: GestureDescription?) {
    handler.post {
     if (!running || closed) return@post
     steps++
     // Re-read after each move: game animations, clears and piece replacement
     // may change the board; never execute stale coordinates.
     pending = emptyList()
     handler.postDelayed({ analyze() }, 1050)
    }
   }
   override fun onCancelled(gestureDescription: GestureDescription?) {
    handler.post { if (running) stop("Gesto annullato: fermato") }
   }
  }, handler)
  if (!accepted) stop("Trascinamento non disponibile")
 }

 private fun closeOverlay() {
  stop("Chiuso")
  closed = true
  panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
  panel = null
  status = null
  toggle = null
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
 override fun onInterrupt() { stop("Servizio interrotto") }
 override fun onDestroy() {
  closeOverlay()
  executor.shutdownNow()
  super.onDestroy()
 }
}
