package info.skyblond.nsp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.skyblond.nsp.data.SettingsRepository
import info.skyblond.nsp.service.ConnectionState
import info.skyblond.nsp.ui.BluetoothEnableGate
import info.skyblond.nsp.ui.DiscoveredCameraDialog
import info.skyblond.nsp.ui.L10n
import info.skyblond.nsp.ui.MainViewModel
import info.skyblond.nsp.ui.PermissionHandler
import info.skyblond.nsp.ui.RequiredPermissions
import info.skyblond.nsp.ui.SavedCameraDialog
import info.skyblond.nsp.ui.theme.NikonSmartGPSTheme
import info.skyblond.nsp.ui.theme.NikonYellow
import info.skyblond.nsp.ui.theme.StatusBusy
import info.skyblond.nsp.ui.theme.StatusError
import info.skyblond.nsp.ui.theme.StatusIdle
import info.skyblond.nsp.ui.theme.StatusReady
import info.skyblond.nsp.ui.theme.StatusScanning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NikonSmartGPSTheme {
                PermissionHandler(
                    onPermissionsGranted = { viewModel.startAndBindService() }
                ) {
                    BluetoothEnableGate {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (RequiredPermissions.allGranted(this)) {
            viewModel.startAndBindService()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService()
    }
}

@Composable
private fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    var spoofName by remember { mutableStateOf(settingsRepo.spoofControllerName() ?: "") }
    var fixedDeviceId by remember { mutableStateOf(settingsRepo.fixedDeviceIdRaw() ?: "") }
    var gpsIntervalSeconds by remember { mutableIntStateOf(settingsRepo.gpsIntervalSeconds()) }
    var showAdvancedSettingsPage by remember { mutableStateOf(false) }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val batteryExempt = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    val onBatteryClick = {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    val onOpenAppSettings = {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    val onSpoofNameChange: (String) -> Unit = { value ->
        val sanitized = value.filter { c -> c.code < 128 }.take(32)
        spoofName = sanitized
        settingsRepo.setSpoofControllerName(sanitized)
    }

    val onFixedDeviceIdChange: (String) -> Unit = { value ->
        val sanitized = value.filter { c -> c.code < 128 }.take(16)
        fixedDeviceId = sanitized
        settingsRepo.setFixedDeviceId(sanitized)
    }

    val onGpsIntervalChange: (Int) -> Unit = { seconds ->
        gpsIntervalSeconds = seconds
        settingsRepo.setGpsIntervalSeconds(seconds)
        viewModel.onGpsIntervalChanged(seconds)
    }

    LaunchedEffect(Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    val connected = uiState.connectionState is ConnectionState.Ready ||
        uiState.connectionState is ConnectionState.Busy
    val connectionInProgress = uiState.connectionState is ConnectionState.Scanning ||
        uiState.connectionState is ConnectionState.Connecting ||
        uiState.connectionState is ConnectionState.Discovering ||
        uiState.connectionState is ConnectionState.Pairing ||
        uiState.connectionState is ConnectionState.Bonding
    val pairingButtonsEnabled = !connected && !connectionInProgress

    BackHandler(enabled = showAdvancedSettingsPage) {
        showAdvancedSettingsPage = false
    }

    if (showAdvancedSettingsPage) {
        AdvancedSettingsPage(
            spoofName = spoofName,
            onSpoofNameChange = onSpoofNameChange,
            fixedDeviceId = fixedDeviceId,
            onFixedDeviceIdChange = onFixedDeviceIdChange,
            gpsIntervalSeconds = gpsIntervalSeconds,
            onGpsIntervalChange = onGpsIntervalChange,
            onBack = { showAdvancedSettingsPage = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight
            val isTabletOrFoldable = maxWidth >= 600.dp
            val isWideLayout = isTabletOrFoldable || (isLandscape && maxWidth >= 500.dp)
            val horizontalPadding = if (maxWidth < 360.dp) 8.dp else 16.dp

            if (isWideLayout) {
                // Wide / Tablet / Landscape 2-column layout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Controls & Telemetry Dashboard
                        Column(
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeaderBar(
                                connectionState = uiState.connectionState,
                                onOpenAdvanced = { showAdvancedSettingsPage = true }
                            )
                            StatusTelemetryCard(
                                uiState = uiState,
                                onCopyCoords = { coords ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("GPS", coords))
                                    Toast.makeText(context, L10n.t("已复制坐标", "Coordinates copied"), Toast.LENGTH_SHORT).show()
                                }
                            )
                            ActionControlGrid(
                                uiState = uiState,
                                pairingButtonsEnabled = pairingButtonsEnabled,
                                connected = connected,
                                connectionInProgress = connectionInProgress,
                                onPairClick = { viewModel.onPairClicked() },
                                onConnectClick = { viewModel.onConnectClicked() },
                                onSendClick = { viewModel.onSendClicked() },
                                onDisconnectClick = { viewModel.onDisconnectClicked() }
                            )
                            BatteryExemptionCard(
                                batteryExempt = batteryExempt,
                                onBatteryClick = onBatteryClick,
                                onOpenAppSettings = onOpenAppSettings
                            )
                        }

                        // Right Column: Full-height Activity Logs
                        EventsTerminalCard(
                            uiState = uiState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                // Portrait / Compact Phone single scrollable column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderBar(
                        connectionState = uiState.connectionState,
                        onOpenAdvanced = { showAdvancedSettingsPage = true }
                    )
                    StatusTelemetryCard(
                        uiState = uiState,
                        onCopyCoords = { coords ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("GPS", coords))
                            Toast.makeText(context, L10n.t("已复制坐标", "Coordinates copied"), Toast.LENGTH_SHORT).show()
                        }
                    )
                    ActionControlGrid(
                        uiState = uiState,
                        pairingButtonsEnabled = pairingButtonsEnabled,
                        connected = connected,
                        connectionInProgress = connectionInProgress,
                        onPairClick = { viewModel.onPairClicked() },
                        onConnectClick = { viewModel.onConnectClicked() },
                        onSendClick = { viewModel.onSendClicked() },
                        onDisconnectClick = { viewModel.onDisconnectClicked() }
                    )
                    BatteryExemptionCard(
                        batteryExempt = batteryExempt,
                        onBatteryClick = onBatteryClick,
                        onOpenAppSettings = onOpenAppSettings
                    )
                    EventsTerminalCard(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp)
                    )
                }
            }
        }
    }

    if (uiState.showDiscoveredDialog) {
        DiscoveredCameraDialog(
            cameras = uiState.discoveredCameras,
            onSelect = { viewModel.onDiscoveredCameraSelected(it) },
            onDismiss = { viewModel.onDismissDiscoveredDialog() }
        )
    }

    if (uiState.showSavedDialog) {
        SavedCameraDialog(
            cameras = uiState.savedCameras,
            onSelect = { viewModel.onSavedCameraSelected(it) },
            onAutoExtract = { viewModel.onAutoExtractClicked(it) },
            onSetDefault = { viewModel.onSetDefaultCamera(it) },
            onDelete = { viewModel.onDeleteCamera(it) },
            defaultCameraName = uiState.defaultCameraName,
            onDismiss = { viewModel.onDismissSavedDialog() }
        )
    }
}

/**
 * Top branding bar with Nikon logo accent, live status chip, and settings menu.
 */
@Composable
private fun HeaderBar(
    connectionState: ConnectionState,
    onOpenAdvanced: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nikon Brand Indicator Accent
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NikonYellow)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = L10n.t("NIKON SMART GPS", "NIKON SMART GPS"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = L10n.t("尼康微单蓝牙定位助手", "Nikon Z Smart GPS Provider"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Connection Status Chip
        StatusIndicatorChip(connectionState = connectionState)

        // Overflow Menu
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = L10n.t("更多", "More"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(L10n.t("高级与省电设置", "Advanced & Power Settings")) },
                    onClick = {
                        menuOpen = false
                        onOpenAdvanced()
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusIndicatorChip(connectionState: ConnectionState) {
    val (statusColor, statusText) = when (connectionState) {
        is ConnectionState.Ready -> StatusReady to L10n.t("就绪", "Ready")
        is ConnectionState.Busy -> StatusBusy to L10n.t("忙碌", "Busy")
        is ConnectionState.Scanning -> StatusScanning to L10n.t("扫描中", "Scanning")
        is ConnectionState.Connecting -> StatusScanning to L10n.t("连接中", "Connecting")
        is ConnectionState.Discovering -> StatusScanning to L10n.t("发现服务", "Discovering")
        is ConnectionState.Pairing -> StatusScanning to L10n.t("握手中", "Pairing")
        is ConnectionState.Bonding -> StatusScanning to L10n.t("配对中", "Bonding")
        is ConnectionState.Error -> StatusError to L10n.t("错误", "Error")
        is ConnectionState.Idle -> StatusIdle to L10n.t("待机", "Idle")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = statusColor
            )
        }
    }
}

/**
 * Modern Dashboard Card displaying connection state, service background status,
 * and high-precision GPS telemetry details with a one-tap copy button.
 */
@Composable
private fun StatusTelemetryCard(
    uiState: MainViewModel.UiState,
    onCopyCoords: (String) -> Unit
) {
    val gps = uiState.gpsState

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Connection Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = L10n.t("连接状态", "Connection Status"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.connectionState.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.serviceBound) StatusReady.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (uiState.serviceBound) L10n.t("后台服务运行中", "Service Active") else L10n.t("服务未启动", "Service Inactive"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.serviceBound) StatusReady else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // GPS Telemetry Body
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = L10n.t("GPS 遥测数据", "GPS Telemetry"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!gps.enabled) {
                    Text(
                        text = L10n.t("未开启（连接相机后自动定位并发送）", "Off (auto-starts after connecting)"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!gps.hasFix) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = NikonYellow
                        )
                        Text(
                            text = L10n.t("正在搜星定位...", "Acquiring satellite fix..."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    val lat = gps.latitude ?: 0.0
                    val lon = gps.longitude ?: 0.0
                    val latDir = if (lat >= 0) "N" else "S"
                    val lonDir = if (lon >= 0) "E" else "W"
                    val coordsStr = String.format(Locale.US, "%.5f°%s, %.5f°%s", lat.absoluteValue, latDir, lon.absoluteValue, lonDir)

                    // Coordinate display with copy button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyCoords(coordsStr) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = coordsStr,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = L10n.t("复制", "Copy"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NikonYellow
                            )
                        }
                    }

                    // Metadata Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            label = L10n.t("精度", "Accuracy"),
                            value = "±${gps.accuracyMeters?.toInt() ?: 0}m"
                        )
                        MetricPill(
                            label = L10n.t("卫星", "Satellites"),
                            value = "GPS/BDS"
                        )
                        gps.lastSentTime?.let {
                            MetricPill(
                                label = L10n.t("最近发送", "Last Sent"),
                                value = formatTime(it)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 2x2 Action Control Grid that organizes actions compactly for all screen sizes.
 */
@Composable
private fun ActionControlGrid(
    uiState: MainViewModel.UiState,
    pairingButtonsEnabled: Boolean,
    connected: Boolean,
    connectionInProgress: Boolean,
    onPairClick: () -> Unit,
    onConnectClick: () -> Unit,
    onSendClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = L10n.t("配对新相机", "Pair New"),
                enabled = pairingButtonsEnabled,
                loading = uiState.connectionState is ConnectionState.Scanning,
                onClick = onPairClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = L10n.t("连接已保存", "Connect Saved"),
                enabled = pairingButtonsEnabled,
                loading = uiState.connectionState is ConnectionState.Connecting ||
                    uiState.connectionState is ConnectionState.Discovering ||
                    uiState.connectionState is ConnectionState.Pairing ||
                    uiState.connectionState is ConnectionState.Bonding,
                onClick = onConnectClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = L10n.t("立即发送 GPS", "Send GPS Now"),
                enabled = uiState.connectionState is ConnectionState.Ready,
                isPrimary = true,
                onClick = onSendClick,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = L10n.t("断开连接", "Disconnect"),
                enabled = connected || connectionInProgress,
                isDanger = true,
                onClick = onDisconnectClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    isPrimary: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColors = when {
        isPrimary && enabled -> ButtonDefaults.buttonColors(
            containerColor = NikonYellow,
            contentColor = Color(0xFF141414)
        )
        isDanger && enabled -> ButtonDefaults.buttonColors(
            containerColor = StatusError.copy(alpha = 0.15f),
            contentColor = StatusError
        )
        else -> ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = buttonColors,
        modifier = modifier.heightIn(min = 46.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact Battery Optimization Card showing status and providing direct access
 * to system application permissions and battery optimization settings.
 */
@Composable
private fun BatteryExemptionCard(
    batteryExempt: Boolean,
    onBatteryClick: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (batteryExempt) StatusReady else StatusBusy)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBatteryClick)
            ) {
                Text(
                    text = if (batteryExempt) {
                        L10n.t("电池优化：已豁免（后台稳定运行）", "Battery: Exempt (runs in background)")
                    } else {
                        L10n.t("电池优化：未豁免（点击申请白名单）", "Battery: Not exempt (tap to allow)")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (batteryExempt) MaterialTheme.colorScheme.onSurface else StatusBusy
                )
            }

            // Clickable button that jumps directly to system application details / permissions
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.clickable(onClick = onOpenAppSettings)
            ) {
                Text(
                    text = L10n.t("系统权限", "Permissions"),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Terminal-styled Live Events Log Card with clean timestamps and readable bullets.
 */
@Composable
private fun EventsTerminalCard(
    uiState: MainViewModel.UiState,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.t("实时日志", "Activity Log"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.lastEvents.size} " + L10n.t("条事件", "events"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            if (uiState.lastEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = L10n.t("暂无活动日志", "No events logged yet"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.lastEvents) { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = NikonYellow
                            )
                            Text(
                                text = event,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standalone advanced-settings and power rules page.
 * Utilizes Scaffold and insets padding to completely prevent collision with the system status bar.
 */
@Composable
private fun AdvancedSettingsPage(
    spoofName: String,
    onSpoofNameChange: (String) -> Unit,
    fixedDeviceId: String,
    onFixedDeviceIdChange: (String) -> Unit,
    gpsIntervalSeconds: Int,
    onGpsIntervalChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = L10n.t("返回", "Back")
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = L10n.t("高级与省电设置", "Advanced & Power Settings"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: GPS Send Interval & Power Rules
                GpsIntervalSection(
                    intervalSeconds = gpsIntervalSeconds,
                    onIntervalChange = onGpsIntervalChange
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Section 2: SnapBridge Compatibility & Identity
                Text(
                    text = L10n.t("相机标识与 SnapBridge 兼容", "Camera Identity & SnapBridge"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = L10n.t(
                        "以下设置一般无需更改；仅在希望与官方 SnapBridge 软件无缝切换共用配对记录时填写。",
                        "These are usually fine as-is; only adjust if switching seamlessly with Nikon SnapBridge."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SpoofNameField(
                    value = spoofName,
                    onValueChange = onSpoofNameChange,
                    modifier = Modifier.fillMaxWidth()
                )

                FixedDeviceIdField(
                    value = fixedDeviceId,
                    onValueChange = onFixedDeviceIdChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Interactive GPS transmission interval & power-saving rules card.
 */
@Composable
private fun GpsIntervalSection(
    intervalSeconds: Int,
    onIntervalChange: (Int) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = L10n.t("GPS 发送与省电规则", "GPS Transmission & Battery Rules"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = L10n.t(
                        "控制定位与蓝牙发送频率，有效延长手机与相机电池续航",
                        "Control send frequency to reduce GPS & Bluetooth battery usage"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val presets = listOf(
                15 to L10n.t("15秒 (高频)", "15s (Fast)"),
                30 to L10n.t("30秒 (推荐)", "30s (Balanced)"),
                60 to L10n.t("1分钟 (省电)", "1m (Saver)"),
                300 to L10n.t("5分钟 (续航)", "5m (Endurance)")
            )

            var isCustom by remember { mutableStateOf(!presets.any { it.first == intervalSeconds }) }
            var customInput by remember { mutableStateOf(if (isCustom) intervalSeconds.toString() else "") }

            // Preset Options Grid (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(2).forEach { (sec, label) ->
                        val selected = !isCustom && intervalSeconds == sec
                        IntervalOptionChip(
                            label = label,
                            selected = selected,
                            onClick = {
                                isCustom = false
                                onIntervalChange(sec)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(2).take(2).forEach { (sec, label) ->
                        val selected = !isCustom && intervalSeconds == sec
                        IntervalOptionChip(
                            label = label,
                            selected = selected,
                            onClick = {
                                isCustom = false
                                onIntervalChange(sec)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                IntervalOptionChip(
                    label = if (isCustom) "${L10n.t("自定义", "Custom")}: ${intervalSeconds}秒" else L10n.t("自定义间隔秒数...", "Custom Seconds..."),
                    selected = isCustom,
                    onClick = {
                        isCustom = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isCustom) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { str ->
                        val filtered = str.filter { it.isDigit() }.take(4)
                        customInput = filtered
                        val value = filtered.toIntOrNull()
                        if (value != null && value in 10..3600) {
                            onIntervalChange(value)
                        }
                    },
                    label = { Text(L10n.t("自定义发送间隔 (10 ~ 3600 秒)", "Custom interval (10 ~ 3600s)")) },
                    placeholder = { Text(L10n.t("例如 45 或 120", "e.g. 45 or 120")) },
                    supportingText = {
                        Text(
                            L10n.t(
                                "当前生效间隔：${intervalSeconds} 秒。静止或移动时均按此周期唤醒发送。",
                                "Active interval: ${intervalSeconds}s. Position reports duty-cycle with this interval."
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = L10n.t(
                        "💡 省电建议：日常扫街推荐「30秒」或「1分钟」；若长时间慢速拍摄，设为「5分钟」可让手机 GPS 芯片完全进入 Duty-Cycle 休眠，功耗减少 70% 以上。",
                        "💡 Battery Tip: \"30s\" or \"1m\" is ideal for daily walks; for long static shoots, \"5m\" allows the phone GNSS chip to enter duty-cycle sleep, saving over 70% power."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun IntervalOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) NikonYellow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) NikonYellow else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) NikonYellow else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SpoofNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(L10n.t("相机端设备名（伪装名，可留空）", "Camera-side device name (optional)")) },
        placeholder = { Text(L10n.t("默认：nsg-poc", "Default: nsg-poc")) },
        supportingText = {
            Text(
                L10n.t(
                    "仅限英文和数字，最长 32 字符；填与 SnapBridge 相同的名字，相机将视为同一设备",
                    "ASCII only, max 32 chars; matching SnapBridge's name makes the camera treat this app as the same device"
                )
            )
        },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun FixedDeviceIdField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(L10n.t("固定设备标识（可选，8或16位十六进制）", "Fixed device ID (optional, 8 or 16 hex)")) },
        placeholder = { Text(L10n.t("如 445D4B24 或 445D4B24981064F7；16位=设备ID+nonce", "e.g. 445D4B24 or 445D4B24981064F7; 16 hex = device+nonce")) },
        supportingText = {
            Text(
                L10n.t(
                    "填入 SnapBridge 的完整设备标识（16位=设备ID+nonce）可与 SnapBridge 无缝切换；8位仅固定设备ID",
                    "Enter SnapBridge's full ID (16 hex = device+nonce) to switch seamlessly; 8 hex fixes only the device ID"
                )
            )
        },
        singleLine = true,
        modifier = modifier
    )
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))
