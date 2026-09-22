package com.iridium.feature.reader.impl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError

/**
 * Fragment host for the Readium [EpubNavigatorFragment], embedded in Compose
 * via a FragmentContainerView. Owns the fragment lifecycle; the publication
 * and factory come from the [ReaderSessionStore] published by the ViewModel.
 *
 * Uses [EntryPointAccessors] instead of @AndroidEntryPoint: Hilt's processor
 * doesn't support entry points in library modules under our KSP setup.
 */
class ReaderHostFragment : Fragment(), EpubNavigatorFragment.Listener {

    private lateinit var store: ReaderSessionStore

    private var navigator: EpubNavigatorFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        store = EntryPointAccessors.fromApplication(
            requireContext().applicationContext,
            ReaderSessionEntryPoint::class.java,
        ).sessionStore()
        val factory = store.navigatorFactory
        if (factory == null) {
            // Process death wiped the in-memory session: install the dummy
            // factory so restoration doesn't crash, then leave via event.
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            lifecycleScope.launch { store.emit(ReaderSessionEvent.SessionLost) }
            return
        }
        childFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = store.initialLocator,
            initialPreferences = store.initialPreferences ?: EpubPreferences(),
            listener = this,
        )
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = FrameLayout(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        if (savedInstanceState == null && store.navigatorFactory != null) {
            childFragmentManager.commitNow {
                add(root.id, EpubNavigatorFragment::class.java, null, NAVIGATOR_TAG)
            }
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment
            ?: return
        navigator = nav
        store.navigator = nav
        nav.addInputListener(chromeTapListener)
        nav.addDecorationListener(DECORATION_GROUP, decorationListener)
        viewLifecycleOwner.lifecycleScope.launch {
            store.emit(ReaderSessionEvent.NavigatorAttached)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            nav.currentLocator.collect { store.onLocator(it) }
        }
    }

    override fun onDestroyView() {
        navigator?.let {
            it.removeInputListener(chromeTapListener)
            it.removeDecorationListener(decorationListener)
        }
        if (store.navigator === navigator) store.navigator = null
        navigator = null
        super.onDestroyView()
    }

    // EpubNavigatorFragment.Listener (via Navigator/Hyperlink contracts).

    override fun onResourceLoadFailed(href: Url, error: ReadError) {
        store.tryEmit(ReaderSessionEvent.ResourceFailed)
    }

    override fun onJumpToLocator(locator: Locator) {
        // TOC-internal jumps are handled by the navigator itself.
    }

    override fun shouldFollowInternalLink(link: Link, context: HyperlinkNavigator.LinkContext?): Boolean = true

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        store.tryEmit(ReaderSessionEvent.ExternalLink(url.toString()))
    }

    /** Center-tap toggles chrome; never consumed so links keep working. */
    private val chromeTapListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            store.tryEmit(ReaderSessionEvent.ContentTapped)
            return false
        }

        override fun onDrag(event: DragEvent): Boolean = false
        override fun onKey(event: KeyEvent): Boolean = false
    }

    private val decorationListener = object : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
            store.tryEmit(ReaderSessionEvent.DecorationTapped(event.decoration.id))
            return true
        }
    }

    companion object {
        const val NAVIGATOR_TAG = "iridium_epub_navigator"
        private const val DECORATION_GROUP = "highlights"

        fun newInstance() = ReaderHostFragment()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReaderSessionEntryPoint {
    fun sessionStore(): ReaderSessionStore
}
