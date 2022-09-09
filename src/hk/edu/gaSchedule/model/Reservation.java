package hk.edu.gaSchedule.model;

public class Reservation {
	private final int nr;
	private final int day;
	private final int time;
	private final int room;
	
	public Reservation(int nr, int day, int time, int room) {
		this.nr = nr;
		this.day = day;
		this.time = time;
		this.room = room;
	}	

	public int getNr() {
		return nr;
	}

	public int getDay() {
		return day;
	}

	public int getTime() {
		return time;
	}

	public int getRoom() {
		return room;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		//Check for null and compare run-time types.
		if ((obj == null) || !this.getClass().equals(obj.getClass()))
			return false;

		Reservation other = (Reservation) obj;
		return hashCode() == other.hashCode();
	}

	@Override
	public int hashCode()
	{
		return day * nr * Constant.DAY_HOURS + room * Constant.DAY_HOURS + time;
	}
	
}
