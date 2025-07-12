package com.depotect.czp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> ExpandableListSection(
    items: List<T>,
    initiallyExpanded: Boolean = false,
    visibleCount: Int = 5,
    blurHeight: Dp = 40.dp,
    blurRadius: Dp = 12.dp,
    content: @Composable (T) -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val showExpand = items.size > visibleCount
    val displayedItems = if (!expanded && showExpand) items.take(visibleCount) else items
    val blurAlpha by animateFloatAsState(if (!expanded && showExpand) 1f else 0f, label = "blurAlpha")

    Box(modifier = Modifier.fillMaxWidth()) {
        // Основной контент
        Column(modifier = Modifier.fillMaxWidth()) {
            displayedItems.forEach { content(it) }
        }
        
        // Blur и стрелка поверх основного блока
        if (showExpand && !expanded) {
            // Blur эффект, который заходит на данные
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blurHeight)
                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                    .blur(blurRadius)
            ) {}
            
            // Стрелка как продолжение блока
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blurHeight)
                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Показать больше",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Показать еще ${items.size - visibleCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
                    )
                }
            }
            
            // Невидимая кнопка для клика
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blurHeight)
                    .align(Alignment.BottomCenter)
                    .background(Color.Transparent)
                    .clickable { expanded = true }
            ) {}
        }
        
        if (showExpand && expanded) {
            // Кнопка сворачивания
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clickable { expanded = false }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Свернуть",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Свернуть",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
                    )
                }
            }
        }
    }
} 