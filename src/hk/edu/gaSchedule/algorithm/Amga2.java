package hk.edu.gaSchedule.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/****************** Archive-based Micro Genetic Algorithm(AMGA2) **********************/
public class Amga2<T extends Chromosome<T> >
{
	// Population of chromosomes
	private List<T> _archivePopulation, _parentPopulation, _offspringPopulation, _combinedPopulation;

	// Prototype of chromosomes in population
	protected T _prototype;

	private int _currentArchiveSize = 0;

	// Number of chromosomes
	protected int _populationSize, _archiveSize;

	// Index for crossover
	protected float _etaCross;

	// Number of classes that is moved randomly by single mutation operation
	private int _mutationSize;

	// Probability that crossover will occur
	protected float _crossoverProbability;

	// Probability that mutation will occur
	private float _mutationProbability;

	final class DistanceMatrix implements Comparable<DistanceMatrix>
	{
		public int index1 = -1;
		public int index2 = -1;
		public float distance = 0.0f;

        public int compareTo(DistanceMatrix other)
        {
			if (other == null)
				return 0;

			if (distance < other.distance)
				return -1;
			if (distance > other.distance)
				return 1;
			if (index1 < other.index1)
				return -1;
			if (index1 > other.index1)
				return 1;
			if (index2 < other.index2)
				return -1;
			if (index2 > other.index2)
				return 1;
			return 0;
		}
    }

	// Initializes Amga2
	private Amga2(T prototype, int numberOfChromosomes)
	{
		_prototype = prototype;
		// there should be at least 2 chromosomes in population
		if (numberOfChromosomes < 2)
			numberOfChromosomes = 2;
		_populationSize = _archiveSize = numberOfChromosomes;
	}

	public Amga2(T prototype, float etaCross, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		this(prototype, 100);
		_mutationSize = mutationSize;
		_etaCross = etaCross;
		_crossoverProbability = crossoverProbability;
		_mutationProbability = mutationProbability;
	}

	// Returns pointer to best chromosomes in population
	public T getResult()
    {
		if(_combinedPopulation == null)
			return null;
		return _combinedPopulation.get(0);
    }
			
	protected void initialize()
	{
		_archivePopulation = new ArrayList<>();
		_parentPopulation = new ArrayList<>();
		_offspringPopulation = new ArrayList<>();
		_combinedPopulation = new ArrayList<>();
		for (int i = 0; i < _archiveSize; ++i) {
			_archivePopulation.add(_prototype.makeNewFromPrototype());
			_combinedPopulation.add(_prototype.makeNewFromPrototype());
		}
		for (int i = 0; i < _populationSize; ++i) {
			_parentPopulation.add(_prototype.makeNewFromPrototype());
			_offspringPopulation.add(_prototype.makeNewFromPrototype());
			_combinedPopulation.add(_prototype.makeNewFromPrototype());
		}
	}

	private void assignInfiniteDiversity(List<T> population, List<Integer> elite)
	{
		elite.forEach(index -> population.get(index).setDiversity(Float.POSITIVE_INFINITY));		
	}

	private void assignDiversityMetric(List<T> population, List<Integer> elite)
	{
		if (elite.size() <= 2)
        {
			assignInfiniteDiversity(population, elite);
			return;
        }

		Queue<Integer> distinct = extractDistinctIndividuals(population, elite);
		if (distinct.size() <= 2)
		{
			assignInfiniteDiversity(population, elite);
			return;
		}
		
		int size = distinct.size();
		distinct.forEach(e -> population.get(e).setDiversity(0.0f));
		int[] indexArray = distinct.stream().mapToInt(x -> x).toArray();

		float val = population.get(indexArray[size - 1]).getFitness() - population.get(indexArray[0]).getFitness();
		if (val == 0)
			return;

		for (int j = 0; j < size; j++) {						
			if (j == 0)
			{
				float[] hashArray = new float[] { 0.0f, population.get(indexArray[j]).getFitness(), population.get(indexArray[j + 1]).getFitness() };
				float r = (hashArray[2] - hashArray[1]) / val;
				population.get(indexArray[j]).setDiversity(population.get(indexArray[j]).getDiversity() + (r * r));
			}
			else if (j == size - 1)
			{
				float[] hashArray = new float[] { population.get(indexArray[j - 1]).getFitness(), population.get(indexArray[j]).getFitness() };
				float l = (hashArray[1] - hashArray[0]) / val;
				population.get(indexArray[j]).setDiversity(population.get(indexArray[j]).getDiversity() + (l * l));
			}
			else
			{
				float[] hashArray = new float[] { population.get(indexArray[j - 1]).getFitness(), population.get(indexArray[j]).getFitness(), population.get(indexArray[j + 1]).getFitness() };
				float l = (hashArray[1] - hashArray[0]) / val;
				float r = (hashArray[2] - hashArray[1]) / val;
				population.get(indexArray[j]).setDiversity(population.get(indexArray[j]).getDiversity() + (l * r));
			}
		}
	}

