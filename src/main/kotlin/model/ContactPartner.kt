package model

import arrow.optics.optics
import repository.None
import repository.PrimaryKey


@optics
data class ContactPartner(
    val id: PrimaryKey<ContactPartner> = None,
    val surname: String,
    val lastname: String,
    val phoneNumber: String,
    val url: String
) {
    companion object
}

