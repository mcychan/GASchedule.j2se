package hk.edu.gaSchedule.algorithm;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NsgaII<T extends Chromosome<T> >
{
	// Population of chromosomes
	private List<T> _chromosomes;

	// Prototype of chromosomes in population
	private T _prototype;
	
	// Number of chromosomes
	protected int _populationSize;
	
	// Number of crossover points of parent's class tables
	private int _numberOfCrossoverPoints;

	// Number of classes that is moved randomly by single mutation operation
	private int _mutationSize;

	// Probability that crossover will occur
	private float _crossoverProbability;

	// Probability that mutation will occur
	private float _mutationProbability;	

	// Initializes NsgaII
	private NsgaII(T prototype, int numberOfChromosomes)
    {
		_prototype = prototype;
		// there should be at least 2 chromosomes in population
		if (numberOfChromosomes < 2)
			numberOfChromosomes = 2;
		_populationSize = numberOfChromosomes;
	}

	public NsgaII(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		this(prototype, 100);
		_mutationSize = mutationSize;
		_numberOfCrossoverPoints = numberOfCrossoverPoints;
		_crossoverProbability = crossoverProbability;
		_mutationProbability = mutationProbability;
	}	

	// Returns pointer to best chromosomes in population
	public T getResult()
    {
		if(_chromosomes == null)
			return null;
		return _chromosomes.get(0);
    }
	
	/************** non-dominated sorting function ***************************/
	private List<Set<Integer> > nonDominatedSorting(List<T> totalChromosome)
	{
		Set<Integer>[] s = (Set<Integer>[]) Array.newInstance(Set.class, _populationSize * 2);
		int[] n = new int[s.length];
		List<Set<Integer> > front = new ArrayList<>();
		int[] rank = new int[s.length];
		front.add(new HashSet<Integer>());
		
		for(int p = 0; p < s.length; ++p) {
			s[p] = new HashSet<Integer>();
			for(int q = 0; q < s.length; ++q) {
				int diff = Float.compare(totalChromosome.get(p).getFitness(), totalChromosome.get(q).getFitness());
				if (diff > 0)
					s[p].add(q);
				else if(diff < 0)
					++n[p];
			}
			
			if (n[p] == 0) {
	            rank[p] = 0;
	            front.get(0).add(p);
			}
		}
		
		int i = 0;
		while(!front.get(i).isEmpty()) {
			Set<Integer> Q = new HashSet<>();
			for(int p : front.get(i)) {
				for(int q : s[p]) {
					if (--n[q] == 0) {
						rank[q] = i + 1;
						Q.add(q);
					}
				}
			}
			++i;
			front.add(Q);
		}
		
		return front.subList(0, front.size() - 1);
	}
	
	/************** calculate crowding distance function ***************************/
	protected Map<Integer, Float> calculateCrowdingDistance(Set<Integer> front, List<T> totalChromosome)
	{
		Map<Integer, Float> distance = front.stream().collect(Collectors.toMap(Function.identity(), m -> 0.0f));		
		Map<Integer, Float> obj = front.stream().collect(Collectors.toMap(Function.identity(), m -> totalChromosome.get(m).getFitness()));

		int[] sortedKeys = obj.entrySet().stream()
			.sorted(Entry.comparingByValue()).mapToInt(e -> e.getKey()).toArray();
		distance.put(sortedKeys[front.size() - 1], Float.MAX_VALUE);
		distance.put(sortedKeys[0], Float.MAX_VALUE);
		
		Set<Float> values = new HashSet<>(obj.values());
		if(values.size() > 1) {
			for(int i = 1; i < front.size() - 1; ++i)
				distance.put(sortedKeys[i], distance.get(sortedKeys[i]) + (obj.get(sortedKeys[i + 1]) - obj.get(sortedKeys[i - 1])) / (obj.get(sortedKeys[front.size() - 1]) - obj.get(sortedKeys[0])));
		}
		return distance;
	}
	
	private List<T> selection(List<Set<Integer> > front, List<T> totalChromosome)
	{
		int N = 0;
		List<Integer> newPop = new ArrayList<>();
		while(N < _populationSize) {
			for(Set<Integer> row : front) {
				N += row.size();
				if(N > _populationSize) {
					Map<Integer, Float> distance = calculateCrowdingDistance(row, totalChromosome);
					Set<Integer> sortedCdf = distance.entrySet().stream()
					.sorted(Entry.comparingByValue()).map(e -> e.getKey())
					.sorted(Comparator.reverseOrder()).collect(Collectors.toSet());					
					for(Integer j : sortedCdf) {
						if(newPop.size() >= _populationSize)
	                        break;
						newPop.add(j);
					}
					break;
				}
				newPop.addAll(row);
			}
		}

		return newPop.stream().map(n -> totalChromosome.get(n)).collect(Collectors.toList());
	}
	
	protected void initialize(List<T> population)
	{
		// initialize new population with chromosomes randomly built using prototype
		for (int i = 0; i < _populationSize; ++i)
			population.add(_prototype.makeNewFromPrototype());
	}
	
	// Starts and executes algorithm
	public void run(int maxRepeat, double minFitness)
    {
		if (_prototype == null)
			return;

		List<T> population = new ArrayList<>();
		initialize(population);		

		// Current generation
		int currentGeneration = 0;
		int repeat = 0;
		double lastBestFit = 0.0;

		for (; ;)
		{				
			T best = getResult();
			if(currentGeneration > 0) {
				String status = String.format("\rFitness: %f\t Generation: %d", best.getFitness(), currentGeneration);
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
					++_crossoverProbability;
			}	
			
			/******************* crossover *****************/
			List<T> offspring = new ArrayList<>();
			List<Integer> S = IntStream.range(0, _populationSize).boxed().collect(Collectors.toList());
			Collections.shuffle(S);
			
			final int halfPopulationSize = _populationSize / 2;
			for(int m = 0; m < halfPopulationSize; ++m) {
				T parent0 = population.get(S.get(2 * m));
				T parent1 = population.get(S.get(2 * m + 1));
				T child0 = parent0.crossover(parent1, _numberOfCrossoverPoints, _crossoverProbability);
				T child1 = parent1.crossover(parent0, _numberOfCrossoverPoints, _crossoverProbability);
				offspring.add(child0);
				offspring.add(child1);
			}
			
			/******************* mutation *****************/
			for(T child : offspring)
				child.mutation(_mutationSize, _mutationProbability);
			
			List<T> totalChromosome = new ArrayList<>(population);
			totalChromosome.addAll(offspring);
			
			/******************* non-dominated sorting *****************/
			List<Set<Integer> > front = nonDominatedSorting(totalChromosome);
			
			/******************* selection *****************/
			population = selection(front, totalChromosome);
			_populationSize = population.size();
			
			/******************* comparison *****************/
			if(currentGeneration == 0)
				_chromosomes = population;
			else {
				totalChromosome = new ArrayList<>(population);
				totalChromosome.addAll(_chromosomes);
				List<Set<Integer> > newBestFront = nonDominatedSorting(totalChromosome);
				_chromosomes = selection(newBestFront, totalChromosome);
				lastBestFit = best.getFitness();
			}
			++currentGeneration;
		}
	}		
}

