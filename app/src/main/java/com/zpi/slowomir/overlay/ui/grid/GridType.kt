package com.zpi.slowomir.overlay.ui.grid

enum class GridType(val id: String) {
    SINGLE("single"),
    DASHED("dashed"),
    CONTRAST("contrast");

    companion object {
        val DEFAULT = SINGLE

        fun fromId(id: String?): GridType = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
