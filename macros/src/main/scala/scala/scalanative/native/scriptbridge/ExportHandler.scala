package scala.scalanative.native.scriptbridge

import de.surfice.smacrotools.MacroAnnotationHandler

import scala.reflect.macros.whitebox

trait ExportHandler extends MacroAnnotationHandler {
  import c.universe._

  val noexportAnnot = c.weakTypeOf[noexport]
  val exportAnnot = c.weakTypeOf[export]
  val exportAnnotParams = Seq("name")

  override def supportsClasses: Boolean = ???
  override def supportsTraits: Boolean = ???
  override def supportsObjects: Boolean = ???
  override def createCompanion: Boolean = ???
  override def annotationName: String = ???

  protected def genCStringTree(s: String): Tree =
    q"scalanative.native.CQuote(StringContext(${Literal(Constant(s))})).c()"

  protected def isPublic(m: DefDef): Boolean = ! (m.mods.hasFlag(Flag.PRIVATE) || m.mods.hasFlag(Flag.PROTECTED))

  protected def shouldExport(m: DefDef): Boolean = isPublic(m) && !hasAnnotation(m.mods.annotations,noexportAnnot)

  protected def exportedMembers(body: Seq[Tree]): Seq[DefDef] = body collect {
    case m: DefDef if shouldExport(m) => m
  }

  protected def exportedName(m: DefDef): String =
    findAnnotation(m.mods.annotations,"export")
      .map( a => extractAnnotationParameters(a,exportAnnotParams).get("name").map{
        case Some(a) => extractStringConstant(a).get
        case None => m.name.toString
      }.get)
      .getOrElse(m.name.toString)

}
