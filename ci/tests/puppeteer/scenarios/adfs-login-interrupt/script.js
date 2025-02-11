const puppeteer = require('puppeteer');
const assert = require('assert');
const cas = require('../../cas.js');

(async () => {
    const service = "https://apereo.github.io";
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    console.log(`Navigating to ${service}`);
    await cas.goto(page, `https://localhost:8443/cas/login?service=${service}`);
    await page.waitForTimeout(2000)
    await cas.click(page, "div .idp span");
    await page.waitForTimeout(4000)
    await cas.type(page, "#userNameInput", process.env.ADFS_USERNAME, true);
    await cas.type(page, "#passwordInput", process.env.ADFS_PASSWORD, true);
    await page.waitForTimeout(1000);
    await cas.submitForm(page, "#loginForm");
    await page.waitForTimeout(3000);
    await cas.assertTextContent(page, "#content h1", "Authentication Interrupt")
    await cas.assertTextContentStartsWith(page, "#content p", "The authentication flow has been interrupted");
    await cas.assertTextContentStartsWith(page, "#interruptMessage", "We interrupted your login");
    await cas.submitForm(page, "#fm1");

    let ticket = await cas.assertTicketParameter(page);
    await cas.goto(page, "https://localhost:8443/cas/login");
    await cas.assertTicketGrantingCookie(page);
    await page.waitForTimeout(3000);
    body = await cas.doRequest(`https://localhost:8443/cas/p3/serviceValidate?service=${service}&ticket=${ticket}&format=JSON`);
    console.log(body)
    let json = JSON.parse(body);
    let authenticationSuccess = json.serviceResponse.authenticationSuccess;
    assert(authenticationSuccess.user.includes("casuser@apereo.org"));
    assert(authenticationSuccess.attributes.firstname != null);
    assert(authenticationSuccess.attributes.lastname != null);
    assert(authenticationSuccess.attributes.uid != null);
    assert(authenticationSuccess.attributes.upn != null);
    assert(authenticationSuccess.attributes.username != null);
    assert(authenticationSuccess.attributes.surname != null);
    assert(authenticationSuccess.attributes.email != null);
    await browser.close();
})();
