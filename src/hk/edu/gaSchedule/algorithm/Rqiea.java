package hk.edu.gaSchedule.algorithm;

/*
 * Zhang, G.X., Rong, H.N., Real-observation quantum-inspired evolutionary algorithm
 * for a class of numerical optimization problems. In: Lecture Notes
 * in Computer Science, vol. 4490, pp. 989–996 (2007).
 * Copyright (c) 2023 Miller Cy Chan
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class Rqiea<T extends Chromosome<T> > extends NsgaII<T>
{
	private int _currentGeneration = 0, _max_iterations = 5000;
	
	private float[] _Q; // quantum population
	private float[] _P; // observed classical population

	private float[][] _bounds;
	private int _chromlen, _catastrophe;

	private T _bestval;
	private float[] _best;
	private float[][] _bestq;
	
	private int _updated = 0;

	// Initializes Real observation QIEA
	public Rqiea(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);	
	}

	@Override
	protected void initialize(List<T> population)
	{		
		_chromlen = 0;
		_catastrophe = (int) _mutationProbability;
		_bestval = null;
		
		List<Integer> bounds = new ArrayList<>();
		for (int i = 0; i < _populationSize; ++i) {		
			if(i < 1) {				
				// initialize new population with chromosomes randomly built using prototype
				population.add(_prototype.makeEmptyFromPrototype(bounds));
				
				_chromlen = bounds.size();
				_Q = new float[_populationSize * _chromlen * 2];
				_P = new float[_populationSize * _chromlen];
				_bounds = new float[_chromlen][2];
				_best = new float[_chromlen];
				_bestq = new float[_chromlen][2];
			}
			else
				population.add(_prototype.makeEmptyFromPrototype(null));
			
			for (int j = 0; j < _chromlen; ++j) {
				int qij = i * 2 * _chromlen + 2 * j;
				float alpha = 2.f * (float) Configuration.random() - 1;
				float beta = (float) (Math.sqrt(1 - alpha * alpha) * ((Configuration.rand(Integer.MAX_VALUE) % 2 != 0) ? -1 : 1));
				_Q[qij] = alpha;
				_Q[qij + 1] = beta;
			}
		}
		
		for (int i = 0; i < bounds.size(); ++i)
			_bounds[i][1] = bounds.get(i);
	}

	private void observe() {
		_updated = 0;
		for (int i = 0; i < _populationSize; ++i) {
			for (int j = 0; j < _chromlen; ++j) {
				int pij = i * _chromlen + j;
				int qij = 2 * pij;
				
				if (Configuration.random() <= .5)
					_P[pij] = _Q[qij] * _Q[qij];
				else
					_P[pij] = _Q[qij + 1] * _Q[qij + 1];
				
				_P[pij] *= _bounds[j][1] - _bounds[j][0];
				_P[pij] += _bounds[j][0];				
			}
			
			int start = i * _chromlen;
			float[] positions = Arrays.copyOfRange(_P, start, start + _chromlen + 1);
			T chromosome = _prototype.makeEmptyFromPrototype(null);
			chromosome.updatePositions(positions);
			if((Configuration.rand(100) <= _catastrophe && i > _catastrophe) || chromosome.getFitness() > _chromosomes.get(i).getFitness()) {
				_chromosomes.set(i, chromosome);
				++_updated;
			}
			else {
				_chromosomes.get(i).extractPositions(positions);
				System.arraycopy(positions, 0, _P, start, _chromlen);
			}
		}
	}
	
	private void storebest() {
		int i_best = 0;
		for (int i = 1; i < _populationSize; i++) {
			if (_chromosomes.get(i).dominates(_chromosomes.get(i_best)))
				i_best = i;
		}
		
		if (_bestval == null || _chromosomes.get(i_best).dominates(_bestval)) {
			_bestval = _chromosomes.get(i_best);
			System.arraycopy(_P, i_best * _chromlen, _best, 0, _chromlen);
			
			int start = i_best * _chromlen * 2;
			for(int i = start, j = 0; i < start + _chromlen * 2; ++j) {
				_bestq[j][0] = _Q[i++];
				_bestq[j][1] = _Q[i++];
			}
		}
	}
	
	private void evaluate() {
		// not implemented			
	}
	
	private float sign(double x) {
		if (x > 0)
			return 1;
		if (x < 0)
			return -1;
		return 0;
	}
	
	private float lut(float alpha, float beta, float alphabest, float betabest) {
		final double M_PI_2 = Math.PI / 2;
		float eps = 1e-5f;
		float xi = (float) Math.atan(beta / alpha);
		float xi_b = (float) Math.atan(betabest / alphabest);
		if (Math.abs(xi_b) < eps || Math.abs(xi) < eps // xi_b or xi = 0
				|| Math.abs(xi_b - M_PI_2) < eps || Math.abs(xi_b - M_PI_2) < eps // xi_b or xi = pi/2
				|| Math.abs(xi_b + M_PI_2) < eps || Math.abs(xi_b + M_PI_2) < eps) // xi_b or xi = -pi/2
		{
			return (Configuration.rand(Integer.MAX_VALUE) % 2 != 0) ? -1 : 1;
		}

		if (xi_b > 0 && xi > 0)
			return xi_b >= xi ? 1 : -1;

		if (xi_b > 0 && xi < 0)
			return sign(alpha * alphabest);

		if (xi_b < 0 && xi > 0)
			return -sign(alpha * alphabest);

		if (xi_b < 0 && xi < 0)
			return xi_b >= xi ? 1 : -1;

		return sign(xi_b);
	}
	
	private void update() {
		for (int i = 0; i < _populationSize; ++i) {
			for (int j = 0; j < _chromlen; ++j) {
				int qij = 2 * (i * _chromlen + j);
				float[] qprim = new float[2];

				double k = Math.PI / (100 + _currentGeneration % 100);
				double theta = k * lut(_Q[qij], _Q[qij + 1], _bestq[j][0], _bestq[j][1]);

				qprim[0] = (float) (_Q[qij] * Math.cos(theta) + _Q[qij + 1] * (-Math.sin(theta)));
				qprim[1] = (float) (_Q[qij] * Math.sin(theta) + _Q[qij + 1] * (Math.cos(theta)));

				_Q[qij] = qprim[0];
				_Q[qij + 1] = qprim[1];
			}
		}
	}
	
	private void recombine() {
		int j;
		int i = Configuration.rand(_populationSize);
		do {
			j = Configuration.rand(_populationSize);
		} while (i == j);

		int h1 = Configuration.rand(_chromlen);
		int h2 = Configuration.rand(_chromlen - h1) + h1;

		int q1 = i * _chromlen * 2;
		int q2 = j * _chromlen * 2;

		float[] buf = new float[2 * _chromlen];
		System.arraycopy(_Q, q1, buf, 0, 2 * _chromlen);

		System.arraycopy(_Q, q2 + h1, _Q, q1 + h1 * 2, (h2 - h1) * 2);
		System.arraycopy(buf, h1, _Q, q2 + h1 * 2, (h2 - h1) * 2);

		for (int k = h1; k < h2; ++k) {
			float tmp = _Q[q1 + k * 2];
			_Q[q1 + k * 2] = _Q[q2 + k * 2];
			_Q[q2 + k * 2] = tmp;
		}
	}	
	
	// Returns pointer to best chromosomes in population
	public T getResult()
    {
		if(_bestval == null)
			return null;
		return _bestval;
    }
	
	// Starts and executes algorithm
	public void run(int maxRepeat, double minFitness)
    {
		if (_prototype == null)
			return;

		List<T> population = new ArrayList<>();
		initialize(population);
		_chromosomes = population;
		_currentGeneration = 0;
		observe();
		evaluate();
		storebest();

		int bestNotEnhance = 0;
		double lastBestFit = 0.0;

		while(_currentGeneration < _max_iterations) {				
			T best = getResult();
			if(_currentGeneration > 0) {
				String status = String.format("\rFitness: %f\t Generation: %d \t Updated: %d", best.getFitness(), _currentGeneration, _updated);
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
			List<T> offspring = crossing(_chromosomes);			
			
			/******************* mutation *****************/
			for(T child : offspring)
				child.mutation(_mutationSize, _mutationProbability);
			
			List<T> totalChromosome = new ArrayList<>(population);
			totalChromosome.addAll(offspring);
			
			/******************* non-dominated sorting *****************/
			List<Set<Integer> > front = nonDominatedSorting(totalChromosome);
			
			/******************* selection *****************/
			population = replacement(front, totalChromosome);
			_populationSize = population.size();
			
			/******************* comparison *****************/
			if(_currentGeneration > 0) {
				totalChromosome = new ArrayList<>(population);
				totalChromosome.addAll(_chromosomes);
				List<Set<Integer> > newBestFront = nonDominatedSorting(totalChromosome);
				_chromosomes = replacement(newBestFront, totalChromosome);
				
				for (int i = 0; i < _populationSize; ++i) {
					float[] positions = new float[_chromlen];
					int start = i * _chromlen;
					_chromosomes.get(i).extractPositions(positions);
					System.arraycopy(positions, 0, _P, start, _chromlen);
				}
			}
			
			observe();
			evaluate();
			storebest();
			update();
			recombine();
			++_currentGeneration;
		}
	}

	
	@Override
	public String toString()
	{
		return "Real observation QIEA (rQIEA)";
	}
}