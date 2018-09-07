package tcl.scriptbridge

import de.surfice.smacrotools.{MacroAnnotationHandler, WhiteboxMacroTools}

import scala.reflect.macros.whitebox
import scala.scalanative.native.scriptbridge.ExportHandler

class TclExportHandler(val c: whitebox.Context) extends ExportHandler {
  import c.universe._

  type ArgTypes = Seq[(TclType.Value,Int)]
  type ReturnType = TclType.Value

  implicit class MacroData(data: Map[String,Any]) {
    type Data = Map[String, Any]
    def exportedFunctions: Seq[DefDef] = data.getOrElse("tcl_exportedFunctions",Nil).asInstanceOf[Seq[DefDef]]
    def withExportedFunctions(exportedFunctions: Seq[DefDef]): Data = data.updated("tcl_exportedFunctions",exportedFunctions)
  }

  val intType = c.weakTypeOf[Int]
  val longType = c.weakTypeOf[Long]
  val booleanType = c.weakTypeOf[Boolean]
  val floatType = c.weakTypeOf[Float]
  val doubleType = c.weakTypeOf[Double]
  val stringType = c.weakTypeOf[String]
  val unitType = c.weakTypeOf[Unit]

  object TclType extends Enumeration {
    val Int = Value
    val Long = Value
    val Boolean = Value
    val Double = Value
    val Float = Value
    val String = Value
    val None = Value
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
             import scalanative.native._
             ..$functionWrappers

             def __register(interp: tcl.TclInterp): Unit = {
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
    val callee = objectParts.name
    val argTypes = getArgTypes(f)
    val argDefs = genArgs(argTypes)
    val argList = genArgList(argTypes)
    val resObj = genTclResult(getReturnType(f))
    q"""def ${f.name}(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
          ..$argDefs
          val res = $callee.${f.name}(..$argList)
            ..$resObj
          tcl.TclStatus.OK
        }"""
  }

  private def getArgTypes(f: DefDef)(implicit commonParts: CommonParts): ArgTypes =
    (f.vparamss match {
      case List(args) => args
      case _ => c.error(c.enclosingPosition, "multiple argument lists no supported for exported functions/methods"); null
    })
      .map( a => tclType(getType(a.tpt)) )
      .zipWithIndex

  private def getReturnType(f: DefDef)(implicit commonParts: CommonParts): ReturnType =
    tclType(getType(f.tpt))

  private def tclType(tpe: Type): TclType.Value = tpe match {
    case t if t <:< intType => TclType.Int
    case t if t <:< longType => TclType.Long
    case t if t <:< booleanType => TclType.Boolean
    case t if t <:< doubleType => TclType.Double
    case t if t <:< floatType => TclType.Float
    case t if t <:< stringType => TclType.String
    case t if t =:= unitType => TclType.None
  }

  private def genArgs(argTypes: ArgTypes)(implicit commonParts: CommonParts): Seq[Tree] = argTypes.flatMap(genArgExtraction)

  private def genArgExtraction(arg: (TclType.Value,Int)): Seq[Tree] = {
    val argName = TermName("arg"+arg._2)
    arg._1 match {
      case TclType.Int =>
        Seq(q"""val $argName  = interp.getInt(objv(${arg._2+1}))""")
      case TclType.Long =>
        Seq(q"""val $argName  = interp.getLong(objv(${arg._2+1}))""")
      case TclType.Boolean =>
        Seq(q"""val $argName = interp.getBoolean(objv(${arg._2+1}))""")
      case TclType.Double =>
        Seq(q"""val $argName = interp.getDouble(objv(${arg._2+1}))""")
      case TclType.Float =>
        Seq(q"""val $argName = interp.getFloat(objv(${arg._2+1}))""")
      case TclType.String =>
        Seq(q"""val $argName = tcl.getString(objv(${arg._2+1}))""")
    }
  }

  private def genTclResult(retType: ReturnType)(implicit commonParts: CommonParts): Seq[Tree] = retType match {
    case TclType.Int =>
      Seq(q"""interp.setObjResult(tcl.newIntObj(res))""")
    case TclType.Long =>
      Seq(q"""interp.setObjResult(tcl.newLongObj(res))""")
    case TclType.Boolean =>
      Seq(q"""interp.setObjResult(tcl.newBooleanObj(res))""")
    case TclType.Double | TclType.Float =>
      Seq(q"""interp.setObjResult(tcl.newDoubleObj(res))""")
    case TclType.String =>
      Seq(q"""interp.setObjResult(tcl.newStringObj(res))""")
    case TclType.None => Nil
  }

  private def genArgList(argTypes: ArgTypes)(implicit commonParts: CommonParts): Seq[Tree] = argTypes map {
    case (tpe,idx) => q"${TermName("arg"+idx)}"
  }

  private def genRegistration(f: DefDef)(implicit commonParts: CommonParts): Tree = {
    val cmdName = genCStringTree(genTclCommandName(f))
    q"""interp.createObjCommand($cmdName,CFunctionPtr.fromFunction4(__tcl.${f.name}),interp.cast[Ptr[Byte]],null)"""
  }

  private def genTclCommandName(f: DefDef)(implicit commonParts: CommonParts) =
    commonParts.fullName.replaceAll("\\.","::") + "::" + f.name.toString

//  private def genQualifiedName(path: Seq[String]): Tree =
//    path.foldLeft()
}
