package com.xlwapp.dailylevel2

import android.Manifest
import android.R.attr
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE = 6666
    private var mProjectionManager: MediaProjectionManager? = null
    private var sMediaProjection: MediaProjection? = null
    private var sc: ScreenCapturer? = null
    val MARKET_GOOGLE = "com.android.vending"
    val ID = ArrayList<String>()

    val requestFloatWindowPermissionCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, requestFloatWindowPermissionCode)
        }
        // 如果是7.1.1以上的系统（包含7.1.1），则需要判断是否有悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }

        val MY_READ_EXTERNAL_REQUEST : Int = 1
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            } else {
                TODO("VERSION.SDK_INT < M")
            }
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_READ_EXTERNAL_REQUEST)
            }
        }

        setContentView(R.layout.activity_main)
        var i: Int = 0
        addId()
        text_view.text = i++.toString()
        buttonStart.setOnClickListener {
            if (!isAvilible(MARKET_GOOGLE)) {
                Toast.makeText(this, "doesn't have google play store", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ID.isNullOrEmpty()) {
                Toast.makeText(this, "finish", Toast.LENGTH_SHORT).show()
            } else {
                showCapButton()
                text_view.text = ID.get(0) + "\n" + i++.toString()
                val intent = Intent(Intent.ACTION_VIEW);
                val uri =
                    Uri.parse("http://play.google.com/store/search?q=" + ID.removeAt(0) + "&c=apps");
                intent.setData(uri);
                intent.setPackage(MARKET_GOOGLE);
                startActivity(intent);
            }
        }
    }

    private fun showCapButton() {
        val buttonC = Button(this).apply {
            text = "截屏";
            setOnClickListener {
                mProjectionManager =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    } else {
                        TODO("VERSION.SDK_INT < LOLLIPOP")
                    };
                startActivityForResult(
                    mProjectionManager?.createScreenCaptureIntent(),
                    REQUEST_CODE
                );
                it.visibility = View.INVISIBLE
                val window: Window = window
                val wl: WindowManager.LayoutParams = window.getAttributes()
                wl.alpha = 0.0f //这句就是设置窗口里控件的透明度的,0.0全透明,1.0不透明．
                window.setAttributes(wl)
            }

        }

        val windowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val wmParams = WindowManager.LayoutParams().apply {
            type = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 -> WindowManager.LayoutParams.TYPE_PHONE
                else -> WindowManager.LayoutParams.TYPE_TOAST
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.END or Gravity.CENTER
        }
        windowManager.addView(buttonC, wmParams)
    }

    private fun addId() {
        ID.add("qr scanner")
        ID.add("qr code reader")
        ID.add("qr code scanner ")
        ID.add("free qr scanner")
        ID.add("free qr code reader")
        ID.add("barcode scanner")

        ID.add("fat burning")
        ID.add("home workout")
        ID.add("weight loss")

        ID.add("abs workout 30 day")
        ID.add("abs workout")

        ID.add("lose weight")
        ID.add("lose weight in 30 days")
        ID.add("weight loss")

        ID.add("increase height ")
        ID.add("height increase")
        ID.add("height increase exercise")
        ID.add("grow taller")
        ID.add("taller exercise")

        ID.add("baby tracker")
    }

    fun isAvilible(packageName: String?): Boolean {
        //获取packagemanager
        val packageManager: PackageManager = this.getPackageManager()
        //获取所有已安装程序的包信息
        val packageInfos =
            packageManager.getInstalledPackages(0)
        //用于存储所有已安装程序的包名
        val packageNames: MutableList<String> = ArrayList()
        //从pinfo中将包名字逐一取出，压入pName list中
        for (i in packageInfos.indices) {
            val packName = packageInfos[i].packageName
            packageNames.add(packName)
        }
        //判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packageName)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestFloatWindowPermissionCode
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
        ) {
            if (Settings.canDrawOverlays(this)) {
                Log.i("ABC", "用户给予悬浮窗权限了")
            } else {
                Log.i("ABC", "用户没给悬浮窗权限")
            }
        }
        Log.d("WOW", "" + REQUEST_CODE + REQUEST_CODE)
        if (RESULT_OK == resultCode && REQUEST_CODE == requestCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    //启动前台服务
                    val service = Intent(this, CaptureScreenService::class.java)
                    service.putExtra("code", resultCode)
                    service.putExtra("data", data)
                    Log.e("testextra", "$resultCode" + attr.data)
                    startForegroundService(service)
                } else {
                    sMediaProjection =
                        mProjectionManager!!.getMediaProjection(resultCode, data!!)
                    if (sMediaProjection != null) {
                        sc = ScreenCapturer(this, sMediaProjection, "").startProjection()
                    }
                }

            }
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

        }
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(sc != null) {
            sc!!.stopProjection()
        }
        val service = Intent(this, CaptureScreenService::class.java)
        stopService(service)
    }
}
