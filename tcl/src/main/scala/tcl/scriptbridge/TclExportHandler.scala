package tcl.scriptbridge

import de.surfice.smacrotools.{MacroAnnotationHandler, WhiteboxMacroTools}

import scala.reflect.macros.whitebox
import scala.scalanative.native.Ptr
import scala.scalanative.native.scriptbridge.{ExportHandler, noexport}

class TclExportHandler(val c: whitebox.Context) extends ExportHandler {
  import c.universe._

  case class ObjType(tclType: TclType.Value, scalaType: Type)

  type ArgTypes = Seq[(ObjType,Int)]
  type ReturnType = TclType.Value

  implicit class MacroData(data: Map[String,Any]) {
    type Data = Map[String, Any]
    def isClass: Boolean = data.getOrElse("tcl_isClass",false).asInstanceOf[Boolean]
    def withIsClass(isClass: Boolean): Data = data.updated("tcl_isClass",isClass)
    def exportedFunctions: Seq[DefDef] = data.getOrElse("tcl_exportedFunctions",Nil).asInstanceOf[Seq[DefDef]]
    def withExportedFunctions(exportedFunctions: Seq[DefDef]): Data = data.updated("tcl_exportedFunctions",exportedFunctions)
    def exportedMethods: Seq[DefDef] = data.getOrElse("tcl_exportedMethods",Nil).asInstanceOf[Seq[DefDef]]
    def withExportedMethods(exportedMethods: Seq[DefDef]): Data = data.updated("tcl_exportedMethods",exportedMethods)
    def ctorParams: Seq[ValDef] = data.getOrElse("tcl_ctorParams",Nil).asInstanceOf[Seq[ValDef]]
    def withCtorParams(params: Seq[ValDef]): Data = data.updated("tcl_ctorParams",params)
    def exportedObjectVals: Seq[ValDef] = data.getOrElse("tcl_exportedObjectVals",Nil).asInstanceOf[Seq[ValDef]]
    def withExportedObjectVals(exportedVals: Seq[ValDef]): Data = data.updated("tcl_exportedObjectVals",exportedVals)
    def exportedClsVals: Seq[ValDef] = data.getOrElse("tcl_exportedClsVals",Nil).asInstanceOf[Seq[ValDef]]
    def withExportedClsVals(exportedVals: Seq[ValDef]): Data = data.updated("tcl_exportedClsVals",exportedVals)
  }

  val intType = c.weakTypeOf[Int]
  val longType = c.weakTypeOf[Long]
  val booleanType = c.weakTypeOf[Boolean]
  val floatType = c.weakTypeOf[Float]
  val doubleType = c.weakTypeOf[Double]
  val stringType = c.weakTypeOf[String]
  val ptrType = c.weakTypeOf[Ptr[_]]
  val unitType = c.weakTypeOf[Unit]
  val tclBridgeInstanceType = c.weakTypeOf[TclBridgeInstance]
  val tclBridgeObject = tq"tcl.scriptbridge.TclBridgeObject"
  val tTclBridgeObject = c.weakTypeOf[TclBridgeObject]

  object TclType extends Enumeration {
    val Int = Value
    val Long = Value
    val Boolean = Value
    val Double = Value
    val Float = Value
    val String = Value
    val Ptr = Value
    val None = Value
    val Scala = Value
  }

  override def analyze: Analysis = {
    /* class with optional companion */
    case (cls: ClassParts, data: Data) =>

      val ctorParams = cls.params.collect{
        case p: ValDef => p
      }
      val paramVals = cls.params.collect{
        case v @ ValDef(mods,_,_,_) if mods.hasFlag(Flag.PARAMACCESSOR) => v
      }

      val (exportedMethods,exportedClsVals) = exportedMembers(cls.body)


      val (exportedFunctions,exportedObjectVals) = cls.companion match {
        case Some(obj) => exportedMembers(obj.body)
        case _ => (Nil,Nil)
      }
      (cls,data
        .withIsClass(true)
        .withCtorParams(ctorParams)
        .withExportedMethods(exportedMethods)
        .withExportedClsVals(exportedClsVals++paramVals)
        .withExportedFunctions(exportedFunctions)
        .withExportedObjectVals(exportedObjectVals)
      )

    /* object without class */
    case (obj: ObjectParts, data: Data) =>
      val (exportedFunctions, exportedVals) = exportedMembers(obj.body)
      val updData = data
        .withExportedFunctions(exportedFunctions)
        .withExportedObjectVals(exportedVals)
      (obj,updData)

    /* default */
    case default => default
  }

