package com.example.pizzaorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thin wrapper around DantSu's ESC/POS library.
 *
 * Strategy: each print = open connection, print, close. Don't hold a long-lived
 * connection. Cheap BT printers are flaky; reconnecting per ticket is more
 * reliable than recovering a broken socket.
 *
 * The printer must already be paired with the tablet via Android Bluetooth
 * settings. selectFirstPaired() then picks it. If you have multiple paired
 * printers, this grabs the first one — fine for the single-printer party setup.
 */
class Printer(private val context: Context) {

    /**
     * @return Result.success on print, Result.failure with a user-facing German
     *         message on any failure (no permission, no printer, BT off, etc.).
     */
    suspend fun printOrder(order: Order): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            check(hasBluetoothPermission()) {
                "Bluetooth-Berechtigung fehlt. In den App-Einstellungen erlauben."
            }

            val connection = BluetoothPrintersConnections.selectFirstPaired()
                ?: error("Kein gepaarter Bluetooth-Drucker gefunden. Erst in Android-Einstellungen koppeln.")

            // Parameters tuned for typical 58mm Chinese ESC/POS printers:
            //   203 dpi, 48mm printable width, 32 chars per line at default font.
            // The "16" in EscPosCharsetEncoding is the printer's codepage table for
            // Windows-1252 (ESC t 16). Most cheap 58mm printers accept this.
            // If umlauts come out garbled on YOUR printer, try 0 (USA/CP437) or 2 (CP850).
            val printer = EscPosPrinter(
                connection,
                203, 48f, 32,
                EscPosCharsetEncoding("windows-1252", 16),
            )
            try {
                printer.printFormattedTextAndCut(formatTicket(order))
            } finally {
                runCatching { printer.disconnectPrinter() }
            }
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Builds the receipt body. DantSu syntax:
     *   [L] [C] [R] alignment per line
     *   <b>bold</b>, <u>underline</u>
     *   <font size='big'|'tall'|'wide'|'normal'>...</font>
     *   \n is mandatory between logical lines
     */
    private fun formatTicket(order: Order): String {
        val time = TIME_FMT.format(Date(order.id))

        val toppingLines = if (order.pizza.toppings.isEmpty()) {
            "[L]  (keine)\n"
        } else {
            order.pizza.toppings.joinToString("") { "[L]  - $it\n" }
        }

        val noteBlock = if (order.note.isBlank()) "" else
            "[L]\n[L]<b>Notiz:</b>\n[L]  ${order.note}\n"

        return """
            [C]<u><font size='big'>PIZZA</font></u>
            [L]
            [C]<font size='big'>${order.customer}</font>
            [L]
            [C]================================
            [L]
            [L]<b><font size='tall'>${order.pizza.name}</font></b>
            [L]
            [L]Sauce: ${order.pizza.sauce}
            [L]Belag:
            $toppingLines
            $noteBlock
            [L]
            [R]$time
            [L]
            [C]================================
        """.trimIndent()
    }

    companion object {
        private val TIME_FMT = SimpleDateFormat("HH:mm", Locale.GERMAN)
    }
}