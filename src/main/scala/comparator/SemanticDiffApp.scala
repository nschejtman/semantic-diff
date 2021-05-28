package comparator
import buildinfo.BuildInfo
import org.apache.commons.cli.{DefaultParser, HelpFormatter, Options, Option => Arg}
import org.apache.jena.rdf.model.{Model, ModelFactory}

import java.io.FileReader
import java.nio.file.Paths

object SemanticDiffApp {
  type InfoOptions    = Options
  type ProgramOptions = Options

  def buildOptions(): (InfoOptions, ProgramOptions) = {
    val left = Arg
      .builder("l")
      .longOpt("left")
      .argName("Left")
      .desc("Left file or directory")
      .hasArg
      .numberOfArgs(1)
      .required
      .`type`(classOf[String])
      .build()

    val right = Arg
      .builder("r")
      .longOpt("right")
      .argName("Right")
      .desc("Right file or directory")
      .hasArg
      .numberOfArgs(1)
      .required
      .`type`(classOf[String])
      .build()

    val paths = Arg
      .builder("pf")
      .longOpt("paths-file")
      .argName("Paths file")
      .desc("File containing relative file paths to left and right base directories")
      .hasArg
      .numberOfArgs(1)
      .required(false)
      .`type`(classOf[String])
      .build()

    val version = Arg
      .builder("v")
      .longOpt("version")
      .argName("Version")
      .desc("Version of semantic diff")
      .required(false)
      .build()

    val help = Arg
      .builder("h")
      .longOpt("help")
      .argName("Help")
      .desc("Help")
      .required(false)
      .build()

    val programOptions = new Options()
      .addOption(left)
      .addOption(right)
      .addOption(paths)

    val infoOptions = new Options()
      .addOption(version)
      .addOption(help)

    (infoOptions, programOptions)
  }

  def main(args: Array[String]): Unit = {
    val (infoOptions, programOptions) = buildOptions()
    val helpFormatter                 = new HelpFormatter()

    val parser = new DefaultParser()

    val infoArgs = parser.parse(infoOptions, args, true)
    if (infoArgs.getOptions.nonEmpty) {
      if (infoArgs.hasOption("version")) {
        println(BuildInfo.version)
      } else if (infoArgs.hasOption("help")) {
        helpFormatter.printHelp("sem-diff", programOptions, true)
      }
    } else {
      val programArgs = parser.parse(programOptions, args)
      val left        = programArgs.getOptionValue("left")
      val right       = programArgs.getOptionValue("right")

      if (programArgs.hasOption("paths-file")) {
        val pathsFile = programArgs.getOptionValue("paths-file")
        bulkDiff(left, right, extractPathsFrom(absolute(pathsFile)))
      } else {
        singleDiff(absolute(left), absolute(right))
      }
    }
  }

  private def absolute(path: String, customRoot: Option[String] = None): String = {
    val isRelativeToHome = path.startsWith("~/")
    val relativePath     = path.stripPrefix("~/")

    val rootPath = {
      if (isRelativeToHome) {
        Paths.get(System.getProperty("user.home"))
      } else if (customRoot.isDefined) {
        val resolvedCustomRoot = absolute(customRoot.get)
        Paths.get(resolvedCustomRoot)
      } else {
        Paths.get(System.getProperty("user.dir"))
      }
    }

    rootPath.resolve(relativePath).toAbsolutePath.toString
  }

  private def extractPathsFrom(pathsFilePath: String): Seq[String] = {
    val bufferedSource = io.Source.fromFile(pathsFilePath)
    val lines          = (for (line <- bufferedSource.getLines()) yield line).toList
    bufferedSource.close
    lines
  }

  private def bulkDiff(leftBasePath: String, rightBasePath: String, relativePaths: Seq[String]): Unit = {
    relativePaths.foreach { file =>
      val leftFilePath  = absolute(file, Some(leftBasePath))
      val rightFilePath = absolute(file, Some(rightBasePath))
      singleDiff(leftFilePath, rightFilePath)
    }
  }

  private def singleDiff(leftFilePath: String, rightFilePath: String): Unit = {
    val leftModel  = readModelFrom(leftFilePath)
    val rightModel = readModelFrom(rightFilePath)

    val isomorphism = leftModel.isIsomorphicWith(rightModel)
    println {
      s"""
         |-------------------------------------------------------------------------------------------------------------------------
         | Semantic diff result
         | Left: $leftFilePath
         | Right: $rightFilePath
         |-------------------------------------------------------------------------------------------------------------------------
         | Isomorphism: $isomorphism
         |""".stripMargin
    }
    if (!isomorphism) {
      val leftDiff  = leftModel.difference(rightModel)
      val rightDiff = rightModel.difference(leftModel)
      if (!leftDiff.isEmpty) printModel(leftDiff, "Left diff")
      if (!rightDiff.isEmpty) printModel(rightDiff, "Right diff")
    }
  }

  private def readModelFrom(filePath: String): Model = {
    val model = ModelFactory.createDefaultModel()
    model.read(new FileReader(filePath), null, "JSON-LD")
  }

  private def printModel(model: Model, name: String): Unit = {
    try {
      println {
        s"""
         | ----
         | $name
         | ----
         | 
         |""".stripMargin
      }
      model.write(System.out, "JSON-LD")
    } catch {
      case e: org.apache.jena.shared.InvalidPropertyURIException => println(s"Invalid Property URI: ${e.getMessage}")
    }
  }

}
