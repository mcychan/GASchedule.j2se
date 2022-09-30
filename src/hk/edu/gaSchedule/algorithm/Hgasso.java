package hk.edu.gaSchedule.algorithm;
/*
 * Shehadeh, Hisham & Mustafa, Hossam & Tubishat, Mohammad. (2022).
 * A Hybrid Genetic Algorithm and Sperm Swarm Optimization (HGASSO) for Multimodal Functions.
 * International Journal of Applied Metaheuristic Computing. 13. 10.4018/IJAMC.292507.
 * Copyright (c) 2022 Miller Cy Chan
 */

import java.util.ArrayList;
import java.util.List;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class Hgasso<T extends Chromosome<T> > extends NsgaII<T>
{
	private float _decline = .25f;
	private float _sgBestScore;
	private boolean[] _motility;
	private float[] _sBestScore;
	private float[] _sgBest = null;
	private float[][] _current_position = null;
	private float[][] _sBest = null;
	private float[][] _velocity = null;

	// Initializes Hybrid Genetic Algorithm and Sperm Swarm Optimization
	public Hgasso(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability, float mutationProbability)
	{
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);		
	}

	@Override
	protected void initialize(List<T> population)
	{		
		int size = 0;
		int numberOfChromosomes = _populationSize;
		for (int i = 0; i < _populationSize; ++i) {
			List<Float> positions = new ArrayList<>();
			
			// initialize new population with chromosomes randomly built using prototype
			population.add(_prototype.makeNewFromPrototype(positions));			
			
			if(i < 1) {				
				size = positions.size();
				_current_position = new float[numberOfChromosomes][size];
				_velocity = new float[numberOfChromosomes][size];
				_sBest = new float[numberOfChromosomes][size];
				_sgBest = new float[numberOfChromosomes];
				_sBestScore = new float[numberOfChromosomes];
				_motility = new boolean[numberOfChromosomes];
			}
			
			_sBestScore[i] = population.get(i).getFitness();
			for(int j = 0; j < size; ++j) {
				_current_position[i][j] = positions.get(j);
				_velocity[i][j] = (float)(Configuration.rand(-.6464f, .7157f) / 3.0);
			}
		}
	}
	
	private void updateVelocities(List<T> population)
	{
		for (int i = 0; i < population.size(); ++i) {
			if(!_motility[i])
				continue;
			
			int dim = _velocity[i].length;
			for(int j = 0; j < dim; ++j) {
				_velocity[i][j] = (float) (Configuration.random() * Math.log10(Configuration.rand(7.0f, 14.0f)) * _velocity[i][j]
				+ Math.log10(Configuration.rand(7.0f, 14.0f)) * Math.log10(Configuration.rand(35.5f, 38.5f)) * (_sBest[i][j] - _current_position[i][j])
		        + Math.log10(Configuration.rand(7.0f, 14.0f)) * Math.log10(Configuration.rand(35.5f, 38.5f)) * (_sgBest[j] - _current_position[i][j]));
				
				_current_position[i][j] += _velocity[i][j];
			}			
		}
	}
	
	@Override
	protected List<T> replacement(List<T> population)
	{
		float climax = 1 - _decline;
		int populationSize = population.size();
		
		for(int i = 0; i < populationSize; ++i) {
			float fitness = population.get(i).getFitness();
			if(fitness < _sBestScore[i]) {
				population.get(i).updatePositions(_current_position[i]);
				fitness = population.get(i).getFitness();
				_motility[i] = true;
			}
				
			if(fitness > _sBestScore[i]) {
				_sBestScore[i] = fitness;
				population.get(i).extractPositions(_current_position[i]);		
				_sBest[i] = _current_position[i].clone();				
			}
			
			if(fitness > _sgBestScore) {
				_sgBestScore = fitness;
				population.get(i).extractPositions(_current_position[i]);
				_sgBest = _current_position[i].clone();
				_motility[i] = !_motility[i];
			}
			
			if(_repeatRatio > _sBestScore[i])
				_sBestScore[i] -= _repeatRatio * _decline;
			if(_repeatRatio > climax && _sgBestScore > climax) {
				if (i > (populationSize * _sgBestScore)) {
					population.get(i).updatePositions(_current_position[i]);
					_motility[i] = true;
				}					
			}
		}
		
		updateVelocities(population);
		return super.replacement(population);
	}
	
	@Override
	public String toString()
	{
		return "Hybrid Genetic Algorithm and Sperm Swarm Optimization (HGASSO)";
	}
}
