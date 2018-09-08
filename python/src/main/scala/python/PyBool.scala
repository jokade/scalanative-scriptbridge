package python

import scalanative.native._
import cobj._

@CObj(namingConvention = CObj.NamingConvention.PascalCase, prefix = "PyBool_", newSuffix = "FromLong")
class PyBool(f: Long) extends PyObject {

}

object PyBool {
  lazy val True: PyBool = new PyBool(1)
  lazy val False: PyBool = new PyBool(0)
}
