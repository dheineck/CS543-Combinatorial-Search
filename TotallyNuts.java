import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.Scanner;

public class TotallyNuts {

	static final int NUM_NUTS = 7;
	static final int NUM_SIDES = 6;
	static int[][] puzzleStart = readPuzzle("puzzle3.txt");

	/*public TotallyNuts(String fileName) {
		puzzleStart = readPuzzle(fileName);
	}*/

	public static void main(String[] args) {
		
		TotallyNuts puzzle = new TotallyNuts();

		//displayPuzzle(puzzle);

		puzzle.search(0, new PuzzleState());  // start the depth-first search

	}

	public int search(int position, PuzzleState s) {
		s.clean(position);		
		Position[] state = s.getPuzzleState();
		boolean[] available = s.getAvailable();

		int fit = 0;							// the nut fits in the position or not
		if (position == NUM_NUTS) {
			System.out.println("*** VICTORY ***");
			int[][] solution = new int[NUM_NUTS][NUM_SIDES];			
			for (int i = 0; i < NUM_NUTS; i++) {
				System.out.println(Arrays.toString(state[i].getCurrVals()) + "\n");
				solution[i] = state[i].getCurrVals();
			}
			writePuzzle(solution, "TotallyNutsSolution.txt");
			return 1;
		}		// if all nuts have been successfully placed
		while(fit != 1 && state[position].placeNut(state, available, position)) {// place new nut if possible
			System.out.println("position " + position + " : placing " + state[position].getNutNum() +" "+state[position]);		
			//Scanner kb = new Scanner(System.in);			
			//String str = kb.nextLine();
			// no need to rotate for position 0			
			if (position == 0) fit = search(position+1, new PuzzleState(state, available));
			while (fit == 0 && position != 0) {	// if there's not yet a fit
				System.out.println("Rotating... from position " + position);				
				fit = state[position].rotateNut(state, position);// rotate to find the next fit
				// if there's a fit, search next level sending the updated state
				if (fit == 1) {
					System.out.println("...Finding a nut to place in position " + (position+1) + "...");
					fit = search(position+1, new PuzzleState(state, available));	
				}
				if (fit == -1) {fit = 0; break;}			// if rotated fully & no fit, place new nut 
			}
		}
		if (fit == 1) return 1;		// final return statement if search(0) is a success
		return 0;					// no fit for this branch
	}

