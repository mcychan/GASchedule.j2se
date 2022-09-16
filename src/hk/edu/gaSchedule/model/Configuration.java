package hk.edu.gaSchedule.model;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

// Reads configration file and stores parsed objects
public class Configuration
{
	// parsed professors
	private Map<Integer, Professor> _professors;

	// parsed student groups
	private Map<Integer, StudentsGroup> _studentGroups;

	// parsed courses
	private Map<Integer, Course> _courses;

	// parsed rooms
	private Map<Integer, Room> _rooms;

	// parsed classes
	private List<CourseClass> _courseClasses;

	// Inidicate that configuration is not parsed yet
	private boolean _isEmpty;
	
	private static Random _random = new Random(System.currentTimeMillis());

	// Initialize data
	public Configuration()  {
		_isEmpty = true;
		_professors = new TreeMap<>();
		_studentGroups = new TreeMap<>();
		_courses = new TreeMap<>();
		_rooms = new TreeMap<>();
		_courseClasses = new ArrayList<>();
	}

	// Returns professor with specified ID
	// If there is no professor with such ID method returns NULL
	Professor getProfessorById(int id)
	{
		if (!_professors.containsKey(id))
			return null;
		return _professors.get(id);
	}

	// Returns number of parsed professors
	public int getNumberOfProfessors() { return _professors.size(); }

	// Returns student group with specified ID
	// If there is no student group with such ID method returns NULL
	StudentsGroup getStudentsGroupById(Integer id)
	{
		if (!_studentGroups.containsKey(id))
			return null;
		return _studentGroups.get(id);
	}

	// Returns number of parsed student groups
	public int getNumberOfStudentGroups() { return _studentGroups.size(); }

	// Returns course with specified ID
	// If there is no course with such ID method returns NULL
	Course getCourseById(int id)
	{
		if (!_courses.containsKey(id))
			return null;
		return _courses.get(id);	
	}

	public int getNumberOfCourses() { return _courses.size(); }

	// Returns room with specified ID
	// If there is no room with such ID method returns NULL
	public Room getRoomById(int id)
	{
		if (!_rooms.containsKey(id))
			return null;
		return _rooms.get(id);
	}

	// Returns number of parsed rooms
	public int getNumberOfRooms() { return _rooms.size(); }

	// Returns reference to list of parsed classes
	public List<CourseClass> getCourseClasses() { return _courseClasses; }

	// Returns number of parsed classes
	public int getNumberOfCourseClasses() { return _courseClasses.size(); }

	// Returns TRUE if configuration is not parsed yet
	public boolean isEmpty() { return _isEmpty; }

	private static <T> T getMember(JsonElement element, Class<T> clazz)
	{
		if(clazz == Integer.class)
			return (T) (Object) element.getAsInt();
		if(clazz == Double.class)
			return (T) (Object) element.getAsDouble();
		if(clazz == Boolean.class)
			return (T) (Object) element.getAsBoolean();
		if(clazz == String.class)
			return (T) (Object) element.getAsString();
		if(element.isJsonArray())
			return (T) (Object) element.getAsJsonArray();
		return (T) (Object) element.getAsJsonObject();		
	}

	// Reads professor's data from config file, makes object and returns
	// Returns NULL if method cannot parse configuration data
	private Professor parseProfessor(Map<String, JsonElement> data)
	{
		if (!data.containsKey("id"))
			return null;
		Integer id = getMember(data.get("id"), Integer.class);

		if (!data.containsKey("name"))
			return null;
		String name = getMember(data.get("name"), String.class);
		return new Professor(id, name);
	}

	// Reads StudentsGroup's data from config file, makes object and returns
	// Returns NULL if method cannot parse configuration data
	private StudentsGroup parseStudentsGroup(Map<String, JsonElement> data)
	{
		if (!data.containsKey("id"))
			return null;
		Integer id = getMember(data.get("id"), Integer.class);

		if (!data.containsKey("name"))
			return null;
		String name = getMember(data.get("name"), String.class);

		if (!data.containsKey("size"))
			return null;
		int size = getMember(data.get("size"), Integer.class);
		return new StudentsGroup(id, name, size);
	}

	// Reads course's data from config file, makes object and returns
	// Returns NULL if method cannot parse configuration data
	private Course parseCourse(Map<String, JsonElement> data)
	{
		if (!data.containsKey("id"))
			return null;
		Integer id = getMember(data.get("id"), Integer.class);

		if (!data.containsKey("name"))
			return null;
		String name = getMember(data.get("name"), String.class);

		return new Course(id, name);
	}

