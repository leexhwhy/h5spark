/*
hdf5 reader in scala
*/
/************************************************************
  This example shows how to read and write data to a
  dataset by filename/datasetname.  The program first writes integers
  in a hyperslab selection to a dataset with dataspace
  dimensions of DIM_XxDIM_Y, then closes the file.  Next, it
  reopens the file, reads back the data, and outputs it to
  the screen.  Finally it reads the data again using a
  different hyperslab selection, and outputs the result to
  the screen.
 ************************************************************/
package org.nersc.io

import ncsa.hdf.hdf5lib._
import ncsa.hdf.hdf5lib.H5._
import ncsa.hdf.hdf5lib.HDF5Constants._
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException

import org.slf4j.LoggerFactory
import scala.io.Source
import java.io.File

//import ncsa.hdf.`object`.{Dataset,HObject}

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext._

object read {

  /**
   * Gets an NDimensional array of from a hdf
   * ***** @param FILENAME where the hdf file is located
   * *****@param DATASETNAME the hdf variable to search for
   * @param x:string contains (filename, datasetname), a line in a csv file
   * @return
   */

    def readone(x:String): (Array[Array[Float]])= {
    	var para =x.split(",")
    	var FILENAME = para{0}.trim
    	var DATASETNAME:String = para{1}.trim
    	var DIM_X: Int = 1
    	var DIM_Y: Int = 1
    	var RANK: Int = 1
    	var logger = LoggerFactory.getLogger(getClass)
    	var file_id = -2
    	var dataset_id = -2
	var dataspace_id = -2 
    	var dset_dims = new Array[Long](2)
    	dset_dims =Array(1,1)

	//Open an existing file
	try{
      	 file_id = H5Fopen(FILENAME, H5F_ACC_RDONLY, H5P_DEFAULT)
    	}
    	catch{
     	 case e: HDF5LibraryException=>  println("File open error,filename:" + FILENAME+",file_id: "+file_id)
    	}

    	if (file_id < 0) {
	 logger.info("File open error" + FILENAME)
    	}
   	
	//Open an existing dataset/variable
    	try{
    	 dataset_id = H5Dopen(file_id,DATASETNAME, H5P_DEFAULT)
    	}
    	catch{
	 case e: Exception=> println("Dataset open error:" + DATASETNAME+"\nDataset_id: "+dataset_id)	 
    	}
    	if (dataset_id < 0) logger.info("File open error:" + FILENAME)
    
    	//Get dimension information of the dataset
    	try{
    	 dataspace_id =  H5Dget_space(dataset_id)
    	 H5Sget_simple_extent_dims(dataspace_id, dset_dims,null)
    	}
    	catch{
    	 case e: Exception=>println("Dataspace open error,dataspace_id: "+dataspace_id)
    	}
   	var dset_data = Array.ofDim[Float](dset_dims(0).toInt,dset_dims(1).toInt)    
    	// Read the data using the default properties.
    	var dread_id = -1
    	if (dataset_id >= 0){
       	  dread_id = H5Dread(dataset_id, H5T_NATIVE_FLOAT,
          H5S_ALL, H5S_ALL,
          H5P_DEFAULT, dset_data)
    	}
   	if(dread_id<0)
     	  logger.info("Dataset open error" + FILENAME)
  
    	dset_data
  }


    def readonep(x:String): (Array[Array[Double]])= {
        var para =x.split(",")
        var FILENAME = para{0}.trim
        var DATASETNAME:String = para{1}.trim
        var start = para{2}.trim.toLong
        var end = para{3}.trim.toLong
        println("start:"+start+"end:"+end)
        var DIM_X: Int = 1
        var DIM_Y: Int = 1
        var RANK: Int = 1
        var logger = LoggerFactory.getLogger(getClass)
        var file_id = -2
        var dataset_id = -2
        var dataspace_id = -2
        var dset_dims = new Array[Long](2)
        dset_dims =Array(1,1)

        //Open an existing file
        try{
         file_id = H5Fopen(FILENAME, H5F_ACC_RDONLY, H5P_DEFAULT)
        }
        catch{
         case e: HDF5LibraryException=>  println("File open error,filename:" + FILENAME+",file_id: "+file_id)
        }

        if (file_id < 0) {
         logger.info("File open error" + FILENAME)
        }
        else println("File open ok"+FILENAME+"\n")
        //Open an existing dataset/variable
        try{
         dataset_id = H5Dopen(file_id,DATASETNAME, H5P_DEFAULT)
        }
        catch{
         case e: Exception=> println("Dataset open error:" + DATASETNAME+"\nDataset_id: "+dataset_id)
        }
        if (dataset_id < 0) logger.info("Dataset open error:" + DATASETNAME)
        else println("Dataset open ok:"+DATASETNAME)

        //Get dimension information of the dataset
        try{
         dataspace_id =  H5Dget_space(dataset_id)
         H5Sget_simple_extent_dims(dataspace_id, dset_dims,null)
        }
        catch{
         case e: Exception=>println("Dataspace open error,dataspace_id: "+dataspace_id)
        }
	println("dset_dims(0):"+dset_dims(0)+"dset_dims[1]:"+dset_dims(1))
        //var dset_data = Array.ofDim[Float](dset_dims(0).toInt,dset_dims(1).toInt)
	var dset_data = Array.ofDim[Double]((end-start).toInt,dset_dims(1).toInt)
        // Read the data using the default properties.
	var start_dims= new Array[Long](2)
	var count_dims= new Array[Long](2)
        start_dims=Array(start,0)
	count_dims=Array(end-start,dset_dims(1).toLong)
        H5Sselect_hyperslab(dataspace_id, H5S_SELECT_SET,start_dims, null , count_dims, null)
        var dread_id = -1
        if (dataset_id >= 0){
		dread_id = H5Dread(dataset_id, H5T_NATIVE_DOUBLE,
          H5S_ALL, dataspace_id,
          H5P_DEFAULT, dset_data)
        }
        if(dread_id<0)
          logger.info("Data read error:" + dread_id)
	else println("Data read ok\n")

        dset_data
  }


  def main(args: Array[String]): Unit = {

    /* test without spark
    val input = Source.fromFile("src/resources/hdf5/scalafilelist")
    for (line <- input.getLines){
	var dset=read.readone(line)
        //println(dset.deep.mkString("\n"))
    }

    */
    //$csvlist $partition $repartition $inputfile $dataset $rows
    if(args.length <6) {
	println("arguments less than 6")
	System.exit(1);
    }
    val csvfile =  args(0)
    val partitions = args(1).toInt
    val repartition = args(2).toInt
    val input = args(3)
    val variable = args(4)
    val rows = args(5).toInt
    val sparkConf = new SparkConf().setAppName("h5spark-scala")
    val sc =new SparkContext(sparkConf)
    val dsetrdd =  sc.textFile(csvfile,minPartitions=partitions).repartition(repartition).flatMap(read.readonep)
    //val dsetrdd = file_path.flatMap(read.readonep)
    dsetrdd.cache()
    var xcount= dsetrdd.count()
    println("\nRDD Count: "+xcount+"i.e., total number of rows of all hdf5 files\n")
    //println(dset.deep.mkString("\n"))
    
  }

}
