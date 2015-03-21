package com.camcecil.rmnlogstats;

/*
** App.java
**
** Driver program for the RetailMeNot Log Analysis Tool.
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

public class AppCli
{
    public static void main( String[] args )
    {
        RMNLogParser logParser = new RMNLogParser("./logs/rmn_weblog_sample_50k.log");

        logParser.updateExitsPerMinute();
        logParser.displayExitStats();
    }
}
