package app.spidy.browser.fragments

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewpager2.widget.ViewPager2
import app.spidy.browser.R
import app.spidy.browser.adapters.PagerStateAdapter
import app.spidy.browser.adapters.TabAdapter
import app.spidy.browser.controllers.Browser


class BrowserFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var stateAdapter: PagerStateAdapter
    private lateinit var menuIcon: ImageView
    private lateinit var tabsBtn: ImageView
    lateinit var urlField: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var recordBtn: ImageView
    private lateinit var terminalBtn: ImageView
    lateinit var browser: Browser
    private var tabAdapter: TabAdapter? = null
    private lateinit var menuDialog: AlertDialog
    private var menuRefreshImage: ImageView? = null
    private lateinit var settingsDialog: Dialog

    var dismissCallback: (() -> Unit)? = null
    var setBrowserListener: (() -> Browser.Listener)? = null
    var urlToLoad: String? = null

    fun getRecordButton(): ImageView? {
        return if (::recordBtn.isInitialized) recordBtn else null
    }

    fun getTerminalButton(): ImageView? {
        return if (::terminalBtn.isInitialized) terminalBtn else null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_browser, container, false)

        viewPager = v.findViewById(R.id.viewPager)
        menuIcon = v.findViewById(R.id.menuImageView)
        tabsBtn = v.findViewById(R.id.tabsBtn)
        urlField = v.findViewById(R.id.urlField)
        progressBar = v.findViewById(R.id.progressBar)
        recordBtn = v.findViewById(R.id.recordBtn)
        terminalBtn = v.findViewById(R.id.terminalBtn)

        browser = Browser(
            context = requireContext(),
            progressBar = progressBar,
            viewPager = viewPager,
            urlField = urlField,
            dismissCallback = {
                dismissCallback?.invoke()
            },
            getStateAdapter = {
                return@Browser stateAdapter
            },
            getCurrentTabIndex = {
                return@Browser viewPager.currentItem
            },
            tabsBtn = tabsBtn,
            getTabAdapter = {
                return@Browser tabAdapter
            },
            getRefreshImageView = {
                return@Browser menuRefreshImage
            }
        )

        browser.listener = setBrowserListener?.invoke()

        stateAdapter = PagerStateAdapter(browser.tabs, requireActivity())
        viewPager.adapter = stateAdapter
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 100

        browser.newTab(urlToLoad)

        settingsDialog = createSettingsDialog()
        menuDialog = createOptionMenu(requireContext())

        menuIcon.setOnClickListener {
            menuDialog.show()
//            browser.listener?.onCloseBrowser(browser.currentTab.fragment.webView)
        }

        recordBtn.setOnClickListener {
            if (!browser.isRecordingEnabled) {
                browser.isRecordingEnabled = true
                recordBtn.setImageResource(R.drawable.ic_record_active)
                browser.listener?.onRecordingEnabled(browser.currentTab.fragment.webView)
            } else {
                browser.isRecordingEnabled = false
                recordBtn.setImageResource(R.drawable.ic_record)
                browser.currentTab.fragment.webView?.reload()
            }
        }

        terminalBtn.setOnClickListener {
            browser.listener?.onOpenTerminal()
        }

        tabsBtn.setOnClickListener {
            var dialog: AlertDialog? = null
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Tabs")
            val tabsView = LayoutInflater.from(context).inflate(R.layout.layout_tabs_dialog, null)
            val recyclerView: RecyclerView = tabsView.findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(context)
            tabAdapter = TabAdapter(requireContext(), browser.tabs, browser) {
                dialog?.dismiss()
            }
            recyclerView.adapter = tabAdapter
            (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            builder.setView(tabsView)
            builder.setPositiveButton("New Tab") {d, _ ->
                browser.newTab()
                d.dismiss()
            }
            builder.setNegativeButton("Cancel") {d, _ -> d.dismiss() }
            dialog = builder.create()
            dialog.show()
        }

        urlField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                browser.browse(urlField.text.toString())
                urlField.isFocusableInTouchMode = false
                urlField.isFocusable = false
                urlField.isFocusableInTouchMode = true
                urlField.isFocusable = true
                val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view?.windowToken, 0)
            }

            return@setOnEditorActionListener true
        }

        CookieManager.getInstance().acceptCookie()

        return v
    }

    private fun createSettingsDialog(): Dialog {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogTheme)
        val view = layoutInflater.inflate(R.layout.browser_layout_settings_dialog, null)
        val closeImage: ImageView = view.findViewById(R.id.settings_close_image)
        closeImage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.window?.also {
            it.attributes.windowAnimations = R.style.SlideUpAndDownAnimationTheme

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = Color.BLACK
            }
        }

        dialog.setOnDismissListener {
            browser.currentTab.fragment.webView?.settings?.javaScriptEnabled = browser.isJavaScriptEnabled
        }

        return dialog
    }

    private fun createOptionMenu(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context, R.style.BrowserTheme_DialogTheme)
        val root: ViewGroup? = null
        val menuDialogView = LayoutInflater.from(context).inflate(R.layout.browser_layout_options_menu, root, false)
        builder.setView(menuDialogView)
        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val wind = dialog.window
        wind?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        wind?.setBackgroundDrawableResource(android.R.color.transparent)
        val params = wind?.attributes
        params?.gravity = Gravity.TOP or Gravity.END
        params?.x = 0
        params?.y = 0
        wind?.attributes = params

        menuRefreshImage = menuDialogView.findViewById(R.id.refreshImage)
        val menuGoForwardImage: ImageView = menuDialogView.findViewById(R.id.goForwardImage)
        val menuGoBackImage: ImageView = menuDialogView.findViewById(R.id.menuGoBackImage)
        val menuPageInfoImage: ImageView = menuDialogView!!.findViewById(R.id.menuPageInfoImage)
        val menuShare: TextView = menuDialogView.findViewById(R.id.menuShare)
        val menuExit: TextView = menuDialogView.findViewById(R.id.menuExit)
        val menuSettings: TextView = menuDialogView.findViewById(R.id.menuSettings)
        val menuFeedback: TextView = menuDialogView.findViewById(R.id.menuFeedback)


        menuRefreshImage!!.setOnClickListener {
            menuDialog.dismiss()

            if (browser.isLoading) {
                browser.currentTab.fragment.webView?.stopLoading()
                browser.hideProgressBar()
            } else {
                browser.currentTab.fragment.webView?.reload()
            }
        }

        menuGoForwardImage.setOnClickListener {
            menuDialog.dismiss()

            browser.currentTab.fragment.webView?.also {
                if (it.canGoForward()) it.goForward()
            }
        }

        menuGoBackImage.setOnClickListener {
            menuDialog.dismiss()

            browser.currentTab.fragment.webView?.also {
                if (it.canGoBack()) it.goBack()
            }
        }


        menuShare.setOnClickListener {
            menuDialog.dismiss()

            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                browser.currentTab.fragment.webView?.title
            )
            sharingIntent.putExtra(Intent.EXTRA_TEXT, browser.currentTab.fragment.webView?.url)
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }

        menuSettings.setOnClickListener {
            menuDialog.dismiss()
            settingsDialog.show()
        }

        menuFeedback.setOnClickListener {
            menuDialog.dismiss()
            val uri = Uri.parse("market://details?id=${context.packageName}");
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket);
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=${context.packageName}"))
                )
            }
        }

        menuExit.setOnClickListener {
            browser.listener?.onCloseBrowser(browser.currentTab.fragment.webView)
            menuDialog.dismiss()
        }

        /* Listeners */
        dialog.setOnShowListener {
            browser.currentTab.fragment.webView?.also {
                if (it.canGoForward()) {
                    menuGoForwardImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_arrow_forward))
                } else {
                    menuGoForwardImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_arrow_forward_disabled))
                }
                if (it.canGoBack()) {
                    menuGoBackImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_arrow_back))
                } else {
                    menuGoBackImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.browser_arrow_back_disabled))
                }
            }
        }

        return dialog
    }

    override fun onDestroy() {
        browser.listener = null
        super.onDestroy()
    }

    fun canGoBack(): Boolean {
        return browser.currentTab.fragment.webView?.canGoBack() ?: false
    }

    fun goBack() {
        browser.currentTab.fragment.webView?.goBack()
    }

    companion object {
        @JvmStatic
        fun newInstance() = BrowserFragment()
    }
}