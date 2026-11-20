package com.zpi.slowomir.voice

import javax.inject.Inject

class PolishTextNormalizer @Inject constructor() {

    private val hundredsMap = mapOf(
        "sto" to 100, "setny" to 100, "setna" to 100, "setne" to 100, "setnego" to 100, "setną" to 100,
        "dwieście" to 200, "dwusetny" to 200, "dwusetna" to 200, "dwusetne" to 200, "dwusetnego" to 200, "dwusetną" to 200,
        "trzysta" to 300, "trzysetny" to 300, "trzysetna" to 300, "trzysetne" to 300, "trzysetnego" to 300, "trzysetną" to 300,
        "czterysta" to 400, "czterysetny" to 400, "czterysetna" to 400, "czterysetne" to 400, "czterysetnego" to 400, "czterysetną" to 400,
        "pięćset" to 500, "pięćsetny" to 500, "pięćsetna" to 500, "pięćsetne" to 500, "pięćsetnego" to 500, "pięćsetną" to 500,
        "sześćset" to 600, "sześćsetny" to 600, "sześćsetna" to 600, "sześćsetne" to 600, "sześćsetnego" to 600, "sześćsetną" to 600,
        "siedemset" to 700, "siedemsetny" to 700, "siedemsetna" to 700, "siedemsetne" to 700, "siedemsetnego" to 700, "siedemsetną" to 700,
        "osiemset" to 800, "osiemsetny" to 800, "osiemsetna" to 800, "osiemsetne" to 800, "osiemsetnego" to 800, "osiemsetną" to 800,
        "dziewięćset" to 900, "dziewięćsetny" to 900, "dziewięćsetna" to 900, "dziewięćsetne" to 900, "dziewięćsetnego" to 900, "dziewięćsetną" to 900
    )

    private val tensMap = mapOf(
        "dwadzieścia" to 20, "dwudziesty" to 20, "dwudziesta" to 20, "dwudzieste" to 20, "dwudziestego" to 20, "dwudziestą" to 20,
        "trzydzieści" to 30, "trzydziesty" to 30, "trzydziesta" to 30, "trzydzieste" to 30, "trzydziestego" to 30, "trzydziestą" to 30,
        "czterdzieści" to 40, "czterdziesty" to 40, "czterdziesta" to 40, "czterdzieste" to 40, "czterdziestego" to 40, "czterdziestą" to 40,
        "pięćdziesiąt" to 50, "pięćdziesiąty" to 50, "pięćdziesiąta" to 50, "pięćdziesiąte" to 50, "pięćdziesiątego" to 50, "pięćdziesiątą" to 50,
        "sześćdziesiąt" to 60, "sześćdziesiąty" to 60, "sześćdziesiąta" to 60, "sześćdziesiąte" to 60, "sześćdziesiątego" to 60, "sześćdziesiątą" to 60,
        "siedemdziesiąt" to 70, "siedemdziesiąty" to 70, "siedemdziesiąta" to 70, "siedemdziesiąte" to 70, "siedemdziesiątego" to 70, "siedemdziesiątą" to 70,
        "osiemdziesiąt" to 80, "osiemdziesiąty" to 80, "osiemdziesiąta" to 80, "osiemdziesiąte" to 80, "osiemdziesiątego" to 80, "osiemdziesiątą" to 80,
        "dziewięćdziesiąt" to 90, "dziewięćdziesiąty" to 90, "dziewięćdziesiąta" to 90, "dziewięćdziesiąte" to 90, "dziewięćdziesiątego" to 90, "dziewięćdziesiątą" to 90
    )

    private val teensMap = mapOf(
        "dziewiętnaście" to 19, "dziewiętnasty" to 19, "dziewiętnasta" to 19, "dziewiętnaste" to 19, "dziewiętnastego" to 19, "dziewiętnastą" to 19,
        "osiemnaście" to 18, "osiemnasty" to 18, "osiemnasta" to 18, "osiemnaste" to 18, "osiemnastego" to 18, "osiemnastą" to 18,
        "siedemnaście" to 17, "siedemnasty" to 17, "siedemnasta" to 17, "siedemnaste" to 17, "siedemnastego" to 17, "siedemnastą" to 17,
        "szesnaście" to 16, "szesnasty" to 16, "szesnasta" to 16, "szesnaste" to 16, "szesnastego" to 16, "szesnastą" to 16,
        "piętnaście" to 15, "piętnasty" to 15, "piętnasta" to 15, "piętnaste" to 15, "piętnastego" to 15, "piętnastą" to 15,
        "czternaście" to 14, "czternasty" to 14, "czternasta" to 14, "czternaste" to 14, "czternastego" to 14, "czternastą" to 14,
        "trzynaście" to 13, "trzynasty" to 13, "trzynasta" to 13, "trzynaste" to 13, "trzynastego" to 13, "trzynastą" to 13,
        "dwanaście" to 12, "dwunasty" to 12, "dwunasta" to 12, "dwunaste" to 12, "dwunastego" to 12, "dwunastą" to 12,
        "jedenaście" to 11, "jedenasty" to 11, "jedenasta" to 11, "jedenaste" to 11, "jedenastego" to 11, "jedenastą" to 11,
        "dziesiątka" to 10, "dziesiątkę" to 10, "dziesięć" to 10, "dziesiąty" to 10, "dziesiąta" to 10, "dziesiąte" to 10, "dziesiątego" to 10, "dziesiątą" to 10
    )

