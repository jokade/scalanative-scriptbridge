package tk

import de.surfice.smacrotools.debug
import tcl.TclInterp
import tcl.scriptbridge.TclBridgeObject

import scalanative.native._
import cobj._

@CObj(prefix="Tk_",namingConvention=NamingConvention.PascalCase)
@debug
class TkInterp extends TclInterp {
  /**
   * Executes the specified script file and then calls the Tk main loop.
   *
   * @note blocks until no more Tk windows exist.
   * @param fileName
   */
  def runFile(fileName: String): Unit = {
    execFile(fileName)
    TkInterp.mainLoop()
  }
}

object TkInterp {
  def apply(bridgeObjects: Iterable[TclBridgeObject], useTclOO: Boolean = false): TkInterp = {
    val interp = TclInterp.initBridge(TclInterp(),bridgeObjects,useTclOO)
    new TkInterp(interp.__ptr)
  }

  def mainLoop(): Unit = extern
}
