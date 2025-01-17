/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.Constants.POCKET_RECOMMENDED_STORIES_UTM_PARAM
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying the presence of home screen and first-run homescreen elements
 *
 *  Note: For private browsing, navigation bar and tabs see separate test class
 *
 */

class ComposeHomeScreenTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private lateinit var firstPocketStoryPublisher: String

    @get:Rule(order = 0)
    val activityTestRule =
        AndroidComposeTestRule(
            HomeActivityTestRule.withDefaultSettingsOverrides(
                tabsTrayRewriteEnabled = true,
            ),
        ) { it.activity }

    @Rule(order = 1)
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/235396
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1844580")
    @Test
    fun homeScreenItemsTest() {
        homeScreen {}.dismissOnboarding()
        homeScreen {
            verifyHomeWordmark()
            verifyHomePrivateBrowsingButton()
            verifyExistingTopSitesTabs("Wikipedia")
            verifyExistingTopSitesTabs("Top Articles")
            verifyExistingTopSitesTabs("Google")
            verifyCollectionsHeader()
            verifyNoCollectionsText()
            scrollToPocketProvokingStories()
            verifyThoughtProvokingStories(true)
            verifyStoriesByTopicItems()
            verifyCustomizeHomepageButton(true)
            verifyNavigationToolbar()
            verifyHomeMenuButton()
            verifyTabButton()
            verifyTabCounter("0")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/244199
    @Test
    fun privateBrowsingHomeScreenItemsTest() {
        homeScreen { }.dismissOnboarding()
        homeScreen { }.togglePrivateBrowsingMode()

        homeScreen {
            verifyPrivateBrowsingHomeScreen()
        }.openCommonMythsLink {
            verifyUrl("common-myths-about-private-browsing")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1364362
    @Test
    fun verifyJumpBackInSectionTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isRecentlyVisitedFeatureEnabled = false
            it.isPocketEnabled = false
        }

        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent(firstWebPage.content)
            verifyUrl(firstWebPage.url.toString())
        }.goToHomescreen {
            verifyJumpBackInSectionIsDisplayed()
            verifyJumpBackInItemTitle(activityTestRule, firstWebPage.title)
            verifyJumpBackInItemWithUrl(activityTestRule, firstWebPage.url.toString())
            verifyJumpBackInShowAllButton()
        }.clickJumpBackInShowAllButton(activityTestRule) {
            verifyExistingOpenTabs(firstWebPage.title)
        }.closeTabDrawer {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            verifyPageContent(secondWebPage.content)
            verifyUrl(secondWebPage.url.toString())
        }.goToHomescreen {
            verifyJumpBackInSectionIsDisplayed()
            verifyJumpBackInItemTitle(activityTestRule, secondWebPage.title)
            verifyJumpBackInItemWithUrl(activityTestRule, secondWebPage.url.toString())
        }.openComposeTabDrawer(activityTestRule) {
            closeTabWithTitle(secondWebPage.title)
        }.closeTabDrawer {
        }

        homeScreen {
            verifyJumpBackInSectionIsDisplayed()
            verifyJumpBackInItemTitle(activityTestRule, firstWebPage.title)
            verifyJumpBackInItemWithUrl(activityTestRule, firstWebPage.url.toString())
        }.openComposeTabDrawer(activityTestRule) {
            closeTab()
        }

        homeScreen {
            verifyJumpBackInSectionIsNotDisplayed()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1569867
    @Test
    fun verifyJumpBackInContextualHintTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isJumpBackInCFREnabled = true
        }

        val genericPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericPage.url) {
        }.goToHomescreen {
            verifyJumpBackInMessage(activityTestRule)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2252509
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1844580")
    @Test
    fun verifyPocketSectionTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isRecentTabsFeatureEnabled = false
            it.isRecentlyVisitedFeatureEnabled = false
        }

        homeScreen {
        }.dismissOnboarding()

        homeScreen {
            verifyThoughtProvokingStories(true)
            scrollToPocketProvokingStories()
            verifyPocketRecommendedStoriesItems()
            // Sponsored Pocket stories are only advertised for a limited time.
            // See also known issue https://bugzilla.mozilla.org/show_bug.cgi?id=1828629
            // verifyPocketSponsoredStoriesItems(2, 8)
            verifyDiscoverMoreStoriesButton()
            verifyStoriesByTopic(true)
            verifyPoweredByPocket()
        }.openThreeDotMenu {
        }.openCustomizeHome {
            clickPocketButton()
        }.goBackToHomeScreen {
            verifyThoughtProvokingStories(false)
            verifyStoriesByTopic(false)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2252513
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1844580")
    @Test
    fun openPocketStoryItemTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isRecentTabsFeatureEnabled = false
            it.isRecentlyVisitedFeatureEnabled = false
        }

        homeScreen {
        }.dismissOnboarding()

        homeScreen {
            verifyThoughtProvokingStories(true)
            scrollToPocketProvokingStories()
            firstPocketStoryPublisher = getProvokingStoryPublisher(1)
        }.clickPocketStoryItem(firstPocketStoryPublisher, 1) {
            verifyUrl(POCKET_RECOMMENDED_STORIES_UTM_PARAM)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2252514
    @Test
    fun pocketDiscoverMoreButtonTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isRecentTabsFeatureEnabled = false
            it.isRecentlyVisitedFeatureEnabled = false
        }

        homeScreen {
        }.dismissOnboarding()

        homeScreen {
            scrollToPocketProvokingStories()
            verifyDiscoverMoreStoriesButton()
        }.clickPocketDiscoverMoreButton {
            verifyUrl("getpocket.com/explore")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2252515
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1844580")
    @Test
    fun selectPocketStoriesByTopicTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isRecentTabsFeatureEnabled = false
            it.isRecentlyVisitedFeatureEnabled = false
        }

        homeScreen {
        }.dismissOnboarding()

        homeScreen {
            verifyStoriesByTopicItemState(activityTestRule, false, 1)
            clickStoriesByTopicItem(activityTestRule, 1)
            verifyStoriesByTopicItemState(activityTestRule, true, 1)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2252516
    @Test
    fun pocketLearnMoreButtonTest() {
        activityTestRule.activityRule.applySettingsExceptions {
            it.isRecentTabsFeatureEnabled = false
            it.isRecentlyVisitedFeatureEnabled = false
        }

        homeScreen {
        }.dismissOnboarding()

        homeScreen {
            verifyPoweredByPocket()
        }.clickPocketLearnMoreLink(activityTestRule) {
            verifyUrl("mozilla.org/en-US/firefox/pocket")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1569839
    @Test
    fun verifyCustomizeHomepageButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.goToHomescreen {
        }.openCustomizeHomepage {
            clickJumpBackInButton()
            clickRecentBookmarksButton()
            clickRecentSearchesButton()
            clickPocketButton()
        }.goBackToHomeScreen {
            verifyCustomizeHomepageButton(false)
        }.openThreeDotMenu {
        }.openCustomizeHome {
            clickJumpBackInButton()
        }.goBackToHomeScreen {
            verifyCustomizeHomepageButton(true)
        }
    }
}
