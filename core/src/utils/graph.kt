
package komplex.utils

import java.util.HashSet
import java.util.ArrayDeque

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

// \todo invert predicates meaning - instead of "cancel?" use "continue?"

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


public fun subgraphDFS<Node>(from: Iterable<Node>,
                             to: Set<Node>,
                             preorderPred: (e: Node) -> Boolean,
                             postorderPred: (Node) -> Boolean,
                             nextNodes: (n: Node) -> Iterable<Node>,
                             checkTraversal: (Node) -> Boolean = makeVisitedTraversalChecker()
                            ): Boolean {

    val subgraphNodes = hashSetOf<Node>()
    val roots = hashSetOf<Node>()
    val stack = ArrayDeque<Node>()
    val sharedCheckTraversal = makeVisitedTraversalChecker<Node>()
    // first phase - calculating reachability graph and finding it's roots
    for (r in from)
        graphDFS(from = nextNodes(r),
                 preorderPred = {
                     stack.offer(it)
                     if (to.contains(it)) {
                         subgraphNodes.addAll(stack)
                         subgraphNodes.add(r)
                         roots.add(r)
                     }
                     false },
                 postorderPred = { stack.pop(); false},
                 nextNodes = { val nn = nextNodes(it); roots.removeAll(nn); nn },
                 checkTraversal = sharedCheckTraversal)
    // second phase - apply dfs to subgraph
    return graphDFS(roots, preorderPred, postorderPred, { nextNodes(it).filter { subgraphNodes.contains(it) } }, checkTraversal)
}

