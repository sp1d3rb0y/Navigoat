package fr.spiderboy.navigoat;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by spiderboy on 20/04/2015.
 */
/// TODO : Add another TextView to store title (content array)
public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private ArrayList<Integer> icons;
    private ArrayList<String> content;

    public static class Element {
        private String line = "";
        private String title;
        private String desc = "";

        public Element(String title, String content) {
            this.title = title;
            this.desc = content;
        }

        public Element(String title, String content, String line) {
            this.title = title;
            this.line = line;
            this.desc = content;
        }

        public String getTitle() {
            return title;
        }
    }

    public CustomListAdapter(Activity context) {
        super(context, R.layout.simplerow);
        this.context = context;
        this.icons = new ArrayList<>();
        this.content = new ArrayList<>();
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.simplerow, null, true);

        TextView title = (TextView) rowView.findViewById(R.id.rowTextView);
        ImageView image = (ImageView) rowView.findViewById(R.id.rowImageView);
        TextView desc = (TextView) rowView.findViewById(R.id.rowTextDescView);
        Integer img_rsc_id = icons.get(position);
        if (img_rsc_id == -1) {
            image.setVisibility(View.GONE);
        } else {
            image.setImageResource(icons.get(position));
        }
        title.setText(getItem(position));
        desc.setText(content.get(position));
        return rowView;
    }

    @Override
    public void add(String title) {
        this.add(title, "");
    }

    public void add(String title, String content) {
        this.add(title, content, "");
    }

    public void add(String title, String content, String line) {
        int id = -1;
        if (line != null && line.equals("")) {
            id = context.getResources().getIdentifier("unknown", "drawable", context.getPackageName());
        } else if (line != null && line.length() > 0) {
            id = context.getResources().getIdentifier(line.toLowerCase().replace(" ", "_"), "drawable", context.getPackageName());
        }
        icons.add(id);
        this.content.add(content);
        super.add(title);
    }

    public void add(Element elt) {
        this.add(elt.title, elt.desc, elt.line);
    }

    public void add_header(String title) {
        this.add(title, "", null);
    }

    @Override
    public void clear() {
        icons.clear();
        content.clear();
        super.clear();
    }

}
