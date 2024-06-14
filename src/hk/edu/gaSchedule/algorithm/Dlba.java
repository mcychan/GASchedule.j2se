package hk.edu.gaSchedule.algorithm;
/*
 * Xie, Jian & Chen, Huan. (2013).
 * A Novel Bat Algorithm Based on Differential Operator and Lévy Flights Trajectory.
 * Computational intelligence and neuroscience. 2013. 453812. 10.1155/2013/453812. 
 * Copyright (c) 2024 Miller Cy Chan
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class Dlba<T extends Chromosome<T> > extends NsgaIII<T> {
	private int _currentGeneration, _max_iterations = 5000;

	private int _chromlen, _minValue = 0;

	private double _alpha, _pa;

	private float[] _frequency, _rate;
	private double[] _loudness;

	private float[] _gBest = null;
	private float[][] _position = null;
	private float[][] _velocity = null;

	private List<Integer> _maxValues;
	private LévyFlights<T> _lf;	
	
	// Initializes Bat algorithm
	public Dlba(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);

		_alpha = 0.9;
		_pa = .25;
	}
	
	protected void initialize(List<T> population)
	{
		_maxValues = new ArrayList<>();
		_prototype.makeEmptyFromPrototype(_maxValues);
		
		for (int i = 0; i < _populationSize; ++i) {
			List<Float> positions = new ArrayList<>();
			
			// initialize new population with chromosomes randomly built using prototype
			population.add(_prototype.makeNewFromPrototype(positions));	
			
			if(i < 1) {
				_chromlen = positions.size();
				_frequency = new float[_chromlen];
				_rate = new float[_populationSize];
				_loudness = new double[_populationSize];
				_position = new float[_populationSize][_chromlen];
				_velocity = new float[_populationSize][_chromlen];
				_lf = new LévyFlights<T>(_chromlen, null);
			}
			
			_rate[i] = (float) Configuration.random();
			_loudness[i] = (float) Configuration.random() + 1;
		}
	}
	
	@Override
	protected void reform()
	{
		Configuration.seed();
		if(_crossoverProbability < 95)
			_crossoverProbability += 1.0f;
		else if(_pa < .5)
			_pa += .01;
	}
	
	private float getMean(float[] array)
	{
		float sum = 0f;
		for(float value : array)
			sum += value;
		return sum / array.length;
	}
	
	private void updateVelocities(List<T> population)
	{
		double mean = Arrays.stream(_loudness).average().orElse(0.0);

		T globalBest = _prototype.makeEmptyFromPrototype(null);
		globalBest.updatePositions(_gBest);
		T localBest = _prototype.makeNewFromPrototype(null);
	
		for (int i = 0; i < _populationSize; ++i) {
			float beta = (float) Configuration.random();
			double rand = Configuration.random();
			double n = Configuration.rand(-1.0, 1.0);

			int dim = _velocity[i].length;
			for(int j = 0; j < dim; ++j) {
				_frequency[j] = ((_maxValues.get(j) - _minValue) * _currentGeneration / (float) n + _minValue) * beta;
				_velocity[i][j] += (_position[i][j] - _gBest[j]) * _frequency[j];
				
				if (rand > _rate[i]) {
					_position[i][j] += _velocity[i][j];
					if(_position[i][j] > _maxValues.get(j)) {
						_position[i][j] = _maxValues.get(j);
						_velocity[i][j] = _minValue;
					}
					else if(_position[i][j] < _minValue)
						_position[i][j] = _velocity[i][j] = _minValue;
				}
			}
			
			T localTemp = _prototype.makeEmptyFromPrototype(null);
			localTemp.updatePositions(_position[i]);
			if (localTemp.dominates(localBest))
				localBest = localTemp;
		}

		for (int i = 0; i < _populationSize; ++i) {
			float[][] positionTemp = _position.clone();
			double rand = Configuration.random();
			if (rand < _loudness[i]) {
				double n = Configuration.rand(-1.0, 1.0);
				int dim = _velocity[i].length;
				for(int j = 0; j < dim; ++j) {
					positionTemp[i][j] = (float) (_gBest[j] + n * mean);
					if(positionTemp[i][j] > _maxValues.get(j)) {
						positionTemp[i][j] = _maxValues.get(j);
						_velocity[i][j] = _minValue;
					}
					else if(positionTemp[i][j] < _minValue)
						positionTemp[i][j] = _velocity[i][j] = _minValue;
				}
				
				if (globalBest.dominates(localBest)) {
					_position[i] = positionTemp[i];
					_rate[i] *= Math.pow(_currentGeneration / n, 3);
					_loudness[i] *= _alpha;
				}
			}
			
			_position[i] = _lf.optimum(_position[i], population.get(i));
		}
	}
	
	@Override
	protected List<T> replacement(List<T> population)
	{
		_gBest = _lf.updateVelocities(population, _populationSize, _position, _gBest);
		updateVelocities(population);
		
		for (int i = 0; i < _populationSize; ++i) {
			T chromosome = _prototype.makeEmptyFromPrototype(null);
			chromosome.updatePositions(_position[i]);
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
		_currentGeneration = 0;
		int bestNotEnhance = 0;
		double lastBestFit = 0.0;

		int cur = 0, next = 1;
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
			++_currentGeneration;
		}
	}
	
	@Override
	public String toString()
	{
		return "Bat algorithm with differential operator and Levy flights trajectory (DLBA)";
	}
}
