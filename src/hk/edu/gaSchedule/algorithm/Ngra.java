package hk.edu.gaSchedule.algorithm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/****************** Non-dominated Ranking Genetic Algorithm (NRGA) **********************/
public class Ngra<T extends Chromosome<T> > extends NsgaII<T>
{		
	public Ngra(T prototype, int numberOfCrossoverPoints, int mutationSize, float crossoverProbability,
			float mutationProbability) {
		super(prototype, numberOfCrossoverPoints, mutationSize, crossoverProbability, mutationProbability);
	}

	/************** calculate crowding distance function ***************************/
	@Override
	protected Map<Integer, Float> calculateCrowdingDistance(Set<Integer> front, List<T> totalChromosome)
	{
		final int N = _populationSize;
		final float divisor = N * (N + 1);
		Map<Integer, Float> distance = front.stream().collect(Collectors.toMap(Function.identity(), m -> 0.0f));		
		Map<Integer, Float> obj = front.stream().collect(Collectors.toMap(Function.identity(), m -> 2 * m / divisor));

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
	
}

