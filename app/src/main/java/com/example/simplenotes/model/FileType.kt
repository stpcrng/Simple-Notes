package com.example.simplenotes.model

enum class FileType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    OTHER;

    companion object {

        fun fromMimeType(mimeType: String?): FileType {
            if (mimeType.isNullOrBlank()) return OTHER

            return when {
                mimeType.startsWith("image") -> IMAGE
                mimeType.startsWith("video") -> VIDEO
                mimeType.startsWith("audio") -> AUDIO
                mimeType.contains("pdf") ||
                        mimeType.contains("word") ||
                        mimeType.contains("text") ||
                        mimeType.contains("officedocument") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
