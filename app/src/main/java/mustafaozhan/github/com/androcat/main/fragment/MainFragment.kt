package mustafaozhan.github.com.androcat.main.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.github.jorgecastillo.FillableLoader
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.livinglifetechway.quickpermissions.annotations.WithPermissions
import im.delight.android.webview.AdvancedWebView
import kotlinx.android.synthetic.main.fragment_main.fillableLoader
import kotlinx.android.synthetic.main.fragment_main.fillableLoaderInverted
import kotlinx.android.synthetic.main.fragment_main.fillableLoaderLayout
import kotlinx.android.synthetic.main.fragment_main.mBottomNavigationView
import kotlinx.android.synthetic.main.fragment_main.mSwipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_main.newsFeedFab
import kotlinx.android.synthetic.main.fragment_main.webView
import me.piruin.quickaction.ActionItem
import me.piruin.quickaction.QuickAction
import mustafaozhan.github.com.androcat.R
import mustafaozhan.github.com.androcat.base.BaseMvvmFragment
import mustafaozhan.github.com.androcat.extensions.remove
import mustafaozhan.github.com.androcat.extensions.runScript
import mustafaozhan.github.com.androcat.main.activity.MainActivity
import mustafaozhan.github.com.androcat.settings.SettingsFragment
import mustafaozhan.github.com.androcat.tools.JsScrip

/**
 * Created by Mustafa Ozhan on 2018-07-22.
 */
@Suppress("TooManyFunctions", "MagicNumber", "LargeClass")
class MainFragment : BaseMvvmFragment<MainFragmentViewModel>(), AdvancedWebView.Listener {

    companion object {
        private const val ARGS_OPEN_URL = "ARGS_OPEN_URL"

        var TAG: String = MainFragment::class.java.simpleName

        fun newInstance(url: String): MainFragment {
            val args = Bundle()
            args.putString(ARGS_OPEN_URL, url)
            val fragment = MainFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance() = MainFragment()
    }

    private lateinit var quickActionProfile: QuickAction
    private lateinit var quickActionExplorer: QuickAction
    private lateinit var baseUrl: String

    private var logoutCount = 0
    private var loader: FillableLoader? = null
    private var isAnimating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setDash()
        initWebView()
        setListeners()
        setActionListeners()

        arguments?.getString(ARGS_OPEN_URL)?.let { url ->
            loadUrlWithAnimation(url)
            arguments?.clear()
        }
    }

    private fun init() {
        fillableLoader.setSvgPath(getString(R.string.androcat_svg_path))
        fillableLoaderInverted.setSvgPath(getString(R.string.androcat_svg_path))

        baseUrl = if (viewModel.isLoggedIn() == true) {
            getString(R.string.url_github)
        } else {
            getString(R.string.url_login)
        }

        invert(viewModel.getSettings().isInvert)

        loadUrlWithAnimation(baseUrl)

        context?.let { ctx ->
            quickActionExplorer = QuickAction(ctx, QuickAction.VERTICAL)
            quickActionExplorer.apply {
                setColorRes(R.color.colorPrimary)
                setTextColorRes(R.color.white)
                setEnabledDivider(false)
                addActionItem(
                    ActionItem(1, getString(R.string.search), R.drawable.ic_search),
                    ActionItem(2, getString(R.string.market_place), R.drawable.ic_market_place),
                    ActionItem(3, getString(R.string.trends), R.drawable.ic_trends),
                    ActionItem(4, getString(R.string.new_gist), R.drawable.ic_gist),
                    ActionItem(5, getString(R.string.new_repository), R.drawable.ic_repository),
                    ActionItem(6, getString(R.string.invert), R.drawable.ic_invert),
                    ActionItem(7, getString(R.string.forward), R.drawable.ic_forward),
                    ActionItem(8, getString(R.string.back), R.drawable.ic_back)
                )
            }

            quickActionProfile = QuickAction(ctx, QuickAction.VERTICAL)
            quickActionProfile.apply {
                setColorRes(R.color.colorPrimary)
                setTextColorRes(R.color.white)
                setEnabledDivider(false)
                addActionItem(
                    ActionItem(1, getString(R.string.starts), R.drawable.ic_stars),
                    ActionItem(2, getString(R.string.repositories), R.drawable.ic_repository),
                    ActionItem(3, getString(R.string.gists), R.drawable.ic_gist),
                    ActionItem(4, getString(R.string.notifications), R.drawable.ic_notifications),
                    ActionItem(5, getString(R.string.app_settings), R.drawable.ic_settings),
                    ActionItem(6, getString(R.string.user_settings), R.drawable.ic_user_settings),
                    ActionItem(7, getString(R.string.log_out), R.drawable.ic_logout),
                    ActionItem(8, getString(R.string.log_in), R.drawable.ic_login),
                    ActionItem(9, getString(R.string.profile), R.drawable.ic_user)
                )
            }
        }
    }

