package com.hdlee73.dailyhabit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.DayOfWeek

/** SQLite는 작은 정수를 Integer로 돌려주므로 Number로 받아 변환한다 */
private fun ResultSet.longOrNull(col: String): Long? = (getObject(col) as? Number)?.toLong()
private fun ResultSet.intOrNull(col: String): Int? = (getObject(col) as? Number)?.toInt()

// ───────────── 할 일 ─────────────

enum class Priority(val label: String) { LOW("낮음"), NORMAL("보통"), HIGH("높음") }

val TODO_CATEGORIES = listOf("개인", "가정", "일", "신앙", "건강", "쇼핑")

data class Todo(
    val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val dueEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Int = 1,
    val category: String = "",
    val memo: String = "",
    val dueMinute: Int? = null,
    val remind: Boolean = false,
    val completedAt: Long? = null,
) {
    val priorityEnum: Priority get() = Priority.entries.getOrElse(priority) { Priority.NORMAL }
}

interface TodoDao {
    fun observeAll(): Flow<List<Todo>>
    suspend fun all(): List<Todo>
    suspend fun pending(): List<Todo>
    suspend fun insert(todo: Todo): Long
    suspend fun update(todo: Todo)
    suspend fun delete(todo: Todo)
    suspend fun clearDone()
    suspend fun clear()
    suspend fun insertAll(list: List<Todo>)
    suspend fun byId(id: Long): Todo? = all().firstOrNull { it.id == id }
    suspend fun withReminders(): List<Todo> =
        all().filter { !it.done && it.remind && it.dueEpochDay != null && it.dueMinute != null }
}

// ───────────── 매일 루틴 ─────────────

data class Routine(
    val id: Long = 0,
    val title: String,
    val emoji: String = "✨",
    val color: Int = 0,
    val days: Int = ALL_DAYS,
    val reminderEnabled: Boolean = false,
    val reminderMinute: Int = 8 * 60,
    val createdEpochDay: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val kind: Int = 0,
) {
    fun activeOn(dayOfWeek: DayOfWeek): Boolean = days and (1 shl (dayOfWeek.value - 1)) != 0

    companion object {
        const val ALL_DAYS = 0b1111111
    }
}

data class RoutineCheck(
    val routineId: Long,
    val epochDay: Long,
    val checkedAt: Long = System.currentTimeMillis(),
)

interface RoutineDao {
    fun observeActive(): Flow<List<Routine>>
    fun observeChecks(): Flow<List<RoutineCheck>>
    suspend fun active(): List<Routine>
    suspend fun allRoutines(): List<Routine>
    suspend fun allChecks(): List<RoutineCheck>
    suspend fun isChecked(routineId: Long, epochDay: Long): Int
    suspend fun insert(r: Routine): Long
    suspend fun update(r: Routine)
    suspend fun check(c: RoutineCheck)
    suspend fun uncheck(routineId: Long, epochDay: Long)
    suspend fun toggle(routineId: Long, epochDay: Long)
    suspend fun delete(id: Long)
    suspend fun clearRoutines()
    suspend fun clearAllChecks()
    suspend fun insertRoutines(list: List<Routine>)
    suspend fun insertChecks(list: List<RoutineCheck>)
    suspend fun byId(id: Long): Routine? = allRoutines().firstOrNull { it.id == id }
    suspend fun checksOn(epochDay: Long): List<RoutineCheck> = allChecks().filter { it.epochDay == epochDay }
}

// ───────────── 맛집 ─────────────

val RESTAURANT_CATEGORIES = listOf("한식", "중식", "일식", "양식", "아시안", "분식", "고기", "해산물", "카페·디저트", "술집", "기타")
val RESTAURANT_TAGS = listOf("가족", "데이트", "친구", "혼밥", "회식", "접대", "가성비", "분위기", "주차 가능", "아이 동반", "포장·배달")
val RESTAURANT_PRICES = listOf("미정", "1만원 미만", "1~2만원", "2~3만원", "3~5만원", "5만원 이상")

