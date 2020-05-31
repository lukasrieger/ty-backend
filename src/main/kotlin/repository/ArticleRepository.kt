package repository

import arrow.Kind
import arrow.core.Valid
import arrow.fx.typeclasses.Concurrent
import arrow.syntax.function.pipe
import model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import repository.dao.ArticlesTable
import repository.extensions.queryPaginate
import validation.Validator
import java.io.FileInputStream


typealias ArticleIndex = PrimaryKey<Article>
typealias ValidArticle = Valid<Article>

@JvmName("nullableToArticle")
fun <F> Concurrent<F>.toArticle(resultRow: ResultRow?, contactReader: Reader<F, ContactPartner>): Kind<F, Article?> =
    resultRow?.let { toArticle(it, contactReader) } ?: just(null)

fun <F> Concurrent<F>.toArticle(resultRow: ResultRow, contactReader: Reader<F, ContactPartner>): Kind<F, Article> =
    fx.concurrent {
        Article(
            id = keyOf(resultRow[ArticlesTable.id].value),
            title = resultRow[ArticlesTable.title],
            text = resultRow[ArticlesTable.text],
            rubric = resultRow[ArticlesTable.rubric],
            priority = resultRow[ArticlesTable.priority],
            targetGroup = resultRow[ArticlesTable.targetGroup],
            supportType = resultRow[ArticlesTable.supportType],
            subject = resultRow[ArticlesTable.subject],
            state = resultRow[ArticlesTable.state],
            archiveDate = resultRow[ArticlesTable.archiveDate],
            recurrentInfo = readRecurrence(resultRow),
            applicationDeadline = resultRow[ArticlesTable.applicationDeadline],
            contactPartner = resultRow[ArticlesTable.contactPartner]?.let { !contactReader.byId(keyOf(it)) },
            childArticle = resultRow[ArticlesTable.childArticle]?.let { keyOf<Article>(it) },
            parentArticle = resultRow[ArticlesTable.parentArticle]?.let { keyOf<Article>(it) }
        )
    }

class ArticleReader<F>(
    override val runtime: Concurrent<F>,
    private val contactReader: Reader<F, ContactPartner>
) : Reader<F, Article> {

    override fun byId(id: PrimaryKey<Article>): Kind<F, Article?> =
        concurrent {
            !!transactionEffect(ArticlesTable) {
                select { ArticlesTable.id eq id.key }
                    .orderBy(applicationDeadline to SortOrder.ASC)
                    .singleOrNull()
                    .pipe { toArticle<F>(it, contactReader) }
            }
        }

    override fun byQuery(query: Query, limit: Int?, offset: Long?): Kind<F, QueryResult<Article>> =
        concurrent {
            val count = !countOf(query)
            val queryResult = queryPaginate(query, limit, offset)
                .bind()
                .map { !toArticle<F>(it, contactReader) }

            QueryResult(count, queryResult)
        }

    override fun countOf(query: Query): Kind<F, Long> =
        concurrent {
            !effect { newSuspendedTransaction { query.count() } }
        }
}


class ArticleWriter<F>(override val runtime: Concurrent<F>) : Writer<F, Article> {

    override fun update(entry: Valid<Article>): Kind<F, ValidArticle> =
        concurrent {
            !transactionEffect(ArticlesTable) {
                val (article) = entry
                val (key) = article.id
                ArticlesTable.update({ ArticlesTable.id eq key }) { article.toStatement(it) }
            }
            entry
        }

    override fun create(entry: Valid<Article>): Kind<F, ValidArticle> =
        concurrent {
            val id = !transactionEffect(ArticlesTable) {
                val (article) = entry
                ArticlesTable.insert { article.toStatement(it) } get ArticlesTable.id
            }
            Valid(Article.id.set(entry.a, keyOf(id.value)))
        }

    override fun delete(id: PrimaryKey<Article>): Kind<F, PrimaryKey<Article>> =
        concurrent {
            !transactionEffect(ArticlesTable) {
                update({ childArticle eq id.key }) { it[childArticle] = null }
                update({ parentArticle eq id.key }) { it[parentArticle] = null }
                deleteWhere { ArticlesTable.id eq id.key }
            }
            id
        }
}


class ArticleRepository<F>(
    override val runtime: Concurrent<F>,
    override val validator: Validator<F, *, Article>,
    contactReader: Reader<F, ContactPartner>
) :
    Reader<F, Article> by ArticleReader(runtime, contactReader),
    Writer<F, Article> by ArticleWriter(runtime),
    Repository<F, Article>


internal fun readRecurrence(row: ResultRow): RecurrentInfo? =
    row[ArticlesTable.recurrentCheckFrom]?.let { rec ->
        row[ArticlesTable.nextApplicationDeadline]?.let { app ->
            row[ArticlesTable.nextArchiveDate]?.let { arch ->
                RecurrentInfo(rec, app, arch)
            }
        }
    }

private fun Article.toStatement(statement: UpdateBuilder<Int>) =
    statement.run {
        this[ArticlesTable.title] = title
        this[ArticlesTable.text] = text
        this[ArticlesTable.rubric] = rubric
        this[ArticlesTable.priority] = priority
        this[ArticlesTable.targetGroup] = targetGroup
        this[ArticlesTable.supportType] = supportType
        this[ArticlesTable.subject] = subject
        this[ArticlesTable.state] = state
        this[ArticlesTable.archiveDate] = archiveDate
        this[ArticlesTable.applicationDeadline] = applicationDeadline
        this[ArticlesTable.contactPartner] = contactPartner?.id?.key
        this[ArticlesTable.childArticle] = childArticle?.key
        this[ArticlesTable.parentArticle] = parentArticle?.key
        this[ArticlesTable.isRecurrent] = recurrentInfo != null
        this[ArticlesTable.recurrentCheckFrom] = recurrentInfo?.recurrentCheckFrom
        this[ArticlesTable.nextApplicationDeadline] = recurrentInfo?.applicationDeadline
        this[ArticlesTable.nextArchiveDate] = recurrentInfo?.archiveDate
    }


internal fun sourcePath(path: String): String = TODO()


fun <F> Concurrent<F>.loadSource(url: String): Kind<F, Source?> =
    fx.concurrent {
        !just(FileInputStream(sourcePath(url)))
            .bracket(
                release = { file -> just(file.close()) },
                use = { file ->
                    val bytes = file.readBytes()
                    val text = String(bytes)

                    just(Source(url, text))
                }
            )
    }