	// Reads rooms's data from config file, makes object and returns
	// Returns NULL if method cannot parse configuration data
	private Room parseRoom(Map<String, JsonElement> data)
	{	
		Boolean lab = false;
		if (data.containsKey("lab"))
			lab = getMember(data.get("lab"), Boolean.class);

		if (!data.containsKey("name"))
			return null;
		String name = getMember(data.get("name"), String.class);

		if (!data.containsKey("size"))
			return null;
		Integer size = getMember(data.get("size"), Integer.class);
		return new Room(name, lab, size);
	}

	// Reads class' data from config file, makes object and returns pointer
	// Returns NULL if method cannot parse configuration data
	private CourseClass parseCourseClass(Map<String, JsonElement> data)
	{
		Integer pid = 0, cid = 0, dur = 1;
		Boolean lab = false;

		List<StudentsGroup> groups = new ArrayList<>();
		for(String key : data.keySet()) {
			switch(key) {
			case "professor":
				pid = getMember(data.get(key), Integer.class);
				break;
			case "course":
				cid = getMember(data.get(key), Integer.class);
				break;
			case "lab":
				lab = getMember(data.get(key), Boolean.class);
				break;
			case "duration":
				dur = getMember(data.get(key), Integer.class);
				break;
			case "group":
			case "groups":
				if (data.containsKey(key))
				{
					if (data.get(key).isJsonArray())
					{
						JsonArray grpList = getMember(data.get(key), JsonArray.class);
						for (JsonElement grp : grpList)
						{
							StudentsGroup g = getStudentsGroupById(getMember(grp, Integer.class));
							if (g != null)
								groups.add(g);
						}
					}
					else
					{
						Integer group = getMember(data.get(key), Integer.class);
						StudentsGroup g = getStudentsGroupById(group);
						if (g != null)
							groups.add(g);
					}
				}
				break;
			}
		}

		// get professor who teaches class and course to which this class belongs
		Professor p = getProfessorById(pid);
		Course c = getCourseById(cid);

		// does professor and class exists
		if (c == null || p == null)
			return null;

		// make object and return
		return new CourseClass(p, c, lab, dur, groups.toArray(new StudentsGroup[0]));
	}
	
	public static Gson getGson() {
    	return new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ssZ").create();
    }

	public void parse(String json) throws Exception
	{
		// clear previously parsed objects
		_professors.clear();
		_studentGroups.clear();
		_courses.clear();
		_rooms.clear();
		_courseClasses.clear();

		Room.restartIDs();
		CourseClass.restartIDs();

		Type type = new TypeToken<Map<String, Map<String, JsonElement> >[]>(){}.getType();
		Map<String, Map<String, JsonElement> >[] data = getGson().fromJson(json, type);
		for (Map<String, Map<String, JsonElement> > item : data)
		{
			for (Entry<String, Map<String, JsonElement> > obj : item.entrySet())
			{
				switch (obj.getKey()) {
					case "prof":
						Professor prof = parseProfessor(obj.getValue());
						_professors.put(prof.Id, prof);
						break;
					case "course":
						Course course = parseCourse(obj.getValue());
						_courses.put(course.Id, course);
						break;
					case "room":
						Room room = parseRoom(obj.getValue());
						_rooms.put(room.Id, room);
						break;
					case "group":
						StudentsGroup group = parseStudentsGroup(obj.getValue());
						_studentGroups.put(group.Id, group);
						break;
					case "class":
						CourseClass courseClass = parseCourseClass(obj.getValue());
						_courseClasses.add(courseClass);
						break;
				}
			}
		}
		_isEmpty = false;
	}
	
	public void parse(Path path) throws Exception
	{
		// read file into a string and deserialize JSON to a type
		parse(new String(Files.readAllBytes(path)));
	}
	
	// parse file and store parsed object
	public void parse(File file) throws Exception
	{
		// read file into a string and deserialize JSON to a type
		parse(Paths.get(file.getAbsolutePath()));
	}
	
	public static int rand()
	{
		return _random.nextInt(32768);
	}
	
	public static double random()
	{
		return _random.nextDouble();
	}
	
	public static int rand(int size)
	{
		return _random.nextInt(size);
	}
	
	public static int rand(int min, int max)
	{
		return min + rand(max - min + 1);
	}
	
	public static double rand(float min, float max)
	{
		return min + _random.nextDouble() * (max - min);
	}
	
	public static void seed()
	{
		_random = new Random(System.currentTimeMillis());
	}
}