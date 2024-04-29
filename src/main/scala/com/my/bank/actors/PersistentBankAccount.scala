package com.my.bank.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.Effect
import com.my.bank.events.{BalanceUpdated, BankAccount, BankAccountBalanceUpdatedResponse, BankAccountCreated, BankAccountCreatedResponse, Command, CreateBankAccount, Event, GetBankAccount, GetBankAccountResponse, UpdateBalance}

/*
 * denote a lowest in hierarchy: A Single Bank BankAccount
 * Will receive some messages and store events in cassandra
 * Event Sourcing: Unlike traditional databases where we store latest state of the data to
 * the database, In Event sourcing we store events that is bits of data which comprise
 * journey to latest state of the data at this moment.
 * So we obtain the latest state of the data by replaying all the events one by one.
 * Fault tolerance and Auditing are two reasons to use Event Sourcing.
 * */
/*
 * Make PersistentBankAccount class A PersistentActor which implement the event sourcing
 * technique out of the box.
 * Persistent Actor in Akka need to handle bunch of things
 * 1. commands:- these are the messages
 * 2. events:- store events to the database
 * 3. state:- internal state of the bank account
 * 4. responses:- that we want to send back to whoever query or want to
 *    modify the bank account
 * */
object PersistentBankAccount {
  // Now we have All: command, event, state, Response. Now create persistent bank account actor
  // persistent actor has:
  // 1. command handler: message handler => when receive message/command this will persist an event,
  //    and the event after being persisted to the persistence store (cassandra) will be
  //    subject to the event handler
  // 2. event handler: this will usually update state, and the updated state will be then used on the
  //    next command that will be received by persistent actor.

  // 1. create command handler: This is function from (state, command) => Produce an Effect[event, state]
  // 2. create a event handler: This is function from (state, event) => produce an updatedState

  val commandHandler: (BankAccount, Command) => Effect[Event, BankAccount] = { (state, command) =>
    // bank creates me
    command match {
      // bank send me CreateBankAccount
      // I persist BankAccountCreated
      // I update my state
      // reply back to bank with BankAccountCreatedResponse
      // the bank will will send the response to the http server
      case CreateBankAccount(user, currency, initialBalance, bank) =>
        val id = state.id
        Effect
          .persist(BankAccountCreated(BankAccount(id, user, currency, initialBalance)))
          .thenReply(bank)(_ => BankAccountCreatedResponse(id))
      case UpdateBalance(_, _, amount, bank) =>
        val newBalance = state.balance + amount
        if(newBalance < 0) // can not withdraw
          Effect.reply(bank)(BankAccountBalanceUpdatedResponse(None))
        else
          Effect
            .persist(BalanceUpdated(newBalance))
            .thenReply(bank)(newState => BankAccountBalanceUpdatedResponse(Some(newState)))
      case GetBankAccount(_, replyTo) =>
        Effect.reply(replyTo)(GetBankAccountResponse(Some(state)))
    }
  }
  val eventHandler: (BankAccount, Event) => BankAccount = (state, event) => {
    event match {
      case BankAccountCreated(account) =>
        account
      case BalanceUpdated(amount) =>
        state.copy(balance = state.balance + amount)
    }
  }

  def apply(id: String): Behavior[Command] = EventSourcedBehavior[Command, Event, BankAccount](
    persistenceId = PersistenceId.ofUniqueId(id),
    emptyState = BankAccount(id, "", "", 0.0), // unused but required
    commandHandler = commandHandler, eventHandler = eventHandler
  )
}
