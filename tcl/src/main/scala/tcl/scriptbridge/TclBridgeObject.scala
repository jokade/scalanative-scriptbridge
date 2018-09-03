//     Project: scalanative-scriptbridge
//      Module:
// Description:
package tcl.scriptbridge

import tcl.TclInterp

trait TclBridgeObject {
  val __tcl: TclBridgeWrapper
}

trait TclBridgeWrapper {
  def __register(interp: TclInterp): Unit
}
