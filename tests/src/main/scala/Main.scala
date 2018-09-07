import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject
import tcl.{TclClientData, TclInterp, TclStatus}

import scala.scalanative.Export
import scalanative.native._

object Main {

  def main(args: Array[String]): Unit = {
//    val interp = TclInterp(Foo)
    //tcl.newStringObj("foo")
//    interp.exec("namespace eval Foo { namespace export bar }; namespace import Foo::*; puts [bar 42]")
 //   println(interp)
    tcl.newStringObj("foo")
  }

}

@Export
@debug
object Foo extends TclBridgeObject {
  def bar(i: Int): Int = i+1
}

