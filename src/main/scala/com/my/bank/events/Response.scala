package com.my.bank.events

import com.my.bank.events.BankAccount

sealed trait Response
case class BankAccountCreatedResponse(id: String) extends Response
// using option if somebody try to update a balance of non existing bank account.
case class BankAccountBalanceUpdatedResponse(mayBeBankAccount: Option[BankAccount]) extends Response
case class GetBankAccountResponse(mayBeBankAccount: Option[BankAccount]) extends Response
