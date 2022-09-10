package hk.edu.gaSchedule.model;

public interface Chromosome<T extends Chromosome<T> > {

	public T makeNewFromPrototype();

    public float getFitness();

    public Configuration getConfiguration();

    public T crossover(T mother, int numberOfCrossoverPoints, float crossoverProbability);
    
    public T crossover(T parent, T r1, T r2, T r3, float etaCross, float crossoverProbability);

    public void mutation(int mutationSize, float mutationProbability);
    
    public int getDifference(T other);
    
    public float getDiversity();
    
    public void setDiversity(float diversity);

    public int getRank();
    
    public void setRank(int rank);
    
}
