package com.example.blockblastauto

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class AutoService : AccessibilityService() {
 private var panel:LinearLayout?=null
 private lateinit var wm:WindowManager
 private val executor=Executors.newSingleThreadExecutor()
 private var status:TextView?=null
 private var closed=false
 override fun onServiceConnected() {
  super.onServiceConnected()
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  closed=false
  val layout=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(8,8,8,8);setBackgroundColor(0xE6222833.toInt())}
  status=TextView(this).apply {text="BB · Pronto per analisi";setTextColor(-1);setPadding(8,6,8,6)}
  layout.addView(status)
  val actions=LinearLayout(this)
  actions.addView(Button(this).apply {text="Analizza";setOnClickListener {analyze()}})
  actions.addView(Button(this).apply {text="✕";setOnClickListener {closeOverlay()}})
  layout.addView(actions)
  val params=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply {gravity=Gravity.TOP or Gravity.END;x=12;y=100}
  try {wm.addView(layout,params);panel=layout} catch (_:Exception) {}
 }
 private fun analyze() {
  if(Build.VERSION.SDK_INT<30) {status?.text="Richiede Android 11+";return}
  status?.text="Lettura schermata…"
  takeScreenshot(0,executor,object:TakeScreenshotCallback {
   override fun onSuccess(result:ScreenshotResult) {
    val hardware=Bitmap.wrapHardwareBuffer(result.hardwareBuffer,result.colorSpace)
    val bitmap=hardware?.copy(Bitmap.Config.ARGB_8888,false)
    result.hardwareBuffer.close()
    val frame=bitmap?.let {Vision.detect(it)}
    val solution=frame?.let {Solver.solve(it.board,it.pieces)}
    bitmap?.recycle()
    runOnUiThread {
     if(!closed) status?.text=when {
      frame==null->"Riconoscimento incerto: nessun tocco"
      solution==null->"Nessuna sequenza valida"
      else->"Analisi: "+solution.moves.joinToString { "P${it.piece+1} (${it.row},${it.col})" }
     }
    }
   }
   override fun onFailure(errorCode:Int) {runOnUiThread {if(!closed)status?.text="Screenshot non disponibile ($errorCode)"}}
  })
 }
 private fun closeOverlay() {
  closed=true
  panel?.let {try {wm.removeView(it)}catch (_:Exception){}}
  panel=null;status=null
 }
 override fun onAccessibilityEvent(event:AccessibilityEvent?) {}
 override fun onInterrupt() {}
 override fun onDestroy() {closeOverlay();executor.shutdownNow();super.onDestroy()}
}