	private void createOffspringPopulation()
	{
		int r1, r2, r3;
		for (int i = 0; i < _populationSize; ++i) {
			do
			{
				r1 = Configuration.rand(_currentArchiveSize);
			} while (_archivePopulation.get(r1).equals(_archivePopulation.get(i)));
			do
			{
				r2 = Configuration.rand(_currentArchiveSize);
			} while (_archivePopulation.get(r2).equals(_archivePopulation.get(i)) || r2 == r1);
			do
			{
				r3 = Configuration.rand(_currentArchiveSize);
			} while (_archivePopulation.get(r3).equals(_archivePopulation.get(i)) || r3 == r1 || r3 == r2);
			_offspringPopulation.set(i, _offspringPopulation.get(i).crossover(_parentPopulation.get(i), _archivePopulation.get(r1), _archivePopulation.get(r2), _archivePopulation.get(r3), _etaCross, _crossoverProbability));
			_offspringPopulation.get(i).setRank(_parentPopulation.get(i).getRank()); //for rank based mutation
		}
	}

	private int checkDomination(T a, T b)
	{
		return Float.compare(a.getFitness(), b.getFitness());
	}

	private Queue<Integer> extractDistinctIndividuals(List<T> population, List<Integer> elite)
	{
		return elite.stream().sorted((Integer e1, Integer e2) ->
			checkDomination(population.get(e1), population.get(e2))).distinct().collect(Collectors.toCollection(ArrayDeque::new));
	}

	private Queue<Integer> extractENNSPopulation(List<T> mixedPopulation, Queue<Integer> pool, int desiredEliteSize)
	{
		int poolSize = pool.size();
		int mixedSize = mixedPopulation.size();
		List<Integer> filtered = pool.stream().filter(index -> Float.isInfinite((Float) mixedPopulation.get(index).getDiversity())).distinct().collect(Collectors.toList());
		int numInf = filtered.size();

		if (desiredEliteSize <= numInf)
			return filtered.stream().limit(desiredEliteSize).distinct().collect(Collectors.toCollection(ArrayDeque::new));

		Queue<Integer> elite = pool.stream().distinct().collect(Collectors.toCollection(ArrayDeque::new));
		pool.clear();
		if (desiredEliteSize >= elite.size())
			return elite;

		float[][] distance = new float[poolSize][poolSize];
		int[] indexArray = new int[poolSize];
		int[] originalArray = new int[mixedSize];

		for (int i = 0; i < mixedSize; ++i)
			originalArray[i] = -1;

		int counter = 0;
		for (int index : elite)
		{
			indexArray[counter] = index;
			originalArray[indexArray[counter]] = counter++;
		}

		List<DistanceMatrix> distArray = new ArrayList<>();
		for (int i = 0; i < poolSize; ++i) {
			for (int j = i + 1; j < poolSize; ++j) {
				DistanceMatrix distMatrix = new DistanceMatrix();
				distMatrix.index1 = indexArray[i];
				distMatrix.index2 = indexArray[j];
				distance[j][i] = distance[i][j] = distMatrix.distance = Math.abs(mixedPopulation.get(distMatrix.index1).getFitness() - mixedPopulation.get(distMatrix.index2).getFitness());
				distArray.add(distMatrix);
			}
		}

		Collections.sort(distArray);
		int idx = 0;
		while (elite.size() > desiredEliteSize && idx < distArray.size())
		{
			Integer index1, index2;
			do
			{
				DistanceMatrix temp = distArray.get(idx++);
				index1 = temp.index1;
				index2 = temp.index2;
			} while ((originalArray[index1] == -1 || originalArray[index2] == -1) && idx < distArray.size());

			if (idx >= distArray.size())
				break;

			if (Float.isInfinite(mixedPopulation.get(index1).getDiversity()) && Float.isInfinite(mixedPopulation.get(index2).getDiversity()))
				continue;
			
			if (Float.isInfinite(mixedPopulation.get(index1).getDiversity()))
			{
				elite.remove(index2);
				pool.add(index2);
				originalArray[index2] = -1;
			}
			else if (Float.isInfinite(mixedPopulation.get(index2).getDiversity()))
			{
				elite.remove(index1);
				pool.add(index1);
				originalArray[index1] = -1;
			}
			else
			{
				float dist1 = Float.POSITIVE_INFINITY;
				for (int index : elite)
				{
					if (index != index1 && index != index2)
					{
						if (dist1 > distance[originalArray[index1]][originalArray[index]])
							dist1 = distance[originalArray[index1]][originalArray[index]];
					}
				}
				float dist2 = Float.POSITIVE_INFINITY;
				for (int index : elite)
				{
					if (index != index1 && index != index2)
					{
						if (dist2 > distance[originalArray[index2]][originalArray[index]])
							dist2 = distance[originalArray[index2]][originalArray[index]];
					}
				}

				if (dist1 < dist2)
				{
					elite.remove(index1);
					pool.add(index1);
					originalArray[index1] = -1;
				}
				else
				{
					elite.remove(index2);
					pool.add(index2);
					originalArray[index2] = -1;
				}
			}
		}
		
		while (elite.size() > desiredEliteSize)
		{
			Integer temp = elite.poll();
			pool.add(temp);
		}
		return elite;
	}

