package com.example.filip.chemeq.detecting;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.filip.chemeq.R;

import java.util.List;

public class RecognitionAdapter extends ArrayAdapter<RecognitionListItem> {

    private Context context;
    private List<RecognitionListItem> recognitionList;

    public RecognitionAdapter(Context context, List<RecognitionListItem> list) {
        super(context, 0, list);
        this.context = context;
        recognitionList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(context).inflate(R.layout.recognition_list_item, parent, false);

        RecognitionListItem currentRecognition = recognitionList.get(position);

        TextView textView = listItem.findViewById(R.id.rowTextView);
        textView.setText(currentRecognition.getEquation());
        return listItem;
    }
}