    private val unitsMap = mapOf(
        "dziewiątka" to 9, "dziewiątkę" to 9, "dziewięć" to 9, "dziewiąty" to 9, "dziewiąta" to 9, "dziewiąte" to 9, "dziewiątego" to 9, "dziewiątą" to 9,
        "ósemka" to 8, "ósemkę" to 8, "osiem" to 8, "ósmy" to 8, "ósma" to 8, "ósme" to 8, "ósmego" to 8, "ósmą" to 8,
        "siódemka" to 7, "siódemkę" to 7, "siedem" to 7, "siódmy" to 7, "siódma" to 7, "siódme" to 7, "siódmego" to 7, "siódmą" to 7,
        "szóstka" to 6, "szóstkę" to 6, "sześć" to 6, "szósty" to 6, "szósta" to 6, "szóste" to 6, "szóstego" to 6, "szóstą" to 6,
        "piątka" to 5, "piątkę" to 5, "pięć" to 5, "piąty" to 5, "piąta" to 5, "piąte" to 5, "piątego" to 5, "piątą" to 5,
        "czwórka" to 4, "czwórkę" to 4, "cztery" to 4, "czwarty" to 4, "czwarta" to 4, "czwarte" to 4, "czwartego" to 4, "czwartą" to 4,
        "trójka" to 3, "trójkę" to 3, "trzy" to 3, "trzeci" to 3, "trzecia" to 3, "trzecie" to 3, "trzeciego" to 3, "trzecią" to 3,
        "dwójka" to 2, "dwójkę" to 2, "dwie" to 2, "dwa" to 2, "drugi" to 2, "druga" to 2, "drugie" to 2, "drugiego" to 2, "drugą" to 2,
        "jedynka" to 1, "jedynkę" to 1, "jeden" to 1, "jedna" to 1, "jedno" to 1, "pierwszy" to 1, "pierwsza" to 1, "pierwsze" to 1, "pierwszego" to 1, "pierwszą" to 1,
        "zero" to 0
    )

    fun normalize(rawText: String): String {
        val cleaned = rawText.lowercase().trim()
            .replace(Regex("[.,!?\\-]"), " ")

        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        val result = mutableListOf<String>()
        var i = 0

        while (i < tokens.size) {
            val (parsedNumber, consumed) = parseNumber(tokens, i)
            if (parsedNumber != null) {
                result.add(parsedNumber.toString())
                i += consumed
            } else {
                result.add(tokens[i])
                i++
            }
        }

        return result.joinToString(" ")
    }

    private fun parseNumber(tokens: List<String>, startIndex: Int): Pair<Int?, Int> {
        var idx = startIndex
        var total = 0
        var hasNumber = false
        var hasHundreds = false
        var hasTensOrTeens = false
        var hasUnits = false

        while (idx < tokens.size) {
            val token = tokens[idx]
            val directInt = token.toIntOrNull()

            when {
                hundredsMap.containsKey(token) && !hasHundreds && !hasTensOrTeens && !hasUnits -> {
                    total += hundredsMap.getValue(token)
                    hasHundreds = true
                    hasNumber = true
                    idx++
                }
                tensMap.containsKey(token) && !hasTensOrTeens && !hasUnits -> {
                    total += tensMap.getValue(token)
                    hasTensOrTeens = true
                    hasNumber = true
                    idx++
                }
                teensMap.containsKey(token) && !hasTensOrTeens && !hasUnits -> {
                    total += teensMap.getValue(token)
                    hasTensOrTeens = true
                    hasUnits = true
                    hasNumber = true
                    idx++
                }
                unitsMap.containsKey(token) && !hasUnits -> {
                    val unit = unitsMap.getValue(token)
                    if (unit == 0 && hasNumber) break
                    total += unit
                    hasUnits = true
                    hasNumber = true
                    idx++
                }
                directInt != null -> {
                    if (!hasNumber) {
                        total = directInt
                        hasNumber = true
                        hasHundreds = directInt >= 100
                        hasTensOrTeens = (directInt % 100) >= 10
                        hasUnits = (directInt % 10) != 0 || directInt in 10..19
                        idx++
                    } else if (directInt in 1..9 && !hasUnits) {
                        total += directInt
                        hasUnits = true
                        idx++
                    } else {
                        break
                    }
                }
                else -> break
            }
        }

        return if (hasNumber) Pair(total, idx - startIndex) else Pair(null, 0)
    }
}