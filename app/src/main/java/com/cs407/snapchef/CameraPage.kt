package com.cs407.snapchef

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPage : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val galleryUri = it
        try{
            if (galleryUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, galleryUri)
                val base64String = bitmapToDataImage(bitmap)

                moveToIdentifyActivity(base64String)
            } else {
                Toast.makeText(this@CameraPage, "Could not upload Image!", Toast.LENGTH_SHORT).show()
            }

        }catch(e:Exception){
            Toast.makeText(this@CameraPage, "Could not upload Image!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cameraPreview)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10001)
        }

        val cameraPreview = findViewById<PreviewView>(R.id.cameraPreview)
        val captureButton = findViewById<Button>(R.id.captureButton)
        val uploadImageButton = findViewById<Button>(R.id.uploadImageButton)

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up the camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = ImageCapture.Builder().build()

            // Bind the camera to the lifecycle
            preview.setSurfaceProvider(cameraPreview.surfaceProvider)
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to bind camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))

        // Click Listeners
        captureButton.setOnClickListener { capturePhoto() }
        uploadImageButton.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    private fun capturePhoto() {
        val imageFile = imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val base64String = bitmapToDataImage(bitmap)

                    moveToIdentifyActivity(base64String)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraPage, "Could not take Image!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun bitmapToDataImage(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun moveToIdentifyActivity(dataImage: String) {
        val file = File(this@CameraPage.filesDir, "image_base64.txt")
        val outputStream = FileOutputStream(file)
        outputStream.write(dataImage.toByteArray())
        outputStream.close()

        val intent = Intent(this, IngredientsConfirmActivity::class.java)
        intent.putExtra("dataImagePath", file.absolutePath)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down the camera executor
        cameraExecutor.shutdown()
    }
}