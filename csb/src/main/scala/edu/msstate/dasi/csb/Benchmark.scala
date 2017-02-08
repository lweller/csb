package edu.msstate.dasi.csb

import org.apache.log4j.{Level, Logger}
import scopt.OptionParser

/**
  * Created by spencer on 11/3/16.
  */

object Benchmark {

  val versionString = "0.2-DEV"

  case class ParamsHelp(
                         /**
                           * Any Arguments
                           */
                         outputGraphPrefix_desc: String = "Prefix to use when saving the output graph",
                         partitions_desc: String = "Number of partitions to set RDDs to.",
                         backend_desc: String = "Backend used to save generated data (fs or neo4j).",
                         checkpointDir_desc: String = "Directory for checkpointing intermediate results. " +
                           "Checkpointing helps with recovery and eliminates temporary shuffle files on disk.",
                         checkpointInterval_desc: String = "Iterations between each checkpoint. Only used if " +
                           "checkpointDir is set.",

                         /**
                           * GenDist Arguments
                           */
                         connLog_desc: String = "Bro IDS conn.log file to augment with a SNORT alert.log.",
                         alertLog_desc: String = "SNORT alert augment with a Bro IDS conn.log.",
                         augLog_desc: String = "Augmented Bro IDS conn.log and SNORT alert.log.",

                         /**
                           * BA Arguments
                           */
                         noProp_desc: String = "Specify whether to generate random properties during generation or not.",
                         numNodesPerIter_desc: String = "The number of nodes to add to the graph per iteration.",
                         seed_desc: String = "Serialized file to use as a seed for BA Graph Generation.",
                         baIter_desc: String = "Number of iterations for Barabasi–Albert model.",

                         /**
                           * Kronecker Arguments
                           */
                         seedMtx_desc: String = "Space-separated matrix file to use as a seed for Kronecker.",
                         kroIter_desc: String = "Number of iterations for Kronecker model.",

                         /**
                           * veracity arguements
                           */
                       veracity_Desc: String = "The veracity metric you want to compute. Options include: degree, inDegree, outDegree, pageRank",
                         veracity_File: String = "The file to save the metric information",
                         seed_Metric: String = "Serialized file to use as a seed",
                         synth_Metric: String = "Serialized file to use as a synth"
                       )

  case class Params(
                     mode: String = "",

                     /**
                       * Any Arguments
                       */
                     outputGraphPrefix: String = "",
                     partitions: Int = 120,
                     backend: String = "fs",
                     checkpointDir: Option[String] = None,
                     checkpointInterval: Int = 10,
                     debug: Boolean = false,

                     /**
                       * GenDist Arguments
                       */
                     connLog: String = "conn.log",
                     alertLog: String = "alert",
                     augLog: String = "aug.log",

                     /**
                       * BA Arguments
                       */
                     noProp: Boolean = false,
                     numNodesPerIter: Long = 120,
                     seed: String = "seed",
                     baIter: Long = 1000,

                     /**
                       * Kronecker Arguments
                       */
                     seedMtx: String = "seed.mtx",
                     kroIter: Int = 10,

                     /**
                       * veracity arguements
                       */
                     metric: String = "hop-plot",
                     metricSave: String = "hop-plotSave",
                     synth: String = "synth"

                   )

