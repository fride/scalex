/* scaladoc, a documentation generator for Scala
 * Copyright 2005-2011 LAMP/EPFL
 * @author  Martin Odersky
 * @author  Geoffrey Washburn
 */

package ophir.dump

import ophir.db

import scala.tools.nsc._
import java.io.File.pathSeparator
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.util.FakePos
import Properties.msilLibPath

class Dumper {

  def process(files: List[String]): Unit = {
    var reporter: ConsoleReporter = null
    val docSettings = new doc.Settings(msg => reporter.error(FakePos("scaladoc"), msg + "\n  scaladoc -help  gives more information"))
    docSettings.debug.value = false
    docSettings.bootclasspath.value = (docSettings.bootclasspath.value :: jarPaths).mkString(":")
    reporter = new ConsoleReporter(docSettings) { override def hasErrors = false }

    log("Creating universe...")
    val universe = new Compiler(reporter, docSettings) universe files

    log("Extracting functions from the model...")
    val functions = new Extractor makeFunctions universe

    log("Dropping previous DB...")
    db.DefRepo.drop

    log("Saving %d functions..." format functions.size)
    functions.sliding(5000, 5000).toList map db.DefRepo.save

    log("Indexing DB...")
    db.DefRepo.index
  }

  private[this] def log(message: String) {
    println("* " + message)
  }

  private[this] def jarPaths: List[String] = {

    def jarPathOfClass(className: String) =
      Class.forName(className).getProtectionDomain.getCodeSource.getLocation.getFile

    List(
      jarPathOfClass("scala.tools.nsc.Interpreter"),
      jarPathOfClass("scala.ScalaObject"))
  }
}
