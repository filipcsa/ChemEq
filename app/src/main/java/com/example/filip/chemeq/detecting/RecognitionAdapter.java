package com.example.filip.chemeq.detecting;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.filip.chemeq.R;
import com.example.filip.chemeq.util.Logger;

import java.util.Iterator;
import java.util.List;

public class RecognitionAdapter extends ArrayAdapter<RecognitionListItem> {

    private Context context;
    private List<RecognitionListItem> recognitionList;
    private static final Logger LOGGER = new Logger(RecognitionAdapter.class.getName());

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

        /*
        TextView textView = listItem.findViewById(R.id.rowTextView);
        textView.setText(currentRecognition.getEquation());
        */


        TableRow formulaNamesRow = listItem.findViewById(R.id.formulaNames);
        TableRow formulasRow = listItem.findViewById(R.id.formulas);

        formulaNamesRow.removeAllViews();
        formulasRow.removeAllViews();


        if (currentRecognition.isMath()) {
            TextView a = initializeTextView();
            int result = 0;
            if (currentRecognition.getOp() == '+')
                result = currentRecognition.getA() + currentRecognition.getB();
            if (currentRecognition.getOp() == '-')
                result = currentRecognition.getA() - currentRecognition.getB();
            if (currentRecognition.getOp() == '×')
                result = currentRecognition.getA() * currentRecognition.getB();
            if (currentRecognition.getOp() == '÷')
                result = currentRecognition.getA() / currentRecognition.getB();
            String matheq = (currentRecognition.getA() + " " + currentRecognition.getOp() + " " + currentRecognition.getB() + " = " + result);
            a.setText(matheq);
            formulasRow.addView(a);
            return listItem;
        }

        // the left side of the equation
        Iterator it = currentRecognition.getLeftSideCompounds().iterator();
        while (it.hasNext()) {
            Pair<String, String> compound = (Pair<String, String>) it.next();
            TextView formulaName = initializeTextView();
            TextView formula = initializeTextView();

            String fn = compound.second.split(",")[0];
            formulaName.setText(fn);
            formula.setText(compound.first);

            formulaNamesRow.addView(formulaName);
            formulasRow.addView(formula);

            if (it.hasNext()) {
                TextView p1 = initializeTextView();
                TextView p2 = initializeTextView();

                p1.setText("");
                p2.setText("+");

                formulaNamesRow.addView(p1);
                formulasRow.addView(p2);
            }
        }

        // adding the equation arrow
        if (currentRecognition.getLeftSideCompounds().size() != 0) {
            TextView p1 = initializeTextView();
            TextView p2 = initializeTextView();
            p2.setTextSize(30);

            p1.setText("");
            p2.setText("→");

            formulaNamesRow.addView(p1);
            formulasRow.addView(p2);
        }

        // the right side of the equation
        it = currentRecognition.getRightSideCompounds().iterator();
        while (it.hasNext()) {
            Pair<String, String> compound = (Pair<String, String>) it.next();
            TextView formulaName = initializeTextView();
            TextView formula = initializeTextView();

            String fn = compound.second.split(",")[0];
            formulaName.setText(fn);
            formula.setText(compound.first);

            formulaNamesRow.addView(formulaName);
            formulasRow.addView(formula);

            if (it.hasNext()) {
                TextView p1 = initializeTextView();
                TextView p2 = initializeTextView();

                p1.setText("");
                p2.setText("+");

                formulaNamesRow.addView(p1);
                formulasRow.addView(p2);
            }
        }

        return listItem;
    }

    private TextView initializeTextView() {
        TextView textView = new TextView(context);
        textView.setPadding(8,0,8,0);
        textView.setTextSize(16);
        textView.setGravity(Gravity.CENTER);

        return textView;
    }
}
