import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.{Behavior, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.my.bank.actors.Bank
import com.my.bank.events.{BankAccountCreatedResponse, CreateBankAccount, GetBankAccount, GetBankAccountResponse, Response}

import scala.concurrent.ExecutionContext



object Main {
  def main(args: Array[String]): Unit = {
    val rootBehaviour: Behavior[NotUsed] = Behaviors.setup{ context =>
      val bank = context.spawn(Bank(), "bank")
      val logger = context.log
      val responseHandler = context.spawn(Behaviors.receiveMessage[Response]{
        case BankAccountCreatedResponse(id) =>
          logger.info(s"successfully created bank account $id")
          Behaviors.same
        case GetBankAccountResponse(mayBeBankAccount) =>
          logger.info(s"Account details: $mayBeBankAccount")
          Behaviors.same
      }, "replyHandler")
      import scala.concurrent.duration._
      implicit val scheduler: Scheduler = context.system.scheduler
      implicit val timeout: Timeout = Timeout(2.seconds)
      implicit val ec: ExecutionContext = context.executionContext

      //bank ! CreateBankAccount("Anish", "rs", 10, responseHandler)
      bank ! GetBankAccount("31487d9a-5c31-448e-adc9-f7c123787f23", responseHandler)
      Behaviors.empty
    }
    val system = ActorSystem(rootBehaviour, "BankDemo")
  }
}