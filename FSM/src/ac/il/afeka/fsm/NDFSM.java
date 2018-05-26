package ac.il.afeka.fsm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

public class NDFSM {

	protected TransitionMapping transitions;
	protected Set<State> states;
	protected Set<State> acceptingStates;
	protected State initialState;
	protected Alphabet alphabet;

	/**
	 * Builds a NDFSM from a string representation (encoding)
	 * 
	 * @param encoding
	 *            the string representation of a NDFSM
	 * @throws Exception
	 *             if the encoding is incorrect or if the transitions contain
	 *             invalid states or symbols
	 */
	public NDFSM(String encoding) throws Exception {
		parse(encoding);

		transitions.verify(states, alphabet);
	}

	/**
	 * Build a NDFSM from its components
	 * 
	 * @param states
	 *            the set of states for this machine
	 * @param alphabet
	 *            this machine's alphabet
	 * @param transitions
	 *            the transition mapping of this machine
	 * @param initialState
	 *            the initial state (must be a member of states)
	 * @param acceptingStates
	 *            the set of accepting states (must be a subset of states)
	 * @throws Exception
	 *             if the components do not represent a valid non deterministic
	 *             machine
	 */
	public NDFSM(Set<State> states, Alphabet alphabet, Set<Transition> transitions, State initialState,
			Set<State> acceptingStates) throws Exception {

		initializeFrom(states, alphabet, transitions, initialState, acceptingStates);
		this.transitions.verify(this.states, alphabet);
	}

	protected void initializeFrom(Set<State> states, Alphabet alphabet, Set<Transition> transitions, State initialState,
			Set<State> acceptingStates) {

		this.states = states;
		this.alphabet = alphabet;
		this.transitions = createMapping(transitions);
		this.initialState = initialState;
		this.acceptingStates = acceptingStates;
	}

	protected NDFSM() {
	}

	/**
	 * Overrides this machine with the machine encoded in string.
	 * 
	 * <p>
	 * Here's an example of the encoding:
	 * </p>
	 * 
	 * <pre>
	0 1/a b/0 , a , 0; 0,b, 1 ;1, a, 0 ; 1, b, 1/0/ 1
	 * </pre>
	 * <p>
	 * This is the encoding of a finite state machine with two states
	 * (identified as 0 and 1), an alphabet that consists of the two characters
	 * 'a' and 'b', and four transitions:
	 * </p>
	 * <ol>
	 * <li>From state 0 on character a it moves to state 0</li>
	 * <li>from state 0 on character b it moves to state 1,</li>
	 * <li>from state 1 on character a it moves to state 0,</li>
	 * <li>from state 1 on character b it moves to state 1.</li>
	 * </ol>
	 * <p>
	 * The initial state of this machine is 0, and the set of accepting states
	 * consists of just one state 1. Here is the format in general:
	 * </p>
	 * 
	 * <pre>
	 {@code
	<states> / <alphabet> / <transitions> / <initial state> / <accepting states>
	}
	 * </pre>
	 * 
	 * where:
	 * 
	 * <pre>
	{@code
	<alphabet> is <char> <char> ...
	
	<transitions> is <transition> ; <transition> ...
	
	<transition> is from , char, to
	
	<initial state> is an integer
	
	<accepting states> is <state> <state> ...
	
	<state> is an integer
	}
	 * </pre>
	 * 
	 * @param string
	 *            the string encoding
	 * @throws Exception
	 *             if the string encoding is invalid
	 */
	public void parse(String string) throws Exception {

		Scanner scanner = new Scanner(string);

		scanner.useDelimiter("\\s*/");

		Map<Integer, State> states = new HashMap<Integer, State>();

		for (Integer stateId : IdentifiedState.parseStateIdList(scanner.next())) {
			states.put(stateId, new IdentifiedState(stateId));
		}

		Alphabet alphabet = Alphabet.parse(scanner.next());

		Set<Transition> transitions = new HashSet<Transition>();

		for (TransitionTuple t : TransitionTuple.parseTupleList(scanner.next())) {
			transitions.add(new Transition(states.get(t.fromStateId()), t.symbol(), states.get(t.toStateId())));
		}

		State initialState = states.get(scanner.nextInt());

		Set<State> acceptingStates = new HashSet<State>();

		if (scanner.hasNext())
			for (Integer stateId : IdentifiedState.parseStateIdList(scanner.next())) {
				acceptingStates.add(states.get(stateId));
			}

		scanner.close();

		initializeFrom(new HashSet<State>(states.values()), alphabet, transitions, initialState, acceptingStates);
		this.transitions.verify(this.states, alphabet);
	}