  override def transform: Transformation = {
    case cls: ClassTransformData =>
      val typeId = q"def __tclTypeId: Int = ${cls.modParts.companion.get.name}.hashCode"
      cls
        .updParents(cls.modParts.parents :+ tq"$tclBridgeInstanceType")
        .addStatements(typeId)
    /* object */
    case obj: ObjectTransformData =>
      implicit val objectParts = obj.modParts
      val methodWrappers = obj.data.exportedMethods map genMethodWrapper
      val functionWrappers = obj.data.exportedFunctions map genFunctionWrapper
      val valWrappers = obj.data.exportedClsVals map genValWrapper
      val objectValWrappers = obj.data.exportedObjectVals map genObjectValWrapper
      val registerCommands = (obj.data.exportedMethods ++ obj.data.exportedClsVals ++ obj.data.exportedFunctions) map genRegistration

      val constructor =
        if(obj.data.isClass) genConstructorWrapper(obj.data.ctorParams)
        else q""

      val registerConstructor =
        if(obj.data.isClass) {
          val cmdName = genCStringTree(genTclCommandName(obj.modParts.fullName,"new"))
          q"""interp.createObjCommand($cmdName,CFunctionPtr.fromFunction4(__tcl.__new),interp.cast[Ptr[Byte]],null)"""
        }
        else q""

      val tcloo = Literal(Constant(genTclOOWrapper(obj.data)))

      val tclObj =
        q"""
           object __tcl extends tcl.scriptbridge.TclBridgeWrapper {
             import scalanative.native._
             $constructor
             ..$methodWrappers
             ..$valWrappers
             ..${objectValWrappers.flatten}
             ..$functionWrappers

             def __register(interp: tcl.TclInterp): Unit = {
               $registerConstructor
               ..$registerCommands
             }

             val __tcloo = $tcloo
           }
         """

      val updParent = obj.modParts.parents.find{ parent => getType(parent,true) <:< tTclBridgeObject } match {
        case None => Some(tclBridgeObject)
        case _ => None
      }
      obj
        .updBody(obj.modParts.body :+ tclObj)
        .updParents(obj.modParts.parents ++ updParent)
    case default => default
  }


  private def genConstructorWrapper(params: Seq[ValDef])(implicit objectParts: ObjectParts): Tree = {
    val callee = TypeName(objectParts.nameString)
    val argTypes = getArgTypes(params)
    val argDefs = genArgs(argTypes)
    val argList = genArgList(argTypes)
    q"""def __new(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
            ..$argDefs
          val inst = new $callee(..$argList)
          val obj = tcl.newObj(inst)
          interp.setObjResult(obj)
          tcl.TclStatus.OK
       }"""
  }

  private def genMethodWrapper(f: DefDef)(implicit objectParts: ObjectParts): Tree = {
    val tpe = TypeName(objectParts.nameString)
    val argTypes = getArgTypes(f)
    val argDefs = genArgs(argTypes,offset = 2)
    val argList = genArgList(argTypes,offset = 2)
    val resObj = genTclResult(getReturnType(f))
    val name = TermName(exportedName(f))
    val call = f.vparamss match {
      case Nil => q"o.${f.name}"
      case List(_) => q"o.${f.name}(..$argList)"
      case _ =>
        c.error(c.enclosingPosition,"mutliple parameter lists are not supported")
        ???
    }
    q"""def $name(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
          val o = tcl.getScalaRep(objv(1)).cast[$tpe]
          ..$argDefs
          val res = $call
          ..$resObj
          tcl.TclStatus.OK
        }"""
  }

