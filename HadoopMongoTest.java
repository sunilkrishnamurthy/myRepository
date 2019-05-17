package com.finicspro.processing.excel;

import java.io.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.*;
import com.mongodb.hadoop.util.MongoConfigUtil;

public class HadoopMongoTest {
    public static class TokenizerMapper extends Mapper<Object, BasicDBObject, Text, Text> {
        @Override
        public void map( Object key, BasicDBObject value, Context context ) throws IOException, InterruptedException {
            FileOutputStream fos = new FileOutputStream("d:\\temp\\map.txt", true);
            fos.write( (key + " " + value + "\n").getBytes());
            fos.close();
            
            System.out.println("inside map ==============>>>>>>>>>" + key + " " + value);
            String name = value.get( "name" ).toString();
            String score = value.get( "score" ).toString();
            context.write( new Text( name.toString() ), new Text( score.toString() ) );
        }
    }

    public static class TextReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce( Text key, Iterable<Text> values, Context context ) throws IOException, InterruptedException {
            FileOutputStream fos = new FileOutputStream("d:\\temp\\reduce.txt", true);
            System.out.println("inside reducer ==============>>>>>>>>>" + key + " " + values);
            String s_res = "";
            for ( Text val : values ) {
                s_res += val.toString();
                fos.write( (key + " " + val + "\n").getBytes());
                
            }
            context.write( key, new Text(s_res) );
            
            fos.close();
        }
    }

    public static void main( String[] args ) throws Exception {
        // input is mongo, output is hdfs
        // ------------------------------
        Configuration conf = new Configuration();
        Job job = Job.getInstance( conf, "word count" );
        MongoConfigUtil.setInputURI( job.getConfiguration(), "mongodb://localhost/Acadgild.user_details" );
        job.setJarByClass( HadoopMongoTest.class );
        job.setNumReduceTasks( 1 );
        job.setMapperClass( TokenizerMapper.class );
        job.setCombinerClass( TextReducer.class );
        job.setReducerClass( TextReducer.class );
        job.setMapOutputKeyClass( Text.class );
        job.setMapOutputValueClass( Text.class );
        job.setInputFormatClass( com.mongodb.hadoop.MongoInputFormat.class );
        job.setOutputKeyClass( Text.class );
        job.setOutputValueClass( Text.class );
        FileOutputFormat.setOutputPath( job, new Path( "hadoopMongoTestResult" ) );
        System.exit( job.waitForCompletion( true ) ? 0 : 1 );
        
        
        // input is mongo, output is also mongo
        // ------------------------------
        /*Configuration conf = new Configuration();
        conf.set("mongo.output.uri", "mongodb://localhost/Acadgild.user_details_res" );
        Job job = Job.getInstance( conf, "word count" );
        MongoConfigUtil.setInputURI( job.getConfiguration(), "mongodb://localhost/Acadgild.user_details" );
        job.setJarByClass( HadoopMongoTest.class );
        job.setNumReduceTasks( 0 );
        job.setMapperClass( TokenizerMapper.class );
        job.setCombinerClass( IntSumReducer.class );
        job.setReducerClass( IntSumReducer.class );
        job.setMapOutputKeyClass( Text.class );
        job.setMapOutputValueClass( Text.class );
        job.setInputFormatClass( com.mongodb.hadoop.MongoInputFormat.class );
        job.setOutputFormatClass( com.mongodb.hadoop.MongoOutputFormat.class );
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(BSONWritable.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        
        System.exit( job.waitForCompletion( true ) ? 0 : 1 );*/
    }
}
