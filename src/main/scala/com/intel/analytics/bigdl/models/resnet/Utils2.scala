package com.intel.analytics.bigdl.models.resnet

import java.nio.ByteBuffer
import java.nio.file.{Files, Path, Paths}

import com.intel.analytics.bigdl.dataset.ByteRecord
import com.intel.analytics.bigdl.utils.{Engine, EngineType, File}
import scopt.OptionParser

import scala.collection.mutable.ArrayBuffer

object Utils2 {
  def engineCoreNumber():Int={
    Engine.coreNumber()
  }
  def engineNodeNumber():Int={
    Engine.nodeNumber()
  }

  def engineType():EngineType= Engine.getEngineType()

  case class TrainParams(
                          folder: String = "./",
                          checkpoint: Option[String] = None,
                          modelSnapshot: Option[String] = None,
                          stateSnapshot: Option[String] = None,
                          optnet: Boolean = false,
                          depth: Int = 20,
                          classes: Int = 10,
                          shortcutType: String = "A",
                          batchSize: Int = 128,
                          nepochs: Int = 165,
                          learningRate: Double = 0.1,
                          weightDecay: Double = 1e-4,
                          momentum: Double = 0.9,
                          dampening: Double = 0.0,
                          nesterov: Boolean = true,
                          graphModel: Boolean = false,
                          warmupEpoch: Int = 0,
                          maxLr: Double = 0.0,
                          validatePortition: Double = 0.02,
                          coreSitePath: String = "/home/yilinma/Documents/IntelliJ_IDEA_Projects/Test/core-site.xml",
                          hbaseSitePath: String = "/home/yilinma/Documents/IntelliJ_IDEA_Projects/Test/hbase-site.xml",
                          hbaseTableName: String = "kfb_512",
                          rowKeyStart: String = "000000001",
                          rowKeyEnd: String = "000000031",
                          modelSavingPath: String = "/home/yilinma/Documents/tmp/trained.obj")

  val trainParser = new OptionParser[TrainParams]("BigDL ResNet Example") {
    head("Train ResNet model on single node")
    opt[String]('f', "folder")
      .text("where you put your training files")
      .action((x, c) => c.copy(folder = x))
    opt[String]("model")
      .text("model snapshot location")
      .action((x, c) => c.copy(modelSnapshot = Some(x)))
    opt[String]("state")
      .text("state snapshot location")
      .action((x, c) => c.copy(stateSnapshot = Some(x)))
    opt[String]("cache")
      .text("where to cache the model")
      .action((x, c) => c.copy(checkpoint = Some(x)))
    opt[Boolean]("optnet")
      .text("shared gradients and caches to reduce memory usage")
      .action((x, c) => c.copy(optnet = x))
    opt[Int]("depth")
      .text("depth of ResNet, 18 | 20 | 34 | 50 | 101 | 152 | 200")
      .action((x, c) => c.copy(depth = x))
    opt[Double]("testsize")
      .text("The percentage of test data used in validation of whole test data set size")
      .action((x, c) => c.copy(validatePortition = x))
    opt[Int]("classes")
      .text("classes of ResNet")
      .action((x, c) => c.copy(classes = x))
    opt[String]("shortcutType")
      .text("shortcutType of ResNet, A | B | C")
      .action((x, c) => c.copy(shortcutType = x))
    opt[Int]("batchSize")
      .text("batchSize of ResNet, 64 | 128 | 256 | ..")
      .action((x, c) => c.copy(batchSize = x))
    opt[Int]("nEpochs")
      .text("number of epochs of ResNet; default is 165")
      .action((x, c) => c.copy(nepochs = x))
    opt[Double]("learningRate")
      .text("initial learning rate of ResNet; default is 0.1")
      .action((x, c) => c.copy(learningRate = x))
    opt[Double]("momentum")
      .text("momentum of ResNet; default is 0.9")
      .action((x, c) => c.copy(momentum = x))
    opt[Double]("weightDecay")
      .text("weightDecay of ResNet; default is 1e-4")
      .action((x, c) => c.copy(weightDecay = x))
    opt[Double]("dampening")
      .text("dampening of ResNet; default is 0.0")
      .action((x, c) => c.copy(dampening = x))
    opt[Boolean]("nesterov")
      .text("nesterov of ResNet; default is trye")
      .action((x, c) => c.copy(nesterov = x))
    opt[Unit]('g', "graphModel")
      .text("use graph model")
      .action((x, c) => c.copy(graphModel = true))
    opt[Int]("warmupEpoch")
      .text("warmup epoch")
      .action((x, c) => c.copy(warmupEpoch = x))
    opt[Double]("maxLr")
      .text("maxLr")
      .action((x, c) => c.copy(maxLr = x))
    opt[String]("coreSitePath")
      .text("absolute path of core-site.xml")
      .action((x, c) => c.copy(coreSitePath = x))
    opt[String]("hbaseSitePath")
      .text("absolute path of hbase-site.xml")
      .action((x, c) => c.copy(hbaseSitePath = x))
    opt[String]("hbaseTableName")
      .text("table name in HBase")
      .action((x, c) => c.copy(hbaseTableName = x))
    opt[String]("rowKeyStart")
      .text("starts from which row key")
      .action((x, c) => c.copy(rowKeyStart = x))
    opt[String]("rowKeyEnd")
      .text("ends at which row key (not included)")
      .action((x, c) => c.copy(rowKeyEnd = x))
    opt[String]("modelSavingPath")
      .text("saving path of the model that will be trained")
      .action((x, c) => c.copy(modelSavingPath = x))
  }

