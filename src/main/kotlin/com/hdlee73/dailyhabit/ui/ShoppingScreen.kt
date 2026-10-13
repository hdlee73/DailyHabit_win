package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.AppDatabase
import com.hdlee73.dailyhabit.data.AppSettings
import com.hdlee73.dailyhabit.data.SHOPPING_CATEGORIES
import com.hdlee73.dailyhabit.data.ShoppingItem
import kotlinx.coroutines.launch

/** 처음 쓰는 사람에게 보여줄 자주 사는 품목 */
private val DefaultSuggestions = listOf(
    "우유", "달걀", "두부", "양파", "대파", "감자", "당근", "사과", "바나나", "닭고기", "삼겹살",
    "쌀", "라면", "식빵", "휴지", "세제",
)

private val CategoryKeywords = mapOf(
    "채소·과일" to listOf("양파", "대파", "파", "마늘", "감자", "고구마", "당근", "오이", "호박", "배추", "상추", "시금치", "버섯", "토마토", "사과", "바나나", "귤", "딸기", "포도", "수박", "과일", "채소", "고추", "무"),
    "정육·수산" to listOf("고기", "닭", "소고기", "돼지", "삼겹", "목살", "생선", "고등어", "오징어", "새우", "조개", "햄", "소시지", "베이컨"),
    "유제품·달걀" to listOf("우유", "달걀", "계란", "치즈", "요거트", "요구르트", "버터", "두유"),
    "식료품" to listOf("쌀", "라면", "국수", "파스타", "식빵", "빵", "두부", "김", "참기름", "간장", "고추장", "된장", "소금", "설탕", "기름", "밀가루", "커피", "차", "시리얼", "통조림"),
    "간식·음료" to listOf("과자", "초콜릿", "젤리", "사탕", "음료", "주스", "콜라", "사이다", "맥주", "와인", "아이스크림", "생수", "물"),
    "생활용품" to listOf("휴지", "세제", "샴푸", "린스", "비누", "치약", "칫솔", "수건", "쓰레기봉투", "봉투", "건전지", "물티슈", "섬유유연제", "주방"),
)

private fun guessCategory(name: String): String =
    CategoryKeywords.entries.firstOrNull { (_, words) -> words.any { name.contains(it) } }?.key ?: "기타"

private val QtyPattern = Regex("""^(.*\S)\s+(\d+(?:\.\d+)?\s?[가-힣a-zA-Z]{0,3})$""")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingScreen(openEditor: Boolean = false, onEditorOpened: () -> Unit = {}, modifier: Modifier = Modifier) {
    val dao = remember { AppDatabase.get().shoppingDao }
    val settings = remember { AppSettings.default }
    val items by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(settings.shoppingHistory) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(openEditor) {
        if (openEditor) {
            runCatching { focus.requestFocus() }
            onEditorOpened()
        }
    }

    val todo = items.filter { !it.checked }
    val done = items.filter { it.checked }

    fun add(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        val m = QtyPattern.matchEntire(text)
        val name = m?.groupValues?.get(1) ?: text
        val qty = m?.groupValues?.get(2)?.trim().orEmpty()
        if (todo.any { it.name == name }) {
            Desktop.toast("이미 목록에 있어요")
            return
        }
        scope.launch { dao.insert(ShoppingItem(name = name, qty = qty, category = guessCategory(name))) }
        history = (listOf(name) + history.filter { it != name }).take(30).also { settings.shoppingHistory = it }
        input = ""
    }

    fun shareList() {
        val body = buildString {
            append("🛒 장보기 목록\n")
            SHOPPING_CATEGORIES.forEach { c ->
                val list = todo.filter { it.category == c }
                if (list.isNotEmpty()) {
                    append("\n[$c]\n")
                    list.forEach { append("□ ${it.name}${if (it.qty.isNotBlank()) " ${it.qty}" else ""}\n") }
                }
            }
        }
        Desktop.share(body.trim())
    }

    val onList = todo.map { it.name }.toSet()
    val suggestions = (history + DefaultSuggestions).distinct().filter { it !in onList }.take(12)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("무엇을 살까요?  (예: 우유 2개)") },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add(input) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable { add(input) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Add, "추가", tint = MaterialTheme.colorScheme.surface) }
            }
        }
        if (suggestions.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { s -> SuggestionChip(onClick = { add(s) }, label = { Text("+ $s") }) }
            }
        }

        if (items.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (todo.isEmpty()) "다 담았어요 🎉" else "${done.size}/${items.size} 담았어요",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (todo.isNotEmpty()) IconButton(onClick = ::shareList) { Icon(Icons.Filled.Share, "목록 보내기") }
                }
                LinearProgressIndicator(
                    progress = { if (items.isEmpty()) 0f else done.size.toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }

        if (items.isEmpty()) item {
            EmptyState("🛒", "장보기 목록이 비어 있어요", "위 칸에 품목을 적거나 아래 추천을 눌러 담아 보세요. 마트에서는 체크하면서 장을 볼 수 있어요.")
        }

        SHOPPING_CATEGORIES.forEach { c ->
            val list = todo.filter { it.category == c }
            if (list.isNotEmpty()) item(key = "cat$c") {
                SoftCard {
                    SectionLabel("$c · ${list.size}")
                    list.forEachIndexed { i, it ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ShoppingRow(it, onToggle = { toggle(scope, dao, it) }, onEdit = { editing = it })
                    }
                }
            }
        }

        if (done.isNotEmpty()) item(key = "done") {
            SoftCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("담은 것 · ${done.size}", Modifier.weight(1f))
                    TextButton(onClick = { scope.launch { dao.uncheckAll() } }) { Text("모두 해제") }
                    TextButton(onClick = { scope.launch { dao.clearChecked() } }) { Text("지우기") }
                }
                done.forEachIndexed { i, it ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ShoppingRow(it, onToggle = { toggle(scope, dao, it) }, onEdit = { editing = it })
                }
            }
        }
    }

    editing?.let { item ->
        ShoppingEditDialog(
            item,
            onDismiss = { editing = null },
            onSave = { scope.launch { dao.update(it) }; editing = null },
            onDelete = { scope.launch { dao.delete(item) }; editing = null },
        )
    }
}

private fun toggle(scope: kotlinx.coroutines.CoroutineScope, dao: com.hdlee73.dailyhabit.data.ShoppingDao, item: ShoppingItem) {
    scope.launch {
        dao.update(
            if (item.checked) item.copy(checked = false, checkedAt = null)
            else item.copy(checked = true, checkedAt = System.currentTimeMillis())
        )
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(item.checked, MaterialTheme.colorScheme.primary, onClick = onToggle)
        Text(
            item.name,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            color = if (item.checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        if (item.qty.isNotBlank()) Pill(item.qty, MaterialTheme.colorScheme.secondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingEditDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(item.qty) }
    var category by remember { mutableStateOf(item.category) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("품목 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(qty, { qty = it }, label = { Text("수량·메모") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SHOPPING_CATEGORIES.forEach { c -> FilterChip(category == c, { category = c }, { Text(c) }) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(item.copy(name = name.trim(), qty = qty.trim(), category = category)) }) { Text("저장") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
