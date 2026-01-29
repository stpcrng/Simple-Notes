package com.example.simplenotes.model

enum class FileType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER;

    companion object {
        fun fromMimeType(mimeType: String): FileType {
            return when {
                mimeType.startsWith("image") -> IMAGE
                mimeType.startsWith("video") -> VIDEO
                mimeType.startsWith("audio") -> AUDIO
                mimeType in listOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"
                ) -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
