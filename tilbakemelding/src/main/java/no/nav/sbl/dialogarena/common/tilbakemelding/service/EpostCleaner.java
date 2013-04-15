package no.nav.sbl.dialogarena.common.tilbakemelding.service;


import org.jsoup.Jsoup;


/*
* Class that cleans a string representing a epostbody for possible security threats (including html and line breaks)
*/
public class EpostCleaner {

    public static String cleanbody(String body) {
        return Jsoup.parse(body).text();
    }


}
