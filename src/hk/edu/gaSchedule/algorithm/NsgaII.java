package hk.edu.gaSchedule.algorithm;

/*
 * K.Deb, A.Pratap, S.Agrawal, T.Meyarivan, A fast and elitist multiobjective genetic algorithm: 
 * NSGA-II, IEEE Transactions on Evolutionary Computation 6 (2002) 182–197.
 * Copyright (c) 2020 - 2022 Miller Cy Chan
 */

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class NsgaII<T extends Chromosome<T> >
{
	// Population of chromosomes
	protected List<T> _chromosomes;

	// Prototype of chromosomes in population
	protected T _prototype;
	
	// Number of chromosomes
	protected int _populationSize;
	
	// Number of crossover points of parent's class tables
	protected int _numberOfCrossoverPoints;

	// Number of classes that is moved randomly by single mutation operation
	protected int _mutationSize;

	// Probability that crossover will occur
	protected float _crossoverProbability;

	// Probability that mutation will occur
	protected float _mutationProbability;
	
	protected float _repeatRatio;

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
	protected List<Set<Integer> > nonDominatedSorting(List<T> totalChromosome)
	{
		Set<Integer>[] s = (Set<Integer>[]) Array.newInstance(Set.class, _populationSize * 2);
		int[] n = new int[s.length];
		List<Set<Integer> > front = new ArrayList<>();
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
			
			if (n[p] == 0)
	            front.get(0).add(p);
		}
		
		int i = 0;
		while(!front.get(i).isEmpty()) {
			Set<Integer> Q = new HashSet<>();
			for(int p : front.get(i)) {
				for(int q : s[p]) {
					if (--n[q] == 0)
						Q.add(q);
				}
			}
			++i;
			front.add(Q);
		}
		
		front.remove(front.size() - 1);
		return front;
	}
	
	/************** calculate crowding distance function ***************************/
	private Map<Integer, Float> calculateCrowdingDistance(Set<Integer> front, List<T> totalChromosome)
	{
		Map<Integer, Float> distance = new HashMap<>();		
		Map<Integer, Float> obj = new HashMap<>();

		for(Integer key : front) {
			distance.put(key, 0.0f);
			float fitness = totalChromosome.get(key).getFitness();
			if(!obj.containsValue(fitness))
				obj.put(key, fitness);
		}
		
		int[] sortedKeys = obj.entrySet().stream()
			.sorted(Entry.comparingByValue()).mapToInt(e -> e.getKey()).toArray();
		distance.put(sortedKeys[obj.size() - 1], Float.MAX_VALUE);
		distance.put(sortedKeys[0], Float.MAX_VALUE);		
		
		if(obj.size() > 1) {
			float diff2 = totalChromosome.get(sortedKeys[obj.size() - 1]).getDifference(totalChromosome.get(sortedKeys[0]));

			for(int i = 1; i < obj.size() - 1; ++i) {
				float diff = totalChromosome.get(sortedKeys[i + 1]).getDifference(totalChromosome.get(sortedKeys[i - 1])) * 1.0f / diff2;
				distance.put(sortedKeys[i], distance.get(sortedKeys[i]) + diff);
			}
		}
		return distance;
	}
	
	protected List<T> selection(List<Set<Integer> > front, List<T> totalChromosome)
	{
		int N = 0;
		List<Integer> newPop = new ArrayList<>();
		while(N < _populationSize) {
			for(Set<Integer> row : front) {
				N += row.size();
				if(N > _populationSize) {
					Map<Integer, Float> distance = calculateCrowdingDistance(row, totalChromosome);
					List<Integer> sortedCdf = distance.entrySet().stream()
					.sorted(Entry.comparingByValue()).map(e -> e.getKey())
					.sorted(Comparator.reverseOrder()).distinct().collect(Collectors.toList());					
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
	
	protected List<T> replacement(List<T> population)
    {
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
		return offspring;
    }
	
	protected void initialize(List<T> population)
	{
		// initialize new population with chromosomes randomly built using prototype
		for (int i = 0; i < _populationSize; ++i)
			population.add(_prototype.makeNewFromPrototype());
	}
	
	protected void reform()
	{
		Configuration.seed();
		if(_crossoverProbability < 95)
			_crossoverProbability += 1.0f;
		else if(_mutationProbability < 30)
			_mutationProbability += 1.0f;
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
		int bestNotEnhance = 0;
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
					++bestNotEnhance;
				else {
					lastBestFit = best.getFitness();
					bestNotEnhance = 0;
				}

				_repeatRatio = bestNotEnhance * 100.0f / maxRepeat;
				if (bestNotEnhance > (maxRepeat / 100))		
					reform();
			}				
			
			/******************* crossover *****************/
			List<T> offspring = replacement(population);			
			
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
			}			
			++currentGeneration;
		}
	}
	
	@Override
	public String toString()
	{
		return "NSGA II";
	}
}