  private def genValWrapper(v: ValDef)(implicit objectParts: ObjectParts): Tree = {
    val tpe = TypeName(objectParts.nameString)
    val resObj = genTclResult(getReturnType(v))
    val name = TermName(exportedName(v))
    val argTypes = getArgTypes(List(v))
    val argDefs = genArgs(argTypes,offset = 2)
    val argList = genArgList(argTypes,offset = 2)
    if(v.mods.hasFlag(Flag.MUTABLE))
      q"""def $name(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
          val o = tcl.getScalaRep(objv(1)).cast[$tpe]
          if(objc>2) {
            ..$argDefs
            o.${v.name} = arg2
          }
          else {
            val res = o.${v.name}
            ..$resObj
          }
          tcl.TclStatus.OK
        }"""
    else
       q"""def $name(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
          val o = tcl.getScalaRep(objv(1)).cast[$tpe]
          val res = o.${v.name}
          ..$resObj
          tcl.TclStatus.OK
        }"""
  }

  private def genFunctionWrapper(f: DefDef)(implicit objectParts: ObjectParts): Tree = {
    val callee = objectParts.name
    val argTypes = getArgTypes(f)
    val argDefs = genArgs(argTypes)
    val argList = genArgList(argTypes)
    val resObj = genTclResult(getReturnType(f))
    val name = TermName(exportedName(f))
    q"""def $name(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
          ..$argDefs
          val res = $callee.${f.name}(..$argList)
            ..$resObj
          tcl.TclStatus.OK
        }"""
  }

  private def genObjectValWrapper(v: ValDef)(implicit objectParts: ObjectParts): Seq[Tree] = {
    val name = exportedName(v)
    val getterName = TermName("get" + name.head.toUpper + name.tail)
    val callee = objectParts.name
    val resObj = genTclResult(getReturnType(v))
    Seq(q"""def $getterName(data: tcl.TclClientData, interpPtr: Ptr[Byte], objc: Int, objv: Ptr[Ptr[Byte]]): Int = {
          val interp = data.cast[tcl.TclInterp]
          val res = $callee.${v.name}
          ..$resObj
          tcl.TclStatus.OK
     }""")
  }

  private def getArgTypes(f: DefDef)(implicit commonParts: CommonParts): ArgTypes =
    getArgTypes(f.vparamss match {
      case Nil => Nil
      case List(args) => args
      case _ => c.error(c.enclosingPosition, s"multiple argument lists no supported for exported functions/methods: ${f.name}")
        ???
    })

  private def getArgTypes(args: Seq[ValDef])(implicit commonParts: CommonParts): ArgTypes =
    args.map { a =>
      val tpe = getType(a.tpt, true)
      ObjType(tclType(tpe), tpe)
    }
    .zipWithIndex

  private def getReturnType(f: ValOrDefDef)(implicit commonParts: CommonParts): ReturnType =
    tclType(getType(f.tpt,true))

  private def tclType(tpe: Type): TclType.Value = tpe match {
    case t if t <:< intType => TclType.Int
    case t if t <:< longType => TclType.Long
    case t if t <:< booleanType => TclType.Boolean
    case t if t <:< doubleType => TclType.Double
    case t if t <:< floatType => TclType.Float
    case t if t <:< stringType => TclType.String
    case t if t =:= unitType => TclType.None
    case t if t <:< ptrType => TclType.Ptr
    case t => TclType.Scala
  }

  private def genArgs(argTypes: ArgTypes, offset: Int = 1)(implicit commonParts: CommonParts): Seq[Tree] = argTypes.flatMap(arg => genArgExtraction(arg,offset))

