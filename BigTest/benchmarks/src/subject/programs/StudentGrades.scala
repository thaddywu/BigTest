package subject.programs

import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by malig on 3/27/18.
  */
object StudentGrades {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    conf.setMaster("local[*]")
    conf.setAppName("Weather")
    val data1 = Array(":0\n:0",	":0\n:41",	":0\n ,:0",	":0\n ,:41",	":41\n:0"	,":41\n:41"	,":41\n ,:0",	":41\n ,:41",	" ,:0\n:0"	,
      " ,:0\n:41"	," ,:0\n ,:0",	" ,:0\n ,:41"	," ,:41\n ,:0",	" ,:41\n:41",	" ,:41\n ,:0",	" ,:41\n ,:41",	"",	" ,")

    val startTime = System.currentTimeMillis();
    val sc = new SparkContext(conf)
    for (i <- 0 to data1.length - 1) {
      try {

        val map1 = sc.parallelize(Array(data1(i))).flatMap(l => l.split("\n")).flatMap{ line =>
        val arr = line.split(",")
        arr
      }
      .map{  s =>
        val a = s.split(":")
        (a(0) , a(1).toInt)
      }
      .map { a =>
        if (a._2 > 40)
          (a._1 + " Pass", 1)
        else
          (a._1 + " Fail", 1)
      }
      .reduceByKey(_ + _)
      .filter(v => v._2 > 1)
      .collect
      .foreach(println)
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }

    println("Time: " + (System.currentTimeMillis() - startTime))
  }

}


/**
  *
  *

val text = sc.textFile("hdfs://scai01.cs.ucla.edu:9000/clash/datasets/bigsift/studentGrades/*").sample(false, 0.001)
text.cache
  text.count

text.flatMap{ line =>
val arr = line.split(",")
arr
}.map{  s =>
  val a = s.split(":")
        (a(0) , a(1).toInt)
      }.map { a =>
        if (a._2 > 40)
          (a._1 + " Pass", 1)
        else
          (a._1 + " Fail", 1)
      }.reduceByKey(_ + _).filter(v => v._2 > 1).count

  */
*/

/***
Big Test Conf
filter1 > "",1
map3> "",1
map4 > "CS:123"
reduceByKey2 > {1,2,3,4}
flatMap5 > "a,a"
DAG >filter1-reduceByKey2:reduceByKey2-map3:map3-map4:map4-flatMap5
*/