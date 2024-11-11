package taskbff

import doobie._

case class TaskInput(task: String, status: String, tags: List[String])
case class Task(id: String, task: String, status: String, tags: List[String])

object Task {
  implicit val taskRead: Read[Task] = Read[(String, String, String, String)].map {
    case (id, task, status, tags) =>
      val cleanTags = tags.stripPrefix("{").stripSuffix("}").split(",").map(_.trim.stripPrefix("\"").stripSuffix("\"")).toList
      Task(id, task, status, cleanTags)
  }
}