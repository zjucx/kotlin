package cn.zjucx.zjucx

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.util.*
import android.hardware.camera2.CameraCaptureSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.*
import android.widget.ImageView
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SurfaceHolder.Callback, View.OnClickListener{

    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCameraManager: CameraManager? = null
    private var mCameraDevice: CameraDevice? = null
    private var mainHandler: Handler? = null
    private var mCameraID: String? = null//摄像头Id 0 为后  1 为前
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private var mImageReader: ImageReader? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var iv_show: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("chengxiang", "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initView()
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun initView() {
        mSurfaceView = findViewById(R.id.localView) as SurfaceView
        mSurfaceHolder = (mSurfaceView as SurfaceView).getHolder() as SurfaceHolder
        mSurfaceHolder!!.setKeepScreenOn(true);

        startBackgroundThread()
        mSurfaceHolder!!.addCallback(this)
        iv_show = findViewById(R.id.iv_show_camera2_activity) as ImageView
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                capture()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {

            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
    @SuppressLint("MissingPermission")
    private fun initCamera2() {
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG,1);
        mImageReader!!.setOnImageAvailableListener(mOnImageAvailableListener, mainHandler)

        mainHandler = Handler(getMainLooper());
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//后摄像头
        //获取摄像头管理
        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        //打开摄像头
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //return;
            }
            mCameraManager!!.openCamera(mCameraID, mStateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraDevice.close()
            mCameraDevice = null
        }

    }

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        // reader -> mBackgroundHandler!!.post(ImageSaver(reader.acquireNextImage(), mFile!!))
        Log.i("chengxiang", "mOnImageAvailableListener")
        // 拿到拍照照片数据
        var image = reader.acquireNextImage();
        var buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)//由缓冲区存入字节数组
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size);
        if (bitmap != null) {
            iv_show!!.setImageBitmap(bitmap);
        }

        image.close()
    }


    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mainHandler = Handler(getMainLooper())

    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        try {

            // 创建预览需要的CaptureRequest.Builder
            var previewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurfaceHolder!!.getSurface());
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice!!.createCaptureSession(Arrays.asList(mSurfaceHolder!!.getSurface(), mImageReader!!.getSurface()),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (null == mCameraDevice) return;
                            // 当摄像头已经准备好时，开始显示预览
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                // 自动对焦
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // 打开闪光灯
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                // 显示预览
                                var previewRequest = previewRequestBuilder.build();
                                mCameraCaptureSession!!.setRepeatingRequest(previewRequest, null, mBackgroundHandler);

                            } catch (e : CameraAccessException ) {
                                e.printStackTrace();
                            }

                        }

                        override fun onConfigureFailed(
                                cameraCaptureSession: CameraCaptureSession) {
                        }
                    }, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        if (null != mCameraDevice) {
            mCameraDevice!!.close();
            this.mCameraDevice = null;
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        initCamera2()

    }

    override fun onClick(v: View?) {

    }
    private fun capture() {
        Log.i("chengxiang", "capture")
        // Handle the camera action
        if (mCameraDevice == null) return

        // 创建拍照需要的CaptureRequest.Builder
        var captureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        // 将imageReader的surface作为CaptureRequest.Builder的目标
        captureRequestBuilder.addTarget(mImageReader!!.getSurface());
        // 自动对焦
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // 自动曝光
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // 获取手机方向
        // var rotation = getWindowManager().getDefaultDisplay().getRotation();
        // 根据设备方向计算设置照片的方向
        // captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
        //拍照
        var mCaptureRequest = captureRequestBuilder.build();
        mCameraCaptureSession!!.capture(mCaptureRequest, null, mainHandler);
    }
}