    private fun setListeners() {
        newsFeedFab.setOnClickListener {
            loadUrlWithAnimation(
                if (viewModel.isLoggedIn() == false) {
                    getString(R.string.url_login)
                } else {
                    getString(R.string.url_github)
                }
            )
        }
        webView?.setListener(getBaseActivity(), this)

        mSwipeRefreshLayout.setOnRefreshListener {
            loadUrlWithAnimation(webView?.url)
            mSwipeRefreshLayout.isRefreshing = false
        }

        mBottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_user -> quickActionProfile.show(mBottomNavigationView.getIconAt(4))
                R.id.navigation_find -> quickActionExplorer.show(mBottomNavigationView.getIconAt(0))
                R.id.navigation_pull_request -> loadUrlWithAnimation(getString(R.string.url_pulls))
                R.id.navigation_Issues -> loadUrlWithAnimation(getString(R.string.url_issues))
            }
            true
        }
    }

    @Suppress("ComplexMethod")
    private fun setActionListeners() {
        quickActionProfile.setOnActionItemClickListener { item ->
            when (item.actionId) {
                1 -> loadIfUserNameSet(getString(R.string.url_github) + viewModel.getUsername() + "?tab=stars")
                2 -> loadIfUserNameSet(getString(R.string.url_github) + viewModel.getUsername() + "?tab=repositories")
                3 -> loadIfUserNameSet(getString(R.string.url_gist) + viewModel.getUsername())
                4 -> loadUrlWithAnimation(getString(R.string.url_notifications))
                5 -> replaceFragment(SettingsFragment.newInstance(), true)
                6 -> loadUrlWithAnimation(getString(R.string.url_settings))
                7 -> {
                    loadUrlWithAnimation(getString(R.string.url_logout))
                    baseUrl = getString(R.string.url_login)
                }
                8 -> loadUrlWithAnimation(getString(R.string.url_login))
                9 -> loadIfUserNameSet(getString(R.string.url_github) + viewModel.getUsername())
                else -> loadUrlWithAnimation(getString(R.string.url_github))
            }
        }
        quickActionExplorer.setOnActionItemClickListener { item ->
            when (item.actionId) {
                1 -> loadUrlWithAnimation(getString(R.string.url_search))
                2 -> loadUrlWithAnimation(getString(R.string.url_market_place))
                3 -> loadUrlWithAnimation(getString(R.string.url_trending))
                4 -> loadUrlWithAnimation(getString(R.string.url_gist))
                5 -> loadUrlWithAnimation(getString(R.string.url_new))
                6 -> invert(!viewModel.getSettings().isInvert, true)
                7 -> webView?.goForward()
                8 -> webView?.goBack()
                else -> loadUrlWithAnimation(getString(R.string.url_github))
            }
        }
    }

    private fun invert(invert: Boolean, changeSettings: Boolean = false) {
        setInversion(invert)
        webView?.runScript(JsScrip.getInversion(invert))

        if (changeSettings) {
            viewModel.updateInvertSettings(invert)
        }
    }

    private fun setDash() = mBottomNavigationView.apply {
        inflateMenu(R.menu.bnvm_dash)
        labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
        enableAnimation(false)
        setTextSize(10.0f)
        setIconsMarginTop(16)
        setIconSize(32.0F, 32.0F)
        getBottomNavigationItemView(2).background = null
        getBottomNavigationItemView(1).background = null
        getBottomNavigationItemView(3).background = null
        getBottomNavigationItemView(2).isClickable = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView?.apply {
            setBackgroundColor(Color.parseColor("#FFFFFF"))

            settings.apply {
                setDesktopMode(true)
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
        }
    }

    private fun loadIfUserNameSet(url: String) =
        viewModel.getUsername()?.let { username ->
            if (username.isEmpty()) {
                snacky(getString(R.string.missUsername), getString(R.string.enter)) {
                    replaceFragment(SettingsFragment.newInstance(), true)
                }
            } else {
                loadUrlWithAnimation(url)
            }
        }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        if (MainActivity.uri != null) {
            loadUrlWithAnimation(MainActivity.uri)
            MainActivity.uri = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        webView?.onActivityResult(requestCode, resultCode, intent)
    }

    @WithPermissions([Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {
        if (!AdvancedWebView.handleDownload(context, url, suggestedFilename)) {
            snacky("Download unsuccessful, download manager has been disabled on device")
        }
    }

    override fun onExternalPageRequest(url: String?) {
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {
        loadUrlWithAnimation(webView?.context?.getString(R.string.url_blank))
    }

    override fun onPageStarted(url: String, favicon: Bitmap?) {
        if (!isAnimating) {
            loadingView(true)
        }
        if (url.contains(webView?.context?.getString(R.string.url_session).toString())) {
            webView?.runScript(JsScrip.GET_USERNAME) { str ->
                if (str != "null")
                    viewModel.updateUser(str.remove("\""), true)
            }
            logoutCount = 0
        }
    }

    override fun onPageFinished(url: String?) {
        webView?.apply {
            runScript(JsScrip.getInversion(viewModel.loadSettings().isInvert)) {
                loadingView(false)
            }
            when (url) {
                context.getString(R.string.url_blank) -> {
                    logoutCount = 0
                }
                context.getString(R.string.url_logout) -> {
                    logoutCount++
                    if (logoutCount == 2) {
                        baseUrl = context.getString(R.string.url_login)
                        loadUrl(context.getString(R.string.url_login))
                        viewModel.updateUser(isLoggedIn = false)
                    }
                }
                context?.getString(R.string.url_session) -> {
                    webView?.runScript(JsScrip.GET_USERNAME) { str ->
                        if (str != "null")
                            viewModel.updateUser(str.remove("\""), true)
                    }
                    logoutCount = 0
                }
                else -> {
                    logoutCount = 0
                }
            }
        }
    }

    private fun loadingView(show: Boolean) {
        if (show && !isAnimating) {
            isAnimating = true
            fillableLoaderLayout?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
            fillableLoaderLayout?.visibility = View.VISIBLE
            loader?.visibility = View.VISIBLE
            fillableLoader?.start()
            fillableLoaderInverted?.start()
        } else {
            fillableLoaderLayout?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out))
            fillableLoaderLayout?.visibility = View.GONE
            fillableLoader?.visibility = View.GONE
            fillableLoaderInverted?.visibility = View.GONE
            fillableLoader?.reset()
            fillableLoaderInverted?.reset()
            isAnimating = false
        }
    }

    private fun setInversion(inversion: Boolean) = context?.let { ctx ->
        loader = if (inversion) {
            fillableLoaderLayout?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.colorPrimaryDark))
            fillableLoader?.visibility = View.GONE
            fillableLoaderInverted
        } else {
            fillableLoaderLayout?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
            fillableLoaderInverted?.visibility = View.GONE
            fillableLoader
        }
    }

    override fun getViewModelClass(): Class<MainFragmentViewModel> = MainFragmentViewModel::class.java

    override fun getLayoutResId(): Int = R.layout.fragment_main

    private fun loadUrlWithAnimation(urlToLoad: String?) = urlToLoad?.let { url ->
        loadingView(true)
        webView?.loadUrl(url)
    }
}