package com.my.bank.http


import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import com.my.bank.events.{BankAccountBalanceUpdatedResponse, BankAccountCreatedResponse, Command, CreateBankAccount, GetBankAccount, GetBankAccountResponse, Response, UpdateBalance}
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future


case class BankAccountCreationRequest(user: String, currency: String, balance: Double){
  def toCommand(replyTo: ActorRef[Response]): Command = {
    CreateBankAccount(user, currency, balance, replyTo)
  }
}

case class BankAccountUpdateRequest(currency: String, amount: Double) {
  def toCommand(id: String, replyTo: ActorRef[Response]): Command = {
    UpdateBalance(id, currency, amount, replyTo)
  }
}

case class FailureResponse(reason: String)

class BankRouter(bank: ActorRef[Command])(implicit system: ActorSystem[_]){
  implicit val timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreationRequest): Future[Response] = {
    bank.ask(replyTo => request.toCommand(replyTo))
  }
  def getBankAccount(id: String): Future[Response] = {
    bank.ask(replyTo => GetBankAccount(id, replyTo))
  }
  def updateBankAccount(id: String, request: BankAccountUpdateRequest): Future[Response] = {
    bank.ask(replyTo => request.toCommand(id, replyTo))
  }
  /*
   * POST /bank
   * payload: Bank account creation request as json
   * Response:
   *   201: CREATED
   *   Location: /bank/uuid
   */
  val routes = pathPrefix("bank"){
    pathEndOrSingleSlash {
      post {
        // parse the payload
        entity(as[BankAccountCreationRequest]) { request =>
          // convert the request into command for the bank actor
          // send the command to the bank
          // expect a reply
          // send back an http response
          onSuccess(createBankAccount(request)){
            case BankAccountCreatedResponse(id) =>
              respondWithHeader(Location(s"/bank/$id")){
                complete(StatusCodes.Created)
              }
          }
        }
      }
    } ~ path(Segment) { id =>
      // send command to the bank
      // expect a response
      // send back the Http Response
      get {
        onSuccess(getBankAccount(id)){
          case GetBankAccountResponse(Some(account)) => complete(account)
          case GetBankAccountResponse(None) =>
            complete(StatusCodes.NotFound, FailureResponse(s"Bank account $id can not be found!"))
        }
      } ~ put {
        entity(as[BankAccountUpdateRequest]) { request =>
          // TODO Validate the request
          onSuccess(updateBankAccount(id, request)) {
            case BankAccountBalanceUpdatedResponse(Some(account)) => complete(account)
            case BankAccountBalanceUpdatedResponse(None) =>
              complete(StatusCodes.NotFound, FailureResponse(s"Bank account $id can not be found!"))
          }
        }
      }
    }
  }
}
