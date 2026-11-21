package com.waleed.onyx.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** وين العنصر: بالشبكة، بالعمود الجانبي، أو بالشريط السفلي */
object Container {
    const val GRID = 0
    const val SIDE = 1
    const val DOCK = 2
    /** تطبيق شلته من الشاشة: يبقى بكل التطبيقات بس */
    const val HIDDEN = 3
    /** داخل مجلد: page = رقم المجلد، x = الترتيب داخله */
    const val FOLDER = 4
}

/** أي شاشة: الخارجية أو الداخلية. كل وحدة لها ترتيب مستقل */
object Screen {
    const val OUTER = 0
    const val INNER = 1
}

object Kind {
    const val APP = 0
    /** ويدجت تطبيق ثاني (بياناته بـ data) */
    const val WIDGET = 1
    /** مجلد (اسمه وشكله بـ data) */
    const val FOLDER = 2
}

/** سطر لكل عنصر على الشاشة */
@Entity(tableName = "items", indices = [Index("screen")])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val screen: Int,
    val container: Int,
    /** الصفحة بالشبكة، أو الترتيب بالعمود والشريط */
    val page: Int,
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
    val kind: Int = Kind.APP,
    /** "الحزمة/الكلاس#رقم المستخدم" */
    val appKey: String = "",
    /** بيانات الويدجت (JSON) لاحقاً */
    val data: String = "",
)

@Dao
abstract class ItemDao {
    @Query("SELECT * FROM items WHERE screen = :screen")
    abstract fun observe(screen: Int): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE screen = :screen")
    abstract suspend fun all(screen: Int): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE screen = :screen")
    abstract suspend fun count(screen: Int): Int

    @Insert
    abstract suspend fun insert(items: List<Item>): List<Long>

    @Update
    abstract suspend fun update(items: List<Item>)

    @Delete
    abstract suspend fun delete(items: List<Item>)

    @Transaction
    open suspend fun apply(insert: List<Item>, update: List<Item>, delete: List<Item>) {
        if (delete.isNotEmpty()) delete(delete)
        if (update.isNotEmpty()) update(update)
        if (insert.isNotEmpty()) insert(insert)
    }
}

@Database(entities = [Item::class], version = 1, exportSchema = false)
abstract class OnyxDb : RoomDatabase() {
    abstract fun items(): ItemDao

    companion object {
        fun create(context: Context): OnyxDb =
            Room.databaseBuilder(context, OnyxDb::class.java, "onyx.db").build()
    }
}
