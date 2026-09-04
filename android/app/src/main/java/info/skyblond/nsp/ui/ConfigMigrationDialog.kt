package info.skyblond.nsp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import info.skyblond.nsp.data.PairedCamera
import info.skyblond.nsp.data.SettingsRepository

@Composable
fun ConfigMigrationDialog(
    savedCameras: List<PairedCamera>,
    exportJson: String,
    onImportConfig: (String) -> SettingsRepository.ImportResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Export, 1: Import
    var importInputText by remember { mutableStateOf("") }
    var importPreview by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = L10n.t("相机配置备份与跨机迁移", "Camera Config Migration"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = L10n.t("在不同手机之间无缝同步已配对相机", "Seamlessly sync paired cameras across devices"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(L10n.t("关闭", "Close"))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                L10n.t("导出配置 (备份)", "Export Config"),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                L10n.t("导入配置 (恢复)", "Import Config"),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // Export View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = L10n.t(
                                    "当前已保存 ${savedCameras.size} 台相机配置：",
                                    "Currently saved cameras (${savedCameras.size}):"
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            savedCameras.forEach { cam ->
                                Text(
                                    text = "• ${cam.displayName} (${cam.name})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // JSON Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = exportJson,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("NSG_Config", exportJson)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            L10n.t("配置 JSON 已成功复制到剪贴板！", "Config JSON copied to clipboard!"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Text(L10n.t("📋 复制到剪贴板", "📋 Copy to Clipboard"))
                                }

                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, exportJson)
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                sendIntent,
                                                L10n.t("分享相机配置", "Share Camera Config")
                                            )
                                        )
                                    }
                                ) {
                                    Text(L10n.t("📤 分享配置", "📤 Share Config"))
                                }
                            }
                        }
                    } else {
                        // Import View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = L10n.t("粘贴从其他手机导出的配置 JSON 文本：", "Paste the config JSON exported from another device:"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = importInputText,
                                onValueChange = {
                                    importInputText = it
                                    importError = null
                                    importPreview = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                placeholder = {
                                    Text(
                                        L10n.t("在此粘贴 {\"cameras\": [...]} JSON 内容", "Paste {\"cameras\": [...]} JSON content here"),
                                        fontSize = 12.sp
                                    )
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                maxLines = 10
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0).text?.toString() ?: ""
                                            if (text.isNotBlank()) {
                                                importInputText = text
                                            } else {
                                                Toast.makeText(context, L10n.t("剪贴板为空", "Clipboard is empty"), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text(L10n.t("从剪贴板粘贴", "Paste from Clipboard"))
                                }

                                Button(
                                    modifier = Modifier.weight(1f),
                                    enabled = importInputText.isNotBlank(),
                                    onClick = {
                                        val res = onImportConfig(importInputText)
                                        if (res.success) {
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            importError = res.message
                                        }
                                    }
                                ) {
                                    Text(L10n.t("确认导入并合并", "Confirm Import & Merge"))
                                }
                            }

                            if (importError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "❌ $importError",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
