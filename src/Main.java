import java.util.ArrayList;
import java.util.List;
import ac.il.afeka.Submission.Submission;
import ac.il.afeka.fsm.DFSM;
import ac.il.afeka.fsm.NDFSM;

public class Main implements Submission {

	public static void main(String[] args) {
		try {
			// An example of a NDFSM ('e' stands for epsilon) that gets converted to a DFSM.
			String aNDFSMencoding = "0 1 2 3 /a b e/0 , e, 1; 0, a, 1; 0, b, 2; 1, e, 3; 2, e, 1; 2, a, 2; 2, a, 3; 3, b, 1; 3, b, 3/0/ 1 3";
			DFSM dfsm = convert(aNDFSMencoding);
			System.out.println(dfsm.compute("")); // The empty string is identified.
			System.out.println(dfsm.compute("abbb")); // An identified string.
			System.out.println(dfsm.compute("aabbb")); // An unidentified string.
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static DFSM convert(String aNDFSMencoding) throws Exception {
		NDFSM ndfsm = new NDFSM(aNDFSMencoding);
		DFSM dfsm = ndfsm.toDFSM();
		return dfsm;
	}


	@Override
	public List<String> submittingStudentIds() {
		List<String> submittingStudents = new ArrayList<>();
		submittingStudents.add("Gal Tabecka, 201668001");
		return submittingStudents;
	}

}
