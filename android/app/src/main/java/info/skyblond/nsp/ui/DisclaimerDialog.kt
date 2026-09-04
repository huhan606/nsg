package info.skyblond.nsp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Professional bilingual Legal and Open Source Disclaimer dialog.
 * Explicitly separates third-party branding from Nikon trademarks under Nominative Fair Use.
 */
@Composable
fun DisclaimerDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (L10n.isChinese) 0 else 1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.t("⚖️ 开源免责声明", "⚖️ Legal & Disclaimer"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                androidx.compose.material3.PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("中文声明") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("English") }
                    )
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 0) {
                        ChineseDisclaimerContent()
                    } else {
                        EnglishDisclaimerContent()
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Bottom button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(L10n.t("我已知悉并同意", "I Understand & Acknowledge"))
                }
            }
        }
    }
}

@Composable
private fun ChineseDisclaimerContent() {
    Text(
        text = "1. 软件性质与独立性声明",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "本软件（GPS Assistant / NSG）为开源社区独立开发的第三方摄影辅助工具，非商业性质。本软件与株式会社尼康（Nikon Corporation）及其任何关联企业均无任何官方隶属、合作、赞助或授权关系。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "2. 商标归属与合理使用（Fair Use）",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "“Nikon”、“尼康”、“SnapBridge”以及相关相机产品型号名称（如 Z5、Z6、Z7、Z8、Z9、Zf、Zfc 等）均为株式会社尼康（Nikon Corporation）的注册商标或商标。\n本应用及文档中提及上述名称与型号，仅用于客观标识设备软硬件兼容性（属于法律保护的指示性合理使用，Nominative Fair Use），绝非表明官方背书或商标混淆。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "3. 开源许可与无担保声明",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "本软件依据 GNU AGPL-3.0 许可证分发，按“原样”（AS-IS）提供，不包含任何明示或默示的担保。在法律允许的最大范围内，作者与贡献者不对使用本软件可能导致的任何直接、间接或连带损失承担责任。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "4. 隐私与数据安全",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "本软件不设置任何远程服务器，不收集、不出售且不上报用户的任何个人信息、定位轨迹或硬件标识。所有定位及蓝牙通信均完全在用户本地设备与相机之间点对点运行。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EnglishDisclaimerContent() {
    Text(
        text = "1. Nature of Software & Non-Affiliation",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "GPS Assistant (NSG) is an independent open-source photography utility developed by the community. It is NOT affiliated with, authorized, maintained, sponsored, or endorsed by Nikon Corporation or any of its affiliates.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "2. Trademark Notice & Nominative Fair Use",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "\"Nikon\", \"SnapBridge\", and associated camera model names (e.g. Z5, Z6, Z7, Z8, Z9, Zf, Zfc, etc.) are registered trademarks of Nikon Corporation.\nReferences to these names and marks within this software and documentation are strictly for the purpose of identifying product interoperability and hardware compatibility (nominative fair use), and do not imply any official endorsement or association.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "3. License & Disclaimer of Warranty",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "This software is distributed under the GNU AGPL-3.0 License on an \"AS-IS\" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. In no event shall the authors or contributors be liable for any direct, indirect, or consequential damages arising from the use of this software.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "4. Privacy & Zero Tracking",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "This software operates entirely offline with zero cloud telemetry. No personal data, location traces, or device identifiers are collected, uploaded, or transmitted to any third-party server.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
