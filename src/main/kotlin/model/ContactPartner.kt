package model

import repository.None
import repository.PrimaryKey


data class ContactPartner(
    val id: PrimaryKey<ContactPartner> = None,
    val surname: String,
    val lastname: String,
    val phoneNumber: String,
    val url: String
)

