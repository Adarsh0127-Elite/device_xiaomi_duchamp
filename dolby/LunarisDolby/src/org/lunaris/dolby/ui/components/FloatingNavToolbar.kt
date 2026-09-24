/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.utils.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavToolbar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    
    val isHomeSelected = currentRoute == "settings"
    val isEqualizerSelected = currentRoute == "equalizer"
    val isAdvancedSelected = currentRoute == "advanced"
    val isProfilesSelected = currentRoute == "app_profiles"
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onContainerColor = MaterialTheme.colorScheme.onSurface
    
    // Liquid Glass specular refraction border (subtle light reflection along top edge)
    val glassBorderBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.08f)
        ) else listOf(
            Color.White.copy(alpha = 0.90f),
            Color.White.copy(alpha = 0.28f)
        )
    )
    
    // Liquid Glass translucent frosted body
    val glassBackgroundBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(
            Color(0xFF2C2C2E).copy(alpha = 0.75f),
            Color(0xFF1C1C1E).copy(alpha = 0.62f)
        ) else listOf(
            Color.White.copy(alpha = 0.80f),
            Color(0xFFF7ECE5).copy(alpha = 0.65f)
        )
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer Floating Liquid Glass Capsule
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.12f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.18f)
                )
                .clip(CircleShape)
                .background(brush = glassBackgroundBrush, shape = CircleShape)
                .border(
                    width = 1.dp,
                    brush = glassBorderBrush,
                    shape = CircleShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavToolbarItem(
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.home),
                    selected = isHomeSelected,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor,
                    onContainerColor = onContainerColor,
                    isDark = isDark,
                    onClick = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                        }
                        onNavigate("settings")
                    }
                )
                
                NavToolbarItem(
                    icon = Icons.Default.GraphicEq,
                    label = stringResource(R.string.equalizer),
                    selected = isEqualizerSelected,
                    isEqualizer = true,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor,
                    onContainerColor = onContainerColor,
                    isDark = isDark,
                    onClick = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                        }
                        onNavigate("equalizer")
                    }
                )
                
                NavToolbarItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.advanced),
                    selected = isAdvancedSelected,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor,
                    onContainerColor = onContainerColor,
                    isDark = isDark,
                    onClick = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                        }
                        onNavigate("advanced")
                    }
                )
                
                NavToolbarItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = stringResource(R.string.profiles),
                    selected = isProfilesSelected,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor,
                    onContainerColor = onContainerColor,
                    isDark = isDark,
                    onClick = {
                        scope.launch {
                            haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.CLICK)
                        }
                        onNavigate("app_profiles")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavToolbarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    primaryColor: Color,
    onPrimaryColor: Color,
    onContainerColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEqualizer: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Background color animation: smooth transition between primary accent and frosted halo
    val unselectedHaloColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.22f)
    val itemBgColor by animateColorAsState(
        targetValue = if (selected) primaryColor else unselectedHaloColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "nav_item_bg_$label"
    )
    
    val itemContentColor by animateColorAsState(
        targetValue = if (selected) onPrimaryColor else onContainerColor.copy(alpha = 0.85f),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "nav_item_content_$label"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 16.dp else 12.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "nav_item_pad_$label"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(color = itemBgColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
        ) {
            if (isEqualizer) {
                AnimatedEqualizerIconDynamic(
                    modifier = Modifier.semantics {
                        contentDescription = label
                    },
                    color = itemContentColor,
                    size = 22.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = itemContentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    expandFrom = Alignment.Start
                ) + fadeIn(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ),
                exit = shrinkHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                ),
                label = "nav_text_$label"
            ) {
                Text(
                    text = label,
                    color = itemContentColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
