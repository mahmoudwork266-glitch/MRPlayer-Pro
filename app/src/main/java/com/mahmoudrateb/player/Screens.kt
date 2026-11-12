package com.mahmoudrateb.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun HomeCard(icon: ImageVector, title: String, sub: String?, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
            if (sub != null) Text(sub, color = Soft, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HomeScreen(vm: MainViewModel, onM3u: () -> Unit, onXtream: () -> Unit, onFile: () -> Unit, onMac: () -> Unit) {
    Column(
        Modifier.fillMaxSize().appBackground().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.size(80.dp))
        Text(stringResource(R.string.app_name), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.developer), color = Soft, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeCard(Icons.Filled.Link, stringResource(R.string.m3u), null, Modifier.weight(1f), onM3u)
            HomeCard(Icons.Filled.VpnKey, stringResource(R.string.xtream), null, Modifier.weight(1f), onXtream)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeCard(Icons.Filled.FolderOpen, stringResource(R.string.playlist), null, Modifier.weight(1f), onFile)
            HomeCard(Icons.Filled.Dns, stringResource(R.string.mac), stringResource(R.string.mac_sub), Modifier.weight(1f), onMac)
        }
        Spacer(Modifier.height(16.dp))
        if (vm.loading) CircularProgressIndicator()
        vm.error?.let { Text(it, color = Color(0xFFFF8A80), textAlign = TextAlign.Center) }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.disclaimer), color = Soft.copy(alpha = 0.7f),
            fontSize = 11.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FormScaffold(
    title: String, vm: MainViewModel, onBack: () -> Unit,
    buttonEnabled: Boolean, onSubmit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(Unit) { vm.error = null }
    Column(Modifier.fillMaxSize().appBackground().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
            Button(
                onClick = onSubmit,
                enabled = buttonEnabled && !vm.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (vm.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.connect))
            }
            vm.error?.let { Text(it, color = Color(0xFFFF8A80)) }
        }
    }
}

@Composable
fun M3uScreen(vm: MainViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    var url by remember { mutableStateOf("") }
    FormScaffold(stringResource(R.string.m3u), vm, onBack, url.isNotBlank(), { vm.loadM3u(url, onDone) }) {
        OutlinedTextField(
            url, { url = it }, label = { Text(stringResource(R.string.m3u_hint)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
    }
}

@Composable
fun XtreamScreen(vm: MainViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    FormScaffold(
        stringResource(R.string.xtream), vm, onBack,
        server.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
        { vm.loadXtream(server, user, pass, onDone) }
    ) {
        OutlinedTextField(
            server, { server = it }, label = { Text(stringResource(R.string.server)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        OutlinedTextField(
            user, { user = it }, label = { Text(stringResource(R.string.username)) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            pass, { pass = it }, label = { Text(stringResource(R.string.password)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
    }
}

@Composable
fun MacScreen(vm: MainViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    var portal by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    FormScaffold(stringResource(R.string.mac), vm, onBack, portal.isNotBlank(), { vm.loadPortal(portal, onDone) }) {
        OutlinedTextField(
            portal, { portal = it }, label = { Text(stringResource(R.string.portal)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.your_mac), color = Soft, fontSize = 12.sp)
                Text(vm.mac, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("mac", vm.mac))
            }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy), tint = Color.White) }
        }
        Text(stringResource(R.string.mac_note), color = Soft, fontSize = 12.sp)
    }
}

private const val FAV = "__fav__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(vm: MainViewModel, onBack: () -> Unit, onPlay: (Channel) -> Unit) {
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<String?>(null) }
    val groups = remember(vm.channels) { vm.channels.map { it.group }.distinct().sorted() }
    val shown = remember(vm.channels, query, group, vm.favorites) {
        vm.channels.filter {
            (group == null || (group == FAV && vm.isFav(it)) || it.group == group) &&
                it.name.contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().appBackground()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            OutlinedTextField(
                query, { query = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(group == null, { group = null }, label = { Text(stringResource(R.string.all)) }) }
            item { FilterChip(group == FAV, { group = FAV }, label = { Text(stringResource(R.string.favorites)) }) }
            items(groups) { g -> FilterChip(group == g, { group = g }, label = { Text(g, maxLines = 1) }) }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_results), color = Soft)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id + it.name }) { ch ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassBg)
                            .clickable { onPlay(ch) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22FFFFFF))) {
                            AsyncImage(ch.logo, null, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ch.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text(ch.group, color = Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { vm.toggleFav(ch) }) {
                            Icon(
                                if (vm.isFav(ch)) Icons.Filled.Star else Icons.Filled.StarBorder,
                                null, tint = if (vm.isFav(ch)) Color(0xFFFFD54F) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
