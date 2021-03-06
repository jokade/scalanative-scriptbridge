package tcl

import java.io.File

import de.surfice.smacrotools.debug
import tcl.scriptbridge.{TclBridgeInstance, TclBridgeObject}

import scalanative.native._
import cobj._

@CObj(prefix = "Tcl_", newSuffix = "CreateInterp", namingConvention = NamingConvention.PascalCase)
//@debug
class TclInterp {
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

  def getPtr(obj: TclObj): Ptr[Byte] = getLong(obj).cast[Ptr[Byte]]

  def createObjCommand(cmdName: CString, proc: TclObjCmdProc, clientData: Ptr[Byte], deleteProc: TclCmdDeleteProc): TclCommand = extern

  def exec(script: CString): Unit = handleResult( eval(script) )
  def exec(script: String): Unit = Zone { implicit z =>
    handleResult( eval(toCString(script)) )
  }
  def execFile(fileName: String): Unit = Zone{ implicit z =>
    handleResult( evalFile(toCString(fileName)) )
  }

  def registerBridgeObjects(objs: Iterable[TclBridgeObject]): Unit =
    objs.foreach(_.__tcl.__register(this))

  def registerObjType(typePtr: Ptr[TclObjType]): Unit = extern

  def registerBridgeObjType(): Unit = registerObjType(tcl.bridgeObjectType)

  private def handleResult(status: TclStatus) = status match {
    case TclStatus.ERROR =>
      throw new TclException( fromCString(getStringResult()) )
    case _ =>
  }
}

object TclInterp {

  def initBridge(interp: TclInterp, bridgeObjects: Iterable[TclBridgeObject], useTclOO: Boolean): TclInterp = {
    interp.registerBridgeObjType()
    interp.registerBridgeObjects(bridgeObjects)
    interp.init()
    if(useTclOO) {
      interp.exec(
        """package require TclOO
          |oo::class create ScalaBridgeObject {
          |  variable Ref
          |  method __ref {} { return $Ref }
          |}
          |proc ::__arg {arg} {
          |  if {[info object isa object $arg] && [info object class $arg ::ScalaBridgeObject]} {
          |    return [$arg __ref]
          |  } else {
          |    return $arg
          |  }
          |}
        """.stripMargin)
      bridgeObjects.foreach(o => interp.exec(o.__tcl.__tcloo))
    }
    interp
  }

  @name("Tcl_CreateInterp")
  def apply(): TclInterp = extern

  def apply(bridgeObjects: Iterable[TclBridgeObject], useTclOO: Boolean = false): TclInterp = initBridge(TclInterp(),bridgeObjects,useTclOO)

}