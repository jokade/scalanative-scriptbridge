organization in ThisBuild := "de.surfice"

version in ThisBuild := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.12"

val Version = new {
  val smacrotools   = "0.0.8"
  val cobj          = "0.0.6"
  val utest         = "0.6.4"
  val sbt_0_13      = "0.13.17"
  val sbt_1_0       = "1.1.0"
  val platform_deps = "1.0.0-M2"
  val scalanative   = "0.3.8"
}


lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-language:implicitConversions","-Xlint"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies ++= Seq(
    "de.surfice" %% "smacrotools" % Version.smacrotools,
    "de.surfice"  %%% "scalanative-interop-cobj" % Version.cobj,
    "com.lihaoyi" %%% "utest" % Version.utest % "test"
    ),
  testFrameworks += new TestFramework("utest.runner.Framework")
)

lazy val nativeSettings = Seq(
  nativeCompileOptions ++= Seq("-g"),
  nativeLinkStubs := true,
  nativeLinkingOptions ++= Seq(
    "-L/usr/local/opt/tcl-tk/lib","-ltcl8.6",
    "-lpython"
  )
)

lazy val scriptbridge = project.in(file("."))
  .aggregate(macros,tcl,plugin)
  .settings(dontPublish:_*)
  .settings(
    name := "scalanative-scriptbridge"
    )


lazy val macros = project
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings ++ nativeSettings ++ publishingSettings:_*)
  .settings(
    name :="scalanative-scriptbridge-macros"
  )

lazy val tcl = project
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(macros)
  .settings(commonSettings ++ nativeSettings ++ publishingSettings:_*)
  .settings(
    name := "scalanative-tcl"
  )

lazy val python = project
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(macros)
  .settings(commonSettings ++ nativeSettings ++ publishingSettings:_*)
  .settings(
    name := "scalanative-python"
  )

lazy val plugin = project
  .settings(publishingSettings: _*)
  .settings(
    name := "sbt-scalanative-scriptbridge",
    sbtBinaryVersion in update := (sbtBinaryVersion in pluginCrossBuild).value,
    scalaVersion := "2.10.6",
    addSbtPlugin("org.portable-scala" % "sbt-platform-deps" % Version.platform_deps),
    addSbtPlugin("org.scala-native" % "sbt-scala-native" % Version.scalanative),
    sbtPlugin := true,
    crossSbtVersions := Seq(Version.sbt_0_13, Version.sbt_1_0),
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "Version.scala"
      IO.write(file,
        s"""package de.surfice.sbt.scriptbridge
        |object Version { val scriptbridgeVersion = "${version.value}" }
        """.stripMargin)
      Seq(file)
    }.taskValue

  )


lazy val tests = project
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(tcl,python)
  .settings(commonSettings ++ nativeSettings ++ dontPublish:_*)
  .settings(
    scalacOptions += "-Xmacro-settings:scalanative.scriptbridge.handlers=" + Seq(
      "tcl.scriptbridge.TclExportHandler"
    ).mkString(";")
  )

//lazy val test = project
//  .enablePlugins(ScalaNativePlugin)
//  .dependsOn(appkit)
//  .settings(commonSettings ++ nativeSettings ++ dontPublish:_*)
//  .settings(
//    nativeLinkingOptions ++= Seq(
//      "-framework", "Foundation",
//      "/Volumes/JKDATA/dev/scala-native/cocoa/util.o"
//    )
//  )
//
//
lazy val dontPublish = Seq(
  publish := {},
  publishLocal := {},
  com.typesafe.sbt.pgp.PgpKeys.publishSigned := {},
  com.typesafe.sbt.pgp.PgpKeys.publishLocalSigned := {},
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository",file("target/unusedrepo")))
)

lazy val publishingSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>https://github.com/jokade/scalanative-scriptbridge</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jokade/scalanative-scriptbridge</url>
      <connection>scm:git:git@github.com:jokade/scalanative-scriptbridge.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jokade</id>
        <name>Johannes Kastner</name>
        <email>jokade@karchedon.de</email>
      </developer>
    </developers>
  )
)

