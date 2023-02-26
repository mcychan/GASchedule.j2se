package hk.edu.gaSchedule.model;

import java.util.List;

public class Criteria {

	static boolean isRoomOverlapped(List<CourseClass>[] slots, Reservation reservation, int dur)
	{
		// check for room overlapping of classes
		for (int i = dur - 1; i >= 0; i--)
		{
			if (slots[reservation.hashCode() + i].size() > 1)
				return true;
		}
		return false;
	}
	
	static boolean isSeatEnough(Room r, CourseClass cc)
	{
		// does current room have enough seats
		return r.NumberOfSeats >= cc.NumberOfSeats;
	}
	
	static boolean isComputerEnough(Room r, CourseClass cc)
	{
		// does current room have computers if they are required
		return !cc.LabRequired || (cc.LabRequired && r.Lab);
	}
	
	static boolean[] isOverlappedProfStudentGrp(List<CourseClass>[] slots, CourseClass cc, int numberOfRooms, int timeId)
	{
		boolean po = false, go = false;
		
		int dur = cc.Duration;
		// check overlapping of classes for professors and student groups
		for (int i = numberOfRooms; i > 0; --i, timeId += Constant.DAY_HOURS)
		{
			// for each hour of class
			for (int j = dur - 1; j >= 0; --j)
			{				
				// check for overlapping with other classes at same time
				List<CourseClass> cl = slots[timeId + j];
				for (CourseClass cc1 : cl)
				{
					if (cc != cc1)
					{
						// professor overlaps?
						if (!po && cc.professorOverlaps(cc1))
							po = true;

						// student group overlaps?
						if (!go && cc.groupsOverlap(cc1))
							go = true;

						// both type of overlapping? no need to check more
						if (po && go)
							return new boolean[] {po, go};
					}
				}
			}
		}

		return new boolean[] {po, go};
	}
	
	public static final float[] weights = { 0f, .5f, .5f, 0f, 0f };
	
}
