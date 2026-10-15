package com.example.ui.screens.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CalculationMode
import com.example.model.GoldPurity
import com.example.model.MetalType
import com.example.model.SilverPurity
import com.example.model.TaxBase
import com.example.ui.components.LuxuryCard
import com.example.ui.components.PersianNumberField
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SilverPrimary
import com.example.ui.viewmodel.CalculatorUiState
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.util.PersianUtils
import kotlinx.coroutines.flow.collectLatest
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Metal Type Tab Switcher (Gold vs Silver)
            MetalTabSwitcher(
                activeMetal = uiState.activeMetal,
                onSelectMetal = { viewModel.setMetal(it) }
            )

            // Result Display Card (Centerpiece)
            ResultCenterpieceCard(
                uiState = uiState,
                onSave = { viewModel.saveCurrentCalculationToHistory() },
                onReset = { viewModel.resetForm() }
            )

            // Mode Toggle (Simple vs Advanced)
            ModeSelectorRow(
                mode = uiState.calculationMode,
                onModeChange = { viewModel.setCalculationMode(it) }
            )

            // Dynamic Inputs based on active metal
            if (uiState.activeMetal == MetalType.GOLD) {
                GoldInputsSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
            } else {
                SilverInputsSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            // Disclaimer Note
            DisclaimerCard()

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun MetalTabSwitcher(
    activeMetal: MetalType,
    onSelectMetal: (MetalType) -> Unit
) {
    val selectedIndex = if (activeMetal == MetalType.GOLD) 0 else 1
    val tabs = listOf("طلا (Gold)", "نقره (Silver)")

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = if (selectedIndex == 0) GoldPrimary else SilverPrimary,
                height = 3.dp
            )
        },
        divider = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("metal_tab_switcher")
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Tab(
                selected = isSelected,
                onClick = { onSelectMetal(if (index == 0) MetalType.GOLD else MetalType.SILVER) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            if (index == 0) GoldPrimary else SilverPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                modifier = Modifier.testTag("tab_metal_$index")
            )
        }
    }
}

@Composable
private fun ModeSelectorRow(
    mode: CalculationMode,
    onModeChange: (CalculationMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "حالت محاسبه:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.testTag("calc_mode_selector")
        ) {
            SegmentedButton(
                selected = mode == CalculationMode.SIMPLE,
                onClick = { onModeChange(CalculationMode.SIMPLE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("ساده")
            }
            SegmentedButton(
                selected = mode == CalculationMode.ADVANCED,
                onClick = { onModeChange(CalculationMode.ADVANCED) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("پیشرفته")
            }
        }
    }
}

@Composable
private fun ResultCenterpieceCard(
    uiState: CalculatorUiState,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    val isGold = uiState.activeMetal == MetalType.GOLD
    val accentColor = if (isGold) GoldPrimary else SilverPrimary

    val totalAmount = if (isGold) {
        uiState.goldResult?.totalAmount ?: BigDecimal.ZERO
    } else {
        uiState.silverResult?.totalAmount ?: BigDecimal.ZERO
    }

    val pureWeight = if (isGold) {
        uiState.goldResult?.pureGoldWeight24k ?: BigDecimal.ZERO
    } else {
        uiState.silverResult?.pureSilverWeight999 ?: BigDecimal.ZERO
    }

    val rawMetalValue = if (isGold) {
        uiState.goldResult?.rawMetalValue ?: BigDecimal.ZERO
    } else {
        uiState.silverResult?.rawMetalValue ?: BigDecimal.ZERO
    }

    LuxuryCard(
        borderColor = accentColor.copy(alpha = 0.5f),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("result_centerpiece_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مبلغ نهایی برآوردی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isGold) "طلای استاندارد" else "نقره استاندارد",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Big Total Price Display
            Text(
                text = PersianUtils.formatMoney(
                    totalAmount,
                    isToman = uiState.isToman,
                    usePersianDigits = uiState.usePersianDigits,
                    includeUnit = true
                ),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.testTag("result_total_price")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Sub-metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isGold) "معادل طلای خالص (۲۴ عیار)" else "معادل نقره خالص (۹۹۹)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = PersianUtils.formatWeight(
                            pureWeight,
                            usePersianDigits = uiState.usePersianDigits,
                            includeUnit = true
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ارزش خام فلز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = PersianUtils.formatMoney(
                            rawMetalValue,
                            isToman = uiState.isToman,
                            usePersianDigits = uiState.usePersianDigits,
                            includeUnit = true
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Advanced Breakdown if mode == ADVANCED
            if (uiState.calculationMode == CalculationMode.ADVANCED) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                AdvancedBreakdownList(uiState = uiState, isGold = isGold)
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_save_calculation")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ذخیره در سوابق", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_reset_calculator")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پاک کردن")
                }
            }
        }
    }
}

