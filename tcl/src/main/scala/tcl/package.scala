import scala.scalanative.native._

package object tcl {

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

  @extern
  object api {
    def Tcl_NewIntObj(i: Int): TclObj = extern
    def Tcl_NewLongObj(i: Long): TclObj = extern
    def Tcl_NewBooleanObj(b: Int): TclObj = extern
    def Tcl_NewDoubleObj(d: Double): TclObj = extern
    // TODO: replace CUnsignedLongLong with Ptr[Byte] when scala-native/scala-native#1347 is resolved
    def Tcl_NewStringObj(bytes: CString, length: Int): CUnsignedLongLong = extern
    def Tcl_GetString(objPtr: TclObj): CString = extern
  }
}
