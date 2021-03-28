import sbt._
import Keys._
import dotty.tools.sbtplugin.DottyPlugin.autoImport._
import scalafix.sbt.ScalafixPlugin.autoImport._

object BuildHelper {
  val Scala213   = "2.13.5"
  val ScalaDotty = "3.0.0-RC1"

  private val stdOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-language:postfixOps",
  ) ++ {
    if (sys.env.contains("CI")) {
      Seq("-Xfatal-warnings")
    } else {
      Nil // to enable Scalafix locally
    }
  }

  private val std2xOptions = Seq(
    "-language:higherKinds",
    "-language:existentials",
    "-explaintypes",
    "-Yrangepos",
    "-Xlint:_,-missing-interpolator,-type-parameter-shadow",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
  )

  //RECOMMENDED SETTINGS: https://tpolecat.github.io/2017/04/25/scalac-flags.html
  private val tpoleCatSettings      = Seq(
    "-language:postfixOps",                      // Added by @tusharmath
    "-deprecation",                              // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",                                     // Specify character encoding used by source files.
    "-explaintypes",                             // Explain type errors in more detail.
    "-feature",                                  // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",                    // Existential types (besides wildcard types) can be written and inferred
    "-language:higherKinds",                     // Allow higher-kinded types
    "-unchecked",                                // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                               // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                          // Fail the compilation if there are any warnings.
    "-Xlint:adapted-args",                       // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant",                           // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",                 // Selecting member of DelayedInit.
    "-Xlint:doc-detached",                       // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",                       // Warn about inaccessible types in method signatures.
    "-Xlint:missing-interpolator",               // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit",                       // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",                    // Option.apply used implicit view.
    "-Xlint:package-object-classes",             // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",             // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",                     // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                        // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",              // A local type parameter shadows a type already in scope.
    "-Xlint:unused",                             // TODO check if we still need -Wunused below
    "-Xlint:nonlocal-return",                    // A return statement used an exception for flow control.
    "-Xlint:implicit-not-found",                 // Check @implicitNotFound and @implicitAmbiguous messages.
    "-Xlint:serial",                             // @SerialVersionUID on traits and non-serializable classes.
    "-Xlint:valpattern",                         // Enable pattern checks in val definitions.
    "-Xlint:eta-zero",                           // Warn on eta-expansion (rather than auto-application) of zero-ary method.
    "-Xlint:eta-sam",                            // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
    "-Xlint:deprecation",                        // Enable linted deprecations.
    "-Wdead-code",                               // Warn when dead code is identified.
    "-Wextra-implicit",                          // Warn when more than one implicit parameter section is defined.
    "-Wmacros:both",                             // Lints code before and after applying a macro
    "-Wnumeric-widen",                           // Warn when numerics are widened.
    "-Woctal-literal",                           // Warn on obsolete octal syntax.
    "-Wunused:imports",                          // Warn if an import selector is not referenced.
    "-Wunused:patvars",                          // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates",                         // Warn if a private member is unused.
    "-Wunused:locals",                           // Warn if a local definition is unused.
    "-Wunused:explicits",                        // Warn if an explicit parameter is unused.
    "-Wunused:implicits",                        // Warn if an implicit parameter is unused.
    "-Wunused:params",                           // Enable -Wunused:explicits,implicits.
    "-Wunused:linted",
    "-Wvalue-discard",                           // Warn when non-Unit expression results are unused.
    "-Ybackend-parallelism",
    "8",                                         // Enable paralellisation — change to desired number!
    "-Ycache-plugin-class-loader:last-modified", // Enables caching of classloaders for compiler plugins
    "-Ycache-macro-class-loader:last-modified",  // and macro definitions. This can lead to performance improvements.

    // FIXME: Disabled because of scalac bug https://github.com/scala/bug/issues/11798
    //  "-Xlint:infer-any",                 // Warn when a type argument is inferred to be `Any`.
    //  "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
    //  "-language:experimental.macros",   // Allow macro definition (besides implementation and application). Disabled, as this will significantly change in Scala 3
    //  "-language:implicitConversions",   // Allow definition of implicit functions called views. Disabled, as it might be dropped in Scala 3. Instead use extension methods (implemented as implicit class Wrapper(val inner: Foo) extends AnyVal {}
  )

  private def optimizerOptions(optimize: Boolean) =
    if (optimize)
      Seq(
        "-opt:l:inline",
      )
    else Nil

  def extraOptions(scalaVersion: String, isDotty: Boolean, optimize: Boolean) =
    CrossVersion.partialVersion(scalaVersion) match {
      case _ if isDotty  =>
        Seq(
          "-language:implicitConversions",
          "-Xignore-scala2-macros",
          "-noindent",
        )
      case Some((2, 13)) => Seq("-Ywarn-unused:params,-implicits") ++ std2xOptions ++ tpoleCatSettings ++ optimizerOptions(optimize)
      case _             => Seq.empty
    }

  def stdSettings(prjName: String) = Seq(
    name := s"$prjName",
    crossScalaVersions in ThisBuild := Seq(Scala213, ScalaDotty),
    scalaVersion in ThisBuild := Scala213,
    useScala3doc := true,
    scalacOptions := stdOptions ++ extraOptions(scalaVersion.value, isDotty.value, optimize = !isSnapshot.value),
    scalacOptions --= {
      if (isDotty.value)
        Seq("-Xfatal-warnings")
      else
        Seq()
    },
    semanticdbEnabled := !isDotty.value,              // enable SemanticDB
    semanticdbOptions += "-P:semanticdb:synthetics:on",
    semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
    ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    ThisBuild / scalafixDependencies ++=
      List(
        "com.github.liancheng" %% "organize-imports" % "0.5.0",
        "com.github.vovapolu"  %% "scaluzzi"         % "0.1.16",
      ),
    parallelExecution in Test := true,
    incOptions ~= (_.withLogRecompileOnMacro(false)),
    autoAPIMappings := true,
  )
}