  case class TestParams(
                         folder: String = "./",
                         model: String = "",
                         batchSize: Int = 128
                       )

  val testParser = new OptionParser[TestParams]("BigDL ResNet on Cifar10 Test Example") {
    opt[String]('f', "folder")
      .text("the location of Cifar10 dataset")
      .action((x, c) => c.copy(folder = x))

    opt[String]('m', "model")
      .text("the location of model snapshot")
      .action((x, c) => c.copy(model = x))
      .required()
      .required()
    opt[Int]('b', "batchSize")
      .text("batch size")
      .action((x, c) => c.copy(batchSize = x))
  }

  private[bigdl] def loadTrain(dataFile: String): Array[ByteRecord] = {
    val allFiles = Array(
      dataFile + "/data_batch_1.bin",
      dataFile + "/data_batch_2.bin",
      dataFile + "/data_batch_3.bin",
      dataFile + "/data_batch_4.bin",
      dataFile + "/data_batch_5.bin"
    )

    val result = new ArrayBuffer[ByteRecord]()
    allFiles.foreach(load(_, result))
    result.toArray
  }

  private[bigdl] def loadTest(dataFile: String): Array[ByteRecord] = {
    val result = new ArrayBuffer[ByteRecord]()
    val testFile = dataFile + "/test_batch.bin"
    load(testFile, result)
    result.toArray
  }

  /**
    * load cifar data.
    * read cifar from hdfs if data folder starts with "hdfs:", otherwise form local file.
    * @param featureFile
    * @param result
    */
  private[bigdl] def load(featureFile: String, result : ArrayBuffer[ByteRecord]): Unit = {
    val rowNum = 32
    val colNum = 32
    val imageOffset = rowNum * colNum * 3 + 1
    val channelOffset = rowNum * colNum
    val bufferOffset = 8

    val featureBuffer = if (featureFile.startsWith(File.hdfsPrefix)) {
      ByteBuffer.wrap(File.readHdfsByte(featureFile))
    } else {
      ByteBuffer.wrap(Files.readAllBytes(Paths.get(featureFile)))
    }

    val featureArray = featureBuffer.array()
    val featureCount = featureArray.length / (rowNum * colNum * 3 + 1)

    var i = 0
    while (i < featureCount) {
      val img = new Array[Byte]((rowNum * colNum * 3 + bufferOffset))
      val byteBuffer = ByteBuffer.wrap(img)
      byteBuffer.putInt(rowNum)
      byteBuffer.putInt(colNum)

      val label = featureArray(i * imageOffset).toFloat
      var y = 0
      val start = i * imageOffset + 1
      while (y < rowNum) {
        var x = 0
        while (x < colNum) {
          img((x + y * colNum) * 3 + 2 + bufferOffset) =
            featureArray(start + x + y * colNum)
          img((x + y * colNum) * 3 + 1 + bufferOffset) =
            featureArray(start + x + y * colNum + channelOffset)
          img((x + y * colNum) * 3 + bufferOffset) =
            featureArray(start + x + y * colNum + 2 * channelOffset)
          x += 1
        }
        y += 1
      }
      result.append(ByteRecord(img, label + 1.0f))
      i += 1
    }
  }

}