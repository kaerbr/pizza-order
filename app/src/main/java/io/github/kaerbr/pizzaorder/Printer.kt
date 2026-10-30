package io.github.kaerbr.pizzaorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around DantSu's ESC/POS library.
 *
 * Strategy: each print = open connection, print, close. Don't hold a long-lived
 * connection. Cheap BT printers are flaky; reconnecting per printout is more
 * reliable than recovering a broken socket.
 *
 * The printer must already be paired with the tablet via Android Bluetooth
 * settings. selectFirstPaired() then picks it. If you have multiple paired
 * printers, this grabs the first one — fine for the single-printer party setup.
 */
class Printer(private val context: Context) {

    // One printout at a time: an order plus a quick reprint would otherwise open two
    // Bluetooth connections at once and one of them fails.
    private val mutex = Mutex()

    /**
     * @return Result.success on print, Result.failure with a user-facing German
     *         message on any failure (no permission, no printer, BT off, etc.).
     */
    suspend fun print(markup: String): Result<Unit> = mutex.withLock { withContext(Dispatchers.IO) {
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
                203, Printout.PAPER_MM, Printout.LINE_CHARS,
                EscPosCharsetEncoding("windows-1252", 16),
            )
            try {
                printer.printFormattedTextAndCut(markup)
            } finally {
                runCatching { printer.disconnectPrinter() }
            }
        }
    } }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }
}
