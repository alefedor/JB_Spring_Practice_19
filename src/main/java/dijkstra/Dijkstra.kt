package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance.get(), o2!!.distance.get()) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathSequential(start: Node, destination: Node): Int {
    start.distance.set(0)
    val q = PriorityQueue<Node>(NODE_DISTANCE_COMPARATOR)
    q.add(start)
    while (q.isNotEmpty()) {
        val cur = q.poll()
        for (e in cur.outgoingEdges) {
            if (e.to.distance.get() > cur.distance.get() + e.weight) {
                e.to.distance.set(cur.distance.get() + e.weight)
                q.remove(e.to) // inefficient, but used for tests only
                q.add(e.to)
            }
        }
    }
    return destination.distance.get()
}

data class EstimatedDistance(val distance: Int,  val node: Node)

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node, destination: Node): Int {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance.set(0)

    val estimatedDistanceComparator = Comparator.comparingInt<EstimatedDistance> { it.distance }
    val queue = PriorityQueue<EstimatedDistance>(estimatedDistanceComparator)
    queue.add(EstimatedDistance(0, start))

    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker

    val workingThreadsNumber = AtomicInteger(workers)

    repeat(workers) {

        thread {
            var isWorking = true

            val work = {
                if (!isWorking) {
                    workingThreadsNumber.incrementAndGet()
                    isWorking = true
                }
            }

            val doNotWork = {
                if (isWorking) {
                    workingThreadsNumber.decrementAndGet()
                    isWorking = false
                }
            }

            while (true) {
                val currentNode: Node? = synchronized(queue) {
                    while (queue.isNotEmpty() && queue.first().distance > queue.first().node.distance.get()) {
                        queue.poll() // delete useless estimations
                    }

                    if (queue.isNotEmpty())
                        work()
                    else
                        doNotWork()

                    queue.poll()?.node
                }

                if (currentNode == null) {
                    if (workingThreadsNumber.get() == 0)
                        break;
                    else
                        continue
                }

                val distanceFrom = currentNode.distance.get()

                for (edge in currentNode.outgoingEdges) {
                    while (true) {
                        val distanceTo = edge.to.distance.get()

                        if (distanceTo <= distanceFrom + edge.weight)
                            break

                        if (edge.to.distance.compareAndSet(distanceTo, distanceFrom + edge.weight)) {
                            synchronized(queue) {
                                queue.add(EstimatedDistance(distanceFrom + edge.weight, edge.to))
                            }
                            break
                        }
                    }
                }
            }

            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()

    return destination.distance.get()
}