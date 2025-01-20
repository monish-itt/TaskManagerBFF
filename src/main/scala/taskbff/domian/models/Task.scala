package taskbff.domian.models

import doobie.Read


case class Task(
                 id: Int,
                 task: String,
                 status: Int,
                 tags: List[String],
                 user_id: Int
               )

object Task {
  implicit val taskRead: Read[Task] = Read[(Int, String, Int, String, Int)].map {
    case (id, task, statusId, tags, userId) =>
      val cleanTags = tags.stripPrefix("{").stripSuffix("}").split(",").map(_.trim.stripPrefix("\"").stripSuffix("\"")).toList
      Task(id, task, statusId, cleanTags, userId)
  }
}