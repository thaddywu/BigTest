package udfExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Runner extends Logging {

    private static UDFDecompilerAndExtractor udf_ex = null;
    public static int loop_bound() {
 	   return Configuration.K_BOUND;   
    }
    public static void main(String[] args) {
        String test_name = "Test2";
        if(args.length >= 1) {
            test_name = args[0];
            System.out.println("--Using > ..." + test_name);
        //    System.exit(1);
        }
        else{
            System.out.println("--Please provide test name. using Test2...");
        }
        
        
        
        try {
        // Set this for now
        //Todo: In case of complete dataflow program this class file path will automatically generated by the tool using just the Spark App.
        //String classfile = "/Users/amytis/Projects/Test-Minimization-in-Big-Data/udf_extractor/target/scala-2.11/classes/"+test_name+"$";
        //String conf_file = "/Users/amytis/Projects/Test-Minimization-in-Big-Data/udf_extractor/src/main/scala/"+test_name+".conf";

        // String classfile = "/Users/malig/workspace/up_jpf/SymExec/bin/examples/"+test_name+"$";
        // String conf_file = "/Users/malig/workspace/up_jpf/SymExec/src/examples/"+test_name+".conf";
        // String classfile = Configuration.BenchBin + test_name + "$" ;
        String classfile = test_name ;
            
        // String conf_file = "/mnt/ssd/thaddywu/bigTest/BigTest/BenchmarksFault/src/" + test_name + ".conf" ;


        // Input arguments to the UDFS  (UDF-name --> input args) .
        // Required for SPF. The Ids appended to the operator name are in reverse order.
        // Write the input args in the <classname>.conf file

        // Configuration.readSPFInputArgs(conf_file);
        // @thaddywu: remove configuration file reading

        /* Manually inserting input args
        Configuration.map_args.put("filter1" , "1");
        Configuration.map_args.put("map2" , "2");
        Configuration.map_args.put("map3" , "\"3\"");
        */

        // String outputJava = Configuration.JPF_HOME+ "jpf-symbc/src/examples/";
        // String outputJava = getModelPath(); // edited by @thaddywu
        createDirectory(getModelPath());
        String classname = classfile.split("/")[classfile.split("/").length - 1];
        String classFile_jad = getModelPath() + "/" + classname + ".jad"; // classname + ".jad";

        // String jpfModel = Configuration.JPF_HOME+ "jpf-symbc/src/examples/";//+fixClassName(classname)+".jpf";
        // String jpfModel = getModelPath();

        // Decompile and extract the UDF
        // Compiling the newly extracted UDF
        udf_ex = new UDFDecompilerAndExtractor(classfile, classFile_jad, getModelPath());
        udf_ex.ParseFilesInDir(getModelPath());
      //  for(JPFDAGNode j: udf_ex.jpf_dag){
            // Run JPF on the UDF
        //    System.out.println(j.operator_name+" "+j.jpf_file);
          //   runCommand(new String[]{"java", "-jar", jpfJar, j.jpf_file}, Configuration.JAVA_RUN_DIR);
       // }
        }catch(Exception e) {
        	e.printStackTrace();
        }

    }

    public static JPFDAGNode getDataFlowDAG() {
        // return Configuration.program_dag == null ? udf_ex.getDAG() : Configuration.program_dag;
        // @thaddywu: Don't read config from files.
        return udf_ex.makeCFG();
    }
    

    public static String getModelPath() {
        return Configuration.Rundir;
        // @thaddywu
        // return Configuration.JPF_HOME+ "jpf-symbc/src/examples/";
    }


    public static void createDirectory(String dir){
        File file = new File(dir);
        if(!file.exists()){
            loginfo(LogType.INFO, "Output directory does not exist: Creating one ...");
            file.mkdirs();
        }else{
            loginfo(LogType.INFO, "Output directory already exist");
        }
    }

    public static void runCommand(String[] args, String dir) {
    // build the system command we want to run
        String s = "";
        for (String a : args) {
            s = s + "  " + a;
        }
         loginfo(LogType.INFO , "Running Command : " + s )  ;

        try {
            List<String> commands = new ArrayList<String>();
            commands.add("/bin/sh");
            commands.add("-c");
            commands.add(s);
            //commands.add("echo $JAVA_HOME");

            // execute the command
            SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands, dir);
            int result = commandExecutor.executeCommand();

            // get the stdout and stderr from the command that was run
            StringBuilder stdout = commandExecutor.getStandardOutputFromCommand();
            StringBuilder stderr = commandExecutor.getStandardErrorFromCommand();

            // print the stdout and stderr
            loginfo(LogType.INFO , stdout.toString());
            loginfo(LogType.WARN , stderr.toString());
    } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String fixClassName(String s){
       return  s.replace("$" , "");
    }

}

