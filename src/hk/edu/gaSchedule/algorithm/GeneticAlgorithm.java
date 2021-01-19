package hk.edu.gaSchedule.algorithm;

import java.lang.reflect.Array;

// Genetic algorithm
public class GeneticAlgorithm<T extends Chromosome<T> >
{
	// Population of chromosomes
	private T[] _chromosomes;

	// Inidicates whether chromosome belongs to best chromosome group
	private boolean[] _bestFlags;

	// Indices of best chromosomes
	private int[] _bestChromosomes;

	// Number of best chromosomes currently saved in best chromosome group
	private int _currentBestSize;

	// Number of chromosomes which are replaced in each generation by offspring
	private int _replaceByGeneration;		

	// Prototype of chromosomes in population
	private T _prototype;

	// Number of crossover points of parent's class tables
	private int _numberOfCrossoverPoints;

	// Number of classes that is moved randomly by single mutation operation
	private int _mutationSize;

	// Probability that crossover will occurr
	private float _crossoverProbability;

	// Probability that mutation will occurr
	private float _mutationProbability;	

	// Initializes genetic algorithm
	private GeneticAlgorithm(T prototype, int numberOfChromosomes, int replaceByGeneration, int trackBest)
    {
		_currentBestSize = 0;
		_prototype = prototype;

		// there should be at least 2 chromosomes in population
		if (numberOfChromosomes < 2)
			numberOfChromosomes = 2;

		// and algorithm should track at least on of best chromosomes
		if (trackBest < 1)
			trackBest = 1;

		// reserve space for population
		_chromosomes = (T[]) Array.newInstance(_prototype.getClass(), numberOfChromosomes);
		_bestFlags = new boolean[numberOfChromosomes];

		// reserve space for best chromosome group
		_bestChromosomes = new int[trackBest];
		
		setReplaceByGeneration(replaceByGeneration);
	}

	public GeneticAlgorithm(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		this(prototype, 100, 8, 5);
		_mutationSize = mutationSize;
		_numberOfCrossoverPoints = numberOfCrossoverPoints;
		_crossoverProbability = crossoverProbability;
		_mutationProbability = mutationProbability;
	}	

	// Returns pointer to best chromosomes in population
	public T getResult()
    {
		return _chromosomes[_bestChromosomes[0]];
    }
	
	private void setReplaceByGeneration(int replaceByGeneration)
	{
		int numberOfChromosomes = _chromosomes.length;
		int trackBest = _bestChromosomes.length;
		if (replaceByGeneration > numberOfChromosomes - trackBest)
			replaceByGeneration = numberOfChromosomes - trackBest;
		_replaceByGeneration = replaceByGeneration;
	}

	// Tries to add chromosomes in best chromosome group
	private void addToBest(int chromosomeIndex)
    {
		// don't add if new chromosome hasn't fitness big enough for best chromosome group
		// or it is already in the group?
		if ((_currentBestSize == _bestChromosomes.length &&
			Float.compare(_chromosomes[_bestChromosomes[_currentBestSize - 1]].getFitness(),
			_chromosomes[chromosomeIndex].getFitness()) >= 0) || _bestFlags[chromosomeIndex])
			return;

		// find place for new chromosome
		int i = _currentBestSize;
		for (; i > 0; i--)
		{
			// group is not full?
			if (i < _bestChromosomes.length)
			{
				// position of new chromosomes is found?
				if (Float.compare(_chromosomes[_bestChromosomes[i - 1]].getFitness(), _chromosomes[chromosomeIndex].getFitness()) > 0)
					break;

				// move chromosomes to make room for new
				_bestChromosomes[i] = _bestChromosomes[i - 1];
			}
			else
				// group is full remove worst chromosomes in the group
				_bestFlags[_bestChromosomes[i - 1]] = false;
		}

		// store chromosome in best chromosome group
		_bestChromosomes[i] = chromosomeIndex;
		_bestFlags[chromosomeIndex] = true;

		// increase current size if it has not reached the limit yet
		if (_currentBestSize < _bestChromosomes.length)
			_currentBestSize++;
	}	

	// Returns TRUE if chromosome belongs to best chromosome group
	private boolean isInBest(int chromosomeIndex)
    {
		return _bestFlags[chromosomeIndex];
	}

	// Clears best chromosome group
	private void clearBest()
    {
		_bestFlags = new boolean[_bestFlags.length];
		_currentBestSize = 0;
	}
	
	protected void initialize(T[] population)
	{
		// initialize new population with chromosomes randomly built using prototype
		for (int i = 0; i < population.length; ++i)
		{
			population[i] = _prototype.makeNewFromPrototype();
			// addToBest(i);
		}
	}
	
	protected T[] selection(T[] population)
    {
		T[] result = (T[]) Array.newInstance(_prototype.getClass(), 2);
		// selects parent randomly
		result[0] = population[Configuration.rand() % population.length];
		result[1] = population[Configuration.rand() % population.length];
		return result;
    }
	
	protected T[] replacement(T[] population)
	{
		// produce offspring
		T[] offspring = (T[]) Array.newInstance(_prototype.getClass(), _replaceByGeneration);
		for (int j = 0; j < _replaceByGeneration; j++)
		{
			// selects parent randomly
			T[] parent = selection(population);

			offspring[j] = parent[0].crossover(parent[1], _numberOfCrossoverPoints, _crossoverProbability);
			offspring[j].mutation(_mutationSize, _mutationProbability);

			// replace chromosomes of current operation with offspring
			int ci;
			do
			{
				// select chromosome for replacement randomly
				ci = Configuration.rand() % population.length;

				// protect best chromosomes from replacement
			} while (isInBest(ci));

			// replace chromosomes
			population[ci] = offspring[j];

			// try to add new chromosomes in best chromosome group
			addToBest(ci);
		}
		return offspring;
    }
	
	// Starts and executes algorithm
	public void run(int maxRepeat, double minFitness)
    {
		if (_prototype == null)
			return;

		// clear best chromosome group from previous execution
		clearBest();
		initialize(_chromosomes);		

		// Current generation
		int currentGeneration = 0;
		int repeat = 0;
		double lastBestFit = 0.0;
		for (; ;)
		{				
			T best = getResult();
			String status = String.format("\rFitness: %f\t Generation: %d", best.getFitness(), currentGeneration++);
			System.out.print(status);
			
			// algorithm has reached criteria?
			if (best.getFitness() > minFitness)
				break;

			double difference = Math.abs(best.getFitness() - lastBestFit);
			if (difference <= 0.0000001)
				++repeat;
			else
				repeat = 0;

			if (repeat > (maxRepeat / 100))
			{				
				setReplaceByGeneration(_replaceByGeneration * 3);
				++_crossoverProbability;
			}			

			replacement(_chromosomes);

			Configuration.seed();
			lastBestFit = best.getFitness();
		}
	}
	
	@Override
	public String toString()
	{
		return "Genetic Algorithm";
	}
}

