package hk.edu.gaSchedule.model;

import java.util.HashMap;
import java.util.Map;

public class Reservation {
	private static Map<Integer, Reservation> _reservationPool = new HashMap<>();
	
	private final int nr;
	private final int day;
	private final int time;
	private final int room;
	
	private Reservation(int nr, int day, int time, int room) {
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
	
	public static Reservation getReservation(int hashCode)
	{
		return _reservationPool.get(hashCode);
	}
	
	private static int hashCode(int nr, int day, int time, int room)
	{
		return day * nr * Constant.DAY_HOURS + room * Constant.DAY_HOURS + time;
	}
	
	public static Reservation getReservation(int nr, int day, int time, int room)
	{
		int hashCode = hashCode(nr, day, time, room);
		Reservation reservation = getReservation(hashCode);
		if(reservation == null) {
			reservation = new Reservation(nr, day, time, room);
			_reservationPool.put(hashCode, reservation);
		}
		return reservation;
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
		return hashCode(nr, day, time, room);
	}
	
}
