package model

enum class SupportType(val description: String) {
    IndividualResearch("Individualförderung"),
    ResearchGroups("Verbundförderung"),
    Scholarship("Stipendium"),
    Price("Preis"),
    TransferSpinOff("Transfer/Ausgründung"),
    StructuralPromotion("Strukturierte Promotionsförderprogramme"),
    Infrastructure("Infrastruktur/Geräte"),
    Meeting("Tagungen/Konferenzen/Lehre")
}