package de.surfice.sbt.scriptbridge

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin
import sbt.Keys._
import sbt._

import scala.scalanative.sbtplugin.ScalaNativePlugin

object ScriptBridgePlugin extends AutoPlugin {

  override def requires = ScalaNativePlugin

  object autoImport {
    object ScriptBridgeLanguage extends Enumeration {
      val Tcl = Value
    }

    val scriptBridgeLanguages: SettingKey[Seq[ScriptBridgeLanguage.Value]] =
      settingKey[Seq[ScriptBridgeLanguage.Value]]("List of script bridge languages to activate")
  }

  import autoImport._

  private val groupId = PlatformDepsPlugin.autoImport.toPlatformDepsGroupID("de.surfice")

  override def projectSettings = Seq(

    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),

    scriptBridgeLanguages := Nil,

    scalacOptions += scriptBridgeLanguages.value.map {
      case ScriptBridgeLanguage.Tcl => "tcl.scriptbridge.TclExportHandler"
    } mkString("-Xmacro-settings:scalanative.scriptbridge.handlers=",";",""),

    libraryDependencies ++=
      scriptBridgeLanguages.value.map{ lang =>
        groupId.%%%(s"scalanative-${languageName(lang)}").%(Version.scriptbridgeVersion)
      }
  )

  private def languageName(lang: ScriptBridgeLanguage.Value): String = lang.toString.toLowerCase
}
