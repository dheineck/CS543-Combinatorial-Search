import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class JobShop {

	public static int[][] taskList;
	public static int num_jobs;
	public static int num_machines;
	public static int step;

	public static boolean MACH_ORDER = false;
	public static boolean JOB_ORDER = false;
	public static int DISCREPANCY = 0;			// Discrepancy budget applied for values > 0
	public static boolean BnB = false;			// branch-and-bound
	public static boolean DEDUP = false;			// state deduplication
	
	public static boolean DURATION = false;
	public static boolean VERBOSE = false;

	//public static HashSet<Integer> hashTable = new HashSet<Integer>(5000000);
	public static HashTable hashTable;	
	public static State best;
	public static long stopTime;
	public static boolean completed;

	// implement a different hash table ... fixed array, and just mod the hash code?  Keep the State, too?
	// 2 JOB_ORDER -- behind & size
	// 3 DISCREPANCY BUDGET --
	// 4 Parser -- & C file
	// 5 Benchmarks?
	// 6 Write up
	public static Instance run(Instance i, String[] args) {

		// initialize the instance here
		num_jobs = i.num_jobs;
		num_machines = i.num_machines;
		taskList = i.jobs;
		System.out.println("Searching: " + i.title);

		// set the search settings
		int duration = 1000*60;	// 1 minute default duration 
		for (String arg : args) {
			if (arg.equals("-v"))
				{ VERBOSE = true; System.out.println("VERBOSE"); }
			if (arg.equals("-b")) 
				{ BnB = true; if (VERBOSE) System.out.print("BnB "); }
			if (arg.equals("-d")) 
				{ DEDUP = true; if (VERBOSE) System.out.print("DEDUP "); }
			if (arg.contains("-d:")) 
				{ DISCREPANCY = Integer.parseInt(arg.substring(3)); 
				  if (VERBOSE) System.out.print("DISCREPANCY "); }
			if (arg.equals("-j"))
				{JOB_ORDER = true; if (VERBOSE) System.out.print("JOB_ORDER "); }
			if (arg.contains("-t:"))
				{ duration = Integer.parseInt(arg.substring(3))*1000;
				  DURATION = true;
				  if (VERBOSE) System.out.print("DURATION "); }
		}
		if (VERBOSE && args.length > 0) System.out.println();

		
		// reset hash table & number of steps
		hashTable = new HashTable(250000, num_machines);
		step = -1;
		best = null;
		completed = false;

		// commence search
		long start = System.currentTimeMillis();
		stopTime = start + duration;
		State s = search(new State());
		long finish = System.currentTimeMillis();

		// Display the resulting schedule
		if (VERBOSE) {		
			System.out.println("FINAL ANSWER");
			for (int row = 0; row < num_jobs; row++) {
				System.out.println(Arrays.toString(best.sched[row]));
			}
			System.out.println("Best timespan: " + best.maxTime);
			System.out.println("Run time: " + (finish - start)/1000);		
			System.out.println("Steps: " + step);
			System.out.println("Hash table size: " + hashTable.size());
		}
		
		i.setSolution(best.sched, best.maxTime, (int)((finish - start)/1000), step, hashTable.size());
		return i;
	}

	public static State search(State s) {

		step++;
		if (step % 100000 == 0) {
			System.out.print(".");
			if (DURATION && step % 500000 == 0 && 
				System.currentTimeMillis() >= stopTime) completed = true;
		}
		if (completed) return s;

		
		// base case
		int sum = 0;
		for (int n : s.jobTaskPtr) sum+=n;
		if (sum == num_jobs*num_machines) {
			if (best == null  || s.maxTime < best.maxTime) {
				best = s;
				System.out.print(best.maxTime);
			}
			return s;
		}

		// create a list of availableMachines at this time step
		ArrayList<Integer> availableMachines = new ArrayList<Integer>();
		State nextState = new State(Integer.MAX_VALUE);	
		boolean bind = true;
		for (int m = 0; m < num_machines; m++) {
			if (s.machTimeAvailable[m] <= s.timeStep) {
				availableMachines.add(m);
			}			
		}


		// explore the path for each of the available machines
		for (int m = 0; m < availableMachines.size(); m++) {
			
			int mach = availableMachines.get(m);

			// create a list of jobs for the given machine
			ArrayList<Integer> availableJobs = new ArrayList<Integer>();
			for (int j = 0; j < num_jobs; j++) {
				if (s.jobTimeAvailable[j] <= s.timeStep)
					if (s.jobTaskPtr[j] < num_machines && taskList[j][s.jobTaskPtr[j]*2] == mach) 
						availableJobs.add(j);
			}


			// sort the available jobs according to??? jobTaskPtr && time
			if (JOB_ORDER) {		

				for (int i = 0; i < availableJobs.size() && availableJobs.size() > 1; i++) {
					int priorityJob = availableJobs.get(i);
					int index = i;				
					for (int j = i; j < availableJobs.size(); j++) {
						if (s.jobTaskPtr[availableJobs.get(j)] < s.jobTaskPtr[priorityJob]) {
							priorityJob = availableJobs.get(j);
							index = j;
						}
					}
					if (index != i) {
						int temp = availableJobs.get(i);
						availableJobs.set(i, priorityJob);
						availableJobs.set(index, temp);
					}
				}

			}

			// create a branch for each job for the given machine
			for(int j = 0; j < availableJobs.size() && s.discrepancy <= DISCREPANCY; j++) {
				
				int job = availableJobs.get(j);
				if (DISCREPANCY > 0 && j > 0) s.discrepancy++;
				nextState = new State(s);

				// assign the job, machine pair in this branch state
				nextState.sched[job][nextState.jobTaskPtr[job]*2] = mach;
				nextState.sched[job][nextState.jobTaskPtr[job]*2+1] = nextState.timeStep;
				
				nextState.machTimeAvailable[mach] = nextState.timeStep + taskList[job][nextState.jobTaskPtr[job]*2+1];
				nextState.jobTimeAvailable[job] = nextState.timeStep + taskList[job][nextState.jobTaskPtr[job]*2+1];
				if (nextState.jobTimeAvailable[job] > nextState.maxTime) 
					nextState.maxTime = nextState.jobTimeAvailable[job];				
				nextState.jobTaskPtr[job]++;
				
				// Branch & Bound
				if (BnB) {		
					//remove the doo-dad from jobTaskList
					nextState.jobTaskList.get(job).remove(nextState.jobTaskList.get(job).size()-1);
					nextState.jobTaskList.get(job).remove(nextState.jobTaskList.get(job).size()-1);				
	
					int maxTime = -1;
					for (int jb = 0; jb < num_jobs; jb++) {
						int tot = 0;
						for (int t = 1; t < nextState.jobTaskList.get(jb).size(); t+=2)
							tot += nextState.jobTaskList.get(jb).get(t);
						tot += nextState.jobTimeAvailable[jb];
						if (tot > maxTime) maxTime = tot;
					}			

					if (best != null && maxTime >= best.maxTime)
						continue;
					else
						bind = false;
				}

				// continue down the path for this time step
				boolean unvisited = true;
				if (DEDUP) 
					unvisited = hashTable.add(nextState);	

				if (unvisited) {	
					nextState = search(new State(nextState));	
				}								
			}

		}

		if (nextState.maxTime < Integer.MAX_VALUE) {
			//if (BnB && bind) return s;			
			s = nextState;
		}
		
		// advance to the next timeStep where a machine becomes available
		int nextTimeStep = Integer.MAX_VALUE;
		for (int t : s.jobTimeAvailable) 
			if (t > s.timeStep && t < nextTimeStep)
				nextTimeStep = t;
		
		// if you've already reached a solution, return it		
		if (nextTimeStep == Integer.MAX_VALUE) return s;

		s = search(new State(nextTimeStep, s.maxTime, s.sched, s.jobTaskPtr, 
		           s.machTimeAvailable, s.jobTimeAvailable, s.discrepancy, s.jobTaskList));

		return s;
	}

}

