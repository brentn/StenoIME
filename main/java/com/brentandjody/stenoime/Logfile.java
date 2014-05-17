package com.brentandjody.stenoime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by brent on 18/02/14.
 * Output logfile similar to Plover's, for stroke analysis
 */
public class Logfile {

    private static final String filename = "sdcard/steno_keyboard.log";
    private BufferedWriter writer;

    public Logfile() {
        File logFile = new File(filename);
        try {
            if (!logFile.exists())
                logFile.createNewFile();
            //BufferedWriter for performance, true to set append to file flag
            writer = new BufferedWriter(new FileWriter(logFile, true));
        }  catch (IOException e) {
             e.printStackTrace();
        }
    }

    public void write(String stroke, String translation) {
        String line = new Date().toString()+" ";
        if (stroke.equals("*")) {
            line += "*Translation(("+stroke+") : ";
        } else {
            line += "Translation(("+stroke+") : ";
        }
        line += translation+")";
        try {
            writer.append(line);
            writer.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
