package com.example.pizzaorder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer

data class AppState(
    val orders: List<Order> = emptyList(),
    val menu: MenuConfig = MenuConfig.DEFAULT,
    val isLoaded: Boolean = false,
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepo = JsonStore(application, "orders.json", ListSerializer(Order.serializer()), emptyList())
    private val menuRepo  = JsonStore(application, "menu.json", MenuConfig.serializer(), MenuConfig.DEFAULT, pretty = true)
    private val printer   = Printer(application)

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val orders = orderRepo.load()
            val menu = menuRepo.load()
            _state.update { it.copy(orders = orders, menu = menu, isLoaded = true) }
        }
    }

    private fun updateMessage(msg: String?) = _state.update { it.copy(message = msg) }

    private fun updateMenu(transform: (MenuConfig) -> MenuConfig) {
        val next = transform(_state.value.menu)
        if (next != _state.value.menu) {
            _state.update { it.copy(menu = next) }
            viewModelScope.launch { menuRepo.save(next) }
        }
    }

    private fun runPrint(order: Order, successMsg: String) = viewModelScope.launch {
        printer.printOrder(order)
            .onSuccess { updateMessage(successMsg) }
            .onFailure { e -> updateMessage("Druck fehlgeschlagen: ${e.message}") }
    }

    // ─── Orders ─────────────────────────────────────────────────────────────

    fun submitOrder(customer: String, pizza: Pizza, note: String) {
        val cleanCustomer = customer.trim()
        if (cleanCustomer.isEmpty()) return updateMessage("Bitte einen Namen eingeben.")

        val order = Order(customer = cleanCustomer, pizza = pizza, note = note.trim())

        _state.update { it.copy(
            orders = listOf(order) + it.orders,
            message = "Bestellung für $cleanCustomer aufgenommen..."
        ) }

        viewModelScope.launch { orderRepo.save(_state.value.orders) }
        runPrint(order, "Bestellung für $cleanCustomer gedruckt.")
    }

    fun reprint(order: Order) = runPrint(order, "Erneut gedruckt: ${order.customer}.")

    fun toggleDone(order: Order) {
        _state.update { s ->
            s.copy(orders = s.orders.map {
                if (it.id == order.id) it.copy(completedAt = if (it.isDone()) null else System.currentTimeMillis())
                else it
            })
        }
        viewModelScope.launch { orderRepo.save(_state.value.orders) }
    }

    fun clearAll() {
        _state.update { it.copy(orders = emptyList(), message = "Alle Bestellungen gelöscht.") }
        viewModelScope.launch { orderRepo.save(emptyList()) }
    }

    fun dismissMessage() = updateMessage(null)

    // ─── Menu ───────────────────────────────────────────────────────────────

    fun addSauce(name: String) = name.trim().takeIf { it.isNotEmpty() }?.let { clean ->
        if (clean in _state.value.menu.sauces) updateMessage("Sauce $clean gibt es schon.")
        else updateMenu { it.withSauce(clean) }
    }

    fun removeSauce(name: String) = updateMenu { m ->
        if (m.sauces.size <= 1) { updateMessage("Mindestens eine Sauce muss bleiben."); m }
        else m.withoutSauce(name)
    }

    fun addTopping(name: String) = name.trim().takeIf { it.isNotEmpty() }?.let { clean ->
        if (clean in _state.value.menu.toppings) updateMessage("Belag $clean gibt es schon.")
        else updateMenu { it.withTopping(clean) }
    }

    fun removeTopping(name: String) = updateMenu { it.withoutTopping(name) }

    fun addPreset(pizza: Pizza) {
        val clean = pizza.name.trim()
        if (clean.isEmpty()) return
        if (_state.value.menu.presets.any { it.name.equals(clean, ignoreCase = true) }) {
            updateMessage("Pizza $clean gibt es schon.")
        } else updateMenu { it.withPreset(pizza.copy(name = clean)) }
    }

    fun removePreset(name: String) = updateMenu { it.withoutPreset(name) }

    fun resetMenu() {
        val newMenu = MenuConfig.DEFAULT
        _state.update { it.copy(menu = newMenu, message = "Standardmenü wiederhergestellt.") }
        viewModelScope.launch { menuRepo.save(newMenu) }
    }
}
