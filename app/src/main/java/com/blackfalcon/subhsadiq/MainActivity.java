package com.blackfalcon.subhsadiq;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private static final int GREEN = Color.parseColor("#2ECC71");
    private static final int AMBER = Color.parseColor("#FFB300");
    private static final int CARD = Color.parseColor("#12122A");
    private static final int GRAY = Color.parseColor("#A0A0C0");
    private static final String NONE = "\u2014";

    // settings
    private int methodIdx = 0;
    private double customAngle = 18.0, kStartOff = Dawn.DEFAULT_KAZIB_START, kEndOff = Dawn.DEFAULT_KAZIB_END;
    private int dayOffset = 0;

    // location
    private LocationManager lm;
    private SharedPreferences sp;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeout;
    private double lat, lon;
    private boolean hasLoc = false, searching = false, wasSearching = false;
    private int locStatus = 0; // 0 none,1 searching,2 live,3 saved,4 gps off,5 no permission,6 no fix

    // views
    private TextView dateTv, locTv, stateTv, countTv, sadiqTime, sadiqSub, kazibTime, kazibSub, sunriseTv,
            methodTv, angleTv, startTv, endTv, warnTv;
    private TimelineView timeline;

    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 20000); }
    };

    // ------------------------------------------------------------- lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sp = getSharedPreferences("dawn", MODE_PRIVATE);
        methodIdx = sp.getInt("method", 0);
        customAngle = sp.getFloat("custom", 18f);
        kStartOff = sp.getFloat("kstart", (float) Dawn.DEFAULT_KAZIB_START);
        kEndOff = sp.getFloat("kend", (float) Dawn.DEFAULT_KAZIB_END);

        buildUi();

        if (sp.contains("lat") && sp.contains("lon")) {
            lat = Double.longBitsToDouble(sp.getLong("lat", 0));
            lon = Double.longBitsToDouble(sp.getLong("lon", 0));
            hasLoc = true;
            locStatus = 3;
        }
        if (hasPerm()) {
            refreshLocation();
        } else {
            locStatus = 5;
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
        if (wasSearching) { wasSearching = false; refreshLocation(); }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        wasSearching = searching;
        stopLocation();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (hasPerm()) refreshLocation(); else { locStatus = 5; refresh(); }
    }

    private boolean hasPerm() {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // -------------------------------------------------------------------- UI
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#050510"));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(10));

        // title
        TextView title = tv(23, Color.WHITE, true);
        title.setText("\uD83C\uDF05 Subh Sadiq & Subh Kazib");
        TextView urdu = tv(18, GRAY, false);
        urdu.setText("\u0635\u0628\u062D \u0635\u0627\u062F\u0642 \u0627\u0648\u0631 \u0635\u0628\u062D \u06A9\u0627\u0630\u0628");
        box.addView(title);
        box.addView(urdu);

        // date row
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        dateRow.setPadding(0, dp(10), 0, dp(4));
        TextView prev = smallBtn("\u25C0");
        TextView next = smallBtn("\u25B6");
        prev.setOnClickListener(v -> { dayOffset--; refresh(); });
        next.setOnClickListener(v -> { dayOffset++; refresh(); });
        dateTv = tv(16, Color.WHITE, true);
        dateTv.setGravity(Gravity.CENTER);
        dateTv.setOnClickListener(v -> { dayOffset = 0; refresh(); });
        dateRow.addView(prev);
        dateRow.addView(dateTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        dateRow.addView(next);
        box.addView(dateRow);

        // location line + refresh
        LinearLayout locRow = new LinearLayout(this);
        locRow.setGravity(Gravity.CENTER_VERTICAL);
        locTv = tv(12, GRAY, false);
        TextView refresh = pill("\u21BB Refresh location", GREEN);
        refresh.setTextSize(13);
        refresh.setPadding(dp(12), dp(8), dp(12), dp(8));
        refresh.setOnClickListener(v -> refreshLocation());
        locRow.addView(locTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        locRow.addView(refresh);
        box.addView(locRow);

        // state card
        LinearLayout stateCard = card(Color.parseColor("#3A3A60"));
        stateTv = tv(17, Color.WHITE, true);
        countTv = tv(14, GRAY, false);
        countTv.setPadding(0, dp(4), 0, 0);
        stateCard.addView(stateTv);
        stateCard.addView(countTv);
        box.addView(stateCard, cardLp(10));

        // Subh Sadiq card
        LinearLayout sadiqCard = card(GREEN);
        TextView st = tv(18, GREEN, true);
        st.setText("Subh Sadiq \u2022 True dawn");
        TextView su = tv(16, GRAY, false);
        su.setText("\u0635\u0628\u062D \u0635\u0627\u062F\u0642  \u2022  \u0627\u0644\u0641\u062C\u0631 \u0627\u0644\u0635\u0627\u062F\u0642");
        sadiqTime = tv(34, Color.WHITE, true);
        sadiqSub = tv(13, GRAY, false);
        sunriseTv = tv(13, GRAY, false);
        TextView sd = tv(14, Color.WHITE, false);
        sd.setText("A thin white light that spreads sideways along the horizon and keeps getting brighter. "
                + "Sehri ends and Fajr time begins; it lasts until sunrise.");
        sd.setPadding(0, dp(8), 0, dp(8));
        sadiqCard.addView(st);
        sadiqCard.addView(su);
        sadiqCard.addView(sadiqTime);
        sadiqCard.addView(sadiqSub);
        sadiqCard.addView(sunriseTv);
        sadiqCard.addView(sd);
        sadiqCard.addView(new DawnArt(this, true), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(120)));
        box.addView(sadiqCard, cardLp(12));

        // Subh Kazib card
        LinearLayout kazibCard = card(AMBER);
        TextView kt = tv(18, AMBER, true);
        kt.setText("Subh Kazib \u2022 False dawn");
        TextView ku = tv(16, GRAY, false);
        ku.setText("\u0635\u0628\u062D \u06A9\u0627\u0630\u0628  \u2022  \u0627\u0644\u0641\u062C\u0631 \u0627\u0644\u0643\u0627\u0630\u0628");
        kazibTime = tv(30, Color.WHITE, true);
        kazibSub = tv(13, GRAY, false);
        TextView kd = tv(14, Color.WHITE, false);
        kd.setText("A faint column of light rising straight up from the eastern horizon (the zodiacal light, "
                + "\"wolf's tail\"). Then darkness returns before the true dawn. It does not end Sehri, and Fajr is not yet due.");
        kd.setPadding(0, dp(8), 0, dp(8));
        kazibCard.addView(kt);
        kazibCard.addView(ku);
        kazibCard.addView(kazibTime);
        kazibCard.addView(kazibSub);
        kazibCard.addView(kd);
        kazibCard.addView(new DawnArt(this, false), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(120)));
        box.addView(kazibCard, cardLp(12));

        // timeline
        LinearLayout tlCard = card(Color.parseColor("#3A3A60"));
        TextView tlt = tv(15, Color.WHITE, true);
        tlt.setText("Timeline");
        timeline = new TimelineView(this);
        tlCard.addView(tlt);
        tlCard.addView(timeline, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));
        box.addView(tlCard, cardLp(12));

        warnTv = tv(13, AMBER, false);
        warnTv.setPadding(dp(4), dp(8), dp(4), 0);
        box.addView(warnTv);

        // settings
        LinearLayout setCard = card(Color.parseColor("#3A3A60"));
        TextView sh = tv(15, Color.WHITE, true);
        sh.setText("Settings");
        TextView ml = tv(12, GRAY, false);
        ml.setText("Subh Sadiq (Fajr) angle \u2014 choose the authority you follow");
        ml.setPadding(0, dp(6), 0, dp(4));
        methodTv = pill("", GREEN);
        methodTv.setTextSize(14);
        methodTv.setOnClickListener(v -> chooseMethod());
        setCard.addView(sh);
        setCard.addView(ml);
        setCard.addView(methodTv);

        angleTv = tv(16, Color.WHITE, true);
        setCard.addView(stepper("Fajr angle (below horizon)", angleTv,
                () -> { switchToCustom(); customAngle = Math.max(10, customAngle - 0.5); saveAndRefresh(); },
                () -> { switchToCustom(); customAngle = Math.min(22, customAngle + 0.5); saveAndRefresh(); }));
        startTv = tv(16, Color.WHITE, true);
        setCard.addView(stepper("Subh Kazib appears: Fajr angle +", startTv,
                () -> { kStartOff = Math.max(kEndOff + 0.5, kStartOff - 0.5); saveAndRefresh(); },
                () -> { kStartOff = Math.min(14, kStartOff + 0.5); saveAndRefresh(); }));
        endTv = tv(16, Color.WHITE, true);
        setCard.addView(stepper("Subh Kazib fades: Fajr angle +", endTv,
                () -> { kEndOff = Math.max(0.5, kEndOff - 0.5); saveAndRefresh(); },
                () -> { kEndOff = Math.min(kStartOff - 0.5, kEndOff + 0.5); saveAndRefresh(); }));
        box.addView(setCard, cardLp(12));

        // sources
        TextView src = tv(12, GRAY, false);
        src.setText("Sources: Qur'an 2:187. Sahih Muslim 1094 (Samura b. Jundub). Ibn Qudama, al-Mughni 1/232 "
                + "(also quoting Ibn 'Abbas on the two dawns). Ibn 'Uthaymeen, al-Sharh al-Mumti' 2/107-108: "
                + "the rulings are tied to the second (true) dawn. Sun position: praytimes.org method (H. Zarrabi-Zadeh); "
                + "field observation: Qassim University study (2014) found about 16\u00B0.\n\n"
                + "Subh Sadiq times use the angle you choose. The Subh Kazib window is only an estimate: "
                + "it is something you see in a dark sky, and no single angle is agreed on, so adjust it after your own observation. "
                + "Times are cut down to the whole minute. Fasting and prayer depend on actually seeing the true dawn, "
                + "so use this as a guide and follow your local scholars and mosque timetable.");
        src.setPadding(dp(4), dp(14), dp(4), dp(8));
        box.addView(src);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        TextView credit = tv(14, Color.WHITE, true);
        credit.setText("By: Black Falcon \uD83E\uDD85");
        credit.setGravity(Gravity.RIGHT);
        credit.setPadding(dp(16), dp(6), dp(16), dp(10));

        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(credit);
        setContentView(root);
    }

    private TextView tv(int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int stroke, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke(dp(2), stroke);
        g.setCornerRadius(dp(r));
        return g;
    }

    private LinearLayout card(int stroke) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(round(CARD, stroke, 18));
        return c;
    }

    private LinearLayout.LayoutParams cardLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(topDp), 0, 0);
        return lp;
    }

    private TextView pill(String s, int color) {
        TextView b = tv(15, color, true);
        b.setText(s);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(11), dp(12), dp(11));
        b.setBackground(round(Color.parseColor("#0D1F14"), color, 24));
        b.setClickable(true);
        return b;
    }

    private TextView smallBtn(String s) {
        TextView b = tv(18, GREEN, true);
        b.setText(s);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(8), dp(16), dp(8));
        b.setClickable(true);
        return b;
    }

    private View stepper(String label, TextView value, final Runnable minus, final Runnable plus) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, 0);
        TextView l = tv(12, GRAY, false);
        l.setText(label);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView m = smallBtn("\u2212");
        TextView p = smallBtn("+");
        m.setOnClickListener(v -> minus.run());
        p.setOnClickListener(v -> plus.run());
        value.setGravity(Gravity.CENTER);
        row.addView(m);
        row.addView(value, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(p);
        box.addView(l);
        box.addView(row);
        return box;
    }

    // ------------------------------------------------------------- settings
    private double fajrAngle() {
        return methodIdx == Dawn.CUSTOM ? customAngle : Dawn.METHOD_ANGLES[methodIdx];
    }

    private void switchToCustom() {
        if (methodIdx != Dawn.CUSTOM) { customAngle = Dawn.METHOD_ANGLES[methodIdx]; methodIdx = Dawn.CUSTOM; }
    }

    private void chooseMethod() {
        String[] items = new String[Dawn.METHOD_NAMES.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = i == Dawn.CUSTOM ? Dawn.METHOD_NAMES[i]
                    : String.format(Locale.US, "%s (%.1f\u00B0)", Dawn.METHOD_NAMES[i], Dawn.METHOD_ANGLES[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Subh Sadiq (Fajr) method")
                .setItems(items, (d, which) -> {
                    if (which == Dawn.CUSTOM && methodIdx != Dawn.CUSTOM) customAngle = fajrAngle();
                    methodIdx = which;
                    saveAndRefresh();
                })
                .show();
    }

    private void saveAndRefresh() {
        sp.edit().putInt("method", methodIdx).putFloat("custom", (float) customAngle)
                .putFloat("kstart", (float) kStartOff).putFloat("kend", (float) kEndOff).apply();
        refresh();
    }

    // -------------------------------------------------------------- display
    private boolean is24() { return android.text.format.DateFormat.is24HourFormat(this); }

    private String fmt(long ms) {
        if (ms == Dawn.NONE) return NONE;
        ms = ms - (ms % 60000L); // cut to the whole minute
        SimpleDateFormat f = new SimpleDateFormat(is24() ? "HH:mm" : "h:mm a", Locale.US);
        return f.format(new Date(ms));
    }

    private long[] eventsFor(Calendar c) {
        return Dawn.day(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
                lat, lon, fajrAngle(), kStartOff, kEndOff);
    }

    private String until(long ms) {
        long diff = ms - System.currentTimeMillis();
        if (diff < 60000L) return "less than a minute";
        long h = diff / 3600000L, m = (diff % 3600000L) / 60000L;
        return h > 0 ? h + " h " + m + " min" : m + " min";
    }

    private void refresh() {
        // location status line
        switch (locStatus) {
            case 1: locTv.setText("Finding your location\u2026"); break;
            case 2: locTv.setText(String.format(Locale.US, "GPS %.3f\u00B0, %.3f\u00B0 \u2022 works offline", lat, lon)); break;
            case 3: locTv.setText(String.format(Locale.US, "Saved %.3f\u00B0, %.3f\u00B0 \u2022 tap Refresh", lat, lon)); break;
            case 4: locTv.setText("GPS is off \u2014 turn it on and tap Refresh"); break;
            case 5: locTv.setText("Location permission is needed"); break;
            case 6: locTv.setText(hasLoc ? "No GPS fix \u2014 using saved location" : "No GPS fix yet \u2014 go outside and tap Refresh"); break;
            default: locTv.setText("Tap Refresh to get your location");
        }

        // settings text
        methodTv.setText(methodIdx == Dawn.CUSTOM ? "Custom angle" : Dawn.METHOD_NAMES[methodIdx]);
        angleTv.setText(String.format(Locale.US, "%.1f\u00B0", fajrAngle()));
        startTv.setText(String.format(Locale.US, "%.1f\u00B0  (Sun %.1f\u00B0 below)", kStartOff, fajrAngle() + kStartOff));
        endTv.setText(String.format(Locale.US, "%.1f\u00B0  (Sun %.1f\u00B0 below)", kEndOff, fajrAngle() + kEndOff));

        Calendar shown = Calendar.getInstance();
        shown.add(Calendar.DAY_OF_YEAR, dayOffset);
        dateTv.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.US).format(shown.getTime())
                + (dayOffset == 0 ? "  (today)" : ""));

        if (!hasLoc) {
            stateTv.setText("Waiting for your location");
            countTv.setText("Dawn times need your place on Earth.");
            sadiqTime.setText(NONE); sadiqSub.setText(""); sunriseTv.setText("");
            kazibTime.setText(NONE); kazibSub.setText("");
            timeline.set(null, null, -1);
            warnTv.setText("");
            return;
        }

        // the selected day
        long[] ev = eventsFor(shown);
        double fa = fajrAngle();
        if (ev[2] == Dawn.NONE) {
            sadiqTime.setText(NONE);
            sadiqSub.setText("");
            sunriseTv.setText("");
            kazibTime.setText(NONE);
            kazibSub.setText("");
            warnTv.setText("At this latitude the Sun does not go deep enough on this date, so there is no true dawn at this angle "
                    + "(twilight lasts all night). Follow your local scholars' ruling for high latitudes.");
            timeline.set(null, null, -1);
        } else {
            sadiqTime.setText(fmt(ev[2]));
            sadiqSub.setText(String.format(Locale.US, "Sun about %.1f\u00B0 below the horizon \u2022 Sehri ends, Fajr begins", fa));
            sunriseTv.setText("Fajr time ends at sunrise: " + fmt(ev[3]));
            if (ev[0] == Dawn.NONE || ev[1] == Dawn.NONE) {
                kazibTime.setText(NONE);
                kazibSub.setText("Not calculable on this date at your latitude");
                timeline.set(null, null, -1);
            } else {
                kazibTime.setText(fmt(ev[0]) + " \u2013 " + fmt(ev[1]));
                kazibSub.setText(String.format(Locale.US, "Estimate: Sun %.1f\u00B0 \u2192 %.1f\u00B0 below the horizon", fa + kStartOff, fa + kEndOff));
                long now = System.currentTimeMillis();
                timeline.set(ev, new String[]{fmt(ev[0]), fmt(ev[1]), fmt(ev[2]), fmt(ev[3])}, dayOffset == 0 ? now : -1);
            }
            warnTv.setText("");
        }

        // what is happening right now (uses today + tomorrow)
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        long[] t = eventsFor(today);
        long[] n = eventsFor(tomorrow);
        long now = System.currentTimeMillis();
        if (t[2] == Dawn.NONE) {
            stateTv.setText("No true dawn at this angle today");
            stateTv.setTextColor(AMBER);
            countTv.setText("");
            return;
        }
        long nextSadiq = now < t[2] ? t[2] : n[2];
        if (now < t[0]) {
            stateTv.setText("\uD83C\uDF19 Night \u2014 the sky is dark");
            stateTv.setTextColor(Color.WHITE);
        } else if (t[0] != Dawn.NONE && t[1] != Dawn.NONE && now < t[1]) {
            stateTv.setText("\uD83C\uDF2B\uFE0F Subh Kazib window \u2014 faint vertical light");
            stateTv.setTextColor(AMBER);
        } else if (now < t[2]) {
            stateTv.setText("\u23F3 Dark gap \u2014 Subh Sadiq has not yet appeared");
            stateTv.setTextColor(Color.WHITE);
        } else if (now < t[3]) {
            stateTv.setText("\uD83C\uDF05 Subh Sadiq has appeared \u2014 Sehri ended, Fajr time is in");
            stateTv.setTextColor(GREEN);
        } else {
            stateTv.setText("\u2600\uFE0F After sunrise");
            stateTv.setTextColor(Color.WHITE);
        }
        countTv.setText(nextSadiq == Dawn.NONE ? "" : "Next Subh Sadiq: " + fmt(nextSadiq) + "  (in " + until(nextSadiq) + ")");
    }

    // ------------------------------------------------------------- location
    private void refreshLocation() {
        if (!hasPerm()) {
            locStatus = 5; refresh();
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            return;
        }
        stopLocation();
        boolean any = false;
        try {
            for (String pr : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                if (lm.isProviderEnabled(pr)) { lm.requestLocationUpdates(pr, 0, 0, this); any = true; }
            }
            if (!hasLoc) {
                Location last = null;
                for (String pr : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                    Location l = lm.getLastKnownLocation(pr);
                    if (l != null && (last == null || l.getTime() > last.getTime())) last = l;
                }
                if (last != null) { lat = last.getLatitude(); lon = last.getLongitude(); hasLoc = true; }
            }
        } catch (SecurityException ignored) { }

        if (!any) { locStatus = 4; refresh(); return; }
        searching = true;
        locStatus = 1;
        refresh();
        timeout = () -> {
            if (!searching) return;
            stopLocation();
            locStatus = 6;
            refresh();
        };
        handler.postDelayed(timeout, 25000);
    }

    private void stopLocation() {
        searching = false;
        if (timeout != null) { handler.removeCallbacks(timeout); timeout = null; }
        try { lm.removeUpdates(this); } catch (SecurityException ignored) { }
    }

    @Override
    public void onLocationChanged(Location l) {
        if (!searching) return;
        stopLocation();
        lat = l.getLatitude();
        lon = l.getLongitude();
        hasLoc = true;
        sp.edit().putLong("lat", Double.doubleToRawLongBits(lat))
                .putLong("lon", Double.doubleToRawLongBits(lon)).apply();
        locStatus = 2;
        refresh();
    }

    @Override public void onStatusChanged(String p, int s, Bundle e) { }
    @Override public void onProviderEnabled(String p) { }
    @Override public void onProviderDisabled(String p) { }
}
