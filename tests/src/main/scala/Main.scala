import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject
import tcl.{TclClientData, TclInterp, TclStatus}
import python._

import scalanative.native._
import scriptbridge._

object Main {

  def main(args: Array[String]): Unit = Zone{ implicit z =>
    val interp = TclInterp(Seq(Foo),useTclOO = true)

    interp.exec(
      """Foo create foo
        |foo foo hello
        |""".stripMargin)
//    Python.setProgramName(c"tests-out")
//    Python.initialize()
//    Python.runSimpleString("print 2")
//    Python.finalizeInterp()

  }

}

@Export
@debug
class Foo {
  var x = 42
  def foo(s: String): Unit = println(s)
  def call(foo: Foo): Foo = foo

}

//@Export
//@debug
object Foo extends TclBridgeObject {
  def bar(i: Int): Int = i+1
}

