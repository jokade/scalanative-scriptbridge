package scala.scalanative.native.scriptbridge

import de.surfice.smacrotools.MacroAnnotationHandler

import scala.reflect.macros.whitebox

trait ExportHandler extends MacroAnnotationHandler {
  import c.universe._

  override def supportsClasses: Boolean = ???
  override def supportsTraits: Boolean = ???
  override def supportsObjects: Boolean = ???
  override def createCompanion: Boolean = ???
  override def annotationName: String = ???

  protected def genCStringTree(s: String): Tree =
    q"scalanative.native.CQuote(StringContext(${Literal(Constant(s))})).c()"
}
