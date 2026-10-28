package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Expense
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ExpenseRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeExpenses(): Flow<List<Expense>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/expenses"
        emitAll(
            db.collection("users").document(uid).collection("expenses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Expense::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addExpense(
        amount: Double,
        currency: String,
        category: String,
        description: String,
        paymentMethod: String,
        rawInput: String,
        date: String
    ): Result<String> {
        val uid = requireUserId()
        val expensesRef = db.collection("users").document(uid).collection("expenses")
        val payload = mapOf(
            "userId" to uid,
            "amount" to amount,
            "currency" to currency,
            "category" to category,
            "description" to description,
            "paymentMethod" to paymentMethod,
            "rawInput" to rawInput,
            "date" to date,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            val docRef = expensesRef.add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, expensesRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        val uid = requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("expenses").document(expense.id)
        val payload = mutableMapOf<String, Any>(
            "amount" to expense.amount,
            "currency" to expense.currency,
            "category" to expense.category,
            "description" to expense.description,
            "paymentMethod" to expense.paymentMethod,
            "rawInput" to expense.rawInput,
            "date" to expense.date,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        val uid = requireUserId()
        require(expenseId.isNotBlank()) { "Expense ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
