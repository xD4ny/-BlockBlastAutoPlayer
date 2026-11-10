package com.example.blockblastauto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
  root.addView(TextView(this).apply { text = "Block Blast Auto Player — anteprima overlay"; textSize = 22f })
  root.addView(TextView(this).apply { text = "Attiva il servizio di accessibilità per mostrare il pannello flottante. Questa versione non gioca ancora automaticamente."; textSize = 16f })
  root.addView(Button(this).apply { text = "Attiva overlay"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
  setContentView(root)
 }
}