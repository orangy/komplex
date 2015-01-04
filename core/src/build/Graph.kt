
package komplex

import java.util.HashSet

// generic DAG edge trait
public trait DagEdge {
    public fun makeInverse(): DagEdge
    public override fun equals(other: Any?): Boolean // assuming to take only nodes into account so reverse check can work
    public override fun hashCode(): Int // same as for equals
}


// generic traverse checker with generators
public class TraversedChecker<Edge>() {
    val traversed = HashSet<Edge>()
    public fun checkAdd(e: Edge): Boolean = traversed.add(e)
    public fun check(e: Edge): Boolean = !traversed.contains(e)
}

public fun makeVisitedTraversalChecker<Edge>(): (Edge) -> kotlin.Boolean {
    val checker = TraversedChecker<Edge>()
    return { (e: Edge) -> checker.checkAdd(e) }
}

public fun makeDagAsserter<Edge: DagEdge>(): (Edge) -> Boolean {
    val checker = TraversedChecker<Edge>()
    return { (e: Edge) ->
        if (!checker.check(e.makeInverse() as Edge)) // \todo find out why the cast is required
            throw RuntimeException("DAG expected, but cycle is detected")
        checker.checkAdd(e) }
}


// generic BFS
public fun graphBFS<Edge>(start: Edge,
                          func: (Edge) -> Boolean,
                          nextEdges: (Edge) -> Iterable<Edge>?,
                          checkTraversal: (Edge) -> Boolean = makeVisitedTraversalChecker() ) {

    if (!checkTraversal(start)) return;

    val queue: java.util.Queue<Edge> = java.util.LinkedList()
    var edge: Edge = start

    while (!func(edge) /* found */) {
        nextEdges(edge)?.forEach { (e) -> if (checkTraversal(e)) queue.add(e) }
        edge = queue.poll() ?: break
    }
}


// generic DFS, recursive impl \todo: make non-recursive impl
public fun graphDFS<Edge>(start: Edge,
                          func: (e: Edge) -> Boolean,
                          nextEdges: (e: Edge) -> Iterable<Edge>?,
                          checkTraversal: (Edge) -> Boolean = makeVisitedTraversalChecker()) {

    if (!checkTraversal(start)) return;

    fun impl(edge: Edge) {
        if (func(edge)) return // found
        nextEdges(edge)?.forEach { (e) -> if (checkTraversal(e)) impl(e) }
    }

    impl(start)
}


// generic graph
public trait Graph<Edge> {
    public fun add(edge: Edge)
    public fun incoming(edge: Edge): Iterable<BuildGraphEdge>?
    public fun outgoing(edge: Edge): Iterable<BuildGraphEdge>?
}

