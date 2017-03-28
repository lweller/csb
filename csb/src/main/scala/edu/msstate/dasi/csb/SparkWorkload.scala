package edu.msstate.dasi.csb

import org.apache.spark.graphx.{Edge, EdgeDirection, Graph, VertexId, VertexRDD}
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object SparkWorkload extends Workload {
  /**
   * The following is used together with RDD::foreach() to force the RDD computation.
   */
  private def doNothing(x: Any): Unit = {}

  /**
   * The number of vertices in the graph.
   */
  def countVertices[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Unit = graph.numVertices

  /**
   * The number of edges in the graph.
   */
  def countEdges[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Unit = graph.numEdges

  /**
   * The degree of each vertex in the graph.
   * @note Vertices with no edges not considered.
   */
  def degree[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Unit = graph.degrees.foreach(doNothing)

  /**
   * The in-degree of each vertex in the graph.
   * @note Vertices with no incoming edges are not considered.
   */
  def inDegree[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Unit = graph.inDegrees.foreach(doNothing)

  /**
   * The out-degree of each vertex in the graph.
   * @note Vertices with no outgoing edges are not considered.
   */
  def outDegree[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Unit = graph.outDegrees.foreach(doNothing)

  /**
   * Run a dynamic version of PageRank returning a graph with vertex attributes containing the
   * PageRank and edge attributes containing the normalized edge weight.
   */
  def pageRank[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], tol: Double = 0.001, resetProb: Double = 0.15): Unit = {
    graph.pageRank(tol, resetProb)
  }

  /**
   * Breadth-first Search: returns the shortest directed-edge path from src to dst in the graph. If no path exists,
   * returns the empty list.
   */
  def bfs[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], src: VertexId, dst: VertexId): Unit = {
//    if (src == dst) return List(src)
    if (src == dst) return

    // The attribute of each vertex is (dist from src, id of vertex with dist-1)
    var g: Graph[(Int, VertexId), ED] =
      graph.mapVertices((id, _) => (if (id == src) 0 else Int.MaxValue, 0L)).cache()

    // Traverse forward from src
    var dstAttr = (Int.MaxValue, 0L)
    while (dstAttr._1 == Int.MaxValue) {
      val msgs = g.aggregateMessages[(Int, VertexId)](
        e => if (e.srcAttr._1 != Int.MaxValue && e.srcAttr._1 + 1 < e.dstAttr._1) {
          e.sendToDst((e.srcAttr._1 + 1, e.srcId))
        },
        (a, b) => if (a._1 < b._1) a else b).cache()

//      if (msgs.count == 0) return List.empty
      if (msgs.count == 0) return

      g = g.ops.joinVertices(msgs) {
        (_, oldAttr, newAttr) =>
          if (newAttr._1 < oldAttr._1) newAttr else oldAttr
      }.cache()

      dstAttr = g.vertices.filter(_._1 == dst).first()._2
    }

    // Traverse backward from dst and collect the path
    var path: List[VertexId] = dstAttr._2 :: dst :: Nil
    while (path.head != src) {
      path = g.vertices.filter(_._1 == path.head).first()._2._2 :: path
    }

//    path
  }

  /**
   * Collects list of neighbors based solely on incoming direction, and returns a list of
   * those neighbors as well as their node attribute
   * @param graph The input graph
   *
   * @return RDD of Arrays which contain VertexId and VD for each neighbor
   */
  def inNeighbors[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED]): Unit = {
    graph.collectNeighbors(EdgeDirection.In).foreach(doNothing)
  }

  /**
   * Collects list of neighbors based solely on outgoing direction, and returns a list of
   * those neighbors as well as their node attribute
   * @param graph The input graph
   * @tparam VD Node attribute type for input graph
   * @tparam ED Edge attribute type for input graph
   * @return RDD of Arrays which contain VertexId and VD for each neighbor
   */
  def outNeighbors[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED]): Unit = {
    graph.collectNeighbors(EdgeDirection.Out).foreach(doNothing)
  }

  /**
   * Collects list of neighbors in both incoming and outgoing direction, and returns a list of
   * those neighbors as well as their node attribute
   * @param graph The input graph
   * @tparam VD Node attribute type for input graph
   * @tparam ED Edge attribute type for input graph
   * @return RDD of Arrays which contain VertexId and VD for each neighbor
   */
  def neighbors[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED]): Unit = {
    graph.collectNeighbors(EdgeDirection.Either).foreach(doNothing)
  }

  /**
   * Grabs all of the edges entering a node by grouping the edges by dstId attribute
   * @param graph The input graph
   * @tparam VD Node attribute type for input graph
   * @tparam ED Edge attribute type for input graph
   * @return RDD containing pairs of (VertexID, Iterable of Edges) for every vertex in the graph
   */
  def inEdges[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED]): Unit = {
    graph.edges.groupBy(record => record.dstId).foreach(doNothing)
  }

  /**
   * Grabs all of the edges exiting a node by grouping the edges by srcId attribute
   * @param graph The input graph
   * @tparam VD Node attribute type for input graph
   * @tparam ED Edge attribute type for input graph
   * @return RDD containing pairs of (VertexID, Iterable of Edges) for every vertex in the graph
   */
  def outEdges[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED]): Unit = {
    graph.edges.groupBy(record => record.srcId).foreach(doNothing)
  }

  /**
   * Computes the connected component membership of each vertex and return a graph with the vertex
   * value containing the lowest vertex id in the connected component containing that vertex.
   */
  def connectedComponents[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED], maxIterations: Int = Int.MaxValue): Unit = {
    graph.connectedComponents(maxIterations)
  }

  /**
   * Compute the strongly connected component (SCC) of each vertex and return a graph with the
   * vertex value containing the lowest vertex id in the SCC containing that vertex.
   */
  def stronglyConnectedComponents[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED], numIter: Int): Unit = {
    graph.stronglyConnectedComponents(numIter)
  }

  /**
   * Computes the number of triangles passing through each vertex.
   */
  def triangleCount[VD: ClassTag, ED: ClassTag](graph: Graph[VD,ED]): Unit = {
    graph.triangleCount()
  }

  /**
   * Computes the betweenness centrality of a graph given a max k value.
   *
   * Credits: Daniel Marcous (https://github.com/dmarcous/spark-betweenness/blob/master/src/main/scala/com/centrality/kBC/KBetweenness.scala)
   *
   * @param graph The input graph
   * @param k The maximum number of hops to compute
   * @return Graph containing the betweenness double values
   */
  def betweennessCentrality[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], k: Int): Unit = {
    KBetweenness.run(graph, k)
  }

  /**
   * Computes the closeness centrality of a node using the formula N/(sum(distances)).
   */
  def closenessCentrality[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], vertex: VertexId): Double = {
    ClosenessCentrality.getClosenessOfVert(vertex, graph)
  }

  /**
   * Computes the shortest path from a source vertex to all other vertices.
   */
  def sssp[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], src: VertexId): Unit = {
    for (dst <- graph.vertices.keys.toLocalIterator) {
      bfs(graph, src, dst)
    }
  }

  /**
   * Finds all edges with a given property.
   */
  def edgesWithProperty[VD: ClassTag](graph: Graph[VD, EdgeData], filter: Edge[EdgeData] => Boolean): RDD[Edge[EdgeData]] = {
    graph.edges.filter(filter)
  }

  /**
   * Verifies if graph contains a subgraph that is isomorphic to pattern.
   */
  def subgraphIsomorphism[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], pattern: Graph[VD, ED]): Unit = ???
}
