package model

import arrow.optics.optics
import repository.Init
import repository.PrimaryKey


@optics
data class ContactPartner(
    val id: PrimaryKey<ContactPartner> = Init,
    val surname: String,
    val lastName: String,
    val phoneNumber: String,
    val url: String
) {
    companion object
}

