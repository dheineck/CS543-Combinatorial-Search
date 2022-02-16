import java.util.Arrays;
import java.util.Stack;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Tents {

	static boolean PRE_SORT = true;
	static boolean BACK_SORT = false;
	static boolean SYM_BREAK = false;
	static boolean BEST_FIT = false;
	static boolean DFBnB = true;

	static final int GOAL = 180;
	static final int MAX_PREF = 12;
	int nodes_searched = 0;


	public static void main(String[] args) {
		
		Tents solver = new Tents();

		// intitialize the camper list & tent list		
		Stack<Camper> campers = readPrefs("tents-prefs.txt");
		Stack<Tent> tentList = new Stack<Tent>();
		tentList.push(new Tent(0, 2));
		tentList.push(new Tent(1, 3));
		tentList.push(new Tent(2, 3));
		tentList.push(new Tent(3, 4));
		tentList.push(new Tent(4, 4));


		// perform applicable presorts
		if (PRE_SORT) campers = sortCampers(campers);	
		if (BACK_SORT) {
			Stack<Camper> flipC = new Stack<Camper>();
			while (campers.size() > 0)
				flipC.push(campers.pop());
			campers = flipC;
			Stack<Tent> flipT = new Stack<Tent>();
			while (tentList.size() > 0)
				flipT.push(tentList.pop());
			tentList = flipT;
		}	

		// create the initial state & start the search
		State s = new State(campers, tentList);
		State result = solver.search(new State(s));

		// sort & write the results to file 
		for (int i = 0; i < result.assnCampers.size(); i++) {
			int min = 1000;	int min_pos = -1;
			for (int j = i; j < result.assnCampers.size(); j++) {
				if (result.assnCampers.get(j).num < min) {
					min = result.assnCampers.get(j).num;
					min_pos = j;
				}
			}
			Camper temp = result.assnCampers.get(min_pos);
			result.assnCampers.set(min_pos, result.assnCampers.get(i));
			result.assnCampers.set(i, temp);
		}		
		writeResults(result, "tent-solution.txt");
		for (Tent t : result.fullTents) System.out.println(t.num + " " + t.occupants);

	}
	

	public State search(State s) {
				
		// Base case
		if (s.availCampers.size() == 0) {
			return s;
		}		

		nodes_searched++;
		if (nodes_searched % 100000 == 0) System.out.print(".");

		// Get the camper to assign a tent to for this node
		Camper camper = s.availCampers.pop();
		s.assnCampers.push(camper);

		// First fit -- assign current camper to first tent (camper and tent sorted)
		// Best fit -- available tents sorted by score instead of heuristic		
		Stack<Tent> openTents = new Stack<Tent>();
		for (Tent t : s.openTents) openTents.push(t);

		if (BEST_FIT) {
			int[][] scores = new int[openTents.size()][2];			
			for (int i = 0; i < openTents.size(); i++) {
				int max = -1000; int max_pos = -1;
				for (int j = i; j < openTents.size(); j++) {
					
					if (i == 0) {
						Tent currTent = openTents.get(j);
						int score_init = currTent.score;
						currTent.setOccupant(camper);
						int score_diff = currTent.score - score_init;
						currTent.removeOccupant();
						scores[j][0] = openTents.get(j).num;
						scores[j][1] = score_diff;					
					} 

					if (scores[j][1] > max) {
						max = scores[j][1];
						max_pos = j;
					}
				}
				Tent temp = openTents.get(max_pos);
				openTents.set(max_pos, openTents.get(i));
				openTents.set(i, temp);				
			}	

		}

		// the actual search procedure -- cycle through the available tents
		State best = new State();		
		for (Tent t : openTents) {	

			if (SYM_BREAK) {
				if (t.occupants.size() > 0 && t.occupants.peek().num > camper.num) {
					continue;			
				}
			}			

			// add current camper to the current tent
			int prevTScore = t.score;
			int prevTent = camper.tentNum;
			camper.tentNum = t.num;
			t.setOccupant(camper);

			// Depth-first brand and bound
			if (DFBnB) {
				// 1. Create a big tent for all unassigned campers
				Tent bigTent = new Tent(-1, s.availCampers.size());
				for (Camper c : s.availCampers)
					bigTent.setOccupant(c);
				bigTent.setOccupant(camper);				

				// 2. Add up all the scores for h_score
				int fillScore = 0;
				for (Tent ot : s.openTents) 
					if (ot.numSpotsAvail()!=ot.capacity) fillScore+=ot.numSpotsAvail()*2*MAX_PREF;
				int h_score = s.score + (t.score-prevTScore) + bigTent.score + fillScore;	

				// 3. If the curr score + big score < 175, skip
				if (h_score < GOAL-20) {
					camper.tentNum = prevTent;
					t.removeOccupant();
					continue;
				}
			}

			// go down the list to the next camper
			State s1 = search(new State(s));
			
			// stop if you've reached the goal or return the best state so far
			if (s1.score >= GOAL) return s1;
			if (s1.score > best.score) { 
				best = s1;
			}
			
			// remove your self from the previous tent before being assigned to the next
			t.removeOccupant();
		}

		return best;
	}

	static void writeResults(State s, String filename) {
		
		System.out.println("Writing " + filename);
		try {
			File dataFile = new File(filename);
			FileWriter fileWriter = new FileWriter(dataFile);
			BufferedWriter writer = new BufferedWriter(fileWriter);
			
			for (Camper c : s.assnCampers) {
				writer.write(c.num + " " + c.tentNum + "\n");		
			}
			writer.write(s.score + "\n");			

			System.out.println("File written\n");
			writer.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

	}

	static Stack<Camper> readPrefs(String filename) {
		Stack<Camper> campers = new Stack<Camper>();
		int count = 0;
		Stack<String> names = new Stack<String>();
		Stack<String[]> data = new Stack<String[]>();

		try {
			File dataFile = new File(filename);
			FileReader fileReader = new FileReader(dataFile);
			BufferedReader reader = new BufferedReader(fileReader);

			String line = "";			
			while ((line = reader.readLine()) != null) {
				String[] splitLine = {"","",""};
				int splitPos = 0;			
				for (int i = 0; i < line.length(); i++) {
					if (line.charAt(i) == ' ' && line.charAt(i-1) != ' ') splitPos++;
					if (line.charAt(i) != ' ') splitLine[splitPos] += line.charAt(i);
				}
				if (names.size() == 0 || !splitLine[0].equals(names.peek()))
					names.push(splitLine[0]);
				data.push(splitLine);	
			}
			reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}


		int camperNum = 0;	
		String prevName = data.get(0)[0];
		int[] prefs = new int[names.size()];				
		for (int i = 0; i < data.size(); i++) {
			if (!data.get(i)[0].equals(prevName)) {
				campers.push(new Camper(camperNum, prefs));
				camperNum++;
				prevName = data.get(i)[0];
				prefs = new int[names.size()];
			}
						
			for(int j = 0; j < names.size(); j++) {
				if (data.get(i)[1].equals(names.get(j))) {
					prefs[j] = Integer.parseInt(data.get(i)[2]);
					break;
				}
			}
		}
		campers.push(new Camper(camperNum, prefs));
		return campers;
	}

	static Stack<Camper> sortCampers(Stack<Camper> unsortedCampers) {
		Stack<Camper> sortedCampers = new Stack<Camper>();

		for (int i = 0; i < unsortedCampers.size(); i++) {
			int max = -1;
			int maxPos = -1;
			for (int j = i; j < unsortedCampers.size(); j++) {
				if (unsortedCampers.get(j).prefSum > max) {
					max = unsortedCampers.get(j).prefSum;
					maxPos = j;
				}
			}
			Camper temp = unsortedCampers.get(maxPos);
			unsortedCampers.set(maxPos, unsortedCampers.get(i));
			unsortedCampers.set(i, temp);
			sortedCampers.push(temp);
		}

		return sortedCampers;
	}

}

class State {

	Stack<Camper> availCampers; // least agreeable at top of stack
	Stack<Camper> assnCampers;
	Stack<Tent> openTents;		// largest tent at top of stack
	Stack<Tent> fullTents;
	int score;
	
	public State() {
		score = 0;
	}

	public State(Stack<Camper> campers, Stack<Tent> tentList) {
		availCampers = campers;
		assnCampers = new Stack<Camper>();
		openTents = tentList;
		fullTents = new Stack<Tent>();
		for (Tent t : openTents) score += t.score;
		for (Tent t : fullTents) score += t.score;
	}

	public State(State s) {
		availCampers = new Stack<Camper>();		
		assnCampers = new Stack<Camper>();
		openTents = new Stack<Tent>();
		fullTents = new Stack<Tent>();

		for (Camper av : s.availCampers) availCampers.push(new Camper(av));
		for (Camper as : s.assnCampers) assnCampers.push(new Camper(as));
		for (Tent fl : s.fullTents) fullTents.push(new Tent(fl));		
		for (Tent op : s.openTents) {
			if (op.numSpotsAvail() == 0) fullTents.push(new Tent(op));
			else openTents.push(new Tent(op));
		}
		for (Tent t : openTents) score += t.score;
		for (Tent t : fullTents) score += t.score;
	}

}

class Camper {

	int num;
	int[] prefs;
	int prefSum;
	int tentNum;

	public Camper(int num, int[] prefs) {
		this.num = num;
		this.prefs = prefs;
		prefSum = 0;
		for (int n : prefs) if (n != -100) prefSum += n;
		tentNum = -1;
	}

	public Camper(Camper c) {
		num = c.num;
		prefs = new int[c.prefs.length];		
		for (int p = 0; p < c.prefs.length; p++) 
			prefs[p] = c.prefs[p];
		prefSum = c.prefSum;
		tentNum = c.tentNum;
	}

	public String prefsToString() {
		String str = "";
		str += num + ": " + Arrays.toString(prefs);
		return str;
	}

	public String toString() {
		return "" + num;
	}

	public void setTent(int tent) {
		this.tentNum = tent;
	}
	
}

class Tent {
	
	int num;	
	int capacity;
	Stack<Camper> occupants = new Stack<Camper>();
	int score;
	
	public Tent(int num, int capacity) {
		this.num = num;		
		this.capacity = capacity;
	}

	public Tent(Tent t) {
		num = t.num;
		capacity = t.capacity;
		for (Camper c : t.occupants) occupants.push(new Camper(c));
		score = t.score;
	}

	public void setOccupant(Camper c) {		
		int points = 0;
		for (Camper o : occupants) points += (o.prefs[c.num] + c.prefs[o.num]);
		score += points;
		occupants.push(c);
	}

	public void removeOccupant() {
		Camper c = occupants.pop();
		int points = 0;
		for (Camper o : occupants) points += (o.prefs[c.num] + c.prefs[o.num]);
		score -= points;
	}

	public int numSpotsAvail() {
		return capacity - occupants.size();
	}

	public String toString() {
		return "" + num;
	}	

}

