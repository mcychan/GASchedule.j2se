package hk.edu.gaSchedule.algorithm;

import java.util.ArrayList;
import java.util.List;

// Stores data about student group
public class StudentsGroup
{
	// Initializes student group data
	public StudentsGroup(int id, String name, int numberOfStudents)
    {
		Id = id;
		Name = name;
		NumberOfStudents = numberOfStudents;
		CourseClasses = new ArrayList<CourseClass>();
	}

	// Bind group to class
	public void addClass(CourseClass courseClass)
    {			
		CourseClasses.add(courseClass);
	}	

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StudentsGroup other = (StudentsGroup) obj;
		if (Id != other.Id)
			return false;
		return true;
	}

	// Returns student group ID
    public int Id;

	// Returns name of student group
	public String Name;

	// Returns number of students in group
	public int NumberOfStudents;

	// Returns reference to list of classes that group attends
	public List<CourseClass> CourseClasses;

}
