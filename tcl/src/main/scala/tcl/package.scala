import scala.scalanative.native.{CFunctionPtr1, CFunctionPtr4, CObj, Ptr}

package object tcl {

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
}
