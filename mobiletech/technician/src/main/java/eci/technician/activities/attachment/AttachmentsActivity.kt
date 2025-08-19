@file:Suppress("SpellCheckingInspection")

package eci.technician.activities.attachment

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.BaseActivity
import eci.technician.R
import eci.technician.databinding.ActivityAttachmentsBinding
import eci.technician.databinding.UpdateNamePhotoDialogBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.DateTimeHelper
import eci.technician.helpers.ErrorHelper.AttachmentErrorListener
import eci.technician.helpers.ErrorHelper.ErrorHandler
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.api.retroapi.ErrorType
import eci.technician.helpers.attachmentHelper.AttachmentHandler
import eci.technician.helpers.attachmentHelper.AttachmentListener
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.models.attachments.ui.AttachmentItemUI
import eci.technician.tools.Constants
import eci.technician.workers.OfflineManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class AttachmentsActivity : BaseActivity(), AttachmentAdapter.AttachmentAdapterListener,
    AttachmentErrorListener, AttachmentListener, View.OnClickListener {
    private lateinit var binding: ActivityAttachmentsBinding
    private lateinit var currentPhotoPath: String
    private var callNumber: String? = ""
    private var callNumberId: Int = 0
    private var mToast: Toast? = null
    private lateinit var adapter: AttachmentAdapter
    private var cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && this::currentPhotoPath.isInitialized) {
                val timeStamp2: String = DateTimeHelper.formatTimeDataAllowedPhotoName(Date())
                val filename = "Photo - $timeStamp2"
                val fileLocationUri: Uri? = Uri.fromFile(File(currentPhotoPath))
                showAlertForUriFile2(fileLocationUri, true, currentPhotoPath, filename)
            }
        }
    private var fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showAlertForUriFile2(
                    result.data?.data,
                    isImage = false,
                    path = "",
                    pictureFileName = ""
                )
            }
        }

    companion object {
        const val TAG = "AttachmentActivity"
        const val EXCEPTION = "Exception"
        val UNSUPPORTED_TYPES = arrayOf("sql", "com", "apk")
        const val STORAGE_PERMISSION_CODE = 1234
        const val CAMERA_PERMISSION_CODE = 1111
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[AttachmentsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttachmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ErrorHandler.get().addErrorListener(this)
        AttachmentHandler.get().addListener(this)
        FBAnalyticsConstants.logEvent(this, FBAnalyticsConstants.ATTACHMENTS_ACTIVITY)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        callNumber = intent.getStringExtra(Constants.EXTRA_CALL_NUMBER_CODE)
        callNumberId = intent.getIntExtra(Constants.EXTRA_CALL_NUMBER_ID, 0)
        viewModel.callNumberId = callNumberId
        viewModel.callNumber = callNumber ?: ""


        viewModel.loadAttachmentFromDB(callNumberId = callNumberId)
        viewModel.fetchAttachments()

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) !=
            PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }

        val connection = NetworkConnection(baseContext)
        connection.observe(this, { t ->
            t?.let {
                if (it) {
                    object : CountDownTimer(1500, 100) {
                        override fun onTick(millisUntilFinished: Long) {/*not used*/
                        }

                        override fun onFinish() {
                            AppAuth.getInstance().isConnected = true
                            OfflineManager.retryAttachmentWorker(baseContext)
                        }
                    }.start()
                } else {
                    AppAuth.getInstance().isConnected = false
                }
            }
        })

        binding.btnAttachFile.setOnClickListener {

            openFiles()
        }

        binding.recAttachments.layoutManager = LinearLayoutManager(this)
        binding.recAttachments.setHasFixedSize(true)
        binding.btnPhoto.setOnClickListener(this)

        setUpRecycler()
        observeNetworkError()
        observeAttachmentFromDB()
        observeLoading()
        observeDownloadSuccess()
        observeSaveError()
        observeLoadOfflineError()
    }

    private fun observeLoadOfflineError() {
        viewModel.offlineError.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { showError ->
                if (showError) {
                    showMessageBox(
                        getString(R.string.offline_couldnt_retrieve_attachments),
                        getString(R.string.offline_warning)
                    )
                }
            }
        }
    }

    private fun observeSaveError() {
        viewModel.saveError.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { showError ->
                if (showError) {
                    showMessageBox(
                        getString(R.string.somethingWentWrong),
                        getString(R.string.cannot_save_file)
                    )
                }
            }
        }
    }

    private fun observeDownloadSuccess() {
        viewModel.downloadSuccess.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { filePath ->
                val file = File(filePath)
                openFile(file)
            }
        }
    }

    private fun setUpRecycler() {
        adapter = AttachmentAdapter(this)
        binding.recAttachments.layoutManager = LinearLayoutManager(this)
        binding.recAttachments.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recAttachments.adapter = adapter
    }

    private fun observeLoading() {
        viewModel.loading.observe(this) { isLoading ->
            binding.loaderIncluded.progressBarContainer.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun observeAttachmentFromDB() {
        viewModel.attachmentList.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun observeNetworkError() {
        viewModel.networkError.observe(this) {
            it.getContentIfNotHandledOrReturnNull()?.let { pair ->
                showNetworkErrorDialog(pair, this, supportFragmentManager)
            }
        }
    }

    private fun downloadAttachment(linkID: Int) {
        viewModel.getAttachmentFile(linkID)
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val intent = Intent(ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.choose_application)))
        } catch (e: Exception) {
            e.printStackTrace()
            showMessageBox(getString(R.string.error), getString(R.string.cannot_open))
        }
    }


    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnPhoto -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE
                    )
                } else {
                    takePicture()
                }
            }
        }
    }

    private fun openFiles() {
        FBAnalyticsConstants.logEvent(
            this,
            FBAnalyticsConstants.AttachmentsActivity.OPEN_FILE_ACTION
        )
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .apply {
                type = "*/*"
            }
        fileLauncher.launch(intent)
    }


    private fun deleteImageAfterUploaded(path: String) {
        try {
            val fDelete = File(path)
            if (fDelete.exists()) {
                fDelete.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun getExtensionFromUri(uri: Uri): String {
        val path = uri.path ?: return ""
        return MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(path)).toString())
    }

    private fun isValidExtension(
        extension: String,
        type: String?,
        typeExtension: String?
    ): Boolean {
        return when {
            UNSUPPORTED_TYPES.contains(extension) || UNSUPPORTED_TYPES.contains(typeExtension) || type == Constants.APPLICATION_OCTET -> {
                showMessageBox(
                    getString(R.string.unsupported_file),
                    getString(R.string.UNSUPPORTED_FILE_TYPE)
                )
                false
            }
            else -> {
                true
            }
        }
    }

    private fun getFileNameFromUri(fileUri: Uri): String {
        var name = ""
        contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val cursorIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(cursorIndex)
        }
        return name
    }

    private fun getFileSizeFromUri(fileUri: Uri): Long {
        var fileSize: Long = 0
        contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val cursorIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            fileSize = cursor.getLong(cursorIndex)
        }
        return fileSize
    }

    private fun showAlertForUriFile2(
        uri: Uri?,
        isImage: Boolean,
        path: String,
        pictureFileName: String
    ) {
        try {
            val fileUri = uri ?: return
            val extension = getExtensionFromUri(fileUri)
            val type = getTypeFromUri(fileUri)
            val typeExtension = getTypeExtensionFromUri(fileUri)
            val fileName = getFileNameFromUri(fileUri)
            val fileExtension = viewModel.getFileExtension(fileName)
            val fileSize = getFileSizeFromUri(fileUri)
            val fileNameWithoutExtension =
                if (isImage) pictureFileName else viewModel.getFileNameWithoutExtension(fileName)

            if (!isValidExtension(extension, type, typeExtension)) return

            val builder = AlertDialog.Builder(this)
            val dialogView = getDialogUpdateNamePhotoView(fileNameWithoutExtension)
            val txtValue = dialogView.findViewById<EditText>(R.id.txtValue)
            builder.setView(dialogView)
            builder.setTitle(getString(R.string.attach_file))
            builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val pictureExtension = viewModel.getPictureExtension(extension, fileExtension)
                if (isImage) {
                    sendPictureFileUri(
                        fileUri,
                        txtValue.text.toString(),
                        isImage,
                        pictureExtension,
                        type ?: "",
                        fileSize
                    )
                } else {
                    sendPictureFileUri(
                        fileUri,
                        txtValue.text.toString(),
                        isImage,
                        fileExtension,
                        type ?: "",
                        fileSize
                    )
                }
            }
            builder.setNegativeButton(R.string.cancel) { _, _ ->
                if (isImage) {
                    deleteImageAfterUploaded(path)
                    takePicture()
                }
            }
            val dialog = builder.create()

            txtValue.doAfterTextChanged { validateInputText(it?.toString() ?: "", dialog) }

            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(baseContext, R.color.colorAccent))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(baseContext, R.color.colorAccent))
                validateInputText(txtValue.text.toString(), dialog)
            }
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
    }

    private fun validateInputText(text: String, dialog: AlertDialog) {
        validateNewText(text, isValid = { isValid ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = isValid
                setTextColor(
                    ContextCompat.getColor(
                        this@AttachmentsActivity,
                        if (isValid) R.color.colorAccent else R.color.disabled_button
                    )
                )
            }
        })
    }


    private fun getTypeExtensionFromUri(fileUri: Uri): String? {
        val type = getTypeFromUri(fileUri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
    }

    private fun getTypeFromUri(fileUri: Uri): String? {
        return contentResolver.getType(fileUri)
    }

    private fun validateNewText(newText: String, isValid: (valid: Boolean) -> Unit) {
        if (viewModel.hasForbiddenCharacters(newText)) {
            showAToast(getString(R.string.attachment_name_format_error))
            isValid.invoke(false)
            return
        }
        if (newText.isEmpty()) {
            showAToast(getString(R.string.photo_dialog_name_cannot_be_empty))
            isValid.invoke(false)
            return
        }
        if (viewModel.isRepeated(newText)) {
            showAToast(getString(R.string.photo_dialog_name_already_taken))
            isValid.invoke(false)
            return
        }
        isValid.invoke(true)
        return
    }


    private fun showAToast(message: String?) {
        mToast?.cancel()
        mToast = Toast.makeText(baseContext, message, Toast.LENGTH_SHORT)
        mToast?.show()
    }


    private fun getDialogUpdateNamePhotoView(fileName: String): View {
        val bindingDialog = UpdateNamePhotoDialogBinding.inflate(LayoutInflater.from(this))
        bindingDialog.txtValue.filters =
            arrayOf(InputFilter.LengthFilter(this.viewModel.maxFileName))
        bindingDialog.txtValue.setText(fileName)
        return bindingDialog.root
    }

    private fun changeFileName(fileName: String): String {
        return "${callNumber}_" + fileName
    }

    private fun sendPictureFileUri(
        uri: Uri,
        name: String,
        isImage: Boolean,
        extension: String,
        contentType: String,
        fileSize: Long
    ) {
        try {
            val fullFileName = "$name.$extension"
            val stream = contentResolver.openInputStream(uri)
            if (stream != null) {
                val bytes = stream.readBytes()
                val size = fileSize.toInt()
                val contentTypeFile = if (isImage) "image/jpeg" else contentType
                viewModel.sendFile(fullFileName, size, contentTypeFile, bytes)
                OfflineManager.retryAttachmentWorker(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
            Toast.makeText(this, "Invalid file", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showQuestionBoxWithCustomButtonsTwoListeners(getString(R.string.android_11_attachments_denied_title),
                        getString(R.string.android_11_attachments_denied_message),
                        getString(R.string.go_to_settings),
                        getString(android.R.string.cancel),
                        { _, _ -> goToSettings() },
                        { _, _ -> finish() })
                }
            }
            CAMERA_PERMISSION_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        showQuestionBoxWithCustomButtons(
                            getString(R.string.android_11_camera_denied_title),
                            getString(R.string.android_11_camera_denied_message),
                            getString(R.string.go_to_settings),
                            getString(android.R.string.cancel)
                        ) { _, _ -> goToSettings() }
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) run {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onItemTap(item: AttachmentItemUI) {
        item.filename?.let {
            val file =
                File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), changeFileName(it))
            if (file.exists()) {
                openFile(file)
            } else {
                downloadAttachment(item.dBFileLinkID)
            }
        }
    }

    private fun takePicture() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager).also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "eci.technician.internal.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = this.absolutePath
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AttachmentHandler.get().removeListener(this)
        ErrorHandler.get().removeListener(this)
    }

    override fun onListenError(
        error: Pair<ErrorType, String?>?,
        requestType: String,
        callId: Int,
        data: String
    ) {
        when (requestType) {
            "UploadEsnDocument" -> showErrorDialog(callId, error)
        }
    }

    override fun updateAttachmentList() {
        // do nothing
    }

    private fun showErrorDialog(callId: Int, error: Pair<ErrorType, String?>?) {
        if (callId != callNumberId) return
        this.showNetworkErrorDialog(error, this, supportFragmentManager)
    }
}
