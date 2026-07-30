package com.pixelagent.app

import android.Manifest
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ════════════════════════════════════════════════════════════
//  COLORS
// ════════════════════════════════════════════════════════════

val Black      = Color(0xFF0A0A0A)
val DarkGray   = Color(0xFF141414)
val Gray       = Color(0xFF1E1E1E)
val LightGray  = Color(0xFF2A2A2A)
val Gold       = Color(0xFFFFD700)
val DarkGold   = Color(0xFFB8860B)
val Cyan       = Color(0xFF00E5FF)
val Magenta    = Color(0xFFFF00E5)

// ════════════════════════════════════════════════════════════
//  MAIN ACTIVITY
// ════════════════════════════════════════════════════════════

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContent {
            PixelAgentTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Black) {
                    PixelAgentApp()
                }
            }
        }
    }
}

@Composable
fun PixelAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Black,
            secondary = Cyan,
            background = Black,
            surface = Gray,
            onSurface = Color.White
        ),
        content = content
    )
}

// ════════════════════════════════════════════════════════════
//  VIEW MODEL
// ════════════════════════════════════════════════════════════

class AgentViewModel : ViewModel() {
    private val python = Python.getInstance()
    private val agentModule = python.getModule("agent")
    private val processCommand = agentModule.get("process_command")

    var workingDir by mutableStateOf(Environment.getExternalStorageDirectory().absolutePath + "/PixelAgent")
    var messages by mutableStateOf(listOf<ChatMessage>())
    var isLoading by mutableStateOf(false)
    var isTalking by mutableStateOf(false)
    var currentView by mutableStateOf(View.CHAT)
    var selectedImages by mutableStateOf(setOf<String>())
    var fileList by mutableStateOf(listOf<FileItem>())

    init {
        sendCommand("mkdir", mapOf("path" to "."))
        refreshFileList()
    }

    fun sendCommand(action: String, params: Map<String, String> = emptyMap()): String {
        val cmd = JSONObject().apply {
            put("action", action)
            put("working_dir", workingDir)
            params.forEach { (k, v) -> put(k, v) }
        }
        return processCommand.call(cmd.toString())?.toString() ?: "{}"
    }

