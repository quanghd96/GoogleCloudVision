package icom.quang.google.cloudvison

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.cameraview.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_CAMERA = 6969
    private lateinit var mCallback: CameraView.Callback
    private val handler = Handler()
    private val runnable = Runnable {
        cameraView.takePicture()
        start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        mCallback = object : CameraView.Callback() {

            override fun onPictureTaken(cameraView: CameraView, data: ByteArray) {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                // we create an scaled bitmap so it reduces the image, not just trim it
                val b2 = Bitmap.createScaledBitmap(bitmap, 400, 400, false)
                val outStream = ByteArrayOutputStream()
                b2.compress(Bitmap.CompressFormat.JPEG, 70, outStream)
                val imageBase64 = String(Base64.encode(outStream.toByteArray(), Base64.DEFAULT))
                val body = "{\"requests\":[{\"image\":{\"content\":\"$imageBase64\"},\"features\":[{\"type\":\"LABEL_DETECTION\",\"maxResults\":10}]}]}"
                val req = JsonObjectRequest(
                        "https://vision.googleapis.com/v1/images:annotate?key=${getString(R.string.api_key)}",
                        JSONObject(body),
                        Response.Listener {
                            try {
                                var result = ""
                                val labelAnnotations = it.getJSONArray("responses").getJSONObject(0).getJSONArray("labelAnnotations")
                                for (i in 0 until labelAnnotations.length()) {
                                    result += labelAnnotations.getJSONObject(i).getString("description")
                                    if (i < labelAnnotations.length() - 1) result += ", "
                                }
                                tvResult.text = result
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        },
                        Response.ErrorListener {
                        }
                )
                AppController.getInstance(this@MainActivity).addToRequestQueue(req)
            }

        }
        cameraView.addCallback(mCallback)
        start()
    }

    private fun start() {
        handler.postDelayed(runnable, 5000)
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) start()
    }

    override fun onStop() {
        super.onStop()
        cameraView.stop()
        handler.removeCallbacks(runnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
