/*
 * Copyright 2017 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.nekocode.musicviz;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.nekocode.musicviz.render.GLScene;
import cn.nekocode.musicviz.render.SceneController;
import cn.nekocode.musicviz.render.VisualizerRenderer;
import cn.nekocode.musicviz.scene.BasicSpectrumScene;
import cn.nekocode.musicviz.scene.ChlastScene;
import cn.nekocode.musicviz.scene.CircSpectrumScene;
import cn.nekocode.musicviz.scene.EnhancedSpectrumScene;
import cn.nekocode.musicviz.scene.InputSoundScene;
import cn.nekocode.musicviz.scene.OriginScene;
import cn.nekocode.musicviz.scene.RainbowSpectrumScene;
import cn.nekocode.musicviz.scene.Sa2WaveScene;
import cn.nekocode.musicviz.scene.WavesRemixScene;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class MainActivity extends AppCompatActivity implements Visualizer.OnDataCaptureListener {
    private static final int REQUEST_PERMISSION = 101;
    private FrameLayout mContainerView;
    private VisualizerRenderer mRender;
    private SceneController mSceneController;
    private List<Pair<String, ? extends GLScene>> mSceneList;
    private MediaPlayer mPlayer;
    private Visualizer mVisualizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mContainerView = new FrameLayout(this));

        /*
          Check premission
         */
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Show an explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "RECORD_AUDIO permission is required.", Toast.LENGTH_SHORT).show();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_PERMISSION);
            }

        } else {
            start();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start();
                }
            }
        }
    }

    int captureSize = 512;

    private void start() {
        Log.d("asuralxd", "getCaptureSizeRange " + Arrays.toString(Visualizer.getCaptureSizeRange()));
        captureSize = Visualizer.getCaptureSizeRange()[1];
        captureSize = captureSize > 512 ? 512 : captureSize;
        /*
          Setup texture view
         */
        final TextureView textureView = new TextureView(this);
        mContainerView.addView(textureView);
        textureView.setSurfaceTextureListener(mRender = new VisualizerRenderer(this, captureSize / 2));
        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {

                mRender.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
            }
        });
        textureView.requestLayout();

        mRender.setSceneController(mSceneController = new SceneController() {
            @Override
            public void onSetup(Context context, int audioTextureId, int textureWidth) {
                mSceneList = new ArrayList<>();
                GLScene defaultScene = new BasicSpectrumScene(context, audioTextureId, textureWidth);
                mSceneList.add(Pair.create("Basic Spectrum", defaultScene));
                mSceneList.add(Pair.create("Circle Spectrum", new CircSpectrumScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Enhanced Spectrum", new EnhancedSpectrumScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Input Sound", new InputSoundScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Sa2Wave", new Sa2WaveScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Waves Remix", new WavesRemixScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Rainbow Spectrum", new RainbowSpectrumScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Chlast", new ChlastScene(context, audioTextureId, textureWidth)));
                mSceneList.add(Pair.create("Origin Texture", new OriginScene(context, audioTextureId, textureWidth)));

                changeScene(defaultScene);

                invalidateOptionsMenu();
            }
        });

        /*
          Setup media player and play music
         */
        mPlayer = MediaPlayer.create(this, R.raw.simple_drum_beat);
        mPlayer.setLooping(false);
        mPlayer.start();
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d("asuralxd", "onPrepared");
                mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
                mVisualizer.setCaptureSize(captureSize);
                Log.d("asuralxd", "getMaxCaptureRate " + Visualizer.getMaxCaptureRate());
                mVisualizer.setDataCaptureListener(MainActivity.this, Visualizer.getMaxCaptureRate(), true, true);
                // Start capturing
                mVisualizer.setEnabled(true);
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("asuralxd", "onCompletion");
                mVisualizer.setEnabled(false);
            }
        });

    }

    @Override
    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
        Log.d("asuralxd", "onWaveFormDataCapture " + samplingRate + " " + waveform.length + " " + waveform);
        mRender.updateWaveFormFrame(new WaveFormFrame(waveform, 0, waveform.length / 2));
    }

    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
        Log.d("asuralxd", "onFftDataCapture " + samplingRate + " " + fft.length + " " + Arrays.toString(fft));
        mRender.updateFFTFrame(new FFTFrame(fft, 0, fft.length / 2));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (mSceneList != null) {
            int id = 0;
            for (Pair<String, ? extends GLScene> pair : mSceneList) {
                menu.add(0, id, id, pair.first);
                id++;
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mSceneList != null && mSceneController != null) {
            final GLScene scene = mSceneList.get(item.getItemId()).second;
            mSceneController.changeScene(scene);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mVisualizer.setEnabled(false);
            mPlayer.release();
        }
    }
}
