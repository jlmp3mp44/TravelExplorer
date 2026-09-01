package com.travel.explorer.ui.trip;

import com.fasterxml.jackson.databind.ser.Serializers.Base;
import com.microsoft.playwright.Page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.travel.explorer.BasePage;
import java.time.LocalDate;

public class TripPage extends BasePage {

  private final Locator startDate;
  private final Locator endDate;
  private final Locator continueButton;
  private final Locator pageTitle;
  private final Locator travelDatesTitle;

  public TripPage(Page page) {
    super(page);
    this.startDate = page.locator("input[type='date']").nth(0);
    this.endDate = page.locator("input[type='date']").nth(1);
    this.continueButton = page.getByRole(
        AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Continue")
    );
    this.pageTitle = page.locator("h1.trip-page__title");
    this.travelDatesTitle = page.locator("h2.trip-step-title");
  }

  public TripPage setStartDate(LocalDate date) {
    startDate.fill(date.toString());
    return this;
  }

  public TripPage setEndDate(LocalDate date) {
    endDate.fill(date.toString());
    return this;
  }

  public void clickContinue() {
    continueButton.click();
  }

  public boolean isPageTitleVisible() {
    return pageTitle.isVisible();
  }

  public boolean isTravelDatesTitleVisible() {
    return travelDatesTitle.isVisible();

  }

  public void waitForPageTitle() {
    pageTitle.waitFor(
        new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
    );
  }
}