package com.ninebot.control.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AuthorsScreen() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Авторы",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { openUrlSafely(context, "https://t.me/besoro") }
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = "Telegram", fontWeight = FontWeight.Bold)
                        Text(text = "@besoro", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { openUrlSafely(context, "https://www.tiktok.com/@am0k102") }
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(imageVector = Icons.Filled.VideoLibrary, contentDescription = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = "TikTok", fontWeight = FontWeight.Bold)
                        Text(text = "am0k102", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun openUrlSafely(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // На устройстве нет приложения, способного открыть ссылку — молча игнорируем.
    }
}
