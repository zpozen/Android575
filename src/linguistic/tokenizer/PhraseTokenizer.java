package linguistic.tokenizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Tokenizes a Project Gutenberg text file
 * @author Megan Galloway
 * 
 * This tokenizer will space out punctuation from words,
 * leaving approximately one sentence per line in the output.
 * It also removes all quotes that do not occur intra-word.
 *
 */
public class PhraseTokenizer {

	/**
	 * @args text file to tokenize [abbreviation file]
	 */
	public static void main(String[] args) {

		PhraseTokenizer tok;
		if (args.length < 2)
			PhraseTokenizer.printHelpAndQuit();
		
		if (args.length > 2)
			tok = new PhraseTokenizer(args[0], args[1], args[2]);
		else
			tok = new PhraseTokenizer(args[0], args[1]);

		try {
			tok.doTokenize();
		}
		catch (Exception e) {
			e.printStackTrace();
			PhraseTokenizer.printHelpAndQuit();
		}
	}

	private String _abbreviationsFilename;
	private String _inputFilename;
	private String _outputFilename;
	
	private Set<String> _abbreviationSet = new HashSet<String>();

	public PhraseTokenizer () {
		
	}

	public PhraseTokenizer (String inputFile, String outputFile) {
		_inputFilename = inputFile;
		_outputFilename = outputFile;
		_abbreviationsFilename = null;
	}

	public PhraseTokenizer (String inputFile, String outputFile, String abbrev) {
		_inputFilename = inputFile;
		_outputFilename = outputFile;
		_abbreviationsFilename = abbrev;
	}
	
	public String[] doTokenize(String phrase) {
		ArrayList<String> aaaaaaa = addSpacing(phrase); 
		return aaaaaaa.toArray(new String[aaaaaaa.size()]);
	}
	
	/**
	 * Break the text out line by line. It's easier to identify where 
	 * the header and footer delimiters are (as opposed to reading word
	 * by word).
	 */
	public void doTokenize() throws IOException {
		readAbbrevs(_abbreviationsFilename);
		Scanner s = null;
		Writer output = new BufferedWriter(new FileWriter(_outputFilename));
		String line;
		boolean isContent = false;

		try {
			s = new Scanner(new BufferedReader(new FileReader(_inputFilename)));
			while(s.hasNextLine()) {
				line = s.nextLine();
				if (!isContent) { //skip all the header stuff
					if (isIntro(line)) isContent = true;
				} else if (isOutro(line)) { //skip all the footer stuff
					break;
				} else //this is actual book content
					;//output.write(addSpacing(line));
			}
		} finally {
			if (s != null) s.close();
			output.close();
		}
	}
	
	/**
	 * Looks for line starting with
	 * *** START OF THIS PROJECT GUTENBERG EBOOK
	 */
	private boolean isIntro(String line) {
		String [] tok = line.split("\\s+");
		if (tok.length >= 2) {
			if (tok[0].equals("***") && tok[1].equals("START"))
				return true;
		}
		return false;
	}
	
	/**
	 * Looks for line starting with
	 * *** END OF THIS PROJECT GUTENBERG EBOOK
	 */
	private boolean isOutro(String line) {
		String [] tok = line.split("\\s+");
		if (tok.length >= 2) {
			if (tok[0].equals("***") && tok[1].equals("END"))
				return true;
		}
		return false;
	}
	
	/**
	 * Takes one line of text and separates words and abbreviations
	 * from punctuation. Also adds newlines at the ends of 
	 * sentence-like endings. Does not care about separating
	 * groups of punctuation, so 
	 *   K.';
	 * becomes
	 *   K .';
	 */
	private ArrayList<String> addSpacing(String in) {
		ArrayList<String> returnMe = new ArrayList<String>();
		String [] whitespaceDelim = in.split("\\s+");
		boolean endOfSentence;
		String leading, trailing;
		
		for (String word : whitespaceDelim) {
			if (word.length() == 0) continue;
			//standardize the word. uppercase, no quotes
			//word = word.toUpperCase();
			word = word.replace("\"", "");
			//don't just willy-nilly replace all single quotes
			if (word.indexOf("\'") == 0) //if it starts with '
				word = word.substring(1,word.length());
			if (word.indexOf("\'") == word.length()-1) //if it ends with '
				word = word.substring(0,word.length()-1);
			
			endOfSentence = false;
			leading = "";
			trailing = "";
			
			if (!_abbreviationSet.contains(word)) { //is this a known entity? leave it alone
				
				//Abbreviations are kind of weird because the exact match
				// could fail due to other punctuation
				// so check for a match after each punctuation find
				
				//Leading punctuation
				int j;
				for(j = 0; j < word.length()-1;) {
					if(word.substring(j, j+1).matches("[a-zA-Z0-9]"))
						break;
					else
						j++;
				}
				if (j != 0 ) { //if it moved past some punctuation this is how we'll know
					leading = word.substring(0,j);
					word =  word.substring(j);
				}
				
				//Abbreviation check, again
				if (!_abbreviationSet.contains(word)) {

					//Trailing punctuation
					int i;
					for(i = word.length()-1; i > 0;) {
						if(word.substring(i, i+1).matches("[a-zA-Z0-9]"))
							break;
						else {
							//see if abbreviation check failed due to trailing punctuation
							if (_abbreviationSet.contains(word.substring(0, i+1)))
								break;
							if (word.substring(i, i+1).matches("[.?!]"))
								endOfSentence = true;
							i--;
						}
					}
					if (i != word.length()-1 ) { //if it moved past some punctuation this is how we'll know
						trailing = word.substring(i+1);
						word = word.substring(0,i+1);
					}

					//We want to separate on dashes, but not hyphens
					int dash = word.indexOf("--");
					if (dash >= 0) {
						word = word.substring(0, dash) + " -- " + word.substring(dash+2);
					}
				} //end of second abbreviation check
			} //end of first abbreviation check
			if (!leading.equals(""))
				returnMe.add(leading);
			returnMe.add(word);
			if (!trailing.equals(""))
				returnMe.add(trailing);
/*
			if (!leading.equals(""))
				returnMe.append(leading + " ");
			returnMe.append(word);
			if (!trailing.equals(""))
				returnMe.append(" " + trailing);
			if (endOfSentence) {
				returnMe.append("\n");
			} else
				returnMe.append(" ");
*/				
		} // for each word

		return returnMe;
	}

	/**
	 * Read in abbreviation file, if any
	 * populates global abbreviation Set
	 */
	private void readAbbrevs(String fileName) throws IOException {
		if (fileName == null)
			return;

		Scanner reader = null;
		String s;
		try {
			reader = new Scanner(new BufferedReader(new FileReader(fileName)));
			while(reader.hasNext()) {
				s = reader.next().toUpperCase();
				_abbreviationSet.add(s);
			}
		}
		finally {
			if (reader != null) reader.close();
		}
	}
	
	private static void printHelpAndQuit() {
		System.out.println("Usage:\njava Tokenizer file_to_tokenize output_file [abbreviation_file]\n");
		System.exit(1);
	}

}
