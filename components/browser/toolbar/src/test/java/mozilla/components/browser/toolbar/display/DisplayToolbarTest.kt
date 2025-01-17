/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.display

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.forEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.R
import mozilla.components.concept.toolbar.Toolbar.SiteSecurity
import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Action
import mozilla.components.support.base.facts.processor.CollectionProcessor
import mozilla.components.support.test.any
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class DisplayToolbarTest {

    @Test
    fun `clicking on the URL switches the toolbar to editing mode`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val urlView = extractUrlView(displayToolbar)
        assertTrue(urlView.performClick())

        verify(toolbar).editMode()
    }

    @Test
    fun `progress is forwarded to progress bar`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val progressView = extractProgressView(displayToolbar)

        displayToolbar.updateProgress(10)
        assertEquals(10, progressView.progress)

        displayToolbar.updateProgress(50)
        assertEquals(50, progressView.progress)

        displayToolbar.updateProgress(75)
        assertEquals(75, progressView.progress)

        displayToolbar.updateProgress(100)
        assertEquals(100, progressView.progress)
    }

    @Test
    fun `icon view will use square size`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val iconView = extractIconView(displayToolbar)

        assertEquals(56, iconView.measuredWidth)
        assertEquals(56, iconView.measuredHeight)
    }

    @Test
    fun `progress view will use full width and 3dp height`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val progressView = extractProgressView(displayToolbar)

        assertEquals(1024, progressView.measuredWidth)
        assertEquals(3, progressView.measuredHeight)
    }

    @Test
    fun `progress view changes with gravity`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        val progressView = extractProgressView(displayToolbar)

        displayToolbar.progressBarGravity = 1
        assertEquals(1, displayToolbar.progressBarGravity)
        assertEquals(progressView.measuredHeight, progressView.bottom)

        displayToolbar.progressBarGravity = 0
        assertEquals(0, displayToolbar.progressBarGravity)
        assertEquals(toolbar.measuredHeight, progressView.bottom)
    }

    @Test
    fun `menu view is gone by default`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val menuView = extractMenuView(displayToolbar)
        assertNotNull(menuView)
        assertTrue(menuView.visibility == View.GONE)
    }

    @Test
    fun `menu view becomes visible once a menu builder is set`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val menuView = extractMenuView(displayToolbar)
        assertNotNull(menuView)

        assertTrue(menuView.visibility == View.GONE)

        displayToolbar.menuBuilder = BrowserMenuBuilder(emptyList())

        assertTrue(menuView.visibility == View.VISIBLE)

        displayToolbar.menuBuilder = null

        assertTrue(menuView.visibility == View.GONE)
    }

    @Test
    fun `no menu builder is set by default`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        assertNull(displayToolbar.menuBuilder)
    }

    @Test
    fun `menu builder will be used to create and show menu when button is clicked`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        val menuView = extractMenuView(displayToolbar)

        val menuBuilder = mock(BrowserMenuBuilder::class.java)
        val menu = mock(BrowserMenu::class.java)
        doReturn(menu).`when`(menuBuilder).build(testContext)

        displayToolbar.menuBuilder = menuBuilder

        verify(menuBuilder, never()).build(testContext)
        verify(menu, never()).show(menuView)

        menuView.performClick()

        verify(menuBuilder).build(testContext)
        verify(menu).show(eq(menuView), any(), anyBoolean(), any())
        verify(menu, never()).invalidate()

        displayToolbar.invalidateActions()

        verify(menu).invalidate()
    }

    @Test
    fun `browser action gets added as view to toolbar`() {
        val contentDescription = "Mozilla"

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        assertNull(extractActionView(displayToolbar, contentDescription))

        val action = BrowserToolbar.Button(mock(), contentDescription) {}
        displayToolbar.addBrowserAction(action)

        val view = extractActionView(displayToolbar, contentDescription)
        assertNotNull(view)
        assertEquals(contentDescription, view?.contentDescription)
    }

    @Test
    fun `clicking browser action view triggers listener of action`() {
        var callbackExecuted = false

        val action = BrowserToolbar.Button(mock(), "Button") {
            callbackExecuted = true
        }

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        displayToolbar.addBrowserAction(action)

        val view = extractActionView(displayToolbar, "Button")
        assertNotNull(view)

        assertFalse(callbackExecuted)

        view?.performClick()

        assertTrue(callbackExecuted)
    }

    @Test
    fun `browser action view will use square size`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val action = BrowserToolbar.Button(mock(), "action") {}
        displayToolbar.addBrowserAction(action)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val view = extractActionView(displayToolbar, "action")!!

        assertEquals(56, view.measuredWidth)
        assertEquals(56, view.measuredHeight)
    }

    @Test
    fun `page actions will be added as view to the toolbar`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        assertNull(extractActionView(displayToolbar, "Reader Mode"))

        val action = BrowserToolbar.Button(mock(), "Reader Mode") {}
        displayToolbar.addPageAction(action)

        assertNotNull(extractActionView(displayToolbar, "Reader Mode"))
    }

    @Test
    fun `clicking a page action view will execute the listener of the action`() {
        var listenerExecuted = false

        val action = BrowserToolbar.Button(mock(), "Reload") {
            listenerExecuted = true
        }

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        displayToolbar.addPageAction(action)

        assertFalse(listenerExecuted)

        val view = extractActionView(displayToolbar, "Reload")
        assertNotNull(view)
        view!!.performClick()

        assertTrue(listenerExecuted)
    }

    @Test
    fun `views for page actions will have a square shape`() {
        val action = BrowserToolbar.Button(mock(), "Open app") {}

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        displayToolbar.addPageAction(action)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val view = extractActionView(displayToolbar, "Open app")!!

        assertEquals(56, view.measuredWidth)
        assertEquals(56, view.measuredHeight)
    }

    @Test
    fun `navigation actions will be added as view to the toolbar`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        assertNull(extractActionView(displayToolbar, "Back"))
        assertNull(extractActionView(displayToolbar, "Forward"))

        displayToolbar.addNavigationAction(BrowserToolbar.Button(mock(), "Back") {})
        displayToolbar.addNavigationAction(BrowserToolbar.Button(mock(), "Forward") {})

        assertNotNull(extractActionView(displayToolbar, "Back"))
        assertNotNull(extractActionView(displayToolbar, "Forward"))
    }

    @Test
    fun `clicking on navigation action will execute listener of the action`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        var listenerExecuted = false
        val action = BrowserToolbar.Button(mock(), "Back") {
            listenerExecuted = true
        }

        displayToolbar.addNavigationAction(action)

        assertFalse(listenerExecuted)

        extractActionView(displayToolbar, "Back")!!
            .performClick()

        assertTrue(listenerExecuted)
    }

    @Test
    fun `navigation action view will have a square shape`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.addNavigationAction(
            BrowserToolbar.Button(mock(), "Back") {})

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val view = extractActionView(displayToolbar, "Back")!!

        assertEquals(56, view.measuredWidth)
        assertEquals(56, view.measuredHeight)
    }

    @Test
    fun `view of not visible navigation action gets removed after invalidating`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        var shouldActionBeDisplayed = true

        val action = BrowserToolbar.Button(
            mock(),
            "Back",
            visible = { shouldActionBeDisplayed }
        ) { /* Do nothing */ }

        displayToolbar.addNavigationAction(action)

        assertNotNull(extractActionView(displayToolbar, "Back"))

        shouldActionBeDisplayed = false
        displayToolbar.invalidateActions()

        assertNull(extractActionView(displayToolbar, "Back"))

        shouldActionBeDisplayed = true
        displayToolbar.invalidateActions()

        assertNotNull(extractActionView(displayToolbar, "Back"))
    }

    @Test
    fun `toolbar should call bind with view argument on action after invalidating`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val action = spy(BrowserToolbar.Button(mock(), "Reload") {})

        displayToolbar.addPageAction(action)

        val view = extractActionView(displayToolbar, "Reload")

        verify(action, never()).bind(view!!)

        displayToolbar.invalidateActions()

        verify(action).bind(view)
    }

    @Test
    fun `page action will not be added if visible lambda of action returns false`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val visibleAction = BrowserToolbar.Button(mock(), "Reload") {}
        val invisibleAction = BrowserToolbar.Button(
            mock(),
            "Reader Mode",
            visible = { false }) {}

        displayToolbar.addPageAction(visibleAction)
        displayToolbar.addPageAction(invisibleAction)

        assertNotNull(extractActionView(displayToolbar, "Reload"))
        assertNull(extractActionView(displayToolbar, "Reader Mode"))
    }

    @Test
    fun `browser action will not be added if visible lambda of action returns false`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val visibleAction = BrowserToolbar.Button(mock(), "Tabs") {}
        val invisibleAction = BrowserToolbar.Button(
            mock(),
            "Settings",
            visible = { false }) {}

        displayToolbar.addBrowserAction(visibleAction)
        displayToolbar.addBrowserAction(invisibleAction)

        assertNotNull(extractActionView(displayToolbar, "Tabs"))
        assertNull(extractActionView(displayToolbar, "Settings"))
    }

    @Test
    fun `navigation action will not be added if visible lambda of action returns false`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val visibleAction = BrowserToolbar.Button(mock(), "Forward") {}
        val invisibleAction = BrowserToolbar.Button(
            mock(),
            "Back",
            visible = { false }) {}

        displayToolbar.addNavigationAction(visibleAction)
        displayToolbar.addNavigationAction(invisibleAction)

        assertNotNull(extractActionView(displayToolbar, "Forward"))
        assertNull(extractActionView(displayToolbar, "Back"))
    }

    @Test
    fun `toolbar will honor minimum width of action view`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val normalAction = BrowserToolbar.Button(mock(), "Forward") {}
        val backAction = object : BrowserToolbar.Button(mock(), "Back", listener = {}) {
            override fun createView(parent: ViewGroup): View {
                return super.createView(parent).apply {
                    minimumWidth = 500
                }
            }
        }

        displayToolbar.addNavigationAction(normalAction)
        displayToolbar.addNavigationAction(backAction)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val forwardView = extractActionView(displayToolbar, "Forward")!!
        val backView = extractActionView(displayToolbar, "Back")!!

        assertEquals(56, forwardView.measuredWidth)
        assertEquals(56, forwardView.measuredHeight)

        assertEquals(500, backView.measuredWidth)
        assertEquals(56, backView.measuredHeight)
    }

    @Test
    fun `url box view will be added and removed from display layout`() {
        val view = TextView(testContext)

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        view assertNotIn displayToolbar

        displayToolbar.urlBoxView = view

        view assertIn displayToolbar

        displayToolbar.urlBoxView = null

        view assertNotIn displayToolbar
    }

    @Test
    fun `url box size matches url and page actions size`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reload") {})
        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reader Mode") {})

        val view = TextView(testContext)
        displayToolbar.urlBoxView = view

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)

        val urlView = extractUrlView(displayToolbar)
        val reloadView = extractActionView(displayToolbar, "Reload")!!
        val readerView = extractActionView(displayToolbar, "Reader Mode")!!
        val iconView = extractIconView(displayToolbar)

        assertTrue(view.measuredWidth > 0)

        assertEquals(urlView.measuredWidth + reloadView.measuredWidth + readerView.measuredWidth + iconView.measuredWidth,
            view.measuredWidth)
        assertEquals(200, view.measuredHeight)
    }

    @Test
    fun `url box position is enclosing icon, url and page actions`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reload") {})
        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reader Mode") {})

        val view = TextView(testContext)
        displayToolbar.urlBoxView = view

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)
        displayToolbar.layout(0, 0, 1024, 200)

        val urlView = extractUrlView(displayToolbar)
        val reloadView = extractActionView(displayToolbar, "Reload")!!
        val readerView = extractActionView(displayToolbar, "Reader Mode")!!
        val iconView = extractIconView(displayToolbar)

        val viewRect = Rect(view.left, view.top, view.right, view.bottom)
        val urlViewRect = Rect(urlView.left, urlView.top, urlView.right, urlView.bottom)
        val reloadViewRect = Rect(reloadView.left, reloadView.top, reloadView.right, reloadView.bottom)
        val readerViewRect = Rect(readerView.left, readerView.top, readerView.right, readerView.bottom)
        val iconViewRect = Rect(iconView.left, iconView.top, iconView.right, iconView.bottom)

        assertTrue(viewRect.width() > 0)
        assertTrue(viewRect.height() > 0)
        assertTrue(viewRect.contains(urlViewRect))
        assertTrue(viewRect.contains(reloadViewRect))
        assertTrue(viewRect.contains(readerViewRect))
        assertTrue(viewRect.contains(iconViewRect))
        assertEquals(iconViewRect.width() + urlViewRect.width() + reloadViewRect.width() + readerViewRect.width(), viewRect.width())

        assertEquals(view.measuredWidth, viewRect.width())
        assertEquals(1024, viewRect.width())
    }

    @Test
    fun `url box is not drawn behind browser actions or menu`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.addBrowserAction(BrowserToolbar.Button(mock(), "Tabs") {})
        displayToolbar.menuBuilder = BrowserMenuBuilder(emptyList())

        val view = TextView(testContext)
        displayToolbar.urlBoxView = view

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)
        displayToolbar.layout(0, 0, 1024, 200)

        val menuView = extractMenuView(displayToolbar)
        val browserActionView = extractActionView(displayToolbar, "Tabs")!!
        val iconView = displayToolbar.siteSecurityIconView
        val urlView = displayToolbar.urlView

        val viewRect = Rect(view.left, view.top, view.right, view.bottom)
        val urlViewRect = Rect(urlView.left, urlView.top, urlView.right, urlView.bottom)
        val browserActionViewRect = Rect(browserActionView.left, browserActionView.top, browserActionView.right, browserActionView.bottom)
        val menuViewRect = Rect(menuView.left, menuView.top, menuView.right, menuView.bottom)
        val iconViewRect = Rect(iconView.left, iconView.top, iconView.right, iconView.bottom)

        assertTrue(viewRect.width() > 0)
        assertTrue(viewRect.height() > 0)

        assertTrue(viewRect.contains(urlViewRect))
        assertTrue(viewRect.contains(iconViewRect))

        assertFalse(viewRect.contains(browserActionViewRect))
        assertFalse(viewRect.contains(menuViewRect))

        // 1024 (width) - 200 (browser action) - 200 (menu) = 624
        assertEquals(624, viewRect.width())
    }

    @Test
    fun `titleView does not display when there is no title text`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)
        displayToolbar.layout(0, 0, 1024, 200)

        val urlView = displayToolbar.urlView
        val titleView = displayToolbar.titleView

        val urlViewRect = Rect(urlView.left, urlView.top, urlView.right, urlView.bottom)
        val titleViewRect = Rect(titleView.left, titleView.top, titleView.right, titleView.bottom)

        assertTrue(urlViewRect.width() > 0)
        assertTrue(urlViewRect.height() > 0)

        assertTrue(titleViewRect.width() == 0)
        assertTrue(titleViewRect.height() == 0)
        assertEquals(titleView.visibility, View.GONE)
    }

    @Test
    fun `titleView is properly laid out when there is title text`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.updateTitle("Mozilla")
        assertEquals(displayToolbar.titleView.visibility, View.VISIBLE)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)
        displayToolbar.layout(0, 0, 1024, 200)

        val urlView = displayToolbar.urlView
        val titleView = displayToolbar.titleView

        val urlViewRect = Rect(urlView.left, urlView.top, urlView.right, urlView.bottom)
        val titleViewRect = Rect(titleView.left, titleView.top, titleView.right, titleView.bottom)

        assertTrue(urlViewRect.width() > 0)
        assertTrue(urlViewRect.height() > 0)

        assertTrue(titleViewRect.width() > 0)
        assertTrue(titleViewRect.height() > 0)

        val totalTextHeights = urlViewRect.height() + titleViewRect.height()
        val totalAvailablePadding = 200 - totalTextHeights
        val padding = totalAvailablePadding / DisplayToolbar.MEASURED_HEIGHT_DENOMINATOR

        // 132 = 200 * (2 / 3), since we want the title view and url to be centered as a singular unit.
        assertTrue(totalTextHeights == 132)
        assertTrue(titleViewRect.left == urlViewRect.left)
        assertTrue(titleViewRect.top == padding)
        assertTrue(titleViewRect.right == urlViewRect.right)
        assertTrue(titleViewRect.bottom == padding + titleViewRect.height())
    }

    @Test
    fun `titleView in displayToolbar is not ellipsized`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        val titleView = displayToolbar.titleView

        assertNull(titleView.ellipsize)
    }

    @Test
    fun `urlView is properly laid out when a title is shown`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.updateTitle("Mozilla")

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)
        displayToolbar.layout(0, 0, 1024, 200)

        val urlView = displayToolbar.urlView
        val titleView = displayToolbar.titleView

        val titleViewRect = Rect(titleView.left, titleView.top, titleView.right, titleView.bottom)
        val urlViewRect = Rect(urlView.left, urlView.top, urlView.right, urlView.bottom)

        val totalTextHeights = urlViewRect.height() + titleViewRect.height()
        val totalAvailablePadding = 200 - totalTextHeights
        val padding = totalAvailablePadding / DisplayToolbar.MEASURED_HEIGHT_DENOMINATOR

        assertTrue(urlViewRect.width() > 0)
        assertTrue(urlViewRect.height() > 0)

        assertTrue(titleViewRect.width() > 0)
        assertTrue(titleViewRect.height() > 0)

        assertTrue(urlViewRect.left == titleViewRect.left)
        assertTrue(urlViewRect.top == padding + titleViewRect.height())
        assertTrue(urlViewRect.right == titleView.right)
        assertTrue(urlViewRect.bottom == padding + titleViewRect.height() + urlViewRect.height())
    }

    @Test
    fun `toolbar only switches to editing mode if onUrlClicked returns true`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.urlView.performClick()

        verify(toolbar).editMode()

        reset(toolbar)
        displayToolbar.onUrlClicked = { false }
        displayToolbar.urlView.performClick()

        verify(toolbar, never()).editMode()

        reset(toolbar)
        displayToolbar.onUrlClicked = { true }
        displayToolbar.urlView.performClick()

        verify(toolbar).editMode()
    }

    @Test
    fun `urlView delegates long click when set`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        var longUrlClicked = false

        displayToolbar.setOnUrlLongClickListener {
            longUrlClicked = true
            false
        }

        assertFalse(longUrlClicked)
        displayToolbar.urlView.performLongClick()
        assertTrue(longUrlClicked)
    }

    @Test
    fun `urlView longClickListener can be unset`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        var longClicked = false
        displayToolbar.setOnUrlLongClickListener {
            longClicked = true
            true
        }

        displayToolbar.urlView.performLongClick()
        assertTrue(longClicked)
        longClicked = false

        displayToolbar.setOnUrlLongClickListener(null)
        displayToolbar.urlView.performLongClick()

        assertFalse(longClicked)
    }

    @Test
    fun `iconView changes image resource when site security changes`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)
        var shadowDrawable = shadowOf(displayToolbar.siteSecurityIconView.drawable)
        assertEquals(R.drawable.mozac_ic_globe, shadowDrawable.createdFromResId)

        displayToolbar.setSiteSecurity(SiteSecurity.SECURE)

        shadowDrawable = shadowOf(displayToolbar.siteSecurityIconView.drawable)
        assertEquals(R.drawable.mozac_ic_lock, shadowDrawable.createdFromResId)

        displayToolbar.setSiteSecurity(SiteSecurity.INSECURE)

        shadowDrawable = shadowOf(displayToolbar.siteSecurityIconView.drawable)
        assertEquals(R.drawable.mozac_ic_globe, shadowDrawable.createdFromResId)
    }

    @Test
    fun `securityIconColor is set when securityIconColor changes`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(testContext, toolbar)

        displayToolbar.securityIconColor = Pair(R.color.photonBlue40, R.color.photonBlue40)

        assertEquals(R.color.photonBlue40, displayToolbar.securityIconColor.first)
        assertEquals(R.color.photonBlue40, displayToolbar.securityIconColor.second)
    }

    @Test
    fun `setSiteSecurity is called when securityIconColor changes`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.displayToolbar

        assertNull(toolbar.displayToolbar.siteSecurityIconView.colorFilter)

        toolbar.siteSecurityColor = Pair(R.color.photonBlue40, R.color.photonBlue40)

        assertNotNull(toolbar.displayToolbar.siteSecurityIconView.colorFilter)
    }

    @Test
    fun `clicking menu button emits facts with additional extras from builder set`() {
        CollectionProcessor.withFactCollection { facts ->
            val toolbar = mock(BrowserToolbar::class.java)
            val displayToolbar = DisplayToolbar(testContext, toolbar)
            val menuView = extractMenuView(displayToolbar)

            val menuBuilder = BrowserMenuBuilder(listOf(SimpleBrowserMenuItem("Mozilla")), mapOf(
                "customTab" to true,
                "test" to "23"
            ))
            displayToolbar.menuBuilder = menuBuilder

            assertEquals(0, facts.size)

            menuView.performClick()

            assertEquals(1, facts.size)

            val fact = facts[0]

            assertEquals(Component.BROWSER_TOOLBAR, fact.component)
            assertEquals(Action.CLICK, fact.action)
            assertEquals("menu", fact.item)
            assertNull(fact.value)

            assertNotNull(fact.metadata)

            val metadata = fact.metadata!!
            assertEquals(2, metadata.size)
            assertTrue(metadata.containsKey("customTab"))
            assertTrue(metadata.containsKey("test"))
            assertEquals(true, metadata["customTab"])
            assertEquals("23", metadata["test"])
        }
    }

    @Test
    fun `clicking on site security indicator invokes listener`() {
        var listenerInvoked = false

        val toolbar = BrowserToolbar(testContext)

        assertNull(toolbar.displayToolbar.siteSecurityIconView.background)

        toolbar.setOnSiteSecurityClickedListener {
            listenerInvoked = true
        }

        assertNotNull(toolbar.displayToolbar.siteSecurityIconView.background)

        toolbar.displayToolbar.siteSecurityIconView.performClick()

        assertTrue(listenerInvoked)

        listenerInvoked = false

        toolbar.setOnSiteSecurityClickedListener { }

        assertNotNull(toolbar.displayToolbar.siteSecurityIconView.background)

        toolbar.displayToolbar.siteSecurityIconView.performClick()

        assertFalse(listenerInvoked)

        toolbar.setOnSiteSecurityClickedListener(null)

        assertNull(toolbar.displayToolbar.siteSecurityIconView.background)
    }

    companion object {
        private fun extractUrlView(displayToolbar: DisplayToolbar): TextView =
            extractView(displayToolbar) {
                it?.id == R.id.mozac_browser_toolbar_url_view
            } ?: throw AssertionError("Could not find URL view")

        private fun extractProgressView(displayToolbar: DisplayToolbar): ProgressBar =
            extractView(displayToolbar) ?: throw AssertionError("Could not find progress bar")

        private fun extractIconView(displayToolbar: DisplayToolbar): ImageView =
            extractView(displayToolbar) ?: throw AssertionError("Could not find icon view")

        private fun extractMenuView(displayToolbar: DisplayToolbar): MenuButton =
            extractView(displayToolbar) ?: throw AssertionError("Could not find menu view")

        private fun extractActionView(
            displayToolbar: DisplayToolbar,
            contentDescription: String
        ): ImageButton? = extractView(displayToolbar) {
            it?.contentDescription == contentDescription
        }

        private inline fun <reified T> extractView(
            displayToolbar: DisplayToolbar,
            otherCondition: (T) -> Boolean = { true }
        ): T? {
            displayToolbar.forEach {
                if (it is T && otherCondition(it)) {
                    return it
                }
            }
            return null
        }
    }
}

infix fun View.assertIn(group: ViewGroup) {
    group.forEach {
        if (this == it) {
            println("Checking $this == $it")
            return
        }
    }

    throw AssertionError("View not found in ViewGroup")
}

infix fun View.assertNotIn(group: ViewGroup) {
    group.forEach {
        if (this == it) {
            throw AssertionError("View should not be in ViewGroup")
        }
    }
}