class State {
	public int[][] sched;				// the schedule, so far at this state
	public int[] jobTaskPtr;			// keeps track of which task is active for a given job
	public int[] machTimeAvailable;		// when each machine is next available
	public int[] jobTimeAvailable;		// when each job is next available
	public int discrepancy;				// the current job for each machine ** not being currently used
	public ArrayList<ArrayList<Integer>> jobTaskList;	// a copy of the original input as an ArrayList
	int timeStep;	// the current time at this state
	int maxTime;	// the max timespan so far
	State best;		// the best state seen so far

	// default State constructor
	public State() {
		sched = new int[JobShop.num_jobs][JobShop.num_machines*2];
		jobTaskPtr = new int[JobShop.num_jobs];
		machTimeAvailable = new int[JobShop.num_machines];
		jobTimeAvailable = new int[JobShop.num_jobs];
		discrepancy = 0;
		jobTaskList = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < JobShop.num_jobs; i++) {
			jobTaskList.add(new ArrayList<Integer>());
			for (int j : JobShop.taskList[i])
				jobTaskList.get(i).add(j);
		}
		timeStep = 0;
		maxTime = 1;
	}

	// copy State constructor
	public State(State s) {
		setState(s.timeStep, s.maxTime, s.sched, s.jobTaskPtr, s.machTimeAvailable, 
				 s.jobTimeAvailable, s.discrepancy, s.jobTaskList);

	}

	// full set State constructor
	public State(int timeStep, int maxTime, int[][] sched, 
				int[] jobTaskPtr, int[] machTimeAvailable, int[] jobTimeAvailable, int discrepancy, 
				ArrayList<ArrayList<Integer>> jobTaskList) {
		
		setState(timeStep, maxTime, sched, jobTaskPtr, machTimeAvailable, jobTimeAvailable, discrepancy, jobTaskList);

	}

	// special State constructor for initial maxTime
	public State(int time) {
		maxTime = time;
	}

	// set method that sets / copies all instance variables
	public void setState(int timeStep, int maxTime, int[][] sched, 
				int[] jobTaskPtr, int[] machTimeAvailable, int[] jobTimeAvailable, int discrepancy, 
				ArrayList<ArrayList<Integer>> jobTaskList) {
		this.timeStep = timeStep;
		this.maxTime = maxTime;

		this.sched = new int[JobShop.num_jobs][JobShop.num_machines*2];
		for (int i = 0; i < sched.length; i++)
			for (int j = 0; j < sched[0].length; j++)
				this.sched[i][j] = sched[i][j];
		
		this.jobTaskPtr = new int[JobShop.num_jobs];
		this.jobTimeAvailable = new int[JobShop.num_jobs];
		for (int i = 0; i < JobShop.num_jobs; i++) {
			this.jobTaskPtr[i] = jobTaskPtr[i];
			this.jobTimeAvailable[i] = jobTimeAvailable[i];
		}

		this.machTimeAvailable = new int[JobShop.num_machines];
		this.discrepancy = discrepancy;
		for (int i = 0; i < JobShop.num_machines; i++) {
			this.machTimeAvailable[i] = machTimeAvailable[i];
		}

		this.jobTaskList = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < jobTaskList.size(); i++) {
			this.jobTaskList.add(new ArrayList<Integer>());			
			for (int job : jobTaskList.get(i))	
				this.jobTaskList.get(i).add(job);
		}
	}

	public int hashCode() {
		return Arrays.deepHashCode(sched) + Arrays.hashCode(machTimeAvailable);
	}

	public boolean equals(Object other) {
		return (this.hashCode() == other.hashCode());
	}

	public void show() {
		for (int row = 0; row < JobShop.num_jobs; row++)
			System.out.println("\t"+Arrays.toString(sched[row]));
		System.out.println("\n\t" + Arrays.toString(machTimeAvailable));
	}

}

class HashTable {

	int[][] hashTable;
	int size;

	public HashTable(int size, int width) {
		hashTable = new int[size][width];
	}

	public boolean add(State s) {
		int hashCode = s.hashCode();
		int key = Math.abs(hashCode % hashTable.length);
		boolean okToAdd = !Arrays.equals(hashTable[key], s.machTimeAvailable);

		if (okToAdd) {
			hashTable[key] = s.machTimeAvailable;
			size++;
		}
		
		return okToAdd;
	}	

	public int size() {
		return size;
	}

}
