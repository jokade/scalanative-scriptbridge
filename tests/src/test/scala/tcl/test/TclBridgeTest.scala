package tcl.test

import de.surfice.smacrotools.debug
import tcl.TclInterp
import tcl.scriptbridge.TclBridgeObject
import utest._

import scala.scalanative.Export

object TclBridgeTest extends TestSuite {
  val tests = Tests {
    val interp = TclInterp(EUT)
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
    }
  }

}

@Export
@debug
object EUT extends TclBridgeObject {
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
}
