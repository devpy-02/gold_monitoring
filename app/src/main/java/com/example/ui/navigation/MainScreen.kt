package com.example.ui.navigation

import android.content.Intent
import android.net.Uri

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.R
import com.example.ui.screens.calculator.CalculatorScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.market.MarketScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.support.SupportScreen
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.SettingsViewModel

enum class MainDestination(
    val titleFa: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    HOME(
        "خانه",
        Icons.Filled.Home,
        Icons.Outlined.Home,
        "nav_home"
    ),

    CALCULATOR(
        "محاسبه",
        Icons.Filled.Calculate,
        Icons.Outlined.Calculate,
        "nav_calculator"
    ),

    MARKET(
        "بازار",
        Icons.Filled.ShowChart,
        Icons.Outlined.ShowChart,
        "nav_market"
    ),

    SETTINGS(
        "تنظیمات",
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        "nav_settings"
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    calculatorViewModel: CalculatorViewModel = viewModel(),
    marketViewModel: MarketViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var currentDestination by remember {
        mutableStateOf(MainDestination.HOME)
    }

    var isViewingHistory by remember {
        mutableStateOf(false)
    }

    var isViewingSupport by remember {
        mutableStateOf(false)
    }

    var isMenuExpanded by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    BackHandler(
        enabled = isViewingSupport ||
            isViewingHistory ||
            currentDestination != MainDestination.HOME
    ) {
        when {
            isViewingSupport -> {
                isViewingSupport = false
            }

            isViewingHistory -> {
                isViewingHistory = false
            }

            else -> {
                currentDestination = MainDestination.HOME
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(
                                id = R.drawable.ic_app_logo
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(
                                    RoundedCornerShape(6.dp)
                                )
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = when {
                                isViewingSupport -> "حمایت مالی"
                                isViewingHistory -> "تاریخچه محاسبات"
                                else -> "زرنگار"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },

                navigationIcon = {
                    if (isViewingHistory || isViewingSupport) {
                        IconButton(
                            onClick = {
                                if (isViewingSupport) {
                                    isViewingSupport = false
                                } else {
                                    isViewingHistory = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    }
                },

                actions = {
                    if (!isViewingHistory && !isViewingSupport) {
                        IconButton(
                            onClick = {
                                isViewingHistory = true
                            },
                            modifier = Modifier.testTag(
                                "topbar_btn_history"
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "سوابق محاسبات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = {
                                isMenuExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "منوی برنامه"
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = {
                                isMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("حمایت مالی ❤️")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    isViewingSupport = true
                                    isViewingHistory = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("ارتباط با ما")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MailOutline,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false

                                    val intent = Intent(
                                        Intent.ACTION_SENDTO
                                    ).apply {
                                        data = Uri.parse(
                                            "mojahed.reza.Ali@gmail.com"
                                        )

                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "ارتباط با توسعه‌دهنده زرنگار"
                                        )
                                    }

                                    if (
                                        intent.resolveActivity(
                                            context.packageManager
                                        ) != null
                                    ) {
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                },

                colors = TopAppBarDefaults
                    .centerAlignedTopAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            )
        },

        bottomBar = {
            if (!isViewingHistory && !isViewingSupport) {
                NavigationBar(
                    containerColor =
                        MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    MainDestination.entries.forEach { destination ->
                        val isSelected =
                            currentDestination == destination

                        NavigationBarItem(
                            selected = isSelected,

                            onClick = {
                                currentDestination = destination
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription =
                                        destination.titleFa
                                )
                            },

                            label = {
                                Text(
                                    text = destination.titleFa,
                                    fontWeight = if (isSelected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            },

                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor =
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor =
                                    MaterialTheme.colorScheme.primary,
                                indicatorColor =
                                    MaterialTheme.colorScheme.primaryContainer
                            ),

                            modifier = Modifier.testTag(
                                destination.testTag
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            if (isViewingSupport) {

                SupportScreen(
                    onBack = {
                        isViewingSupport = false
                    }
                )

            } else if (isViewingHistory) {

                HistoryScreen(
                    viewModel = historyViewModel,

                    onReopenRecord = { record ->
                        calculatorViewModel
                            .loadFromHistoryRecord(record)

                        isViewingHistory = false
                        currentDestination =
                            MainDestination.CALCULATOR
                    }
                )

            } else {

                Crossfade(
                    targetState = currentDestination,
                    label = "screen_crossfade"
                ) { dest ->

                    when (dest) {

                        MainDestination.HOME -> {
                            HomeScreen(
                                marketViewModel = marketViewModel,
                                onNavigateToCalculator = {
                                    currentDestination =
                                        MainDestination.CALCULATOR
                                }
                            )
                        }

                        MainDestination.CALCULATOR -> {
                            CalculatorScreen(
                                viewModel = calculatorViewModel
                            )
                        }

                        MainDestination.MARKET -> {
                            MarketScreen(
                                viewModel = marketViewModel
                            )
                        }

                        MainDestination.SETTINGS -> {
                            SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
