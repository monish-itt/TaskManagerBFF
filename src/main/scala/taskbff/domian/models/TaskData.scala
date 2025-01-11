package taskbff.domian.models

case class TaskData(
                     task: String,
                     statusId: Int,
                     tags: List[String],
                     userId: Int
                   )
