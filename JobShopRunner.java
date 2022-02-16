import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class JobShopRunner {

	public static void main(String args[]) {

		ArrayList<Instance> instances = readJobInstances("jobshop1.txt");
		System.out.println("Read " + instances.size() + " instances");
		//for (Instance i : instances)
		//	System.out.println(i);

		if (args.length == 0) args = new String[] {"-b", "-d", "-j", "-d:2", "-t:10"};
		for (int i = 0; i < instances.size(); i++) {
			JobShop.run(instances.get(i), args); 
			System.out.println("("+instances.get(i).time_span+")");
			//System.out.println("\n" + instances.get(i).resultToString());
		}
		
		writeResults(instances, "jobshop1_results.txt");
	}

	static void writeResults(ArrayList<Instance> instances, String filename) {
		
		System.out.println("Writing " + filename);
		try {
			File dataFile = new File(filename);
			FileWriter fileWriter = new FileWriter(dataFile);
			BufferedWriter writer = new BufferedWriter(fileWriter);
			
			for (Instance i : instances) {
				writer.write(i.resultToString() + "\n");		
			}

			System.out.println("\nFile written\n");
			writer.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

	}

	static ArrayList<Instance> readJobInstances(String filename) {
		
		ArrayList<Instance> instances = new ArrayList<Instance>();
		int count = 0;

		try {
			File dataFile = new File(filename);
			FileReader fileReader = new FileReader(dataFile);
			BufferedReader reader = new BufferedReader(fileReader);

			String line = "";
			int line_count_down = -1;
			Instance currInstance = new Instance();
			String title = "";			
			while ((line = reader.readLine()) != null) {

				if (line.length() > 0) line = line.trim();
				if (line_count_down > 0) line_count_down--;
				if (line.length() >= 10 && line.substring(0,8).equals("instance")) 
					line_count_down = 5;
				if (line_count_down == 2) 
					title = line;
				if (line_count_down == 1) {
					String[] lineArr = line.split(" ");
					currInstance = new Instance(title, Integer.parseInt(lineArr[0]), 
												Integer.parseInt(lineArr[1]));					
				}
				if (line_count_down == 0 && line.length() > 0 && line.charAt(0) != '+') {
					String[] lineArr = line.split(" ");
					int[] jobs = new int[currInstance.num_machines*2];

					int offset = 0;					
					for (int i = 0; i < lineArr.length; i++) {
						if (lineArr[i].length() > 0)						
							jobs[i-offset] = Integer.parseInt(lineArr[i]);
						else
							offset++;
					}
					currInstance.add(jobs);
				}
				else if (line_count_down == 0 && line.length() > 0 && line.charAt(0) == '+') {
					line_count_down = -1;
					instances.add(currInstance);
				} 
					
			}
			reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		return instances;
	}


}

class Instance {

	public String title;
	public int num_jobs;
	public int num_machines;
	public int[][] jobs;
	int job_ptr;
	public int[][] sched = new int[1][1];
	public int time_span = Integer.MAX_VALUE;
	public int run_time = Integer.MAX_VALUE;		
	public int steps = Integer.MAX_VALUE;
	public int hash_table_size = Integer.MAX_VALUE;

	public Instance() {
		this.title = "";
		this.num_jobs = 0;		
		this.num_machines = 0;
		this.jobs = new int[num_jobs][num_machines];
		this.job_ptr = 0;
	}

	public Instance(String title, int jobs, int machines) {
		this.title = title;
		this.num_jobs = jobs;		
		this.num_machines = machines;
		this.jobs = new int[num_jobs][num_machines];
		this.job_ptr = 0;
	}

	public void add(int[] job) {
		if (job_ptr < num_jobs) {
			jobs[job_ptr] = job;
			job_ptr++;
		}
	}
	
	public void setSolution(int[][] sched, int time_span, int run_time, int steps, int hash_table_size) {
		this.sched = sched;		
		this.time_span = time_span;
		this.run_time = run_time;		
		this.steps = steps;
		this.hash_table_size = hash_table_size;
	}

	public String toString() {
		String str = "";
		str += title + "\n";
		str += num_jobs + " " + num_machines + "\n";
		for (int[] j : jobs)
			str += Arrays.toString(j) + "\n";
		return str;
	}

	public String resultToString() {
		String str = title + "\n";
		for (int row = 0; row < num_jobs; row++) {
			for (int col = 0; col < num_machines*2; col++) {			
				str += sched[row][col];
				if (col < num_machines*2-1) str += " ";
				else str += "\n";
			}
		}
		str += "Time span: " + time_span + "\n";
		return str;
	}
}
