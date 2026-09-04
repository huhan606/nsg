package info.skyblond.nsp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.skyblond.nsp.data.DiscoveredCamera
import info.skyblond.nsp.data.PairedCamera

@Composable
fun DiscoveredCameraDialog(
    cameras: List<DiscoveredCamera>,
    onSelect: (DiscoveredCamera) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.t("选择要配对的相机", "Select camera to pair")) },
        text = {
            if (cameras.isEmpty()) {
                Text(L10n.t("未发现相机，请确认相机已进入配对模式", "No camera found. Make sure the camera is in pairing mode."))
            } else {
                LazyColumn {
                    items(cameras, key = { it.address }) { camera ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { onSelect(camera) }
                        ) {
                            Text(
                                text = buildString {
                                    append(camera.name)
                                    append("\n")
                                    append(camera.address)
                                    if (camera.manufacturerData != null) {
                                        append("\n[" + L10n.t("已有配对记录，选择后可直接切换连接", "Already paired; select to switch") + "]")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.t("取消", "Cancel"))
            }
        }
    )
}

@Composable
fun SavedCameraDialog(
    cameras: List<PairedCamera>,
    onSelect: (PairedCamera) -> Unit,
    onAutoExtract: (PairedCamera) -> Unit,
    onSetDefault: (PairedCamera) -> Unit,
    onDelete: (PairedCamera) -> Unit,
    onRename: (PairedCamera, String?) -> Unit,
    defaultCameraName: String?,
    onDismiss: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<PairedCamera?>(null) }
    var pendingRename by remember { mutableStateOf<PairedCamera?>(null) }
    var renameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            pendingDelete = null
            pendingRename = null
            onDismiss()
        },
        title = { Text(L10n.t("选择已保存的相机", "Select a saved camera")) },
        text = {
            if (cameras.isEmpty()) {
                Text(L10n.t("暂无已保存的相机，请先配对", "No saved cameras yet. Pair one first."))
            } else {
                LazyColumn {
                    if (cameras.size > 1) {
                        item {
                            Text(
                                text = L10n.t(
                                    "已保存 ${cameras.size} 台相机。可点「设为默认」选择启动时自动连接的相机，点「重命名」自定义别名。",
                                    "${cameras.size} camera(s) saved. Use \"Set Default\" to pick auto-connect camera; \"Rename\" to set an alias."
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    items(cameras, key = { it.address }) { camera ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val isDefault = defaultCameraName == camera.name
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = camera.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (camera.customName != null) "${camera.name} · ${camera.address}" else camera.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (isDefault) {
                                    Text(
                                        text = L10n.t("✓ 启动时默认连接", "✓ Default at startup"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(onClick = { onSelect(camera) }) {
                                        Text(L10n.t("连接", "Connect"))
                                    }
                                    TextButton(onClick = { onAutoExtract(camera) }) {
                                        Text(L10n.t("自动提取标识", "Auto Extract ID"))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(onClick = {
                                        pendingRename = camera
                                        renameInput = camera.customName ?: ""
                                    }) {
                                        Text(L10n.t("重命名", "Rename"))
                                    }
                                    if (cameras.size > 1) {
                                        TextButton(onClick = { onSetDefault(camera) }) {
                                            Text(if (isDefault) L10n.t("取消默认", "Unset Default") else L10n.t("设为默认", "Set Default"))
                                        }
                                    }
                                    TextButton(onClick = { pendingDelete = camera }) {
                                        Text(L10n.t("删除", "Delete"), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pendingDelete = null
                pendingRename = null
                onDismiss()
            }) {
                Text(L10n.t("关闭", "Close"))
            }
        }
    )
    pendingDelete?.let { camera ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(L10n.t("删除相机", "Delete camera")) },
            text = {
                Text(
                    L10n.t(
                        "确定要删除 ${camera.name} 吗？删除后需要重新配对才能连接。",
                        "Delete ${camera.name}? You will need to pair again to reconnect."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(camera)
                }) {
                    Text(L10n.t("删除", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(L10n.t("取消", "Cancel"))
                }
            }
        )
    }
    pendingRename?.let { camera ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text(L10n.t("自定义相机名称", "Rename Camera")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = L10n.t(
                            "为 ${camera.name} 设置别名（如「主力机 Z8」），方便在多台机身间快速识别与切换：",
                            "Set an alias for ${camera.name} (e.g. \"Main Z8\") to easily distinguish multiple bodies:"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text(L10n.t("相机别名/备注", "Camera Alias")) },
                        placeholder = { Text(camera.name) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(camera, renameInput.takeIf { it.isNotBlank() })
                    pendingRename = null
                }) {
                    Text(L10n.t("保存", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(L10n.t("取消", "Cancel"))
                }
            }
        )
    }
}
