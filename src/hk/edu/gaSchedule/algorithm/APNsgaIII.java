package hk.edu.gaSchedule.algorithm;
/*
 * Wu, M.; Yang, D.; Zhou, B.; Yang, Z.; Liu, T.; Li, L.; Wang, Z.; Hu,
 * K. Adaptive Population NSGA-III with Dual Control Strategy for Flexible Job
 * Shop Scheduling Problem with the Consideration of Energy Consumption and Weight. Machines 2021, 9, 344.
 * https://doi.org/10.3390/machines9120344
 * Copyright (c) 2023 Miller Cy Chan
 */

import java.util.ArrayList;
import java.util.List;

import hk.edu.gaSchedule.model.Chromosome;

public class APNsgaIII<T extends Chromosome<T> > extends NsgaIII<T>
{
	private int _currentGeneration = 0, _max_iterations = 5000;
	
	// Worst of chromosomes
	protected T _worst;

	// Initializes Adaptive Population NSGA-III with Dual Control Strategy
	public APNsgaIII(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);		
	}
	
	private double ex(T chromosome)
	{
		double numerator = 0.0, denominator = 0.0;
		for (int f = 0; f < chromosome.getObjectives().length; ++f) {
			numerator += chromosome.getObjectives()[f] - _best.getObjectives()[f];
			denominator += _worst.getObjectives()[f] - _best.getObjectives()[f];
		}
		return (numerator + 1) / (denominator + 1);
	}
	
	private void popDec(List<T> population)
	{
		if(population.size() <= _populationSize)
			return;
		
		int N = population.size();		
		int rank = (int) (.3 * _populationSize);
		
		for(int i = 0; i < N; ++i) {
			double exValue = ex(population.get(i));
			
			if(exValue > .5 && i > rank) {
				population.remove(i);				
				if(--N <= _populationSize)
					break;
			}
		}
		
		for(int i = population.size() - 1; i >= _populationSize; --i)
			population.remove(i);
	}
	
	// Starts and executes algorithm
	@Override
	public void run(int maxRepeat, double minFitness)
    {
		if (_prototype == null)
			return;
		
		List<T>[] pop = new ArrayList[2];
		pop[0] = new ArrayList<>();
		initialize(pop[0]);
		int nMax = (int) (1.5 * _populationSize);

		int bestNotEnhance = 0;
		double lastBestFit = 0.0;

		int cur = 0, next = 1;
		while(_currentGeneration < _max_iterations)
		{
			T best = getResult();
			if(_currentGeneration > 0) {	
				double difference = Math.abs(best.getFitness() - lastBestFit);
				if (difference <= 1e-6)
					++bestNotEnhance;
				else {
					lastBestFit = best.getFitness();
					bestNotEnhance = 0;
				}
				
				String status = String.format("\rFitness: %.9f\t Generation: %d", best.getFitness(), _currentGeneration);	
				if(bestNotEnhance >= 15)
					status = String.format("%s\t Best not enhance: %d", status, bestNotEnhance);
				System.out.print(status);
				
				if (best.getFitness() > minFitness) 
					break;

				if (bestNotEnhance > (maxRepeat / 50))		
					reform();
			}				
			
			/******************* crossover *****************/
			List<T> offspring = replacement(pop[cur]);
			
			/******************* mutation *****************/
			for(T child : offspring)
				child.mutation(_mutationSize, _mutationProbability);			

			pop[cur].addAll(offspring);
			
			/******************* selection *****************/		
			pop[next] = super.selection(pop[cur]);			
			_best = dominate(pop[next].get(0), pop[cur].get(0)) ? pop[next].get(0) : pop[cur].get(0);
			
			int N = pop[next].size();
			int nTmp = N;
			for(int i = 0; i < nTmp; ++i) {			
				T parent = pop[next].get(i);
				T child = parent.clone();
				child.mutation(_mutationSize, _mutationProbability);
				
				_worst = pop[next].get(pop[next].size() - 1);
				if(dominate(child, parent)) {
					pop[next].set(i, child);
					if(dominate(child, _best))
						_best = child;
				}
				else {
					if(bestNotEnhance >= 15 && N++ < nMax) {	
						if(dominate(_worst, child))
							pop[next].add(child);
						else
							pop[next].add(pop[next].size() - 1, child);
					}
				}				
			}
			popDec(pop[next]);
			
			int temp = cur;
			cur = next;
			next = temp;
			++_currentGeneration;
		}
	}
	
	@Override
	public String toString()
	{
		return "Adaptive Population NSGA-III with Dual Control Strategy (APNsgaIII)";
	}
}
