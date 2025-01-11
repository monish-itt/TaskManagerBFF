package taskbff.infrastructure.routes

import cats.effect.{Async, LiftIO}
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.Logger
import taskbff.Task
import taskbff.domian.models.TaskData
import taskbff.domian.repositories.TaskRepository

object TaskRoutes {

  implicit def taskEntityDecoder[F[_]: Async]: EntityDecoder[F, Task] = jsonOf[F, Task]
  implicit def taskEntityEncoder[F[_]: Async]: EntityEncoder[F, Task] = jsonEncoderOf[F, Task]
  implicit def taskInputEntityDecoder[F[_]: Async]: EntityDecoder[F, TaskData] = jsonOf[F, TaskData]
  implicit def taskInputEntityEncoder[F[_]: Async]: EntityEncoder[F, TaskData] = jsonEncoderOf[F, TaskData]
  implicit def listTaskEntityEncoder[F[_]: Async]: EntityEncoder[F, List[Task]] = jsonEncoderOf[F, List[Task]]

  def taskRoutes[F[_]: Async: LiftIO: Logger](taskRepo: TaskRepository): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      case req @ POST -> Root / "tasks" =>
        for {
          taskInput <- req.as[TaskData]
          _ <- Logger[F].info(s"Received request to create task: $taskInput")
          taskWithId = Task(0, taskInput.task, taskInput.statusId, taskInput.tags, taskInput.userId)
          _ <- Logger[F].info(s"Received request to create task: $taskWithId")
          result <- LiftIO[F].liftIO(taskRepo.createTask(taskWithId)).attempt
          response <- result match {
            case Right(_) => Created(taskWithId)
            case Left(e) => InternalServerError(s"Failed to create task: ${e.getMessage}")
          }
        } yield response


      case GET -> Root / "tasks" =>
        Logger[F].info("Received request to get all tasks") >>
          LiftIO[F].liftIO(taskRepo.getAllTasks).flatMap { tasks =>
            Logger[F].info(s"Successfully retrieved tasks: $tasks") >>
              Ok(tasks)
          }.handleErrorWith { err =>
            Logger[F].error(err)("Failed to retrieve tasks") >>
              InternalServerError("Something went wrong")
          }

      case GET -> Root / "tasks" / IntVar(id) =>
        Logger[F].info(s"Received request to get task with id $id") >>
          LiftIO[F].liftIO(taskRepo.getTaskById(id)).flatMap {
            case Some(task) => Ok(task)
            case None => NotFound(s"No task found with id $id")
          }

      case req @ PUT -> Root / "tasks" / IntVar(id) =>
        req.as[TaskData].flatMap { updatedTaskInput =>
          Logger[F].info(s"Received request to update task with id $id with data: $updatedTaskInput") >>
            LiftIO[F].liftIO {
              taskRepo.updateTask(id, Task(id, updatedTaskInput.task, updatedTaskInput.statusId, updatedTaskInput.tags, updatedTaskInput.userId))
            }.flatMap {
              case 1 => Ok(updatedTaskInput)
              case _ => NotFound(s"No task found with id $id")
            }
        }

      case DELETE -> Root / "tasks" / IntVar(id) =>
        Logger[F].info(s"Received request to delete task with id $id") >>
          LiftIO[F].liftIO(taskRepo.deleteTask(id)).flatMap {
            case 1 => NoContent()
            case _ => NotFound(s"No task found with id $id")
          }
    }
  }
}