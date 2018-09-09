//     Project: scalanative-scriptbridge
//      Module:
// Description:
package tcl.scriptbridge

import tcl.TclInterp

trait TclBridgeInstance {
  def __tclTypeId: Int
}

trait TclBridgeObject {
  val __tcl: TclBridgeWrapper
}

trait TclBridgeWrapper {
  def __register(interp: TclInterp): Unit
  def __tcloo: String
}
