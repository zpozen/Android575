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
        //if you see this, something didn't work
        String result = "this is the default text";
        try {
        	//the phrase handler can't reach assets, so pass in a stream from here
			InputStream model = assetMgr.open("pos-en-general-brown.HiddenMarkovModel");
			PhraseFinder phraseHandler = new PhraseFinder(model);
			result = phraseHandler.findPhrasalVerb(inputPhrase);
		} catch (IOException e) {
			System.out.println("Failed to find HMM file");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Failed to find aliasi classes");
			e.printStackTrace();
		}
        
        //display result on page
        TextView tv = new TextView(this);
        tv.setText(result);
        setContentView(tv);
    }
}