	private boolean extractBestRank(List<T> population, Queue<Integer> pool, List<Integer> elite)
	{
		if (pool.isEmpty())
			return false;

		Queue<Integer> remains = new ArrayDeque<>();
		Integer index1 = pool.poll();
		elite.add(index1);

		while ((index1 = pool.poll()) != null)
		{
			int flag = -1;
			int index2 = 0;
			while (index2 < elite.size())
			{
				flag = checkDomination(population.get(index1), population.get(index2));
				if (flag == 1)
				{
					remains.add(index2);
					elite.remove(index2);
				}
				else if (flag == -1)
					break;
				else
					++index2;
			}

			if (flag > -1)
				elite.add(index1);
			else
				remains.add(index1);
		}
		pool.clear();
		pool.addAll(remains);
		return true;
	}

	private void fillBestPopulation(List<T> mixedPopulation, int mixedLength, List<T> population, int populationLength)
	{
		Queue<Integer> pool = IntStream.range(0, mixedLength).boxed().collect(Collectors.toCollection(ArrayDeque::new));
		Queue<Integer> elite = new ArrayDeque<>();
		List<Integer> filled = new ArrayList<>();
		AtomicInteger rank = new AtomicInteger(1);

		pool.forEach(index -> mixedPopulation.get(index).setDiversity(0.0f));

		boolean hasBetter = true;
		while (hasBetter && filled.size() < populationLength)
		{
			List<Integer> elites = new ArrayList<>(elite);
			hasBetter = extractBestRank(mixedPopulation, pool, elites);
			elites.forEach(index -> mixedPopulation.get(index).setRank(rank.get()));
			
			if (rank.getAndIncrement() == 1)
				assignInfiniteDiversity(mixedPopulation, elites);
			elite = new ArrayDeque<>(elites);

			if (elite.size() + filled.size() < populationLength)
			{
				filled.addAll(elite);
				elite.clear();
			}
			else
			{
				Queue<Integer> temp = extractENNSPopulation(mixedPopulation, elite, populationLength - filled.size());
				filled.addAll(temp);
			}
		}

		AtomicInteger j = new AtomicInteger(0);
		filled.forEach(index -> population.set(j.getAndIncrement(), mixedPopulation.get(index)));
	}

	private void fillDiversePopulation(List<T> mixedPopulation, List<Integer> pool, List<T> population, int startLocation, int desiredSize)
	{
		assignDiversityMetric(mixedPopulation, pool);
		int poolSize = pool.size();
		int[] indexArray = pool.stream().sorted((Integer e1, Integer e2) ->
			Float.compare(mixedPopulation.get(e1).getDiversity(), mixedPopulation.get(e2).getDiversity())).mapToInt(x -> x).toArray();

		for (int i = 0; i < desiredSize; ++i)
			population.set(startLocation + i, mixedPopulation.get(indexArray[poolSize - 1 - i]));
	}