  def main(args: Array[String]) {
    val dP = Params()
    val h = ParamsHelp()

    val parser = new OptionParser[Params]("Benchmark") {
      head(s"Benchmark $versionString: a synthetic Graph Generator for the busy scientist.")

      /**
        * All Arguments:
        */
      opt[String]("output")
        .text(s"${h.outputGraphPrefix_desc} default ${dP.outputGraphPrefix}")
        .action((x, c) => c.copy(outputGraphPrefix = x))
      opt[Int]("partitions")
        .text(s"${h.partitions_desc} default: ${dP.partitions}")
        .validate(x => if (x > 0) success else failure("Partition count must be greater than 0."))
        .action((x, c) => c.copy(partitions = x))
      opt[String]("backend")
        .text(s"${h.backend_desc} default: ${dP.backend}")
        .validate(x => if (x == "fs" || x == "neo4j") success else failure("Backend must be fs or neo4j."))
        .action((x, c) => c.copy(backend = x))
      opt[String]("checkpointDir")
        .text(s"${h.checkpointDir_desc} default: ${dP.checkpointDir}")
        .action((x, c) => c.copy(checkpointDir = Some(x)))
      opt[Int]("checkpointInterval")
        .text(s"${h.checkpointInterval_desc} default: ${dP.checkpointInterval}")
        .action((x, c) => c.copy(checkpointInterval = x))
      opt[Unit]("debug")
        .hidden()
        .action((_, c) => c.copy(debug = true))
        .text(s"Debug mode, prints all log output to terminal. default: ${dP.debug}")

      /**
        * GenDist Arguments:
        */
      note("\n")
      cmd("gen_dist").action((_, c) => c.copy(mode = "gen_dist"))
        .text(s"Generate distribution data for a given input dataset.")
        .children(
          arg[String]("bro_log")
            .text(s"${h.connLog_desc} default: ${dP.connLog}")
            .required()
            .action((x, c) => c.copy(connLog = x)),
          arg[String]("alert_log")
            .text(s"${h.alertLog_desc} default: ${dP.alertLog}")
            .required()
            .action((x, c) => c.copy(alertLog = x)),
          arg[String]("aug_log")
            .text(s"${h.augLog_desc} default: ${dP.augLog}")
            .required()
            .action((x, c) => c.copy(augLog = x)),
          arg[String]("seed")
            .text(s"Output file for ${h.seed_desc} default: ${dP.seed}")
            .required()
            .action((x, c) => c.copy(seed = x))
        )

      /**
        * BA Arguments:
        */
      note("\n")
      cmd("ba").action((_, c) => c.copy(mode = "ba"))
        .text(s"Generate synthetic graph using the Barabasi–Albert model.")
        .children(
          opt[Unit]("no-prop")
            .text(s"${h.noProp_desc} default: ${dP.noProp}")
            .action((_, c) => c.copy(noProp = true)),
          opt[Long]("nodes-per-iter")
            .text(s"${h.numNodesPerIter_desc} default: ${dP.numNodesPerIter}")
            .action((x, c) => c.copy(numNodesPerIter = x)),
          arg[String]("seed_vert")
            .text(s"${h.seed_desc} default: ${dP.seed}")
            .required()
            .action((x, c) => c.copy(seed = x)),
          arg[Int]("<# of Iterations>")
            .text(s"${h.baIter_desc} default: ${dP.baIter}")
            .validate(x => if (x > 0) success else failure("Iteration count must be greater than 0."))
            .action((x, c) => c.copy(baIter = x))
        )

      /**
        * Kronecker Arguments
        */
      note("\n")
      cmd("kro").action((_, c) => c.copy(mode = "kro"))
        .text(s"Generate synthetic graph using the Probabilistic Kronecker model.")
        .children(
          opt[Unit]("no-prop")
            .text(s"${h.noProp_desc} default ${dP.noProp}")
            .action((_, c) => c.copy(noProp = true)),
          arg[String]("seed-mtx")
            .text(s"${h.seedMtx_desc} default: ${dP.seedMtx}")
            .required()
            .action((x, c) => c.copy(seedMtx = x)),
          arg[String]("seed_vert")
            .text(s"${h.seed_desc} default: ${dP.seed}")
            .required()
            .action((x, c) => c.copy(seed = x)),
          arg[Int]("<# of Iterations>")
            .text(s"${h.kroIter_desc} default: ${dP.kroIter}")
            .validate(x => if (x > 0) success else failure("Iteration count must be greater than 0."))
            .action((x, c) => c.copy(kroIter = x))
        )
      note("\n")

      cmd("ver").action((_, c) => c.copy(mode = "ver"))
        .text(s"Compute veracity metrics on a given vertices and edge seed files")
        .children(
          arg[String]("seed")
              .text(s"${h.seed_Metric} default: ${dP.seed}")
              .required()
              .action((x,c) => c.copy(seed = x)),
          arg[String]("synth")
              .text(s"${h.synth_Metric}")
              .required()
              .action((x,c) => c.copy(synth = x)),
          arg[String]("metric")
            .text(s"${h.veracity_Desc}")
            .required()
            .action((x,c) => c.copy(metric = x)),
          arg[String]("save_file")
            .text(s"${h.veracity_File}")
            .required()
            .action((x,c) => c.copy(metricSave = x))
        )

    }

    parser.parse(args, dP) match {
      case Some(params) => if (params.mode != "") run(params)
      else {
        println("Error: Must specify command")
        parser.showUsageAsError()
      }
      case _ => sys.exit(1)
    }
  }

  /** * Main function of our program, controls graph generation and other pieces.
    *
    * @param params Parameters for the function to run.
    */
  def run(params: Params): Boolean = {
    if ( ! params.debug ) {
      //turn off annoying log messages
      Logger.getLogger("org").setLevel(Level.OFF)
      Logger.getLogger("akka").setLevel(Level.OFF)
    } else {

    }

    params.mode match {
      case "gen_dist" => run_gendist(params)
      case "ba" => run_synth(params)
      case "kro" => run_synth(params)
      case "ver" => run_ver(params)
      case _ => sys.exit(1)
    }

    sys.exit()
    true
  }

