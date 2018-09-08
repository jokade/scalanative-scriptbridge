package tcl

import java.io.File

import de.surfice.smacrotools.debug
import tcl.scriptbridge.TclBridgeObject

import scalanative.native._
import cobj._

@CObj(prefix = "Tcl_", newSuffix = "CreateInterp", namingConvention = CObj.NamingConvention.PascalCase)
@debug
final class TclInterp {
  def init(): TclStatus = extern

  def eval(script: CString): TclStatus = extern
  def evalFile(fileName: CString): TclStatus = extern

  def setResult(result: CString, freeProc: Ptr[Byte]): TclStatus = extern
  def setObjResult(obj: TclObj): Unit = extern
  def getStringResult(): CString = extern


  def getIntFromObj(objPtr: TclObj, intPtr: Ptr[Int]): TclStatus = extern
  def getInt(obj: TclObj): Int = {
    val i = stackalloc[Int]
    if( getIntFromObj(obj,i) == TclStatus.ERROR )
      throw new TclException("could not parse to int value: "+tcl.getString(obj))
    else
      !i
  }

  def getLongFromObj(objPtr: TclObj, intPtr: Ptr[Long]): TclStatus = extern
  def getLong(obj: TclObj): Long = {
    val i = stackalloc[Long]
    if( getLongFromObj(obj,i) == TclStatus.ERROR )
      throw new TclException("could not parse to long value: "+tcl.getString(obj))
    else
      !i
  }

  def getBooleanFromObj(objPtr: TclObj, boolPtr: Ptr[Int]): TclStatus = extern
  def getBoolean(obj: TclObj): Boolean = {
    val b = stackalloc[Int]
    if( getBooleanFromObj(obj,b) == TclStatus.ERROR )
      throw new TclException("could not parse to bool value: "+tcl.getString(obj))
    else
      !b > 0
  }

  def getDoubleFromObj(objPtr: TclObj, doublePtr: Ptr[Double]): TclStatus = extern
  def getDouble(obj: TclObj): Double = {
    val d = stackalloc[Double]
    if( getDoubleFromObj(obj,d) == TclStatus.ERROR )
      throw new TclException("could not parse to double value: "+tcl.getString(obj))
    else
      !d
  }

  def getFloat(obj: TclObj): Float = getDouble(obj).toFloat

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