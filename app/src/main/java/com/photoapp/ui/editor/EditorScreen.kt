package com.photoapp.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.ui.components.AdjustmentSlider
import com.photoapp.ui.components.AspectRatioPreset
import com.photoapp.ui.components.CropControls
import com.photoapp.ui.components.CropOverlay
import com.photoapp.ui.components.FilterSelector
import com.photoapp.util.AiEditingEngine
import com.photoapp.util.ImageUtils
import com.photoapp.util.PhotoFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var selectedAspectRatio by remember { mutableStateOf(AspectRatioPreset.FREE) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show saved notification
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Photo saved successfully!")
            onBack()
        }
    }

    // Show AI error
    LaunchedEffect(uiState.aiError) {
        uiState.aiError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Build combined color matrix for live preview
    val adjustmentMatrix = remember(
        uiState.brightness,
        uiState.contrast,
        uiState.saturation,
        uiState.warmth,
        uiState.selectedFilter
    ) {
        val matrix = ImageUtils.createAdjustmentMatrix(
            brightness = uiState.brightness,
            contrast = uiState.contrast,
            saturation = uiState.saturation,
            warmth = uiState.warmth
        )
        if (uiState.selectedFilter != PhotoFilter.NONE) {
            matrix.postConcat(uiState.selectedFilter.matrix)
        }
        ColorMatrix(matrix.array)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = { viewModel.saveAsCopy() },
                            enabled = uiState.hasChanges
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Save Copy")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Image preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.aiPreviewBitmap != null) {
                    // Show AI-processed preview
                    androidx.compose.foundation.Image(
                        bitmap = uiState.aiPreviewBitmap!!.asImageBitmap(),
                        contentDescription = "AI Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    // Show original with color adjustments
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.photo?.contentUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(adjustmentMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer(
                                rotationZ = uiState.rotationDegrees,
                                scaleX = if (uiState.isFlippedH) -1f else 1f,
                                scaleY = if (uiState.isFlippedV) -1f else 1f
                            )
                    )
                }

                // Crop overlay when in crop mode
                if (uiState.selectedTab == EditorTab.CROP) {
                    CropOverlay()
                }

                // AI processing overlay
                if (uiState.isAiProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (uiState.aiMode) {
                                    AiEditMode.BACKGROUND_BLUR -> "Blurring background…"
                                    AiEditMode.BACKGROUND_REMOVE -> "Removing background…"
                                    AiEditMode.AUTO_ENHANCE -> "Analyzing image…"
                                    AiEditMode.OBJECT_HIGHLIGHT -> "Highlighting subject…"
                                    else -> "Processing…"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // AI Apply/Reset bar when preview is active
            if (uiState.aiPreviewBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearAiPreview() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.applyAiEdit() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply")
                    }
                }
            }

            // Tab bar
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                EditorTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    EditorTab.ADJUST -> "Adjust"
                                    EditorTab.FILTERS -> "Filters"
                                    EditorTab.CROP -> "Crop"
                                    EditorTab.AI -> "✨ AI"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            // Controls area
            AnimatedContent(
                targetState = uiState.selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = "editorTabContent"
            ) { tab ->
                when (tab) {
                    EditorTab.ADJUST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(vertical = 8.dp)
                        ) {
                            AdjustmentSlider(
                                label = "Brightness",
                                value = uiState.brightness,
                                onValueChange = { viewModel.setBrightness(it) },
                                onReset = { viewModel.setBrightness(0f) }
                            )
                            AdjustmentSlider(
                                label = "Contrast",
                                value = uiState.contrast,
                                onValueChange = { viewModel.setContrast(it) },
                                onReset = { viewModel.setContrast(0f) }
                            )
                            AdjustmentSlider(
                                label = "Saturation",
                                value = uiState.saturation,
                                onValueChange = { viewModel.setSaturation(it) },
                                onReset = { viewModel.setSaturation(0f) }
                            )
                            AdjustmentSlider(
                                label = "Warmth",
                                value = uiState.warmth,
                                onValueChange = { viewModel.setWarmth(it) },
                                onReset = { viewModel.setWarmth(0f) }
                            )
                        }
                    }

                    EditorTab.FILTERS -> {
                        FilterSelector(
                            photoUri = uiState.photo?.uri ?: "",
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = { viewModel.selectFilter(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        )
                    }

                    EditorTab.CROP -> {
                        CropControls(
                            selectedAspectRatio = selectedAspectRatio,
                            onAspectRatioSelected = { selectedAspectRatio = it },
                            onRotateLeft = { viewModel.rotateLeft() },
                            onRotateRight = { viewModel.rotateRight() },
                            onFlipHorizontal = { viewModel.flipHorizontal() },
                            onFlipVertical = { viewModel.flipVertical() },
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        )
                    }

                    EditorTab.AI -> {
                        AiControlsPanel(
                            uiState = uiState,
                            onBlurBackground = { viewModel.applyBackgroundBlur() },
                            onRemoveBackground = { viewModel.applyBackgroundRemove() },
                            onAutoEnhance = { viewModel.applyAutoEnhance() },
                            onHighlightSubject = { viewModel.applyObjectHighlight() },
                            onBlurRadiusChange = { viewModel.setBlurRadius(it) },
                            onBgFillChange = { viewModel.setBgFill(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────
// AI CONTROLS PANEL
// ──────────────────────────────────────────────

@Composable
private fun AiControlsPanel(
    uiState: EditorUiState,
    onBlurBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
    onAutoEnhance: () -> Unit,
    onHighlightSubject: () -> Unit,
    onBlurRadiusChange: (Int) -> Unit,
    onBgFillChange: (AiEditingEngine.BackgroundFill) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
    ) {
        // AI Feature Buttons Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AiFeatureCard(
                    icon = Icons.Default.BlurOn,
                    label = "Background\nBlur",
                    isActive = uiState.aiMode == AiEditMode.BACKGROUND_BLUR,
                    isProcessing = uiState.isAiProcessing && uiState.aiMode == AiEditMode.BACKGROUND_BLUR,
                    onClick = onBlurBackground
                )
            }
            item {
                AiFeatureCard(
                    icon = Icons.Default.ContentCut,
                    label = "Remove\nBackground",
                    isActive = uiState.aiMode == AiEditMode.BACKGROUND_REMOVE,
                    isProcessing = uiState.isAiProcessing && uiState.aiMode == AiEditMode.BACKGROUND_REMOVE,
                    onClick = onRemoveBackground
                )
            }
            item {
                AiFeatureCard(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Auto\nEnhance",
                    isActive = uiState.aiMode == AiEditMode.AUTO_ENHANCE,
                    isProcessing = uiState.isAiProcessing && uiState.aiMode == AiEditMode.AUTO_ENHANCE,
                    onClick = onAutoEnhance
                )
            }
            item {
                AiFeatureCard(
                    icon = Icons.Default.ColorLens,
                    label = "Color\nPop",
                    isActive = uiState.aiMode == AiEditMode.OBJECT_HIGHLIGHT,
                    isProcessing = uiState.isAiProcessing && uiState.aiMode == AiEditMode.OBJECT_HIGHLIGHT,
                    onClick = onHighlightSubject
                )
            }
        }

        // Additional controls based on active AI mode
        when (uiState.aiMode) {
            AiEditMode.BACKGROUND_BLUR -> {
                Spacer(modifier = Modifier.height(12.dp))
                // Blur radius slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blur",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(40.dp)
                    )
                    Slider(
                        value = uiState.blurRadius.toFloat(),
                        onValueChange = { onBlurRadiusChange(it.toInt()) },
                        valueRange = 1f..25f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${uiState.blurRadius}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onBlurBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = !uiState.isAiProcessing
                ) {
                    Text("Re-apply with radius ${uiState.blurRadius}")
                }
            }

            AiEditMode.BACKGROUND_REMOVE -> {
                Spacer(modifier = Modifier.height(12.dp))
                // Background fill chooser
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.bgFill == AiEditingEngine.BackgroundFill.TRANSPARENT,
                        onClick = {
                            onBgFillChange(AiEditingEngine.BackgroundFill.TRANSPARENT)
                            onRemoveBackground()
                        },
                        label = { Text("Transparent") }
                    )
                    FilterChip(
                        selected = uiState.bgFill == AiEditingEngine.BackgroundFill.WHITE,
                        onClick = {
                            onBgFillChange(AiEditingEngine.BackgroundFill.WHITE)
                            onRemoveBackground()
                        },
                        label = { Text("White") }
                    )
                    FilterChip(
                        selected = uiState.bgFill == AiEditingEngine.BackgroundFill.BLACK,
                        onClick = {
                            onBgFillChange(AiEditingEngine.BackgroundFill.BLACK)
                            onRemoveBackground()
                        },
                        label = { Text("Black") }
                    )
                }
            }

            else -> { /* No additional controls */ }
        }
    }
}

@Composable
private fun AiFeatureCard(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(85.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
                } else Modifier
            )
            .clickable(enabled = !isProcessing) { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight
        )
    }
}
