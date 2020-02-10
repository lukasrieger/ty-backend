package constraint

import org.jetbrains.exposed.sql.or
import repository.dao.ArticlesTable

fun matchHeaderOrText(queryStr: String) = constraint(true) {
    ArticlesTable.name match queryStr or
            (ArticlesTable.text match queryStr)
}