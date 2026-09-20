package com.linehu.asi.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText
import com.linehu.asi.data.notes.NoteEntity
import com.linehu.asi.data.notes.NotesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NotesBg = Color(0xFF1C1C1E)
private val NotesCard = Color(0xFF2C2C2E)
private val NotesYellow = Color(0xFFFFD60A)

/** Notes app: list + plain editor, persisted through Room. */
@Composable
fun NotesScreen(
    repository: NotesRepository,
    modifier: Modifier = Modifier,
) {
    val notes by repository.allNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    var editingId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(NotesBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        IosStatusBar(contentColor = Color.White)
        val editing = notes.firstOrNull { it.id == editingId }
        if (editingId != null) {
            NoteEditor(
                existing = editing,
                repository = repository,
                onBack = { editingId = null },
            )
        } else {
            NotesList(
                notes = notes,
                onOpen = { editingId = it.id },
                onCreate = { editingId = "new" },
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<NoteEntity>,
    onOpen: (NoteEntity) -> Unit,
    onCreate: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("M月d日", Locale.getDefault()) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            IosText(
                "备忘录",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NotesYellow,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(notes, key = { it.id }) { note ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NotesCard)
                            .clickable { onOpen(note) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        IosText(
                            note.title.ifBlank { "(无标题)" },
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        IosText(
                            text = formatter.format(Date(note.updatedAt)) + "  " +
                                note.body.lineSequence().firstOrNull().orEmpty(),
                            fontSize = 13.sp,
                            color = Color(0x99EBEBF5),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NotesCard)
                .clickable(onClick = onCreate)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = NotesYellow)
                IosText("  新建备忘录", color = NotesYellow, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun NoteEditor(
    existing: NoteEntity?,
    repository: NotesRepository,
    onBack: () -> Unit,
) {
    var body by remember(existing?.id) { mutableStateOf(existing?.body.orEmpty()) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回并保存",
                tint = NotesYellow,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            repository.save(
                                title = "",
                                body = body,
                                existingId = existing?.id,
                            )
                        }
                        onBack()
                    }
                    .padding(8.dp),
            )
            IosText("备忘录", color = NotesYellow, fontSize = 16.sp)
            Spacer(Modifier.padding(8.dp))
        }
        androidx.compose.material3.TextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier.fillMaxSize(),
            placeholder = { IosText("开始书写…", color = Color(0x66EBEBF5)) },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = NotesYellow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}
