import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject
import tk.TkInterp

import scala.scalanative.native.scriptbridge.Export
import scalanative.native._

object Main {

  @Export
  @debug
  case class Calculator(var value: Double) {
    def calculate(): Double = value
  }

  object Calculator {
  }


  def main(args: Array[String]): Unit = {
    val interp = TkInterp(Seq(Calculator),true)
    interp.runFile("tcl/src/main/tcl/ui.tcl")
  }
}
