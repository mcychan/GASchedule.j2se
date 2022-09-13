package hk.edu.gaSchedule.model;

import java.util.HashMap;
import java.util.Map;

public class Reservation {
	private static Map<Integer, Reservation> _reservationPool = new HashMap<>();
	
	private static int NR;
	private final int day;
	private final int time;
	private final int room;
	
	private Reservation(int day, int time, int room) {		
		this.day = day;
		this.time = time;
		this.room = room;
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
		Reservation reservation = _reservationPool.get(hashCode);
		if(reservation == null) {
			final int day = hashCode / (Constant.DAY_HOURS * NR);
			final int hashCode2 = hashCode - (day * Constant.DAY_HOURS * NR);
		    final int room = hashCode2 / Constant.DAY_HOURS;
		    final int time = hashCode2 % Constant.DAY_HOURS;
			reservation = new Reservation(day, time, room);
			_reservationPool.put(hashCode, reservation);
		}
		return reservation;
	}
	
	private static int hashCode(int day, int time, int room)
	{
		return day * Constant.DAY_HOURS * NR + room * Constant.DAY_HOURS + time;
	}
	
	public static Reservation getReservation(int nr, int day, int time, int room)
	{
		if(nr != NR && nr > 0) {
			NR = nr;
			_reservationPool.clear();
		}
		
		int hashCode = hashCode(day, time, room);
		Reservation reservation = getReservation(hashCode);
		if(reservation == null) {
			reservation = new Reservation(day, time, room);
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
		return hashCode(day, time, room);
	}
	
}
