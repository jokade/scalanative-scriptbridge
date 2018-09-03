package scala.scalanative.scriptbridge

import scala.language.experimental.macros
import de.surfice.smacrotools.MacroAnnotationHandler

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.reflect.macros.whitebox

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Export extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Export.Macro.impl
}

object Export {

  private[scriptbridge] class Macro(val c: whitebox.Context) extends MacroAnnotationHandler {

    override def annotationName: String = "Export"

    override def supportsClasses: Boolean = false

    override def supportsTraits: Boolean = false

    override def supportsObjects: Boolean = true

    override def createCompanion: Boolean = true

    val handlerClasses = Seq("tcl.scriptbridge.TclExportHandler")

    val handlers = handlerClasses map { cls =>
      getClass.getClassLoader.loadClass(cls).getConstructors.head.newInstance(c).asInstanceOf[MacroAnnotationHandler]
    }


    override def analyze: Analysis = handlers.foldLeft(super.analyze)((analyzer,handler) => analyzer andThen handler.analyze.asInstanceOf[Analysis])

    override def transform: Transformation = handlers.foldLeft(super.transform)((transformer,handler) => transformer andThen handler.transform.asInstanceOf[Transformation])
  }

}
