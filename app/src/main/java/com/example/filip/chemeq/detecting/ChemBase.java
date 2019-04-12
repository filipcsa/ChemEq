package com.example.filip.chemeq.detecting;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class for static access of known chemical compounds
 */
public class ChemBase {

    static List<Pair<String, String>> compounds = new ArrayList<>();

    /**
     * Loads the json file after starting the app
     * @param context needed to access assets
     * @return
     */
    public static void loadJSON(Context context) throws JSONException {
        String file = "chem_db.json";
        String json = null;
        try {
            InputStream in = context.getAssets().open(file);
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            json = new String(buffer, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
            obj = obj.getJSONObject("_default");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        Iterator<String> keys = obj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            JSONObject val = (JSONObject) obj.get(key);
            Pair<String, String> pair = new Pair<>(val.getString("formula"), val.getString("name"));
            Log.i("CHEMBASE", pair.first + " " + pair.second);
            compounds.add(pair);
        }
    }

    public static String getNameOfFormula(String formula) {
        for (Pair<String, String> pair : compounds) {
            if (pair.first.equals(formula))
                return pair.second;
        }
        return "unknown";
    }

    private static int levenstein(String a, String b){
        return 0;
    }
}
