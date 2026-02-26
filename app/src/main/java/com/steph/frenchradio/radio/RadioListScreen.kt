package com.steph.frenchradio.radio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steph.frenchradio.model.RadioStation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RadioListScreen(
    viewModel: RadioListViewModel,
    onStationClick: (RadioStation) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtered = viewModel.filteredStations()
    val favorites = filtered.filter { it.id in uiState.favorites }
    val nonFavorites = filtered.filter { it.id !in uiState.favorites }

    // Dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<RadioStation?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var stationToDelete by remember { mutableStateOf<RadioStation?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingStation = null
                    showEditDialog = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add station")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search radio...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )

            // Genre filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(uiState.allGenres) { genre ->
                    FilterChip(
                        selected = genre in uiState.selectedGenres,
                        onClick = { viewModel.onGenreToggled(genre) },
                        label = { Text(genre, fontSize = 12.sp) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Station grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Favorites section
                if (favorites.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "★ Favorites",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                        )
                    }
                    items(favorites, key = { it.id }) { station ->
                        StationCard(
                            station = station,
                            isFavorite = true,
                            onClick = { onStationClick(station) },
                            onLongClick = {
                                editingStation = station
                                showEditDialog = true
                            },
                            onFavoriteToggle = { viewModel.onToggleFavorite(station.id) },
                        )
                    }
                }

                // All stations section header
                if (favorites.isNotEmpty() && nonFavorites.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "All Stations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                        )
                    }
                }

                items(nonFavorites, key = { it.id }) { station ->
                    StationCard(
                        station = station,
                        isFavorite = false,
                        onClick = { onStationClick(station) },
                        onLongClick = {
                            editingStation = station
                            showEditDialog = true
                        },
                        onFavoriteToggle = { viewModel.onToggleFavorite(station.id) },
                    )
                }

                // Empty state
                if (filtered.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "No stations found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Edit/Add dialog
    if (showEditDialog) {
        StationEditDialog(
            station = editingStation,
            onDismiss = { showEditDialog = false },
            onSave = { station ->
                if (editingStation != null) {
                    viewModel.updateStation(editingStation!!.id, station)
                } else {
                    viewModel.addStation(station)
                }
                showEditDialog = false
            },
            onDelete = if (editingStation != null) {
                {
                    stationToDelete = editingStation
                    showEditDialog = false
                    showDeleteConfirm = true
                }
            } else null,
        )
    }

    // Delete confirmation
    if (showDeleteConfirm && stationToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Station") },
            text = { Text("Delete \"${stationToDelete!!.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStation(stationToDelete!!.id)
                        showDeleteConfirm = false
                        stationToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCard(
    station: RadioStation,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onFavoriteToggle: () -> Unit,
) {
    val brandColor = try {
        Color(android.graphics.Color.parseColor(station.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo placeholder: colored circle with initials
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(brandColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = station.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Station name
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Favorite star
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Dialog for adding or editing a radio station.
 */
@Composable
fun StationEditDialog(
    station: RadioStation?,
    onDismiss: () -> Unit,
    onSave: (RadioStation) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val isEditing = station != null
    var name by remember(station) { mutableStateOf(station?.name ?: "") }
    var streamUrl by remember(station) { mutableStateOf(station?.streamUrl ?: "") }
    var genres by remember(station) { mutableStateOf(station?.genres?.joinToString(", ") ?: "") }
    var color by remember(station) { mutableStateOf(station?.color ?: "#2196F3") }
    var nameError by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Station" else "Add Station") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name *") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("Name is required") }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it; urlError = false },
                    label = { Text("Stream URL *") },
                    isError = urlError,
                    supportingText = if (urlError) {{ Text("Valid URL required") }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text("Genres (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (#hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()
                    urlError = streamUrl.isBlank() || !streamUrl.startsWith("http")
                    if (!nameError && !urlError) {
                        val genreList = genres.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val id = station?.id ?: name.lowercase()
                            .replace(Regex("[^a-z0-9]"), "_")
                            .replace(Regex("_+"), "_")
                            .trim('_')
                        onSave(
                            RadioStation(
                                id = id,
                                name = name.trim(),
                                streamUrl = streamUrl.trim(),
                                logo = station?.logo ?: id,
                                genres = genreList,
                                color = color.trim().ifEmpty { "#2196F3" },
                            )
                        )
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
