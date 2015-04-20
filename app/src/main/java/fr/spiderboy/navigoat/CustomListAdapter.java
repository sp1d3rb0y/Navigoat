package fr.spiderboy.navigoat;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by spiderboy on 20/04/2015.
 */
public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;

    public CustomListAdapter(Activity context) {
        super(context, R.layout.simplerow);
        this.context = context;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.simplerow, null, true);

        TextView title = (TextView) rowView.findViewById(R.id.rowTextView);
        ImageView image = (ImageView) rowView.findViewById(R.id.rowImageView);
        String val = getItem(position);
        if (val.contains("Ligne ")) {
            /// TODO : Handle 3bis
            int number = Integer.parseInt(val.substring(6, 7));
            int id = context.getResources().getIdentifier("l" + number, "drawable", context.getPackageName());
            Log.i(MainActivity.dTag, "Found id " + id);
            image.setImageResource(id);
        } else {
            image.setImageResource(R.drawable.abc_btn_radio_to_on_mtrl_015);
        }
        /// TODO : Remove line number from text
        title.setText(getItem(position));
        return rowView;
    }

}
