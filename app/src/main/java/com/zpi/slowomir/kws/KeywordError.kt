package com.zpi.slowomir.kws

enum class KeywordError(val message: String) {
    PERMISSION_DENIED("Brak uprawnienia do mikrofonu"),
    MODEL_LOAD_FAILED("Nie udało się załadować modelu KWS"),
    MIC_UNAVAILABLE("Mikrofon niedostępny"),
    UNKNOWN("Nieznany błąd")
}