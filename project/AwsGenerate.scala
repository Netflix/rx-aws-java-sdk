import scala.collection.JavaConversions._

import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

object AwsGenerate {
  import com.google.common.reflect._

  val clientPattern = """^Amazon([A-Za-z0-9]+?(?<!Async))?Client$""".r

  def test(dir: File): Seq[File] = {
    val file = new File(dir, "Test2.java")
println(clientClasses)
    val content = """class Test2 { static { System.out.println("Hi"); } }"""
    Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))
    Seq(file)
  }

  def clientClasses: List[ClassPath.ClassInfo] = {
    val cl = classOf[com.amazonaws.services.ec2.AmazonEC2].getClassLoader
    val pkg = "com.amazonaws.services"
    ClassPath.from(cl).getTopLevelClassesRecursive(pkg).flatMap(cinfo => {
      cinfo.getSimpleName match {
        case clientPattern(prefix) => {
          println(prefix)
          Some(cinfo)
        }
        case _ => None
      }
    }).toList.sortBy(_.toString)
  }
}
