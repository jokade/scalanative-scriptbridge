package python

import de.surfice.smacrotools.debug

import scalanative.native._
import cobj._

@CObj(namingConvention = CObj.NamingConvention.PascalCase, prefix = "PyInt_", newSuffix = "FromLong")
@debug
class PyInt(i: Long) extends PyObject {
  def asLong: Long = extern
  def asInt: Int = asLong.toInt
}
