package model

enum class Subject(val description: String, val akronym: String) {
    OpenProgram("Themenoffenes Programm", "TP"),
    LifeSciences("Medizin", "LS"),
    PhysicalSciencesAndEngineering("Naturwissenschaften", "PSE"),
    SocialSciencesAndHumanities("Sozial- und Geisteswisschenschaften", "SSH")

}