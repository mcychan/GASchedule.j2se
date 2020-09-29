package hk.edu.gaSchedule.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CourseClass implements Comparable<CourseClass>
{
	// ID counter used to assign IDs automatically
    private static int _nextClassId = 0;
	// Initializes class object
	public CourseClass(Professor professor, Course course, boolean requiresLab, int duration, StudentsGroup... groups)
    {
		Id = _nextClassId++;
		Professor = professor;
		Course = course;
		NumberOfSeats = 0;
		LabRequired = requiresLab;
		Duration = duration;
		Groups = new ArrayList<StudentsGroup>();

		// bind professor to class
		Professor.addCourseClass(this);

		// bind student groups to class
		for(StudentsGroup group : groups)
        {
			group.addClass(this);
			Groups.add(group);
			NumberOfSeats += group.NumberOfStudents;
		}
	}

	// Returns TRUE if another class has one or overlapping student groups.
	public boolean groupsOverlap(CourseClass c)
    {
		return !Collections.disjoint(Groups, c.Groups);
    }

	// Returns TRUE if another class has same professor.
	public boolean professorOverlaps(CourseClass c) {
		return Professor.equals(c.Professor);
	}
	
	// Returns class ID - automatically assigned
    public int Id;

	// Return pointer to professor who teaches
	public Professor Professor;

	// Return pointer to course to which class belongs
	public Course Course;

	// Returns reference to list of student groups who attend class
	public List<StudentsGroup> Groups;

	// Returns number of seats (students) required in room
	public int NumberOfSeats;

	// Returns TRUE if class requires computers in room.
	public boolean LabRequired;

	// Returns duration of class in hours
	public int Duration;	

	// Restarts ID assigments
    public static void restartIDs() { _nextClassId = 0; }

	@Override
	public int compareTo(CourseClass that) {
		if(that == null)
			return -1;
	    return that.Id - Id;
	}
}

