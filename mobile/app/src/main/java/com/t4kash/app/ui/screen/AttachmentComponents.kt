package com.t4kash.app.ui.screen

import android.app.DownloadManager
import android.content.Context
import android.graphics.BitmapFactory
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t4kash.app.BuildConfig
import com.t4kash.app.ui.formatFileSize
import com.t4kash.app.ui.model.AttachmentDto
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.session.UserSession
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted

@Composable
fun AttachmentPickerSection(
    attachments: List<PendingAttachment>,
    onAttachmentsChange: (List<PendingAttachment>) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String = "Archivos adjuntos"
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val availableSlots = MAX_ATTACHMENTS_PER_ACTION - attachments.size
        if (availableSlots <= 0) {
            onError("Puedes agregar hasta $MAX_ATTACHMENTS_PER_ACTION archivos.")
            return@rememberLauncherForActivityResult
        }
        val selected = mutableListOf<PendingAttachment>()
        for (uri in uris.take(availableSlots)) {
            when (val result = context.readPendingAttachment(uri)) {
                is AttachmentReadResult.Success -> selected += result.attachment
                is AttachmentReadResult.Error -> {
                    onError(result.message)
                    return@rememberLauncherForActivityResult
                }
            }
        }
        onAttachmentsChange(attachments + selected)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = T4Text
            )
            Text(
                text = "PDF, imagen, TXT, Word o ZIP. Maximo 10 MB por archivo.",
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
            attachments.forEachIndexed { index, attachment ->
                PendingAttachmentRow(
                    attachment = attachment,
                    onRemove = {
                        onAttachmentsChange(
                            attachments.filterIndexed { itemIndex, _ ->
                                itemIndex != index
                            }
                        )
                    },
                    enabled = enabled
                )
            }
            OutlinedButton(
                onClick = { launcher.launch(arrayOf("*/*")) },
                enabled = enabled && attachments.size < MAX_ATTACHMENTS_PER_ACTION,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = null
                )
                Text(
                    text = if (attachments.isEmpty()) {
                        "Seleccionar archivos"
                    } else {
                        "Agregar otro archivo"
                    }
                )
            }
        }
    }
}

@Composable
private fun PendingAttachmentRow(
    attachment: PendingAttachment,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    val imagePreview = remember(attachment.content, attachment.mimeType) {
        attachment.content
            .takeIf { attachment.mimeType.startsWith("image/") }
            ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            ?.asImageBitmap()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imagePreview != null) {
            Image(
                bitmap = imagePreview,
                contentDescription = "Vista previa de ${attachment.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Filled.InsertDriveFile,
                contentDescription = null,
                tint = T4Primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = T4Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(attachment.content.size.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
        }
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Quitar ${attachment.name}"
            )
        }
    }
}

@Composable
fun StoredAttachmentRow(
    attachment: AttachmentDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = T4Primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.nombreOriginal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = T4Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(attachment.tamanoBytes),
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )
        }
        IconButton(onClick = { context.downloadAttachment(attachment) }) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Descargar ${attachment.nombreOriginal}"
            )
        }
    }
}

private fun Context.readPendingAttachment(uri: Uri): AttachmentReadResult {
    val metadata = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use(::readMetadata)
        ?: return AttachmentReadResult.Error("No se pudo leer el archivo seleccionado.")

    if (metadata.size > MAX_ATTACHMENT_BYTES) {
        return AttachmentReadResult.Error("${metadata.name} supera el limite de 10 MB.")
    }
    val content = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return AttachmentReadResult.Error("No se pudo abrir ${metadata.name}.")
    if (content.isEmpty()) {
        return AttachmentReadResult.Error("${metadata.name} esta vacio.")
    }
    if (content.size > MAX_ATTACHMENT_BYTES) {
        return AttachmentReadResult.Error("${metadata.name} supera el limite de 10 MB.")
    }
    return AttachmentReadResult.Success(
        PendingAttachment(
            name = metadata.name,
            mimeType = contentResolver.getType(uri) ?: "application/octet-stream",
            content = content
        )
    )
}

private fun readMetadata(cursor: Cursor): AttachmentMetadata? {
    if (!cursor.moveToFirst()) {
        return null
    }
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
        cursor.getLong(sizeIndex)
    } else {
        0L
    }
    return name?.takeIf { it.isNotBlank() }?.let { AttachmentMetadata(it, size) }
}

private fun Context.downloadAttachment(attachment: AttachmentDto) {
    val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
    val request = DownloadManager.Request(Uri.parse(baseUrl + attachment.rutaDescarga))
        .setTitle(attachment.nombreOriginal)
        .setDescription("Descargando archivo de T4KASH")
        .setMimeType(attachment.tipoMime)
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            attachment.nombreOriginal
        )
    UserSession.accessToken?.takeIf { it.isNotBlank() }?.let {
        request.addRequestHeader("Authorization", "Bearer $it")
    }
    val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}

private data class AttachmentMetadata(val name: String, val size: Long)

private sealed interface AttachmentReadResult {
    data class Success(val attachment: PendingAttachment) : AttachmentReadResult
    data class Error(val message: String) : AttachmentReadResult
}

private const val MAX_ATTACHMENTS_PER_ACTION = 3
private const val MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024
