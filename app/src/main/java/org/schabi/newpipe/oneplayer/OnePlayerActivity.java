package org.schabi.newpipe.oneplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.schabi.newpipe.MainActivity;

/** Compatibility for old development shortcuts; the launcher is now MainActivity. */
public final class OnePlayerActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
