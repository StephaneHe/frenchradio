package com.steph.frenchradio.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioListScreen(
    viewModel: RadioListViewModel,
    onStationClick: (RadioStation) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtered = viewModel.filteredStations()
    val favorites = filtered.filter { it.id in uiState.favorites }
    val nonFavorites = filtered.filter { it.id !in uiState.favorites }

    Column(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun StationCard(
    station: RadioStation,
    isFavorite: Boolean,
    onClick: () -> Unit,
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
            .clickable { onClick() },
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
