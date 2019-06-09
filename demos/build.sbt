organization in ThisBuild := "de.surfice"

version in ThisBuild := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.12"

val Version = new {
  val scalanative   = "0.3.8"
}


lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-language:implicitConversions","-Xlint"),
  libraryDependencies ++= Seq(
    )
)

lazy val demos = project.in(file("."))
  .settings(dontPublish:_*)
  .settings {
    name := "ScriptBridge Demos"
  }


lazy val tcl = project
  .enablePlugins(ScalaNativePlugin,ScriptBridgePlugin)
  .settings(
    nativeLinkingOptions ++= Seq(
      "-L/usr/local/opt/tcl-tk/lib","-ltcl8.6","-ltk8.6"
    ),
    scriptBridgeLanguages ++= Seq(ScriptBridgeLanguage.Tcl)
  )


lazy val dontPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository",file("target/unusedrepo")))
)

