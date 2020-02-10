package model

enum class Rubric(val international: Boolean = false,
                  val akronym: String) {


    HORIZONT_2020 (true, "Horizont 2020"),
    OTHER_EU_PROGRAM (true, "EU-Programme: Sonstige"),
    OTHER_INTERNATIONAL (true, "Weiter Förderinstitutionen (int)"),

    LMU (akronym = "LMU"),
    BMBF (akronym = "Bundesministerium für Bildung und Forschung (BMBF)")

}

