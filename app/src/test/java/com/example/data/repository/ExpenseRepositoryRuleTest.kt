package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpenseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addExpense_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                amount = 18.50,
                currency = "PEN",
                category = "comida",
                description = "Menú con chicha morada",
                paymentMethod = "yape",
                rawInput = "18.50 menú con chicha morada yape",
                date = "2026-10-27"
            )
        }
        assertTrue("Expense creation should succeed", result.isSuccess)
        val expenseId = result.getOrThrow()
        assertNotNull(expenseId)
        assertTrue(expenseId.isNotBlank())
    }

    @Test
    fun observeExpenses_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)
        val expenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                amount = 25.0,
                currency = "PEN",
                category = "transporte",
                description = "Taxi a Miraflores",
                paymentMethod = "efectivo",
                rawInput = "taxi miraflores 25",
                date = "2026-10-27"
            ).getOrThrow()
        }

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses().first { list ->
                list.any { it.id == expenseId }
            }
        }
        assertTrue(emittedList.any { it.id == expenseId })
    }

    @Test
    fun updateExpense_authenticatedOwner_updatesSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)
        val expenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                amount = 10.0,
                currency = "PEN",
                category = "gustitos",
                description = "Café americano",
                paymentMethod = "efectivo",
                rawInput = "café 10",
                date = "2026-10-27"
            ).getOrThrow()
        }

        val updatedExpense = Expense(
            id = expenseId,
            userId = auth.currentUser!!.uid,
            amount = 12.0,
            currency = "PEN",
            category = "gustitos",
            description = "Café con leche",
            paymentMethod = "tarjeta",
            rawInput = "café 12 tarjeta",
            date = "2026-10-27"
        )
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateExpense(updatedExpense)
        }
        assertTrue("Update should succeed", updateResult.isSuccess)
    }

    @Test
    fun deleteExpense_authenticatedOwner_deletesSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ExpenseRepository(firestore)
        val expenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(
                amount = 5.0,
                currency = "PEN",
                category = "transporte",
                description = "Pasaje bus",
                paymentMethod = "efectivo",
                rawInput = "pasaje 5",
                date = "2026-10-27"
            ).getOrThrow()
        }

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteExpense(expenseId)
        }
        assertTrue("Delete should succeed", deleteResult.isSuccess)
    }

    @Test
    fun observeExpenses_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ExpenseRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeExpenses().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            if (firestoreException != null) {
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
            } else {
                // IllegalStateException from requireUserId() before Firestore query
                assertTrue(e is IllegalStateException || e.message?.contains("User must be signed in") == true)
            }
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@gastosfacil.pe"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
