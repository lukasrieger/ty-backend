package instances.article

import model.Article
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.joda.time.DateTime
import types.ReadDB
import types.dao.ArticlesTable


suspend fun ReadDB<Article>.getAllArticles() =
    getByQuery(ArticlesTable.selectAll())


suspend fun ReadDB<Article>.byQueryArchived(query: Query, limit: Int?, offset: Long?): Sequence<Article> =
    getByQuery(
        query = query
            .andWhere { ArticlesTable.archiveDate less DateTime.now() }
            .orderBy(ArticlesTable.archiveDate to SortOrder.DESC),
        limit,
        offset
    )

