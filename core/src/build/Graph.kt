
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

// generic preorder BFS
// \todo check if keeping separate preorder version makes sense e.g. performance-wise
public fun graphPreorderBFS<Edge>(from: Iterable<Edge>,
                                  preorderPred: (Edge) -> Boolean,
                                  nextEdges: (Edge) -> Iterable<Edge>,
                                  checkTraversal: (Edge) -> Boolean = makeVisitedTraversalChecker()
                                 ): Boolean {

    val queue: java.util.Queue<Edge> = linkedListOf() // \todo check/optimize collection
    var currentQueue = from;

    while (true) {
        // checking which edges to add from currentQueue
        for (e in currentQueue) {
            if (checkTraversal(e)) {
                if (preorderPred(e)) return true // found, stop traversing
                queue.add(e)
            }
        }
        if (queue.isEmpty()) break
        currentQueue = nextEdges(queue.poll())
    }
    return false
}

// generic pre+postorder BFS
public fun graphBFS<Edge>(from: Iterable<Edge>,
                                   preorderPred: (Edge) -> Boolean,
                                   postorderPred: (Edge) -> Boolean,
                                   nextEdges: (Edge) -> Iterable<Edge>,
                                   checkTraversal: (Edge) -> Boolean = makeVisitedTraversalChecker()
                                  ): Boolean {

    // note: this is not nice, but fail-fast iterators do not allow nice implementation based on standard containers
    val stack = arrayListOf<Edge>()
    var currentQueue = from
    var pos = 0
    while (true) {
        // checking which edges to add from currentQueue
        for (e in currentQueue) {
            if (checkTraversal(e)) {
                if (preorderPred(e)) return true // found, stop collecting
                stack.add(e)
            }
        }
        // finding next non-empty list of edges from added ones
        if (pos >= stack.size()) break
        currentQueue = nextEdges(stack.get(pos++))
    }
    // process collected edges
    for (i in (stack.size()-1)..0) {
        if (postorderPred(stack.get(i))) return true // found on postorder
        // \todo optimization: remove processed edge here?
    }
    return false
}


// generic DFS, recursive impl \todo: make non-recursive impl
public fun graphDFS<Edge>(from: Iterable<Edge>,
                          preorderPred: (e: Edge) -> Boolean,
                          postorderPred: (Edge) -> Boolean,
                          nextEdges: (e: Edge) -> Iterable<Edge>,
                          checkTraversal: (Edge) -> Boolean = makeVisitedTraversalChecker()
                         ): Boolean {

    for (e in from)
        if (checkTraversal(e)) {
            if (preorderPred(e)) return true // found, stop traversing
            if (graphDFS(nextEdges(e), preorderPred, postorderPred, nextEdges, checkTraversal)) return true // found in subgraph
            if (postorderPred(e)) return true // found on postorder
        }
    return false
}


// generic graph
public trait Graph<Edge> {
    public fun add(edge: Edge)
    public fun incoming(edge: Edge): Iterable<BuildGraphEdge>
    public fun outgoing(edge: Edge): Iterable<BuildGraphEdge>
}

