package com.camcecil.rmnlogstats;

/*
** RMNLogParser.java
**
** Parses the log files of RetailMeNot.com.
**
** This part of a submission for a MindSumo contest from RetailMeNot.
** - URL: https://www.mindsumo.com/contests/analyze-clicks-and-bounce-rates-from
**        \ -online-couponers?utm_source=computer_fwd&utm_campaign=academicdepts
**        \ &utm_medium=email&utm_campaign=website&utm_source=sendgrid.com&utm_m
**        \ edium=email
**
** @author: Cam Cecil <cam@camcecil.com>
**
*/

import java.io.*;
import java.util.regex.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class RMNLogParser {
    private final int MAX_ERRORS = 20;
    private int       total_errors = 0;

    /* Declarations */
    private String fileName;

    /* Initializers */
    private List<String> logLines = new ArrayList<String>();
    private HashMap<Integer, Integer> exitsByMinute = new HashMap<Integer, Integer>(); // Min, Exits

    /*
     * Constructor
     *
     * @params String Path to the log file to parse.
     */
    public RMNLogParser(String fileName) {
        this.fileName = fileName;
        BufferedReader buffReader;

        try {
            String currentLine;

            FileReader fileReader = new FileReader(fileName);
            buffReader = new BufferedReader(fileReader);

            while((currentLine = buffReader.readLine()) != null) {
                logLines.add(currentLine);
            }

        }
        catch(IOException e) {
            displayError("Log file does not exist.");
        }
        catch(Exception e) {
            displayError("There was an error during initialization:\n" + e.getMessage() );
        }
    }

    /*
     * Parse the different sections of the log's lines using a [very ugly] REGEX
     *
     * @params int index of the line to parse (beginning at 0)
     */
    public ArrayList parseLogLine(int index) {
        String data = logLines.get(index);
        ArrayList<String> result = new ArrayList<String>();

    /*==========================================================================
     * Split values:
     * 0: IP
     * 1: Datetime string
     * 2: Request path
     * 3: Status Code
     * 4: Response size (minus header)
     * 5: Referrer
     * 6: User-Agent
    **==========================================================================*/

        String apacheLogRegex = "^(\\-?\\s?|(?:(?:\\w*|\\d{1,3})\\.*\\,?\\s?)*)(?:[^\\s]*) (?:[^\\s]*) (?:\\[([^\\]]*)\\]) (?:\\\"\\w* (\\/*(?:[^\\s]*)) [^\\\"]*\\\") ([^\\s]*) ([^\\s]*) (?:\\\"([^\\\"]*)\\\") (?:\\\"([^\\\"]*)\\\")";
        Pattern p = Pattern.compile(apacheLogRegex);
        Matcher m = p.matcher(data);

        if(m.find()) {
            for(int i = 1; i <= m.groupCount(); i++) {
                result.add(m.group(i));
            }
        } else {
            // If the REGEX doesn't pass for this very specific application,
            // we should exit.
            displayError("Log index #" + index +" format is incompatible.\n\t" + data);
        }

        return result;
    }

    /*
     * Extracts the seconds from the formatted datestring
     *
     * @params int line number to extract second from
     *
     * @return String Datestrings Minute field in 00 format
     */
    public String getMinuteFromDateString(int line) {
        String result = "";

        try {
            String dateStringRegex = "\\:\\d{2}\\:(\\d{2})\\:\\d{2}";
            Pattern p = Pattern.compile(dateStringRegex);
            Matcher m = p.matcher( parseLogLine(line).get(1).toString() );

            if(m.find()) {
                result = m.group(1);
            } else {
                result = "404";   // Corrupt Datetime strings will show up as the 404th
                // minute. My first easter egg?
            }
        }
        catch(IndexOutOfBoundsException e) {
            displayError("Invalid line #: " + e.getMessage());
        }

        return result;
    }

    /*
     * Organizes exits by the minute into a HashMap
     */
    public void updateExitsPerMinute() {
        int minute;

        String outRegex = "\\/out\\/(.+)";
        Pattern p = Pattern.compile(outRegex);

        for(int i = 0; i < this.logLines.size(); i++) {
            try {
                Matcher m = p.matcher( parseLogLine(i).get(2).toString() );

                if(m.find()) {
                    minute = Integer.parseInt(getMinuteFromDateString(i));

                    if(exitsByMinute.containsKey(minute))
                        exitsByMinute.put(minute, exitsByMinute.get(minute)+1);
                    else
                        exitsByMinute.put(minute, 1);
                }
            }
            catch(Exception e) {
                displayError("Internal Error with the Log Listing");
            }
        }
    }

    /*
     * Prints Out-going click statistics to the console
     */
    public void displayExitStats() {

        int total, min, max;
        double mean, median, standard_deviation;

        int[] valArray = new int[this.exitsByMinute.size()];
        int current = 0;

        total = 0;

        // Add each minute's amount of clicks to an array
        for(Map.Entry<Integer, Integer> minute : exitsByMinute.entrySet()) {
            valArray[current] = minute.getValue();
            total += valArray[current++];                 // also increments current
        }

        // Sort the array for calculations
        Arrays.sort(valArray);

        // CALCULATIONS ====================================================================

        if(valArray.length > 0) {
            /* MIN, MAX AND MEAN */
            min = valArray[0];
            max = valArray[valArray.length-1];
            mean = total / valArray.length;

            /* MEDIAN */
            if(valArray.length % 2 == 0) {
                // If it's even, the median = the mean of the two middle numbers
                median = ( valArray[valArray.length / 2]
                        + valArray[(valArray.length / 2) - 1] ) / 2;
            } else {
                median = valArray[(int)valArray.length / 2];
            }

            /* STANDARD DEVIATION */
            standard_deviation = 0.0;

            for(int val : valArray) {
                standard_deviation += Math.pow((val - mean), 2);
            }

            standard_deviation /= (valArray.length-1);

            standard_deviation = Math.sqrt(standard_deviation);

            // OUTPUT =====================================================================

            System.out.printf("%-50s\n", "Out-going Clicks-Per-Minute Statistics:");
            System.out.println("--------------------------------------------------------");
            System.out.printf("%20s: %-30d\n",   "Total",              total);
            System.out.printf("%20s: %-30d\n",   "Minimum",            min);
            System.out.printf("%20s: %-30d\n",   "Maximum",            max);
            System.out.printf("%20s: %-30.1f\n", "Mean",               mean);
            System.out.printf("%20s: %-30.1f\n", "Median",             median);
            System.out.printf("%20s: %-30.1f\n", "Standard Deviation", standard_deviation);
        }
        else {
            displayError("Something went wrong");
        }
    }

    /*
     * Handle errors
     */
    private void displayError(String error) {
        // Print errors to the console for brevities sake.
        total_errors++;
        System.out.println("[!] ERROR #" + total_errors + ": " + error);

        if(total_errors >= MAX_ERRORS)
            System.exit(0);
    }
}
