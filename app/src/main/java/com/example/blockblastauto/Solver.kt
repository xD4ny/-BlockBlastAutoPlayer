package com.example.blockblastauto

/** Search all legal orders and placements of the three visible pieces. */
object Solver {
 data class Cell(val r:Int,val c:Int)
 data class Move(val piece:Int,val row:Int,val col:Int)
 data class Result(val moves:List<Move>,val score:Double)
 fun solve(initial:Long, pieces:List<List<Cell>>):Result? {
  if(pieces.size!=3 || pieces.any { it.isEmpty() }) return null
  var best:Result?=null
  fun dfs(board:Long, remaining:List<Int>, path:List<Move>, cleared:Int) {
   if(remaining.isEmpty()) {
    val empty=64-java.lang.Long.bitCount(board)
    val holes=(0..7).sumOf { r -> (0..7).count { c -> board and (1L shl (r*8+c))==0L && (0..r).any { above -> board and (1L shl (above*8+c))!=0L } } }
    val score=cleared*120.0+empty*2.0-holes*1.8
    if(best==null || score>best!!.score) best=Result(path,score)
    return
   }
   for(i in remaining) {
    val shape=pieces[i]
    for(r in 0..7) for(c in 0..7) {
     var mask=0L; var valid=true
     for(p in shape) {
      val rr=r+p.r; val cc=c+p.c
      if(rr !in 0..7 || cc !in 0..7) {valid=false;break}
      mask=mask or (1L shl (rr*8+cc))
     }
     if(!valid || board and mask!=0L) continue
     var next=board or mask; var clear=0L; var lines=0
     for(rr in 0..7) {
      val rowMask=255L shl (rr*8)
      if(next and rowMask==rowMask) {clear=clear or rowMask;lines++}
     }
     for(cc in 0..7) {
      var colMask=0L
      for(rr in 0..7) colMask=colMask or (1L shl (rr*8+cc))
      if(next and colMask==colMask) {clear=clear or colMask;lines++}
     }
     next=next and clear.inv()
     dfs(next,remaining.filter { it!=i },path+Move(i,r,c),cleared+lines)
    }
   }
  }
  dfs(initial,pieces.indices.toList(),emptyList(),0)
  return best
 }
}