	protected TransitionMapping createMapping(Set<Transition> transitions) {
		return new TransitionRelation(transitions);
	}

	/**
	 * Returns a version of this state machine with all the unreachable states
	 * removed.
	 * 
	 * @return NDFSM that recognizes the same language as this machine, but has
	 *         no unreachable states.
	 */
	public NDFSM removeUnreachableStates() {

		Set<State> reachableStates = reachableStates();

		Set<Transition> transitionsToReachableStates = new HashSet<Transition>();

		for (Transition t : transitions.transitions()) {
			if (reachableStates.contains(t.fromState()) && reachableStates.contains(t.toState()))
				transitionsToReachableStates.add(t);
		}

		Set<State> reachableAcceptingStates = new HashSet<State>();
		for (State s : acceptingStates) {
			if (reachableStates.contains(s))
				reachableAcceptingStates.add(s);
		}

		NDFSM aNDFSM = (NDFSM) create();

		aNDFSM.initializeFrom(reachableStates, alphabet, transitionsToReachableStates, initialState,
				reachableAcceptingStates);

		return aNDFSM;
	}

	protected NDFSM create() {
		return new NDFSM();
	}

	// returns a set of all states that are reachable from the initial state

	private Set<State> reachableStates() {

		List<Character> symbols = new ArrayList<Character>();

		symbols.add(Alphabet.EPSILON);

		for (Character c : alphabet) {
			symbols.add(c);
		}

		Alphabet alphabetWithEpsilon = new Alphabet(symbols);

		Set<State> reachable = new HashSet<State>();

		Set<State> newlyReachable = new HashSet<State>();

		newlyReachable.add(initialState);

		while (!newlyReachable.isEmpty()) {
			reachable.addAll(newlyReachable);
			newlyReachable = new HashSet<State>();
			for (State state : reachable) {
				for (Character symbol : alphabetWithEpsilon) {
					for (State s : transitions.at(state, symbol)) {
						if (!reachable.contains(s))
							newlyReachable.add(s);
					}
				}
			}
		}

		return reachable;
	}

	/**
	 * Encodes this state machine as a string
	 * 
	 * @return the string encoding of this state machine
	 */
	public String encode() {
		return State.encodeStateSet(states) + "/" + alphabet.encode() + "/" + transitions.encode() + "/"
				+ initialState.encode() + "/" + State.encodeStateSet(acceptingStates);
	}

	/**
	 * Prints a set notation description of this machine.
	 * 
	 * <p>
	 * To see the Greek symbols on the console in Eclipse, go to Window -&gt;
	 * Preferences -&gt; General -&gt; Workspace and change
	 * <tt>Text file encoding</tt> to <tt>UTF-8</tt>.
	 * </p>
	 * 
	 * @param out
	 *            the output stream on which the description is printed.
	 */
	public void prettyPrint(PrintStream out) {
		out.print("K = ");
		State.prettyPrintStateSet(states, out);
		out.println("");

		out.print("\u03A3 = ");
		alphabet.prettyPrint(out);
		out.println("");

		out.print(transitions.prettyName() + " = ");
		transitions.prettyPrint(out);
		out.println("");

		out.print("s = ");
		initialState.prettyPrint(out);
		out.println("");

		out.print("A = ");
		State.prettyPrintStateSet(acceptingStates, out);
		out.println("");
	}

