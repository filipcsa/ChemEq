package com.example.filip.chemeq;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.example.filip.chemeq.detecting.ChemBase;
import com.example.filip.chemeq.detecting.ChemicalEquation;
import com.example.filip.chemeq.detecting.Compound;
import com.example.filip.chemeq.ocr.TessOCRAnalyzer;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TessOCRAnalyzerTest {

    Context testContext;
    TessOCRAnalyzer t;

    @Before
    public void setUp() {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        t = new TessOCRAnalyzer(null, null);

        try {
            ChemBase.loadJSON(testContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    String[] filenames = {"10.png","100.png","101.png","1010.png","102.png","1020.png","1021.png","1030.png","1031.png","1050.png","1060.png","1070.png","1080.png","110.png","1130.png","1131.png","1150.png","1160.png","1170.png","1171.png","1172.png","1180.png","1190.png","1191.png","1192.png","1193.png","120.png","1200.png","1201.png","1202.png","1203.png","1220.png","1221.png","1222.png","1223.png","1230.png","1231.png","1240.png","1241.png","1242.png","1270.png","1271.png","1280.png","1281.png","1290.png","1291.png","1320.png","1321.png","1322.png","1340.png","1341.png","1350.png","1351.png","1352.png","1353.png","1354.png","1370.png","1390.png","1391.png","1392.png","140.png","1410.png","1460.png","1470.png","1480.png","150.png","1510.png","1511.png","1520.png","1530.png","1540.png","1541.png","1550.png","1551.png","1560.png","1561.png","1580.png","1581.png","1590.png","1610.png","1611.png","1620.png","1621.png","1640.png","1641.png","1642.png","1660.png","1680.png","170.png","1700.png","1701.png","1702.png","1703.png","1710.png","1711.png","1712.png","1713.png","1720.png","1721.png","1722.png","1723.png","1740.png","1741.png","1742.png","1750.png","1770.png","1790.png","180.png","1800.png","1810.png","1811.png","1812.png","1840.png","1860.png","1880.png","1890.png","1891.png","190.png","1900.png","1920.png","1921.png","1940.png","1950.png","1951.png","1960.png","1990.png","1991.png","20.png","200.png","2000.png","2010.png","2030.png","2050.png","2070.png","2071.png","2072.png","2073.png","2080.png","2081.png","2082.png","2083.png","210.png","2100.png","2101.png","2102.png","2110.png","2111.png","2112.png","2120.png","2121.png","2122.png","2130.png","2131.png","2140.png","2170.png","2180.png","220.png","2210.png","2220.png","2240.png","2241.png","2242.png","2243.png","2250.png","2251.png","2252.png","2253.png","2254.png","2260.png","2261.png","2262.png","2263.png","2270.png","2271.png","2272.png","2273.png","2280.png","2281.png","2290.png","2291.png","230.png","2300.png","231.png","2310.png","232.png","2320.png","2330.png","2340.png","2350.png","2351.png","2352.png","2353.png","2360.png","2361.png","2370.png","2380.png","2381.png","2382.png","2390.png","2391.png","2400.png","2401.png","2410.png","2420.png","2430.png","2431.png","2432.png","2433.png","2434.png","2435.png","2440.png","2441.png","2442.png","2443.png","2450.png","2451.png","2460.png","2461.png","2470.png","2471.png","2472.png","2480.png","2490.png","2491.png","2500.png","2501.png","2510.png","2511.png","2520.png","2521.png","280.png","290.png","300.png","310.png","320.png","350.png","360.png","370.png","380.png","381.png","382.png","390.png","391.png","40.png","410.png","420.png","421.png","422.png","430.png","431.png","440.png","450.png","451.png","452.png","460.png","461.png","480.png","490.png","500.png","501.png","502.png","510.png","520.png","521.png","530.png","540.png","541.png","570.png","571.png","580.png","581.png","590.png","591.png","60.png","600.png","601.png","61.png","610.png","611.png","620.png","650.png","651.png","660.png","661.png","670.png","680.png","681.png","682.png","690.png","691.png","692.png","70.png","71.png","710.png","711.png","72.png","73.png","730.png","731.png","750.png","751.png","760.png","770.png","780.png","790.png","791.png","80.png","800.png","801.png","81.png","82.png","83.png","830.png","831.png","840.png","850.png","851.png","860.png","880.png","881.png","890.png","891.png","90.png","900.png","901.png","902.png","903.png","91.png","92.png","920.png","921.png","922.png","923.png","93.png","930.png","94.png","940.png","941.png","95.png","970.png","971.png","980.png","981.png","990.png"};

    String folder = "test_images/";
    String[] testSamples = {"10.png", "100.png", "101.png", "1010.png", "102.png", "1020.png", "1021.png", "1030.png", "1031.png", "1050.png", "1060.png", "1070.png", "110.png", "1130.png", "1131.png", "1150.png", "1160.png", "1170.png", "1171.png", "1172.png", "1180.png", "1190.png", "1191.png", "120.png", "1200.png", "1201.png", "1202.png", "1203.png", "1220.png", "1221.png", "1222.png", "1223.png", "1240.png", "1270.png", "1271.png", "1280.png", "1281.png", "1290.png", "1370.png", "1390.png", "1391.png", "1392.png", "140.png", "1410.png", "1460.png", "1470.png",
            "1480.png", "150.png", "1510.png", "1511.png", "1530.png", "1540.png", "1541.png", "1550.png", "1551.png", "1560.png", "1561.png", "1580.png", "1581.png", "1590.png", "1610.png", "1611.png", "1640.png", "1641.png", "1642.png", "1660.png", "1680.png", "1700.png", "1701.png", "1710.png", "1711.png", "1712.png", "1713.png", "1720.png", "1721.png", "1722.png", "1723.png", "1740.png", "1741.png", "1742.png", "1750.png", "1770.png", "1790.png", "180.png", "1800.png", "1811.png", "1812.png", "1840.png", "1860.png", "1880.png", "1890.png", "1891.png", "190.png", "1900.png", "1920.png", "1921.png", "1950.png", "1951.png", "1960.png", "1990.png", "1991.png", "20.png", "200.png", "2000.png", "2010.png", "2070.png", "2071.png", "2072.png", "2073.png", "2080.png", "2081.png", "2082.png", "2083.png", "210.png", "2100.png", "2101.png", "2102.png", "2110.png", "2111.png", "2120.png", "2121.png", "2122.png", "2130.png", "2131.png", "2140.png", "2170.png", "2180.png", "220.png", "2210.png", "2220.png", "2240.png", "2241.png", "2242.png", "2250.png", "2251.png", "2252.png", "2253.png", "2254.png", "2260.png", "2261.png", "2262.png", "2263.png", "2270.png", "2271.png", "2272.png", "2273.png", "2280.png", "2281.png", "2290.png", "2291.png", "230.png", "2300.png", "231.png", "2310.png", "232.png", "2320.png", "2330.png", "2350.png", "2351.png", "2352.png", "2353.png", "2360.png", "2361.png", "2380.png", "2381.png",
            "2382.png", "2390.png", "2391.png", "2440.png", "2441.png", "2442.png", "2443.png", "2450.png", "2460.png", "2461.png", "2470.png", "2471.png", "2472.png", "2480.png", "2490.png", "2491.png", "2500.png", "2501.png", "2510.png", "2511.png", "2520.png", "2521.png", "280.png", "290.png", "300.png", "310.png", "320.png", "350.png", "360.png", "370.png", "380.png", "390.png", "391.png", "40.png", "410.png", "420.png", "421.png", "430.png", "431.png", "440.png", "450.png", "451.png", "452.png", "460.png", "461.png", "480.png", "490.png", "530.png", "540.png", "541.png", "570.png", "571.png", "580.png", "590.png", "591.png", "60.png", "600.png", "61.png", "620.png", "650.png", "660.png", "661.png", "670.png", "680.png", "690.png", "691.png", "70.png", "71.png", "710.png", "711.png", "72.png", "760.png", "791.png", "80.png", "801.png", "81.png",
            "82.png", "83.png", "830.png", "831.png", "840.png", "880.png", "890.png", "90.png", "900.png", "901.png", "902.png", "903.png", "91.png", "92.png", "920.png", "921.png", "922.png", "930.png", "940.png", "941.png", "95.png", "970.png", "971.png", "980.png", "981.png", "990.png"};


    @Test
    public void runAnalyzerTest() {
        String log = "";
        int totalEqs = 0;
        int correctEqs = 0;
        int correctRaw = 0;
        int totalCompounds = 0;
        int correctCompounds = 0;
        int totalDistance = 0;
        Instant before = Instant.now();

        for (String testSample : testSamples) {
            // READ IMAGE FILE AND ANNOTATION
            AssetManager assetManager = testContext.getAssets();
            InputStream testInput = null;
            try {
                testInput = assetManager.open(folder + testSample);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap bitmap = BitmapFactory.decodeStream(testInput);
            ChemicalEquation chemeq = t.testOCR(bitmap);
            // log += "Detected: " + chemeq.getFullEquation() + "\n";

            // READ ANNOTATION
            String annFileName = testSample + ".txt";
            FileReader fileReader = null;
            String expected = "";
            try {
                InputStreamReader isr = new InputStreamReader(testContext.getAssets()
                .open(folder + annFileName));
                // log += "Expected: ";
                BufferedReader bufferedReader = new BufferedReader(isr);
                expected = bufferedReader.readLine();
                // log += expected + "\n";
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*
            // VYPISY
            log += "Possible Left: \n";
            for (List<Compound> possibleLeft : chemeq.getAllPossibleLeft()) {
                for (Compound c : possibleLeft) {
                    log += c.getCompound() + " = " + c.getTrivName() + " | ";
                }
                log += "\n-\n";
            }

            log += "Possible right: \n";
            for (List<Compound> possibleRight : chemeq.getAllPossibleRight()) {
                for (Compound c : possibleRight) {
                    log += c.getCompound() + " = " + c.getTrivName() + " | ";
                }
                log += "\n-\n";
            }
            */

            /*
            if (expected == null || !expected.contains("→"))
                log += "RETARDED IMAGE " + testSample + "\n";
            */

            if (chemeq.getFullEquation().equals(expected))
                correctEqs++;
            else {
                totalDistance += calculate(chemeq.getFullEquation(), expected);
                log += "ERROR AT " + testSample;
                log += "Expected: " + expected;
                log += "Actual: " + chemeq.getFullEquation();
            }

            if (chemeq.getRawDetection().equals(expected))
                correctRaw++;
            totalEqs++;


            // COUNTING COMPOUNDS
            String actual = chemeq.getRawDetection();
            String actualLeft = actual.split(" → ")[0];
            String actualRight = "";
            if (actual.split(" → ").length > 1)
                actualRight = actual.split(" → ")[1];
            String[] acLeftComps = actualLeft.split(" \\+ ");
            String[] acRightComps = actualRight.split(" \\+ ");
            List<String> actualLeftComps = new ArrayList<>(Arrays.asList(acLeftComps));
            List<String> actualRightComps = new ArrayList<>(Arrays.asList(acRightComps));


            String left = expected.split(" → ")[0];
            String right = expected.split(" → ")[1];
            String[] leftComps = left.split(" \\+ ");
            String[] rightComps = right.split(" \\+ ");
            List<String> expectedLeftComps = new ArrayList<>(Arrays.asList(leftComps));
            List<String> expectedRightComps = new ArrayList<>(Arrays.asList(rightComps));

            totalCompounds += leftComps.length + rightComps.length;

            // intersections
            expectedLeftComps.retainAll(actualLeftComps);
            expectedRightComps.retainAll(actualRightComps);

            correctCompounds += expectedLeftComps.size() + expectedRightComps.size();

            log += "\n\n\n";
        }
        log += "----------------------\n\n\n";

        Instant after = Instant.now();
        long dur = Duration.between(before, after).toMillis();
        log += "Duration: " + dur + "ms\n";
        log += "Equations: " + totalEqs + "\n";
        log += "Correctly recognized: " + correctEqs + "\n";
        log += "Correct raw: " + correctRaw + "\n";
        log += "Total compounds: " + totalCompounds + "\n";
        log += "Correct compounds: " + correctCompounds + "\n";
        log += "Total distance: " + totalDistance + "\n";

        saveStringToFile(log, "test.txt");
    }

    private int calculate(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }







    /*
    @Test
    public void createTestset() {

        try {
            ChemBase.loadJSON(testContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
        try {
            createAnnotations("82.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        /*
        for (String filename : filenames) {
            try {
                createAnnotations(filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void foo() {
        try {
            ChemBase.loadJSON(testContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            bar("82.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bar(String filename) throws IOException {
        AssetManager assetManager = testContext.getAssets();
        String string = "";

        InputStream testInput = assetManager.open(filename);

        Bitmap bitmap = BitmapFactory.decodeStream(testInput);
        List<Compound> chemeq = t.test2OCR(bitmap);
        for (Compound compound : chemeq) {
            string += compound.getCompound() + "\n";
            string += ChemBase.getNameOfFormula(compound.getCompound()) + "\n";
        }
        saveStringToFile(string, filename);
    }
    */

    public void createAnnotations(String filename) throws IOException {
        // String filename = "1580.png";
        AssetManager assetManager = testContext.getAssets();
        String string = "";

        InputStream testInput = assetManager.open(filename);

        Bitmap bitmap = BitmapFactory.decodeStream(testInput);
        ChemicalEquation chemeq = t.testOCR(bitmap);

        /*
        string += "Left side compounds: " + chemeq.getLeftCompounds().size() + "\n";
        string += "Right side compounds: " + chemeq.getRightCompounds().size() + "\n";
        string += "Raw detection: " + chemeq.getRawDetection() + "\n";
        */
        Iterator<Compound> it = chemeq.getLeftCompounds().iterator();
        while (it.hasNext()) {
            string += it.next().getCompound();
            if (it.hasNext()) string += " + ";
        }

        if (chemeq.getRightCompounds().size() > 0)
            string += " → ";

        it = chemeq.getRightCompounds().iterator();
        while (it.hasNext()) {
            string += it.next().getCompound();
            if (it.hasNext()) string += " + ";
        }

        saveStringToFile(string, filename);

    }

    public void saveStringToFile(String string, String filename) {
        filename += ".txt";
        File testFile = new File(Environment.getExternalStorageDirectory().toString(), filename);
        FileWriter writer;
        try {
            writer = new FileWriter(testFile);
            writer.append(string);
            writer.flush();
            writer.close();
            MediaScannerConnection.scanFile(testContext, new String[] { testFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
