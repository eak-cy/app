import sbt.*
import sbt.Keys.*

object Projects {

  implicit class ProjectOps(private val project: Project) {
    def withDependencies(deps: ModuleID*): Project =
      project.settings(libraryDependencies ++= deps.toSeq)
  }
}
