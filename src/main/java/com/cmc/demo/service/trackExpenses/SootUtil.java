package com.cmc.demo.service.trackExpenses;

import java.util.Arrays;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;

public class SootUtil {
	  public static void main(String[] args) {
		  try {

	            String mainClass = "com.cmc.demo.service.trackExpenses.PaymentService"; // 确保类名正确
	            String separator = System.getProperty("path.separator");
	            String projectBuildDir = System.getProperty("user.dir") +"\\"+ "target"+"\\"+"classes"; // 示例路径
	            System.out.println(projectBuildDir);
	            String classPath = projectBuildDir + separator + System.getProperty("java.class.path");

	            G.reset();
	            Options.v().set_whole_program(true);
	            Options.v().set_app(true);
	            Options.v().set_main_class(mainClass);
	            Options.v().set_src_prec(Options.src_prec_java);

	            Options.v().set_prepend_classpath(true);
	            Options.v().set_verbose(true);
	            Options.v().set_keep_line_number(true);
	            Options.v().set_output_format(Options.output_format_jimple);
	            Options.v().set_allow_phantom_refs(true);
	            Options.v().set_process_dir(Arrays.asList(projectBuildDir));
	            Options.v().set_soot_classpath(classPath);
	            String outputDir = "path/to/output";  // 替换为实际的输出目录路径
	            Options.v().set_output_dir(outputDir);
	            Scene.v().loadNecessaryClasses();
	            PackManager.v().runPacks();
	            PackManager.v().writeOutput();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		    System.out.println("Java version: " + System.getProperty("java.version"));
	        System.out.println("Java class path: " + System.getProperty("java.class.path"));
	  }
	void ccc() {

	}
}
