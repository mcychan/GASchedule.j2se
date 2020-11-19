package hk.edu.gaSchedule;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import hk.edu.gaSchedule.algorithm.Configuration;
// import hk.edu.gaSchedule.algorithm.GeneticAlgorithm;
import hk.edu.gaSchedule.algorithm.NsgaII;
import hk.edu.gaSchedule.algorithm.Schedule;

public class ConsoleApp
{
    public static void main(String[] args)
    {
    	try {
	        System.out.println(String.format("GaSchedule Version %s . Making a Class Schedule Using a Genetic Algorithm (NSGA-II).", "1.1.0"));
	        System.out.println("Copyright (C) 2020 Miller Cy Chan.");
	
	        final String FILE_NAME = args.length > 0 ? args[0] : "GaSchedule.json";
	        final long startTime = System.currentTimeMillis();

	        Configuration configuration = new Configuration();
	        File targetFile = new File(System.getProperty("user.dir") + "/" + FILE_NAME);
	        if(!targetFile.exists())
	        	targetFile = new File(new File(ConsoleApp.class.getResource("/").toURI()).getParentFile() + "/" + FILE_NAME);
	        configuration.parseFile(targetFile.getAbsolutePath());	        
	        
	        // GeneticAlgorithm<Schedule> alg = new GeneticAlgorithm<>(new Schedule(configuration), 2, 2, 80, 3);
	        NsgaII<Schedule> alg = new NsgaII<>(new Schedule(configuration), 2, 2, 80, 3);
	        alg.run(9999, 0.999);
	        
	        String htmlResult = HtmlOutput.getResult(alg.getResult());
	
	        String tempFilePath = System.getProperty("java.io.tmpdir") + FILE_NAME.replace(".json", ".htm");
	        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFilePath))))
	        {
	            writer.write(htmlResult);
	            writer.flush();
	        }
	
	        double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
	        System.out.println(String.format("\nCompleted in %f secs.", seconds));
	        if (Desktop.isDesktopSupported()) {
	            try {
	                Desktop.getDesktop().open(new File(tempFilePath));
	            } catch (Exception ex) {
	                // no application registered for html
	            }
	        }
    	}
    	catch(Exception ex) {
    		ex.printStackTrace();
    	}
    }
}