  private def genArgExtraction(arg: (ObjType,Int), offset: Int = 1): Seq[Tree] = {
    val argIdx = arg._2 + offset
    val argName = TermName("arg"+argIdx)
    arg._1.tclType match {
      case TclType.Int =>
        Seq(q"""val $argName  = interp.getInt(objv($argIdx))""")
      case TclType.Long =>
        Seq(q"""val $argName  = interp.getLong(objv($argIdx))""")
      case TclType.Boolean =>
        Seq(q"""val $argName = interp.getBoolean(objv($argIdx))""")
      case TclType.Double =>
        Seq(q"""val $argName = interp.getDouble(objv($argIdx))""")
      case TclType.Float =>
        Seq(q"""val $argName = interp.getFloat(objv($argIdx))""")
      case TclType.String =>
        Seq(q"""val $argName = tcl.getString(objv($argIdx))""")
      case TclType.Scala =>
        Seq(q"""val $argName = tcl.getScalaRep(objv($argIdx)).cast[Object].asInstanceOf[${arg._1.scalaType}]""")
      case TclType.Ptr =>
        Seq(q"""val $argName = interp.getPtr(objv($argIdx)).cast[${arg._1.scalaType}]""")
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
    case TclType.Scala | TclType.Ptr =>
      Seq(q"""interp.setObjResult(tcl.newObj(res))""")
    case TclType.None => Nil
  }

  private def genArgList(argTypes: ArgTypes, offset: Int = 1)(implicit commonParts: CommonParts): Seq[Tree] = argTypes map {
    case (_,idx) => q"${TermName("arg"+(idx+offset))}"
  }

  private def genRegistration(f: ValOrDefDef)(implicit commonParts: CommonParts): Tree = {
    val cmdName = genCStringTree(genTclCommandName(f))
    val name = TermName(exportedName(f))
    q"""interp.createObjCommand($cmdName,CFunctionPtr.fromFunction4(__tcl.$name),interp.cast[Ptr[Byte]],null)"""
  }

  private def genTclOOParamList(func: DefDef): Seq[String] = func.vparamss match {
    case List(xs) => xs.map(_.name.toString)
    case Nil => Nil
  }

  private def genTclOOParamListString(func: DefDef): String = genTclOOParamList(func) mkString " "

  private def genTclOOCallArgsString(func: DefDef): String = genTclOOParamList(func)
    .map(p => "[::__arg $"+ p +"]")
    .mkString(" ")

  private def genTclOOWrapper(data: Data)(implicit commonParts: CommonParts): String = {
    val clsPath = genTclPath(commonParts.fullName)
    val clsName = commonParts.nameString
    val pkgPath = clsPath.split("::").init.mkString("::")
    val newCmd = genTclCommandName(commonParts.fullName,"new")
    val newArgs = data.ctorParams.map(_.name.toString)

    val accessors = data.exportedClsVals.map { v =>
      val call = genTclCommandName(commonParts.fullName,v.name.toString)
      if(v.mods.hasFlag(Flag.MUTABLE))
        s"""  method ${v.name} {args} {
           |    $call $$Ref {*}$$args
           |  }""".stripMargin
      else
        s"""  method ${v.name} {} {
           |    $call $$Ref
           |  }""".stripMargin
    } mkString "\n"

    val methods = data.exportedMethods.map { m =>
      val params = genTclOOParamListString(m)
      val callArgs = genTclOOCallArgsString(m)
      val call = genTclCommandName(commonParts.fullName,m.name.toString)
        s"""|  method ${m.name} {$params} {
            |    $call $$Ref $callArgs
            |  }""".stripMargin
    } mkString "\n"

    val functions = data.exportedFunctions.map { f =>
      val params = genTclOOParamListString(f)
      val callArgs = genTclOOCallArgsString(f)
      val call = genTclCommandName(commonParts.fullName,f.name.toString)
      s"""|  method ${f.name} {$params} {
          |    $call $callArgs
          |  }""".stripMargin
    } mkString "\n"

    val exports = s"namespace eval $pkgPath { namespace export $clsName }"
    val superclass = "::ScalaBridgeObject"

    s"""$exports
       |oo::class create $clsPath {
       |  superclass $superclass
       |  variable Ref
       |  constructor {${newArgs.mkString(" ")}} {
       |    set Ref [$newCmd ${newArgs.map("$"+_).mkString(" ")}]
       |  }
       |$accessors
       |$methods
       |
       |  export __ref
       |}
       |
       |oo::objdefine $clsPath {
       |$functions
       |}
    """.stripMargin
  }


  private def genTclCommandName(f: ValOrDefDef)(implicit commonParts: CommonParts): String =
    genTclCommandName(commonParts.fullName,exportedName(f))

  private def genTclCommandName(prefix: String, cmd: String): String =
    genTclPath(prefix) + "::" + cmd

  private def genTclPath(path: String): String =
    path.replaceAll("\\.","::")

//  private def genQualifiedName(path: Seq[String]): Tree =
//    path.foldLeft()
}
