package hk.edu.gaSchedule.model;

import java.util.ArrayList;
import java.util.List;

// Stores data about professor
public class Professor
{
    // Initializes professor data
    public Professor(int id, String name)  {
        Id = id;
        Name = name;
        CourseClasses = new ArrayList<CourseClass>();
    }

    // Bind professor to course
    public void addCourseClass(CourseClass courseClass)
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
		Professor other = (Professor) obj;
		if (Id != other.Id)
			return false;
		return true;
	}

	// Returns professor's ID
    public int Id;
    
    // Returns professor's name
    public String Name;

    // Returns reference to list of classes that professor teaches
    public List<CourseClass> CourseClasses;

}
