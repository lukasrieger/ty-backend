package model

import service.Id



data class ContactPartner(
    val id: Id<ContactPartner>,
    val surname: String,
    val lastName: String,
    val phoneNumber: String,
    val url: String
)
