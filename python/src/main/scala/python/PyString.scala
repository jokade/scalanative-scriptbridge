//     Project: scalanative-scriptbridge
//      Module:
// Description:
package python

import de.surfice.smacrotools.debug

import scalanative.native._
import cobj._

@CObj(namingConvention = CObj.NamingConvention.PascalCase,prefix = "PyString_", newSuffix="fromString")
class PyString(s: CString) extends PyObject {
  def size: CSize = extern
  @name("PyString_AsString")
  def asCString: CString = extern
}

