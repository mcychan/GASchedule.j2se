package hk.edu.gaSchedule.algorithm;

public class Reservation {
	private int nr;
	private int day;
	private int time;
	private int room;
	
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

	public int index() {
		return day * nr * Constant.DAY_HOURS + room * Constant.DAY_HOURS + time;
	}		
	
}
