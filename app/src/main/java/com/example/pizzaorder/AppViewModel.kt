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

    // ─── Orders ─────────────────────────────────────────────────────────────

    fun submitOrder(customer: String, pizza: Pizza, note: String) {
        val cleanCustomer = customer.trim()
        if (cleanCustomer.isEmpty()) {
            _state.update { it.copy(message = "Bitte einen Namen eingeben.") }
            return
        }

        val order = Order(
            customer = cleanCustomer,
            pizza = pizza,
            note = note.trim(),
        )

        // Atomic update: Add order and show immediate feedback
        _state.update { it.copy(
            orders = listOf(order) + it.orders,
            message = "Bestellung für $cleanCustomer aufgenommen..."
        ) }

        viewModelScope.launch {
            orderRepo.save(_state.value.orders)
            printer.printOrder(order)
                .onSuccess { 
                    _state.update { it.copy(message = "Bestellung für $cleanCustomer gedruckt.") } 
                }
                .onFailure { e -> 
                    _state.update { it.copy(message = "Druck fehlgeschlagen: ${e.message}") } 
                }
        }
    }

    fun reprint(order: Order) {
        viewModelScope.launch {
            printer.printOrder(order)
                .onSuccess { _state.update { it.copy(message = "Erneut gedruckt: ${order.customer}.") } }
                .onFailure { e -> _state.update { it.copy(message = "Druck fehlgeschlagen: ${e.message}") } }
        }
    }

    /**
     * Flip an order's done state. Persists immediately. No snackbar — the
     * card visibly moves between sections, that's feedback enough.
     */
    fun toggleDone(order: Order) {
        _state.update { currentState ->
            val newList = currentState.orders.map {
                if (it.id == order.id) {
                    val doneNewValue = if(it.isDone()) null else System.currentTimeMillis()
                    it.copy(completedAt = doneNewValue)
                } else {
                    it
                }
            }
            currentState.copy(orders = newList)
        }
        
        viewModelScope.launch {
            orderRepo.save(_state.value.orders)
        }
    }

    fun clearAll() {
        _state.update { it.copy(orders = emptyList(), message = "Alle Bestellungen gelöscht.") }
        viewModelScope.launch {
            orderRepo.save(emptyList())
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    // ─── Menu ───────────────────────────────────────────────────────────────

    fun addSauce(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        
        var newMenu: MenuConfig? = null
        _state.update { currentState ->
            if (clean in currentState.menu.sauces) {
                currentState.copy(message = "Sauce $clean gibt es schon.")
            } else {
                newMenu = currentState.menu.copy(sauces = currentState.menu.sauces + clean)
                currentState.copy(menu = newMenu)
            }
        }
        newMenu?.let { saveMenuBackground(it) }
    }

    fun removeSauce(name: String) {
        var newMenu: MenuConfig? = null
        _state.update { currentState ->
            val currentMenu = currentState.menu
            val newSauces = currentMenu.sauces - name
            if (newSauces.isEmpty()) {
                currentState.copy(message = "Mindestens eine Sauce muss bleiben.")
            } else {
                val fallback = newSauces.first()
                val newPresets = currentMenu.presets.map {
                    if (it.sauce == name) it.copy(sauce = fallback) else it
                }
                newMenu = currentMenu.copy(sauces = newSauces, presets = newPresets)
                currentState.copy(menu = newMenu)
            }
        }
        newMenu?.let { saveMenuBackground(it) }
    }

    fun addTopping(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        
        var newMenu: MenuConfig? = null
        _state.update { currentState ->
            if (clean in currentState.menu.toppings) {
                currentState.copy(message = "Belag $clean gibt es schon.")
            } else {
                newMenu = currentState.menu.copy(toppings = currentState.menu.toppings + clean)
                currentState.copy(menu = newMenu)
            }
        }
        newMenu?.let { saveMenuBackground(it) }
    }

    fun removeTopping(name: String) {
        var newMenu: MenuConfig? = null
        _state.update { currentState ->
            val currentMenu = currentState.menu
            val newPresets = currentMenu.presets.map { it.copy(toppings = it.toppings - name) }
            newMenu = currentMenu.copy(
                toppings = currentMenu.toppings - name,
                presets = newPresets,
            )
            currentState.copy(menu = newMenu)
        }
        newMenu?.let { saveMenuBackground(it) }
    }

    fun addPreset(pizza: Pizza) {
        val clean = pizza.copy(name = pizza.name.trim())
        if (clean.name.isEmpty()) return
        
        var newMenu: MenuConfig? = null
        _state.update { currentState ->
            if (currentState.menu.presets.any { it.name.equals(clean.name, ignoreCase = true) }) {
                currentState.copy(message = "Pizza ${clean.name} gibt es schon.")
            } else {
                newMenu = currentState.menu.copy(presets = currentState.menu.presets + clean)
                currentState.copy(menu = newMenu)
            }
        }
        newMenu?.let { saveMenuBackground(it) }
    }

    fun removePreset(name: String) {
        var newMenu: MenuConfig? = null
        _state.update { currentState ->
            newMenu = currentState.menu.copy(presets = currentState.menu.presets.filter { it.name != name })
            currentState.copy(menu = newMenu)
        }
        newMenu?.let { saveMenuBackground(it) }
    }

    fun resetMenu() {
        val newMenu = MenuConfig.DEFAULT
        _state.update { it.copy(menu = newMenu, message = "Standardmenü wiederhergestellt.") }
        saveMenuBackground(newMenu)
    }

    private fun saveMenuBackground(new: MenuConfig) {
        viewModelScope.launch { menuRepo.save(new) }
    }
}
