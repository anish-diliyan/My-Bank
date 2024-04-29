package com.my.bank.events

import com.my.bank.events.BankAccount

sealed trait Event
case class BankAccountCreated(account: BankAccount) extends Event
case class BalanceUpdated(amount: Double) extends Event