	/**
	 * Returns a canonic version of this machine.
	 * 
	 * <p>
	 * The canonic encoding of two minimal state machines that recognize the
	 * same language is identical.
	 * </p>
	 * 
	 * @return a canonic version of this machine.
	 */

	public NDFSM toCanonicForm() {

		Set<Character> alphabetAndEpsilon = new HashSet<Character>();

		for (Character symbol : alphabet) {
			alphabetAndEpsilon.add(symbol);
		}
		alphabetAndEpsilon.add(Alphabet.EPSILON);

		Set<Transition> canonicTransitions = new HashSet<Transition>();
		Stack<State> todo = new Stack<State>();
		Map<State, State> canonicStates = new HashMap<State, State>();
		Integer free = 0;

		todo.push(initialState);
		canonicStates.put(initialState, new IdentifiedState(free));
		free++;

		while (!todo.isEmpty()) {
			State top = todo.pop();
			for (Character symbol : alphabetAndEpsilon) {
				for (State nextState : transitions.at(top, symbol)) {
					if (!canonicStates.containsKey(nextState)) {
						canonicStates.put(nextState, new IdentifiedState(free));
						todo.push(nextState);
						free++;
					}
					canonicTransitions
							.add(new Transition(canonicStates.get(top), symbol, canonicStates.get(nextState)));
				}
			}
		}

		Set<State> canonicAcceptingStates = new HashSet<State>();
		for (State s : acceptingStates) {
			if (canonicStates.containsKey(s)) // unreachable accepting states
												// will not appear in the
												// canonic form of the state
												// machine
				canonicAcceptingStates.add(canonicStates.get(s));
		}

		NDFSM aNDFSM = create();

		aNDFSM.initializeFrom(new HashSet<State>(canonicStates.values()), alphabet, canonicTransitions,
				canonicStates.get(initialState), canonicAcceptingStates);

		return aNDFSM;
	}

	public boolean compute(String input) {
		return toDFSM().compute(input);
	}

	
	// Code starts here:
	public DFSM toDFSM() {
		// Declaring variables and initializing them with appropriate methods.
		char epsilonValue = 'e';
		Map<State, Set<State>> epsilonClosureGraph = getEpsilonClosureGraph(epsilonValue);
		IdentifiedState terminate = new IdentifiedState(-1);
		Set<Transition> delta = new HashSet<>();
		Set<State> states = new HashSet<>();
		Set<State> acceptingStates = new HashSet<>();
		List<Character> symbols = new ArrayList<>();
		State initialState = new IdentifiedState(this.initialState.hashCode());
		ArrayList<Map<State, Set<State>>> arr = initHelperArray(epsilonClosureGraph);
		ArrayList<Map<Set<State>, Set<State>>> arr2 = initMapsWithinDfsmArray();
		
		
		determineTerminate(arr, terminate);
		initializeSymbolList(symbols);
		intializeDfsmStates(states, arr);
		initNodesDfsmArray(arr, arr2, symbols, terminate);
		constructFullDfsmArray(arr, arr2, terminate);
		determineTerminateDfsm(arr2, terminate);
		initializeDelta(arr2, states, delta);
		initializeAcceptingStates(arr2, acceptingStates, initialState, epsilonClosureGraph);
		DFSM dfsm = getDfsm(states, symbols, delta, initialState, acceptingStates);
		return dfsm;
	}

	// Construct epsilon graph.
	private Map<State, Set<State>> getEpsilonGraph(char epsilonValue) {
		Map<State, Set<State>> epsGraph = new HashMap<>();
		for (State state : this.states) {
			Set<State> statesReachableWithEps = this.transitions.at(state, epsilonValue);
			if (statesReachableWithEps.size() != 0) {
				epsGraph.put(state, statesReachableWithEps);
			} else {
				Set<State> set = new HashSet<>();
				set.add(state);
				epsGraph.put(state, set);
			}
		}
		return epsGraph;
	}

