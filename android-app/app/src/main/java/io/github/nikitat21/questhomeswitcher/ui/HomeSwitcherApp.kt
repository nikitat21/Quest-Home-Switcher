package io.github.nikitat21.questhomeswitcher.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nikitat21.questhomeswitcher.BuildConfig
import io.github.nikitat21.questhomeswitcher.R
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironmentSource
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironmentType
import io.github.nikitat21.questhomeswitcher.shell.PrivilegeState

private val Accent = Color(0xFF63DDB7)
private val AccentBlue = Color(0xFF72C7F6)
private val Warning = Color(0xFFF4C66B)
private val Error = Color(0xFFFF8C84)
private val Background = Color(0xFF080C12)
private val Panel = Color(0xFF111821)
private val PanelRaised = Color(0xFF18212D)
private val Divider = Color(0xFF2A3645)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextSecondary = Color(0xFFB5C0CF)

private val AppColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF07110E),
    secondary = AccentBlue,
    tertiary = Warning,
    background = Background,
    surface = Panel,
    surfaceVariant = PanelRaised,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun HomeSwitcherApp(
    onActivationStarted: () -> Unit = {},
    viewModel: HomeSwitcherViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = AppColors) {
        HomeSwitcherScreen(
            state = state,
            formatSize = viewModel::formatSize,
            showDebugSettings = BuildConfig.DEBUG,
            onRefresh = viewModel::refresh,
            onOpenDebugSettings = viewModel::openMetaDebugSettings,
            onRequestShizuku = viewModel::requestShizukuPermission,
            onSelect = viewModel::select,
            onActivate = {
                viewModel.activateSelected()
                onActivationStarted()
            },
            onRestart = viewModel::restartQuest,
        )
    }
}