	public static void writePuzzle(int[][] solution, String fileName) {
		System.out.println("Writing " + fileName);
		try {
			File dataFile = new File(fileName);
			FileWriter fileWriter = new FileWriter(dataFile);
			
			BufferedWriter writer = new BufferedWriter(fileWriter);
			
			for (int i = 0; i < solution.length; i++) {
 				String str = "";
				for (int j = 0; j < solution[i].length; j++)
					str += solution[i][j] + " ";	
					writer.write(str+"\n");
			}			
			System.out.println("File written\n");
			writer.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public static int[][] readPuzzle(String filename) {
		
		int[][] puzzle = new int[NUM_NUTS][NUM_SIDES];

		try {
			File dataFile = new File(filename);
			FileReader fileReader = new FileReader(dataFile);
			BufferedReader reader = new BufferedReader(fileReader);
			
			int count = 0;
			String line = "";
					
			while ((line = reader.readLine()) != null) {
				
				/* step through the line  */
				String[] splitLine = line.split(" ");
				for(int i = 0; i < splitLine.length; i++) {
					puzzle[count][i] = Integer.parseInt(splitLine[i]);
				}
				count++;
			}
			reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return puzzle;
	}

	public void displayPuzzle(int[][] puzzle) {
		for(int i = 0; i < puzzle.length; i++) {
			for (int j = 0; j < puzzle[i].length; j++) {
				System.out.print(puzzle[i][j] + " ");
			}
			System.out.println();
		}
	}
}

class PuzzleState {
	Position[] puzzleState;
	boolean[] available;
	int NUM_NUTS;
	int NUM_SIDES;

	public PuzzleState() {
		NUM_NUTS = TotallyNuts.NUM_NUTS;
		NUM_SIDES = TotallyNuts.NUM_SIDES;
		puzzleState = new Position[NUM_NUTS];
		available = new boolean[NUM_NUTS];			
		for (int i = 0; i < NUM_NUTS; i++) {
			puzzleState[i] = new Position();		
			available[i] = true;
		}
	}

	public PuzzleState(Position[] puzzleState, boolean[] available) {
		this.puzzleState = puzzleState;
		this.available = available;
	}

	public Position[] getPuzzleState() {
		return puzzleState;
	}

	public boolean[] getAvailable() {
		return available;
	}

	public void clean(int position) {
		// clean this position? and downstream
		for (int i = position; i < NUM_NUTS; i++) {
			puzzleState[i].makeAvail(available);			
			puzzleState[i] = new Position();
			//need to be careful about freeing the available...
			// go to each position from here
			// call a 'free' or 'clean' method that cleans history, occupied and makes available...or just makes available (if occ) 
		}
	}	

	// placeNut()
	// rotateNut() belong here? -- don't need to pass as much junk around?
	// I think it makes more sense...at least at the moment...i dunno...maybe it's just easier 
	// the other way (the current way)
}

class Position {
	Stack<Integer> nutHist;
	Stack<Integer> rotHist;
	boolean isCurrOcc;
	int[] currVals;
	int NUM_NUTS;
	int NUM_SIDES;
	int[][] puzzleStart;

	public Position() {
		nutHist = new Stack<Integer>();
		rotHist = new Stack<Integer>();
		isCurrOcc = false;
		NUM_NUTS = TotallyNuts.NUM_NUTS;
		NUM_SIDES = TotallyNuts.NUM_SIDES;
		puzzleStart = TotallyNuts.puzzleStart;
	}

	public String toString() {
		return Arrays.toString(currVals);
	}

	public boolean placeNut(Position[] state, boolean[] available, int position) {
				


		// if there's a nut already here, return that nut to the available list -- clear this positions history?
		if (isCurrOcc) {
			available[nutHist.peek()] = true;	
			isCurrOcc = false;
			currVals = new int[NUM_SIDES];
			rotHist = new Stack<Integer>();
			for (int i = position+1; i < NUM_NUTS; i++) {
				state[i].makeAvail(available);
				state[i] = new Position();
			}
		}
		// create a list of available nuts from the available list		
		ArrayList<Integer> nutToUse = new ArrayList<Integer>(NUM_NUTS);
		for (int n = 0; n < available.length; n++)
			if (available[n]) nutToUse.add(n);

		// remove the nuts that have been used from the currently available list
		for (int h = 0; h < nutHist.size(); h++)
			for (int a = 0; a < nutToUse.size(); a++)
				if (nutToUse.get(a) == nutHist.get(h)) {
					nutToUse.remove(a);
					a--;
				}
		
		// if all available nuts have been used, return false
		if (nutToUse.size() == 0) {
		//System.out.println("  No nut to place : \n\tavailable: " + Arrays.toString(available) + "\n\thist: " + nutHist);
		System.out.println("  ..no go : avail "+Arrays.toString(available)+" hist " +nutHist);		
		return false;
		}
		// otherwise add the first unused, available nut to the history & make it unavailable
		nutHist.push(nutToUse.get(0));
		available[nutToUse.get(0)] = false;
		isCurrOcc = true;
		currVals = puzzleStart[nutToUse.get(0)];
		return true;
	}

	public int rotateNut(Position[] state, int position) {
		int pos = position;		
		// rotate isn't called for position zero		
		// get the values for the current nut's default orientation		
		if (!isCurrOcc) return -1;

		if (position == 1) {
			// 1,3 match 0,0
			for (int rot = 0; rot < NUM_SIDES; rot++) {	// int rot == rotHist.peek()
				// get the current nut at position zero
				// and get the value, considering roations, that's currently at the zero position
				if (currVals[(3+rot)%NUM_SIDES] == state[0].getCurrFaceVal(0) && rotHist.search(rot) == -1) {
					rotHist.push(rot);
					System.out.println("rotating position "+position+" nut "+nutHist.peek()
									+" by "+rot+" : "+Arrays.toString(getCurrVals()));					
					return 1;
				}
			}
		}

		if (position >= 2 && position <= 6) {
			// 2,5 match 1,2
			// 2,4 match 0,1
			// 3,0 match 2,3
			// 3,5 match 0,2
			// 4,1 match 3,4
			// 4,0 match 0,3
			// 5,2 match 4,5
			// 5,1 match 0,4
			// 6,3 match 5,0
			// 6,2 match 0,5
			// 6,1 match 1,4
			for (int rot = 0; rot < NUM_SIDES; rot++) {	
				// get the current nut at position zero
				// and get the value, considering roations, that's currently at the zero position
				if (currVals[((pos+3)%NUM_SIDES+rot)%NUM_SIDES] == state[pos-1].getCurrFaceVal(pos%NUM_SIDES) 
					&& currVals[((pos+2)%NUM_SIDES+rot)%NUM_SIDES] == state[0].getCurrFaceVal(pos-1)					
					&& rotHist.search(rot) == -1) {
					if (pos == 6 && currVals[(6+rot)%NUM_SIDES] != state[1].getCurrFaceVal(4)) return -1;					
					rotHist.push(rot);
					System.out.println("rotating position "+position+" nut "+nutHist.peek()
										+" by "+rot+" : "+Arrays.toString(getCurrVals()));
					System.out.print("\t" + currVals[((pos+3)%NUM_SIDES+rot)%NUM_SIDES] + " == " 
									+ state[pos-1].getCurrFaceVal(pos%NUM_SIDES) 
									+ Arrays.toString(state[pos-1].getCurrVals()) + " and ");	
					System.out.println(currVals[((pos+2)%NUM_SIDES+rot)%NUM_SIDES] + " == " + state[0].getCurrFaceVal(pos-1)
									+ Arrays.toString(state[0].getCurrVals()));	
					//if (position ==5 ) System.exit(101);					
					return 1;
				}
			}		
		}
		// if rotate fully and no fit...return -1
		System.out.println("  pos " + position + " nut " + nutHist.peek() + " rotated fully with no match");
		return -1;
	}

	public int[] getCurrVals() {
		int rotAmt = (rotHist.size() == 0) ? 0 : rotHist.peek(); 		
		int[] rotated = new int[currVals.length];
		for (int i = 0; i < rotated.length; i++) {
			rotated[i] = currVals[(i+rotAmt)%NUM_SIDES];
		}
		return rotated;
	}

	public int getCurrFaceVal(int faceNum) {
		int[] values = getCurrVals();
		return values[faceNum];
	}

	public int getNutNum() {
		return nutHist.peek();
	}

	public void makeAvail(boolean[] available) {
		//if (isCurrOcc) {
			if (nutHist.size() > 0) available[nutHist.peek()] = true;
		//}
	}
}
