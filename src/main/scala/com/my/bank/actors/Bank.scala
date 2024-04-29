package com.my.bank.actors

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.my.bank.events.{BankAccountBalanceUpdatedResponse, Command, CreateBankAccount, GetBankAccount, GetBankAccountResponse, UpdateBalance}

import java.util.UUID


object Bank {
  // commands: Same as Persistent bank account
  // events
  sealed trait Event
  case class BankAccountCreated(id: String) extends Event
  // state
  case class State(accounts: Map[String, ActorRef[Command]])

  // command handler
   def commandHandler(context: ActorContext[Command]): (State, Command) => Effect[Event, State] = (state, command) => {
    command match {
      case createCommand @ CreateBankAccount(_, _, _, _) =>
        val id = UUID.randomUUID().toString
        val newBankAccount = context.spawn(PersistentBankAccount(id), id)
        Effect
          .persist(BankAccountCreated(id))
          .thenReply(newBankAccount)(_ => createCommand)
      case updateCommand @ UpdateBalance(id, _, _, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) => Effect.reply(account)(updateCommand)
          case None => Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(None))
        }
      case getCmd @ GetBankAccount(id, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) => Effect.reply(account)(getCmd)
          case None => Effect.reply(replyTo)(GetBankAccountResponse(None))
        }
    }
  }
  // event handler
  def eventHandler(context: ActorContext[Command]): (State, Event) => State = (state, event) =>
    event match {
      case BankAccountCreated(id) =>
        val account = context.child(id)
         .getOrElse(context.spawn(PersistentBankAccount(id), id))
         .asInstanceOf[ActorRef[Command]]
        state.copy(state.accounts + (id -> account))
    }
  // behaviour
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }
}