	// Construct epsilon closure graph.
	private Map<State, Set<State>> getEpsilonClosureGraph(char epsilonValue) {
		Map<State, Set<State>> epsilonGraph = getEpsilonGraph(epsilonValue);
		for (State state : epsilonGraph.keySet()) {
			for (State child : epsilonGraph.get(state)) {
				for (State relative : this.transitions.at(child, epsilonValue)) {
					epsilonGraph.get(state).add(relative);
				}
			}
			epsilonGraph.get(state).add(state);
		}
		return epsilonGraph;
	}

	// Initializing an arraylist to hold maps for each symbol in the alphabet -
	// to be used later as a helper arraylist.
	private ArrayList<Map<State, Set<State>>> initMapsWithinHelperArray() {
		ArrayList<Map<State, Set<State>>> arr = new ArrayList<>();
		for (Character symbol : this.alphabet.getSymbols()) {
			if (symbol != 'e')
				arr.add(new HashMap<>());
		}
		return arr;
	}

	// Per the stages of the algorithm - this method uses the epsilon closure
	// graph to build a new graph which holds all edges
	// between states.
	private ArrayList<Map<State, Set<State>>> initHelperArray(Map<State, Set<State>> epsilonClosureGraph) {
		ArrayList<Map<State, Set<State>>> arr = initMapsWithinHelperArray();
		for (int i = 0; i < arr.size(); i++) {
			char symbol = this.alphabet.getSymbols().get(i);
			if (symbol == 'e')
				break;
			for (State state : epsilonClosureGraph.keySet()) {
				arr.get(i).put(state, new HashSet<>());
				for (State child : epsilonClosureGraph.get(state)) {
					Set<State> statesReachableWithSymbol = new HashSet<>();
					statesReachableWithSymbol = this.transitions.at(child, symbol);
					for (State state2 : statesReachableWithSymbol) {
						for (State child2 : epsilonClosureGraph.get(state2)) {
							arr.get(i).get(state).add(child2);
						}
					}
				}
			}
		}
		return arr;
	}

	// A method to add a 'terminate' state to each appropriate state.
	private void determineTerminate(ArrayList<Map<State, Set<State>>> arr, State terminate) {
		for (int i = 0; i < arr.size(); i++) {
			char symbol = this.alphabet.getSymbols().get(i);
			if (symbol == 'e')
				break;
			for (State state : arr.get(i).keySet()) {
				if (arr.get(i).get(state).isEmpty())
					arr.get(i).get(state).add(terminate);
			}
		}

	}

	private void initializeSymbolList(List<Character> symbols) {
		for (Character symbol : this.alphabet.getSymbols()) {
			if (symbol != 'e')
				symbols.add(symbol);
		}
	}

	// This method creates new states for the dfsm. Because a new singular state
	// in the dfsm is sometimes a bunch of states from
	// the ndfsm, i chose to use a unique hashcode in order to represent the
	// state in the dfsm.
	private void intializeDfsmStates(Set<State> states, ArrayList<Map<State, Set<State>>> arr) {
		// Initialize states.
		states.add(new IdentifiedState(this.initialState.hashCode()));
		for (int i = 0; i < arr.size(); i++) {
			for (State state : arr.get(i).keySet()) {
				Set<State> tempStates = arr.get(i).get(state);
				states.add(new IdentifiedState(tempStates.hashCode()));
			}
		}
	}

	// Initializing an arraylist to hold maps for each symbol in the alphabet -
	// this will be used to represent the final dfsm.
	private ArrayList<Map<Set<State>, Set<State>>> initMapsWithinDfsmArray() {
		ArrayList<Map<Set<State>, Set<State>>> arr2 = new ArrayList<>();
		for (Character symbol : this.alphabet.getSymbols()) {
			if (symbol != 'e')
				arr2.add(new HashMap<>());
		}
		return arr2;
	}

	// A method to constructs the full dfsm nodes (from states) within the
	// arraylist of maps - a map for each symbol in the alphabet.
	private void initNodesDfsmArray(ArrayList<Map<State, Set<State>>> arr, ArrayList<Map<Set<State>, Set<State>>> arr2,
			List<Character> symbols, State terminate) {
		for (int i = 0; i < symbols.size(); i++) {
			Set<State> temp = new HashSet<>();
			temp.add(this.initialState);
			arr2.get(i).put(temp, new HashSet<>());
			temp = new HashSet<>();
			temp.add(terminate);
			arr2.get(i).put(temp, new HashSet<>());
			for (Set<State> values : arr.get(i).values()) {
				if (!values.isEmpty())
					arr2.get(i).put(values, new HashSet<>());
			}
		}
	}

