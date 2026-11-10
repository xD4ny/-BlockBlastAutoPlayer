package com.example.blockblastauto

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

/** Heuristic detector for the standard portrait layout; rejects uncertain frames. */
object Vision {
 data class Frame(val board:Long,val pieces:List<List<Solver.Cell>>,val gridX:Float,val gridY:Float,val step:Float,val trays:List<Pair<Float,Float>>)
 private fun distance(a:Int,b:Int):Int = abs(Color.red(a)-Color.red(b))+abs(Color.green(a)-Color.green(b))+abs(Color.blue(a)-Color.blue(b))
 fun detect(bmp:Bitmap):Frame? {
  val w=bmp.width.toFloat();val h=bmp.height.toFloat()
  if(w<=0 || h<=0 || h/w !in 1.6f..2.7f) return null
  // Initial layout calibration based on the supplied portrait screenshot.
  val step=w*0.111f;val left=w*0.056f;val top=h*0.265f
  if(top+step*8>=h*.79f) return null
  val colors=Array(8){r->IntArray(8){c->bmp.getPixel((left+(c+.5f)*step).toInt().coerceIn(0,bmp.width-1),(top+(r+.5f)*step).toInt().coerceIn(0,bmp.height-1))}}
  val counts=mutableMapOf<Int,Int>()
  for(row in colors) for(v in row) {
   val key=(Color.red(v)/24 shl 16) or (Color.green(v)/24 shl 8) or (Color.blue(v)/24)
   counts[key]=(counts[key]?:0)+1
  }
  val baseKey=counts.maxByOrNull { it.value }?.key?:return null
  val bg=Color.rgb(((baseKey shr 16)*24).coerceAtMost(255),(((baseKey shr 8) and 255)*24).coerceAtMost(255),((baseKey and 255)*24).coerceAtMost(255))
  var board=0L
  for(r in 0..7) for(c in 0..7) if(distance(colors[r][c],bg)>105) board=board or (1L shl (r*8+c))
  // Detect pieces by sampling a small grid in each of three tray regions.
  val pieces=mutableListOf<List<Solver.Cell>>()
  val trays=(0..2).map { Pair(w*(.19f+it*.31f),h*.825f) }
  for((cx,cy) in trays) {
   val unit=step*.83f
   val samples=mutableListOf<Solver.Cell>()
   val center=bmp.getPixel(cx.toInt().coerceIn(0,bmp.width-1),cy.toInt().coerceIn(0,bmp.height-1))
   val background=bmp.getPixel(cx.toInt().coerceIn(0,bmp.width-1),(cy+unit*2.4f).toInt().coerceIn(0,bmp.height-1))
   if(distance(center,background)<75) return null
   for(r in -2..2) for(c in -2..2) {
    val x=(cx+c*unit).toInt();val y=(cy+r*unit).toInt()
    if(x !in 0 until bmp.width || y !in 0 until bmp.height) continue
    val pixel=bmp.getPixel(x,y)
    if(distance(pixel,center)<80 && distance(pixel,background)>75) samples.add(Solver.Cell(r,c))
   }
   if(samples.isEmpty() || samples.size>9) return null
   val minR=samples.minOf{it.r};val minC=samples.minOf{it.c}
   pieces.add(samples.map{Solver.Cell(it.r-minR,it.c-minC)})
  }
  return Frame(board,pieces,left,top,step,trays)
 }
}
