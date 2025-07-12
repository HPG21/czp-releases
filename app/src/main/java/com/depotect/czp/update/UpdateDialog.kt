package com.depotect.czp.update

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit
) {
    when (updateState) {
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "Проверка обновлений",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Проверяем наличие обновлений...")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                }
            )
        }
        
        is UpdateState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { if (!updateState.updateInfo.isForceUpdate) onDismiss() },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Доступно обновление",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Новая версия: ${updateState.updateInfo.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Размер файла: ${formatFileSize(updateState.updateInfo.fileSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Что нового:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Красивый чейнджлог
                        FormattedChangelog(changelog = updateState.updateInfo.releaseNotes)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обновить")
                    }
                },
                dismissButton = {
                    if (!updateState.updateInfo.isForceUpdate) {
                        TextButton(onClick = onDismiss) {
                            Text("Позже")
                        }
                    }
                }
            )
        }
        
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Нельзя закрыть во время загрузки */ },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Загрузка обновления",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Прогресс бар
                        LinearProgressIndicator(
                            progress = { updateState.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Процент загрузки
                        Text(
                            text = "${updateState.progress}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Статус загрузки
                        Text(
                            text = when {
                                updateState.progress < 25 -> "Подготовка к загрузке..."
                                updateState.progress < 50 -> "Загружаем файл..."
                                updateState.progress < 75 -> "Почти готово..."
                                updateState.progress < 100 -> "Завершение..."
                                else -> "Загрузка завершена"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Анимированные точки
                        Row(
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) { index ->
                                val delay = index * 200
                                val infiniteTransition = rememberInfiniteTransition(label = "dots")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, delayMillis = delay),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "dot$index"
                                )
                                
                                Text(
                                    text = "•",
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { /* Ничего не делаем */ },
                        enabled = false
                    ) {
                        Text("Загрузка...")
                    }
                }
            )
        }
        
        is UpdateState.DownloadComplete -> {
            AlertDialog(
                onDismissRequest = { /* Нельзя закрыть */ },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Загрузка завершена",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Обновление готово к установке",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Нажмите 'Установить' для обновления приложения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onInstall(updateState.file) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Установить")
                    }
                }
            )
        }
        
        is UpdateState.NoUpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Обновлений нет",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "У вас установлена последняя версия приложения",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Проверьте обновления позже",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Ок")
                    }
                }
            )
        }
        
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ошибка",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                text = {
                    Text(
                        text = updateState.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Ок")
                    }
                }
            )
        }
        
        else -> {
            // NoUpdate - ничего не показываем
        }
    }
}

@Composable
fun FormattedChangelog(changelog: String) {
    val sections = parseChangelog(changelog)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        sections.forEach { section ->
            when (section) {
                is ChangelogSection.Header -> {
                    Text(
                        text = section.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }
                is ChangelogSection.SubHeader -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = section.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = section.text,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = section.color
                        )
                    }
                }
                is ChangelogSection.Item -> {
                    Row(
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = section.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is ChangelogSection.Text -> {
                    Text(
                        text = section.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

sealed class ChangelogSection {
    data class Header(val text: String) : ChangelogSection()
    data class SubHeader(val text: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: androidx.compose.ui.graphics.Color) : ChangelogSection()
    data class Item(val text: String) : ChangelogSection()
    data class Text(val text: String) : ChangelogSection()
}

fun parseChangelog(changelog: String): List<ChangelogSection> {
    val sections = mutableListOf<ChangelogSection>()
    val lines = changelog.split("\n")
    
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        
        when {
            // Заголовки (## или ###)
            line.startsWith("## ") -> {
                sections.add(ChangelogSection.Header(line.substring(3)))
            }
            line.startsWith("### ") -> {
                sections.add(ChangelogSection.Header(line.substring(4)))
            }
            // Подзаголовки с эмодзи
            line.contains("🎯") -> {
                sections.add(ChangelogSection.SubHeader(
                    line.replace("🎯", "").trim(),
                    Icons.Default.Star,
                    androidx.compose.ui.graphics.Color(0xFFFFD700)
                ))
            }
            line.contains("📊") -> {
                sections.add(ChangelogSection.SubHeader(
                    line.replace("📊", "").trim(),
                    Icons.Default.Analytics,
                    androidx.compose.ui.graphics.Color(0xFF2196F3)
                ))
            }
            line.contains("📱") -> {
                sections.add(ChangelogSection.SubHeader(
                    line.replace("📱", "").trim(),
                    Icons.Default.Phone,
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                ))
            }
            line.contains("⚙️") -> {
                sections.add(ChangelogSection.SubHeader(
                    line.replace("⚙️", "").trim(),
                    Icons.Default.Settings,
                    androidx.compose.ui.graphics.Color(0xFF9C27B0)
                ))
            }
            line.contains("🔧") -> {
                sections.add(ChangelogSection.SubHeader(
                    line.replace("🔧", "").trim(),
                    Icons.Default.Build,
                    androidx.compose.ui.graphics.Color(0xFFFF9800)
                ))
            }
            line.contains("🐛") -> {
                sections.add(ChangelogSection.SubHeader(
                    line.replace("🐛", "").trim(),
                    Icons.Default.BugReport,
                    androidx.compose.ui.graphics.Color(0xFFF44336)
                ))
            }
            // Элементы списка
            line.startsWith("- ") -> {
                sections.add(ChangelogSection.Item(line.substring(2)))
            }
            line.startsWith("• ") -> {
                sections.add(ChangelogSection.Item(line.substring(2)))
            }
            // Обычный текст
            line.isNotEmpty() -> {
                sections.add(ChangelogSection.Text(line))
            }
        }
        
        i++
    }
    
    return sections
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes Б"
        bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
        else -> "${bytes / (1024 * 1024)} МБ"
    }
} 