package hk.edu.gaSchedule.algorithm;
/*
 * X. -S. Yang and Suash Deb, "Cuckoo Search via Lévy flights,"
 * 2009 World Congress on Nature & Biologically Inspired Computing (NaBIC), Coimbatore, India,
 * 2009, pp. 210-214, doi: 10.1109/NABIC.2009.5393690.
 * Copyright (c) 2023 Miller Cy Chan
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class Cso<T extends Chromosome<T> > extends NsgaIII<T> {
	private int _max_iterations = 5000;
	
	private int _chromlen;
	
	private double _pa, _beta, _σu, _σv;

	private float[][] _current_position = null;
	
	private static Random _random = new Random(System.currentTimeMillis());

	// Initializes Cso
	public Cso(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);		
		_pa = .25;
		_beta = 1.5;
		
		double num = gamma(1 + _beta) * Math.sin(Math.PI * _beta / 2);
		double den = gamma((1 + _beta) / 2) * _beta * Math.pow(2, (_beta - 1) / 2);
		_σu = Math.pow(num / den, 1 / _beta);
		_σv = 1;
	}

	private static double gamma(double z)
	{
		if (z < 0.5)
			return Math.PI / Math.sin(Math.PI * z) / gamma(1.0 - z);

		// Lanczos approximation g=5, n=7
		double[] coef = new double[] { 1.000000000190015, 76.18009172947146, -86.50532032941677,
		24.01409824083091, -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5 };

		double zz = z - 1.0;
		double b = zz + 5.5; // g + 0.5
		double sum = coef[0];
		for (int i = 1; i < coef.length; ++i)
			sum += coef[i] / (zz + i);

		double LogSqrtTwoPi = 0.91893853320467274178;
		return Math.exp(LogSqrtTwoPi + Math.log(sum) - b + Math.log(b) * (zz + 0.5));
	}
	
	protected void initialize(List<T> population)
	{
		for (int i = 0; i < _populationSize; ++i) {			
			List<Float> positions = new ArrayList<>();
			
			// initialize new population with chromosomes randomly built using prototype
			population.add(_prototype.makeNewFromPrototype(positions));	
			
			if(i < 1) {
				_chromlen = positions.size();
				_current_position = new float[_populationSize][_chromlen];
			}

		}
	}
	
	private float[] optimum(float[] localVal, T chromosome)
	{
		T localBest = _prototype.makeEmptyFromPrototype(null);
		localBest.updatePositions(localVal);
		
		if(localBest.dominates(chromosome)) {
			chromosome.updatePositions(localVal);
			return localVal;
		}
		
		float[] positions = new float[_chromlen];
		chromosome.extractPositions(positions);
		return positions;
	}

	private void updatePosition1(List<T> population)
	{
		float[][] current_position = _current_position.clone();
		float[] sBestScore = null;
		for(int i = 0; i < _populationSize; ++i) {
			double u = _random.nextGaussian() * _σu;
			double v = _random.nextGaussian() * _σv;
			double S = u / Math.pow(Math.abs(v), 1 / _beta);
			
			if(i == 0) {
				sBestScore = new float[_chromlen];
				population.get(i).extractPositions(sBestScore);
			}
			else
				sBestScore = optimum(sBestScore, population.get(i));

			for(int j = 0; j < _chromlen; ++j)
				_current_position[i][j] += (float) (_random.nextGaussian() * 0.01 * S * (current_position[i][j] - sBestScore[j]));

			_current_position[i] = optimum(_current_position[i], population.get(i));
		}
	}
	
	private void updatePosition2(List<T> population)
	{
		float[][] current_position = _current_position.clone();
		for (int i = 0; i < _populationSize; ++i) {
			for(int j = 0; j < _chromlen; ++j) {
				double r = Configuration.random();
				if(r < _pa) {
					int d1 = Configuration.rand(5);
					int d2;
					do {
						d2 = Configuration.rand(5);
					} while(d1 == d2);
					_current_position[i][j] += (float) (Configuration.random() * (current_position[d1][j] - current_position[d2][j]));
				}
			}
			_current_position[i] = optimum(_current_position[i], population.get(i));
		}
	}
	
	@Override
	protected List<T> replacement(List<T> population)
	{
		updatePosition1(population);
		updatePosition2(population);
		
		for (int i = 0; i < _populationSize; ++i) {
			T chromosome = _prototype.makeEmptyFromPrototype(null);
			chromosome.updatePositions(_current_position[i]);
			population.set(i, chromosome);
		}

		return super.replacement(population);
	}
	
	// Starts and executes algorithm
	public void run(int maxRepeat, double minFitness)
	{
		if (_prototype == null)
			return;

		List<T>[] pop = new ArrayList[2];
		pop[0] = new ArrayList<>();
		initialize(pop[0]);

		// Current generation
		int currentGeneration = 0;
		int bestNotEnhance = 0;
		double lastBestFit = 0.0;

		int cur = 0, next = 1;
		while(currentGeneration < _max_iterations)
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

				if (bestNotEnhance > (maxRepeat / 100))		
					reform();
			}			
			
			/******************* crossover *****************/
			List<T> offspring = crossing(pop[cur]);			
			
			/******************* mutation *****************/
			for(T child : offspring)
				child.mutation(_mutationSize, _mutationProbability);
			
			pop[cur].addAll(offspring);
			
			/******************* replacement *****************/	
			pop[next] = replacement(pop[cur]);
			_best = pop[next].get(0).dominates( pop[cur].get(0)) ? pop[next].get(0) : pop[cur].get(0);
			
			int temp = cur;
			cur = next;
			next = temp;
			++currentGeneration;
		}
	}
	
	@Override
	public String toString()
	{
		return "Cuckoo Search Optimization (CSO)";
	}
}
