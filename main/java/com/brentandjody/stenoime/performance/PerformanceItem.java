package com.brentandjody.stenoime.performance;

import java.util.Date;

/**
 * Created by brent on 10/02/14.
 */
public class PerformanceItem {

    private Date when;
    private double minutes, max_speed;
    private int letters, strokes, corrections;

    public PerformanceItem() {
        when = new Date();
        minutes = 0d;
        letters = 0;
        max_speed = 0d;
        corrections = 0;
        strokes = 0;
    }

    public PerformanceItem(Date when, double minutes, int letters, double max_speed, int corrections, int strokes) {
        this.when = when;
        this.minutes = minutes;
        this.letters = letters;
        this.max_speed = max_speed;
        this.corrections = corrections;
        this.strokes = strokes;
    }
    
    public Date when() {return when;}
    public double minutes() {return minutes;}
    public int letters() {return letters;}
    public double max_speed() {return max_speed;}
    public int corrections() {return corrections;}
    public int strokes() {return strokes;}

    public void addStroke() {strokes++;}
    public void addCorrection() {corrections++;}
    public void addLetters(int how_many) {letters+=how_many;}
    public void setMinutes(double mins) {minutes = mins;}

}
