package cn.nekocode.musicviz.recorder;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;
import com.zlw.main.recorderlib.RecordManager;
import com.zlw.main.recorderlib.recorder.RecordConfig;
import com.zlw.main.recorderlib.recorder.RecordHelper;
import com.zlw.main.recorderlib.recorder.listener.RecordFftDataListener;
import com.zlw.main.recorderlib.recorder.listener.RecordResultListener;
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener;
import com.zlw.main.recorderlib.utils.Logger;

import java.io.File;
import java.util.Locale;

import cn.nekocode.musicviz.BuildConfig;
import cn.nekocode.musicviz.MyApp;
import cn.nekocode.musicviz.R;
import cn.nekocode.musicviz.databinding.ActivityHzBinding;


public class TestHzActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = TestHzActivity.class.getSimpleName();
    private ActivityHzBinding binding;
    private boolean isStart = false;
    private boolean isPause = false;
    final RecordManager recordManager = RecordManager.getInstance();
    private static final String[] STYLE_DATA = new String[]{"STYLE_ALL", "STYLE_NOTHING", "STYLE_WAVE", "STYLE_HOLLOW_LUMP"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_hz);
        initPermission();
        initAudioView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initRecord();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recordManager.stop();
    }

    private void initAudioView() {
        binding.audioView.setStyle(AudioView.ShowStyle.STYLE_ALL, AudioView.ShowStyle.STYLE_ALL);
        binding.tvState.setVisibility(View.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, STYLE_DATA);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spUpStyle.setAdapter(adapter);
        binding.spDownStyle.setAdapter(adapter);
        binding.spUpStyle.setOnItemSelectedListener(this);
        binding.spDownStyle.setOnItemSelectedListener(this);
        initViewClicked();
    }


    private void initPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(new String[]{Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE,
                        Permission.RECORD_AUDIO})
                .start();
    }

    private void initRecord() {
        recordManager.init(MyApp.getInstance(), BuildConfig.DEBUG);
        recordManager.changeFormat(RecordConfig.RecordFormat.WAV);
        String recordDir = String.format(Locale.getDefault(), "%s/Record/com.zlw.main/",
                Environment.getExternalStorageDirectory().getAbsolutePath());
        recordManager.changeRecordDir(recordDir);

        recordManager.setRecordStateListener(new RecordStateListener() {
            @Override
            public void onStateChange(RecordHelper.RecordState state) {
                Logger.i(TAG, "onStateChange %s", state.name());

                switch (state) {
                    case PAUSE:
                        binding.tvState.setText("暂停中");
                        break;
                    case IDLE:
                        binding.tvState.setText("空闲中");
                        break;
                    case RECORDING:
                        binding.tvState.setText("录音中");
                        break;
                    case STOP:
                        binding.tvState.setText("停止");
                        break;
                    case FINISH:
                        binding.tvState.setText("录音结束");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(String error) {
                Logger.i(TAG, "onError %s", error);
            }
        });
        recordManager.setRecordResultListener(new RecordResultListener() {
            @Override
            public void onResult(File result) {
                Toast.makeText(TestHzActivity.this, "录音文件： " + result.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        });
        recordManager.setRecordFftDataListener(new RecordFftDataListener() {
            @Override
            public void onFftData(byte[] data) {
                byte[] newdata = new byte[data.length - 36];
                for (int i = 0; i < newdata.length; i++) {
                    newdata[i] = data[i + 36];
                }
                binding.audioView.setWaveData(data);
            }
        });
    }

    public void initViewClicked() {
        binding.btRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStart) {
                    recordManager.pause();
                    binding.btRecord.setText("开始");
                    isPause = true;
                    isStart = false;
                } else {
                    if (isPause) {
                        recordManager.resume();
                    } else {
                        recordManager.start();
                    }
                    binding.btRecord.setText("暂停");
                    isStart = true;
                }
            }
        });
        binding.btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordManager.stop();
                binding.btRecord.setText("开始");
                isPause = false;
                isStart = false;
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spUpStyle:
                binding.audioView.setStyle(AudioView.ShowStyle.getStyle(STYLE_DATA[position]), binding.audioView.getDownStyle());
                break;
            case R.id.spDownStyle:
                binding.audioView.setStyle(binding.audioView.getUpStyle(), AudioView.ShowStyle.getStyle(STYLE_DATA[position]));
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
