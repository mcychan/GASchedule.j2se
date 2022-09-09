package hk.edu.gaSchedule.algorithm;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hk.edu.gaSchedule.model.Configuration;

/****************** Non-dominated Ranking Genetic Algorithm (NRGA) **********************/
public class Ngra<T extends Chromosome<T> > extends NsgaII<T>
{		
	public Ngra(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability,
			float mutationProbability) {
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);
	}

	/************** ranked based roulette wheel function ***************************/
	@Override
	protected List<T> replacement(List<T> population)
	{
		Map<Integer, Float> obj = IntStream.range(0, population.size()).boxed()
			.collect(Collectors.toMap(Function.identity(), m -> population.get(m).getFitness()));
		List<Integer> sortedIndices = obj.entrySet().stream().sorted(Entry.comparingByValue(Comparator.reverseOrder()))
			.map(e -> e.getKey()).collect(Collectors.toList());
		
		final int totalFitness = (population.size() + 1) * population.size() / 2;
		
		List<Double> probSelection = IntStream.range(0, population.size()).mapToDouble(i -> i * 1.0 / totalFitness).boxed().collect(Collectors.toList());
		List<Double> cumProb = IntStream.range(0, population.size()).mapToObj(i -> probSelection.subList(0, i + 1).stream()
			.mapToDouble(Double::doubleValue).sum()).collect(Collectors.toList());
		
		double[] selectIndices = IntStream.range(0, population.size()).mapToDouble(i -> Configuration.random()).toArray();		
		
		T[] parent = (T[]) Array.newInstance(_prototype.getClass(), 2);
		int parentIndex = 0;
		List<T> offspring = new ArrayList<>();
		for(int i = 0; i < population.size(); ++i) {			
			boolean selected = false;
			for(int j = 0; j < population.size() - 1; ++j) {
				if(cumProb.get(j) < selectIndices[i] && cumProb.get(j+1) >= selectIndices[i]) {
					parent[parentIndex++ % 2] = population.get(sortedIndices.get(j+1));
					selected = true;
					break;
				}
			}
			
			if(!selected)
				parent[parentIndex++ % 2] = population.get(sortedIndices.get(i));
			
			if(parentIndex % 2 == 0) {
				T child0 = parent[0].crossover(parent[1], _numberOfCrossoverPoints, _crossoverProbability);
				T child1 = parent[1].crossover(parent[0], _numberOfCrossoverPoints, _crossoverProbability);
				offspring.add(child0);
				offspring.add(child1);
			}
		}

		return offspring;
	}
	
	@Override
	protected void initialize(List<T> population)
	{
		super.initialize(population);
		List<T> offspring = replacement(population);
		population.clear();
		population.addAll(offspring);
	}
	
	@Override
	public String toString()
	{
		return "Non-dominated Ranking Genetic Algorithm (NRGA)";
	}
	
}

