package fr.spiderboy.navigoat.models

data class CardInfo(
    val uid: String,
    val events: List<NavigoEvent> = emptyList(),
    val contracts: List<NavigoContract> = emptyList(),
    val specialEvents: List<NavigoEvent> = emptyList(),
    val rawDump: String = ""
)

data class NavigoEvent(
    val title: String,
    val content: String,
    val line: String = "",
    val lineLogo: String? = null
)

data class NavigoContract(
    val title: String,
    val content: String
)