data class Restaurant(
    val id: Long = 0,
    val name: String,
    val category: String = "",
    val menu: String = "",
    val address: String = "",
    val link: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val memo: String = "",
    val tags: String = "",
    val price: Int = 0,
    val wish: Boolean = false,
    val revisit: Boolean = false,
    val visitCount: Int = 0,
    val lastVisitEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val tagList: List<String> get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

interface RestaurantDao {
    fun observeAll(): Flow<List<Restaurant>>
    suspend fun all(): List<Restaurant>
    suspend fun insert(r: Restaurant): Long
    suspend fun update(r: Restaurant)
    suspend fun delete(r: Restaurant)
    suspend fun clear()
    suspend fun insertAll(list: List<Restaurant>)
    suspend fun byId(id: Long): Restaurant? = all().firstOrNull { it.id == id }
}

data class RestaurantVisit(
    val id: Long = 0,
    val restaurantId: Long,
    val epochDay: Long,
    val note: String = "",
    val photo: String = "",
)

interface VisitDao {
    fun observeAll(): Flow<List<RestaurantVisit>>
    suspend fun all(): List<RestaurantVisit>
    suspend fun of(restaurantId: Long): List<RestaurantVisit>
    suspend fun insert(v: RestaurantVisit): Long
    suspend fun update(v: RestaurantVisit)
    suspend fun delete(id: Long)
    suspend fun clear()
    suspend fun insertAll(list: List<RestaurantVisit>)
    suspend fun deleteOf(restaurantId: Long) = of(restaurantId).forEach { delete(it.id) }
}

// ───────────── 한 줄 일기 ─────────────

val DIARY_MOODS = listOf("😊", "🥰", "😌", "😐", "😔", "😤")

data class DiaryEntry(
    val epochDay: Long,
    val text: String,
    val mood: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

interface DiaryDao {
    fun observeAll(): Flow<List<DiaryEntry>>
    suspend fun all(): List<DiaryEntry>
    suspend fun upsert(e: DiaryEntry)
    suspend fun delete(epochDay: Long)
    suspend fun clear()
    suspend fun insertAll(list: List<DiaryEntry>)
}

// ───────────── 장보기 ─────────────

val SHOPPING_CATEGORIES = listOf("채소·과일", "정육·수산", "유제품·달걀", "식료품", "간식·음료", "생활용품", "기타")

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val qty: String = "",
    val category: String = "기타",
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val checkedAt: Long? = null,
)

interface ShoppingDao {
    fun observeAll(): Flow<List<ShoppingItem>>
    suspend fun all(): List<ShoppingItem>
    suspend fun insert(item: ShoppingItem): Long
    suspend fun update(item: ShoppingItem)
    suspend fun delete(item: ShoppingItem)
    suspend fun clearChecked()
    suspend fun uncheckAll()
    suspend fun clear()
    suspend fun insertAll(list: List<ShoppingItem>)
}

// ───────────── 독서 ─────────────

enum class BookStatus(val label: String) { READING("읽는 중"), WANT("읽고 싶은"), DONE("다 읽은") }

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val status: Int = 1,
    val rating: Int = 0,
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val review: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val statusEnum: BookStatus get() = BookStatus.entries.getOrElse(status) { BookStatus.WANT }
    val progress: Float
        get() = when {
            statusEnum == BookStatus.DONE -> 1f
            totalPages > 0 -> (currentPage.toFloat() / totalPages).coerceIn(0f, 1f)
            else -> 0f
        }
}

