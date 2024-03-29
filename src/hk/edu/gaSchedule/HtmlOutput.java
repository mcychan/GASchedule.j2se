package hk.edu.gaSchedule;

import java.awt.Point;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import hk.edu.gaSchedule.model.Constant;
import hk.edu.gaSchedule.model.CourseClass;
import hk.edu.gaSchedule.model.Reservation;
import hk.edu.gaSchedule.model.Room;
import hk.edu.gaSchedule.model.Schedule;

public class HtmlOutput
{
	private static final int ROOM_COLUMN_NUMBER = Constant.DAYS_NUM + 1;
	private static final int ROOM_ROW_NUMBER = Constant.DAY_HOURS + 1;

	private static final String COLOR1 = "#319378";
	private static final String COLOR2 = "#CE0000";
	private static final char[] CRITERIAS = { 'R', 'S', 'L', 'P', 'G'};
	private static final String[] OK_DESCR = { "Current room has no overlapping", "Current room has enough seats", "Current room with enough computers if they are required",
		"Professors have no overlapping classes", "Student groups has no overlapping classes" };
	private static final String[] FAIL_DESCR = { "Current room has overlapping", "Current room has not enough seats", "Current room with not enough computers if they are required",
			"Professors have overlapping classes", "Student groups has overlapping classes" };
	private static final String[] PERIODS = {"", "9 - 10", "10 - 11", "11 - 12", "12 - 13", "13 - 14", "14 - 15", "15 - 16", "16 - 17", "17 - 18", "18 - 19", "19 - 20", "20 - 21" };
	private static final String[] WEEK_DAYS = { "MON", "TUE", "WED", "THU", "FRI"};

	private static String getTableHeader(Room room)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<tr><th style='border: .1em solid black' scope='col' colspan='2'>Room: ");
		sb.append(room.Name);
		sb.append("</th>\n");
		for(String weekDay : WEEK_DAYS)
		sb.append("<th style='border: .1em solid black; padding: .25em; width: 15%' scope='col' rowspan='2'>").append(weekDay).append("</th>\n");
		sb.append("</tr>\n");
		sb.append("<tr>\n");
		sb.append("<th style='border: .1em solid black; padding: .25em'>Lab: ").append(room.Lab ? "Yes" : "No").append("</th>\n");
		sb.append("<th style='border: .1em solid black; padding: .25em'>Seats: ").append(room.NumberOfSeats).append("</th>\n");
		sb.append("</tr>\n");
		return sb.toString();
	}
	
	private static String getCourseClass(final CourseClass cc, final boolean[] criterias, final int ci)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(cc.Course.Name).append("<br />").append(cc.Professor.Name).append("<br />");
		sb.append(String.join("/", cc.Groups.stream().map(grp -> grp.Name).collect(Collectors.toList())));
		sb.append("<br />");
		if (cc.LabRequired)
			sb.append("Lab<br />");

		for(int i=0; i< CRITERIAS.length; ++i)
		{
			sb.append("<span style='color:");
			if(criterias[ci + i])
			{
				sb.append(COLOR1).append("' title='");
				sb.append(OK_DESCR[i]);
			}
			else
			{
				sb.append(COLOR2).append("' title='");
				sb.append(FAIL_DESCR[i]);
			}
			sb.append("'> ").append(CRITERIAS[i]);
			sb.append(" </span>");
		}
		return sb.toString();
	}

	private static Map<Point, String[]> generateTimeTable(Schedule solution, Map<Point, int[]> slotTable)
	{
		int ci = 0;
		Map<CourseClass, Integer> classes = solution.getClasses();
		
		Map<Point, String[]> timeTable = new HashMap<>();
		
		for (CourseClass cc : classes.keySet())
		{
			// coordinate of time-space slot
			Reservation reservation = Reservation.getReservation(classes.get(cc));
			int dayId = reservation.getDay() + 1;
			int periodId = reservation.getTime() + 1;
			int roomId = reservation.getRoom();

			Point key = new Point(periodId, roomId);
			int[] roomDurations = slotTable.get(key);
			if (roomDurations == null)
			{
				roomDurations = new int[ROOM_COLUMN_NUMBER];
				slotTable.put(key, roomDurations);
			}
			roomDurations[dayId] = cc.Duration;
			for (int m = 1; m < cc.Duration; ++m)
			{
				Point nextRow = new Point(periodId + m, roomId);
				if (!slotTable.containsKey(nextRow))
					slotTable.put(nextRow, new int[ROOM_COLUMN_NUMBER]);
				if (slotTable.get(nextRow)[dayId] < 1)
					slotTable.get(nextRow)[dayId] = -1;
			}

			String[] roomSchedule = timeTable.get(key);
			if (roomSchedule == null) {
				roomSchedule = new String[ROOM_COLUMN_NUMBER];
				timeTable.put(key, roomSchedule);
			}
			roomSchedule[dayId] = getCourseClass(cc, solution.getCriteria(), ci);
			ci += CRITERIAS.length;
		}
		return timeTable;
	}

	private static String getHtmlCell(String content, int rowspan)
	{
		if (rowspan == 0)
			return "<td></td>";

		if (content == null)
			return "";

		StringBuilder sb = new StringBuilder();
		if (rowspan > 1)
			sb.append("<td style='border: .1em solid black; padding: .25em' rowspan='").append(rowspan).append("'>");
		else
			sb.append("<td style='border: .1em solid black; padding: .25em'>");

		sb.append(content);
		sb.append("</td>");
		return sb.toString();
	}

	public static String getResult(Schedule solution)
	{
		StringBuilder sb = new StringBuilder();
		int nr = solution.getConfiguration().getNumberOfRooms();

		Map<Point, int[]> slotTable = new HashMap<>();
		Map<Point, String[]> timeTable = generateTimeTable(solution, slotTable);
		if (slotTable.isEmpty() || timeTable.isEmpty())
			return "";

		for (int roomId = 0; roomId < nr; ++roomId)
		{
			Room room = solution.getConfiguration().getRoomById(roomId);
			for (int periodId = 0; periodId < ROOM_ROW_NUMBER; ++periodId)
			{
				if (periodId == 0)
				{
					sb.append("<div id='room_").append(room.Name).append("' style='padding: 0.5em'>\n");
					sb.append("<table style='border-collapse: collapse; width: 95%'>\n");
					sb.append(getTableHeader(room));
				}
				else
				{
					Point key = new Point(periodId, roomId);
					int[] roomDurations = slotTable.get(key);
					String[] roomSchedule = timeTable.get(key);
					sb.append("<tr>");
					for (int dayId = 0; dayId < ROOM_COLUMN_NUMBER; ++dayId)
					{
						if(dayId == 0)
						{
							sb.append("<th style='border: .1em solid black; padding: .25em' scope='row' colspan='2'>").append(PERIODS[periodId]).append("</th>\n");
							continue;
						}

						if (roomSchedule == null && roomDurations == null)
							continue;

						String content = (roomSchedule != null) ? roomSchedule[dayId] : null;
						sb.append(getHtmlCell(content, roomDurations[dayId]));
					}
					sb.append("</tr>\n");
				}

				if (periodId == ROOM_ROW_NUMBER - 1)
					sb.append("</table>\n</div>\n");
			}
		}

		return sb.toString();
	}

}
