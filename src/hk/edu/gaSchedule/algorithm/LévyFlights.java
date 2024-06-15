package hk.edu.gaSchedule.algorithm;

import java.util.List;
import java.util.Random;

import hk.edu.gaSchedule.model.Chromosome;

final class LévyFlights<T extends Chromosome<T> > {

	private int _chromlen;
	private double _beta, _σu, _σv;	
	
	private static Random _random = null;
	
	LévyFlights(int chromlen, Random random)
	{
		_chromlen = chromlen;
		_random = random != null ? random : new Random(System.currentTimeMillis());		
		
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
	
	float[] optimum(float[] localVal, T chromosome)
	{
		T localBest = chromosome.makeEmptyFromPrototype(null);
		localBest.updatePositions(localVal);
		
		if(localBest.dominates(chromosome)) {
			chromosome.updatePositions(localVal);
			return localVal;
		}
		
		float[] positions = new float[_chromlen];
		chromosome.extractPositions(positions);
		return positions;
	}
	
	float[] updatePosition(T chromosome, float[][] currentPosition, int i, float[] gBest)
	{
		float[] curPos = currentPosition[i].clone();
		double u = _random.nextGaussian() * _σu;
		double v = _random.nextGaussian() * _σv;
		double S = u / Math.pow(Math.abs(v), 1 / _beta);
		
		if(gBest == null) {
			gBest = new float[_chromlen];
			chromosome.extractPositions(gBest);
		}
		else
			gBest = optimum(gBest, chromosome);

		for(int j = 0; j < _chromlen; ++j)
			currentPosition[i][j] += (float) (_random.nextGaussian() * 0.01 * S * (curPos[j] - gBest[j]));

		currentPosition[i] = optimum(currentPosition[i], chromosome);
		return gBest;
	}
	
	float[] updatePositions(List<T> population, int populationSize, float[][] currentPosition, float[] gBest)
	{
		for(int i = 0; i < populationSize; ++i)
			gBest = updatePosition(population.get(i), currentPosition, i, gBest);

		return gBest;
	}
}