  def run_gendist(params: Params): Boolean = {
    //these two statements create the aug log (conn.log plus alert)
    val logAug = new log_Augment()
    logAug.run(params.alertLog, params.connLog, params.augLog)

    val seed = DataParser.logToGraph(params.augLog, params.partitions)

    println("Vertices #: " + seed.numVertices + ", Edges #: " + seed.numEdges)

    //this is called to read the newly created aug log and create the distributions from it
    new DataDistributions(params.augLog)

    println("Saving the seed graph...")
    var graphPs = null.asInstanceOf[GraphPersistence]
    params.backend match {
      case "fs" => graphPs = new SparkPersistence()
      case "neo4j" => graphPs = new Neo4jPersistence()
    }

    graphPs.saveGraph(seed, "seed", overwrite = true)

    println("Finished saving the synthetic graph.")

    true
  }

  def run_synth(params: Params): Boolean = {
    var graphPs = null.asInstanceOf[GraphPersistence]
    params.backend match {
      case "fs" => graphPs = new SparkPersistence()
      case "neo4j" => graphPs = new Neo4jPersistence()
    }

    val seed = graphPs.loadGraph[VertexData, EdgeData]("seed")
    println("Vertices #: " + seed.numVertices + ", Edges #: " + seed.numEdges)

    val seedDists = new DataDistributions(params.augLog)

    var synthesizer: GraphSynth = null
    params.mode match {
      case "ba" => synthesizer = new BaSynth (params.partitions, params.baIter, params.numNodesPerIter)
      case "kro" => synthesizer = new KroSynth (params.partitions, params.seedMtx, params.kroIter)
    }

    val synth = synthesizer.synthesize(seed, seedDists, !params.noProp)

    println("Saving the synthetic graph...")

    var startTime = System.nanoTime()
    graphPs.saveGraph(synth, params.outputGraphPrefix, overwrite = true)
    var timeSpan = (System.nanoTime() - startTime) / 1e9

    println("Finished saving the synthetic graph. Total time elapsed: " + timeSpan.toString + " s")

    println("Calculating veracity metrics...")
    startTime = System.nanoTime()
    val degVeracity = Degree(seed, synth)
    timeSpan = (System.nanoTime() - startTime) / 1e9
    println(s"\tDegree Veracity: $degVeracity [$timeSpan s]")

    startTime = System.nanoTime()
    val inDegVeracity = InDegree(seed, synth)
    timeSpan = (System.nanoTime() - startTime) / 1e9
    println(s"\tIn Degree Veracity: $inDegVeracity [$timeSpan s]")

    startTime = System.nanoTime()
    val outDegVeracity = OutDegree(seed, synth)
    timeSpan = (System.nanoTime() - startTime) / 1e9
    println(s"\tOut Degree Veracity: $outDegVeracity [$timeSpan s]")

    startTime = System.nanoTime()
    val pageRankVeracity = PageRank(seed, synth)
    timeSpan = (System.nanoTime() - startTime) / 1e9
    println(s"\tPage Rank Veracity: $pageRankVeracity  [$timeSpan s]")

    true
  }

  def run_ver(params: Params): Boolean = {
    var graphPs = null.asInstanceOf[GraphPersistence]
    params.backend match {
      case "fs" => graphPs = new SparkPersistence()
      case "neo4j" => graphPs = new Neo4jPersistence()
    }

    val seed = graphPs.loadGraph[VertexData, EdgeData](params.seed)

    val synth = graphPs.loadGraph[VertexData, EdgeData](params.synth)

    params.metric match {
      case "degree" =>
        val startTime = System.nanoTime()
        val degree = Degree(seed, synth, saveDistAsCSV = true, overwrite = true)
        val timeSpan = (System.nanoTime() - startTime) / 1e9
        println(s"\tPage Rank Veracity: $degree [$timeSpan s]")

      case "inDegree" =>
        val startTime = System.nanoTime()
        val inDegree = InDegree(seed, synth, saveDistAsCSV = true, overwrite = true)
        val timeSpan = (System.nanoTime() - startTime) / 1e9
        println(s"\tPage Rank Veracity: $inDegree [$timeSpan s]")

      case "outDegree" =>
        val startTime = System.nanoTime()
        val outDegree = OutDegree(seed, synth, saveDistAsCSV = true, overwrite = true)
        val timeSpan = (System.nanoTime() - startTime) / 1e9
        println(s"\tPage Rank Veracity: $outDegree [$timeSpan s]")

      case "pageRank" =>
        val startTime = System.nanoTime()
        val pageRank = PageRank(seed, synth, saveDistAsCSV = true, overwrite = true)
        val timeSpan = (System.nanoTime() - startTime) / 1e9
        println(s"\tPage Rank Veracity: $pageRank [$timeSpan s]")

      case _ => println("Invalid metric:" + params.metric)
    }

    true
  }
}