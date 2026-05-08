package com.example.pizzaorder

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pizzaorder.ui.OrderScreen
import com.example.pizzaorder.ui.OrdersListScreen
import com.example.pizzaorder.ui.SettingsScreen
import com.example.pizzaorder.ui.theme.PizzaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(vm)
        }
    }
}

@Composable
fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val systemDark = isSystemInDarkTheme()
    var darkTheme by remember(systemDark) { mutableStateOf(systemDark) }

    val btLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* handled at print time */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            btLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                ),
            )
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbar.showSnackbar(msg)
            vm.dismissMessage()
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    var showSettings by rememberSaveable { mutableStateOf(false) }

    PizzaTheme(darkTheme = darkTheme) {
        if (showSettings) {
            BackHandler { showSettings = false }
            SettingsScaffold(
                vm = vm,
                state = state,
                snackbar = snackbar,
                onClose = { showSettings = false },
            )
        } else {
            MainScaffold(
                state = state,
                snackbar = snackbar,
                pagerState = pagerState,
                onOpenSettings = { showSettings = true },
                onToggleTheme = { darkTheme = !darkTheme },
                isDark = darkTheme,
                vm = vm,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    state: AppState,
    snackbar: SnackbarHostState,
    pagerState: PagerState,
    onOpenSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    isDark: Boolean,
    vm: AppViewModel,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAuth by remember { mutableStateOf(false) }
    var authInput by remember { mutableStateOf("") }

    if (showAuth) {
        AlertDialog(
            onDismissRequest = { showAuth = false; authInput = "" },
            title = { Text("Passwort erforderlich") },
            text = {
                OutlinedTextField(
                    value = authInput,
                    onValueChange = { authInput = it },
                    label = { Text("Passwort") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (authInput == "1234") {
                        showAuth = false; authInput = ""; onOpenSettings()
                    } else {
                        Toast.makeText(context, "Falsches Passwort", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showAuth = false; authInput = "" }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("🍕  Pizza Party", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                            contentDescription = if (isDark) "Lightmode" else "Darkmode",
                        )
                    }
                    IconButton(onClick = { showAuth = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Einstellungen",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Tab counter shows pending orders only — that's the actionable
            // queue. Done orders are still in the list but already handled.
            val pendingCount = state.orders.count { !it.isDone() }

            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Bestellen", fontWeight = FontWeight.SemiBold) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = {
                        Text(
                            "Bestellungen ($pendingCount)",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> OrderScreen(
                        menu = state.menu,
                        onSubmit = vm::submitOrder,
                    )
                    else -> OrdersListScreen(
                        orders = state.orders,
                        onToggleDone = vm::toggleDone,
                        onReprint = vm::reprint,
                        onClearAll = vm::clearAll,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    vm: AppViewModel,
    state: AppState,
    snackbar: SnackbarHostState,
    onClose: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        SettingsScreen(
            menu = state.menu,
            onAddSauce      = vm::addSauce,
            onRemoveSauce   = vm::removeSauce,
            onAddTopping    = vm::addTopping,
            onRemoveTopping = vm::removeTopping,
            onAddPreset     = vm::addPreset,
            onRemovePreset  = vm::removePreset,
            onResetMenu     = vm::resetMenu,
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }
}
