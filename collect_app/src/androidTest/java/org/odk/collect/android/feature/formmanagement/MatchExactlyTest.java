package org.odk.collect.android.feature.formmanagement;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.CopyFormRule;
import org.odk.collect.android.support.NotificationDrawerRule;
import org.odk.collect.android.support.TestDependencies;
import org.odk.collect.android.support.TestRuleChain;
import org.odk.collect.android.support.pages.FillBlankFormPage;
import org.odk.collect.android.support.pages.MainMenuPage;
import org.odk.collect.android.support.pages.ServerAuthDialog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class MatchExactlyTest {

    final CollectTestRule rule = new CollectTestRule();
    final TestDependencies testDependencies = new TestDependencies();
    final NotificationDrawerRule notificationDrawerRule = new NotificationDrawerRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain(testDependencies)
            .around(notificationDrawerRule)
            .around(new CopyFormRule("one-question.xml"))
            .around(new CopyFormRule("one-question-repeat.xml"))
            .around(rule);

    @Test
    public void whenMatchExactlyEnabled_clickingFillBlankForm_andClickingRefresh_getsLatestFormsFromServer() {
        FillBlankFormPage page = rule.mainMenu()
                .setServer(testDependencies.server.getURL())
                .enableMatchExactly()
                .clickFillBlankForm()
                .assertText("One Question")
                .assertText("One Question Repeat");

        testDependencies.server.addForm("One Question Updated", "one_question", "one-question-updated.xml");
        testDependencies.server.addForm("Two Question", "two_question", "two-question.xml");

        page.clickRefresh()
                .assertText("Two Question") // Check new form downloaded
                .assertText("One Question Updated") // Check updated form updated
                .assertTextDoesNotExist("One Question Repeat"); // Check deleted form deleted
    }

    @Test
    public void whenMatchExactlyEnabled_getsLatestFormsFromServer_automaticallyAndRepeatedly() throws Exception {
        MainMenuPage page = rule.mainMenu()
                .setServer(testDependencies.server.getURL())
                .enableMatchExactly();

        testDependencies.server.addForm("One Question Updated", "one_question", "one-question-updated.xml");
        testDependencies.server.addForm("Two Question", "two_question", "two-question.xml");
        testDependencies.scheduler.runDeferredTasks();

        page = page.clickFillBlankForm()
                .assertText("Two Question")
                .assertText("One Question Updated")
                .assertTextDoesNotExist("One Question Repeat")
                .pressBack(new MainMenuPage(rule));

        testDependencies.server.removeForm("Two Question");
        testDependencies.scheduler.runDeferredTasks();

        page.assertOnPage()
                .clickFillBlankForm()
                .assertText("One Question Updated")
                .assertTextDoesNotExist("Two Question");
    }

    @Test
    public void whenMatchExactlyEnabled_andThereIsAnErrorDuringUpdate_clickingErrorNotification_goesToFillBlankForm() throws Exception {
        String url = testDependencies.server.getURL();

        rule.mainMenu()
                .setServer(url)
                .enableMatchExactly();

        testDependencies.server.alwaysReturnError();
        testDependencies.scheduler.runDeferredTasks();

        notificationDrawerRule
                .open()
                .clickNotification(
                        "ODK Collect",
                        "Form update failed",
                        "If you keep having this problem, report it to the person who asked you to collect data.",
                        "Fill Blank Form",
                        new FillBlankFormPage(rule)
                );
    }

    @Test
    public void whenMatchExactlyEnabled_andThereIsAnAuthenticationErrorDuringUpdate_clickingErrorNotification_goesToFillBlankForm_andPromptsForCredentials() throws Exception {
        String url = testDependencies.server.getURL();

        rule.mainMenu()
                .setServer(url)
                .enableMatchExactly();

        testDependencies.server.addForm("One Question Updated", "one_question", "one-question-updated.xml");
        testDependencies.server.setCredentials("Klay", "Thompson");
        testDependencies.scheduler.runDeferredTasks();

        notificationDrawerRule
                .open()
                .clickNotification(
                        "ODK Collect",
                        "Form update failed",
                        "If you keep having this problem, report it to the person who asked you to collect data.",
                        "Server Requires Authentication",
                        new ServerAuthDialog(rule)
                )
                .fillUsername("Klay")
                .fillPassword("Thompson")
                .clickOK(new FillBlankFormPage(rule))
                .clickRefresh()
                .assertText("One Question Updated");
    }

    @Test
    public void whenMatchExactlyEnabled_hidesGetBlankForms() {
        rule.mainMenu()
                .enableMatchExactly()
                .assertTextNotDisplayed(R.string.get_forms);
    }

    @Test
    public void whenMatchExactlyDisabled_stopsSyncingAutomatically() {
        rule.mainMenu()
                .setServer(testDependencies.server.getURL())
                .enableMatchExactly()
                .enableManualUpdates();

        assertThat(testDependencies.scheduler.getDeferredTasks(), is(empty()));
    }

}
