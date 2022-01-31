package hk.edu.gaSchedule;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import hk.edu.gaSchedule.algorithm.Configuration;
import hk.edu.gaSchedule.algorithm.Constant;
import hk.edu.gaSchedule.algorithm.CourseClass;
import hk.edu.gaSchedule.algorithm.Reservation;
import hk.edu.gaSchedule.algorithm.Room;
import hk.edu.gaSchedule.algorithm.Schedule;

public class JsonOutput {
	private final static int ROOM_COLUMN_NUMBER = Constant.DAYS_NUM + 1;
    private final static int ROOM_ROW_NUMBER = Constant.DAY_HOURS + 1;

	private static char[] CRITERIAS = { 'R', 'S', 'L', 'P', 'G'};
	private static String[] CRITERIAS_DESCR = { "Current room has %soverlapping", "Current room has %senough seats", "Current room with %senough computers if they are required",
		"Professors have %soverlapping classes", "Student groups has %soverlapping classes" };
	private static String[] PERIODS = {"", "9 - 10", "10 - 11", "11 - 12", "12 - 13", "13 - 14", "14 - 15", "15 - 16", "16 - 17", "17 - 18", "18 - 19", "19 - 20", "20 - 21" };
	private static String[] WEEK_DAYS = { "MON", "TUE", "WED", "THU", "FRI"};

	private static String getRoomJson(Room room)
	{
		StringBuilder sb = new StringBuilder("\"Room ");
		sb.append(room.Id).append("\": ");
		sb.append(Configuration.getGson().toJson(room));
		return sb.toString();
	}

	private static Map<Point, String[]> generateTimeTable(Schedule solution, Map<Point, int[]> slotTable)
	{
		int ci = 0;
		Map<CourseClass, Reservation> classes = solution.getClasses();

		Map<Point, String[]> timeTable = new HashMap<>();
		for (CourseClass cc : classes.keySet())
		{
			// coordinate of time-space slot
			Reservation reservation = classes.get(cc);
			int day = reservation.getDay() + 1;
			int time = reservation.getTime() + 1;
			int room = reservation.getRoom();

			Point key = new Point(time, room);
			int[] roomDuration = slotTable.containsKey(key) ? slotTable.get(key) : null;
			if (roomDuration == null)
			{
				roomDuration = new int[ROOM_COLUMN_NUMBER];
				slotTable.put(key, roomDuration);
			}
			roomDuration[day] = cc.Duration;
			for (int m = 1; m < cc.Duration; ++m)
			{
				Point nextRow = new Point(time + m, room);
				if (!slotTable.containsKey(nextRow))
					slotTable.put(nextRow, new int[ROOM_COLUMN_NUMBER]);
				if (slotTable.get(nextRow)[day] < 1)
					slotTable.get(nextRow)[day] = -1;
			}

			String[] roomSchedule = timeTable.containsKey(key) ? timeTable.get(key) : null;
			StringBuilder sb = new StringBuilder();
			if (roomSchedule == null) {
				roomSchedule = new String[ROOM_COLUMN_NUMBER];
				timeTable.put(key, roomSchedule);
			}
			sb.append("\"Course\": \"").append(cc.Course.Name).append("\"");
			sb.append(", \"Professor\": \"").append(cc.Professor.Name).append("\"");
			sb.append(", \"Groups\": \"").append(String.join("/", cc.Groups.stream().map(grp -> grp.Name).collect(Collectors.toList())));
			sb.append("\", ");
			if (cc.LabRequired)
				sb.append("\"Lab\": true, ");
			sb.append("\"Remarks\": [");

			for (int i=0; i< CRITERIAS.length; ++i)
            {
				sb.append("{");
				if(solution.getCriteria()[ci + i])
                {
					sb.append("\"Ok\": \"");
					sb.append(String.format(CRITERIAS_DESCR[i], (i == 1 || i == 2) ? "" : "no ")).append("\"");
				}
				else
                {
					sb.append("\"Fail\": \"");
					sb.append(String.format(CRITERIAS_DESCR[i], (i == 1 || i == 2) ? "not " : "")).append("\"");
				}
				sb.append(", \"Code\": \"").append(CRITERIAS[i]).append("\"");
				sb.append("}");

				if(i < CRITERIAS.length - 1)
					sb.append(", ");
			}
			sb.append("]");
			roomSchedule[day] = sb.toString();
			ci += CRITERIAS.length;
		}
		return timeTable;
	}

	private static String getCell(String content, int duration)
    {
		if (duration == 0)
			return "{}";

		if (content == null)
			return "{}";

		StringBuilder sb = new StringBuilder("{");
		sb.append(content);
		sb.append(", \"Duration\": ").append(duration);
		return sb + "}";
	}

	public static String getResult(Schedule solution)
	{
		StringBuilder sb = new StringBuilder();
		int nr = solution.getConfiguration().getNumberOfRooms();

		Map<Point, int[]> slotTable = new HashMap<>();
		Map<Point, String[]> timeTable = generateTimeTable(solution, slotTable); // Point.X = time, Point.Y = roomId
		if (slotTable.isEmpty() || timeTable.isEmpty())
			return "";

		for (int k = 0; k < nr; k++)
		{
			Room room = solution.getConfiguration().getRoomById(k);
			for (int j = 0; j < ROOM_ROW_NUMBER; ++j)
			{
				if (j == 0)
				{
					if (k > 0)
						sb.append(", ");
					sb.append(getRoomJson(room));						
				}
				else
				{
					Point key = new Point(j, k);
					int[] roomDuration = slotTable.containsKey(key) ? slotTable.get(key) : null;
					String[] roomSchedule = timeTable.containsKey(key) ? timeTable.get(key) : null;
					sb.append("\"Room ").append(room.Id).append(" (");
					sb.append(PERIODS[j]).append(")\": {");
					for (int i = 0; i < ROOM_COLUMN_NUMBER; ++i)
					{
						if (i == 0)
							continue;

						if (roomSchedule == null && roomDuration == null)
							continue;

						String content = (roomSchedule != null) ? roomSchedule[i] : null;
						sb.append("\"").append(WEEK_DAYS[i - 1]).append("\": ");
						sb.append(getCell(content, roomDuration[i]));

						if (i < ROOM_COLUMN_NUMBER - 1)
							sb.append(", ");
					}
					sb.append("}");
				}

				if (j < ROOM_ROW_NUMBER - 1)
					sb.append(", ");
			}
		}

		return "{" + sb + "}";
	}

}
