
package komplex.utils

import komplex.log
import java.util.*

// generic traverse checker with generators
class TraversedChecker<Node>() {
    val traversed = HashSet<Node>()
    fun checkAdd(n: Node): Boolean = traversed.add(n)
    fun check(n: Node): Boolean = !traversed.contains(n)
}

fun <Node> makeVisitedTraversalChecker(): (Node) -> kotlin.Boolean {
    val checker = TraversedChecker<Node>()
    return { n: Node -> checker.checkAdd(n) }
}

fun <Node> makeTracingVisitedTraversalChecker(): (Node) -> kotlin.Boolean {
    val checker = TraversedChecker<Node>()
    return if (log.isTraceEnabled)
        { n: Node ->
            val res = checker.checkAdd(n)
            if (!res) log.trace("skipped already traversed node $n")
            res
        }
    else { n: Node -> checker.checkAdd(n) }
}


// \todo invert predicates meaning - instead of "cancel?" use "continue?"

// generic preorder BFS
// \todo check if keeping separate preorder version makes sense e.g. performance-wise
fun <Node> graphPreorderBFS(from: Iterable<Node>,
                            preorderPred: (Node) -> Boolean,
                            nextNodes: (Node) -> Iterable<Node>,
                            checkTraversal: (Node) -> Boolean = makeVisitedTraversalChecker()
): Boolean {

    val queue: java.util.Queue<Node> = LinkedList<Node>() // \todo check/optimize collection
    var currentQueue = from

    while (true) {
        // checking which nodes to add from currentQueue
        for (e in currentQueue) {
            if (checkTraversal(e)) {
                if (preorderPred(e)) return true // found, stop traversing
                queue.add(e)
            }
        }
        if (queue.isEmpty()) break
        currentQueue = nextNodes(queue.poll())
    }
    return false
}

// generic pre+postorder BFS
fun <Node> graphBFS(from: Iterable<Node>,
                    preorderPred: (Node) -> Boolean,
                    postorderPred: (Node) -> Boolean,
                    nextNodes: (Node) -> Iterable<Node>,
                    checkTraversal: (Node) -> Boolean = makeVisitedTraversalChecker()
): Boolean {

    // note: this is not nice, but fail-fast iterators do not allow nice implementation based on standard containers
    val stack = arrayListOf<Node>()
    var currentQueue = from
    var pos = 0
    while (true) {
        // checking which nodes to add from currentQueue
        for (e in currentQueue) {
            if (checkTraversal(e)) {
                if (preorderPred(e)) return true // found, stop collecting
                stack.add(e)
            }
        }
        // finding next non-empty list of edges from added ones
        if (pos >= stack.size) break
        currentQueue = nextNodes(stack.get(pos++))
    }
    // process collected nodes
    pos = stack.size
    while (pos > 0) {
        if (postorderPred(stack.get(--pos))) return true // found on postorder
        // \todo optimization: remove processed edge here?
    }
    return false
}


// generic DFS, recursive impl \todo: make non-recursive impl
fun <Node> graphDFS(from: Iterable<Node>,
                    preorderPred: (e: Node) -> Boolean,
                    postorderPred: (Node) -> Boolean,
                    nextNodes: (e: Node) -> Iterable<Node>,
                    checkTraversal: (Node) -> Boolean = makeVisitedTraversalChecker()
): Boolean {
    for (e in from)
        if (checkTraversal(e)) {
            if (preorderPred(e)) return true // found, stop traversing
            if (graphDFS(nextNodes(e), preorderPred, postorderPred, nextNodes, checkTraversal)) return true // found in subgraph
            if (postorderPred(e)) return true // found on postorder
        }
    return false
}


fun <Node> subgraphDFS(from: Iterable<Node>,
                       to: Set<Node>,
                       preorderPred: (e: Node) -> Boolean,
                       postorderPred: (Node) -> Boolean,
                       nextNodes: (n: Node) -> Iterable<Node>,
                       checkTraversal: (Node) -> Boolean = makeVisitedTraversalChecker()
): Boolean {
    if (log.isTraceEnabled) {
        log.trace("subgraph from: ${from.joinToString(", ", "(", ")")}")
        log.trace("subgraph to: ${to.joinToString(", ", "(", ")")}")
    }
    val subgraphNodes = hashSetOf<Node>()
    val roots = hashSetOf<Node>()
    val stack = ArrayDeque<Node>()
    // first phase - calculating reachability graph and finding it's roots
    // \todo check effectiveness of the algo
    for (r in from) {
        if (r in to) roots.add(r)
        graphDFS(
                from = nextNodes(r),
                preorderPred = {
                    stack.offer(it)
                    if (to.contains(it)) {
                        subgraphNodes.addAll(stack)
                        subgraphNodes.add(r)
                        roots.add(r)
                    }
                    false
                },
                postorderPred = { stack.peek(); stack.pop(); false },
                nextNodes = { val nn = nextNodes(it); roots.removeAll(nn); nn },
                checkTraversal = { true } // \todo doublecheck traversal checking for path finding
        )
    }
    if (log.isTraceEnabled) {
        log.trace("subgraph roots: ${roots.joinToString(", ", "(", ")")}")
        log.trace("subgraph nodes: ${subgraphNodes.joinToString(", ", "(", ")")}")
    }
    // second phase - apply dfs to subgraph
    return graphDFS(
            from = roots,
            preorderPred = preorderPred,
            postorderPred = postorderPred,
            nextNodes = { nextNodes(it).filter { subgraphNodes.contains(it) } },
            checkTraversal = checkTraversal)
}


