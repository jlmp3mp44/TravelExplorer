package com.travel.explorer.ui.trip;

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
  private final Locator stepTitle;
  private final Page page;
  private final Locator budgetAmount;
  private final Locator currency;
  private final Locator createTripButton;

  public TripPage(Page page) {
    super(page);
    this.page = page;
    this.startDate = page.locator("input[type='date']").nth(0);
    this.endDate = page.locator("input[type='date']").nth(1);
    this.continueButton = page.getByRole(
        AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Continue")
    );
    this.pageTitle = page.locator("h1.trip-page__title");
    this.stepTitle = page.locator("form").getByRole(
        AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
    this.budgetAmount = page.getByLabel("Amount", new Page.GetByLabelOptions().setExact(true));
    this.currency = page.getByLabel("Currency", new Page.GetByLabelOptions().setExact(true));
    this.createTripButton = page.locator("form").getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create trip").setExact(true));
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

  public void selectCountry(String country) {
    page.getByText("Tap to choose a country", new Page.GetByTextOptions().setExact(true)).click();
    page.getByText(country, new Page.GetByTextOptions().setExact(true)).click();
  }

  public void setBudget(int amount, String currencyCode) {
    budgetAmount.fill(Integer.toString(amount));
    currency.selectOption(currencyCode);
  }

  public void selectInterest(String category, String interest) {
    page.getByText(category, new Page.GetByTextOptions().setExact(true)).click();
    getInterest(interest).click();
  }

  public Locator getInterest(String interest) {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName(interest).setExact(true));
  }

  public void selectPace(String pace) {
    getPace(pace).click();
  }

  public Locator getPace(String pace) {
    return page.getByRole(AriaRole.RADIOGROUP,
            new Page.GetByRoleOptions().setName("Trip pace").setExact(true))
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(pace + ",").setExact(false));
  }

  public Locator getStepTitle() {
    return stepTitle;
  }

  public Locator getCreateTripButton() {
    return createTripButton;
  }

  public void clickCreateTrip() {
    createTripButton.click();
  }

  public Locator getCreatedTripTitle() {
    return page.locator(".trip-details-page").getByRole(
        AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public boolean isPageTitleVisible() {
    return pageTitle.isVisible();
  }

  public boolean isTravelDatesTitleVisible() {
    return stepTitle.isVisible();

  }

  public void waitForPageTitle() {
    pageTitle.waitFor(
        new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
    );
  }
}
