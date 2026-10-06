package com.blackfalcon.subhsadiq;

import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Dawn times from the Sun's position (praytimes.org style maths). Works fully offline. */
public final class Dawn {

    private Dawn() { }

    public static final long NONE = Long.MIN_VALUE;

    public static final String[] METHOD_NAMES = {
            "University of Islamic Sciences, Karachi",
            "Muslim World League",
            "Umm al-Qura University, Makkah",
            "Egyptian General Authority of Survey",
            "ISNA (North America)",
            "Institute of Geophysics, Univ. of Tehran",
            "Qassim University field study (observation)",
            "Custom angle"};
    public static final double[] METHOD_ANGLES = {18.0, 18.0, 18.5, 19.5, 15.0, 17.7, 16.0, 18.0};
    public static final int CUSTOM = 7;

    public static final double DEFAULT_KAZIB_START = 6.0; // degrees deeper than the Subh Sadiq angle
    public static final double DEFAULT_KAZIB_END = 1.0;   // degrees deeper than the Subh Sadiq angle

    private static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }

    /** {declination in degrees, equation of time in hours} for a Julian day. */
    static double[] sun(double jd) {
        double d = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * d, 360);
        double q = fix(280.459 + 0.98564736 * d, 360);
        double L = fix(q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g)), 360);
        double e = 23.439 - 0.00000036 * d;
        double ra = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(L)),
                Math.cos(Math.toRadians(L)))) / 15.0;
        ra = fix(ra, 24);
        double eqt = q / 15.0 - ra;
        while (eqt > 12) eqt -= 24;
        while (eqt < -12) eqt += 24;
        double decl = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(L))));
        return new double[]{decl, eqt};
    }

    /** UTC hours after UTC midnight (jd0) when the Sun is `alpha` degrees below the horizon; NaN if it never is. */
    static double timeForAngle(double jd0, double lat, double lon, double alpha, boolean morning) {
        double t = 12 - lon / 15.0 + (morning ? -6 : 6);
        double phi = Math.toRadians(lat), a = Math.toRadians(alpha);
        for (int i = 0; i < 5; i++) {
            double[] s = sun(jd0 + t / 24.0);
            double dh = 12 - lon / 15.0 - s[1];
            double dec = Math.toRadians(s[0]);
            double arg = (-Math.sin(a) - Math.sin(phi) * Math.sin(dec)) / (Math.cos(phi) * Math.cos(dec));
            if (arg < -1 || arg > 1) return Double.NaN;
            double T = Math.toDegrees(Math.acos(arg)) / 15.0;
            t = dh + (morning ? -T : T);
        }
        return t;
    }

    /** Sun altitude in degrees at `tHours` UTC hours after UTC midnight (jd0). */
    static double altitude(double jd0, double lat, double lon, double tHours) {
        double[] s = sun(jd0 + tHours / 24.0);
        double H = Math.toRadians((tHours + lon / 15.0 + s[1] - 12.0) * 15.0);
        double phi = Math.toRadians(lat), dec = Math.toRadians(s[0]);
        return Math.toDegrees(Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H)));
    }

    static long utcMidnight(int y, int m0, int d) {
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(y, m0, d, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /**
     * Morning events for a calendar date (the date in the phone's own time zone):
     * {Subh Kazib starts, Subh Kazib ends, Subh Sadiq starts, sunrise} as epoch millis, or NONE.
     */
    public static long[] day(int y, int m0, int d, double lat, double lon,
                             double fajrAngle, double kazibStartOff, double kazibEndOff) {
        long base = utcMidnight(y, m0, d);
        double jd0 = base / 86400000.0 + 2440587.5;
        double[] angles = {fajrAngle + kazibStartOff, fajrAngle + kazibEndOff, fajrAngle, 0.833};
        long[] out = new long[4];
        for (int i = 0; i < 4; i++) {
            double h = timeForAngle(jd0, lat, lon, angles[i], true);
            out[i] = Double.isNaN(h) ? NONE : base + Math.round(h * 3600000.0);
        }
        return out;
    }
}
