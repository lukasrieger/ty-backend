package constraint

import org.jetbrains.exposed.sql.or
import service.dao.ArticlesTable

fun matchHeaderOrText(queryStr: String) = constraint(true) {
    ArticlesTable.title match queryStr or (ArticlesTable.text match queryStr)
}