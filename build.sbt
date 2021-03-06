import sbtrelease.Version

lazy val contributors = Seq(
  "pchiusano" -> "Paul Chiusano",
  "pchlupacek" -> "Pavel Chlupáček",
  "alissapajer" -> "Alissa Pajer",
  "djspiewak" -> "Daniel Spiewak",
  "fthomas" -> "Frank Thomas",
  "runarorama" -> "Rúnar Ó. Bjarnason",
  "jedws" -> "Jed Wesley-Smith",
  "wookietreiber" -> "Christian Krause",
  "mpilquist" -> "Michael Pilquist",
  "guersam" -> "Jisoo Park"
)

val catsVersion = "0.8.0"

def scmBranch(v: String): String = {
  val Some(ver) = Version(v)
  s"v${ver.string}"
}

lazy val commonSettings = Seq(
  name := "fs2-cats",
  organization := "co.fs2",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.12.0-RC2"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import"
  ),
  scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (Test, console) <<= (scalacOptions in (Compile, console)),
  libraryDependencies ++= Seq(
    "co.fs2" %%% "fs2-core" % "0.9.1",
    "org.typelevel" %%% "cats-core" % catsVersion,
    "org.typelevel" %%% "cats-laws" % catsVersion % "test"
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/functional-streams-for-scala/fs2-cats"), "git@github.com:functional-streams-for-scala/fs2-cats.git")),
  homepage := Some(url("https://github.com/functional-streams-for-scala/fs2")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  initialCommands := s"""
    import fs2._
    import fs2.interop.cats._
    import cats._
    import cats.implicits._
  """,
  resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.2" cross CrossVersion.binary)
) ++ testSettings ++ scaladocSettings ++ publishingSettings ++ releaseSettings

lazy val testSettings = Seq(
  parallelExecution in Test := false,
  logBuffered in Test := false,
  testOptions in Test += Tests.Argument("-verbosity", "2"),
  testOptions in Test += Tests.Argument("-minSuccessfulTests", "500"),
  publishArtifact in Test := true
)

lazy val scaladocSettings = Seq(
  scalacOptions in (Compile, doc) ++= Seq(
    "-doc-source-url", s"${scmInfo.value.get.browseUrl}/tree/${scmBranch(version.value)}€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-implicits",
    "-implicits-show-all"
  ),
  scalacOptions in (Compile, doc) ~= (_.filterNot(_ == "-Xfatal-warnings")),
  autoAPIMappings := true
)

lazy val publishingSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  pomExtra := {
    <developers>
      {for ((username, name) <- contributors) yield
      <developer>
        <id>{username}</id>
        <name>{name}</name>
        <url>http://github.com/{username}</url>
      </developer>
      }
    </developers>
  },
  pomPostProcess := { node =>
    import scala.xml._
    import scala.xml.transform._
    def stripIf(f: Node => Boolean) = new RewriteRule {
      override def transform(n: Node) =
        if (f(n)) NodeSeq.Empty else n
    }
    val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
    new RuleTransformer(stripTestScope).transform(node)(0)
  }
)

lazy val releaseSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val commonJsSettings = Seq(
  requiresDOM := false,
  scalaJSStage in Test := FastOptStage,
  jsEnv in Test := NodeJSEnv().value,
  scalacOptions in Compile += {
    val dir = project.base.toURI.toString.replaceFirst("[^/]+/?$", "")
    val url = s"https://raw.githubusercontent.com/functional-streams-for-scala/fs2-cats"
    s"-P:scalajs:mapSourceURI:$dir->$url/${scmBranch(version.value)}/"
  }
)

lazy val fs2Cats = crossProject.in(file(".")).
  settings(commonSettings: _*).
  jsSettings(commonJsSettings: _*)

lazy val fs2CatsJVM = fs2Cats.jvm
lazy val fs2CatsJS = fs2Cats.js