    fun refreshFileList() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = sendCommand("list", mapOf("path" to "."))
            val json = JSONObject(result)
            val items = json.optJSONArray("items") ?: JSONArray()
            val files = mutableListOf<FileItem>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                files.add(FileItem(
                    name = item.getString("name"),
                    path = item.getString("path"),
                    isDir = item.getString("type") == "dir",
                    size = item.optLong("size", 0)
                ))
            }
            withContext(Dispatchers.Main) { fileList = files }
        }
    }

    fun sendMessage(text: String) {
        messages = messages + ChatMessage(text, true)
        isLoading = true
        isTalking = true

        viewModelScope.launch(Dispatchers.IO) {
            val response = when {
                text.contains("reverse search", ignoreCase = true) ||
                text.contains("what anime", ignoreCase = true) ||
                text.contains("source", ignoreCase = true) ||
                text.contains("where is this from", ignoreCase = true) -> {
                    val img = fileList.firstOrNull { !it.isDir && isImage(it.name) }
                    if (img != null) {
                        val result = sendCommand("reverse_search", mapOf("image_path" to img.path))
                        formatReverseSearch(JSONObject(result))
                    } else "No images found. Add images to working directory first."
                }
                text.contains("search web", ignoreCase = true) ||
                text.contains("google", ignoreCase = true) ||
                text.contains("look up", ignoreCase = true) -> {
                    val query = text.replace("search web", "")
                        .replace("google", "")
                        .replace("look up", "")
                        .replace("for", "")
                        .trim()
                    val result = sendCommand("web_search", mapOf("query" to query))
                    formatWebSearch(JSONObject(result))
                }
                text.contains("zip", ignoreCase = true) ||
                text.contains("archive", ignoreCase = true) ||
                text.contains("compress", ignoreCase = true) -> {
                    val dir = fileList.firstOrNull { it.isDir }
                    if (dir != null) {
                        val fmt = when {
                            text.contains("cbz", ignoreCase = true) -> "cbz"
                            text.contains("7z", ignoreCase = true) -> "7z"
                            else -> "zip"
                        }
                        val result = sendCommand("archive", mapOf(
                            "source" to dir.path,
                            "name" to (dir.name + "_archive"),
                            "format" to fmt
                        ))
                        val json = JSONObject(result)
                        if (json.has("success")) "Created ${fmt.uppercase()} archive of ${dir.name}"
                        else "Error: ${json.optString("error")}"
                    } else "No folders found to archive."
                }
                text.contains("organize", ignoreCase = true) ||
                text.contains("sort", ignoreCase = true) ||
                text.contains("clean up", ignoreCase = true) -> {
                    val result = sendCommand("organize", mapOf("path" to ".", "by" to "extension"))
                    val json = JSONObject(result)
                    if (json.has("success")) "Files organized into subfolders by extension."
                    else "Error: ${json.optString("error")}"
                }
                text.contains("duplicate", ignoreCase = true) -> {
                    val result = sendCommand("duplicates", mapOf("path" to "."))
                    val json = JSONObject(result)
                    val dups = json.optJSONArray("duplicates")
                    if (dups == null || dups.length() == 0) "No duplicate files found."
                    else {
                        val lines = mutableListOf("Found ${dups.length()} duplicate(s):")
                        for (i in 0 until dups.length()) {
                            val dup = dups.getJSONObject(i)
                            lines.add("  ${dup.optString("file1", "").substringAfterLast("/")} == ${dup.optString("file2", "").substringAfterLast("/")}")
                        }
                        lines.joinToString("
")
                    }
                }
                text.contains("download", ignoreCase = true) -> {
                    val url = text.substringAfter("http").let { if (it.isNotEmpty()) "http$it" else "" }
                    if (url.isNotEmpty()) {
                        val result = sendCommand("download", mapOf("url" to url))
                        val json = JSONObject(result)
                        if (json.has("success")) "Downloaded: ${json.optString("path", "done")} (${formatSize(json.optLong("size", 0))})"
                        else "Error: ${json.optString("error")}"
                    } else "Please provide a URL to download."
                }
                else -> {
                    val result = sendCommand("list")
                    val json = JSONObject(result)
                    val items = json.optJSONArray("items") ?: JSONArray()
                    val lines = mutableListOf("Files in working directory:")
                    for (i in 0 until minOf(items.length(), 20)) {
                        val item = items.getJSONObject(i)
                        val icon = if (item.getString("type") == "dir") "📁" else "📄"
                        val size = if (item.getString("type") == "file") formatSize(item.optLong("size", 0)) else ""
                        lines.add("  $icon ${item.getString("name")} $size")
                    }
                    lines.joinToString("
")
                }
            }

            withContext(Dispatchers.Main) {
                messages = messages + ChatMessage(response, false)
                isLoading = false
                isTalking = false
                refreshFileList()
            }
        }
    }

    fun reverseSearchImage(path: String) {
        isLoading = true
        isTalking = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = sendCommand("reverse_search", mapOf("image_path" to path))
            val response = formatReverseSearch(JSONObject(result))
            withContext(Dispatchers.Main) {
                messages = messages + ChatMessage("🔍 Reverse search result:
$response", false)
                isLoading = false
                isTalking = false
            }
        }
    }

    private fun formatReverseSearch(json: JSONObject): String {
        val sb = StringBuilder()
        val tm = json.optJSONObject("trace_moe")
        if (tm != null && tm.optBoolean("found")) {
            sb.appendLine("🎬 trace.moe (Anime Scene)")
            sb.appendLine("  Title: ${tm.optString("title", "N/A")}")
            if (tm.has("title_english")) sb.appendLine("  English: ${tm.optString("title_english")}")
            sb.appendLine("  Episode: ${tm.optString("episode", "N/A")}")
            sb.appendLine("  Timestamp: ${tm.optString("timestamp", "N/A")}")
            sb.appendLine("  Similarity: ${tm.optString("similarity", "0%")}")
            sb.appendLine()
        } else {
            sb.appendLine("🎬 trace.moe: No anime match found")
            sb.appendLine()
        }
        val sn = json.optJSONObject("saucenao")
        if (sn != null && sn.optBoolean("found")) {
            sb.appendLine("🎨 SauceNAO (Art Source)")
            sb.appendLine("  Title: ${sn.optString("title", "N/A")}")
            sb.appendLine("  Author: ${sn.optString("author", "N/A")}")
            sb.appendLine("  Source: ${sn.optString("source", "N/A")}")
            sb.appendLine("  Similarity: ${sn.optString("similarity", "0%")}")
            if (sn.has("source_url")) sb.appendLine("  URL: ${sn.optString("source_url")}")
            sb.appendLine()
        } else {
            sb.appendLine("🎨 SauceNAO: No art source found")
            sb.appendLine()
        }
        val gi = json.optJSONObject("google")
        if (gi != null) {
            sb.appendLine("🌐 Google Images")
            sb.appendLine("  ${gi.optString("message", "Open Google Lens")}")
            sb.appendLine("  ${gi.optString("lens_url", "https://lens.google.com")}")
        }
        return sb.toString().ifEmpty { "No results found." }
    }

    private fun formatWebSearch(json: JSONObject): String {
        val sb = StringBuilder("🌐 Web Search Results
")
        val results = json.optJSONArray("results")
        if (results == null || results.length() == 0) return "No results found."
        for (i in 0 until minOf(results.length(), 5)) {
            val r = results.getJSONObject(i)
            sb.appendLine()
            sb.appendLine("• ${r.optString("title", "No title")}")
            sb.appendLine("  ${r.optString("url", "")}")
        }
        return sb.toString()
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)
data class FileItem(val name: String, val path: String, val isDir: Boolean, val size: Long)

enum class View { CHAT, FILES, IMAGES, SETTINGS }

fun isImage(name: String): Boolean {
    return name.endsWith(".png", true) || name.endsWith(".jpg", true) ||
           name.endsWith(".jpeg", true) || name.endsWith(".gif", true) ||
           name.endsWith(".webp", true)
}

fun formatSize(size: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var s = size.toDouble()
    for (unit in units) {
        if (s < 1024) return "%.1f%s".format(s, unit)
        s /= 1024
    }
    return "%.1fTB".format(s)
}

// ════════════════════════════════════════════════════════════
//  MAIN APP
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PixelAgentApp(viewModel: AgentViewModel = viewModel()) {
    val context = LocalContext.current
    val storagePermission = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    val writePermission = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    LaunchedEffect(Unit) {
        if (!storagePermission.status.isGranted) storagePermission.launchPermissionRequest()
        if (!writePermission.status.isGranted) writePermission.launchPermissionRequest()
    }

    Scaffold(
        topBar = { PixelAgentTopBar(viewModel) },
        bottomBar = { PixelAgentBottomBar(viewModel) },
        containerColor = Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (viewModel.currentView) {
                View.CHAT -> ChatView(viewModel)
                View.FILES -> FilesView(viewModel)
                View.IMAGES -> ImagesView(viewModel)
                View.SETTINGS -> SettingsView(viewModel)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  TOP BAR with Animated Pixel Agent
// ════════════════════════════════════════════════════════════

@Composable
fun PixelAgentTopBar(viewModel: AgentViewModel) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated Pixel Agent!
                PixelAgent(
                    isTalking = viewModel.isTalking,
                    size = 56,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        "PIXEL AGENT",
                        color = Gold,
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "AI File Manager",
                        color = DarkGold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Black,
            titleContentColor = Gold
        ),
        actions = {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (viewModel.isLoading) Cyan else Color(0xFF00FF00),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.currentView = View.FILES }) {
                Icon(Icons.Default.Folder, "Files", tint = Gold)
            }
            IconButton(onClick = { viewModel.currentView = View.IMAGES }) {
                Icon(Icons.Default.Image, "Images", tint = Gold)
            }
            IconButton(onClick = { viewModel.currentView = View.SETTINGS }) {
                Icon(Icons.Default.Settings, "Settings", tint = Gold)
            }
        }
    )
}

// ════════════════════════════════════════════════════════════
//  BOTTOM BAR
// ════════════════════════════════════════════════════════════

@Composable
fun PixelAgentBottomBar(viewModel: AgentViewModel) {
    var text by remember { mutableStateOf("") }

    BottomAppBar(
        containerColor = DarkGray,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ask me anything...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = LightGray,
                    unfocusedContainerColor = LightGray
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            viewModel.sendMessage(text)
                            text = ""
                        }
                    }
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (text.isNotBlank()) {
                        viewModel.sendMessage(text)
                        text = ""
                    }
                },
                containerColor = Gold,
                contentColor = Black,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, "Send")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  CHAT VIEW
// ════════════════════════════════════════════════════════════

@Composable
fun ChatView(viewModel: AgentViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        reverseLayout = true
    ) {
        if (viewModel.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelAgent(isTalking = true, size = 40)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking...", color = Cyan, fontSize = 14.sp)
                    }
                }
            }
        }
        items(viewModel.messages.reversed()) { msg ->
            ChatBubble(msg)
        }
        if (viewModel.messages.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PixelAgent(isTalking = false, size = 80)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Welcome to Pixel Agent!",
                        color = Gold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ask me to manage files, reverse search images,
or browse the web.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Quick suggestion chips
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip("List my files") { viewModel.sendMessage(it) }
                        SuggestionChip("Organize files") { viewModel.sendMessage(it) }
                        SuggestionChip("Find duplicates") { viewModel.sendMessage(it) }
                        SuggestionChip("Reverse search") { viewModel.sendMessage(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: (String) -> Unit) {
    Surface(
        color = LightGray,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick(text) }
    ) {
        Text(
            text,
            color = Cyan,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val backgroundColor = if (msg.isUser) DarkGold else LightGray
    val textColor = if (msg.isUser) Black else Color.White
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                msg.text,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  FILES VIEW
// ════════════════════════════════════════════════════════════

@Composable
fun FilesView(viewModel: AgentViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            "📁 ${viewModel.workingDir}",
            color = Gold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn {
            items(viewModel.fileList) { file ->
                FileListItem(file, viewModel)
            }
        }
    }
}

@Composable
fun FileListItem(file: FileItem, viewModel: AgentViewModel) {
    val icon = if (file.isDir) "📁" else "📄"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                if (file.isDir) {
                    viewModel.workingDir = file.path
                    viewModel.refreshFileList()
                }
            },
        colors = CardDefaults.cardColors(containerColor = LightGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, color = Color.White, fontSize = 14.sp)
                if (!file.isDir) {
                    Text(formatSize(file.size), color = Color.Gray, fontSize = 12.sp)
                }
            }
            if (!file.isDir && isImage(file.name)) {
                IconButton(onClick = { viewModel.reverseSearchImage(file.path) }) {
                    Icon(Icons.Default.Search, "Search", tint = Cyan)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  IMAGES VIEW
// ════════════════════════════════════════════════════════════

@Composable
fun ImagesView(viewModel: AgentViewModel) {
    val images = viewModel.fileList.filter { isImage(it.name) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🖼 Images (${images.size})",
                color = Gold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (viewModel.selectedImages.isNotEmpty()) {
                Button(
                    onClick = {
                        viewModel.selectedImages.forEach { viewModel.reverseSearchImage(it) }
                        viewModel.selectedImages = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("🔍 Search (${viewModel.selectedImages.size})", color = Black, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (images.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelAgent(isTalking = false, size = 60)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No images found.", color = Color.Gray)
                    Text("Add images to your working directory.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { img ->
                    ImageThumbnail(img, viewModel)
                }
            }
        }
    }
}

@Composable
fun ImageThumbnail(img: FileItem, viewModel: AgentViewModel) {
    val isSelected = viewModel.selectedImages.contains(img.path)
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                viewModel.selectedImages = if (isSelected) {
                    viewModel.selectedImages - img.path
                } else {
                    viewModel.selectedImages + img.path
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkGold else LightGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🖼", fontSize = 32.sp)
                Text(
                    img.name.take(12) + if (img.name.length > 12) "…" else "",
                    color = if (isSelected) Black else Color.Gray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  SETTINGS VIEW
// ════════════════════════════════════════════════════════════

@Composable
fun SettingsView(viewModel: AgentViewModel) {
    var workingDir by remember { mutableStateOf(viewModel.workingDir) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚙ Settings", color = Gold, fontSize = 24.sp)

        OutlinedTextField(
            value = workingDir,
            onValueChange = { workingDir = it },
            label = { Text("Working Directory", color = Gold) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan,
                unfocusedBorderColor = LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.workingDir = workingDir
                viewModel.refreshFileList()
                viewModel.currentView = View.CHAT
            },
            colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply", color = Black)
        }

        Divider(color = LightGray, modifier = Modifier.padding(vertical = 8.dp))

        Text("Quick Actions", color = Gold, fontSize = 18.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("📁 List") { viewModel.sendMessage("List files"); viewModel.currentView = View.CHAT }
            ActionButton("📂 Organize") {
                viewModel.sendCommand("organize", mapOf("path" to ".", "by" to "extension"))
                viewModel.refreshFileList()
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("📦 Zip") { viewModel.sendMessage("Create zip"); viewModel.currentView = View.CHAT }
            ActionButton("🔍 Search") { viewModel.sendMessage("Search web for "); viewModel.currentView = View.CHAT }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("🖼 Images") { viewModel.currentView = View.IMAGES }
            ActionButton("🔁 Duplicates") { viewModel.sendMessage("Find duplicates"); viewModel.currentView = View.CHAT }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Agent preview
        Card(
            colors = CardDefaults.cardColors(containerColor = LightGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your Pixel Agent", color = Gold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                PixelAgent(isTalking = true, size = 100)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bounce • Glow • Blink • Particles", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = LightGray),
        modifier = Modifier.weight(1f)
    ) {
        Text(text, color = Cyan, fontSize = 12.sp)
    }
}
