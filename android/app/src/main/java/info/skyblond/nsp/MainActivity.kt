package info.skyblond.nsp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Switch
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
import info.skyblond.nsp.data.PairedCamera
import info.skyblond.nsp.data.SettingsRepository
import info.skyblond.nsp.service.ConnectionState
import info.skyblond.nsp.service.GpxTrackLogger
import info.skyblond.nsp.ui.BluetoothEnableGate
import info.skyblond.nsp.ui.ConfigMigrationDialog
import info.skyblond.nsp.ui.DiscoveredCameraDialog
import info.skyblond.nsp.ui.L10n
import info.skyblond.nsp.ui.MainViewModel
import info.skyblond.nsp.ui.PermissionHandler
import info.skyblond.nsp.ui.RequiredPermissions
import info.skyblond.nsp.ui.SavedCameraDialog
import info.skyblond.nsp.ui.TrackMapDialog
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
    var isHapticEnabled by remember { mutableStateOf(settingsRepo.isHapticFeedbackEnabled()) }
    var isTrackLoggingEnabled by remember { mutableStateOf(settingsRepo.isTrackLoggingEnabled()) }
    var showAdvancedSettingsPage by remember { mutableStateOf(false) }
    var showConfigMigrationDialog by remember { mutableStateOf(false) }
    var showTrackMapDialog by remember { mutableStateOf(false) }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val batteryExempt = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    val onHapticEnabledChange: (Boolean) -> Unit = { enabled ->
        isHapticEnabled = enabled
        settingsRepo.setHapticFeedbackEnabled(enabled)
    }

    val onTrackLoggingEnabledChange: (Boolean) -> Unit = { enabled ->
        isTrackLoggingEnabled = enabled
        settingsRepo.setTrackLoggingEnabled(enabled)
    }

    val onExportGpx = {
        val file = GpxTrackLogger.exportGpx(context)
        if (file != null) {
            GpxTrackLogger.shareGpx(context, file)
        } else {
            Toast.makeText(context, L10n.t("暂无航迹点可导出", "No track points to export"), Toast.LENGTH_SHORT).show()
        }
    }

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

    val onOpenNotificationSettings = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
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
            isHapticEnabled = isHapticEnabled,
            onHapticEnabledChange = onHapticEnabledChange,
            isTrackLoggingEnabled = isTrackLoggingEnabled,
            onTrackLoggingEnabledChange = onTrackLoggingEnabledChange,
            onExportGpx = onExportGpx,
            onOpenTrackMap = { showTrackMapDialog = true },
            onOpenConfigMigration = { showConfigMigrationDialog = true },
            batteryExempt = batteryExempt,
            onBatteryClick = onBatteryClick,
            onOpenAppSettings = onOpenAppSettings,
            onOpenNotificationSettings = onOpenNotificationSettings,
            uiState = uiState,
            onBack = { showAdvancedSettingsPage = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val horizontalPadding = if (maxWidth < 360.dp) 10.dp else 16.dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HeaderBar(
                            onOpenTrackMap = { showTrackMapDialog = true },
                            onOpenConfigMigration = { showConfigMigrationDialog = true },
                            onOpenAdvanced = { showAdvancedSettingsPage = true }
                        )
                        StatusTelemetryCard(
                            uiState = uiState,
                            onSwitchCamera = { viewModel.onSavedCameraSelected(it) },
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
                            onQuickReconnectClick = { targetCam -> viewModel.onSavedCameraSelected(targetCam) },
                            onSendClick = { viewModel.onSendClicked() },
                            onDisconnectClick = { viewModel.onDisconnectClicked() }
                        )
                    }
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
            onRename = { camera, newName -> viewModel.onRenameCamera(camera, newName) },
            defaultCameraName = uiState.defaultCameraName,
            onDismiss = { viewModel.onDismissSavedDialog() }
        )
    }

    if (showConfigMigrationDialog) {
        ConfigMigrationDialog(
            savedCameras = uiState.savedCameras,
            exportJson = settingsRepo.exportConfigJson(),
            onImportConfig = { json ->
                val result = settingsRepo.importConfigJson(json)
                if (result.success) {
                    viewModel.onRefreshSavedCameras(context)
                }
                result
            },
            onDismiss = { showConfigMigrationDialog = false }
        )
    }

    if (showTrackMapDialog) {
        TrackMapDialog(
            onExportGpx = onExportGpx,
            onClearTrack = {
                GpxTrackLogger.clear()
                Toast.makeText(context, L10n.t("已清空航迹记录", "Track points cleared"), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTrackMapDialog = false }
        )
    }
}