	private void createParentPopulation()
	{
		Queue<Integer> pool = IntStream.range(0, _currentArchiveSize).boxed().collect(Collectors.toCollection(ArrayDeque::new));
		List<Integer> elite = new ArrayList<>();
		List<Integer> selectionPool = new ArrayList<>();

		int rank = 1;
		while (selectionPool.size() < _populationSize)
		{
			extractBestRank(_archivePopulation, pool, elite);
			for (int i : elite)
			{
				_archivePopulation.get(i).setRank(rank);
				selectionPool.add(i);
			}
			++rank;
			elite.clear();
		}

		AtomicInteger j = new AtomicInteger(0);
		selectionPool.forEach(i -> _parentPopulation.set(j.getAndIncrement(), _archivePopulation.get(i)));
		fillDiversePopulation(_archivePopulation, selectionPool, _parentPopulation, j.get(), _populationSize - j.get());
	}

	private void mutateOffspringPopulation()
	{
		for (int i = 0; i < _populationSize; ++i) {				
			float pMut = _mutationProbability + (1.0f - _mutationProbability) * ((float)(_offspringPopulation.get(i).getRank() - 1) / (_currentArchiveSize - 1)); //rank-based variation
			_offspringPopulation.get(i).mutation(_mutationSize, pMut);
		}
	}

	private void updateArchivePopulation()
	{
		if (_currentArchiveSize + _populationSize <= _archiveSize)
		{
			for (int j = _currentArchiveSize, i = 0; i < _populationSize; ++i, ++j)
				_archivePopulation.set(j, _offspringPopulation.get(i));

			_currentArchiveSize += _populationSize;
		}
		else
		{
			for (int i = 0; i < _currentArchiveSize; ++i)
				_combinedPopulation.set(i, _archivePopulation.get(i));

			for (int i = 0; i < _populationSize; ++i)
				_combinedPopulation.set(_currentArchiveSize + i, _offspringPopulation.get(i));

			fillBestPopulation(_combinedPopulation, _currentArchiveSize + _populationSize, _archivePopulation, _archiveSize);
			_currentArchiveSize = _archiveSize;
		}

		_archivePopulation.forEach(e -> e.setDiversity(0.0f));
	}

	private void finalizePopulation()
	{		
		List<Integer> elite = new ArrayList<>();
		Queue<Integer> pool = IntStream.range(0, _currentArchiveSize).boxed().filter(i -> _archivePopulation.get(i).getFitness() >= 0.0).collect(Collectors.toCollection(ArrayDeque::new));

		if (!pool.isEmpty())
		{
			extractBestRank(_archivePopulation, pool, elite);
			pool.clear();
			if (elite.size() > _populationSize)
			{
				elite.forEach(index -> _archivePopulation.get(index).setDiversity(0.0f));

				assignInfiniteDiversity(_archivePopulation, elite);
				extractENNSPopulation(_archivePopulation, pool, _populationSize);
				elite = new ArrayList<>(pool);
			}
			_currentArchiveSize = elite.size();
			AtomicInteger i = new AtomicInteger(0);
			elite.forEach(index -> _combinedPopulation.set(i.getAndIncrement(), _archivePopulation.get(index)));
		}
		else
			_currentArchiveSize = 0;
	}

	// Starts and executes algorithm
	public void run(int maxRepeat, double minFitness)
	{
		if (_prototype == null)
			return;

		initialize();
		_currentArchiveSize = _populationSize;

		// Current generation
		int currentGeneration = 0;
		int repeat = 0;
		double lastBestFit = 0.0;

		for (; ; )
		{
			T best = getResult();
			if(currentGeneration > 0) {
				String status = String.format("\rFitness: %f\t Generation: %d", best.getFitness(), currentGeneration);
				System.out.print(status);

				// algorithm has reached criteria?
				if (best.getFitness() > minFitness)
				{
					finalizePopulation();
					break;
				}

				double difference = Math.abs(best.getFitness() - lastBestFit);
				if (difference <= 0.0000001)
					++repeat;
				else
					repeat = 0;

				if (repeat > (maxRepeat / 100))
					++_mutationProbability;
				lastBestFit = best.getFitness();
			}

			createParentPopulation();
			createOffspringPopulation();
			mutateOffspringPopulation();
			updateArchivePopulation();
			Configuration.seed();
			++currentGeneration;
		}
	}

	@Override
	public String toString()
	{
		return "Archive-based Micro Genetic Algorithm (AMGA2)";
	}
}

