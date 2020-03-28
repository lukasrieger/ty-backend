package model

enum class Rubric(
    val akronym: String,
    val international: Boolean = false
) {


    Horizont2020("Horizont 2020", true),
    OtherEUProgram("EU-Programme: Sonstige", true),
    OtherInternational("Weiter Förderinstitutionen (int)", true),

    LMU("LMU"),
    BMBF("Bundesministerium für Bildung und Forschung (BMBF)")

}


