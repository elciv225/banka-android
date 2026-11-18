package com.banka.app.domain

import com.banka.app.data.csv.CsvParser

enum class Category(
    val key: String,
    val label: String,
    val emoji: String,
    val color: Long,
    val keywords: List<String>,
) {
    REVENUS(
        "REVENUS", "Revenus", "💰", 0xFF1F8A4C,
        listOf("salaire", "virement recu", "virement reçu", "pension", "allocation", "prime",
            "interets", "intérêts", "dividende", "remboursement", "rembourse", "caisse alloc", "caf "),
    ),
    ALIMENTATION(
        "ALIMENTATION", "Alimentation", "🛒", 0xFFE07B39,
        listOf("carrefour", "auchan", "leclerc", "e.leclerc", "lidl", "aldi", "monoprix", "monop'",
            "intermarche", "intermarché", "super u", "systeme u", "casino", "franprix", "biocoop",
            "picard", "boulanger", "boulangerie", "epicerie", "marché", "marche ", "la vie claire",
            "kruidvat", "flunch", "quick ", "starbucks", "pain"),
    ),
    RESTAURANTS(
        "RESTAURANTS", "Restaurants", "🍽️", 0xFFC2410C,
        listOf("restaurant", "resto", "mc donald", "mcdonald", "burger", "kfc", "pizza", "kebab",
            "brasserie", "bar tabac", "tabac presse", "deliveroo", "uber eat", "ubereats",
            "just eat", "foodora", "cafe de", "café", "coffee", "starbucks coffee"),
    ),
    TRANSPORT(
        "TRANSPORT", "Transport", "🚆", 0xFF2563EB,
        listOf("uber", "bolt", "sncf", "oui.sncf", "ratp", "navigo", "metro", "métro", "transilien",
            "tram", "bus ", "blablacar", "taxi", "g7", "esso", "shell", "bp ", "total", "agip",
            "essence", "gazole", "petrole", "péage", "peage", "parking", "air france", "easyjet",
            "ryanair", "vincci", "autoroute"),
    ),
    LOGEMENT(
        "LOGEMENT", "Logement", "🏠", 0xFF7C3AED,
        listOf("loyer", "edf", "engie", "electricite", "électricité", "gaz ", "vattenfall",
            "direct energie", "eau de", "veolia", "suez", "internet", "fibre", "free mobile",
            "orange", "sfr", "bouygues", "la poste", "credit immobilier", "credit agricole",
            "assurance habitation", "maif", "macif"),
    ),
    ABONNEMENTS(
        "ABONNEMENTS", "Abonnements", "🔁", 0xFF0891B2,
        listOf("netflix", "spotify", "amazon prime", "prime video", "disney", "canal+", "canal plus",
            "canalplus", "apple.com", "apple bill", "google play", "forfait", "abonnement",
            "deezer", "youtube premium", "video club", "fitbit", "in trems"),
    ),
    SANTÉ(
        "SANTE", "Santé", "💊", 0xFFDC2626,
        listOf("pharmacie", "parapharmacie", "medecin", "médecin", "docteur", "optique", "opticien",
            "dentiste", "kine", "kiné", "laboratoire", "mutuelle", "hopital", "hôpital", "clinique"),
    ),
    LOISIRS(
        "LOISIRS", "Loisirs", "🎮", 0xFF9333EA,
        listOf("cinema", "cinéma", "ugc", "pathe", "pathé", "steam", "playstation", "xbox",
            "nintendo", "concert", "ticketmaster", "fnac", "lire ", "librairie", "sport",
            "salle de sport", "basic fit", "cmg sport", "musee", "musée"),
    ),
    SHOPPING(
        "SHOPPING", "Shopping", "🛍️", 0xFFDB2777,
        listOf("amazon", "zalando", "vinted", "decathlon", "decathl", "shein", "h&m", "zara",
            "uniqlo", "kiabi", "cdiscount", "ikea", "leroy merlin", "darty", "boulanger "),
    ),
    AUTRE("AUTRE", "Autre", "📦", 0xFF64748B, emptyList()),

    ;

    companion object {
        fun fromEntryKey(key: String): Category = entries.firstOrNull { it.key == key } ?: AUTRE
    }
}

object Categorizer {

    fun classify(merchantLabel: String): Category {
        val label = CsvParser.normalize(merchantLabel)
        if (label.isBlank()) return Category.AUTRE
        for (category in Category.entries) {
            if (category == Category.AUTRE) continue
            if (category.keywords.any { label.contains(it.trim()) }) return category
        }
        return Category.AUTRE
    }
}
