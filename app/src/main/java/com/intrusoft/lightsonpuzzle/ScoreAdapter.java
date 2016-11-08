package com.intrusoft.lightsonpuzzle;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by IntruSoft on 2/3/2016.
 */
public class ScoreAdapter extends BaseAdapter {

    Context context;
    DatabaseHelper helper;
    int possibleN[] = new int[]{2, 3, 6, 7, 8};

    ScoreAdapter(Context context) {
        helper = new DatabaseHelper(context);
        this.context = context;
        helper.open();
    }

    @Override
    public int getCount() {
        return possibleN.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.score_parent_layout, null);
        }
        Cursor cursor = helper.getSelect(possibleN[position]);
        TextView l = (TextView) convertView.findViewById(R.id.level);
        l.setText("Level " + (position+1));
        CustomListView listView = (CustomListView) convertView.findViewById(R.id.childList);
        listView.setAdapter(new ChildAdapter(cursor));
        return convertView;
    }

    public class ChildAdapter extends BaseAdapter {

        Cursor cursor;

        ChildAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.score_child_layout, null);
            }
            TextView name = (TextView) convertView.findViewById(R.id.name);
            TextView time = (TextView) convertView.findViewById(R.id.time);
            TextView move = (TextView) convertView.findViewById(R.id.moves);
            TextView score = (TextView) convertView.findViewById(R.id.score);
            cursor.moveToPosition(position);
            name.setText(cursor.getString(cursor.getColumnIndex("name")));
            time.setText(cursor.getString(cursor.getColumnIndex("time")));
            move.setText(cursor.getString(cursor.getColumnIndex("steps")) + " Step");
            score.setText(cursor.getString(cursor.getColumnIndex("score")));
            return convertView;
        }
    }
}
