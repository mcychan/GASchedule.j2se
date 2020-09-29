package hk.edu.gaSchedule.algorithm;

public interface Chromosome<T extends Chromosome<T> > {

	public T makeNewFromPrototype();

    public float getFitness();

    public Configuration getConfiguration();

    public T crossover(T mother, int numberOfCrossoverPoints, float crossoverProbability);

    public void mutation(int mutationSize, float mutationProbability);
    
}
