/**
 * Searches stand-up meetings wiki pages and counts the attendance
 *
 * User: Hubert Gajewski <hubert@hubertgajewski.com>, Aviary.pl
 * Date: 22.05.2012
 * Time: 01:20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package standup

import com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl
import org.xml.sax.InputSource

import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import java.text.SimpleDateFormat

class StandUpTools {

    final static String DATE_FORMAT = "yyyy-MM-dd"
    final static String AVIARY_PL_WIKI_URL = "http://wiki.aviary.pl"
    final static Locale AVIARY_PL_WIKI_LOCALE = new Locale("pl", "PL")
    static XPath xpath = XPathFactory.newInstance().newXPath()

    /**
     * Checks if stand-up date comes from the future
     * @param standUpUrlText A string containing the date in format "yyyy-MM-dd"
     * @return true if the date comes from the future, otherwise false
     */
    static boolean isFutureDate(String standUpUrlText, String lastStandUpDate) {
        return (new SimpleDateFormat(DATE_FORMAT).parse(standUpUrlText.split(":").last().toString()) >
                new SimpleDateFormat(DATE_FORMAT).parse(lastStandUpDate))
    }

    /**
     * Takes stand-up links from category page
     * @param standUpListUrl URL to list of stand-up links (category page)
     * @param firstStandUpDate First date which should be counted
     * @return List of stand-up links
     */
    static List<String> getStandUpsList(URL standUpListUrl, String firstStandUpDate = null, String lastStandUpDate =
            new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime())) {
        List<String> standUpList = []

        // Reading category page to get list of stand-up URLs
        if (firstStandUpDate != null) {
            standUpListUrl = (standUpListUrl.toString() + /&pagefrom=Stand-up%3A$firstStandUpDate/).toURL()
        }

        // Saving stand-up links
        while (standUpListUrl != null) {

            // Reading the category page to get a list of stand-up URLs
            xpath.evaluate('//*[@id="mw-pages"]//ul/li/a[contains(text(),"Stand-up:")]/@href',
                    new InputSource(standUpListUrl.toString()), XPathConstants.NODESET).each {
                // Future stand-up meetings will be omitted
                if (!isFutureDate(it.getTextContent(), lastStandUpDate)) {
                    standUpList << AVIARY_PL_WIKI_URL + it.getTextContent()
                }
            }

            // Getting the URL with additional links
            def nextItems = xpath.evaluate('//*[@id="mw-pages"]/a[contains(@href,"pagefrom")][1]/@href',
                    new InputSource(standUpListUrl.toString()), XPathConstants.NODESET).each {
                standUpListUrl = (AVIARY_PL_WIKI_URL + it.getTextContent()).toURL()
            }

            // If there are no additional pages with links - nextItems length equals 0
            if (nextItems.properties.length < 1) {
                standUpListUrl = null
            }
        }

        return standUpList
    }

    /**
     * Returns stand-up attendees
     * @param standUpsList List of stand-up links
     * @param silentMode if true the method does not display information on console
     * @return Two-dimensional list with stand-up attendees
     */
    static List<List<String>> getStandUpAttendees(List<String> standUpsList, boolean silentMode = false) {
        List<List<String>> attendees = [][]
        standUpsList.eachWithIndex() { standUps, i ->
            silentMode ?: println("Fetching data from " + standUpsList[i])
            if (standUpsList[i].toString().contains("2007-06-17")) {
                // First stand-up meeting (2007-06-17) does not contain a list of attendees
                attendees.add(["Adrianer", "Gandalf", "Marcoos", "Nikdo", "Zwierz"])

            } else {
                xpath.evaluate('//h2//a/text()', new InputSource(standUpsList[i].toString()), XPathConstants.NODESET)
                        .eachWithIndex() { attendee, j ->
                    if (attendees[i] == null) {
                        attendees.add([attendee.getTextContent()])
                    } else {
                        attendees[i][j] = attendee.getTextContent()
                    }
                }
            }
        }

        return attendees
    }

    /**
     * Counts stand-up attendance
     * @param attendees Two-dimensional list with stand-up attendees
     * @return Stand-up attendance
     */
    static LinkedHashMap countAttendance(List attendees) {
        def LinkedHashMap attendance = [:]

        attendees.eachWithIndex { obj1, i ->
            attendees[i].eachWithIndex { obj2, j ->
                if (attendance.containsKey(attendees[i][j])) {
                    attendance.putAt(attendees[i][j], ++attendance.getAt(attendees[i][j]))
                } else {
                    attendance.putAt(attendees[i][j], 1)
                }
            }
        }

        return attendance
    }

    /**
     * Returns template URL valid for provided standUpDate
     * @param standUpDate Stand-up meeting date
     * @return Template URL
     */
    static String getTemplateUrl(String standUpDate) {
        // TODO: check if all changes are displayed (check "mw-lastlink"). If not - the limit should be extended
        String url = (AVIARY_PL_WIKI_URL + "/index.php?title=Szablon:Standup&limit=1000&action=history")
        DeferredElementNSImpl historicStandUpTemplate = null
        for (link in xpath.evaluate('//*[@id="pagehistory"]//*[@class="mw-changeslist-date"]',
                new InputSource(url), XPathConstants.NODESET)) {
            historicStandUpTemplate = (DeferredElementNSImpl) link
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM yyyy", AVIARY_PL_WIKI_LOCALE)
            Date d = sdf.parse(historicStandUpTemplate.getTextContent())
            sdf.applyPattern(DATE_FORMAT)
            if (new SimpleDateFormat(DATE_FORMAT).parse(standUpDate) > d) {
                return historicStandUpTemplate.getAttribute('href')
            }

        }
        return historicStandUpTemplate?.getAttribute('href')
    }

    /**
     * Returns list of attendees placed in the template valid for provided standUpDate
     * @param standUpDate Stand-up meeting date
     * @return List of attendees
     */
    static List<String> getTemplateAttendees(String standUpDate = new SimpleDateFormat(DATE_FORMAT)
            .format(Calendar.getInstance().getTime()).toString()) {
        List<String> attendees = []
        String tempList = getTemplateUrl(standUpDate)
        String url = (AVIARY_PL_WIKI_URL + tempList)
        xpath.evaluate('//h2//a/text()', new InputSource(url), XPathConstants.NODESET).eachWithIndex() {
            attendee, i ->
                if (attendees[i] == null) {
                    attendees.add(attendee.getTextContent())
                } else {
                    attendees[i] = attendee.getTextContent()
                }
        }
        return attendees
    }
}