ThisBuild / scalaVersion := "2.12.13"
ThisBuild / version := "0.1"
ThisBuild / libraryDependencies += "org.apache.jena" % "jena-arq"    % "3.16.0"
ThisBuild / libraryDependencies += "commons-cli"     % "commons-cli" % "1.4"

enablePlugins(PackPlugin)
enablePlugins(BuildInfoPlugin)
packMain := Map("sem-diff" -> "comparator.SemanticDiffApp")
