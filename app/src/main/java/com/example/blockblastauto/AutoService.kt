package com.example.blockblastauto

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AutoService : AccessibilityService() {
 private var panel: LinearLayout? = null
 private lateinit var wm: WindowManager
 override fun onServiceConnected() {
  super.onServiceConnected()
  wm = getSystemService(WINDOW_SERVICE) as WindowManager
  val layout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,8,8,8); setBackgroundColor(0xDD20252B.toInt()) }
  layout.addView(TextView(this).apply { text = "BB · Anteprima"; setTextColor(-1); setPadding(6,12,8,6) })
  layout.addView(Button(this).apply { text = "✕"; setOnClickListener { closeOverlay() } })
  val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x=12; y=100 }
  try { wm.addView(layout,params); panel=layout } catch (_: Exception) {}
 }
 private fun closeOverlay() { panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }; panel=null }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
 override fun onInterrupt() {}
 override fun onDestroy() { closeOverlay(); super.onDestroy() }
}