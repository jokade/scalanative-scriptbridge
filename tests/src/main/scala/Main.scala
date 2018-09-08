import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject
import tcl.{TclClientData, TclInterp, TclStatus}
import python._

import scalanative.native._
import scriptbridge._

object Main {

  def main(args: Array[String]): Unit = Zone{ implicit z =>
//    val interp = TclInterp(Foo)
    //tcl.newStringObj("foo")
//    interp.exec("namespace eval Foo { namespace export bar }; namespace import Foo::*; puts [bar 42]")
 //   println(interp)
//    tcl.newStringObj("foo")

    Python.setProgramName(c"tests-out")
    Python.initialize()
    Python.runSimpleString("print 2")
    Python.finalizeInterp()

  }

}

@extern
object Py {
  def PyInt_FromLong(l: CLong): Ptr[Byte] = extern
  def PyLong_FromLong(l: CLong): Ptr[Byte] = extern
  val Py_False: Ptr[Byte] = extern
}