	// This method constructs the full nodes(from state), transitions and
	// children (to states) of the final dfsm.
	private void constructFullDfsmArray(ArrayList<Map<State, Set<State>>> arr,
			ArrayList<Map<Set<State>, Set<State>>> arr2, State terminate) {
		int j = 0;
		for (int i = 0; i < arr2.size(); i++) {
			for (Set<State> statesSet : arr2.get(i).keySet()) {
				Iterator<State> it = statesSet.iterator();
				while (it.hasNext()) {
					State temp = it.next();
					for (State state : arr.get(j).keySet()) {
						if (temp == state) {
							if (!arr.get(j).get(state).isEmpty()) {
								for (State child : arr.get(j).get(state)) {
									if (!child.equals(terminate))
										arr2.get(i).get(statesSet).add(child);
								}
							}
						}
					}
				}
			}
			j++;
		}
	}

	// A method to add the 'terminate' state in the dfsm arraylist.
	private void determineTerminateDfsm(ArrayList<Map<Set<State>, Set<State>>> arr2, IdentifiedState terminate) {
		for (int i = 0; i < arr2.size(); i++) {
			for (Set<State> key : arr2.get(i).keySet()) {
				if (arr2.get(i).get(key).isEmpty()) {
					arr2.get(i).get(key).add(terminate);
				}
			}
		}
	}

	// A method to extract data from the arraylist and to build the delta
	// function.
	private void initializeDelta(ArrayList<Map<Set<State>, Set<State>>> arr2, Set<State> states,
			Set<Transition> delta) {
		for (int i = 0; i < arr2.size(); i++) {
			char symbol = this.alphabet.getSymbols().get(i);
			if (symbol == 'e')
				break;
			for (Set<State> key : arr2.get(i).keySet()) {
				for (State fromState : states) {
					if (fromState.equals(new IdentifiedState(key.hashCode()))) {
						for (State toState : states) {
							if (toState.equals(new IdentifiedState(arr2.get(i).get(key).hashCode()))) {
								Transition t = new Transition(fromState, symbol, toState);
								delta.add(t);
							}
						}
					}
				}
			}
		}
	}

	// A method to determine and initialize all accepting states for the new
	// dfsm.
	private void initializeAcceptingStates(ArrayList<Map<Set<State>, Set<State>>> arr2, Set<State> acceptingStates, State initialState, Map<State, Set<State>> epsilonClosureGraph) {
		// This loop determines whether to add the initial state to the accepting states set.
		for(State key : epsilonClosureGraph.keySet()){
			if(initialState.equals(new IdentifiedState(key.hashCode()))){
				for(State state : epsilonClosureGraph.get(key)){
					for(State state2 : this.acceptingStates){
						if(state2.equals(state)){
							acceptingStates.add(initialState);
							break;
						}
					}
				}
			}
		}
		for (int i = 0; i < arr2.size(); i++) {
			for (Set<State> statesSet : arr2.get(i).keySet()) {
				Iterator<State> it = statesSet.iterator();
				while (it.hasNext()) {
					State temp = it.next();
					for (State state : this.acceptingStates) {
						if (state.equals(temp)) {
							acceptingStates.add(new IdentifiedState(statesSet.hashCode()));
						}
					}
				}
			}
		}

	}
	
	// Construct final dfsm and return it.
	private DFSM getDfsm(Set<State> states, List<Character> symbols, Set<Transition> delta, State initialState,
			Set<State> acceptingStates) {
		try {
			Alphabet alphabet = new Alphabet(symbols);
			DFSM dfsm = new DFSM(states, alphabet, delta, initialState, acceptingStates);
			return dfsm;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