@Composable
private fun HomeSwitcherScreen(
    state: HomeSwitcherUiState,
    formatSize: (Long) -> String,
    showDebugSettings: Boolean,
    onRefresh: () -> Unit,
    onOpenDebugSettings: () -> Unit,
    onRequestShizuku: () -> Unit,
    onSelect: (HomeEnvironment) -> Unit,
    onActivate: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D121A),
                        Background,
                    ),
                ),
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppHeader(
            homeCount = state.homes.size,
            isBusy = state.isBusy,
            showDebugSettings = showDebugSettings,
            debugSettingsEnabled = state.canOpenMetaDebugSettings(),
            onRefresh = onRefresh,
            onOpenDebugSettings = onOpenDebugSettings,
        )

        AccessBanner(
            state = state,
            onRefresh = onRefresh,
            onRequestShizuku = onRequestShizuku,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HomeLibraryPane(
                homes = state.homes,
                selected = state.selected,
                activeHome = state.activeHome,
                formatSize = formatSize,
                onSelect = onSelect,
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
            )

            SelectedHomePane(
                state = state,
                formatSize = formatSize,
                onActivate = onActivate,
                onRestart = onRestart,
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun AppHeader(
    homeCount: Int,
    isBusy: Boolean,
    showDebugSettings: Boolean,
    debugSettingsEnabled: Boolean,
    onRefresh: () -> Unit,
    onOpenDebugSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Quest Home Switcher",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = "Choose and apply a Quest Home environment",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        CountBadge(homeCount)
        Spacer(Modifier.width(12.dp))
        if (showDebugSettings) {
            OutlinedButton(
                onClick = onOpenDebugSettings,
                enabled = debugSettingsEnabled,
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .widthIn(min = 170.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Divider),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Debug settings", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(10.dp))
        }
        OutlinedButton(
            onClick = onRefresh,
            enabled = !isBusy,
            modifier = Modifier
                .heightIn(min = 52.dp)
                .widthIn(min = 156.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Divider),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = TextSecondary,
                )
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
            }
            Spacer(Modifier.width(9.dp))
            Text("Refresh homes", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AppMark() {
    Surface(
        modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = Accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.45f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(29.dp),
            )
        }
    }
}

@Composable
private fun CountBadge(homeCount: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PanelRaised,
        border = BorderStroke(1.dp, Divider),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = homeCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (homeCount == 1) "Home" else "Homes",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

private enum class AccessAction {
    REFRESH,
    REQUEST_SHIZUKU,
}

private data class AccessPresentation(
    val title: String,
    val description: String,
    val accent: Color,
    val ready: Boolean,
    val checking: Boolean = false,
    val actionLabel: String? = null,
    val action: AccessAction? = null,
)

private fun HomeSwitcherUiState.accessPresentation(): AccessPresentation = when (privilegeState) {
    PrivilegeState.ROOT -> AccessPresentation(
        title = "Root access ready",
        description = "Homes can be discovered and applied directly on this Quest.",
        accent = Accent,
        ready = true,
    )

    PrivilegeState.READY -> AccessPresentation(
        title = "Shizuku connected",
        description = "The Home Switcher has the access it needs.",
        accent = Accent,
        ready = true,
    )

    PrivilegeState.CHECKING -> AccessPresentation(
        title = "Checking device access",
        description = "Looking for root access or a running Shizuku service...",
        accent = AccentBlue,
        ready = false,
        checking = true,
    )

    PrivilegeState.NOT_INSTALLED -> AccessPresentation(
        title = "Shizuku is not installed",
        description = "Install Shizuku with Quest Shizuku Setup, then check again.",
        accent = Warning,
        ready = false,
        actionLabel = "Check again",
        action = AccessAction.REFRESH,
    )

    PrivilegeState.SERVER_OFFLINE -> AccessPresentation(
        title = "Shizuku is offline",
        description = "Start Shizuku on the headset, then return to this app.",
        accent = Warning,
        ready = false,
        actionLabel = "Open Shizuku",
        action = AccessAction.REQUEST_SHIZUKU,
    )

    PrivilegeState.PERMISSION_REQUIRED -> AccessPresentation(
        title = "Permission required",
        description = "Allow Quest Home Switcher when the Shizuku prompt appears.",
        accent = Warning,
        ready = false,
        actionLabel = "Grant permission",
        action = AccessAction.REQUEST_SHIZUKU,
    )
}

@Composable
private fun AccessBanner(
    state: HomeSwitcherUiState,
    onRefresh: () -> Unit,
    onRequestShizuku: () -> Unit,
) {
    val presentation = state.accessPresentation()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = presentation.accent.copy(alpha = 0.09f),
        ),
        border = BorderStroke(1.dp, presentation.accent.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (presentation.checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = presentation.accent,
                )
            } else {
                Icon(
                    imageVector = if (presentation.ready) Icons.Rounded.Security else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = presentation.accent,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = presentation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            if (presentation.ready) {
                StatusTag(label = "Ready", color = Accent)
            } else if (presentation.actionLabel != null && presentation.action != null) {
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = when (presentation.action) {
                        AccessAction.REFRESH -> onRefresh
                        AccessAction.REQUEST_SHIZUKU -> onRequestShizuku
                    },
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .widthIn(min = 168.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = presentation.accent,
                        contentColor = Color(0xFF111418),
                    ),
                ) {
                    Text(presentation.actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeLibraryPane(
    homes: List<HomeEnvironment>,
    selected: HomeEnvironment?,
    activeHome: HomeEnvironment?,
    formatSize: (Long) -> String,
    onSelect: (HomeEnvironment) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleHomes = remember(homes, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            homes
        } else {
            homes.filter { home ->
                home.displayName.contains(query, ignoreCase = true) ||
                    home.packageName?.contains(query, ignoreCase = true) == true ||
                    home.apkPath.substringAfterLast('/').contains(query, ignoreCase = true)
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Divider),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Home library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "APKs found on this Quest",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Text(
                    text = if (searchQuery.isBlank()) {
                        "${homes.size} found"
                    } else {
                        "${visibleHomes.size} of ${homes.size}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Search by name or package") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                    )
                },
            )

            Spacer(Modifier.height(12.dp))
            if (visibleHomes.isEmpty()) {
                LibraryEmptyState(
                    noHomesAvailable = homes.isEmpty(),
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                ) {
                    items(visibleHomes, key = { it.apkPath }) { home ->
                        HomeListItem(
                            home = home,
                            selected = home.apkPath == selected?.apkPath,
                            active = home.apkPath == activeHome?.apkPath,
                            size = formatSize(home.sizeBytes),
                            onClick = { onSelect(home) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeListItem(
    home: HomeEnvironment,
    selected: Boolean,
    active: Boolean,
    size: String,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) Accent else Divider
    val backgroundColor = if (selected) Accent.copy(alpha = 0.09f) else PanelRaised

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewBox(
                name = home.displayName,
                path = home.previewPath,
                modifier = Modifier.size(84.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = home.displayName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = home.packageName ?: home.apkPath.substringAfterLast('/'),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (active) {
                        StatusTag(label = "Active", color = Accent)
                    } else if (home.installed) {
                        StatusTag(label = "Installed", color = AccentBlue)
                    }
                    StatusTag(
                        label = home.source.displayLabel,
                        color = if (home.source == HomeEnvironmentSource.OFFICIAL_LIBRARY) {
                            Accent
                        } else {
                            AccentBlue
                        },
                    )
                    Text(
                        text = size,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }

            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = Accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectedHomePane(
    state: HomeSwitcherUiState,
    formatSize: (Long) -> String,
    onActivate: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Divider),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Selected Home",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Review the Home before applying it",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                if (state.selected?.apkPath == state.activeHome?.apkPath && state.selected != null) {
                    StatusTag(label = "Currently active", color = Accent)
                }
            }

            val selected = state.selected
            if (selected == null) {
                SelectedHomeEmptyState(Modifier.weight(1f))
            } else {
                SelectedHomePreview(
                    home = selected,
                    active = selected.apkPath == state.activeHome?.apkPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 124.dp),
                )
                HomeMetadata(home = selected, formattedSize = formatSize(selected.sizeBytes))
            }

            OperationStatusCard(state)
            HomeActions(
                state = state,
                onActivate = onActivate,
                onRestart = onRestart,
            )
            TechnicalDetails(log = state.log)
        }
    }
}

@Composable
private fun SelectedHomePreview(
    home: HomeEnvironment,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Divider, RoundedCornerShape(16.dp)),
    ) {
        PreviewBox(
            name = home.displayName,
            path = home.previewPath,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xE610151D),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = if (active) "ACTIVE HOME" else "READY TO APPLY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) Accent else AccentBlue,
            )
            Text(
                text = home.displayName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun HomeMetadata(home: HomeEnvironment, formattedSize: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusTag(label = home.type.displayLabel(), color = AccentBlue)
            if (home.installed) {
                StatusTag(label = "Installed", color = Accent)
            }
            StatusTag(label = home.source.displayLabel, color = AccentBlue)
            Text(
                text = formattedSize,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
        }
        MetadataLine(label = "Package", value = home.packageName ?: "Not installed yet")
        MetadataLine(label = "APK", value = home.apkPath.substringAfterLast('/'))
    }
}

private fun HomeEnvironmentType.displayLabel(): String = when (this) {
    HomeEnvironmentType.ENVIRONMENT -> "Environment"
    HomeEnvironmentType.VISTA -> "Vista"
    HomeEnvironmentType.FOOTPRINT -> "Footprint"
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
        )
    }
}

@Composable
private fun OperationStatusCard(state: HomeSwitcherUiState) {
    if (state.message.isBlank() && !state.isBusy) return

    val message = if (state.message.isBlank()) "Working on your request..." else state.message
    val lower = message.lowercase()
    val isProblem = listOf("failed", "offline", "required", "could not", "not installed", "denied")
        .any(lower::contains)
    val isSuccess = lower.startsWith("active:") ||
        lower.contains("ready") ||
        lower.contains("online") ||
        lower.contains("found")
    val accentColor = when {
        state.isBusy -> AccentBlue
        isProblem -> Error
        isSuccess -> Accent
        else -> TextSecondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = accentColor,
                )
            } else {
                Icon(
                    imageVector = if (isProblem) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun HomeActions(
    state: HomeSwitcherUiState,
    onActivate: () -> Unit,
    onRestart: () -> Unit,
) {
    val accessReady = state.rootReady || state.shizukuReady

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onActivate,
            enabled = state.selected != null && accessReady && !state.isBusy,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 62.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color(0xFF07110E),
                disabledContainerColor = Color(0xFF303A46),
                disabledContentColor = Color(0xFF929EAC),
            ),
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFF07110E),
                )
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = if (state.isBusy) "Applying Home..." else "Apply Home",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (state.showRestartAction) {
            OutlinedButton(
                onClick = onRestart,
                enabled = accessReady && !state.isBusy,
                modifier = Modifier
                    .heightIn(min = 62.dp)
                    .widthIn(min = 176.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Warning),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Warning)
                Spacer(Modifier.width(8.dp))
                Text("Restart Quest", color = Warning, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TechnicalDetails(log: String) {
    if (log.isBlank()) return

    var expanded by rememberSaveable(log) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (expanded) "Hide technical details" else "Show technical details",
                fontWeight = FontWeight.SemiBold,
                color = AccentBlue,
            )
        }
        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 96.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF070A0F),
                border = BorderStroke(1.dp, Divider),
            ) {
                SelectionContainer {
                    Text(
                        text = log,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD9E0E9),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun LibraryEmptyState(
    noHomesAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (noHomesAvailable) Icons.Rounded.Folder else Icons.Rounded.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = if (noHomesAvailable) "No Home APKs found" else "No matching Homes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = if (noHomesAvailable) {
                    "Copy Home APK files to Downloads or Quest Homes, then select Refresh homes."
                } else {
                    "Try a different name or package."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun SelectedHomeEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(50.dp),
            )
            Text(
                text = "Select a Home",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Choose an item from the library to review and apply it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PreviewBox(name: String, path: String?, modifier: Modifier) {
    val bitmap = remember(path) {
        path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1B2634),
                        Color(0xFF080C12),
                    ),
                ),
            )
            .border(1.dp, Divider.copy(alpha = 0.75f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            else -> ThemedHomeArtwork(name)
        }
    }
}

@Composable
private fun ThemedHomeArtwork(name: String) {
    val key = name.lowercase()
    Canvas(Modifier.fillMaxSize()) {
        when {
            key.contains("space") || key.contains("station") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF050A18), Color(0xFF12294A))),
                    size = size,
                )
                drawCircle(Color(0xFF9EDBFF), radius = size.minDimension * 0.14f, center = Offset(size.width * 0.76f, size.height * 0.25f))
                drawCircle(Color(0x66FFFFFF), radius = size.minDimension * 0.035f, center = Offset(size.width * 0.22f, size.height * 0.22f))
                drawCircle(Color(0x88FFFFFF), radius = size.minDimension * 0.025f, center = Offset(size.width * 0.45f, size.height * 0.14f))
                drawCircle(Color(0x77FFFFFF), radius = size.minDimension * 0.02f, center = Offset(size.width * 0.62f, size.height * 0.48f))
                drawRect(Color(0xFFE7F7FF), topLeft = Offset(size.width * 0.28f, size.height * 0.55f), size = Size(size.width * 0.44f, size.height * 0.08f))
                drawRect(Color(0xFF5EE0B8), topLeft = Offset(size.width * 0.46f, size.height * 0.38f), size = Size(size.width * 0.08f, size.height * 0.34f))
                drawCircle(Color(0xFF0A0E14), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.5f, size.height * 0.55f))
                drawLine(Color(0xAA61D9FF), Offset(size.width * 0.15f, size.height * 0.78f), Offset(size.width * 0.85f, size.height * 0.78f), strokeWidth = size.minDimension * 0.025f)
            }

            key.contains("polar") || key.contains("village") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF18305A), Color(0xFF77C7D8), Color(0xFFEAF9FF))),
                    size = size,
                )
                drawLine(Color(0xAA5EE0B8), Offset(0f, size.height * 0.2f), Offset(size.width, size.height * 0.35f), strokeWidth = size.minDimension * 0.06f)
                drawLine(Color(0x887B8DFF), Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.18f), strokeWidth = size.minDimension * 0.045f)
                drawCircle(Color(0xEEFFFFFF), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.2f, size.height * 1.05f))
                drawCircle(Color(0xFFE4F5FF), radius = size.minDimension * 0.5f, center = Offset(size.width * 0.8f, size.height * 1.0f))
                drawRect(Color(0xFF4B2F26), topLeft = Offset(size.width * 0.34f, size.height * 0.55f), size = Size(size.width * 0.28f, size.height * 0.22f))
                drawRect(Color(0xFFF2C94C), topLeft = Offset(size.width * 0.44f, size.height * 0.62f), size = Size(size.width * 0.08f, size.height * 0.08f))
                drawLine(Color.White, Offset(size.width * 0.30f, size.height * 0.56f), Offset(size.width * 0.48f, size.height * 0.42f), strokeWidth = size.minDimension * 0.06f)
                drawLine(Color.White, Offset(size.width * 0.66f, size.height * 0.56f), Offset(size.width * 0.48f, size.height * 0.42f), strokeWidth = size.minDimension * 0.06f)
            }

            key.contains("winter") || key.contains("loft") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF101B36), Color(0xFF415B7D), Color(0xFFD8EBF6))),
                    size = size,
                )
                drawCircle(Color(0x55FFFFFF), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.78f, size.height * 0.22f))
                drawCircle(Color(0xFFEAF7FF), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.18f, size.height * 1.0f))
                drawCircle(Color(0xFFFFFFFF), radius = size.minDimension * 0.5f, center = Offset(size.width * 0.78f, size.height * 1.02f))
                drawRect(Color(0xFF3B261F), topLeft = Offset(size.width * 0.28f, size.height * 0.5f), size = Size(size.width * 0.42f, size.height * 0.26f))
                drawLine(Color(0xFFE6F4FF), Offset(size.width * 0.22f, size.height * 0.51f), Offset(size.width * 0.49f, size.height * 0.32f), strokeWidth = size.minDimension * 0.07f)
                drawLine(Color(0xFFE6F4FF), Offset(size.width * 0.76f, size.height * 0.51f), Offset(size.width * 0.49f, size.height * 0.32f), strokeWidth = size.minDimension * 0.07f)
                drawRect(Color(0xFFFFD36B), topLeft = Offset(size.width * 0.42f, size.height * 0.6f), size = Size(size.width * 0.12f, size.height * 0.12f))
                drawLine(Color(0xFF5EE0B8), Offset(size.width * 0.16f, size.height * 0.82f), Offset(size.width * 0.84f, size.height * 0.82f), strokeWidth = size.minDimension * 0.025f)
            }

            key.contains("dome") || key.contains("environment") -> {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF10152A), Color(0xFF143645), Color(0xFF091014))),
                    size = size,
                )
                val left = Offset(size.width * 0.16f, size.height * 0.72f)
                val top = Offset(size.width * 0.5f, size.height * 0.26f)
                val right = Offset(size.width * 0.84f, size.height * 0.72f)
                drawLine(Color(0xCC5EE0B8), left, top, strokeWidth = size.minDimension * 0.025f)
                drawLine(Color(0xCC5EE0B8), top, right, strokeWidth = size.minDimension * 0.025f)
                drawLine(Color(0x885EE0B8), Offset(size.width * 0.28f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.26f), strokeWidth = size.minDimension * 0.015f)
                drawLine(Color(0x885EE0B8), Offset(size.width * 0.72f, size.height * 0.72f), Offset(size.width * 0.5f, size.height * 0.26f), strokeWidth = size.minDimension * 0.015f)
                drawLine(Color(0x665EE0B8), Offset(size.width * 0.16f, size.height * 0.72f), Offset(size.width * 0.84f, size.height * 0.72f), strokeWidth = size.minDimension * 0.018f)
                drawCircle(Color(0xAAF2C94C), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.5f, size.height * 0.58f))
                drawLine(Color(0x777B8DFF), Offset(0f, size.height * 0.24f), Offset(size.width, size.height * 0.14f), strokeWidth = size.minDimension * 0.04f)
            }

            else -> {
                drawRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF1B2634), Color(0xFF080C12))),
                    size = size,
                )
                drawCircle(Color(0x335EE0B8), radius = size.minDimension * 0.34f, center = Offset(size.width * 0.7f, size.height * 0.25f))
            }
        }
    }
}
