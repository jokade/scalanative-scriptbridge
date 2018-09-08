package python

import scala.scalanative.native._

object Python {
//  private var _programName: CString = _

  @inline def initialize(): Unit = api.Py_Initialize()
  @inline def isInitialized: Boolean = api.Py_IsInitialized() != 0
  @inline def finalizeInterp(): Unit = api.Py_Finalize()
  @inline def version: String = fromCString(api.Py_GetVersion())

  //def setProgramName(name: String): Unit = Zone{ implicit z =>
  //  val cstr = toCString(name)
  //  if(_programName != null)
  //    stdlib.free(_programName)
  //  _programName = stdlib.malloc(string.strlen(cstr)+1)
  //  string.strcpy(_programName,cstr)
  //  api.Py_SetProgramName(_programName)
  //}
  def setProgramName(name: CString): Unit = api.Py_SetProgramName(name)

  def runSimpleString(command: String): Unit = Zone{ implicit z =>
    val cstr = toCString(command)
    if(api.PyRun_SimpleString(cstr) != 0)
      throw new PythonException("error while executing command")
  }

  @extern
  object api {
    def Py_Initialize(): Unit = extern
    def Py_IsInitialized(): Int = extern
    def Py_Finalize(): Unit = extern
    def Py_SetProgramName(name: CString): Unit = extern
    def Py_GetVersion(): CString = extern

    def PyRun_SimpleString(command: CString): Int = extern
  }
}
