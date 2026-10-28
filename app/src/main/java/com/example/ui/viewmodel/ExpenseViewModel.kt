package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiExpenseParser
import com.example.data.ai.LocalExpenseParser
import com.example.data.ai.ParsedExpense
import com.example.data.model.Expense
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PeriodFilter(val label: String) {
    TODAY("Hoy"),
    WEEK("Esta semana"),
    MONTH("Este mes"),
    ALL("Todo")
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository(application)
    private val aiParser = AiExpenseParser()

    val rawExpensesState: StateFlow<UiState<List<Expense>>> = repository.observeExpenses()
        .map<List<Expense>, UiState<List<Expense>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar los gastos")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.MONTH)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _defaultCurrency = MutableStateFlow("PEN")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _inputBarText = MutableStateFlow("")
    val inputBarText: StateFlow<String> = _inputBarText.asStateFlow()

    private val _livePrediction = MutableStateFlow<ParsedExpense?>(null)
    val livePrediction: StateFlow<ParsedExpense?> = _livePrediction.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Filtered expenses based on search, period, and category
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        rawExpensesState,
        _searchQuery,
        _selectedPeriod,
        _selectedCategory
    ) { state, query, period, cat ->
        if (state !is UiState.Success) return@combine emptyList()
        val all = state.data

        val now = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val calWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val weekAgoStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calWeekAgo.time)

        val calMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthStartStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calMonthStart.time)

        all.filter { expense ->
            // Period filter
            val matchesPeriod = when (period) {
                PeriodFilter.TODAY -> expense.date == todayStr
                PeriodFilter.WEEK -> expense.date >= weekAgoStr
                PeriodFilter.MONTH -> expense.date >= monthStartStr
                PeriodFilter.ALL -> true
            }

            // Category filter
            val matchesCat = cat == null || expense.category.equals(cat, ignoreCase = true)

            // Search query
            val matchesQuery = query.isBlank() ||
                expense.description.contains(query, ignoreCase = true) ||
                expense.category.contains(query, ignoreCase = true) ||
                expense.paymentMethod.contains(query, ignoreCase = true) ||
                expense.formattedAmount().contains(query, ignoreCase = true)

            matchesPeriod && matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun onInputChanged(text: String) {
        _inputBarText.value = text
        if (text.isNotBlank()) {
            val prediction = LocalExpenseParser.parse(text, _defaultCurrency.value)
            _livePrediction.value = prediction
        } else {
            _livePrediction.value = null
        }
    }

    fun onQuickSuggestionClicked(suggestionText: String) {
        submitExpense(suggestionText)
    }

    fun submitExpense(explicitText: String? = null) {
        val textToSubmit = explicitText ?: _inputBarText.value
        val trimmed = textToSubmit.trim()
        if (trimmed.isBlank() || _isSubmitting.value) return

        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                // 1. Try smart AI parse or fallback to local instant parser
                val parsed = aiParser.parseWithAiOrFallback(trimmed, _defaultCurrency.value)

                val amountToSave = if (parsed.amount > 0) parsed.amount else 1.0
                val dateToSave = if (parsed.date.isNotBlank()) parsed.date else SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val result = repository.addExpense(
                    amount = amountToSave,
                    currency = parsed.currency,
                    category = parsed.category,
                    description = parsed.description,
                    paymentMethod = parsed.paymentMethod,
                    rawInput = trimmed,
                    date = dateToSave
                )

                if (result.isSuccess) {
                    _inputBarText.value = ""
                    _livePrediction.value = null
                    val symbol = if (parsed.currency == "PEN") "S/." else parsed.currency
                    _userMessage.value = "⚡ Registrado: $symbol ${String.format(Locale.US, "%.2f", amountToSave)} (${parsed.description})"
                } else {
                    _userMessage.value = "Error al guardar el gasto."
                }
            } catch (e: Exception) {
                _userMessage.value = "Error: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            val result = repository.updateExpense(expense)
            if (result.isSuccess) {
                _userMessage.value = "Gasto actualizado"
            } else {
                _userMessage.value = "No se pudo actualizar el gasto"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val result = repository.deleteExpense(expenseId)
            if (result.isSuccess) {
                _userMessage.value = "Gasto eliminado"
            } else {
                _userMessage.value = "No se pudo eliminar el gasto"
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPeriodFilter(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setDefaultCurrency(currency: String) {
        _defaultCurrency.value = currency
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
