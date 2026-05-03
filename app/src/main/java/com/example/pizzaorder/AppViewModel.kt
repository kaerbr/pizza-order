package com.example.pizzaorder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppState(
    val orders: List<Order> = emptyList(),
    val menu: MenuConfig = MenuConfig.DEFAULT,
    val isLoaded: Boolean = false,
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepo = OrderRepo(application)
    private val menuRepo  = MenuRepo(application)
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

    // ─── Orders ─────────────────────────────────────────────────────────────

    fun submitOrder(customer: String, pizza: Pizza, note: String) {
        val cleanCustomer = customer.trim()
        if (cleanCustomer.isEmpty()) return updateMessage("Bitte einen Namen eingeben.")

        val order = Order(customer = cleanCustomer, pizza = pizza, note = note.trim())

        _state.update { it.copy(
            orders = listOf(order) + it.orders,
            message = "Bestellung für $cleanCustomer aufgenommen..."
        ) }

        viewModelScope.launch {
            orderRepo.save(_state.value.orders)
            printer.printOrder(order)
                .onSuccess { updateMessage("Bestellung für $cleanCustomer gedruckt.") }
                .onFailure { e -> updateMessage("Druck fehlgeschlagen: ${e.message}") }
        }
    }

    fun reprint(order: Order) = viewModelScope.launch {
        printer.printOrder(order)
            .onSuccess { updateMessage("Erneut gedruckt: ${order.customer}.") }
            .onFailure { e -> updateMessage("Druck fehlgeschlagen: ${e.message}") }
    }

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
        else updateMenu { it.copy(sauces = it.sauces + clean) }
    }

    fun removeSauce(name: String) = updateMenu { m ->
        val nextSauces = m.sauces - name
        if (nextSauces.isEmpty()) {
            updateMessage("Mindestens eine Sauce muss bleiben.")
            m
        } else {
            val fallback = nextSauces.first()
            m.copy(
                sauces = nextSauces,
                presets = m.presets.map { if (it.sauce == name) it.copy(sauce = fallback) else it }
            )
        }
    }

    fun addTopping(name: String) = name.trim().takeIf { it.isNotEmpty() }?.let { clean ->
        if (clean in _state.value.menu.toppings) updateMessage("Belag $clean gibt es schon.")
        else updateMenu { it.copy(toppings = it.toppings + clean) }
    }

    fun removeTopping(name: String) = updateMenu { m ->
        m.copy(
            toppings = m.toppings - name,
            presets = m.presets.map { it.copy(toppings = it.toppings - name) }
        )
    }

    fun addPreset(pizza: Pizza) = pizza.copy(name = pizza.name.trim()).takeIf { it.name.isNotEmpty() }?.let { clean ->
        if (_state.value.menu.presets.any { it.name.equals(clean.name, ignoreCase = true) }) {
            updateMessage("Pizza ${clean.name} gibt es schon.")
        } else updateMenu { it.copy(presets = it.presets + clean) }
    }

    fun removePreset(name: String) = updateMenu { it.copy(presets = it.presets.filter { p -> p.name != name }) }

    fun resetMenu() {
        val newMenu = MenuConfig.DEFAULT
        _state.update { it.copy(menu = newMenu, message = "Standardmenü wiederhergestellt.") }
        viewModelScope.launch { menuRepo.save(newMenu) }
    }
}
