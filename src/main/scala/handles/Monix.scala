package handles

import main.Main.logger
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success}

object Monix {

  //Schedulers
  implicit val io: Scheduler = Scheduler.fixedPool("io", 200)
  implicit val aggregation: Scheduler = Scheduler.fixedPool("aggregation", 200)

  implicit def completeTask[A <: AnyRef](x: Task[A])(implicit scheduler: Scheduler): Unit = {
    x.runOnComplete {
      case Success(res) => logger.info(s"${Console.MAGENTA} SUCCESS: \n${Console.GREEN} $res")
      case Failure(err) => logger.error(s"${Console.RED} FAILURE \n${err.getMessage}")
    }(global)
  }
}

