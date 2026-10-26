package com.example.pizzaorder

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

data class AppState(
    val orders: List<Order> = emptyList(),
    val menu: MenuConfig = MenuConfig.Standard.menu,
    val isLoaded: Boolean = false,
    val message: String? = null,
    /** Set together with [message] when the snackbar should offer "Rückgängig" for a just-finished order. */
    val undoDone: Order? = null,
    val printStyle: PrintStyle = PrintStyle.Receipt,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepo = JsonStore(application, "orders.json", ListSerializer(Order.serializer()), emptyList())
    private val menuRepo  = JsonStore(application, "menu.json", MenuConfig.serializer(), MenuConfig.Standard.menu, pretty = true)
    private val printer   = Printer(application)

    private val prefs     = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AppState(
        printStyle = PrintStyle.entries.find { it.name == prefs.getString("printStyle", null) } ?: PrintStyle.Receipt,
    ))
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
        // Fresh random QR link on every print, reprint and test print.
        val link = _state.value.menu.funLinks.randomOrNull()
        printer.printOrder(order, _state.value.printStyle, link)
            .onSuccess { updateMessage(successMsg) }
            .onFailure { e -> updateMessage("Druck fehlgeschlagen: ${e.message}") }
    }

    // ─── Admin lock ─────────────────────────────────────────────────────────

    // Lives in the ViewModel so the wrong-try counter survives rotation.
    val adminLock = AdminLock()

    // ─── Ausdruck ───────────────────────────────────────────────────────────

    fun setPrintStyle(style: PrintStyle) {
        _state.update { it.copy(printStyle = style) }
        prefs.edit { putString("printStyle", style.name) }
    }

    fun testPrint() = runPrint(
        (_state.value.menu.presets.firstOrNull() ?: Pizza("Margherita", MenuConfig.Standard.sauce, linkedSetOf())).let { pizza ->
            Order(number = 42, customer = "Testgast", pizza = pizza, note = "Nur ein Testdruck", diet = _state.value.menu.dietOf(pizza))
        },
        "Testdruck (${_state.value.printStyle.label}) gesendet.",
    )

    // ─── Orders ─────────────────────────────────────────────────────────────

    fun submitOrder(customer: String, pizza: Pizza, note: String) {
        val cleanCustomer = customer.trim()
        if (cleanCustomer.isEmpty()) return updateMessage("Bitte einen Namen eingeben.")

        // ponytail: restarts at #001 after "Alle löschen"; persist a counter if numbers must stay unique all night
        val number = (_state.value.orders.maxOfOrNull { it.number } ?: 0) + 1
        val order = Order(number = number, customer = cleanCustomer, pizza = pizza, note = note.trim(), diet = _state.value.menu.dietOf(pizza))

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

        // A tap is easy to hit by accident: offer undo when an order just became "fertig".
        // Undo is simply toggleDone again, which flips it back without a new message.
        if (_state.value.orders.find { it.id == order.id }?.isDone() == true) {
            _state.update { it.copy(message = "${order.customer}: als fertig markiert.", undoDone = order) }
        }
    }

    fun clearAll() {
        _state.update { it.copy(orders = emptyList(), message = "Alle Bestellungen gelöscht.") }
        viewModelScope.launch { orderRepo.save(emptyList()) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null, undoDone = null) }

    // ─── Menu ───────────────────────────────────────────────────────────────

    fun addSauce(name: String) = name.trim().takeIf { it.isNotEmpty() }?.let { clean ->
        updateMenu { it.withSauce(Sauce(clean)) }
    }

    fun removeSauce(sauce: Sauce) = updateMenu { m ->
        if (m.sauces.size <= 1) { updateMessage("Mindestens eine Sauce muss bleiben."); m }
        else m.withoutSauce(sauce)
    }

    fun addTopping(name: String) = name.trim().takeIf { it.isNotEmpty() }?.let { clean ->
        updateMenu { it.withTopping(Topping(clean)) }
    }

    fun removeTopping(topping: Topping) = updateMenu { it.withoutTopping(topping) }

    fun setDiet(name: String, diet: Diet) = updateMenu { it.withDiet(name, diet) }

    fun addPreset(pizza: Pizza) {
        val clean = pizza.name.trim()
        if (clean.isEmpty()) return
        updateMenu { it.withPreset(pizza.copy(name = clean)) }
    }

    fun removePreset(name: String) = updateMenu { it.withoutPreset(name) }

    fun addFunLink(teaser: String, url: String) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return updateMessage("Link muss mit http:// oder https:// beginnen.")
        }
        updateMenu { it.withFunLink(FunLink(teaser.trim().ifEmpty { "Überraschung!" }, cleanUrl)) }
    }

    fun removeFunLink(link: FunLink) = updateMenu { it.withoutFunLink(link) }

    fun resetMenu() {
        val newMenu = MenuConfig.Standard.menu
        _state.update { it.copy(menu = newMenu, message = "Standardmenü wiederhergestellt.") }
        viewModelScope.launch { menuRepo.save(newMenu) }
    }
}
