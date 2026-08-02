package com.kasiguru.data.local

import androidx.room.TypeConverter
import com.kasiguru.data.local.entity.StoryPage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room type converters for complex data types.
 */
class Converters {

    @TypeConverter
    fun fromStoryPageList(pages: List<StoryPage>): String {
        val jsonArray = JSONArray()
        pages.forEach { page ->
            val obj = JSONObject().apply {
                put("pageNumber", page.pageNumber)
                put("kasiguranin", page.kasiguranin)
                put("tagalog", page.tagalog)
                put("english", page.english)
                put("audioFileName", page.audioFileName)
                put("illustrationDesc", page.illustrationDesc)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStoryPageList(json: String): List<StoryPage> {
        if (json.isBlank() || json == "[]") return emptyList()
        val jsonArray = JSONArray(json)
        return (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            StoryPage(
                pageNumber = obj.optInt("pageNumber", i + 1),
                kasiguranin = obj.optString("kasiguranin", ""),
                tagalog = obj.optString("tagalog", ""),
                english = obj.optString("english", ""),
                audioFileName = obj.optString("audioFileName", ""),
                illustrationDesc = obj.optString("illustrationDesc", "")
            )
        }
    }
}
