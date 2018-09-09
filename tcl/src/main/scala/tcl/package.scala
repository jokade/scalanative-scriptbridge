import tcl.scriptbridge.TclBridgeInstance

import scala.scalanative.native._

package object tcl {

  type TclObjStruct = CStruct5[Int,CString,Int,Ptr[TclObjType],CStruct2[Ptr[Byte],Int]]
  type TclObj = Ptr[Byte]

  type TclStatus = Int

  object TclStatus {
    val OK: TclStatus = 0
    val ERROR: TclStatus = 1
    val RETURN: TclStatus = 2
    val BREAK: TclStatus = 3
    val CONTINUE: TclStatus = 4
  }

  type TclObjCmdProc = CFunctionPtr4[TclClientData,Ptr[Byte],Int,Ptr[Ptr[Byte]],Int]

  type TclCmdDeleteProc = CFunctionPtr1[TclClientData,Unit]

  type TclCommand = Ptr[Byte]

  type TclClientData = Ptr[Byte]

  type TclFreeInternalRepProc = CFunctionPtr1[TclObj,Unit]

  type TclDupInternalRepProc = CFunctionPtr2[TclObj,TclObj,Unit]

  type TclUpdateStringProc = CFunctionPtr1[TclObj,Unit]

  type TclSetFromAnyProc = CFunctionPtr2[Ptr[Byte],TclObj,Int]

  type TclObjType = CStruct5[CString,TclFreeInternalRepProc,TclDupInternalRepProc,TclUpdateStringProc,TclSetFromAnyProc]

  def newObj(objType: Ptr[TclObjType], stringRep: CString, internalRep: Ptr[Byte], internalType: Int): TclObj = {
    val obj = api.Tcl_NewObj().cast[Ptr[TclObjStruct]]
    //!obj._2 = c"c"//stringRep
    //!obj._3 = 2 //string.strlen(stringRep).toInt
    !obj._4 = objType
    !obj._5._1 = internalRep
    !obj._5._2 = internalType
    obj.cast[TclObj]
  }

  /**
   * Creates a new TclObj wrapping the specified Scala instance.
   *
   * @param scalaObj Scala object to be wrapped
   * @return
   */
  def newObj(scalaObj: TclBridgeInstance): TclObj = {
    newObj(bridgeObjectType,c"<snobj>",scalaObj.cast[Ptr[Byte]],scalaObj.__tclTypeId)
  }

  private def getBridgeObject(obj: TclObj): Ptr[TclObjStruct] = {
    val p = obj.cast[Ptr[TclObjStruct]]
    if((!p._4) != bridgeObjectType)
      throw new TclException("TclObj is not a Scala object ")
    p
  }

  /**
   * Returns the pointer to the scala object represented by the specified TclObj.
   *
   * @param obj TclObj
   * @throws TclException if the specified TclObj does not represent a Scala object
   */
  def getScalaRep(obj: TclObj): Ptr[Byte] = !getBridgeObject(obj)._5._1

  def getScalaTypeId(obj: TclObj): Int = !getBridgeObject(obj)._5._2

  @inline def newObj(): TclObj = api.Tcl_NewObj()

  @inline def newIntObj(i: Int): TclObj = api.Tcl_NewIntObj(i)

  @inline def newLongObj(i: Long): TclObj = api.Tcl_NewLongObj(i)

  @inline def newBooleanObj(b: Boolean): TclObj = if(b) api.Tcl_NewBooleanObj(1) else api.Tcl_NewBooleanObj(0)

  @inline def newDoubleObj(d: Double): TclObj = api.Tcl_NewDoubleObj(d)

  // TODO: replace CUnsignedLongLong with Ptr[Byte] when scala-native/scala-native#1347 is resolved
  def newStringObj(s: String): TclObj = {
    var obj: CUnsignedLongLong = 0.toULong
    Zone{ implicit z =>
      val cstr = toCString(s)
      val len = string.strlen(cstr).toInt
      obj = api.Tcl_NewStringObj(cstr,len)
    }
    obj.cast[Ptr[Byte]]
  }

  def getString(objPtr: TclObj): String = fromCString(api.Tcl_GetString(objPtr))

  /* Representation of bridge objects */
  private def setFromAny(interpPtr: Ptr[Byte], obj: TclObj): TclStatus = {
    println("setFromAny")
    TclStatus.ERROR
  }

  private def updStringRep(obj: TclObj): Unit = {
    println("updStringRep")
  }

  private def dupInternalRep(src: TclObj, dest: TclObj): Unit = {
    println("dupInternalRep")
  }

  private def freeInternalRep(obj: TclObj): Unit = {
    println("freeInternalRep")
  }

  protected[tcl] lazy val bridgeObjectType: Ptr[TclObjType] = {
    val p = stdlib.malloc(sizeof[TclObjType]).cast[Ptr[TclObjType]]
    !p._1 = c"snobj"
    !p._2 = CFunctionPtr.fromFunction1(freeInternalRep)
    !p._3 = CFunctionPtr.fromFunction2(dupInternalRep)
    !p._4 = CFunctionPtr.fromFunction1(updStringRep)
    !p._5 = CFunctionPtr.fromFunction2(setFromAny)
    p
  }

  @extern
  object api {
    def Tcl_NewObj(): TclObj = extern
    def Tcl_NewIntObj(i: Int): TclObj = extern
    def Tcl_NewLongObj(i: Long): TclObj = extern
    def Tcl_NewBooleanObj(b: Int): TclObj = extern
    def Tcl_NewDoubleObj(d: Double): TclObj = extern
    // TODO: replace CUnsignedLongLong with Ptr[Byte] when scala-native/scala-native#1347 is resolved
    def Tcl_NewStringObj(bytes: CString, length: Int): CUnsignedLongLong = extern
    def Tcl_GetString(objPtr: TclObj): CString = extern
  }
}
