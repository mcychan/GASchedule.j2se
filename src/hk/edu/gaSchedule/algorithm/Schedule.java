package hk.edu.gaSchedule.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Schedule chromosome
public class Schedule implements Chromosome<Schedule>
{
	private Configuration _configuration;		

	// Fitness value of chromosome
	private float _fitness;

	// Flags of class requirements satisfaction
	private boolean[] _criteria;

	// Time-space slots, one entry represent one hour in one classroom
	private List<CourseClass>[] _slots;

	// Class table for chromosome
	// Used to determine first time-space slot used by class
	private Map<CourseClass, Reservation> _classes;

	// Initializes chromosomes with configuration block (setup of chromosome)
	public Schedule(Configuration configuration)
	{
		_configuration = configuration;			
		_fitness = 0;

		// reserve space for time-space slots in chromosomes code
		_slots = (List<CourseClass>[]) new List[Constant.DAYS_NUM * Constant.DAY_HOURS * _configuration.getNumberOfRooms()];
		for(int i=0; i< _slots.length; ++i)
			_slots[i] = new ArrayList<CourseClass>();
		_classes = new TreeMap<>();

		// reserve space for flags of class requirements
		_criteria = new boolean[_configuration.getNumberOfCourseClasses() * Constant.DAYS_NUM];
	}

	// Copy constructor
	private Schedule copy(Schedule c, boolean setupOnly)
	{		
		if (!setupOnly)
		{
			_configuration = c._configuration;
			// copy code				
			_slots = c._slots;
			_classes = c._classes;

			// copy flags of class requirements
			_criteria = c._criteria;

			// copy fitness
			_fitness = c._fitness;
			return this;
		}
		return new Schedule(c._configuration);
	}

	// Makes new chromosome with same setup but with randomly chosen code
	public Schedule makeNewFromPrototype()
	{
		// make new chromosome, copy chromosome setup
		Schedule newChromosome = copy(this, true);
		int nr = _configuration.getNumberOfRooms();

		// place classes at random position
		List<CourseClass> c = _configuration.getCourseClasses();
		for (CourseClass courseClass : c)
		{
			// determine random position of class			
			int dur = courseClass.Duration;

			int day = Configuration.rand() % Constant.DAYS_NUM;
			int room = Configuration.rand() % nr;
			int time = Configuration.rand() % (Constant.DAY_HOURS + 1 - dur);
			Reservation reservation = new Reservation(nr, day, time, room);

			// fill time-space slots, for each hour of class
			for (int i = dur - 1; i >= 0; i--)
				newChromosome._slots[reservation.index() + i].add(courseClass);

			// insert in class table of chromosome
			newChromosome._classes.put(courseClass, reservation);
		}

		newChromosome.calculateFitness();
		return newChromosome;
	}

	// Performes crossover operation using to chromosomes and returns pointer to offspring
	public Schedule crossover(Schedule parent2, int numberOfCrossoverPoints, float crossoverProbability)
	{
		// check probability of crossover operation
		if (Configuration.rand() % 100 > crossoverProbability)
			// no crossover, just copy first parent
			return copy(this, false);

		// new chromosome object, copy chromosome setup
		Schedule n = copy(this, true);

		// number of classes
		int size = _classes.size();

		boolean[] cp = new boolean[size];

		// determine crossover point (randomly)
		for (int i = numberOfCrossoverPoints; i > 0; i--)
		{
			for(; ;)
			{
				int p = Configuration.rand() % size;
				if (!cp[p])
				{
					cp[p] = true;
					break;
				}
			}
		}

		// make new code by combining parent codes
		boolean first = Configuration.rand() % 2 == 0;
		CourseClass[] classes = _classes.keySet().toArray(new CourseClass[0]);
		CourseClass[] parentClasses = parent2._classes.keySet().toArray(new CourseClass[0]);
		for (int i = 0; i < size; ++i)
		{
			if (first)
			{
				CourseClass courseClass = classes[i];
				Reservation reservation = _classes.get(courseClass);
				// insert class from first parent into new chromosome's class table
				n._classes.put(courseClass, reservation);
				// all time-space slots of class are copied
				for (int j = courseClass.Duration - 1; j >= 0; j--)
					n._slots[reservation.index() + j].add(courseClass);
			}
			else
			{
				CourseClass courseClass = parentClasses[i];
				Reservation reservation = parent2._classes.get(courseClass);
				// insert class from second parent into new chromosome's class table
				n._classes.put(courseClass, reservation);
				// all time-space slots of class are copied
				for (int j = courseClass.Duration - 1; j >= 0; j--)
					n._slots[reservation.index() + j].add(courseClass);
			}

			// crossover point
			if (cp[i])
				// change source chromosome
				first = !first;
		}

		n.calculateFitness();

		// return smart pointer to offspring
		return n;
	}

