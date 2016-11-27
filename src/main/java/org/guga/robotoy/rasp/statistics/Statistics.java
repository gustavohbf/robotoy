/*******************************************************************************
 * Copyright 2016 See https://github.com/gustavohbf/robotoy/blob/master/AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.guga.robotoy.rasp.statistics;

/**
 * Take some statistics over some values.<BR>
 * These statistics are updated as more values are fed.<BR>
 * <BR>
 * References:<BR>
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance<BR>
 * Donald E. Knuth (1998). The Art of Computer Programming, volume 2:
 * Seminumerical Algorithms, 3rd edn., p. 232. Boston: Addison-Wesley.<BR>
 * B. P. Welford (1962)."Note on a method for calculating corrected sums of
 * squares and products". Technometrics 4(3):419-420.<BR>
 * Chan, Tony F.; Golub, Gene H.; LeVeque, Randall J. (1979), "Updating Formulae
 * and a Pairwise Algorithm for Computing Sample Variances.", Technical Report
 * STAN-CS-79-773, Department of Computer Science, Stanford University,
 * ftp://reports.stanford.edu/pub/cstr/reports/cs/tr/79/773/CS-TR-79-773.pdf .
 * Terriberry, Timothy B. (2007), Computing Higher-Order Moments Online,
 * http://people.xiph.org/~tterribe/notes/homs.html<BR>
 * 
 * @author Gustavo Figueiredo
 * 
 *
 */
public class Statistics {

	private volatile double sum = 0;

	private volatile int k = 0;

	private volatile double M = 0, S = 0;

	private volatile double max, min;
	
	public synchronized void feed(double x) {
		if (k == 0) {
			k++;
			M = x;
			S = 0;
			min = x;
			max = x;
			sum = x;
		} else {
			k++;
			double M_prev = M;
			final double delta = (x - M_prev);
			final double delta_n = delta / k;
			final double term1 = delta * delta_n * (k - 1);
			M += delta_n;
			S += term1;
			if (x > max)
				max = x;
			if (x < min)
				min = x;
			sum += x;
		}
	}

	public double getStandardDeviation() {
		if (k > 1) {
			return Math.sqrt(S / (k - 1));
		}
		else {
			return Double.NaN;
		}
	}

	public double getMean() {
		return M;
	}

	public double getMaximum() {
		return max;
	}

	public double getMinimum() {
		return min;
	}

	public double getSum() {
		return sum;
	}

	public int getCount() {
		return k;
	}

	public void reset() {
		sum = 0;
		k = 0;
		M = 0;
		S = 0;
		max = 0;
		min = 0;
	}

}
