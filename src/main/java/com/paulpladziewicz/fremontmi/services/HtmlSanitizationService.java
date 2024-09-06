package com.paulpladziewicz.fremontmi.services;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizationService {

    public String sanitizeHtml(String html) {
        Safelist safelist = new Safelist()
                .addTags("br");

        return Jsoup.clean(html, safelist);
    }
}