	// Performs mutation on chromosome
	public void mutation(int mutationSize, float mutationProbability)
	{
		// check probability of mutation operation
		if (Configuration.rand() % 100 > mutationProbability)
			return;

		// number of classes
		int numberOfClasses = _classes.size();
		int nr = _configuration.getNumberOfRooms();

		CourseClass[] classes = _classes.keySet().toArray(new CourseClass[0]);
		// move selected number of classes at random position
		for (int i = mutationSize; i > 0; i--)
		{
			// select ranom chromosome for movement
			int mpos = Configuration.rand() % numberOfClasses;

			// current time-space slot used by class
			CourseClass cc1 = classes[mpos];
			Reservation reservation1 = _classes.get(cc1);

			// determine position of class randomly			
			int dur = cc1.Duration;
			int day = Configuration.rand() % Constant.DAYS_NUM;
			int room = Configuration.rand() % nr;
			int time = Configuration.rand() % (Constant.DAY_HOURS + 1 - dur);
			Reservation reservation2 = new Reservation(nr, day, time, room);

			// move all time-space slots
			for (int j = dur - 1; j >= 0; j--)
			{
				// remove class hour from current time-space slot
				List<CourseClass> cl = _slots[reservation1.index() + j];
				cl.removeIf(cc -> cc == cc1);

				// move class hour to new time-space slot
				_slots[reservation2.index() + j].add(cc1);
			}

			// change entry of class table to point to new time-space slots
			_classes.put(cc1, reservation2);
		}

		calculateFitness();
	}

	// Calculates fitness value of chromosome
	public void calculateFitness()
	{
		// chromosome's score
		int score = 0;

		int numberOfRooms = _configuration.getNumberOfRooms();
		int daySize = Constant.DAY_HOURS * numberOfRooms;

		int ci = 0;
		// check criterias and calculate scores for each class in schedule
		for (CourseClass cc : _classes.keySet())
		{
			// coordinate of time-space slot
			Reservation reservation = _classes.get(cc);
			int day = reservation.getDay();
			int time = reservation.getTime();
			int room = reservation.getRoom();

			int dur = cc.Duration;

			// check for room overlapping of classes
			boolean ro = false;
			for (int i = dur - 1; i >= 0; i--)
			{
				if (_slots[reservation.index() + i].size() > 1)
				{
					ro = true;
					break;
				}
			}

			// on room overlapping
			if (!ro)
				score++;
			else
				score = 0;

			_criteria[ci + 0] = !ro;
			
			Room r = _configuration.getRoomById(room);
			// does current room have enough seats
			_criteria[ci + 1] = r.NumberOfSeats >= cc.NumberOfSeats;
			if (_criteria[ci + 1])
				score++;
			else
				score /= 2;

			// does current room have computers if they are required
			_criteria[ci + 2] = !cc.LabRequired || (cc.LabRequired && r.Lab);
			if (_criteria[ci + 2])
				score++;
			else
				score /= 2;

			boolean po = false, go = false;
			
			total_overlap:
			// check overlapping of classes for professors and student groups
			for (int i = numberOfRooms, t = day * daySize + time; i > 0; i--, t += Constant.DAY_HOURS)
			{
				// for each hour of class
				for (int j = dur - 1; j >= 0; j--)
				{
					// check for overlapping with other classes at same time
					List<CourseClass> cl = _slots[t + j];
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
								break total_overlap;
						}
					}
				}
			}		

			// professors have no overlapping classes?
			if (!po)
				score++;
			else
				score = 0;
			_criteria[ci + 3] = !po;

			// student groups has no overlapping classes?
			if (!go)
				score++;
			else
				score = 0;
			_criteria[ci + 4] = !go;
			ci += Constant.DAYS_NUM;
		}

		// calculate fitess value based on score
		_fitness = (float)score / (_configuration.getNumberOfCourseClasses() * Constant.DAYS_NUM);		
	}

	// Returns fitness value of chromosome
	public float getFitness() { return _fitness; }

	public Configuration getConfiguration() { return _configuration; }

	// Returns reference to table of classes
	public Map<CourseClass, Reservation> getClasses() { return _classes; }

	// Returns array of flags of class requiroments satisfaction
	public boolean[] getCriteria() { return _criteria; }

	// Return reference to array of time-space slots
	public List<CourseClass>[] getSlots() { return _slots; }
}
