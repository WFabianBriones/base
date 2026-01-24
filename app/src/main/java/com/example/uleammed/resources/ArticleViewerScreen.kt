package com.example.uleammed.resources

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla de lectura de artículo con soporte para:
 * - Markdown renderizado localmente
 * - WebView para artículos externos
 * - Modo de lectura optimizado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleViewerScreen(
    resourceId: String,
    onBack: () -> Unit,
    viewModel: ResourceViewModel = viewModel()
) {
    var resource by remember { mutableStateOf<ResourceItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(ViewMode.LOCAL) } // LOCAL o WEB
    var showWebError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(resourceId) {
        isLoading = true
        resource = viewModel.getResourceById(resourceId)

        // Registrar lectura
        if (resource != null) {
            viewModel.trackReading(resourceId)

            // Decidir modo inicial
            viewMode = if (resource!!.sourceUrl != null) {
                ViewMode.WEB // Si tiene URL, preferir web
            } else {
                ViewMode.LOCAL // Si no, usar renderizado local
            }
        }

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (resource != null) {
                        Text(
                            text = resource!!.type.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (resource != null) {
                        // Toggle modo vista (si tiene URL)
                        if (resource!!.sourceUrl != null) {
                            IconButton(onClick = {
                                viewMode = if (viewMode == ViewMode.LOCAL) {
                                    ViewMode.WEB
                                } else {
                                    ViewMode.LOCAL
                                }
                            }) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.LOCAL) {
                                        Icons.Filled.Language
                                    } else {
                                        Icons.Filled.Article
                                    },
                                    contentDescription = "Cambiar vista"
                                )
                            }
                        }

                        // Favorito
                        IconButton(
                            onClick = {
                                viewModel.toggleFavorite(resourceId)
                                resource = resource?.copy(isFavorite = !resource!!.isFavorite)
                            }
                        ) {
                            Icon(
                                imageVector = if (resource!!.isFavorite) {
                                    Icons.Filled.Bookmark
                                } else {
                                    Icons.Filled.BookmarkBorder
                                },
                                contentDescription = if (resource!!.isFavorite) {
                                    "Quitar de favoritos"
                                } else {
                                    "Añadir a favoritos"
                                },
                                tint = if (resource!!.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        // Compartir
                        IconButton(onClick = { /* TODO: Implementar compartir */ }) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartir")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (resource == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Artículo no encontrado",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onBack) {
                        Text("Volver")
                    }
                }
            }
        } else {
            // Contenido según modo
            when (viewMode) {
                ViewMode.LOCAL -> LocalArticleView(
                    resource = resource!!,
                    modifier = Modifier.padding(paddingValues)
                )
                ViewMode.WEB -> WebArticleView(
                    url = resource!!.sourceUrl ?: "",
                    onError = { showWebError = true },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Dialog de error web
    if (showWebError) {
        AlertDialog(
            onDismissRequest = { showWebError = false },
            icon = { Icon(Icons.Filled.ErrorOutline, contentDescription = null) },
            title = { Text("Error de Conexión") },
            text = { Text("No se pudo cargar el artículo desde la web. Intenta ver la versión local.") },
            confirmButton = {
                Button(onClick = {
                    showWebError = false
                    viewMode = ViewMode.LOCAL
                }) {
                    Text("Ver Versión Local")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebError = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Vista local del artículo (Markdown renderizado)
 */
@Composable
fun LocalArticleView(
    resource: ResourceItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        ArticleHeader(resource = resource)

        // Contenido parseado
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Puntos clave (si existen)
            if (resource.keyPoints.isNotEmpty()) {
                KeyPointsCard(keyPoints = resource.keyPoints)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Contenido Markdown
            MarkdownContent(content = resource.content)

            // Referencias
            if (resource.references.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ReferencesCard(references = resource.references)
            }
        }
    }
}

/**
 * Vista web del artículo (WebView)
 */
@Composable
fun WebArticleView(
    url: String,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            onError()
                        }
                    }

                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading overlay
        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Cargando artículo...")
                    }
                }
            }
        }
    }
}

/**
 * Header del artículo
 */
@Composable
fun ArticleHeader(resource: ResourceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Badge de categoría
        CategoryBadge(category = resource.category)

        // Título
        Text(
            text = resource.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.3
        )

        // Resumen
        Text(
            text = resource.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Metadatos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetadataChip(
                        icon = Icons.Filled.AccessTime,
                        text = resource.readTime
                    )
                    MetadataChip(
                        icon = resource.difficulty.icon,
                        text = resource.difficulty.displayName
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (resource.isPeerReviewed) {
                        MetadataChip(
                            icon = Icons.Filled.Verified,
                            text = "Verificado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    MetadataChip(
                        icon = Icons.Filled.Source,
                        text = resource.source
                    )
                }
            }
        }

        // Tags
        if (resource.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resource.tags.take(5).forEach { tag ->
                    SuggestionChip(
                        onClick = { /* TODO: Buscar por tag */ },
                        label = { Text("#$tag") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Parser simple de Markdown
 */
@Composable
fun MarkdownContent(content: String) {
    val lines = content.lines()

    lines.forEach { line ->
        when {
            // Headers
            line.startsWith("# ") -> {
                Text(
                    text = line.substring(2),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }
            line.startsWith("## ") -> {
                Text(
                    text = line.substring(3),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            line.startsWith("### ") -> {
                Text(
                    text = line.substring(4),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            // Lista con viñetas
            line.startsWith("- ") -> {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("•", modifier = Modifier.padding(top = 2.dp))
                    Text(
                        text = line.substring(2),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Texto en negrita (simplificado)
            line.contains("**") -> {
                val parts = line.split("**")
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    parts.forEachIndexed { index, part ->
                        if (part.isNotEmpty()) {
                            Text(
                                text = part,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (index % 2 == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Línea horizontal
            line.startsWith("---") -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Línea vacía
            line.isBlank() -> {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Texto normal
            else -> {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Card de puntos clave
 */
@Composable
fun KeyPointsCard(keyPoints: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Puntos Clave",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            keyPoints.forEach { point ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Card de referencias
 */
@Composable
fun ReferencesCard(references: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Referencias Científicas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            references.forEach { reference ->
                Text(
                    text = "• $reference",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Modo de visualización
 */
enum class ViewMode {
    LOCAL,  // Renderizado local de Markdown
    WEB     // WebView con URL externa
}