package megazi.smartup;


import java.io.IOException;
import java.util.Map;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/*
 * This is a single-selection list view, showing files under assets/texts
 */
public class SelectTextActivity extends ListActivity {	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        try {
			mStrings = getAssets().list("texts");
		} catch (IOException e) {			
			e.printStackTrace();
		}
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, 
                mStrings));
        
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setTextFilterEnabled(true);
    }

    private String[] mStrings;    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        
    	String text = (String)l.getItemAtPosition(position);        
        
        Intent myIntent = new Intent(this, SmartUpActivity.class);        
        myIntent.putExtra("read", "texts/" + text);
        this.startActivity(myIntent);
    }
}
