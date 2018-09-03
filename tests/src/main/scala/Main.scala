import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject
import tcl.{TclClientData, TclInterp, TclStatus}

import scala.scalanative.scriptbridge.Export
import scalanative.native._

object Main {
  def main(args: Array[String]): Unit = {
    val interp = TclInterp(Foo)
    interp.exec("namespace eval Foo { namespace export bar }; namespace import Foo::*; bar 42")
  }

}

@Export
@debug
object Foo extends TclBridgeObject {

  def bar(i: Int) = println("YEAH:"+i)
}
