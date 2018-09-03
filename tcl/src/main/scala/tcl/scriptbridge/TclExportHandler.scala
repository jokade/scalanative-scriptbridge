package tcl.scriptbridge

import de.surfice.smacrotools.{MacroAnnotationHandler, WhiteboxMacroTools}

import scala.reflect.macros.whitebox
import scala.scalanative.scriptbridge.ExportHandler

class TclExportHandler(val c: whitebox.Context) extends ExportHandler {
  import c.universe._

  type ArgTypes = Seq[(TclType.Value,Int)]

  implicit class MacroData(data: Map[String,Any]) {
    type Data = Map[String, Any]
    def exportedFunctions: Seq[DefDef] = data.getOrElse("tcl_exportedFunctions",Nil).asInstanceOf[Seq[DefDef]]
    def withExportedFunctions(exportedFunctions: Seq[DefDef]): Data = data.updated("tcl_exportedFunctions",exportedFunctions)
  }

  val intType = c.weakTypeOf[Int]

  object TclType extends Enumeration {
    val Int = Value
  }

  override def analyze: Analysis = {
    case (obj: ObjectParts, data: Data) =>
      val exportedFunctions = exportedMembers(obj.body)

      val updData = data.withExportedFunctions(exportedFunctions)
      (obj,updData)
    case default => default
  }

  override def transform: Transformation = {
    case obj: ObjectTransformData =>
      implicit val objectParts = obj.modParts
      val functionWrappers = obj.data.exportedFunctions map genFunctionWrapper
      val registerCommands = obj.data.exportedFunctions map genRegistration

      val tclObj =
        q"""
           object __tcl extends tcl.scriptbridge.TclBridgeWrapper {
             ..$functionWrappers

             def __register(interp: tcl.TclInterp)   : Unit = {
               import scalanative.native.CFunctionPtr
               ..$registerCommands
             }
           }
         """
      obj.updBody(obj.modParts.body :+ tclObj)
    case default => default
  }
  private def isPublic(m: DefDef): Boolean = ! (m.mods.hasFlag(Flag.PRIVATE) || m.mods.hasFlag(Flag.PROTECTED))

  private def exportedMembers(body: Seq[Tree]): Seq[DefDef] = body collect {
    case m: DefDef if isPublic(m) => m
  }

  private def genFunctionWrapper(f: DefDef)(implicit objectParts: ObjectParts): Tree = {
    val callee = TermName(objectParts.fullName)
    val argTypes = getArgTypes(f)
    val argDefs = genArgs(argTypes)
    val argList = genArgList(argTypes)
    q"""def ${f.name}(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = interpPtr.cast[tcl.TclInterp]
          ..$argDefs
          $callee.${f.name}(..$argList)
          tcl.TclStatus.OK
        }"""
  }

  private def getArgTypes(f: DefDef)(implicit commonParts: CommonParts): ArgTypes =
    (f.vparamss match {
      case List(args) => args
      case _ => c.error(c.enclosingPosition, "multiple argument lists no supported for exported functions/methods"); null
    })
      .map(a => getType(a.tpt))
      .map{
        case t if t <:< intType => TclType.Int
      }
      .zipWithIndex

  private def genArgs(argTypes: ArgTypes)(implicit commonParts: CommonParts): Seq[Tree] = argTypes.flatMap(genArgExtraction)

  private def genArgExtraction(arg: (TclType.Value,Int)): Seq[Tree] = {
    val argName = TermName("arg"+arg._2)
    arg._1 match {
      case TclType.Int =>
        Seq(q"""val $argName = scalanative.native.stackalloc[Int]""",
            q"""interp.getIntFromObj(objv(${arg._2+1}),$argName)""")
    }
  }

  private def genArgList(argTypes: ArgTypes)(implicit commonParts: CommonParts): Seq[Tree] = argTypes map {
    case (tpe,idx) if tpe == TclType.Int => q"!${TermName("arg"+idx)}"
  }

  private def genRegistration(f: DefDef)(implicit commonParts: CommonParts): Tree = {
    val cmdName = genCStringTree(genTclCommandName(f))
    q"""interp.createObjCommand($cmdName,CFunctionPtr.fromFunction4(__tcl.${f.name}),interp.cast[Ptr[Byte]],null)"""
  }

  private def genTclCommandName(f: DefDef)(implicit commonParts: CommonParts) =
    commonParts.fullName.replaceAll("\\.","::") + "::" + f.name.toString
}
