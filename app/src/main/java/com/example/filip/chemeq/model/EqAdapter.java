package com.example.filip.chemeq.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.filip.chemeq.R;
import com.example.filip.chemeq.util.Logger;

import java.util.Iterator;
import java.util.List;

public class EqAdapter extends ArrayAdapter<EqListItem> {

    private Context context;
    private List<EqListItem> recognitionList;
    private static final Logger LOGGER = new Logger(EqAdapter.class.getName());

    public EqAdapter(Context context, List<EqListItem> list) {
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

        EqListItem currentRecognition = recognitionList.get(position);

        /*
        TextView textView = listItem.findViewById(R.id.rowTextView);
        textView.setText(currentRecognition.getEquation());
        */


        TableRow formulaNamesRow = listItem.findViewById(R.id.formulaNames);
        TableRow formulasRow = listItem.findViewById(R.id.formulas);
        TableLayout table = listItem.findViewById(R.id.table);
        EditText editText = listItem.findViewById(R.id.editEquation);

        formulaNamesRow.removeAllViews();
        formulasRow.removeAllViews();

        if (currentRecognition.getEquationTest() == null)
            return listItem;

        if (currentRecognition.getEquationTest().getEquationType() == Equation.Type.MATH) {
            TextView a = initializeTextView();
            int result = 0;
            if (currentRecognition.getEquationTest().getOp() == '+')
                result = currentRecognition.getEquationTest().getA() + currentRecognition.getEquationTest().getB();
            if (currentRecognition.getEquationTest().getOp() == '-')
                result = currentRecognition.getEquationTest().getA() - currentRecognition.getEquationTest().getB();
            if (currentRecognition.getEquationTest().getOp() == '×')
                result = currentRecognition.getEquationTest().getA() * currentRecognition.getEquationTest().getB();
            if (currentRecognition.getEquationTest().getOp() == '÷')
                result = currentRecognition.getEquationTest().getA() / currentRecognition.getEquationTest().getB();
            String matheq = (currentRecognition.getEquationTest().getA() + " " + currentRecognition.getEquationTest().getOp() + " " + currentRecognition.getEquationTest().getB() + " = " + result);
            a.setText(matheq);
            formulasRow.addView(a);
            return listItem;
        }

        // the left side of the equation
        Iterator<Compound> it = currentRecognition.getEquationTest().getLeftCompounds().iterator();
        while (it.hasNext()) {
            Compound compound = it.next();
            TextView formulaName = initializeTextView();
            TextView formula = initializeTextView();

            String fn = compound.getTrivName().split(",")[0];
            formulaName.setText(fn);
            formula.setText(compound.getCompound());

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
        if (currentRecognition.getEquationTest().getLeftCompounds().size() != 0) {
            TextView p1 = initializeTextView();
            TextView p2 = initializeTextView();
            p2.setTextSize(30);

            p1.setText("");
            p2.setText("→");

            formulaNamesRow.addView(p1);
            formulasRow.addView(p2);
        }

        // the right side of the equation
        it = currentRecognition.getEquationTest().getRightCompounds().iterator();
        while (it.hasNext()) {
            Compound compound = it.next();
            TextView formulaName = initializeTextView();
            TextView formula = initializeTextView();

            String fn = compound.getTrivName().split(",")[0];
            formulaName.setText(fn);
            formula.setText(compound.getCompound());

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

        table.setOnTouchListener(new View.OnTouchListener(){
            private GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    LOGGER.i("DOUBLE TAP!");
                    onDoubleTapTableCallback();

                    return super.onDoubleTap(e);
                }

                /** HANDLING THE DOUBLE TAP **/
                private void onDoubleTapTableCallback() {
                    table.setVisibility(View.GONE);
                    String eq = currentRecognition.getEquationTest().getFullEquation();
                    editText.setText(eq);
                    editText.setVisibility(View.VISIBLE);
                }
            });
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                LOGGER.i("On FOCUS changed");
                long t = System.currentTimeMillis();
                long delta = t - focusTime;
                if (hasFocus) {
                    LOGGER.i("edit equation gained FOCUSED");
                    if (delta > minDelta) {
                        focusTime = t;
                        focusTarget = v;
                    }
                }
                else {
                    if (delta <= minDelta && v == focusTarget) { // reset lost focus
                        LOGGER.i("reseting FOCUS");
                        focusTarget.requestFocus();
                    }
                    else {
                        LOGGER.i("Focus LOST");
                        editText.setVisibility(View.GONE);
                        table.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        return listItem;
    }

    // some deltas for focus
    private final int minDelta = 300;
    private long focusTime = 0;
    private View focusTarget = null;

    public void looseFocus() {
        if (focusTarget != null){
            LOGGER.i("CLEARING FOCUS");
            focusTarget.clearFocus();
        }
    }


    private TextView initializeTextView() {
        TextView textView = new TextView(context);
        textView.setPadding(8,0,8,0);
        textView.setTextSize(16);
        textView.setGravity(Gravity.CENTER);

        return textView;
    }
}
