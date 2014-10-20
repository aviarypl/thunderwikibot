/**
 * Displays stand-up meetings attendance (by default for the last 6 months)
 * User: Hubert Gajewski <hubert@hubertgajewski.com>, Aviary.pl
 * Date: 22.05.2012
 * Time: 01:22
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

import static standup.StandUpTools.yesterdayDate

import java.text.DecimalFormat
import java.text.SimpleDateFormat

if (args.length == 0) {
    // Setting today's date minus 6 months
    Calendar cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, (cal.get(Calendar.MONTH) - 6))
    args = [new SimpleDateFormat(StandUpTools.DATE_FORMAT).format(cal.getTime()).toString()]

} else if (args.length == 1) {
    println("Preparing stand-up meetings attendance report (data since " + args[0] + ")")

} else {
    println("Preparing stand-up meetings attendance report (data from " + args[0] + " to " + args[1] + ")")
}
List standUpsList = StandUpTools.getStandUpsList((StandUpTools.AVIARY_PL_WIKI_URL +
        "/index.php?title=Kategoria:Stand-up").toURL(), *args)
List<List<String>> attendees = StandUpTools.getStandUpAttendees(standUpsList)
List<String> teamFirstDay = StandUpTools.getTemplateAttendees(args[0])
List<String> teamLastDay = StandUpTools.getTemplateAttendees()
int teamSizeFirstDay = teamFirstDay.size()
int teamSizeLastDay = teamLastDay.size()
LinkedHashMap attendance = StandUpTools.countAttendance(attendees)
Map<String, String> enoughAttendancePeople = attendance.findAll { it.value >= standUpsList.size() / 4 }
Map<String, String> lowAttendancePeople = attendance.findAll { it.value < standUpsList.size() / 4 }
List<String> zeroAttendancePeople = teamFirstDay.findAll { !attendees.toListString().contains(it) }
DecimalFormat percentFormat = new DecimalFormat("###%")

println("\nPEOPLE")
println("Team members (${args[0]}): " + teamFirstDay)
println("Team members (${args.size() == 2 ? args[1] : yesterdayDate}): " + teamLastDay)
println("Attendees: " + attendance)
println("Enough attendance: " + enoughAttendancePeople)
println("Low attendance: " + lowAttendancePeople)
println("Zero attendance: " + zeroAttendancePeople)

println("\nNUMBERS")
println("Stand-up meetings: " + standUpsList.size())
println("Team members (${args[0]}): " + teamSizeFirstDay)
println("Team members (${args.size() == 2 ? args[1] : yesterdayDate}): " + teamSizeLastDay)
println("Attendees: " + attendance.size())
println("Enough attendance: ${enoughAttendancePeople.size()} " +
        "(~${percentFormat.format(enoughAttendancePeople.size() / teamSizeFirstDay)})")
println("Low attendance: ${lowAttendancePeople.size()}")
println("Zero attendance: ${zeroAttendancePeople.size()}")