/**
 * Top branding bar with Nikon logo accent and settings menu.
 */
@Composable
private fun HeaderBar(
    onOpenTrackMap: () -> Unit,
    onOpenConfigMigration: () -> Unit,
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
                    text = { Text(L10n.t("🗺️ 拍摄足迹地图", "🗺️ Photo Footprint Map")) },
                    onClick = {
                        menuOpen = false
                        onOpenTrackMap()
                    }
                )
                DropdownMenuItem(
                    text = { Text(L10n.t("🔄 相机配置备份与迁移", "🔄 Camera Config Migration")) },
                    onClick = {
                        menuOpen = false
                        onOpenConfigMigration()
                    }
                )
                DropdownMenuItem(
                    text = { Text(L10n.t("⚙️ 高级与省电设置", "⚙️ Advanced & Power Settings")) },
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
private fun BreathingStatusIndicator(
    connectionState: ConnectionState,
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val isPulsing = connectionState is ConnectionState.Ready ||
        connectionState is ConnectionState.Busy ||
        connectionState is ConnectionState.Scanning ||
        connectionState is ConnectionState.Connecting ||
        connectionState is ConnectionState.Pairing

    if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "breathingPulse")
        val pulseDuration = when (connectionState) {
            is ConnectionState.Busy -> 700
            is ConnectionState.Scanning -> 1100
            else -> 1800
        }
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (connectionState is ConnectionState.Busy) 2.2f else 1.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(pulseDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseScale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(pulseDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseAlpha"
        )

        Box(
            modifier = modifier.size(dotSize * 2.2f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize * scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    } else {
        Box(
            modifier = modifier.size(dotSize * 2.2f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
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
    onSwitchCamera: (PairedCamera) -> Unit,
    onCopyCoords: (String) -> Unit
) {
    val gps = uiState.gpsState
    val statusColor = when (uiState.connectionState) {
        is ConnectionState.Ready -> StatusReady
        is ConnectionState.Busy -> StatusBusy
        is ConnectionState.Scanning, is ConnectionState.Connecting,
        is ConnectionState.Discovering, is ConnectionState.Pairing,
        is ConnectionState.Bonding -> StatusScanning
        is ConnectionState.Error -> StatusError
        is ConnectionState.Idle -> StatusIdle
    }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BreathingStatusIndicator(
                            connectionState = uiState.connectionState,
                            color = statusColor,
                            dotSize = 8.dp
                        )
                        Text(
                            text = uiState.connectionState.label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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

            // Multi-Camera Quick Switch Row (if paired cameras exist)
            if (uiState.savedCameras.isNotEmpty()) {
                var cameraMenuOpen by remember { mutableStateOf(false) }
                val activeCamera = uiState.savedCameras.firstOrNull { it.name == uiState.defaultCameraName }
                    ?: uiState.savedCameras.firstOrNull()
                val activeCameraName = activeCamera?.displayName ?: ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.t("目标相机", "Target Camera"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NikonYellow.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable(enabled = uiState.savedCameras.size > 1) {
                                cameraMenuOpen = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📷 $activeCameraName",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (uiState.savedCameras.size > 1) {
                                    Text(
                                        text = "▼",
                                        fontSize = 8.sp,
                                        color = NikonYellow
                                    )
                                }
                            }
                        }

                        if (uiState.savedCameras.size > 1) {
                            DropdownMenu(
                                expanded = cameraMenuOpen,
                                onDismissRequest = { cameraMenuOpen = false }
                            ) {
                                uiState.savedCameras.forEach { cam ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(cam.displayName)
                                                if (cam.name == uiState.defaultCameraName) {
                                                    Text(
                                                        text = L10n.t("[默认]", "[Default]"),
                                                        color = NikonYellow,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            cameraMenuOpen = false
                                            onSwitchCamera(cam)
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                        text = L10n.t("GPS 待命 · 连接相机后自动开启高精度卫星定位与持续授时", "GPS Standby · High-precision positioning & sync starts upon connection"),
                        style = MaterialTheme.typography.bodySmall,
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
                            text = L10n.t("正在捕获卫星信号 · 建议移至户外开阔处加速锁定", "Acquiring GNSS satellites · Move to an open sky area for faster lock"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    val lat = gps.latitude ?: 0.0
                    val lon = gps.longitude ?: 0.0
                    val latDir = if (lat >= 0) "N" else "S"
                    val lonDir = if (lon >= 0) "E" else "W"
                    val coordsStr = String.format(Locale.US, "%.5f°%s, %.5f°%s", lat.absoluteValue, latDir, lon.absoluteValue, lonDir)

                    // Accuracy Level & Satellite Pill Banner
                    val accMeters = gps.accuracyMeters
                    val (accColor, accLabel) = when {
                        accMeters == null -> MaterialTheme.colorScheme.onSurfaceVariant to L10n.t("定位中", "Fixing")
                        accMeters <= 8f -> StatusReady to L10n.t("高精度", "High Precision")
                        accMeters <= 20f -> StatusBusy to L10n.t("良好", "Good")
                        else -> StatusError to L10n.t("粗略", "Coarse")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accColor)
                                )
                                Text(
                                    text = "$accLabel (±${accMeters?.toInt() ?: 0}m)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = accColor
                                )
                            }
                        }

                        val satText = if (gps.satellites != null) {
                            "${gps.satellites} ${L10n.t("颗卫星", "sats")}" + (gps.totalSatellites?.let { "/$it" } ?: "")
                        } else {
                            L10n.t("GNSS 已锁定", "GNSS Locked")
                        }
                        Text(
                            text = "🛰️ $satText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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
                            label = L10n.t("海拔", "Altitude"),
                            value = if (gps.altitude != null) "${if (gps.altitude >= 0) "+" else ""}${gps.altitude.toInt()}m" else "-- m"
                        )
                        MetricPill(
                            label = L10n.t("卫星", "Satellites"),
                            value = if (gps.satellites != null) "${gps.satellites}颗" else "4+颗"
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

            val isConnected = uiState.connectionState is ConnectionState.Ready || uiState.connectionState is ConnectionState.Busy
            if (!isConnected && uiState.savedCameras.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = L10n.t(
                            "💡 提示：开启相机蓝牙，相机将自动监听并与手机极速握手连接。",
                            "💡 Tip: Turn on camera Bluetooth; it will automatically handshake and connect."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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
 * Action Control Grid that adapts between 1-hop quick reconnect and active shooting controls.
 */
@Composable
private fun ActionControlGrid(
    uiState: MainViewModel.UiState,
    pairingButtonsEnabled: Boolean,
    connected: Boolean,
    connectionInProgress: Boolean,
    onPairClick: () -> Unit,
    onConnectClick: () -> Unit,
    onQuickReconnectClick: (PairedCamera) -> Unit,
    onSendClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val targetCam = uiState.savedCameras.firstOrNull { it.name == uiState.defaultCameraName }
        ?: uiState.savedCameras.firstOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (connected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "📡 " + L10n.t("立即发送 GPS", "Send GPS Now"),
                    enabled = uiState.connectionState is ConnectionState.Ready,
                    isPrimary = true,
                    onClick = onSendClick,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "✕ " + L10n.t("断开连接", "Disconnect"),
                    enabled = true,
                    isDanger = true,
                    onClick = onDisconnectClick,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (connectionInProgress) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = L10n.t("正在连接...", "Connecting..."),
                    enabled = false,
                    loading = true,
                    modifier = Modifier.weight(1.3f),
                    onClick = {}
                )
                ActionButton(
                    text = "✕ " + L10n.t("取消", "Cancel"),
                    enabled = true,
                    isDanger = true,
                    onClick = onDisconnectClick,
                    modifier = Modifier.weight(0.7f)
                )
            }
        } else if (targetCam != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "⚡ " + L10n.t("重新连接 (${targetCam.displayName})", "Reconnect (${targetCam.displayName})"),
                    enabled = pairingButtonsEnabled,
                    isPrimary = true,
                    onClick = { onQuickReconnectClick(targetCam) },
                    modifier = Modifier.weight(1.3f)
                )
                ActionButton(
                    text = L10n.t("相机管理", "Manage"),
                    enabled = pairingButtonsEnabled,
                    onClick = onConnectClick,
                    modifier = Modifier.weight(0.7f)
                )
            }
            ActionButton(
                text = "🔍 " + L10n.t("配对新相机", "Pair New Camera"),
                enabled = pairingButtonsEnabled,
                onClick = onPairClick,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "🔍 " + L10n.t("配对新相机", "Pair New"),
                    enabled = pairingButtonsEnabled,
                    isPrimary = true,
                    onClick = onPairClick,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = L10n.t("相机管理", "Manage"),
                    enabled = pairingButtonsEnabled,
                    onClick = onConnectClick,
                    modifier = Modifier.weight(1f)
                )
            }
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
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenNotificationSettings)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.t("🔔 锁屏与通知栏胶囊权限", "🔔 Lockscreen & Notifications"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = L10n.t("若锁屏息屏看不到胶囊，请进入系统设置开启「锁屏显示通知」与「公开内容」", "If not visible on lockscreen, ensure lockscreen notifications are enabled in system settings"),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "⚙️",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
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
    isHapticEnabled: Boolean,
    onHapticEnabledChange: (Boolean) -> Unit,
    isTrackLoggingEnabled: Boolean,
    onTrackLoggingEnabledChange: (Boolean) -> Unit,
    onExportGpx: () -> Unit,
    onOpenTrackMap: () -> Unit,
    onOpenConfigMigration: () -> Unit,
    batteryExempt: Boolean,
    onBatteryClick: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    uiState: MainViewModel.UiState,
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

                // Section 2: Photography Assist & Haptic Feedback
                PhotographyToolsSection(
                    isHapticEnabled = isHapticEnabled,
                    onHapticEnabledChange = onHapticEnabledChange,
                    isTrackLoggingEnabled = isTrackLoggingEnabled,
                    onTrackLoggingEnabledChange = onTrackLoggingEnabledChange,
                    onExportGpx = onExportGpx,
                    onOpenTrackMap = onOpenTrackMap
                )

                // Section 3: Camera Config Clone & Migration
                ConfigMigrationSectionCard(
                    onOpenConfigMigration = onOpenConfigMigration
                )

                // Section 4: Battery Optimization & Notification Settings
                BatteryExemptionCard(
                    batteryExempt = batteryExempt,
                    onBatteryClick = onBatteryClick,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenNotificationSettings = onOpenNotificationSettings
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Section 5: SnapBridge Compatibility & Identity
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Section 6: Real-time Communication Logs
                EventsTerminalCard(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 340.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ConfigMigrationSectionCard(
    onOpenConfigMigration: () -> Unit
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
                    text = L10n.t("相机配置克隆与跨机迁移", "Camera Config Clone & Migration"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = L10n.t(
                        "将已配对相机的密钥与别名导出为 JSON 备份，可在另一台手机上直接导入，无需重新在相机端配对。",
                        "Export camera pairing keys & aliases as JSON to clone to another phone without repairing on camera."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onOpenConfigMigration,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(L10n.t("🔄 打开备份与迁移工具", "🔄 Open Backup & Migration Tool"))
            }
        }
    }
}

@Composable
private fun PhotographyToolsSection(
    isHapticEnabled: Boolean,
    onHapticEnabledChange: (Boolean) -> Unit,
    isTrackLoggingEnabled: Boolean,
    onTrackLoggingEnabledChange: (Boolean) -> Unit,
    onExportGpx: () -> Unit,
    onOpenTrackMap: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = L10n.t("实战摄影与触感辅助", "Shooting Assist & Haptics"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = L10n.t("专为户外口袋盲操与旅行记录设计的专属体验", "Designed for pocket blind-shooting and travel records"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Haptic switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = L10n.t("触感震动反馈", "Haptic Vibration Feedback"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = L10n.t("相机连接就绪、定位发送成功及异常断开时触发物理触觉反馈", "Vibrate when camera connects, GPS sends, or disconnects"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isHapticEnabled,
                    onCheckedChange = onHapticEnabledChange
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Track logging switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = L10n.t("记录拍摄足迹 (GPX)", "Record Photo Track (GPX)"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = L10n.t("同步定位时记录航迹，方便导入 Lightroom / 摄影地图回放", "Record GPS track for Lightroom / photography map"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isTrackLoggingEnabled,
                    onCheckedChange = onTrackLoggingEnabledChange
                )
            }

            // Map preview and Export GPX buttons
            val pointCount = remember { GpxTrackLogger.pointCount() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenTrackMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = L10n.t("🗺️ 轨迹预览", "🗺️ View Route"))
                }
                OutlinedButton(
                    onClick = onExportGpx,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = L10n.t("📤 导出 GPX", "📤 Export") + " ($pointCount)"
                    )
                }
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
