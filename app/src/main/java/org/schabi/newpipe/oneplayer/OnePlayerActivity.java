package org.schabi.newpipe.oneplayer;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;
import org.schabi.newpipe.util.ThemeHelper;

import dev.oneyoutubeplayer.playback.EngineId;
import dev.oneyoutubeplayer.playback.PlaybackCoordinator;
import dev.oneyoutubeplayer.playback.PlaybackRequest;

/** Minimal manual-entry screen shared by touch devices and the TV launcher. */
public final class OnePlayerActivity extends AppCompatActivity {
    private EditText videoInput;
    private RadioGroup engines;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
        setTitle("OneYoutubePlayer");

        final ScrollView scroll = new ScrollView(this);
        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        final int padding = (int) (24 * getResources().getDisplayMetrics().density);
        body.setPadding(padding, padding, padding, padding);
        scroll.addView(body);
        setContentView(scroll);

        final TextView heading = new TextView(this);
        heading.setText("OneYoutubePlayer");
        heading.setTextSize(26);
        body.addView(heading);

        videoInput = new EditText(this);
        videoInput.setId(R.id.oneplayer_video_input);
        videoInput.setHint(R.string.oneplayer_video_input);
        videoInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        videoInput.setSingleLine(true);
        body.addView(videoInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView label = new TextView(this);
        label.setText(R.string.oneplayer_engine);
        body.addView(label);
        engines = new RadioGroup(this);
        final PlaybackCoordinator runtime = OnePlayerRuntime.get(this);
        for (EngineId id : runtime.availableEngines()) {
            final RadioButton button = new RadioButton(this);
            button.setId(id == EngineId.PIPEPIPE ? R.id.oneplayer_pipepipe : R.id.oneplayer_smarttube);
            button.setTag(id);
            button.setText(id.displayName());
            engines.addView(button);
        }
        final android.content.SharedPreferences preferences = getSharedPreferences("oneplayer", MODE_PRIVATE);
        engines.check("SMARTTUBE".equals(preferences.getString("engine", "PIPEPIPE"))
                ? R.id.oneplayer_smarttube : R.id.oneplayer_pipepipe);
        videoInput.setText(preferences.getString("video", ""));
        body.addView(engines);

        if (!runtime.availableEngines().contains(EngineId.SMARTTUBE)) {
            final TextView pending = new TextView(this);
            pending.setText(R.string.oneplayer_smarttube_pending);
            body.addView(pending);
        }

        final Button play = new Button(this);
        play.setText(R.string.oneplayer_play);
        play.setOnClickListener(view -> openVideo());
        body.addView(play);
        final Button browse = new Button(this);
        browse.setText(R.string.oneplayer_browse);
        browse.setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));
        body.addView(browse);
    }

    private void openVideo() {
        final PlaybackRequest request;
        try {
            request = PlaybackRequest.parse(videoInput.getText().toString());
        } catch (IllegalArgumentException error) {
            videoInput.setError(getString(R.string.oneplayer_bad_url));
            videoInput.requestFocus();
            return;
        }
        final RadioButton selected = findViewById(engines.getCheckedRadioButtonId());
        if (selected == null) {
            return;
        }
        try {
            final EngineId engine = (EngineId) selected.getTag();
            getSharedPreferences("oneplayer", MODE_PRIVATE).edit()
                    .putString("engine", engine.name()).putString("video", request.watchUrl()).apply();
            OnePlayerRuntime.get(this).open(engine, request);
        } catch (Exception error) {
            Toast.makeText(this, R.string.oneplayer_open_failed, Toast.LENGTH_LONG).show();
        }
    }
}
