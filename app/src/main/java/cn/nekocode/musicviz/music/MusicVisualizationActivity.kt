package cn.nekocode.musicviz.music

import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import cn.nekocode.musicviz.MyApp
import cn.nekocode.musicviz.R
import cn.nekocode.musicviz.databinding.ActivityMusicVisualizationBinding
import cn.nekocode.musicviz.recorder.AudioView
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordHelper.RecordState
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener
import com.zlw.main.recorderlib.utils.Logger
import java.util.*

/**
 * Author: Asuraliu
 * Date: 2021/12/3 14:45
 * Description: 音乐可视化页面
 * History:
 * <author> <time> <version> <desc>
 * Asuraliu 2021/12/3 1.0 首次创建
 */
class MusicVisualizationActivity : AppCompatActivity() {

    private val TAG = MusicVisualizationActivity::class.java.simpleName
    var binding: ActivityMusicVisualizationBinding? = null
    private var isRecordStart = false
    private var isRecordPause = false
    private var isMusicStart = false
    private var isMusicPause = false
    private val recordManager = RecordManager.getInstance()
    private var mPlayer: MediaPlayer? = null
    private var mVisualizer: Visualizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_music_visualization)
        initMusicForm()
        initEvent()
        initRecord()
        AndPermission.with(this)
            .runtime()
            .permission(
                arrayOf(
                    Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE,
                    Permission.RECORD_AUDIO
                )
            )
            .start()
    }

    private fun initMusicForm() {
        if (binding!!.rgAudioFrom.checkedRadioButtonId == R.id.rbMusic) {
            binding!!.lineRecord.visibility = View.GONE
            initViewClicked()
        } else {
            binding!!.lineRecord.visibility = View.VISIBLE
            initAudioView()
        }
    }

    private fun initAudioView() {
        binding!!.audioView.setStyle(AudioView.ShowStyle.getStyle("STYLE_HOLLOW_LUMP"), AudioView.ShowStyle.getStyle("STYLE_HOLLOW_LUMP"))
        initViewClicked2()
    }

    private fun initViewClicked() {
        binding!!.btRecord.setOnClickListener {

            /*
          Setup media player and play music
         */mPlayer = MediaPlayer.create(this, R.raw.youhebuke)
            mPlayer?.setLooping(true)
            mPlayer?.start()

            mVisualizer = Visualizer(mPlayer!!.getAudioSessionId())

            var captureSize = Visualizer.getCaptureSizeRange()[1]
            captureSize = if (captureSize > 512) 512 else captureSize

            mVisualizer!!.captureSize = captureSize
            binding!!.audioView.setStyle(AudioView.ShowStyle.STYLE_HOLLOW_LUMP, AudioView.ShowStyle.STYLE_HOLLOW_LUMP)
            mVisualizer!!.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    binding!!.audioView.setWaveData(waveform)
                }

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {

                }

            }, Visualizer.getMaxCaptureRate(), true, true)
            // Start capturing
            mVisualizer!!.enabled = true
        }
        binding!!.btStop.setOnClickListener {
            mPlayer?.stop()
            mPlayer?.release()
        }
    }

    private fun initViewClicked2() {
        binding!!.btRecord.setOnClickListener { doPlay() }
        binding!!.btStop.setOnClickListener { doStop() }
    }

    private fun doStop() {
        recordManager.stop()
        binding!!.btRecord.text = "开始"
        isRecordPause = false
        isRecordStart = false
    }

    private fun doPlay() {
        if (isRecordStart) {
            recordManager.pause()
            binding!!.btRecord.text = "开始"
            isRecordPause = true
            isRecordStart = false
        } else {
            if (isRecordPause) {
                recordManager.resume()
            } else {
                recordManager.start()
            }
            binding!!.btRecord.text = "暂停"
            isRecordStart = true
        }
    }

    private fun initEvent() {
        binding!!.rgAudioFrom.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbMusic -> {
                    binding!!.lineRecord.visibility = View.GONE
                }
                R.id.rbRecord -> {
                    binding!!.lineRecord.visibility = View.VISIBLE
                }
                else -> {
                }
            }
        }
        binding!!.rgAudioFormat.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbPcm -> recordManager.changeFormat(RecordConfig.RecordFormat.PCM)
                R.id.rbMp3 -> recordManager.changeFormat(RecordConfig.RecordFormat.MP3)
                R.id.rbWav -> recordManager.changeFormat(RecordConfig.RecordFormat.WAV)
                else -> {
                }
            }
        }
        binding!!.rgSimpleRate.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb8K -> recordManager.changeRecordConfig(recordManager.recordConfig.setSampleRate(8000))
                R.id.rb16K -> recordManager.changeRecordConfig(recordManager.recordConfig.setSampleRate(16000))
                R.id.rb44K -> recordManager.changeRecordConfig(recordManager.recordConfig.setSampleRate(44100))
                else -> {
                }
            }
        }
        binding!!.tbEncoding.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb8Bit -> recordManager.changeRecordConfig(recordManager.recordConfig.setEncodingConfig(AudioFormat.ENCODING_PCM_8BIT))
                R.id.rb16Bit -> recordManager.changeRecordConfig(recordManager.recordConfig.setEncodingConfig(AudioFormat.ENCODING_PCM_16BIT))
                else -> {
                }
            }
        }
    }

    private fun initRecord() {
        recordManager.init(MyApp.getInstance(), true)
        recordManager.changeFormat(RecordConfig.RecordFormat.WAV)
        val recordDir = String.format(
            Locale.getDefault(), "%s/Record/com.zlw.main/",
            Environment.getExternalStorageDirectory().absolutePath
        )
        recordManager.changeRecordDir(recordDir)
        initRecordEvent()
    }

    private fun initRecordEvent() {
        recordManager.setRecordStateListener(object : RecordStateListener {
            override fun onStateChange(state: RecordState) {
                Logger.i(TAG, "onStateChange %s", state.name)
                when (state) {
                    RecordState.PAUSE -> binding!!.tvState.text = "暂停中"
                    RecordState.IDLE -> binding!!.tvState.text = "空闲中"
                    RecordState.RECORDING -> binding!!.tvState.text = "录音中"
                    RecordState.STOP -> binding!!.tvState.text = "停止"
                    RecordState.FINISH -> {
                        binding!!.tvState.text = "录音结束"
                        binding!!.tvSoundSize.text = "---"
                    }
                    else -> {
                    }
                }
            }

            override fun onError(error: String) {
                Logger.i(TAG, "onError %s", error)
            }
        })
        recordManager.setRecordSoundSizeListener { soundSize ->
            binding!!.tvSoundSize.text = String.format(Locale.getDefault(), "声音大小：%s db", soundSize)
        }
        recordManager.setRecordResultListener { result ->
            Toast.makeText(this, "录音文件： " + result.absolutePath, Toast.LENGTH_SHORT).show()
        }
        recordManager.setRecordFftDataListener { data -> binding!!.audioView.setWaveData(data) }
    }
}