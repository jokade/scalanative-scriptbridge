package tcl

import java.io.File

import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject

import scalanative.native._

@CObj(prefix = "Tcl_", newSuffix = "CreateInterp", namingConvention = CObj.NamingConvention.PascalCase)
@debug
final class TclInterp {
  def init(): TclStatus = extern

  def eval(script: CString): TclStatus = extern
  def evalFile(fileName: CString): TclStatus = extern

  def getStringResult(): CString = extern
  def getIntFromObj(objPtr: Ptr[Byte], intPtr: Ptr[Int]): TclStatus = extern

  def createObjCommand(cmdName: CString, proc: TclObjCmdProc, clientData: Ptr[Byte], deleteProc: TclCmdDeleteProc): TclCommand = extern

  def exec(script: CString): Unit = handleResult( eval(script) )
  def exec(script: String): Unit = Zone { implicit z =>
    handleResult( eval(toCString(script)) )
  }
  def execFile(fileName: String): Unit = Zone{ implicit z =>
    handleResult( evalFile(toCString(fileName)) )
  }

  private def handleResult(status: TclStatus) = status match {
    case TclStatus.ERROR =>
      throw new TclException( fromCString(getStringResult()) )
    case _ =>
  }
}

object TclInterp {

  def apply(bridgeObjects: TclBridgeObject*): TclInterp = {
    val interp = new TclInterp
    interp.init()
    bridgeObjects.foreach(_.__tcl.__register(interp))
    interp
  }
}