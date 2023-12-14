package top.jiecs.screener.ui.resolution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import top.jiecs.screener.databinding.FragmentResolutionBinding

import top.jiecs.screener.R
import android.content.Context
import android.view.Display
import android.view.WindowManager
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper

import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.util.Log

class ResolutionFragment : Fragment() {

    private var _binding: FragmentResolutionBinding? = null
   
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    
    private lateinit var windowManager: WindowManager
    
    private lateinit var iWindowManager: Any

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val resolutionViewModel =
            ViewModelProvider(this).get(ResolutionViewModel::class.java)

        _binding = FragmentResolutionBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        windowManager = requireActivity().windowManager
        iWindowManager = asInterface("android.view.IWindowManager", "window")
        resolutionViewModel.fetchScreenResolution(windowManager)
        
        val textHeight = binding.textHeight.editText!!
        val textWidth = binding.textWidth.editText!!
        val textDpi = binding.textDpi.editText!!
        resolutionViewModel.resolutionMap.observe(viewLifecycleOwner) {
            textHeight.setText(it["height"].toString())
            textWidth.setText(it["width"].toString())
            textDpi.setText(it["dpi"].toString())
        }
        
        val textView = binding.textResolution
        resolutionViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        
        binding.btApply.setOnClickListener {
            applyResolution(textHeight.text.toString().toInt(),
              textWidth.text.toString().toInt(),
              textDpi.text.toString().toInt())
        }
        binding.btReset.setOnClickListener {
            resetResolution()
        }
        return root
    }
    
    private fun asInterface(className: String, serviceName: String): Any =
        ShizukuBinderWrapper(SystemServiceHelper.getSystemService(serviceName)).let {
            Class.forName("$className\$Stub").run {
                HiddenApiBypass.invoke(this, null, "asInterface", it)
            }
        }
    
    fun applyResolution(height: Int, width: Int, dpi: Int) {
        Log.d("screener", "apply")
        // TODO: apply dpi for each user
        
        HiddenApiBypass.invoke(iWindowManager::class.java, iWindowManager,
          "setForcedDisplaySize", Display.DEFAULT_DISPLAY, width, height)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
          .setTitle(getString(R.string.success))
          .setMessage(getString(R.string.reset_hint))
          .setPositiveButton(getString(R.string.looks_fine), null)
          .setNegativeButton(getString(R.string.undo_changes)) { _, _ ->
               resetResolution()
          }
          .setCancelable(false)
          .show()
          
        val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        var countdown = 10
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
          override fun run() {
            if (isAdded) negativeButton.text = getString(R.string.undo_changes, "${countdown}s")

            if (countdown <= 0) {
              resetResolution()
              if (isAdded && dialog.isShowing) negativeButton.text =
                getString(R.string.undo_changes, getString(R.string.undone))
              return
            }
            countdown--
            handler.postDelayed(this, 1000)
          }
        }
        
        Handler(Looper.getMainLooper()).post(runnable)
        
        dialog.setOnDismissListener {
            handler.removeCallbacks(runnable)
        }
    }
    
    fun resetResolution() {
        HiddenApiBypass.invoke(iWindowManager::class.java, iWindowManager,
          "clearForcedDisplaySize", Display.DEFAULT_DISPLAY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
}
