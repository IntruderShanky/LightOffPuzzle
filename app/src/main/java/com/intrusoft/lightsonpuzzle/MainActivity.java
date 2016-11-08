package com.intrusoft.lightsonpuzzle;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    LinearLayout rootLayout;
    Display display;
    View lightView[][];
    int lights[][];
    int backup[][];
    TextView step, time;
    Button reset, hint, home;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    SharedPreferences.Editor editor;
    SharedPreferences preferences;
    long updatedTime = 0L;
    int s = 0;
    boolean isHint = false;
    List<List<Integer>> lookUpTable = new ArrayList<>();
    List<Integer> clicked = new ArrayList<>();
    List<String> posPro = new ArrayList<>();
    DatabaseHelper helper;
    GoogleApiClient googleApiClient;
    private static int RC_SIGN_IN = 9001;
    private boolean mResolvingConnectionFailure = false;
    public boolean mAutoStartSignInflow = true;
    private boolean mSignInClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.click);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
        preferences = getSharedPreferences("PLAYER_INFO", MODE_PRIVATE);
        editor = preferences.edit();
        final int n = getIntent().getIntExtra("n", 2);
        createView(n);
        lights = createRandomGame(n);
        setView(n);
        helper = new DatabaseHelper(MainActivity.this);
        Runnable updateTimerThread = new Runnable() {
            public void run() {
                timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                updatedTime = timeSwapBuff + timeInMilliseconds;
                int secs = (int) (updatedTime / 1000);
                int mins = secs / 60;
                secs = secs % 60;
                int hour = mins / 60;
                time.setText(String.format("%s:%s:%s", String.format("%02d", hour), String.format("%02d", mins), String.format("%02d", secs)));
                customHandler.postDelayed(this, 0);
            }
        };
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int finalI = i;
                final int finalJ = j;
                lightView[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mediaPlayer.start();
                        s++;
                        lights = setValue(finalI, finalJ, n, lights);
                        setView(n);
                        if (isHint) {
                            if (finalI == 0) {
                                clicked.add(finalJ);
                            }
                        }
                    }
                });
            }
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int[][] sol = new int[n][n];
                for (int i = 0; i < n; i++) {
                    List<Integer> value = new ArrayList<>();
                    for (int p = 0; p < n; p++)
                        for (int q = 0; q < n; q++)
                            sol[p][q] = 0;
                    sol = setValue(0, i, n, sol);
                    for (int j = 0; j < n; j++)
                        for (int k = 0; k < n; k++)
                            if (sol[j][k] == 1)
                                if (j != (n - 1))
                                    sol = setValue(j + 1, k, n, sol);
                    String end = "";
                    for (int m = 0; m < n; m++) {
                        end += String.valueOf(sol[n - 1][m]);
                    }
                    value.add(i);
                    if (!posPro.contains(end)) {
                        lookUpTable.add(value);
                        posPro.add(end);
                    }
                }
                int s = 0;
                while (lookUpTable.size() > s) {
                    for (int i = s + 1; i < lookUpTable.size(); i++) {
                        String next = getXOR(posPro.get(s), posPro.get(i), n);
                        List<Integer> nextList = new ArrayList<>();
                        nextList.addAll(lookUpTable.get(s));
                        for (int f = 0; f < lookUpTable.get(i).size(); f++) {
                            if (!nextList.contains(lookUpTable.get(i).get(f))) {
                                nextList.add(lookUpTable.get(i).get(f));
                            }
                        }
                        Collections.sort(nextList);
                        if (!posPro.contains(next)) {
                            posPro.add(next);
                            lookUpTable.add(nextList);
                        }
                    }
                    s++;
                }
                backup = new int[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++)
                        backup[i][j] = lights[i][j];
                }
            }
        });
        thread.start();
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++)
                        lights[i][j] = backup[i][j];
                    setView(n);
                }
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
            }
        });
        hint.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        isHint = true;
                                        int[][] grid = new int[n][n];
                                        for (int i = 0; i < n; i++) {
                                            for (int j = 0; j < n; j++) {
                                                grid[i][j] = lights[i][j];
                                            }
                                        }
                                        if (isGameEasy(n)) {
                                            for (int i = 0; i < n - 1; i++) {
                                                int j;
                                                for (j = 0; j < n; j++) {
                                                    if (lights[i][j] == 1) {
                                                        lightView[i + 1][j].setBackgroundResource(R.drawable.light_hint);
                                                        break;
                                                    }
                                                }
                                                if (j < n) {
                                                    break;
                                                }
                                            }
                                        } else {
                                            difficultHint(n, grid);
                                        }
                                    }

                                }

        );
    }

    public void difficultHint(int n, int[][] grid) {
        for (int j = 0; j < n; j++)
            for (int k = 0; k < n; k++)
                if (grid[j][k] == 1)
                    if (j != (n - 1))
                        grid = setValue(j + 1, k, n, grid);
        String last = "";
        for (int k = 0; k < n; k++) {
            last += String.valueOf(grid[n - 1][k]);
        }
        if (posPro.contains(last)) {
            int i;
            for (i = 0; i < lookUpTable.get(posPro.indexOf(last)).size(); i++) {
                if (!clicked.contains(lookUpTable.get(posPro.indexOf(last)).get(i))) {
                    lightView[0][lookUpTable.get(posPro.indexOf(last)).get(i)]
                            .setBackgroundResource(R.drawable.light_hint);
                    break;
                }
            }
            if (i == lookUpTable.get(posPro.indexOf(last)).size()) {
                clicked.clear();
                lightView[0][lookUpTable.get(posPro.indexOf(last)).get(0)]
                        .setBackgroundResource(R.drawable.light_hint);
            }
        }
    }

    public boolean isGameEasy(int n) {
        int[][] grid = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                grid[i][j] = lights[i][j];
            }
        }
        for (int j = 0; j < n; j++)
            for (int k = 0; k < n; k++)
                if (grid[j][k] == 1)
                    if (j != (n - 1))
                        grid = setValue(j + 1, k, n, grid);
        for (int j = 0; j < n; j++) {
            if (grid[n - 1][j] == 1) {
                return false;
            }
        }
        return true;
    }

    public String getXOR(String one, String two, int n) {
        String result = "";
        if (two.equals(null)) {
            return one;
        } else {
            for (int i = 0; i < n; i++) {
                if (one.charAt(i) == two.charAt(i)) {
                    result += "0";
                } else {
                    result += "1";
                }
            }
            return result;
        }
    }

    private int[][] setValue(int i, int j, int n, int[][] grid) {
        grid = toogleValue(i, j, grid);
        if (i != 0) {
            grid = toogleValue(i - 1, j, grid);
        }
        if (i != n - 1) {
            grid = toogleValue(i + 1, j, grid);
        }
        if (j != 0) {
            grid = toogleValue(i, j - 1, grid);
        }
        if (j != n - 1) {
            grid = toogleValue(i, j + 1, grid);
        }
        return grid;
    }

    public int[][] toogleValue(int i, int j, int[][] grid) {
        if (grid[i][j] == 1) {
            grid[i][j] = 0;
        } else {
            grid[i][j] = 1;
        }
        return grid;
    }

    private void setView(final int n) {
        step.setText("Steps:" + s);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (lights[i][j] == 0) lightView[i][j].setBackgroundResource(R.drawable.light_on);
                else lightView[i][j].setBackgroundResource(R.drawable.light_off);
            }
        }
        if (checkWin(n)) {
            String s = "";
            switch (n) {
                case 2:
                    s = getResources().getString(R.string.level_1);
                    break;
                case 3:
                    s = getResources().getString(R.string.level_2);
                    break;
                case 6:
                    s = getResources().getString(R.string.level_3);
                    break;
                case 7:
                    s = getResources().getString(R.string.level_4);
                    break;
                case 8:
                    s = getResources().getString(R.string.level_5);
                    break;
            }
            try {
                Games.Leaderboards.submitScore(googleApiClient, s, ((100 * n) - (this.s * 2)));
            }catch (Exception e){}
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(false);
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.custom_dialog_view, null);
            final EditText input = (EditText) view.findViewById(R.id.input);
            TextView level = (TextView) view.findViewById(R.id.level);
            level.setText("Level: " + n + "x" + n);
            TextView tt = (TextView) view.findViewById(R.id.time);
            tt.setText(time.getText());
            TextView st = (TextView) view.findViewById(R.id.step);
            st.setText("Steps: " + this.s);
            TextView gs = (TextView) view.findViewById(R.id.gs);
            gs.setText("Global Score : " + ((100 * n) - (this.s * 2)));
            input.setText(preferences.getString("name", ""));
            builder.setView(view);
            builder.setPositiveButton("Play Again", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = input.getText().toString();
                    MainActivity.this.finish();
                    Intent i = new Intent(MainActivity.this, MainActivity.class);
                    i.putExtra("n", n);
                    startActivity(i);
                    editor.putString("name", name);
                    editor.commit();
                    helper.open();
                    ContentValues values = new ContentValues();
                    values.put("level", n);
                    values.put("name", name);
                    values.put("steps", MainActivity.this.s);
                    values.put("time", time.getText().toString());
                    values.put("score",((100 * n) - (MainActivity.this.s * 2)));
                    helper.insertData(helper.TABLE_NAME, values);
                    helper.close();
                }
            });
            builder.setNegativeButton("Different Level", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                    String name = input.getText().toString();
                    editor.putString("name", name);
                    editor.commit();
                    helper.open();
                    ContentValues values = new ContentValues();
                    values.put("level", n);
                    values.put("name", name);
                    values.put("steps", MainActivity.this.s);
                    values.put("time", time.getText().toString());
                    values.put("score",((100 * n) - (MainActivity.this.s * 2)));
                    helper.insertData(helper.TABLE_NAME, values);
                    helper.close();
                }
            });
            builder.show();

        }
    }

    private Boolean checkWin(int n) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (lights[i][j] == 1) return false;
            }
        }
        return true;
    }

    public int[][] createRandomGame(int n) {
        int l[][] = new int[n][n];
        Random r = new Random();
        int p;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                l[i][j] = 1;
        p = r.nextInt(n * n) + 1;
        for (int i = 0; i < p; i++) {
            int row = r.nextInt(n);
            int col = r.nextInt(n);
            l[row][col] = 0;
        }
        return l;
    }

    public void createView(int n) {
        rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
        display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        lightView = new View[n][n];
        TextView label = new TextView(getApplicationContext());
        ViewGroup.LayoutParams labelParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        label.setPadding(16, 16, 16, 16);
        label.setLayoutParams(labelParams);
        label.setGravity(Gravity.CENTER);
        label.setText("Level: " + n + "x" + n);
        label.setTextSize(20);
        label.setTextColor(getResources().getColor(R.color.white));
        label.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        int width = (display.getWidth() - 18) / n;
        LinearLayout vl = new LinearLayout(this);
        vl.setOrientation(LinearLayout.VERTICAL);
        vl.setGravity(Gravity.CENTER);
        vl.setPadding(0, 16, 0, 0);
        rootLayout.addView(label);
        LinearLayout hl[] = new LinearLayout[n];
        for (int i = 0; i < n; i++) {
            hl[i] = new LinearLayout(MainActivity.this);
            hl[i].setOrientation(LinearLayout.HORIZONTAL);
            hl[i].setGravity(Gravity.CENTER);
            for (int j = 0; j < n; j++) {
                lightView[i][j] = new View(this);
                lightView[i][j] = new View(this);
                lightView[i][j].setBackgroundResource(R.drawable.light_off);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, width);
                hl[i].addView(lightView[i][j], params);
            }
            vl.addView(hl[i]);
        }
        rootLayout.addView(vl);
        LinearLayout footer = new LinearLayout(getApplicationContext());
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 16, 0, 0);
        footer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout f1 = new LinearLayout(getApplicationContext());
        f1.setOrientation(LinearLayout.HORIZONTAL);
        f1.setGravity(Gravity.CENTER);
        f1.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        step = new TextView(MainActivity.this);
        step.setTextSize(16);
        step.setText("Steps : 10");
        step.setGravity(Gravity.CENTER);
        step.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        f1.addView(step);
        time = new TextView(MainActivity.this);
        time.setTextSize(16);
        time.setText("10:11:1");
        time.setGravity(Gravity.CENTER);
        time.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        f1.addView(time);
        footer.addView(f1);
        LinearLayout f2 = new LinearLayout(getApplicationContext());
        f2.setOrientation(LinearLayout.HORIZONTAL);
        f2.setGravity(Gravity.CENTER);
        f2.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hint = new Button(MainActivity.this);
        hint.setTextSize(16);
        hint.setText("Hint");
        hint.setGravity(Gravity.CENTER);
        hint.setBackgroundResource(R.drawable.light_on);
        hint.setTextColor(getResources().getColor(R.color.white));
        hint.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        f2.addView(hint);
        reset = new Button(MainActivity.this);
        reset.setTextSize(16);
        reset.setText("Reset");
        reset.setGravity(Gravity.CENTER);
        reset.setBackgroundResource(R.drawable.light_on);
        reset.setTextColor(getResources().getColor(R.color.white));
        reset.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        f2.addView(reset);
        footer.addView(f2);
        home = new Button(MainActivity.this);
        home.setTextSize(16);
        home.setText("Home");
        home.setGravity(Gravity.CENTER);
        home.setBackgroundResource(R.drawable.light_on);
        home.setTextColor(getResources().getColor(R.color.white));
        home.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        footer.addView(home);
        rootLayout.addView(footer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                googleApiClient.connect();
            } else {
                // Bring up an error dialog to alert the user that sign-in
                // failed. The R.string.signin_failure should reference an error
                // string in your strings.xml file that tells the user they
                // could not be signed in, such as "Unable to sign in."
                BaseGameUtils.showActivityResultError(this,
                        requestCode, resultCode, R.string.signin_failure);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // already resolving
            return;
        }

        // if the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInflow) {
            mAutoStartSignInflow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign-in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    googleApiClient, connectionResult,
                    RC_SIGN_IN, "Sign in Error")) {
                mResolvingConnectionFailure = false;
            }
        }
    }

}
