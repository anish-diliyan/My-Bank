package com.my.bank.events

import akka.actor.typed.ActorRef

sealed trait Command
case class CreateBankAccount(
                              user: String,
                              currency: String,
                              initialBalance: Double,
                              replyTo: ActorRef[Response]
                            ) extends Command
// id: id of bank account
// amount: this can be both + for deposit and - for withdrawal
case class UpdateBalance(
                          id: String,
                          currency: String,
                          amount: Double,
                          replyTo: ActorRef[Response]
                        ) extends Command
// id: id of bank account
case class GetBankAccount(id: String, replyTo: ActorRef[Response]) extends Command