@Composable
private fun AdvancedBreakdownList(
    uiState: CalculatorUiState,
    isGold: Boolean
) {
    val ojrat = if (isGold) uiState.goldResult?.ojratAmount else uiState.silverResult?.ojratAmount
    val profit = if (isGold) uiState.goldResult?.profitAmount else uiState.silverResult?.profitAmount
    val tax = if (isGold) uiState.goldResult?.taxAmount else uiState.silverResult?.taxAmount
    val costs = if (isGold) uiState.goldResult?.additionalCosts else uiState.silverResult?.additionalCosts
    val discount = if (isGold) uiState.goldResult?.discount else uiState.silverResult?.discount

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "ریز هزینه‌های قانونی و ساخت:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        BreakdownRow(
            label = "اجرت ساخت:",
            amount = ojrat ?: BigDecimal.ZERO,
            uiState = uiState
        )
        BreakdownRow(
            label = "سود فروشنده:",
            amount = profit ?: BigDecimal.ZERO,
            uiState = uiState
        )
        BreakdownRow(
            label = "مالیات بر ارزش افزوده:",
            amount = tax ?: BigDecimal.ZERO,
            uiState = uiState
        )
        if (costs != null && costs > BigDecimal.ZERO) {
            BreakdownRow(
                label = "هزینه‌های جانبی و متعلقات:",
                amount = costs,
                uiState = uiState
            )
        }
        if (discount != null && discount > BigDecimal.ZERO) {
            BreakdownRow(
                label = "تخفیف:",
                amount = discount.negate(),
                uiState = uiState,
                isNegative = true
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    uiState: CalculatorUiState,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = PersianUtils.formatMoney(
                amount,
                isToman = uiState.isToman,
                usePersianDigits = uiState.usePersianDigits,
                includeUnit = true
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldInputsSection(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel
) {
    LuxuryCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "مشخصات طلا",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Weight Field
            PersianNumberField(
                value = uiState.goldWeightInput,
                onValueChange = { viewModel.setGoldWeight(it) },
                label = "وزن طلا",
                unitSuffix = "گرم",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                testTag = "input_gold_weight"
            )

            // Purity / Karat Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "عیار طلا:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoldPurity.entries.forEach { purity ->
                        val isSelected = uiState.goldPuritySelection == purity
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setGoldPurity(purity) },
                            label = { Text(purity.label) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = GoldPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_karat_${purity.name}")
                        )
                    }
                }

                if (uiState.goldPuritySelection == GoldPurity.CUSTOM) {
                    PersianNumberField(
                        value = uiState.customGoldKaratInput,
                        onValueChange = { viewModel.setCustomGoldKarat(it) },
                        label = "عیار دلخواه (مثلاً ۱۷.۵ یا ۷۵۰)",
                        allowDecimal = true,
                        usePersianDigits = uiState.usePersianDigits,
                        testTag = "input_custom_karat"
                    )
                }
            }

            // Reference Price Field + Live market copy button
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PersianNumberField(
                    value = uiState.goldRefPriceInput,
                    onValueChange = { viewModel.setGoldRefPrice(it) },
                    label = "قیمت پایه طلا (${if (uiState.goldRefBasisKarat == 24.0) "۲۴ عیار" else "۱۸ عیار"})",
                    unitSuffix = if (uiState.isToman) "تومان" else "ریال",
                    allowDecimal = false,
                    usePersianDigits = uiState.usePersianDigits,
                    testTag = "input_gold_ref_price"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ref Basis Toggle (18K vs 24K)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مبنا: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = uiState.goldRefBasisKarat == 18.0,
                            onClick = { viewModel.setGoldRefBasis(18.0) },
                            label = { Text("۱۸ عیار") },
                            shape = RoundedCornerShape(6.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = uiState.goldRefBasisKarat == 24.0,
                            onClick = { viewModel.setGoldRefBasis(24.0) },
                            label = { Text("۲۴ عیار") },
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    // Copy Live Price
                    OutlinedButton(
                        onClick = { viewModel.applyLiveMarketPrice() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_copy_live_gold_price")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قیمت روز بازار", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Advanced Fields if ADVANCED mode
            if (uiState.calculationMode == CalculationMode.ADVANCED) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                AdvancedGoldInputs(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedGoldInputs(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "تنظیمات اجرت، سود و مالیات",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Ojrat (Making Charge) Field + Type switcher (% vs fixed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersianNumberField(
                value = uiState.goldOjratInput,
                onValueChange = { viewModel.setGoldOjrat(it) },
                label = if (uiState.goldIsOjratPercent) "درصد اجرت ساخت" else "اجرت ثابت هر گرم",
                unitSuffix = if (uiState.goldIsOjratPercent) "٪" else "تومان",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_gold_ojrat"
            )

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = uiState.goldIsOjratPercent,
                    onClick = { viewModel.setGoldIsOjratPercent(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("درصدی")
                }
                SegmentedButton(
                    selected = !uiState.goldIsOjratPercent,
                    onClick = { viewModel.setGoldIsOjratPercent(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("ثابت")
                }
            }
        }

        // Profit & Tax Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersianNumberField(
                value = uiState.goldProfitInput,
                onValueChange = { viewModel.setGoldProfit(it) },
                label = "سود فروشنده",
                unitSuffix = "٪",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_gold_profit"
            )

            PersianNumberField(
                value = uiState.goldTaxInput,
                onValueChange = { viewModel.setGoldTax(it) },
                label = "مالیات (VAT)",
                unitSuffix = "٪",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_gold_tax"
            )
        }

        // Tax Base Dropdown
        TaxBaseDropdown(
            currentBase = uiState.goldTaxBase,
            onBaseSelected = { viewModel.setGoldTaxBase(it) }
        )

        // Additional Costs & Discount Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersianNumberField(
                value = uiState.goldAdditionalCostsInput,
                onValueChange = { viewModel.setGoldAdditionalCosts(it) },
                label = "هزینه‌های جانبی",
                unitSuffix = if (uiState.isToman) "تومان" else "ریال",
                allowDecimal = false,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_gold_additional_costs"
            )

            PersianNumberField(
                value = uiState.goldDiscountInput,
                onValueChange = { viewModel.setGoldDiscount(it) },
                label = "تخفیف",
                unitSuffix = if (uiState.isToman) "تومان" else "ریال",
                allowDecimal = false,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_gold_discount"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxBaseDropdown(
    currentBase: TaxBase,
    onBaseSelected: (TaxBase) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentBase.titlePersian,
            onValueChange = {},
            readOnly = true,
            label = { Text("مبنای محاسبه مالیات") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaxBase.entries.forEach { base ->
                DropdownMenuItem(
                    text = { Text(base.titlePersian) },
                    onClick = {
                        onBaseSelected(base)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SilverInputsSection(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel
) {
    LuxuryCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "مشخصات نقره",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Weight Field + Unit Toggle (Gram vs Troy Ounce)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersianNumberField(
                    value = uiState.silverWeightInput,
                    onValueChange = { viewModel.setSilverWeight(it) },
                    label = "وزن نقره",
                    unitSuffix = if (uiState.silverIsTroyOunce) "اونس" else "گرم",
                    allowDecimal = true,
                    usePersianDigits = uiState.usePersianDigits,
                    modifier = Modifier.weight(1f),
                    testTag = "input_silver_weight"
                )

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !uiState.silverIsTroyOunce,
                        onClick = { viewModel.setSilverIsTroyOunce(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("گرم")
                    }
                    SegmentedButton(
                        selected = uiState.silverIsTroyOunce,
                        onClick = { viewModel.setSilverIsTroyOunce(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("اونس")
                    }
                }
            }

            // Fineness Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "عیار نقره (خلوص در هزار):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SilverPurity.entries.forEach { purity ->
                        val isSelected = uiState.silverPuritySelection == purity
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSilverPurity(purity) },
                            label = { Text(purity.label) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SilverPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SilverPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_silver_fineness_${purity.name}")
                        )
                    }
                }

                if (uiState.silverPuritySelection == SilverPurity.CUSTOM) {
                    PersianNumberField(
                        value = uiState.customSilverFinenessInput,
                        onValueChange = { viewModel.setCustomSilverFineness(it) },
                        label = "خلوص دلخواه (مثلاً ۸۴۰ یا ۹۰۰)",
                        allowDecimal = false,
                        usePersianDigits = uiState.usePersianDigits,
                        testTag = "input_custom_silver_fineness"
                    )
                }
            }

            // Reference Silver Price
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PersianNumberField(
                    value = uiState.silverRefPriceInput,
                    onValueChange = { viewModel.setSilverRefPrice(it) },
                    label = "قیمت پایه نقره خالص (۹۹۹)",
                    unitSuffix = if (uiState.isToman) "تومان" else "ریال",
                    allowDecimal = false,
                    usePersianDigits = uiState.usePersianDigits,
                    testTag = "input_silver_ref_price"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { viewModel.applyLiveMarketPrice() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_copy_live_silver_price")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قیمت روز نقره", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Advanced Fields if ADVANCED mode
            if (uiState.calculationMode == CalculationMode.ADVANCED) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                AdvancedSilverInputs(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AdvancedSilverInputs(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "تنظیمات اجرت و سود نقره",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersianNumberField(
                value = uiState.silverOjratInput,
                onValueChange = { viewModel.setSilverOjrat(it) },
                label = if (uiState.silverIsOjratPercent) "درصد اجرت ساخت" else "اجرت هر گرم",
                unitSuffix = if (uiState.silverIsOjratPercent) "٪" else "تومان",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_silver_ojrat"
            )

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = uiState.silverIsOjratPercent,
                    onClick = { viewModel.setSilverIsOjratPercent(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("درصدی")
                }
                SegmentedButton(
                    selected = !uiState.silverIsOjratPercent,
                    onClick = { viewModel.setSilverIsOjratPercent(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("ثابت")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersianNumberField(
                value = uiState.silverProfitInput,
                onValueChange = { viewModel.setSilverProfit(it) },
                label = "سود فروشنده",
                unitSuffix = "٪",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_silver_profit"
            )

            PersianNumberField(
                value = uiState.silverTaxInput,
                onValueChange = { viewModel.setSilverTax(it) },
                label = "مالیات (VAT)",
                unitSuffix = "٪",
                allowDecimal = true,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_silver_tax"
            )
        }

        TaxBaseDropdown(
            currentBase = uiState.silverTaxBase,
            onBaseSelected = { viewModel.setSilverTaxBase(it) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersianNumberField(
                value = uiState.silverAdditionalCostsInput,
                onValueChange = { viewModel.setSilverAdditionalCosts(it) },
                label = "هزینه‌های جانبی",
                unitSuffix = if (uiState.isToman) "تومان" else "ریال",
                allowDecimal = false,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_silver_additional_costs"
            )

            PersianNumberField(
                value = uiState.silverDiscountInput,
                onValueChange = { viewModel.setSilverDiscount(it) },
                label = "تخفیف",
                unitSuffix = if (uiState.isToman) "تومان" else "ریال",
                allowDecimal = false,
                usePersianDigits = uiState.usePersianDigits,
                modifier = Modifier.weight(1f),
                testTag = "input_silver_discount"
            )
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "سلب مسئولیت: تمامی مبالغ محاسبه شده بر اساس فرمول‌های رسمی صنف طلا و جواهر برآورد شده‌اند و ممکن است قیمت نهایی معامله در طلافروشی‌ها با توجه به عیار دقیق‌سنجی یا تخفیف ویژه فروشنده اندکی تفاوت داشته باشد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
