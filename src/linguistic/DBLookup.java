package linguistic;

import java.util.HashMap;

public class DBLookup {
	
	/**
	 * This is a placeholder for lookup up a search string in the database.
	 * 
	 * Until our verbs are in the db with definitions, I'm using
	 * this little hashmap.
	 */
	
	private static HashMap<String, String> verbList = new HashMap<String, String>();
	static {
		verbList.put("take-VB x-NN in-IN", "absorb, kinda");
		verbList.put("get-VB x-NN of-IN", "what");
		verbList.put("keep-VB tabs-NN on-IN", "track activity");
	}
	
	/** Takes a query string in the form of
	 *    average-vb out-in
	 *    
	 *  
	 * @param queryString
	 * @return definition of the phrase
	 */
	public static String search(String queryString) {
		if (verbList.containsKey(queryString)) {
			return verbList.get(queryString);
		}
		return "no definition found";
	}

}
