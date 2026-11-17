package com.plcoding.spendlesssample.transactions.domain.model

/**
 * Unvalidated user input from the create sheet. `TransactionValidator` turns it into a
 * [Transaction] (contracts/domain-interfaces.md).
 */
data class TransactionDraft(
    val type: TransactionType,
    val name: String,
    val amountMinor: Long,
    val note: String?,
    val category: ExpenseCategory?,
    val repeat: RepeatFrequency,
)
