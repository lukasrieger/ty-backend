package constraint

import model.ArticleState
import model.Rubric
import model.SupportType
import model.TargetGroup
import org.jetbrains.exposed.sql.*
import repository.dao.ArticlesTable

class Constraint(val isUnion: Boolean, val operator: Op<Boolean>) {
    operator fun component1() = operator
}

fun constraint(isUnion: Boolean, cons: SqlExpressionBuilder.() -> Op<Boolean>): Constraint =
    Constraint(isUnion, SqlExpressionBuilder.cons())


fun Rubric.toConstraint(isUnion: Boolean = false) = constraint(isUnion) {
    ArticlesTable.rubric eq this@toConstraint
}

fun SupportType.toConstraint(isUnion: Boolean = false) = constraint(isUnion) {
    ArticlesTable.supportType eq this@toConstraint
}

fun TargetGroup.toConstraint(isUnion: Boolean = false) = constraint(isUnion) {
    ArticlesTable.targetGroup eq this@toConstraint
}

fun ArticleState.toConstraint(isUnion: Boolean = false) = constraint(isUnion) {
    ArticlesTable.state eq this@toConstraint
}

fun build(ops: Iterable<Constraint>) = ops.fold(ArticlesTable.selectAll()) { query, cons ->
    val (op) = cons
    if (cons.isUnion) {
        query.orWhere { op }
    } else {
        query.andWhere { op }
    }

}

