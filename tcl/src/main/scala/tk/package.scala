import tcl.{TclAppInitProc, TclInterp}

import scalanative.native._

package object tk {

  /*
  def init(interp: Ptr[Byte]): Int = 0

  def runTkMain(args: Array[String], interp: TclInterp): Int = {
    tcl.api.Tcl_FindExecutable(null)
    api.Tk_MainEx(0,null,CFunctionPtr.fromFunction1(init),interp.__ptr)
  }
*/
  @extern
  object api {
    def Tk_Init(interp: Ptr[Byte]): Int = extern
    def Tk_MainEx(argc: CInt, argv: Ptr[CString], appInitProc: TclAppInitProc, interp: Ptr[Byte]): CInt = extern
//    def Tk_MainLoop(): Unit = extern
  }
}
