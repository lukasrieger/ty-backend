package model

import types.Id
import types.Index


data class ContactPartner(
    override val id: Id<ContactPartner>? = null,
    val surname: String,
    val lastName: String,
    val phoneNumber: String,
    val url: String
) : Index<ContactPartner>
