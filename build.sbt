organization in ThisBuild := "de.surfice"

version in ThisBuild := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.12"

val Version = new {
  val smacrotools = "0.0.8"
  val cobj        = "0.0.5-SNAPSHOT"
  val utest       = "0.6.4"
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
    "-ltcl")
)

lazy val scriptbridge = project.in(file("."))
  .aggregate(macros,tcl)
  .settings(dontPublish:_*)
  .settings(
    name := "scalanative-scriptbridge"
    )


lazy val macros = project
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings ++ nativeSettings ++ publishingSettings:_*)
  .settings(
    name :=" scalanative-scriptbridge-macros"
  )

lazy val tcl = project
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(macros)
  .settings(commonSettings ++ nativeSettings ++ publishingSettings:_*)
  .settings(
    name := "scalanative-tcl"
  )

lazy val tests = project
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(tcl)
  .settings(commonSettings ++ nativeSettings ++ dontPublish:_*)


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

