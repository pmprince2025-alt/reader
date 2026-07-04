package com.folio.feature.reader

import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
        viewModel.refreshBookmarkState()
    }

    val backgroundColor = when (state.readingMode) {
        ReadingMode.STANDARD -> Color(0xFFFAF7F2)
        ReadingMode.SEPIA -> Color(0xFFF2E8D5)
        ReadingMode.NIGHT -> Color(0xFF141210)
    }

    val textColor = when (state.readingMode) {
        ReadingMode.STANDARD -> Color(0xFF1A1614)
        ReadingMode.SEPIA -> Color(0xFF3A2E22)
        ReadingMode.NIGHT -> Color(0xFFEBE4DA)
    }

    var showTocSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showBookInfoDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uiContext = LocalContext.current
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState(initial = true)
    val hapticsIntensity by viewModel.hapticsIntensity.collectAsState(initial = 1.0f)
    val currentView = LocalView.current
    val reduceMotion = remember {
        try {
            Settings.Global.getFloat(
                uiContext.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val errorMessage = state.errorMessage
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = textColor
                )
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val effectiveTurnMode = if (reduceMotion && state.turnMode == TurnMode.CURL) TurnMode.SLIDE else state.turnMode
                    when (effectiveTurnMode) {
                        TurnMode.CURL -> {
                            PageCurlReader(
                                viewModel = viewModel,
                                currentPage = state.currentPage,
                                pageCount = state.pageCount,
                                backgroundColor = backgroundColor,
                                onChromeToggle = { viewModel.toggleChrome() },
                                onPageChanged = { page ->
                                    if (hapticsEnabled) {
                                        currentView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    viewModel.onPageTurnComplete(page)
                                }
                            )
                        }
                        TurnMode.SLIDE -> {
                            SlideReader(
                                viewModel = viewModel,
                                currentPage = state.currentPage,
                                pageCount = state.pageCount,
                                backgroundColor = backgroundColor,
                                onChromeToggle = { viewModel.toggleChrome() }
                            )
                        }
                        TurnMode.SCROLL -> {
                            ScrollReader(
                                viewModel = viewModel,
                                currentPage = state.currentPage,
                                pageCount = state.pageCount,
                                backgroundColor = backgroundColor,
                                textColor = textColor
                            )
                        }
                    }

                    if (!state.isChromeVisible) {
                        TapZones(
                            onCenterTap = { viewModel.toggleChrome() },
                            onPrevPage = {
                                if (hapticsEnabled) {
                                    currentView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                viewModel.navigateToPage(state.currentPage - 1)
                            },
                            onNextPage = {
                                if (hapticsEnabled) {
                                    currentView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                viewModel.navigateToPage(state.currentPage + 1)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (hapticsEnabled) {
                                    currentView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                viewModel.navigateToPage(state.currentPage - 1)
                            },
                            enabled = state.currentPage > 0
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = backgroundColor.copy(alpha = 0.7f),
                                shadowElevation = 4.dp
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous page",
                                    modifier = Modifier.padding(12.dp),
                                    tint = textColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (hapticsEnabled) {
                                    currentView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                viewModel.navigateToPage(state.currentPage + 1)
                            },
                            enabled = state.currentPage < state.pageCount - 1
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = backgroundColor.copy(alpha = 0.7f),
                                shadowElevation = 4.dp
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next page",
                                    modifier = Modifier.padding(12.dp),
                                    tint = textColor
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.isChromeVisible,
                    enter = if (reduceMotion) fadeIn(animationSpec = tween(180)) else fadeIn() + slideInVertically { -it },
                    exit = if (reduceMotion) fadeOut(animationSpec = tween(180)) else fadeOut() + slideOutVertically { -it }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = state.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = { showTocSheet = true }) {
                                    @Suppress("DEPRECATION") Icon(Icons.Filled.List, "Table of Contents")
                                }
                                IconButton(onClick = { viewModel.toggleBookmark(); viewModel.refreshBookmarkState() }) {
                                    Icon(
                                        if (state.isCurrentPageBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        "Bookmark",
                                        tint = if (state.isCurrentPageBookmarked) MaterialTheme.colorScheme.tertiary else textColor
                                    )
                                }
                                IconButton(onClick = { showMoreSheet = true }) {
                                    Icon(Icons.Filled.MoreVert, "More")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = backgroundColor.copy(alpha = 0.95f),
                                titleContentColor = textColor,
                                navigationIconContentColor = textColor,
                                actionIconContentColor = textColor
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            color = backgroundColor.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                PageScrubber(
                                    currentPage = state.currentPage,
                                    pageCount = state.pageCount,
                                    textColor = textColor,
                                    getThumbnail = { viewModel.getPageBitmap(it) },
                                    onSeek = { viewModel.navigateToPage(it) }
                                )

                                var showBrightness by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { showBookmarksSheet = true }) {
                                        Icon(
                                            Icons.Filled.Bookmarks,
                                            contentDescription = "Bookmarks",
                                            tint = textColor
                                        )
                                    }
                                    IconButton(onClick = { showBrightness = !showBrightness }) {
                                        Icon(
                                            Icons.Filled.BrightnessMedium,
                                            contentDescription = "Brightness",
                                            tint = textColor
                                        )
                                    }
                                }
                                if (showBrightness) {
                                    Slider(
                                        value = state.brightness,
                                        onValueChange = { viewModel.setBrightness(it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = textColor,
                                            activeTrackColor = textColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(state.brightness) {
                    val window = (uiContext as? android.app.Activity)?.window
                    val lp = window?.attributes
                    if (lp != null) {
                        lp.screenBrightness = state.brightness
                        window.attributes = lp
                    }
                }

                LaunchedEffect(state.isChromeVisible) {
                    if (state.isChromeVisible) {
                        delay(if (reduceMotion) 180 else 2500)
                        viewModel.chromeAutoHide()
                    }
                }

                LaunchedEffect(state.turnMode) {
                    if (state.turnMode != TurnMode.CURL || reduceMotion) return@LaunchedEffect
                    var lastFrameTime = withFrameNanos { it }
                    var consecutiveDrops = 0
                    var hasTriggeredFallback = false
                    while (!hasTriggeredFallback) {
                        delay(500)
                        val currentFrameTime = withFrameNanos { it }
                        val fps = 1_000_000_000f / (currentFrameTime - lastFrameTime).coerceAtLeast(1L)
                        lastFrameTime = currentFrameTime
                        if (fps < 30f) {
                            consecutiveDrops++
                            if (consecutiveDrops >= 3) {
                                viewModel.setTurnMode(TurnMode.SLIDE)
                                hasTriggeredFallback = true
                            }
                        } else {
                            consecutiveDrops = 0
                        }
                    }
                }
            }
        }
    }

    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            containerColor = backgroundColor
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (state.tocEntries.isEmpty()) {
                    Text(
                        text = "No table of contents available",
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    state.tocEntries.forEach { entry ->
                        Surface(
                            onClick = {
                                viewModel.navigateToPage(entry.pageIndex)
                                showTocSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (entry.pageIndex == state.currentPage)
                                textColor.copy(alpha = 0.1f)
                            else
                                Color.Transparent
                        ) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = backgroundColor
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Reading Mode
                Text("Reading Mode", style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.readingMode == mode,
                            onClick = { viewModel.setReadingMode(mode) },
                            label = { Text(mode.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Page Turn Mode
                Text("Page Turn", style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TurnMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.turnMode == mode,
                            onClick = { viewModel.setTurnMode(mode) },
                            label = { Text(mode.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Brightness
                Text("Brightness", style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.6f))
                Slider(
                    value = state.brightness,
                    onValueChange = { viewModel.setBrightness(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = textColor.copy(alpha = 0.15f))

                ListItem(
                    headlineContent = { Text("Go to Page") },
                    leadingContent = { Icon(Icons.Filled.Numbers, contentDescription = null, tint = textColor) },
                    modifier = Modifier.combinedClickable(
                        onClick = { showMoreSheet = false; showGoToPageDialog = true }
                    )
                )
                ListItem(
                    headlineContent = { Text("Bookmarks (${state.bookmarks.size})") },
                    leadingContent = { Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = textColor) },
                    modifier = Modifier.combinedClickable(
                        onClick = { showMoreSheet = false; showBookmarksSheet = true }
                    )
                )
                ListItem(
                    headlineContent = { Text("Book Info") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null, tint = textColor) },
                    modifier = Modifier.combinedClickable(
                        onClick = { showMoreSheet = false; showBookInfoDialog = true }
                    )
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showGoToPageDialog) {
        var pageInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGoToPageDialog = false },
            title = { Text("Go to Page", color = textColor) },
            containerColor = backgroundColor,
            text = {
                Column {
                    Text(
                        "Enter page number (1-${state.pageCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            val page = pageInput.toIntOrNull()?.minus(1)
                            if (page != null && page in 0 until state.pageCount) {
                                viewModel.navigateToPage(page)
                                showGoToPageDialog = false
                            }
                        }),
                        singleLine = true,
                        placeholder = { Text("e.g. 42") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val page = pageInput.toIntOrNull()?.minus(1)
                    if (page != null && page in 0 until state.pageCount) {
                        viewModel.navigateToPage(page)
                        showGoToPageDialog = false
                    }
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPageDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            containerColor = backgroundColor
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (state.bookmarks.isEmpty()) {
                    Text(
                        text = "No bookmarks yet. Tap the bookmark icon to add one.",
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    state.bookmarks.forEach { bm ->
                        Surface(
                            onClick = {
                                viewModel.navigateToPage(bm.pageIndex)
                                showBookmarksSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (bm.pageIndex == state.currentPage)
                                textColor.copy(alpha = 0.1f)
                            else
                                Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bm.label ?: "Page ${bm.pageIndex + 1}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColor
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(bm.createdAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteBookmark(bm.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete bookmark", tint = textColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showBookInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBookInfoDialog = false },
            title = { Text("Book Info", color = textColor) },
            containerColor = backgroundColor,
            text = {
                Column {
                    InfoRow("Title", state.title, textColor)
                    InfoRow("Current Page", "${state.currentPage + 1} of ${state.pageCount}", textColor)
                    InfoRow("Reading Progress", "${((state.currentPage + 1).toFloat() / state.pageCount.coerceAtLeast(1) * 100).toInt()}%", textColor)
                    InfoRow("Reading Mode", state.readingMode.name, textColor)
                    InfoRow("Page Turn", state.turnMode.name, textColor)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookInfoDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}

@Composable
private fun TapZones(
    onCenterTap: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.15f)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectTapGestures { onPrevPage() }
                }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.7f)
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures { onCenterTap() }
                }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.15f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectTapGestures { onNextPage() }
                }
        )
    }
}

@Composable
private fun PageScrubber(
    currentPage: Int,
    pageCount: Int,
    textColor: Color,
    getThumbnail: (Int) -> Bitmap?,
    onSeek: (Int) -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    var previewPage by remember { mutableIntStateOf(0) }
    var scrubProgress by remember { mutableFloatStateOf(currentPage.toFloat() / pageCount.coerceAtLeast(1)) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Page ${currentPage + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                text = "of $pageCount",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = scrubProgress,
            onValueChange = { progress ->
                scrubProgress = progress
                previewPage = (progress * (pageCount - 1)).toInt().coerceIn(0, pageCount - 1)
                showPreview = true
            },
            onValueChangeFinished = {
                showPreview = false
                onSeek(previewPage)
            },
            colors = SliderDefaults.colors(
                thumbColor = textColor,
                activeTrackColor = textColor,
                inactiveTrackColor = textColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
        )

        if (showPreview) {
            val thumb = getThumbnail(previewPage)
            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = "Page $previewPage preview",
                    modifier = Modifier
                        .size(width = 80.dp, height = 110.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun PageCurlReader(
    viewModel: ReaderViewModel,
    currentPage: Int,
    pageCount: Int,
    backgroundColor: Color,
    onChromeToggle: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    PageCurlComposable(
        currentPageBitmap = viewModel.getPageBitmap(currentPage),
        nextPageBitmap = viewModel.getPageBitmap((currentPage + 1).coerceAtMost(pageCount - 1)),
        onPageTurn = { direction ->
            val newPage = when (direction) {
                TurnDirection.NEXT -> (currentPage + 1).coerceAtMost(pageCount - 1)
                TurnDirection.PREVIOUS -> (currentPage - 1).coerceAtLeast(0)
            }
            onPageChanged(newPage)
        },
        backgroundColor = backgroundColor
    )
}

enum class TurnDirection { NEXT, PREVIOUS }

@Composable
private fun PageCurlComposable(
    currentPageBitmap: Bitmap?,
    nextPageBitmap: Bitmap?,
    onPageTurn: (TurnDirection) -> Unit,
    backgroundColor: Color
) {
    val curlProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    var dragDirection by remember { mutableStateOf(TurnDirection.NEXT) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var isCornerCurl by remember { mutableStateOf(false) }
    var cornerDragOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos ->
                        scope.launch { curlProgress.snapTo(0f) }
                        dragStart = startPos
                        cornerDragOffset = Offset.Zero
                        val cornerThreshold = 0.15f
                        val nearLeft = startPos.x < size.width * cornerThreshold
                        val nearRight = startPos.x > size.width * (1f - cornerThreshold)
                        val nearTop = startPos.y < size.height * cornerThreshold
                        val nearBottom = startPos.y > size.height * (1f - cornerThreshold)
                        isCornerCurl = (nearLeft || nearRight) && (nearTop || nearBottom)
                    },
                    onDragEnd = {
                        if (curlProgress.value > 0.3f) {
                            onPageTurn(dragDirection)
                        }
                        scope.launch {
                            curlProgress.animateTo(
                                0f,
                                spring(stiffness = 300f, dampingRatio = 0.7f)
                            )
                        }
                        isCornerCurl = false
                        cornerDragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        scope.launch { curlProgress.animateTo(0f) }
                        isCornerCurl = false
                        cornerDragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.x < 0) {
                            dragDirection = TurnDirection.NEXT
                        } else if (dragAmount.x > 0 && curlProgress.value < 0.01f) {
                            dragDirection = TurnDirection.PREVIOUS
                        }
                        if (isCornerCurl) {
                            cornerDragOffset = Offset(
                                cornerDragOffset.x + dragAmount.x,
                                cornerDragOffset.y + dragAmount.y
                            )
                            val dist = kotlin.math.sqrt(
                                cornerDragOffset.x * cornerDragOffset.x + cornerDragOffset.y * cornerDragOffset.y
                            )
                            val maxDist = size.width * 0.5f
                            val newProgress = (dist / maxDist).coerceIn(0f, 1f)
                            scope.launch { curlProgress.snapTo(newProgress) }
                        } else {
                            val delta = if (dragDirection == TurnDirection.NEXT) -dragAmount.x else dragAmount.x
                            val newProgress = (curlProgress.value + delta / size.width)
                                .coerceIn(0f, 1f)
                            scope.launch { curlProgress.snapTo(newProgress) }
                        }
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val progress = curlProgress.value

        if (isCornerCurl && progress > 0.01f) {
            drawCornerCurl(
                w = w, h = h, progress = progress,
                dragStart = dragStart, dragOffset = cornerDragOffset,
                backgroundColor = backgroundColor,
                currentPageBitmap = currentPageBitmap, nextPageBitmap = nextPageBitmap,
                textMeasurer = textMeasurer
            )
        } else {
            drawPageOnCanvas(
                textMeasurer = textMeasurer,
                bitmap = currentPageBitmap, backgroundColor = backgroundColor,
                pageNumber = 1, area = Offset.Zero,
                pageWidth = w / 2f, height = h,
                curlProgress = progress, isLeft = true
            )
            val rightPageWidth = w / 2f * (1f - progress)
            if (rightPageWidth > 0f) {
                drawPageOnCanvas(
                    textMeasurer = textMeasurer,
                    bitmap = nextPageBitmap, backgroundColor = backgroundColor,
                    pageNumber = 2, area = Offset(w / 2f, 0f),
                    pageWidth = w / 2f, height = h,
                    curlProgress = progress, isLeft = false
                )
            }
            if (progress > 0.01f) {
                val foldX = w / 2f + (w / 2f) * (1f - progress)
                val shadowWidth = 30f * progress
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x00000000), Color(0x44000000), Color(0x00000000)),
                        startX = foldX - shadowWidth, endX = foldX + shadowWidth
                    ),
                    topLeft = Offset(foldX - shadowWidth, 0f),
                    size = Size(shadowWidth * 2, h)
                )
                val curlHeight = h * progress * 0.5f
                val curlWidth = (w - foldX) * 0.4f
                val versoPath = Path().apply {
                    moveTo(foldX, 0f)
                    cubicTo(foldX + curlWidth * 0.5f, -curlHeight * 0.3f, foldX + curlWidth, -curlHeight * 0.7f, w, 0f)
                    lineTo(foldX, 0f)
                    close()
                }
                drawPath(path = versoPath, color = Color(0xFFE8D9C4).copy(alpha = 0.7f * progress))
                val shadowPath = Path().apply {
                    moveTo(foldX + curlWidth * 0.3f, -curlHeight * 0.3f)
                    cubicTo(foldX + curlWidth * 1.2f, -curlHeight * 0.8f, w + 20f, curlHeight * 0.5f, foldX + curlWidth * 0.5f, curlHeight * 0.2f)
                    close()
                }
                drawPath(path = shadowPath, color = Color(0x22000000))
            }
        }
    }
}

private fun DrawScope.drawCornerCurl(
    w: Float, h: Float, progress: Float,
    dragStart: Offset, dragOffset: Offset,
    backgroundColor: Color,
    currentPageBitmap: Bitmap?, nextPageBitmap: Bitmap?,
    textMeasurer: TextMeasurer
) {
    val corner = dragStart
    val dx = dragOffset.x
    val dy = dragOffset.y

    if (currentPageBitmap != null) {
        drawImage(
            image = currentPageBitmap.asImageBitmap(),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(w.toInt(), h.toInt())
        )
    } else {
        drawRect(color = backgroundColor, size = Size(w, h))
        val textResult = textMeasurer.measure(
            AnnotatedString("Page 1"),
            style = TextStyle(color = Color(0xFF1A1614), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
        drawText(textLayoutResult = textResult, topLeft = Offset((w - textResult.size.width) / 2f, (h - textResult.size.height) / 2f))
    }

    val foldDist = progress * w * 0.4f
    val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
    val perpAngle = angle + kotlin.math.PI.toFloat() / 2f
    val foldLen = w * 0.35f * progress

    val foldCenter = Offset(corner.x - kotlin.math.cos(angle.toDouble()).toFloat() * foldDist, corner.y - kotlin.math.sin(angle.toDouble()).toFloat() * foldDist)
    val foldStart = Offset(foldCenter.x - kotlin.math.cos(perpAngle.toDouble()).toFloat() * foldLen / 2f, foldCenter.y - kotlin.math.sin(perpAngle.toDouble()).toFloat() * foldLen / 2f)
    val foldEnd = Offset(foldCenter.x + kotlin.math.cos(perpAngle.toDouble()).toFloat() * foldLen / 2f, foldCenter.y + kotlin.math.sin(perpAngle.toDouble()).toFloat() * foldLen / 2f)

    val versoPath = Path().apply {
        moveTo(foldStart.x, foldStart.y)
        cubicTo(foldStart.x + (corner.x - foldStart.x) * 0.4f, foldStart.y + (corner.y - foldStart.y) * 0.3f, foldEnd.x + (corner.x - foldEnd.x) * 0.3f, foldEnd.y + (corner.y - foldEnd.y) * 0.4f, foldEnd.x, foldEnd.y)
        lineTo(corner.x, corner.y)
        close()
    }
    drawPath(path = versoPath, color = Color(0xFFE8D9C4).copy(alpha = 0.7f * progress))

    val shadowExt = 25f * progress
    val shadowPerp = Offset(-kotlin.math.sin(perpAngle.toDouble()).toFloat() * shadowExt, kotlin.math.cos(perpAngle.toDouble()).toFloat() * shadowExt)
    val shadowPath = Path().apply {
        moveTo(foldStart.x, foldStart.y)
        lineTo(foldEnd.x, foldEnd.y)
        lineTo(foldEnd.x + shadowPerp.x, foldEnd.y + shadowPerp.y)
        lineTo(foldStart.x + shadowPerp.x, foldStart.y + shadowPerp.y)
        close()
    }
    drawPath(path = shadowPath, color = Color(0x22000000))
    drawLine(color = Color(0x55FFFFFF).copy(alpha = progress), start = foldStart, end = foldEnd, strokeWidth = 2f)
}

private fun DrawScope.drawPageOnCanvas(
    textMeasurer: TextMeasurer, bitmap: Bitmap?, backgroundColor: Color,
    pageNumber: Int, area: Offset, pageWidth: Float, height: Float,
    curlProgress: Float, isLeft: Boolean
) {
    val clipWidth = if (isLeft) pageWidth else pageWidth * (1f - curlProgress)
    if (clipWidth <= 0f) return

    clipRect(left = area.x, top = area.y, right = area.x + clipWidth, bottom = area.y + height) {
        if (bitmap != null) {
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(area.x.toInt(), area.y.toInt()),
                dstSize = IntSize(pageWidth.toInt(), height.toInt())
            )
        } else {
            drawRect(color = backgroundColor, topLeft = area, size = Size(pageWidth, height))
            val textResult = textMeasurer.measure(
                AnnotatedString("Page $pageNumber"),
                style = TextStyle(color = Color(0xFF1A1614), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            )
            drawText(textLayoutResult = textResult, topLeft = Offset(area.x + (pageWidth - textResult.size.width) / 2f, (height - textResult.size.height) / 2f))
        }
    }
}

@Composable
private fun SlideReader(
    viewModel: ReaderViewModel, currentPage: Int, pageCount: Int,
    backgroundColor: Color, onChromeToggle: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onChromeToggle() } }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale != scale) {
                        val scaleChange = newScale / scale
                        offsetX = (offsetX - centroid.x) * scaleChange + centroid.x
                        offsetY = (offsetY - centroid.y) * scaleChange + centroid.y
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                    scale = newScale
                    viewModel.setZoom(scale)
                    viewModel.setScroll(offsetX, offsetY)
                }
            }
    ) {
        val bitmap = viewModel.getPageBitmap(currentPage)
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${currentPage + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    LaunchedEffect(scale) {
        if (scale <= 1.01f && (offsetX != 0f || offsetY != 0f)) {
            delay(100)
            val startX = offsetX; val startY = offsetY
            val animProgress = Animatable(0f)
            animProgress.animateTo(1f, spring(stiffness = 100f, dampingRatio = 0.8f)) {
                val t = value
                offsetX = startX * (1f - t); offsetY = startY * (1f - t)
                viewModel.setScroll(offsetX, offsetY)
            }
            offsetX = 0f; offsetY = 0f
            viewModel.setScroll(0f, 0f)
        }
    }
}

@Composable
private fun ScrollReader(
    viewModel: ReaderViewModel, currentPage: Int, pageCount: Int,
    backgroundColor: Color, textColor: Color
) {
    val scrollState = rememberScrollState()
    var displayedPage by remember { mutableIntStateOf(currentPage) }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value }
            .collect { scrollOffset ->
                val pageHeight = 2000
                val newPage = (scrollOffset / pageHeight).coerceIn(0, pageCount - 1)
                if (newPage != displayedPage) {
                    displayedPage = newPage
                    viewModel.navigateToPage(newPage)
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
    ) {
        for (i in 0 until pageCount) {
            val bitmap = viewModel.getPageBitmap(i)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${i + 1}",
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(2000.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Page ${i + 1}", color = textColor, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
