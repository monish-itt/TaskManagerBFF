package taskbff

import doobie.Read

case class Tag(tag_id: Int, name: String)

object Tag {
  implicit val tagRead: Read[Tag] = Read[(Int, String)].map {
    case (id, name) =>
      Tag(id, name)
  }
}
