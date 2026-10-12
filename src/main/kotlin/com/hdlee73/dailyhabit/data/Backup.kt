package com.hdlee73.dailyhabit.data

import org.json.JSONArray
import org.json.JSONObject

/** 모든 기록을 JSON 파일 하나로 내보내고 되돌린다 (Android 백업 파일과 100% 호환) */
object Backup {
    private const val FORMAT = 1

    data class Summary(val counts: List<Pair<String, Int>>) {
        val text: String get() = counts.filter { it.second > 0 }.joinToString(" · ") { "${it.first} ${it.second}" }.ifEmpty { "기록 없음" }
    }

    private fun JSONObject.longOrNull(name: String): Long? = if (isNull(name)) null else getLong(name)
    private fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else getInt(name)
    private fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> = List(length()) { f(getJSONObject(it)) }
    private fun <T> JSONObject.list(name: String, f: (JSONObject) -> T): List<T> =
        optJSONArray(name)?.mapObjects(f) ?: emptyList()
    private fun <T> Iterable<T>.toJson(f: (T) -> JSONObject) = JSONArray().also { a -> forEach { a.put(f(it)) } }

    suspend fun export(): String {
        val db = AppDatabase.get()
        val settings = AppSettings.default
        val root = JSONObject()
        root.put("app", "DailyHabit")
        root.put("format", FORMAT)
        root.put("appVersion", "1.0.0-win")
        root.put("exportedAt", System.currentTimeMillis())
        val year = java.time.LocalDate.now().year
        root.put(
            "settings",
            JSONObject().put("dailyEnabled", settings.dailyEnabled).put("dailyMinute", settings.dailyMinute)
                .put("readingGoalYear", year).put("readingGoal", settings.readingGoal(year)),
        )

        root.put("todos", db.todoDao.all().toJson {
            JSONObject().put("id", it.id).put("title", it.title).put("done", it.done).put("dueEpochDay", it.dueEpochDay)
                .put("createdAt", it.createdAt).put("priority", it.priority).put("category", it.category).put("memo", it.memo)
                .put("dueMinute", it.dueMinute).put("remind", it.remind).put("completedAt", it.completedAt)
        })
        root.put("routines", db.routineDao.allRoutines().toJson {
            JSONObject().put("id", it.id).put("title", it.title).put("emoji", it.emoji).put("color", it.color).put("days", it.days)
                .put("reminderEnabled", it.reminderEnabled).put("reminderMinute", it.reminderMinute)
                .put("createdEpochDay", it.createdEpochDay).put("archived", it.archived).put("sortOrder", it.sortOrder).put("kind", it.kind)
        })
        root.put("routineChecks", db.routineDao.allChecks().toJson {
            JSONObject().put("routineId", it.routineId).put("epochDay", it.epochDay).put("checkedAt", it.checkedAt)
        })
        root.put("restaurants", db.restaurantDao.all().toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("category", it.category).put("menu", it.menu)
                .put("address", it.address).put("link", it.link).put("rating", it.rating).put("comment", it.comment)
                .put("memo", it.memo).put("tags", it.tags).put("price", it.price).put("wish", it.wish)
                .put("revisit", it.revisit).put("visitCount", it.visitCount).put("lastVisitEpochDay", it.lastVisitEpochDay)
                .put("createdAt", it.createdAt)
        })
        root.put("restaurantVisits", db.visitDao.all().toJson {
            JSONObject().put("id", it.id).put("restaurantId", it.restaurantId).put("epochDay", it.epochDay)
                .put("note", it.note).put("photo", it.photo)
        })
        root.put("diaryEntries", db.diaryDao.all().toJson {
            JSONObject().put("epochDay", it.epochDay).put("text", it.text).put("mood", it.mood).put("updatedAt", it.updatedAt)
        })
        root.put("shoppingItems", db.shoppingDao.all().toJson {
            JSONObject().put("id", it.id).put("name", it.name).put("qty", it.qty).put("category", it.category)
                .put("checked", it.checked).put("createdAt", it.createdAt).put("checkedAt", it.checkedAt)
        })
        root.put("books", db.bookDao.allBooks().toJson {
            JSONObject().put("id", it.id).put("title", it.title).put("author", it.author).put("totalPages", it.totalPages)
                .put("currentPage", it.currentPage).put("status", it.status).put("rating", it.rating)
                .put("startEpochDay", it.startEpochDay).put("endEpochDay", it.endEpochDay).put("review", it.review)
                .put("createdAt", it.createdAt)
        })
        root.put("bookNotes", db.bookDao.allNotes().toJson {
            JSONObject().put("id", it.id).put("bookId", it.bookId).put("text", it.text).put("page", it.page).put("createdAt", it.createdAt)
        })
        return root.toString(2)
    }

    suspend fun inspect(raw: String): Result<Summary> = runCatching {
        val root = JSONObject(raw)
        Summary(
            listOf(
                "할 일" to (root.optJSONArray("todos")?.length() ?: 0),
                "루틴" to (root.optJSONArray("routines")?.length() ?: 0),
                "맛집" to (root.optJSONArray("restaurants")?.length() ?: 0),
                "방문 기록" to (root.optJSONArray("restaurantVisits")?.length() ?: 0),
                "일기" to (root.optJSONArray("diaryEntries")?.length() ?: 0),
                "장보기" to (root.optJSONArray("shoppingItems")?.length() ?: 0),
                "책" to (root.optJSONArray("books")?.length() ?: 0),
            )
        )
    }

    suspend fun restore(raw: String): Result<Unit> = runCatching {
        val root = JSONObject(raw)
        val db = AppDatabase.get()

        val todos = root.list("todos") {
            Todo(
                id = it.getLong("id"),
                title = it.getString("title"),
                done = it.optBoolean("done", false),
                dueEpochDay = it.longOrNull("dueEpochDay"),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                priority = it.optInt("priority", 1),
                category = it.optString("category", ""),
                memo = it.optString("memo", ""),
                dueMinute = it.intOrNull("dueMinute"),
                remind = it.optBoolean("remind", false),
                completedAt = it.longOrNull("completedAt"),
            )
        }

        val routines = root.list("routines") {
            Routine(
                id = it.getLong("id"),
                title = it.getString("title"),
                emoji = it.optString("emoji", "✨"),
                color = it.optInt("color", 0),
                days = it.optInt("days", Routine.ALL_DAYS),
                reminderEnabled = it.optBoolean("reminderEnabled", false),
                reminderMinute = it.optInt("reminderMinute", 8 * 60),
                createdEpochDay = it.optLong("createdEpochDay", java.time.LocalDate.now().toEpochDay()),
                archived = it.optBoolean("archived", false),
                sortOrder = it.optInt("sortOrder", 0),
                kind = it.optInt("kind", 0),
            )
        }

        val checks = root.list("routineChecks") {
            RoutineCheck(
                routineId = it.getLong("routineId"),
                epochDay = it.getLong("epochDay"),
                checkedAt = it.optLong("checkedAt", System.currentTimeMillis()),
            )
        }

        val restaurants = root.list("restaurants") {
            Restaurant(
                id = it.getLong("id"),
                name = it.getString("name"),
                category = it.optString("category", ""),
                menu = it.optString("menu", ""),
                address = it.optString("address", ""),
                link = it.optString("link", ""),
                rating = it.optInt("rating", 0),
                comment = it.optString("comment", ""),
                memo = it.optString("memo", ""),
                tags = it.optString("tags", ""),
                price = it.optInt("price", 0),
                wish = it.optBoolean("wish", false),
                revisit = it.optBoolean("revisit", false),
                visitCount = it.optInt("visitCount", 0),
                lastVisitEpochDay = it.longOrNull("lastVisitEpochDay"),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val visits = root.list("restaurantVisits") {
            RestaurantVisit(
                id = it.optLong("id", 0),
                restaurantId = it.getLong("restaurantId"),
                epochDay = it.getLong("epochDay"),
                note = it.optString("note", ""),
                photo = it.optString("photo", ""),
            )
        }

        val diary = root.list("diaryEntries") {
            DiaryEntry(
                epochDay = it.getLong("epochDay"),
                text = it.getString("text"),
                mood = it.optInt("mood", 0),
                updatedAt = it.optLong("updatedAt", System.currentTimeMillis()),
            )
        }

        val shopping = root.list("shoppingItems") {
            ShoppingItem(
                id = it.getLong("id"),
                name = it.getString("name"),
                qty = it.optString("qty", ""),
                category = it.optString("category", "기타"),
                checked = it.optBoolean("checked", false),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                checkedAt = it.longOrNull("checkedAt"),
            )
        }

        val books = root.list("books") {
            Book(
                id = it.getLong("id"),
                title = it.getString("title"),
                author = it.optString("author", ""),
                totalPages = it.optInt("totalPages", 0),
                currentPage = it.optInt("currentPage", 0),
                status = it.optInt("status", 1),
                rating = it.optInt("rating", 0),
                startEpochDay = it.longOrNull("startEpochDay"),
                endEpochDay = it.longOrNull("endEpochDay"),
                review = it.optString("review", ""),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val notes = root.list("bookNotes") {
            BookNote(
                id = it.getLong("id"),
                bookId = it.getLong("bookId"),
                text = it.getString("text"),
                page = it.optInt("page", 0),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        // DB 갱신
        db.todoDao.clear()
        db.todoDao.insertAll(todos)

        db.routineDao.clearAllChecks()
        db.routineDao.clearRoutines()
        db.routineDao.insertRoutines(routines)
        db.routineDao.insertChecks(checks)

        db.restaurantDao.clear()
        db.restaurantDao.insertAll(restaurants)

        db.visitDao.clear()
        db.visitDao.insertAll(visits)

        db.diaryDao.clear()
        db.diaryDao.insertAll(diary)

        db.shoppingDao.clear()
        db.shoppingDao.insertAll(shopping)

        db.bookDao.clearNotes()
        db.bookDao.clearBooks()
        db.bookDao.insertBooks(books)
        db.bookDao.insertNotes(notes)

        // 설정 복원
        root.optJSONObject("settings")?.let { s ->
            val settings = AppSettings.default
            if (s.has("dailyEnabled")) settings.dailyEnabled = s.getBoolean("dailyEnabled")
            if (s.has("dailyMinute")) settings.dailyMinute = s.getInt("dailyMinute")
            if (s.has("readingGoalYear") && s.has("readingGoal")) {
                settings.setReadingGoal(s.getInt("readingGoalYear"), s.getInt("readingGoal"))
            }
        }
    }
}
