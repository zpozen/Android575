package megazi.smartup;

import java.io.IOException;
import java.io.InputStream;

import linguistic.PhraseFinder;
import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.TextView;

public class SmartUpActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        AssetManager assetMgr = getAssets();
        //here is the string that we've decided to look for a phrase in
        String inputPhrase = "She's keeping tabs on the spy";
        //if you see this on the screen, something didn't work
        String result = "this is the default text";
        try {
        	//the phrase handler can't reach assets, so pass in a stream from here
			InputStream model = assetMgr.open("pos-en-general-brown.HiddenMarkovModel");
			PhraseFinder phraseHandler = new PhraseFinder(model);
			
			//probably the easiest way to mark the highlights is to break the input string into
			//something with indices. it's up to you to map the indices to the larger text
			//context in TextView
			String [] indexed = inputPhrase.split("\\s+");
			if(phraseHandler.findPhrasalVerb(indexed)) {
				StringBuilder sb = new StringBuilder();
				//pull the phrase out of the text we sent
				for (int i = phraseHandler.getBeginningIndex(); i <= phraseHandler.getEndIndex(); i++)
					sb.append(indexed[i]).append(" ");
				result = sb.toString();
			}
			
		} catch (IOException e) {
			System.out.println("Failed to find HMM file");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Failed to find aliasi classes");
			e.printStackTrace();
		}
        
        //display result on page
        TextView tv = new TextView(this);
        tv.setText(inputPhrase + "\n" + result);
        setContentView(tv);
    }
}