package com.mazzika.lyrics.data.nearby

import org.json.JSONObject

sealed class SyncMessage {

    data class SessionInfo(
        val title: String,
        val pageCount: Int,
        val fileHash: String,
    ) : SyncMessage()

    data class AlreadyHave(val fileHash: String) : SyncMessage()

    data class NeedFile(val fileHash: String) : SyncMessage()

    data class PageChange(val page: Int) : SyncMessage()

    data class SessionEnd(val reason: String) : SyncMessage()

    fun toBytes(): ByteArray {
        val json = JSONObject()
        when (this) {
            is SessionInfo -> {
                json.put("type", "SessionInfo")
                json.put("title", title)
                json.put("pageCount", pageCount)
                json.put("fileHash", fileHash)
            }
            is AlreadyHave -> {
                json.put("type", "AlreadyHave")
                json.put("fileHash", fileHash)
            }
            is NeedFile -> {
                json.put("type", "NeedFile")
                json.put("fileHash", fileHash)
            }
            is PageChange -> {
                json.put("type", "PageChange")
                json.put("page", page)
            }
            is SessionEnd -> {
                json.put("type", "SessionEnd")
                json.put("reason", reason)
            }
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromBytes(bytes: ByteArray): SyncMessage? {
            return try {
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                when (json.getString("type")) {
                    "SessionInfo" -> SessionInfo(
                        title = json.getString("title"),
                        pageCount = json.getInt("pageCount"),
                        fileHash = json.getString("fileHash"),
                    )
                    "AlreadyHave" -> AlreadyHave(fileHash = json.getString("fileHash"))
                    "NeedFile" -> NeedFile(fileHash = json.getString("fileHash"))
                    "PageChange" -> PageChange(page = json.getInt("page"))
                    "SessionEnd" -> SessionEnd(reason = json.optString("reason", ""))
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
