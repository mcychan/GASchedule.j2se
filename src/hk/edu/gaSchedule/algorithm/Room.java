package hk.edu.gaSchedule.algorithm;

// Stores data about classroom
public class Room
{
    // ID counter used to assign IDs automatically
    private static int _nextRoomId = 0;

    // Initializes room data and assign ID to room
    public Room(String name, boolean lab, int numberOfSeats)
    {
        Id = _nextRoomId++;
        Name = name;
        Lab = lab;
        NumberOfSeats = numberOfSeats;
    }

    // Returns room ID - automatically assigned
    public int Id;

    // Returns name
    public String Name;

    // Returns TRUE if room has computers otherwise it returns FALSE
    public boolean Lab;

    // Returns number of seats in room
    public int NumberOfSeats;

    // Restarts ID assigments
    public static void restartIDs() { _nextRoomId = 0; }
}