data class BookNote(
    val id: Long = 0,
    val bookId: Long,
    val text: String,
    val page: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

interface BookDao {
    fun observeBooks(): Flow<List<Book>>
    fun observeNotes(): Flow<List<BookNote>>
    suspend fun allBooks(): List<Book>
    suspend fun allNotes(): List<BookNote>
    suspend fun insert(b: Book): Long
    suspend fun update(b: Book)
    suspend fun delete(id: Long)
    suspend fun insertNote(n: BookNote): Long
    suspend fun deleteNote(id: Long)
    suspend fun clearBooks()
    suspend fun clearNotes()
    suspend fun insertBooks(list: List<Book>)
    suspend fun insertNotes(list: List<BookNote>)
}

// ───────────── SQLite JDBC Database & DAOs ─────────────

class AppDatabase private constructor(private val dbFile: File) {

    private val changeTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    private fun connect(): Connection = DriverManager.getConnection(
        "jdbc:sqlite:${dbFile.absolutePath}",
        java.util.Properties().apply { setProperty("busy_timeout", "5000") },
    )

    init {
        initSchema()
    }

    private fun initSchema() {
        connect().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        done INTEGER NOT NULL DEFAULT 0,
                        dueEpochDay INTEGER,
                        createdAt INTEGER NOT NULL,
                        priority INTEGER NOT NULL DEFAULT 1,
                        category TEXT NOT NULL DEFAULT '',
                        memo TEXT NOT NULL DEFAULT '',
                        dueMinute INTEGER,
                        remind INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        color INTEGER NOT NULL,
                        days INTEGER NOT NULL,
                        reminderEnabled INTEGER NOT NULL,
                        reminderMinute INTEGER NOT NULL,
                        createdEpochDay INTEGER NOT NULL,
                        archived INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        kind INTEGER NOT NULL DEFAULT 0
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS routine_checks (
                        routineId INTEGER NOT NULL,
                        epochDay INTEGER NOT NULL,
                        checkedAt INTEGER NOT NULL,
                        PRIMARY KEY(routineId, epochDay)
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS restaurants (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        menu TEXT NOT NULL,
                        address TEXT NOT NULL,
                        link TEXT NOT NULL,
                        rating INTEGER NOT NULL,
                        comment TEXT NOT NULL,
                        memo TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        wish INTEGER NOT NULL,
                        revisit INTEGER NOT NULL,
                        visitCount INTEGER NOT NULL,
                        lastVisitEpochDay INTEGER,
                        createdAt INTEGER NOT NULL
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS restaurant_visits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        restaurantId INTEGER NOT NULL,
                        epochDay INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        photo TEXT NOT NULL DEFAULT ''
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS diary_entries (
                        epochDay INTEGER PRIMARY KEY,
                        text TEXT NOT NULL,
                        mood INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        qty TEXT NOT NULL,
                        category TEXT NOT NULL,
                        checked INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        checkedAt INTEGER
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        totalPages INTEGER NOT NULL,
                        currentPage INTEGER NOT NULL,
                        status INTEGER NOT NULL,
                        rating INTEGER NOT NULL,
                        startEpochDay INTEGER,
                        endEpochDay INTEGER,
                        review TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    );
                """)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS book_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        bookId INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        page INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    );
                """)
            }
        }
    }

    private fun notifyChanged() {
        changeTrigger.tryEmit(Unit)
    }

    // ── Todo DAO
    val todoDao: TodoDao = object : TodoDao {
        override fun observeAll(): Flow<List<Todo>> = flow {
            emit(all())
            changeTrigger.collect { emit(all()) }
        }

        override suspend fun all(): List<Todo> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery(
                    "SELECT * FROM todos ORDER BY done ASC, CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, priority DESC, createdAt DESC"
                )
                val list = mutableListOf<Todo>()
                while (rs.next()) {
                    list.add(
                        Todo(
                            id = rs.getLong("id"),
                            title = rs.getString("title"),
                            done = rs.getInt("done") == 1,
                            dueEpochDay = rs.longOrNull("dueEpochDay"),
                            createdAt = rs.getLong("createdAt"),
                            priority = rs.getInt("priority"),
                            category = rs.getString("category"),
                            memo = rs.getString("memo"),
                            dueMinute = rs.intOrNull("dueMinute"),
                            remind = rs.getInt("remind") == 1,
                            completedAt = rs.longOrNull("completedAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun pending(): List<Todo> = all().filter { !it.done }

        override suspend fun insert(todo: Todo): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO todos (title, done, dueEpochDay, createdAt, priority, category, memo, dueMinute, remind, completedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setString(1, todo.title)
                ps.setInt(2, if (todo.done) 1 else 0)
                ps.setObject(3, todo.dueEpochDay)
                ps.setLong(4, todo.createdAt)
                ps.setInt(5, todo.priority)
                ps.setString(6, todo.category)
                ps.setString(7, todo.memo)
                ps.setObject(8, todo.dueMinute)
                ps.setInt(9, if (todo.remind) 1 else 0)
                ps.setObject(10, todo.completedAt)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun update(todo: Todo) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "UPDATE todos SET title = ?, done = ?, dueEpochDay = ?, priority = ?, category = ?, memo = ?, dueMinute = ?, remind = ?, completedAt = ? WHERE id = ?"
                )
                ps.setString(1, todo.title)
                ps.setInt(2, if (todo.done) 1 else 0)
                ps.setObject(3, todo.dueEpochDay)
                ps.setInt(4, todo.priority)
                ps.setString(5, todo.category)
                ps.setString(6, todo.memo)
                ps.setObject(7, todo.dueMinute)
                ps.setInt(8, if (todo.remind) 1 else 0)
                ps.setObject(9, todo.completedAt)
                ps.setLong(10, todo.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun delete(todo: Todo) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM todos WHERE id = ?")
                ps.setLong(1, todo.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clearDone() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM todos WHERE done = 1")
                notifyChanged()
            }
        }

        override suspend fun clear() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM todos")
                notifyChanged()
            }
        }

        override suspend fun insertAll(list: List<Todo>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO todos (id, title, done, dueEpochDay, createdAt, priority, category, memo, dueMinute, remind, completedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setString(2, it.title)
                    ps.setInt(3, if (it.done) 1 else 0)
                    ps.setObject(4, it.dueEpochDay)
                    ps.setLong(5, it.createdAt)
                    ps.setInt(6, it.priority)
                    ps.setString(7, it.category)
                    ps.setString(8, it.memo)
                    ps.setObject(9, it.dueMinute)
                    ps.setInt(10, if (it.remind) 1 else 0)
                    ps.setObject(11, it.completedAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    // ── Routine DAO
    val routineDao: RoutineDao = object : RoutineDao {
        override fun observeActive(): Flow<List<Routine>> = flow {
            emit(active())
            changeTrigger.collect { emit(active()) }
        }

        override fun observeChecks(): Flow<List<RoutineCheck>> = flow {
            emit(allChecks())
            changeTrigger.collect { emit(allChecks()) }
        }

        override suspend fun active(): List<Routine> = allRoutines().filter { !it.archived }.sortedWith(compareBy({ it.sortOrder }, { it.id }))

        override suspend fun allRoutines(): List<Routine> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM routines ORDER BY sortOrder ASC, id ASC")
                val list = mutableListOf<Routine>()
                while (rs.next()) {
                    list.add(
                        Routine(
                            id = rs.getLong("id"),
                            title = rs.getString("title"),
                            emoji = rs.getString("emoji"),
                            color = rs.getInt("color"),
                            days = rs.getInt("days"),
                            reminderEnabled = rs.getInt("reminderEnabled") == 1,
                            reminderMinute = rs.getInt("reminderMinute"),
                            createdEpochDay = rs.getLong("createdEpochDay"),
                            archived = rs.getInt("archived") == 1,
                            sortOrder = rs.getInt("sortOrder"),
                            kind = rs.getInt("kind"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun allChecks(): List<RoutineCheck> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM routine_checks")
                val list = mutableListOf<RoutineCheck>()
                while (rs.next()) {
                    list.add(
                        RoutineCheck(
                            routineId = rs.getLong("routineId"),
                            epochDay = rs.getLong("epochDay"),
                            checkedAt = rs.getLong("checkedAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun isChecked(routineId: Long, epochDay: Long): Int = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("SELECT COUNT(*) FROM routine_checks WHERE routineId = ? AND epochDay = ?")
                ps.setLong(1, routineId)
                ps.setLong(2, epochDay)
                val rs = ps.executeQuery()
                if (rs.next()) rs.getInt(1) else 0
            }
        }

        override suspend fun insert(r: Routine): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO routines (title, emoji, color, days, reminderEnabled, reminderMinute, createdEpochDay, archived, sortOrder, kind) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setString(1, r.title)
                ps.setString(2, r.emoji)
                ps.setInt(3, r.color)
                ps.setInt(4, r.days)
                ps.setInt(5, if (r.reminderEnabled) 1 else 0)
                ps.setInt(6, r.reminderMinute)
                ps.setLong(7, r.createdEpochDay)
                ps.setInt(8, if (r.archived) 1 else 0)
                ps.setInt(9, r.sortOrder)
                ps.setInt(10, r.kind)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun update(r: Routine) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("UPDATE routines SET title = ?, emoji = ?, color = ?, days = ?, reminderEnabled = ?, reminderMinute = ?, archived = ?, sortOrder = ?, kind = ? WHERE id = ?")
                ps.setString(1, r.title)
                ps.setString(2, r.emoji)
                ps.setInt(3, r.color)
                ps.setInt(4, r.days)
                ps.setInt(5, if (r.reminderEnabled) 1 else 0)
                ps.setInt(6, r.reminderMinute)
                ps.setInt(7, if (r.archived) 1 else 0)
                ps.setInt(8, r.sortOrder)
                ps.setInt(9, r.kind)
                ps.setLong(10, r.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun check(c: RoutineCheck) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO routine_checks (routineId, epochDay, checkedAt) VALUES (?, ?, ?)")
                ps.setLong(1, c.routineId)
                ps.setLong(2, c.epochDay)
                ps.setLong(3, c.checkedAt)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun uncheck(routineId: Long, epochDay: Long) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM routine_checks WHERE routineId = ? AND epochDay = ?")
                ps.setLong(1, routineId)
                ps.setLong(2, epochDay)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun toggle(routineId: Long, epochDay: Long) {
            if (isChecked(routineId, epochDay) > 0) uncheck(routineId, epochDay)
            else check(RoutineCheck(routineId, epochDay))
        }

        override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps1 = conn.prepareStatement("DELETE FROM routine_checks WHERE routineId = ?")
                ps1.setLong(1, id)
                ps1.executeUpdate()
                val ps2 = conn.prepareStatement("DELETE FROM routines WHERE id = ?")
                ps2.setLong(1, id)
                ps2.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clearRoutines() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM routines")
                notifyChanged()
            }
        }

        override suspend fun clearAllChecks() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM routine_checks")
                notifyChanged()
            }
        }

        override suspend fun insertRoutines(list: List<Routine>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO routines (id, title, emoji, color, days, reminderEnabled, reminderMinute, createdEpochDay, archived, sortOrder, kind) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setString(2, it.title)
                    ps.setString(3, it.emoji)
                    ps.setInt(4, it.color)
                    ps.setInt(5, it.days)
                    ps.setInt(6, if (it.reminderEnabled) 1 else 0)
                    ps.setInt(7, it.reminderMinute)
                    ps.setLong(8, it.createdEpochDay)
                    ps.setInt(9, if (it.archived) 1 else 0)
                    ps.setInt(10, it.sortOrder)
                    ps.setInt(11, it.kind)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }

        override suspend fun insertChecks(list: List<RoutineCheck>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO routine_checks (routineId, epochDay, checkedAt) VALUES (?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.routineId)
                    ps.setLong(2, it.epochDay)
                    ps.setLong(3, it.checkedAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    // ── Restaurant DAO
    val restaurantDao: RestaurantDao = object : RestaurantDao {
        override fun observeAll(): Flow<List<Restaurant>> = flow {
            emit(all())
            changeTrigger.collect { emit(all()) }
        }

        override suspend fun all(): List<Restaurant> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM restaurants ORDER BY createdAt DESC")
                val list = mutableListOf<Restaurant>()
                while (rs.next()) {
                    list.add(
                        Restaurant(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            category = rs.getString("category"),
                            menu = rs.getString("menu"),
                            address = rs.getString("address"),
                            link = rs.getString("link"),
                            rating = rs.getInt("rating"),
                            comment = rs.getString("comment"),
                            memo = rs.getString("memo"),
                            tags = rs.getString("tags"),
                            price = rs.getInt("price"),
                            wish = rs.getInt("wish") == 1,
                            revisit = rs.getInt("revisit") == 1,
                            visitCount = rs.getInt("visitCount"),
                            lastVisitEpochDay = rs.longOrNull("lastVisitEpochDay"),
                            createdAt = rs.getLong("createdAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun insert(r: Restaurant): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO restaurants (name, category, menu, address, link, rating, comment, memo, tags, price, wish, revisit, visitCount, lastVisitEpochDay, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setString(1, r.name)
                ps.setString(2, r.category)
                ps.setString(3, r.menu)
                ps.setString(4, r.address)
                ps.setString(5, r.link)
                ps.setInt(6, r.rating)
                ps.setString(7, r.comment)
                ps.setString(8, r.memo)
                ps.setString(9, r.tags)
                ps.setInt(10, r.price)
                ps.setInt(11, if (r.wish) 1 else 0)
                ps.setInt(12, if (r.revisit) 1 else 0)
                ps.setInt(13, r.visitCount)
                ps.setObject(14, r.lastVisitEpochDay)
                ps.setLong(15, r.createdAt)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun update(r: Restaurant) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("UPDATE restaurants SET name = ?, category = ?, menu = ?, address = ?, link = ?, rating = ?, comment = ?, memo = ?, tags = ?, price = ?, wish = ?, revisit = ?, visitCount = ?, lastVisitEpochDay = ? WHERE id = ?")
                ps.setString(1, r.name)
                ps.setString(2, r.category)
                ps.setString(3, r.menu)
                ps.setString(4, r.address)
                ps.setString(5, r.link)
                ps.setInt(6, r.rating)
                ps.setString(7, r.comment)
                ps.setString(8, r.memo)
                ps.setString(9, r.tags)
                ps.setInt(10, r.price)
                ps.setInt(11, if (r.wish) 1 else 0)
                ps.setInt(12, if (r.revisit) 1 else 0)
                ps.setInt(13, r.visitCount)
                ps.setObject(14, r.lastVisitEpochDay)
                ps.setLong(15, r.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun delete(r: Restaurant) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM restaurants WHERE id = ?")
                ps.setLong(1, r.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clear() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM restaurants")
                notifyChanged()
            }
        }

        override suspend fun insertAll(list: List<Restaurant>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO restaurants (id, name, category, menu, address, link, rating, comment, memo, tags, price, wish, revisit, visitCount, lastVisitEpochDay, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setString(2, it.name)
                    ps.setString(3, it.category)
                    ps.setString(4, it.menu)
                    ps.setString(5, it.address)
                    ps.setString(6, it.link)
                    ps.setInt(7, it.rating)
                    ps.setString(8, it.comment)
                    ps.setString(9, it.memo)
                    ps.setString(10, it.tags)
                    ps.setInt(11, it.price)
                    ps.setInt(12, if (it.wish) 1 else 0)
                    ps.setInt(13, if (it.revisit) 1 else 0)
                    ps.setInt(14, it.visitCount)
                    ps.setObject(15, it.lastVisitEpochDay)
                    ps.setLong(16, it.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    // ── Visit DAO
    val visitDao: VisitDao = object : VisitDao {
        override fun observeAll(): Flow<List<RestaurantVisit>> = flow {
            emit(all())
            changeTrigger.collect { emit(all()) }
        }

        override suspend fun all(): List<RestaurantVisit> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM restaurant_visits ORDER BY epochDay DESC, id DESC")
                val list = mutableListOf<RestaurantVisit>()
                while (rs.next()) {
                    list.add(
                        RestaurantVisit(
                            id = rs.getLong("id"),
                            restaurantId = rs.getLong("restaurantId"),
                            epochDay = rs.getLong("epochDay"),
                            note = rs.getString("note"),
                            photo = rs.getString("photo"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun of(restaurantId: Long): List<RestaurantVisit> = all().filter { it.restaurantId == restaurantId }

        override suspend fun insert(v: RestaurantVisit): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO restaurant_visits (restaurantId, epochDay, note, photo) VALUES (?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setLong(1, v.restaurantId)
                ps.setLong(2, v.epochDay)
                ps.setString(3, v.note)
                ps.setString(4, v.photo)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun update(v: RestaurantVisit) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("UPDATE restaurant_visits SET note = ?, photo = ? WHERE id = ?")
                ps.setString(1, v.note)
                ps.setString(2, v.photo)
                ps.setLong(3, v.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM restaurant_visits WHERE id = ?")
                ps.setLong(1, id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clear() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM restaurant_visits")
                notifyChanged()
            }
        }

        override suspend fun insertAll(list: List<RestaurantVisit>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO restaurant_visits (id, restaurantId, epochDay, note, photo) VALUES (?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setLong(2, it.restaurantId)
                    ps.setLong(3, it.epochDay)
                    ps.setString(4, it.note)
                    ps.setString(5, it.photo)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    // ── Diary DAO
    val diaryDao: DiaryDao = object : DiaryDao {
        override fun observeAll(): Flow<List<DiaryEntry>> = flow {
            emit(all())
            changeTrigger.collect { emit(all()) }
        }

        override suspend fun all(): List<DiaryEntry> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM diary_entries ORDER BY epochDay DESC")
                val list = mutableListOf<DiaryEntry>()
                while (rs.next()) {
                    list.add(
                        DiaryEntry(
                            epochDay = rs.getLong("epochDay"),
                            text = rs.getString("text"),
                            mood = rs.getInt("mood"),
                            updatedAt = rs.getLong("updatedAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun upsert(e: DiaryEntry) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO diary_entries (epochDay, text, mood, updatedAt) VALUES (?, ?, ?, ?)")
                ps.setLong(1, e.epochDay)
                ps.setString(2, e.text)
                ps.setInt(3, e.mood)
                ps.setLong(4, e.updatedAt)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun delete(epochDay: Long) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM diary_entries WHERE epochDay = ?")
                ps.setLong(1, epochDay)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clear() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM diary_entries")
                notifyChanged()
            }
        }

        override suspend fun insertAll(list: List<DiaryEntry>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO diary_entries (epochDay, text, mood, updatedAt) VALUES (?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.epochDay)
                    ps.setString(2, it.text)
                    ps.setInt(3, it.mood)
                    ps.setLong(4, it.updatedAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    // ── Shopping DAO
    val shoppingDao: ShoppingDao = object : ShoppingDao {
        override fun observeAll(): Flow<List<ShoppingItem>> = flow {
            emit(all())
            changeTrigger.collect { emit(all()) }
        }

        override suspend fun all(): List<ShoppingItem> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM shopping_items ORDER BY checked ASC, createdAt DESC")
                val list = mutableListOf<ShoppingItem>()
                while (rs.next()) {
                    list.add(
                        ShoppingItem(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            qty = rs.getString("qty"),
                            category = rs.getString("category"),
                            checked = rs.getInt("checked") == 1,
                            createdAt = rs.getLong("createdAt"),
                            checkedAt = rs.longOrNull("checkedAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun insert(item: ShoppingItem): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO shopping_items (name, qty, category, checked, createdAt, checkedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setString(1, item.name)
                ps.setString(2, item.qty)
                ps.setString(3, item.category)
                ps.setInt(4, if (item.checked) 1 else 0)
                ps.setLong(5, item.createdAt)
                ps.setObject(6, item.checkedAt)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun update(item: ShoppingItem) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("UPDATE shopping_items SET name = ?, qty = ?, category = ?, checked = ?, checkedAt = ? WHERE id = ?")
                ps.setString(1, item.name)
                ps.setString(2, item.qty)
                ps.setString(3, item.category)
                ps.setInt(4, if (item.checked) 1 else 0)
                ps.setObject(5, item.checkedAt)
                ps.setLong(6, item.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun delete(item: ShoppingItem) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM shopping_items WHERE id = ?")
                ps.setLong(1, item.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clearChecked() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM shopping_items WHERE checked = 1")
                notifyChanged()
            }
        }

        override suspend fun uncheckAll() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("UPDATE shopping_items SET checked = 0, checkedAt = NULL")
                notifyChanged()
            }
        }

        override suspend fun clear() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM shopping_items")
                notifyChanged()
            }
        }

        override suspend fun insertAll(list: List<ShoppingItem>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO shopping_items (id, name, qty, category, checked, createdAt, checkedAt) VALUES (?, ?, ?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setString(2, it.name)
                    ps.setString(3, it.qty)
                    ps.setString(4, it.category)
                    ps.setInt(5, if (it.checked) 1 else 0)
                    ps.setLong(6, it.createdAt)
                    ps.setObject(7, it.checkedAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    // ── Book DAO
    val bookDao: BookDao = object : BookDao {
        override fun observeBooks(): Flow<List<Book>> = flow {
            emit(allBooks())
            changeTrigger.collect { emit(allBooks()) }
        }

        override fun observeNotes(): Flow<List<BookNote>> = flow {
            emit(allNotes())
            changeTrigger.collect { emit(allNotes()) }
        }

        override suspend fun allBooks(): List<Book> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM books ORDER BY createdAt DESC")
                val list = mutableListOf<Book>()
                while (rs.next()) {
                    list.add(
                        Book(
                            id = rs.getLong("id"),
                            title = rs.getString("title"),
                            author = rs.getString("author"),
                            totalPages = rs.getInt("totalPages"),
                            currentPage = rs.getInt("currentPage"),
                            status = rs.getInt("status"),
                            rating = rs.getInt("rating"),
                            startEpochDay = rs.longOrNull("startEpochDay"),
                            endEpochDay = rs.longOrNull("endEpochDay"),
                            review = rs.getString("review"),
                            createdAt = rs.getLong("createdAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun allNotes(): List<BookNote> = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val rs = conn.createStatement().executeQuery("SELECT * FROM book_notes ORDER BY createdAt DESC")
                val list = mutableListOf<BookNote>()
                while (rs.next()) {
                    list.add(
                        BookNote(
                            id = rs.getLong("id"),
                            bookId = rs.getLong("bookId"),
                            text = rs.getString("text"),
                            page = rs.getInt("page"),
                            createdAt = rs.getLong("createdAt"),
                        )
                    )
                }
                list
            }
        }

        override suspend fun insert(b: Book): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO books (title, author, totalPages, currentPage, status, rating, startEpochDay, endEpochDay, review, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setString(1, b.title)
                ps.setString(2, b.author)
                ps.setInt(3, b.totalPages)
                ps.setInt(4, b.currentPage)
                ps.setInt(5, b.status)
                ps.setInt(6, b.rating)
                ps.setObject(7, b.startEpochDay)
                ps.setObject(8, b.endEpochDay)
                ps.setString(9, b.review)
                ps.setLong(10, b.createdAt)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun update(b: Book) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("UPDATE books SET title = ?, author = ?, totalPages = ?, currentPage = ?, status = ?, rating = ?, startEpochDay = ?, endEpochDay = ?, review = ? WHERE id = ?")
                ps.setString(1, b.title)
                ps.setString(2, b.author)
                ps.setInt(3, b.totalPages)
                ps.setInt(4, b.currentPage)
                ps.setInt(5, b.status)
                ps.setInt(6, b.rating)
                ps.setObject(7, b.startEpochDay)
                ps.setObject(8, b.endEpochDay)
                ps.setString(9, b.review)
                ps.setLong(10, b.id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps1 = conn.prepareStatement("DELETE FROM book_notes WHERE bookId = ?")
                ps1.setLong(1, id)
                ps1.executeUpdate()
                val ps2 = conn.prepareStatement("DELETE FROM books WHERE id = ?")
                ps2.setLong(1, id)
                ps2.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun insertNote(n: BookNote): Long = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement(
                    "INSERT INTO book_notes (bookId, text, page, createdAt) VALUES (?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )
                ps.setLong(1, n.bookId)
                ps.setString(2, n.text)
                ps.setInt(3, n.page)
                ps.setLong(4, n.createdAt)
                ps.executeUpdate()
                val keys = ps.generatedKeys
                val id = if (keys.next()) keys.getLong(1) else 0L
                notifyChanged()
                id
            }
        }

        override suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                val ps = conn.prepareStatement("DELETE FROM book_notes WHERE id = ?")
                ps.setLong(1, id)
                ps.executeUpdate()
                notifyChanged()
            }
        }

        override suspend fun clearBooks() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM books")
                notifyChanged()
            }
        }

        override suspend fun clearNotes() = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.createStatement().executeUpdate("DELETE FROM book_notes")
                notifyChanged()
            }
        }

        override suspend fun insertBooks(list: List<Book>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO books (id, title, author, totalPages, currentPage, status, rating, startEpochDay, endEpochDay, review, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setString(2, it.title)
                    ps.setString(3, it.author)
                    ps.setInt(4, it.totalPages)
                    ps.setInt(5, it.currentPage)
                    ps.setInt(6, it.status)
                    ps.setInt(7, it.rating)
                    ps.setObject(8, it.startEpochDay)
                    ps.setObject(9, it.endEpochDay)
                    ps.setString(10, it.review)
                    ps.setLong(11, it.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }

        override suspend fun insertNotes(list: List<BookNote>) = withContext(Dispatchers.IO) {
            connect().use { conn ->
                conn.autoCommit = false
                val ps = conn.prepareStatement("INSERT OR REPLACE INTO book_notes (id, bookId, text, page, createdAt) VALUES (?, ?, ?, ?, ?)")
                list.forEach {
                    ps.setLong(1, it.id)
                    ps.setLong(2, it.bookId)
                    ps.setString(3, it.text)
                    ps.setInt(4, it.page)
                    ps.setLong(5, it.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
                conn.commit()
                notifyChanged()
            }
        }
    }

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(): AppDatabase = instance ?: synchronized(this) {
            instance ?: run {
                val file = File(AppDirs.root, "dailyhabit.db")
                AppDatabase(file).also { instance = it }
            }
        }
    }
}
