package com.example.pizzaorder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Theme override. Initialized from the system, then toggled manually.
    // Not persisted — restart returns to system default, by design.
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
                    IconButton(onClick = onOpenSettings) {
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

