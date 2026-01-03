package com.example.elpriscompose.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.example.elpriscompose.notification.NotificationHelper
import com.example.elpriscompose.notification.PriceCheckWorker
import com.example.elpriscompose.ui.home.HomeViewModel
import com.example.elpriscompose.ui.theme.BackgroundCard
import com.example.elpriscompose.ui.theme.BackgroundPrimary
import com.example.elpriscompose.ui.theme.BackgroundSecondary
import com.example.elpriscompose.ui.theme.BrandBlue
import com.example.elpriscompose.ui.theme.StatGreen
import com.example.elpriscompose.ui.theme.StatRed
import com.example.elpriscompose.ui.theme.TextPrimary
import com.example.elpriscompose.ui.theme.TextSecondary
import com.example.elpriscompose.ui.theme.TextTertiary

@Composable
fun SettingsScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val settings by viewModel.alertSettings.collectAsState()
    val currentRegion by viewModel.region.collectAsState()

    // Check if notification permission is granted (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Track if we've already tried requesting permission
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // Re-check permission when screen is resumed (e.g., returning from settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                val newPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                hasNotificationPermission = newPermissionState
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        hasRequestedPermission = true
        if (isGranted && settings.enabled) {
            PriceCheckWorker.schedule(context)
        }
    }

    // Function to open notification settings directly
    fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    // Function to request notification permission
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                if (hasRequestedPermission) {
                    // Already asked once, open settings instead
                    openNotificationSettings()
                } else {
                    // First time asking, use the permission launcher
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // Handle worker scheduling when settings change
    LaunchedEffect(settings.enabled, hasNotificationPermission) {
        if (settings.enabled && hasNotificationPermission) {
            PriceCheckWorker.schedule(context)
        } else {
            PriceCheckWorker.cancel(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Inställningar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Text(
            text = "Anpassa appen efter dina behov.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Display Settings Card
        SettingsCard(title = "Visning") {
            SettingsToggleRow(
                title = "Visa priser inkl. moms",
                subtitle = "Lägg till 25% moms på alla priser",
                checked = settings.showVAT,
                onCheckedChange = { checked ->
                    viewModel.updateAlertSettings(settings.copy(showVAT = checked))
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alerts Settings Card
        SettingsCard(title = "Prisvarningar") {
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = if (settings.enabled && hasNotificationPermission)
                        Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = if (settings.enabled && hasNotificationPermission) StatGreen else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Text(
                        text = when {
                            settings.enabled && hasNotificationPermission -> "Aktiv"
                            settings.enabled && !hasNotificationPermission -> "Kräver behörighet"
                            else -> "Inaktiv"
                        },
                        fontWeight = FontWeight.Medium,
                        color = when {
                            settings.enabled && hasNotificationPermission -> StatGreen
                            settings.enabled && !hasNotificationPermission -> StatRed
                            else -> TextTertiary
                        }
                    )
                    Text(
                        text = "Region: ${currentRegion.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            SettingsToggleRow(
                title = "Aktivera prislarm",
                subtitle = "Få notifikationer när priset når dina gränser",
                checked = settings.enabled,
                onCheckedChange = { checked ->
                    if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Request permission first
                        requestNotificationPermission()
                    }
                    viewModel.updateAlertSettings(settings.copy(enabled = checked))
                }
            )

            if (settings.enabled) {
                // Permission warning
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openNotificationSettings() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = StatRed.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "⚠️ Notifikationer är blockerade. Tryck här för att aktivera.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Meddela mig när priset är:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Low price threshold - slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Under",
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${settings.lowPrice.toInt()} öre",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = StatGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = settings.lowPrice.toFloat(),
                            onValueChange = { value ->
                                viewModel.updateAlertSettings(settings.copy(lowPrice = value.toInt().toDouble()))
                            },
                            valueRange = 0f..150f,
                            steps = 29, // 0, 5, 10, 15... 150
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = StatGreen,
                                activeTrackColor = StatGreen,
                                inactiveTrackColor = StatGreen.copy(alpha = 0.2f)
                            )
                        )
                        Text(
                            text = "Bra för elbilsladdning och tunga apparater",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High price threshold - slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatRed.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Över",
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${settings.highPrice.toInt()} öre",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = StatRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = settings.highPrice.toFloat(),
                            onValueChange = { value ->
                                viewModel.updateAlertSettings(settings.copy(highPrice = value.toInt().toDouble()))
                            },
                            valueRange = 50f..500f,
                            steps = 89, // 50, 55, 60... 500 (steg om 5)
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = StatRed,
                                activeTrackColor = StatRed,
                                inactiveTrackColor = StatRed.copy(alpha = 0.2f)
                            )
                        )
                        Text(
                            text = "Undvik tunga apparater vid högt pris",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test notification button
                if (settings.enabled && hasNotificationPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val helper = NotificationHelper(context)
                                helper.showTestNotification()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Testa notifikation",
                                fontWeight = FontWeight.Medium,
                                color = BrandBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Info about VAT sync
                val vatText = if (settings.showVAT) "inkl. moms" else "exkl. moms"
                Text(
                    text = "Priserna jämförs $vatText (samma som din visningsinställning). Kontrolleras var 15:e minut.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Card
        SettingsCard(title = "Om appen") {
            SettingsInfoRow(label = "Version", value = "1.0.0")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInfoRow(label = "Utvecklare", value = "Elnor Elhandel AB")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInfoRow(label = "Datakälla", value = "elprisetjustnu.se")
        }

        Spacer(modifier = Modifier.size(100.dp)) // Bottom nav spacing
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BackgroundSecondary
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}