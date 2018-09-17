package tcl.test

import de.surfice.smacrotools.debug
import tcl.TclInterp
import tcl.scriptbridge.TclBridgeObject
import utest._

import scalanative.native._
import scriptbridge._

object TclBridgeTest extends TestSuite {
  val tests = Tests {
    val interp = TclInterp(Seq(EUT),useTclOO = true)
    interp.exec("set eut [tcl::test::EUT::new 1]")

    'functions-{
      'noArgs_noResult-{
        EUT.res = 0
        interp.exec("tcl::test::EUT::noArgsNoResult")
        EUT.res ==> 1
      }
      'intArgs_noResult-{
        EUT.res = 0
        interp.exec("tcl::test::EUT::intArgsNoResult 1 2")
        EUT.res ==> 3
      }
      'longArgs_longResult-{
        interp.exec("if {[tcl::test::EUT::longArgsLongResult 1234567890123 1000000000000] != 2234567890123} { error {expected 2234567890123}}")
      }
      'noArgs_intResult-{
        EUT.res = 42
        interp.exec("if {[tcl::test::EUT::noArgsIntResult] != 42} error")
      }
      'boolArg_boolResult-{
        interp.exec("if {[tcl::test::EUT::boolArgBoolResult true]} { error {expected false} }")
        interp.exec("if {![tcl::test::EUT::boolArgBoolResult false]} { error {expected true} }")
      }
      'doubleArgs_doubleResult-{
        interp.exec("if {[tcl::test::EUT::doubleArgsDoubleResult 1234.5678 -1234.5678] != 0.0} { error {expected 0} }")
      }
      'floatArgs_floatResult-{
        interp.exec("if {[tcl::test::EUT::floatArgsFloatResult 1234.5678 -1234.5678] != 0.0} { error {expected 0} }")
      }
      'stringArg_stringResult-{
        interp.exec("if {[tcl::test::EUT::stringArgStringResult {hello world}] != {hello world}} { error {expected {hello world}} }")
      }
      'doublePtr-{
        interp.exec("if {[tcl::test::EUT::doubleFromPtr [tcl::test::EUT::doublePtr]] != 1234.5678} { error {expected 1234.5678} }")
      }
    }

    'methods-{
      'add-{
        interp.exec("if {[tcl::test::EUT::add $eut 123 321] != 445} { error {expected 445} }")
      }
    }

    'TclOO-{
      interp.exec(
        """
          |namespace import tcl::test::EUT
          |
          |EUT create eut 1
          |
          |if {[eut add 123 321] != 445} { error {expected 445} }
          |
          |if {[EUT boolArgBoolResult true]} { error {expected false} }
        """.stripMargin)
    }
  }

}

@Export
@debug
class EUT(incr: Int) {
  def add(a: Int, b: Int): Int = a + b + incr
}

object EUT extends TclBridgeObject {
  private val _dPtr = stdlib.malloc(sizeof[Double]).cast[Ptr[Double]]
  !_dPtr = 1234.5678

  var res = 0

  def noArgsNoResult(): Unit = {
    res = 1
  }

  def intArgsNoResult(a: Int, b: Int): Unit = {
    res = a+b
  }

  def noArgsIntResult(): Int = res

  def boolArgBoolResult(f: Boolean): Boolean = !f

  def doubleArgsDoubleResult(a: Double, b: Double): Double = a + b

  def floatArgsFloatResult(a: Float, b: Float): Float = a + b

  def stringArgStringResult(s: String): String = s

  def longArgsLongResult(a: Long, b: Long): Long = a + b

  def doublePtr(): Ptr[Double] = _dPtr

  def doubleFromPtr(ptr: Ptr[Double]): Double = !ptr

}
