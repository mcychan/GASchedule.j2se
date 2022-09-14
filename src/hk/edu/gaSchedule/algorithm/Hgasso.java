package hk.edu.gaSchedule.algorithm;
/*
 * Shehadeh, Hisham & Mustafa, Hossam & Tubishat, Mohammad. (2022).
 * A Hybrid Genetic Algorithm and Sperm Swarm Optimization (HGASSO) for Multimodal Functions.
 * International Journal of Applied Metaheuristic Computing. 13. 10.4018/IJAMC.292507. 
 */

import java.util.ArrayList;
import java.util.List;

import hk.edu.gaSchedule.model.Chromosome;
import hk.edu.gaSchedule.model.Configuration;

public class Hgasso<T extends Chromosome<T> > extends NsgaII<T>
{
	private float _sgBestScore;
	private double _threshold = .75;
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
			}
			
			_sBestScore[i] = population.get(i).getFitness();
			for(int j = 0; j < size; ++j) {
				_current_position[i][j] = positions.get(j);
				_velocity[i][j] = (float)(Configuration.rand(-.6464f, .7157f) / 3.0);
			}
		}
	}
	
	protected void updatePositions(T chromosome, int pos)
	{
		chromosome.updatePositions(_current_position[pos]);
	}
	
	private void updateVelocities(List<T> population)
	{
		for (int i = 0; i < population.size(); ++i) {
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
		int start = (int) (population.size() * _threshold);
		for(int i = start; i < population.size(); ++i) {
			updatePositions(population.get(i), i);
			float fitness = population.get(i).getFitness();
				
			if(fitness > _sBestScore[i]) {
				_sBestScore[i] = fitness;
				_sBest[i] = _current_position[i];
			}
			
			if(fitness > _sgBestScore) {
				_sgBestScore = fitness;
				_sgBest = _current_position[i];
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
