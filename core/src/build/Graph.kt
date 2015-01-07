
package komplex

import java.util.HashSet

// generic traverse checker with generators
public class TraversedChecker<Node>() {
    val traversed = HashSet<Node>()
    public fun checkAdd(n: Node): Boolean = traversed.add(n)
    public fun check(n: Node): Boolean = !traversed.contains(n)
}

public fun makeVisitedTraversalChecker<Node>(): (Node) -> kotlin.Boolean {
    val checker = TraversedChecker<Node>()
    return { (n: Node) -> checker.checkAdd(n) }
}

// generic preorder BFS
// \todo check if keeping separate preorder version makes sense e.g. performance-wise
public fun graphPreorderBFS<Node>(from: Iterable<Node>,
                                  preorderPred: (Node) -> Boolean,
                                  nextNodes: (Node) -> Iterable<Node>,
                                  checkTraversal: (Node) -> Boolean = makeVisitedTraversalChecker()
                                 ): Boolean {

    val queue: java.util.Queue<Node> = linkedListOf() // \todo check/optimize collection
    var currentQueue = from;

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
public fun graphBFS<Node>(from: Iterable<Node>,
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
        if (pos >= stack.size()) break
        currentQueue = nextNodes(stack.get(pos++))
    }
    // process collected nodes
    pos = stack.size()
    while (pos > 0) {
        if (postorderPred(stack.get(--pos))) return true // found on postorder
        // \todo optimization: remove processed edge here?
    }
    return false
}


// generic DFS, recursive impl \todo: make non-recursive impl
public fun graphDFS<Node>(from: Iterable<Node>,
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


// generic graph
public trait Graph<Node> {
    public fun add(node: Node)
}