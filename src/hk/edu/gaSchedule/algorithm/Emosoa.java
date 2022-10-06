package hk.edu.gaSchedule.algorithm;
/*
 * Dhiman, Gaurav & Singh, Krishna & Slowik, Adam & Chang, Victor & Yildiz, Ali & Kaur, Amandeep & Garg, Meenakshi. (2021).
 * EMoSOA: A New Evolutionary Multi-objective Seagull Optimization Algorithm for Global Optimization.
 * International Journal of Machine Learning and Cybernetics. 12. 10.1007/s13042-020-01189-1.
 * Copyright (c) 2022 Miller Cy Chan
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class Emosoa<T extends Chromosome<T> > extends NsgaII<T>
{
	private int _currentGeneration = 0, _max_iterations = 5000;
	private float _gBestScore;
	private float[] _bestScore;
	private float[] _gBest = null;
	private float[][] _current_position = null;

	// Initializes Evolutionary multi-objective seagull optimization algorithm
	public Emosoa(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);		
	}
	
	private void exploitation(List<T> population)
	{
		int b = 1;
		float Fc = 2f - _currentGeneration * (2f / _max_iterations);
		double tau = 2 * Math.PI;
		for (int i = 0; i < population.size(); ++i) {
			int dim = _current_position[i].length;
			for(int j = 0; j < dim; ++j) {
				double A1 = 2 * Fc * Configuration.random() - Fc;
				double ll = (Fc - 1) * Configuration.random() + 1;

				double D_alphs = Fc * _current_position[i][j] + A1 * (_gBest[j] - _current_position[i][j]);
				_current_position[i][j] = (float)(D_alphs * Math.exp(b * ll) * Math.cos(ll * tau) + _gBest[j]);
			}
		}
	}
	
	@Override
	protected List<T> replacement(List<T> population)
	{
		int populationSize = population.size();
		float climax = .9f;
		
		for(int i = 0; i < populationSize; ++i) {
			float fitness = population.get(i).getFitness();
			if(fitness < _bestScore[i]) {
				population.get(i).updatePositions(_current_position[i]);
				fitness = population.get(i).getFitness();
			}
				
			if(fitness > _bestScore[i]) {
				_bestScore[i] = fitness;
				population.get(i).extractPositions(_current_position[i]);
			}
			
			if(fitness > _gBestScore) {
				_gBestScore = fitness;
				population.get(i).extractPositions(_current_position[i]);
				_gBest = _current_position[i].clone();
			}			

			if(_repeatRatio > climax && _gBestScore > climax) {
				if (i > (populationSize * _repeatRatio))
					population.get(i).updatePositions(_current_position[i]);									
			}
		}
		
		exploitation(population);
		return super.replacement(population);
	}
	
	@Override
	protected void initialize(List<T> population)
	{		
		int size = 0;
		int numberOfChromosomes = _populationSize;
		for (int i = 0; i < _populationSize; ++i) {
			List<Float> positions = new ArrayList<>();
			
			// add new search agent to population
			population.add(_prototype.makeNewFromPrototype(positions));
			
			if(i < 1) {				
				size = positions.size();
				_current_position = new float[numberOfChromosomes][size];
				_gBest = new float[numberOfChromosomes];
				_bestScore = new float[numberOfChromosomes];
			}
			
			_bestScore[i] = population.get(i).getFitness();
			for(int j = 0; j < size; ++j)
				_current_position[i][j] = positions.get(j);
		}
	}
	
	// Starts and executes algorithm
	@Override
	public void run(int maxRepeat, double minFitness)
    {
		if (_prototype == null)
			return;

		List<T> population = new ArrayList<>();
		initialize(population);		

		int repeat = 0;
		double lastBestFit = 0.0;

		while(_currentGeneration < _max_iterations)
		{
			T best = getResult();
			if(_currentGeneration > 0) {
				String status = String.format("\rFitness: %f\t Generation: %d", best.getFitness(), _currentGeneration);
				System.out.print(status);
				
				// algorithm has reached criteria?
				if (best.getFitness() > minFitness)
					break;
	
				double difference = Math.abs(best.getFitness() - lastBestFit);
				if (difference <= 0.0000001)
					++repeat;
				else
					repeat = 0;

				_repeatRatio = repeat * 100.0f / maxRepeat;
				if (repeat > (maxRepeat / 100))		
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
			if(_currentGeneration == 0)
				_chromosomes = population;
			else {
				totalChromosome = new ArrayList<>(population);
				totalChromosome.addAll(_chromosomes);
				List<Set<Integer> > newBestFront = nonDominatedSorting(totalChromosome);
				_chromosomes = selection(newBestFront, totalChromosome);
				lastBestFit = best.getFitness();
			}			
			++_currentGeneration;
		}
	}
	
	@Override
	public String toString()
	{
		return "Evolutionary multi-objective seagull optimization algorithm for global optimization (EMoSOA)";
	}
}
