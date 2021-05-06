/*-
 * Copyright (c) 2012, 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.january.dataset;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.eclipse.january.asserts.TestUtils;
import org.eclipse.january.asserts.TestUtils.Verbosity;
import org.eclipse.january.dataset.ComplexDoubleDataset;
import org.eclipse.january.dataset.CompoundDataset;
import org.eclipse.january.dataset.CompoundDoubleDataset;
import org.eclipse.january.dataset.CompoundShortDataset;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.DoubleDataset;
import org.eclipse.january.dataset.IndexIterator;
import org.eclipse.january.dataset.Maths;
import org.eclipse.january.dataset.Random;
import org.eclipse.january.dataset.Slice;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MathsTest {
	private final static int SSTEP = 15;
	private final static int SITER = 3;
	private final static double ABSERRD = 1e-8;
	private final static double ABSERRF = 1e-5;
	private final static double RELERR = 1e-5;
	private final static int ISIZEA = 2;
	private final static int ISIZEB = 3;
	private final static int MAXISIZE = Math.max(ISIZEA, ISIZEB);

	private Map<String, Class<? extends Dataset>> classes;

	@Before
	public void setUpClass() {
		classes = new LinkedHashMap<>();
//		classes.put("Boolean", BooleanDataset.class);
		classes.put("Byte", ByteDataset.class);
		classes.put("Short", ShortDataset.class);
		classes.put("Integer", IntegerDataset.class);
		classes.put("Long", LongDataset.class);
		classes.put("Float", FloatDataset.class);
		classes.put("Double", DoubleDataset.class);
		classes.put("ComplexF", ComplexFloatDataset.class);
		classes.put("ComplexD", ComplexDoubleDataset.class);
		classes.put("ArrayB", CompoundByteDataset.class);
		classes.put("ArrayS", CompoundShortDataset.class);
		classes.put("ArrayI", CompoundIntegerDataset.class);
		classes.put("ArrayL", CompoundLongDataset.class);
		classes.put("ArrayF", CompoundFloatDataset.class);
		classes.put("ArrayD", CompoundDoubleDataset.class);
		TestUtils.setVerbosity(Verbosity.QUIET);
	}

	@After
	public void closeDown() {
		TestUtils.setVerbosity(Verbosity.QUIET);
	}

	private void checkDatasets(Object a, Object b, Dataset c, Dataset d) {
		Assert.assertNotNull(c);
		Assert.assertNotNull(d);
		Assert.assertEquals("Class does not match", c.getClass(), d.getClass());
		Assert.assertEquals("Size does not match", c.getSize(), d.getSize());
		Assert.assertEquals("ISize does not match", c.getElementsPerItem(), d.getElementsPerItem());
		Assert.assertArrayEquals("Shape does not match", c.getShapeRef(), d.getShapeRef());

		final IndexIterator ci = c.getIterator(true);
		final IndexIterator di = d.getIterator();
		final int is = c.getElementsPerItem();

		final double abserr = c.getElementClass().equals(Float.class) ? ABSERRF : ABSERRD;

		if (is == 1) {
			while (ci.hasNext() && di.hasNext()) {
				double av = c.getElementDoubleAbs(ci.index);
				double bv = d.getElementDoubleAbs(di.index);
				double tol = Math.max(abserr, Math.abs(av*RELERR));
				if (Math.abs(av - bv) > tol) {
					if (a != null) {
						if (a instanceof Dataset)
							System.err.printf("A was %s ", ((Dataset) a).getString(ci.getPos()));
						else
							System.err.printf("A was %s ", a);
					}
					if (b != null) {
						if (b instanceof Dataset)
							System.err.printf("B was %s ", ((Dataset) b).getString(ci.getPos()));
						else
							System.err.printf("B was %s ", b);
					}
					System.err.printf("at %s\n", Arrays.toString(ci.getPos()));
				}
				Assert.assertEquals("Value does not match at " + Arrays.toString(ci.getPos()) + ", with tol " + tol + ": ",
						av, bv, tol);
			}
		} else {
			while (ci.hasNext() && di.hasNext()) {
				for (int j = 0; j < is; j++) {
					double av = c.getElementDoubleAbs(ci.index + j);
					double bv = d.getElementDoubleAbs(di.index + j);
					double tol = Math.max(abserr, Math.abs(av*RELERR));
					if (Math.abs(av - bv) > tol) {
						if (a != null) {
							if (a instanceof Dataset)
								System.err.printf("A was %s ", ((Dataset) a).getString(ci.getPos()));
							else
								System.err.printf("A was %s ", a);
						}
						if (b != null) {
							if (b instanceof Dataset)
								System.err.printf("B was %s ", ((Dataset) b).getString(ci.getPos()));
							else
								System.err.printf("B was %s ", b);
						}
						System.err.printf("at %s\n", Arrays.toString(ci.getPos()));
					}
					Assert.assertEquals("Value does not match at " + Arrays.toString(ci.getPos()) + "; " + j +
							", with tol " + tol + ": ", av, bv, tol);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAddition() {
		Dataset a, b, c = null, d = null;
		Complex zv = new Complex(-3.5, 0);
		final double dv = zv.getReal();
		long start;
		int n;
		int eCount = 0;

		for (String dn : classes.keySet()) {
			final Class<? extends Dataset> dClass = classes.get(dn);
			Random.seed(12735L);
			for (String en : classes.keySet()) {
				final Class<? extends Dataset> eClass = classes.get(en);

				TestUtils.verbosePrintf("%s to %s, ", dn, en);

				n = 32;
				for (int i = 0; i < SITER; i++) {
					if (!InterfaceUtils.isCompound(dClass)) {
						a = Random.randn(n).imultiply(100);
						a = a.cast(dClass);
					} else {
						Dataset[] aa = new Dataset[ISIZEA];
						for (int j = 0; j < ISIZEA; j++) {
							aa[j] = Random.randn(n).imultiply(100);
						}
						a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
					}
					if (!InterfaceUtils.isCompound(eClass)) {
						b = Random.randn(n).imultiply(100);
						b = b.cast(eClass);
					} else {
						Dataset[] ab = new Dataset[ISIZEB];
						for (int j = 0; j < ISIZEB; j++) {
							ab[j] = Random.randn(n).imultiply(100);
						}
						b = DatasetUtils.cast((Class<CompoundDataset>) eClass, ab);
					}

					start = -System.nanoTime();
					try {
						c = Maths.add(a, b);
					} catch (IllegalArgumentException e) {
						TestUtils.verbosePrintf("Could not perform this operation: %s\n\n", e.getMessage());
						eCount++;
						continue;
					}
					start += System.nanoTime();
					double ntime = ((double) start) / c.getSize();

					d = DatasetFactory.zeros(c);
					start = -System.nanoTime();
					IndexIterator ita = a.getIterator();
					IndexIterator itb = b.getIterator();
					int j = 0;
					if (InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).add((Complex) b
											.getObjectAbs(itb.index)));
							j += is;
						}
					} else if (InterfaceUtils.isComplex(dClass) && !InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).add(new Complex(b.getElementDoubleAbs(itb.index), 0)));
							j += is;
						}
					} else if (!InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, new Complex(a.getElementDoubleAbs(ita.index), 0).add((Complex) b.getObjectAbs(itb.index)));
							j += is;
						}
					} else {
						if (!InterfaceUtils.isCompound(dClass) && !InterfaceUtils.isCompound(eClass)) {
							while (ita.hasNext() && itb.hasNext()) {
								d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue()
												+ ((Number) b.getObjectAbs(itb.index)).doubleValue());
							}
						} else {
							final double[] answer = new double[MAXISIZE];
							final int is = d.getElementsPerItem();
							if (a.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double da = a.getElementDoubleAbs(ita.index);
									for (int k = 0; k < ISIZEB; k++) {
										answer[k] = da + b.getElementDoubleAbs(itb.index + k);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else if (b.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double db = b.getElementDoubleAbs(itb.index);
									for (int k = 0; k < ISIZEA; k++) {
										answer[k] = a.getElementDoubleAbs(ita.index + k) + db;
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else {
								while (ita.hasNext() && itb.hasNext()) {
									for (int k = 0; k < is; k++) {
										answer[k] = a.getElementDoubleAbs(ita.index + k)
												+ b.getElementDoubleAbs(itb.index + k);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							}
						}
					}
					if (d == null)
						break;
					start += System.nanoTime();
					double otime = ((double) start) / d.getSize();

					TestUtils.verbosePrintf("Time taken by add for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.
								* (otime - ntime) / otime);
					checkDatasets(a, b, c, d);

					n *= SSTEP;
				}
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("constant to %s, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.add(a, dv);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: %s\n\n", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).add(zv));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue() + dv);
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = a.getElementDoubleAbs(ita.index + k) + dv;
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();

				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by add for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(a, dv, c, d);

				n *= SSTEP;
			}
		}

		if (eCount > 0) {
			TestUtils.verbosePrintf("Number of exceptions caught: %d\n", eCount);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSubtraction() {
		Dataset a, b, c = null, d = null;
		Complex zv = new Complex(-3.5, 0);
		final double dv = zv.getReal();
		long start;
		int n;
		int eCount = 0;

		for (String dn : classes.keySet()) {
			final Class<? extends Dataset> dClass = classes.get(dn);
			Random.seed(12735L);
			for (String en : classes.keySet()) {
				final Class<? extends Dataset> eClass = classes.get(en);

				TestUtils.verbosePrintf("%s to %s, ", dn, en);

				n = 32;
				for (int i = 0; i < SITER; i++) {
					if (!InterfaceUtils.isCompound(dClass)) {
						a = Random.randn(n).imultiply(100);
						a = a.cast(dClass);
					} else {
						Dataset[] aa = new Dataset[ISIZEA];
						for (int j = 0; j < ISIZEA; j++) {
							aa[j] = Random.randn(n).imultiply(100);
						}
						a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
					}
					if (!InterfaceUtils.isCompound(eClass)) {
						b = Random.randn(n).imultiply(100);
						b = b.cast(eClass);
					} else {
						Dataset[] ab = new Dataset[ISIZEB];
						for (int j = 0; j < ISIZEB; j++) {
							ab[j] = Random.randn(n).imultiply(100);
						}
						b = DatasetUtils.cast((Class<CompoundDataset>) eClass, ab);
					}

					start = -System.nanoTime();
					try {
						c = Maths.subtract(a, b);
					} catch (IllegalArgumentException e) {
						TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
						eCount++;
						continue;
					}
					start += System.nanoTime();
					double ntime = ((double) start) / c.getSize();

					d = DatasetFactory.zeros(c);
					start = -System.nanoTime();
					IndexIterator ita = a.getIterator();
					IndexIterator itb = b.getIterator();
					int j = 0;
					if (InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).subtract((Complex) b
											.getObjectAbs(itb.index)));
							j += is;
						}
					} else if (InterfaceUtils.isComplex(dClass) && !InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).subtract(new Complex(b.getElementDoubleAbs(itb.index), 0)));
							j += is;
						}
					} else if (!InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, new Complex(a.getElementDoubleAbs(ita.index), 0).subtract((Complex) b.getObjectAbs(itb.index)));
							j += is;
						}
					} else {
						if (!InterfaceUtils.isCompound(dClass) && !InterfaceUtils.isCompound(eClass)) {
							while (ita.hasNext() && itb.hasNext()) {
								d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue()
												- ((Number) b.getObjectAbs(itb.index)).doubleValue());
							}
						} else {
							final double[] answer = new double[MAXISIZE];
							final int is = d.getElementsPerItem();

							if (a.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double da = a.getElementDoubleAbs(ita.index);
									for (int k = 0; k < ISIZEB; k++) {
										answer[k] = da - b.getElementDoubleAbs(itb.index + k);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else if (b.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double db = b.getElementDoubleAbs(itb.index);
									for (int k = 0; k < ISIZEA; k++) {
										answer[k] = a.getElementDoubleAbs(ita.index + k) - db;
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else {
								while (ita.hasNext() && itb.hasNext()) {
									for (int k = 0; k < is; k++) {
										answer[k] = a.getElementDoubleAbs(ita.index + k)
												- b.getElementDoubleAbs(itb.index + k);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							}
						}
					}
					if (d == null)
						break;
					start += System.nanoTime();

					double otime = ((double) start) / d.getSize();
					TestUtils.verbosePrintf("Time taken by sub for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.
							* (otime - ntime) / otime);
					checkDatasets(a, b, c, d);

					n *= SSTEP;
				}
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("constant from %s, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.subtract(a, dv);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).subtract(zv));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue() - dv);
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = a.getElementDoubleAbs(ita.index + k) - dv;
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by add for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(a, dv, c, d);

				n *= SSTEP;
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("%s from constant, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.subtract(dv, a);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, zv.subtract((Complex) a.getObjectAbs(ita.index)));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, dv - ((Number) a.getObjectAbs(ita.index)).doubleValue());
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = dv - a.getElementDoubleAbs(ita.index + k);
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by sub for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(dv, a, c, d);

				n *= SSTEP;
			}
		}

		if (eCount > 0) {
			TestUtils.verbosePrintf("Number of exceptions caught: %d\n", eCount);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMultiplication() {
		Dataset a, b, c = null, d = null;
		Complex zv = new Complex(-3.5, 0);
		final double dv = zv.getReal();
		long start;
		int n;
		int eCount = 0;

		for (String dn : classes.keySet()) {
			final Class<? extends Dataset> dClass = classes.get(dn);
			Random.seed(12735L);

			for (String en : classes.keySet()) {
				final Class<? extends Dataset> eClass = classes.get(en);

				TestUtils.verbosePrintf("%s by %s, ", dn, en);
				n = 32;
				for (int i = 0; i < SITER; i++) {
					if (!InterfaceUtils.isCompound(dClass)) {
						a = Random.randn(n).imultiply(100);
						a = a.cast(dClass);
					} else {
						Dataset[] aa = new Dataset[ISIZEA];
						for (int j = 0; j < ISIZEA; j++) {
							aa[j] = Random.randn(n).imultiply(100);
						}
						a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
					}
					if (!InterfaceUtils.isCompound(eClass)) {
						b = Random.randn(n).imultiply(100);
						b = b.cast(eClass);
					} else {
						Dataset[] ab = new Dataset[ISIZEB];
						for (int j = 0; j < ISIZEB; j++) {
							ab[j] = Random.randn(n).imultiply(100);
						}
						b = DatasetUtils.cast((Class<CompoundDataset>) eClass, ab);
					}

					start = -System.nanoTime();
					try {
						c = Maths.multiply(a, b);
					} catch (IllegalArgumentException e) {
						TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
						eCount++;
						continue;
					}
					start += System.nanoTime();
					double ntime = ((double) start) / c.getSize();

					d = DatasetFactory.zeros(c);
					start = -System.nanoTime();
					IndexIterator ita = a.getIterator();
					IndexIterator itb = b.getIterator();
					int j = 0;
					if (InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).multiply((Complex) b
											.getObjectAbs(itb.index)));
							j += is;
						}
					} else if (InterfaceUtils.isComplex(dClass) && !InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).multiply(b.getElementDoubleAbs(itb.index)));
							j += is;
						}
					} else if (!InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, new Complex(a.getElementDoubleAbs(ita.index), 0).multiply((Complex) b.getObjectAbs(itb.index)));
							j += is;
						}
					} else {
						if (!InterfaceUtils.isCompound(dClass) && !InterfaceUtils.isCompound(eClass)) {
							while (ita.hasNext() && itb.hasNext()) {
								d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue()
												* ((Number) b.getObjectAbs(itb.index)).doubleValue());
							}
						} else {
							final double[] answer = new double[MAXISIZE];
							final int is = d.getElementsPerItem();

							if (a.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double da = a.getElementDoubleAbs(ita.index);
									for (int k = 0; k < ISIZEB; k++) {
										answer[k] = da * b.getElementDoubleAbs(itb.index + k);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else if (b.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double db = b.getElementDoubleAbs(itb.index);
									for (int k = 0; k < ISIZEA; k++) {
										answer[k] = a.getElementDoubleAbs(ita.index + k) * db;
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else {
								while (ita.hasNext() && itb.hasNext()) {
									for (int k = 0; k < is; k++) {
										answer[k] = a.getElementDoubleAbs(ita.index + k)
												* b.getElementDoubleAbs(itb.index + k);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							}
						}
					}
					if (d == null)
						break;
					start += System.nanoTime();
					double otime = ((double) start) / d.getSize();

					TestUtils.verbosePrintf("Time taken by mul for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.
								* (otime - ntime) / otime);

					checkDatasets(a, b, c, d);

					n *= SSTEP;
				}
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("constant with %s, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.multiply(a, dv);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).multiply(zv));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue() * dv);
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = a.getElementDoubleAbs(ita.index + k) * dv;
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by mul for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(a, dv, c, d);

				n *= SSTEP;
			}
		}

		if (eCount > 0) {
			TestUtils.verbosePrintf("Number of exceptions caught: %d\n", eCount);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDivision() {
		Dataset a, b, c = null, d = null;
		Complex zv = new Complex(-3.5, 0);
		final double dv = zv.getReal();
		long start;
		int n;
		int eCount = 0;

		for (String dn : classes.keySet()) {
			final Class<? extends Dataset> dClass = classes.get(dn);
			Random.seed(12735L);

			for (String en : classes.keySet()) {
				final Class<? extends Dataset> eClass = classes.get(en);

				TestUtils.verbosePrintf("%s by %s, ", dn, en);
				n = 32;
				for (int i = 0; i < SITER; i++) {
					if (!InterfaceUtils.isCompound(dClass)) {
						a = Random.randn(n).imultiply(100);
						a = a.cast(dClass);
					} else {
						Dataset[] aa = new Dataset[ISIZEA];
						for (int j = 0; j < ISIZEA; j++) {
							aa[j] = Random.randn(n).imultiply(100);
						}
						a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
					}
					if (!InterfaceUtils.isCompound(eClass)) {
						b = Random.randn(n).imultiply(100);
						b = b.cast(eClass);
					} else {
						Dataset[] ab = new Dataset[ISIZEB];
						for (int j = 0; j < ISIZEB; j++) {
							ab[j] = Random.randn(n).imultiply(100);
						}
						b = DatasetUtils.cast((Class<CompoundDataset>) eClass, ab);
					}

					start = -System.nanoTime();
					try {
						c = Maths.divide(a, b);
					} catch (IllegalArgumentException e) {
						TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
						eCount++;
						continue;
					}
					start += System.nanoTime();
					double ntime = ((double) start) / c.getSize();

					d = DatasetFactory.zeros(c);
					start = -System.nanoTime();
					IndexIterator ita = a.getIterator();
					IndexIterator itb = b.getIterator();
					int j = 0;
					if (InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).divide((Complex) b
											.getObjectAbs(itb.index)));
							j += is;
						}
					} else if (InterfaceUtils.isComplex(dClass) && !InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							Complex z = (Complex) a.getObjectAbs(ita.index);
							double br = b.getElementDoubleAbs(itb.index);
							Complex zr = z.divide(br);
							if (br == 0) { // CM's implementation is different to NumPy's
								zr = new Complex(z.getReal() != 0 ? z.getReal() / br : zr.getReal(),
										z.getImaginary() != 0 ? z.getImaginary() / br : zr.getImaginary());
							}
							d.setObjectAbs(j, zr);
							j += is;
						}
					} else if (!InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, new Complex(a.getElementDoubleAbs(ita.index), 0).divide((Complex) b.getObjectAbs(itb.index)));
							j += is;
						}
					} else {
						if (!InterfaceUtils.isCompound(dClass) && !InterfaceUtils.isCompound(eClass)) {
							if (d.hasFloatingPointElements()) {
								while (ita.hasNext() && itb.hasNext()) {
									d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue()
													/ ((Number) b.getObjectAbs(itb.index)).doubleValue());
								}
							} else {
								while (ita.hasNext() && itb.hasNext()) {
									double bv = ((Number) b.getObjectAbs(itb.index)).doubleValue();
									d.setObjectAbs(j++, bv == 0 ? 0 :((Number) a.getObjectAbs(ita.index)).doubleValue()
													/ bv);
								}
							}
						} else {
							final double[] answer = new double[MAXISIZE];
							final int is = d.getElementsPerItem();

							if (a.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double xa = a.getElementDoubleAbs(ita.index);
									if (d.hasFloatingPointElements()) {
										for (int k = 0; k < ISIZEB; k++) {
											answer[k] = xa / b.getElementDoubleAbs(itb.index + k);
										}
									} else {
										for (int k = 0; k < ISIZEB; k++) {
											final double v = xa / b.getElementDoubleAbs(itb.index + k);
											answer[k] = Double.isInfinite(v) || Double.isNaN(v) ? 0 : v;
										}
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else if (b.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double xb = b.getElementDoubleAbs(itb.index);
									if (d.hasFloatingPointElements()) {
										for (int k = 0; k < ISIZEA; k++) {
											answer[k] = a.getElementDoubleAbs(ita.index + k) / xb;
										}
									} else {
										if (xb == 0) {
											for (int k = 0; k < ISIZEA; k++) {
												answer[k] = 0;
											}
										} else {
											for (int k = 0; k < ISIZEA; k++) {
												answer[k] = a.getElementDoubleAbs(ita.index + k) / xb;
											}
										}
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else {
								while (ita.hasNext() && itb.hasNext()) {
									if (d.hasFloatingPointElements()) {
										double v;
										for (int k = 0; k < is; k++) {
											v = a.getElementDoubleAbs(ita.index + k)
													/ b.getElementDoubleAbs(itb.index + k);
											answer[k] = Double.isInfinite(v) || Double.isNaN(v) ? 0 : v;
										}
									} else {
										double v;
										for (int k = 0; k < is; k++) {
											v = a.getElementDoubleAbs(ita.index + k)
													/ b.getElementDoubleAbs(itb.index + k);
											answer[k] = Double.isInfinite(v) || Double.isNaN(v) ? 0 : v;
										}
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							}
						}
					}
					start += System.nanoTime();
					double otime = ((double) start) / d.getSize();

					TestUtils.verbosePrintf("Time taken by div for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.
								* (otime - ntime) / otime);
					checkDatasets(a, b, c, d);

					n *= SSTEP;
				}
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("%s by constant, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.divide(a, dv);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).divide(zv));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue() / dv);
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = a.getElementDoubleAbs(ita.index + k) / dv;
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by div for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(a, dv, c, d);

				n *= SSTEP;
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("constant by %s, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.divide(dv, a);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: %s\n", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, zv.divide((Complex) a.getObjectAbs(ita.index)));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, dv / ((Number) a.getObjectAbs(ita.index)).doubleValue());
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = dv / a.getElementDoubleAbs(ita.index + k);
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by div for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(dv, a, c, d);

				n *= SSTEP;
			}
		}

		if (eCount > 0) {
			TestUtils.verbosePrintf("Number of exceptions caught: %d\n", eCount);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRemainder() {
		Dataset a, b, c = null, d = null;
		Complex zv = new Complex(-3.5, 0);
		final double dv = zv.getReal();
		long start;
		int n;
		int eCount = 0;

		for (String dn : classes.keySet()) {
			final Class<? extends Dataset> dClass = classes.get(dn);
			Random.seed(12735L);

			for (String en : classes.keySet()) {
				final Class<? extends Dataset> eClass = classes.get(en);

				TestUtils.verbosePrintf("%s by %s, ", dn, en);

				n = 32;
				for (int i = 0; i < SITER; i++) {
					if (!InterfaceUtils.isCompound(dClass)) {
						a = Random.randn(n).imultiply(100);
						a = a.cast(dClass);
					} else {
						Dataset[] aa = new Dataset[ISIZEA];
						for (int j = 0; j < ISIZEA; j++) {
							aa[j] = Random.randn(n).imultiply(100);
						}
						a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
					}
					if (!InterfaceUtils.isCompound(eClass)) {
						b = Random.randn(n).imultiply(100);
						b = b.cast(eClass);
					} else {
						Dataset[] ab = new Dataset[ISIZEB];
						for (int j = 0; j < ISIZEB; j++) {
							ab[j] = Random.randn(n).imultiply(100);
						}
						b = DatasetUtils.cast((Class<CompoundDataset>) eClass, ab);
					}

					start = -System.nanoTime();
					try {
						c = Maths.remainder(a, b);
					} catch (IllegalArgumentException e) {
						TestUtils.verbosePrintf("Could not perform this operation: ", e.getMessage());
						eCount++;
						continue;
					} catch (UnsupportedOperationException ue) {
						TestUtils.verbosePrintf("Could not perform this operation: ", ue.getMessage());
						continue;
					}
					start += System.nanoTime();
					double ntime = ((double) start) / c.getSize();

					d = DatasetFactory.zeros(c);
					start = -System.nanoTime();
					IndexIterator ita = a.getIterator();
					IndexIterator itb = b.getIterator();
					int j = 0;
					if (!InterfaceUtils.isCompound(dClass) && !InterfaceUtils.isCompound(eClass)) {
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue()
											% ((Number) b.getObjectAbs(itb.index)).doubleValue());
						}
					} else {
						final double[] answer = new double[MAXISIZE];
						final int is = d.getElementsPerItem();

						if (a.getElementsPerItem() < is) {
							while (ita.hasNext() && itb.hasNext()) {
								final double xa = a.getElementDoubleAbs(ita.index);
								for (int k = 0; k < ISIZEB; k++) {
									answer[k] = xa % b.getElementDoubleAbs(itb.index + k);
								}
								d.setObjectAbs(j, answer);
								j += is;
							}
						} else if (b.getElementsPerItem() < is) {
							while (ita.hasNext() && itb.hasNext()) {
								final double xb = b.getElementDoubleAbs(itb.index);
								for (int k = 0; k < ISIZEA; k++) {
									answer[k] = a.getElementDoubleAbs(ita.index + k) % xb;
								}
								d.setObjectAbs(j, answer);
								j += is;
							}
						} else {
							while (ita.hasNext() && itb.hasNext()) {
								for (int k = 0; k < is; k++) {
									answer[k] = a.getElementDoubleAbs(ita.index + k) % b.getElementDoubleAbs(itb.index + k);
								}
								d.setObjectAbs(j, answer);
								j += is;
							}
						}
					}
					if (d == null)
						break;
					start += System.nanoTime();
					double otime = ((double) start) / d.getSize();

					TestUtils.verbosePrintf("Time taken by rem for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.
								* (otime - ntime) / otime);
					checkDatasets(a, b, c, d);

					n *= SSTEP;
				}
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("%s by constant, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.remainder(a, dv);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: ", e.getMessage());
					eCount++;
					continue;
				} catch (UnsupportedOperationException ue) {
					TestUtils.verbosePrintf("Could not perform this operation: ", ue.getMessage());
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (!InterfaceUtils.isCompound(dClass)) {
					while (ita.hasNext()) {
						d.setObjectAbs(j++, ((Number) a.getObjectAbs(ita.index)).doubleValue() % dv);
					}
				} else {
					final double[] answer = new double[ISIZEA];
					while (ita.hasNext()) {
						for (int k = 0; k < ISIZEA; k++) {
							answer[k] = a.getElementDoubleAbs(ita.index + k) % dv;
						}
						d.setObjectAbs(j, answer);
						j += ISIZEA;
					}
				}
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by rem for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(a, dv, c, d);

				n *= SSTEP;
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("constant by %s, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.remainder(dv, a);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: ", e.getMessage());
					eCount++;
					continue;
				} catch (UnsupportedOperationException ue) {
					TestUtils.verbosePrintf("Could not perform this operation: ", ue.getMessage());
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (!InterfaceUtils.isCompound(dClass)) {
					while (ita.hasNext()) {
						d.setObjectAbs(j++, dv % ((Number) a.getObjectAbs(ita.index)).doubleValue());
					}
				} else {
					final double[] answer = new double[ISIZEA];
					while (ita.hasNext()) {
						for (int k = 0; k < ISIZEA; k++) {
							answer[k] = dv % a.getElementDoubleAbs(ita.index + k);
						}
						d.setObjectAbs(j, answer);
						j += ISIZEA;
					}
				}
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by rem for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(dv, a, c, d);

				n *= SSTEP;
			}
		}

		if (eCount > 0) {
			TestUtils.verbosePrintf("Number of exceptions caught: %d\n", eCount);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPower() {
		Dataset a, b, c = null, d = null;
		Complex zv = new Complex(-3.5, 0);
		final double dv = zv.getReal();
		long start;
		int n;
		int eCount = 0;

		for (String dn : classes.keySet()) {
			final Class<? extends Dataset> dClass = classes.get(dn);
			Random.seed(12735L);

			for (String en : classes.keySet()) {
				final Class<? extends Dataset> eClass = classes.get(en);

				TestUtils.verbosePrintf("%s by %s, ", dn, en);

				n = 32;
				for (int i = 0; i < SITER; i++) {
					if (!InterfaceUtils.isCompound(dClass)) {
						a = Random.randn(n).imultiply(100);
						a = a.cast(dClass);
					} else {
						Dataset[] aa = new Dataset[ISIZEA];
						for (int j = 0; j < ISIZEA; j++) {
							aa[j] = Random.randn(n).imultiply(100);
						}
						a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
					}
					if (!InterfaceUtils.isCompound(eClass)) {
						b = Random.randn(n).imultiply(100);
						b = b.cast(eClass);
					} else {
						Dataset[] ab = new Dataset[ISIZEB];
						for (int j = 0; j < ISIZEB; j++) {
							ab[j] = Random.randn(n).imultiply(100);
						}
						b = DatasetUtils.cast((Class<CompoundDataset>) eClass, ab);
					}

					start = -System.nanoTime();
					try {
						c = Maths.power(a, b);
					} catch (IllegalArgumentException e) {
						TestUtils.verbosePrintf("Could not perform this operation: ", e.getMessage());
						eCount++;
						continue;
					}
					start += System.nanoTime();
					double ntime = ((double) start) / c.getSize();

					d = DatasetFactory.zeros(c);
					start = -System.nanoTime();
					IndexIterator ita = a.getIterator();
					IndexIterator itb = b.getIterator();
					int j = 0;
					if (InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).pow((Complex) b
											.getObjectAbs(itb.index)));
							j += is;
						}
					} else if (InterfaceUtils.isComplex(dClass) && !InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).pow(new Complex(b.getElementDoubleAbs(itb.index), 0)));
							j += is;
						}
					} else if (!InterfaceUtils.isComplex(dClass) && InterfaceUtils.isComplex(eClass)) {
						final int is = d.getElementsPerItem();
						while (ita.hasNext() && itb.hasNext()) {
							d.setObjectAbs(j, new Complex(a.getElementDoubleAbs(ita.index), 0).pow((Complex) b.getObjectAbs(itb.index)));
							j += is;
						}
					} else {
						if (!InterfaceUtils.isCompound(dClass) && !InterfaceUtils.isCompound(eClass)) {
							while (ita.hasNext() && itb.hasNext()) {
								d.setObjectAbs(j++, Math.pow(a.getElementDoubleAbs(ita.index),
												b.getElementDoubleAbs(itb.index)));
							}
						} else {
							final double[] answer = new double[MAXISIZE];
							final int is = d.getElementsPerItem();

							if (a.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double xa = a.getElementDoubleAbs(ita.index);
									for (int k = 0; k < ISIZEB; k++) {
										answer[k] = Math.pow(xa, b.getElementDoubleAbs(itb.index + k));
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else if (b.getElementsPerItem() < is) {
								while (ita.hasNext() && itb.hasNext()) {
									final double xb = b.getElementDoubleAbs(itb.index);
									for (int k = 0; k < ISIZEA; k++) {
										answer[k] = Math.pow(a.getElementDoubleAbs(ita.index + k), xb);
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							} else {
								while (ita.hasNext() && itb.hasNext()) {
									for (int k = 0; k < is; k++) {
										answer[k] = Math.pow(a.getElementDoubleAbs(ita.index + k),
												b.getElementDoubleAbs(itb.index + k));
									}
									d.setObjectAbs(j, answer);
									j += is;
								}
							}
						}
					}
					if (d == null)
						break;
					start += System.nanoTime();
					double otime = ((double) start) / d.getSize();

					TestUtils.verbosePrintf("Time taken by pow for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.
								* (otime - ntime) / otime);
					checkDatasets(a, b, c, d);

					n *= SSTEP;
				}
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("%s by constant, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.power(a, dv);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: ", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, ((Complex) a.getObjectAbs(ita.index)).pow(zv));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, Math.pow(((Number) a.getObjectAbs(ita.index)).doubleValue(), dv));
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = Math.pow(a.getElementDoubleAbs(ita.index + k), dv);
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by pow for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(a, dv, c, d);

				n *= SSTEP;
			}

			Random.seed(12735L);
			n = 32;
			TestUtils.verbosePrintf("constant by %s, ", dn);
			for (int i = 0; i < SITER; i++) {
				if (!InterfaceUtils.isCompound(dClass)) {
					a = Random.randn(n);
					a.imultiply(100);
					a = a.cast(dClass);
				} else {
					Dataset[] aa = new Dataset[ISIZEA];
					for (int j = 0; j < ISIZEA; j++) {
						aa[j] = Random.randn(n).imultiply(100);
					}
					a = DatasetUtils.cast((Class<CompoundDataset>) dClass, aa);
				}

				start = -System.nanoTime();
				try {
					c = Maths.power(dv, a);
				} catch (IllegalArgumentException e) {
					TestUtils.verbosePrintf("Could not perform this operation: ", e.getMessage());
					eCount++;
					continue;
				}
				start += System.nanoTime();
				double ntime = ((double) start)/c.getSize();

				d = DatasetFactory.zeros(c);
				start = -System.nanoTime();
				IndexIterator ita = a.getIterator();
				int j = 0;
				if (InterfaceUtils.isComplex(dClass)) {
					final int is = d.getElementsPerItem();
					while (ita.hasNext()) {
						d.setObjectAbs(j, zv.pow((Complex) a.getObjectAbs(ita.index)));
						j += is;
					}
				} else {
					if (!InterfaceUtils.isCompound(dClass)) {
						while (ita.hasNext()) {
							d.setObjectAbs(j++, Math.pow(dv, ((Number) a.getObjectAbs(ita.index)).doubleValue()));
						}
					} else {
						final double[] answer = new double[ISIZEA];
						while (ita.hasNext()) {
							for (int k = 0; k < ISIZEA; k++) {
								answer[k] = Math.pow(dv, a.getElementDoubleAbs(ita.index + k));
							}
							d.setObjectAbs(j, answer);
							j += ISIZEA;
						}
					}
				}
				if (d == null)
					break;
				start += System.nanoTime();
				double otime = ((double) start)/d.getSize();

				TestUtils.verbosePrintf("Time taken by pow for %s: %s; %s (%.1f%%)\n", n, otime, ntime, 100.*(otime - ntime)/otime);
				checkDatasets(dv, a, c, d);

				n *= SSTEP;
			}
		}

		if (eCount > 0) {
			TestUtils.verbosePrintf("Number of exceptions caught: %d\n", eCount);
		}
	}

	@Test
	public void testDifference() {
		int[] data = {0,1,3,9,5,10};

		Dataset a = DatasetFactory.createFromObject(data);
		Dataset d = Maths.difference(a, 1, -1);
		int[] tdata;
		tdata = new int[] {1,  2,  6, -4,  5};
		Dataset ta = DatasetFactory.createFromObject(tdata);
		checkDatasets(null, null, d, ta);

		Slice[] slices = new Slice[] {new Slice(3)};
		d = Maths.difference(a.getSliceView(slices), 1, -1);
		ta = Maths.difference(a.getSlice(slices), 1, -1);
		checkDatasets(null, null, d, ta);
		slices = new Slice[] {new Slice(-2, null, -1)};
		d = Maths.difference(a.getSliceView(slices), 1, -1);
		ta = Maths.difference(a.getSlice(slices), 1, -1);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(ComplexDoubleDataset.class, new double[] {0, 1, 2, 3, 4, 5});
		d = Maths.difference(a, 1, -1);
		ta = DatasetFactory.createFromObject(ComplexDoubleDataset.class, new double[] {2, 2, 2, 2});
		checkDatasets(null, null, d, ta);

		d = Maths.difference(a.getSliceView(slices), 1, -1);
		ta = Maths.difference(a.getSlice(slices), 1, -1);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(2, CompoundDoubleDataset.class, new double[] {0, 1, 2, 3, 4, 5});
		d = Maths.difference(a, 1, -1);
		ta = DatasetFactory.createFromObject(2, CompoundDoubleDataset.class, new double[] {2, 2, 2, 2});
		checkDatasets(null, null, d, ta);
		d = Maths.difference(a.getSliceView(slices), 1, -1);
		ta = Maths.difference(a.getSlice(slices), 1, -1);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(new byte[] {0, 1, 2, 4, 7, 11});
		d = Maths.difference(a, 2, -1);
		ta = DatasetFactory.createFromObject(new byte[] {0, 1, 1, 1});
		checkDatasets(null, null, d, ta);
		d = Maths.difference(a.getSliceView(slices), 2, -1);
		ta = Maths.difference(a.getSlice(slices), 2, -1);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(2, CompoundShortDataset.class, new short[] {0, 1, 2, 3, 4, 5, 7, 6});
		d = Maths.difference(a, 2, -1);
		ta = DatasetFactory.createFromObject(2, CompoundShortDataset.class, new short[] {0, 0, 1, -1});
		checkDatasets(null, null, d, ta);
		d = Maths.difference(a.getSliceView(slices), 2, -1);
		ta = Maths.difference(a.getSlice(slices), 2, -1);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(2, CompoundDoubleDataset.class, new double[] {0, 1, 2, 3, 4, 5, 7, 6});
		d = Maths.difference(a, 2, -1);
		ta = DatasetFactory.createFromObject(2, CompoundDoubleDataset.class, new double[] {0, 0, 1, -1});
		checkDatasets(null, null, d, ta);
		d = Maths.difference(a.getSliceView(slices), 2, -1);
		ta = Maths.difference(a.getSlice(slices), 2, -1);
		checkDatasets(null, null, d, ta);
	}

	@Test
	public void testGradient() {
		double[] data = {1, 2, 4, 7, 11, 16};
		double[] tdata;

		Dataset a = DatasetFactory.createFromObject(data);
		Dataset d = Maths.gradient(a).get(0);
		tdata = new double[] {1., 1.5, 2.5, 3.5, 4.5, 5.};
		Dataset ta = DatasetFactory.createFromObject(tdata);
		checkDatasets(null, null, d, ta);
		Slice[] slices = new Slice[] {new Slice(3)};
		d = Maths.gradient(a.getSliceView(slices)).get(0);
		ta = Maths.gradient(a.getSlice(slices)).get(0);
		checkDatasets(null, null, d, ta);


		Dataset b = DatasetFactory.createRange(a.getClass(), a.getShapeRef()[0]);
		b.imultiply(2);
		tdata = new double[] {0.5 , 0.75, 1.25, 1.75, 2.25, 2.5};
		ta = DatasetFactory.createFromObject(tdata);
		d = Maths.gradient(a, b).get(0);
		checkDatasets(null, null, d, ta);
		d = Maths.gradient(a.getSliceView(slices), b.getSliceView(slices)).get(0);
		ta = Maths.gradient(a.getSlice(slices), b.getSlice(slices)).get(0);
		checkDatasets(null, null, d, ta);

		data = new double[] {1, 2, 6, 3, 4, 5};
		a = DatasetFactory.createFromObject(data, 2, 3);
		List<? extends Dataset> l = Maths.gradient(a);
		tdata = new double[] { 2., 2., -1., 2., 2., -1.};
		ta = DatasetFactory.createFromObject(tdata, 2, 3);
		checkDatasets(null, null, l.get(0), ta);
		tdata = new double[] { 1., 2.5, 4., 1., 1., 1.};
		ta = DatasetFactory.createFromObject(tdata, 2, 3);
		checkDatasets(null, null, l.get(1), ta);

		b = DatasetFactory.createRange(a.getClass(), a.getShapeRef()[0]);
		b.imultiply(2);
		Dataset c = DatasetFactory.createRange(a.getClass(), a.getShapeRef()[1]);
		c.imultiply(-1.5);

		l = Maths.gradient(a, b, c);
		tdata = new double[] { 2., 2., -1., 2., 2., -1.};
		ta = DatasetFactory.createFromObject(tdata, 2, 3);
		ta.idivide(2);
		checkDatasets(null, null, l.get(0), ta);
		tdata = new double[] { 1., 2.5, 4., 1., 1., 1.};
		ta = DatasetFactory.createFromObject(tdata, 2, 3);
		ta.idivide(-1.5);
		checkDatasets(null, null, l.get(1), ta);

		a = DatasetFactory.createFromObject(new byte[] {0, 1, 2, 4, 7, 11});
		d = Maths.gradient(a).get(0);
		ta = DatasetFactory.createFromObject(new byte[] {1, 1, 1, 2, 3, 4});
		checkDatasets(null, null, d, ta);

		slices = new Slice[] {new Slice(-2, null, -1)};
		d = Maths.gradient(a.getSliceView(slices)).get(0);
		ta = Maths.gradient(a.getSlice(slices)).get(0);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(ComplexDoubleDataset.class, new double[] {0, 1, 2, 3, 4, 5});
		d = Maths.gradient(a).get(0);
		ta = DatasetFactory.createFromObject(ComplexDoubleDataset.class, new double[] {2, 2, 2, 2, 2, 2});
		checkDatasets(null, null, d, ta);
		d = Maths.gradient(a.getSliceView(slices)).get(0);
		ta = Maths.gradient(a.getSlice(slices)).get(0);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(2, CompoundShortDataset.class, new short[] {0, 1, 2, 3, 4, 5, 7, 6});
		d = Maths.gradient(a).get(0);
		ta = DatasetFactory.createFromObject(2, CompoundShortDataset.class, new short[] {2, 2, 2, 2, 2, 1, 3, 1});
		checkDatasets(null, null, d, ta);
		d = Maths.gradient(a.getSliceView(slices)).get(0);
		ta = Maths.gradient(a.getSlice(slices)).get(0);
		checkDatasets(null, null, d, ta);

		a = DatasetFactory.createFromObject(2, CompoundDoubleDataset.class, new double[] {0, 1, 2, 3, 4, 5, 7, 6});
		d = Maths.gradient(a).get(0);
		ta = DatasetFactory.createFromObject(2, CompoundDoubleDataset.class, new double[] {2, 2, 2, 2, 2.5, 1.5, 3, 1});
		checkDatasets(null, null, d, ta);
		d = Maths.gradient(a.getSliceView(slices)).get(0);
		ta = Maths.gradient(a.getSlice(slices)).get(0);
		checkDatasets(null, null, d, ta);
	}

	/**
	 * Test rounding
	 */
	@Test
	public void testRounding() {
		DoubleDataset t;
		DoubleDataset x;
		double tol = 1e-6;

		double[] val = { -1.7, -1.5, -1.2, 0.3, 1.4, 1.5, 1.6 };
		t = DatasetFactory.createFromObject(DoubleDataset.class, val);

		double[] resFloor = { -2, -2, -2, 0, 1, 1, 1 };
		x = (DoubleDataset) Maths.floor(t);
		for (int i = 0, imax = t.getSize(); i < imax; i++) {
			assertEquals(resFloor[i], x.get(i), tol);
		}

		double[] resCeil = { -1, -1, -1, 1, 2, 2, 2 };
		x = (DoubleDataset) Maths.ceil(t);
		for (int i = 0, imax = t.getSize(); i < imax; i++) {
			assertEquals(resCeil[i], x.get(i), tol);
		}

		double[] resRint= { -2, -2, -1, 0, 1, 2, 2 };
		x = (DoubleDataset) Maths.rint(t);
		for (int i = 0, imax = t.getSize(); i < imax; i++) {
			assertEquals(resRint[i], x.get(i), tol);
		}
	}

	private void checkInterpolate(Dataset a, double x) {
		int s = a.getShapeRef()[0];
//		double v = Maths.interpolate(a, x);
		double v = Maths.interpolate(a, new double[] {x});
		if (x <= -1 || x >= s) {
			Assert.assertEquals(0, v, 1e-15);
			return;
		}

		int i = (int) Math.floor(x);
		double f1 = 0;
		double f2 = 0;
		double t = x - i;
		if (x < 0) {
			f2 = a.getDouble(0);
		} else if (x >= s - 1) {
			f1 = a.getDouble(i);
		} else {
			f1 = a.getDouble(i);
			f2 = a.getDouble(i + 1);
		}
		Assert.assertEquals((1 - t) * f1 + t * f2, v, 1e-15);
	}

	private void checkInterpolate2(Dataset a, double x) {
		int s = a.getShapeRef()[0];
		Dataset xa = DatasetFactory.createRange(s);
		double a1 = checkInterpolate2(xa, a, x, false);
		xa = DatasetFactory.createRange(s-1., -1, -1);
		double a2 = checkInterpolate2(xa.getSliceView(new Slice(null, null, -1)), a, x, false);
		TestUtils.assertEquals("Flipped x - but reflipped", a1, a2);
		a2 = checkInterpolate2(xa, a.getSliceView(new Slice(null, null, -1)), x, true);
		TestUtils.assertEquals("Flipped x and flipped a", a1, a2);

		try {
			Random.seed(1231);
			xa = Random.randint(0, s, a.getShapeRef());
			checkInterpolate2(xa, a, x, false);
			Assert.fail("No exception raised");
		} catch (IllegalArgumentException e) {

		} catch (Exception e) {
			Assert.fail("Wrong exception raised");
		}
	}

	private double checkInterpolate2(Dataset xa, Dataset a, double x, boolean isXaReversed) {
		int s = a.getShapeRef()[0];
		Dataset dv = Maths.interpolate(xa, a, DatasetFactory.createFromObject(x), null, null);
		double v = dv.getElementDoubleAbs(0);
		if (x <= -1 || x >= s) {
			Assert.assertEquals(0, v, 1e-15);
			return v;
		}

		int i = (int) Math.floor(x);
		double f1 = 0;
		double f2 = 0;
		double t = x - i;
		if (x < 0) {
			f2 = a.getDouble(isXaReversed ? s - 1 : 0);
		} else if (x >= s - 1) {
			f1 = a.getDouble(isXaReversed ? 0 : s - 1);
		} else {
			if (isXaReversed) {
				i = s - 1 - i;
				f1 = a.getDouble(i);
				f2 = a.getDouble(i - 1);
			} else {
				f1 = a.getDouble(i);
				f2 = a.getDouble(i + 1);
			}
		}
		Assert.assertEquals((1 - t) * f1 + t * f2, v, 1e-15);
		return v;
	}

	private void checkInterpolate3(Dataset a, double x) {
		int s = a.getShapeRef()[0];
		Dataset dv = Maths.interpolate(DatasetFactory.createRange(IntegerDataset.class, s), a, DatasetFactory.createFromObject(x), 0, 0);
		double v = dv.getElementDoubleAbs(0);
		if (x <= -1 || x >= s) {
			Assert.assertEquals(0, v, 1e-15);
			return;
		}

		int i = (int) Math.floor(x);
		double f1 = 0;
		double f2 = 0;
		double t = x - i;
		if (x < 0 || x > s - 1) {
		} else if (x == s - 1) {
			f1 = a.getDouble(i);
		} else {
			f1 = a.getDouble(i);
			f2 = a.getDouble(i + 1);
		}
		Assert.assertEquals((1 - t) * f1 + t * f2, v, 1e-15);
	}

	private void checkInterpolateArray(CompoundDataset a, double x) {
		int s = a.getShapeRef()[0];
		int is = a.getElementsPerItem();
		double[] v = new double[is];
		Maths.interpolate(v, a, x);

		int i = (int) Math.floor(x);
		double[] e = new double[is];
		double[] f1 = new double[is];
		double[] f2 = new double[is];
		if (x <= -1 || x >= s) {
		} else if (x < 0) {
			a.getDoubleArray(f2, 0);
		} else if (x >= s - 1) {
			a.getDoubleArray(f1, s - 1);
		} else {
			a.getDoubleArray(f1, i);
			a.getDoubleArray(f2, i + 1);
		}

		double t = x - i;
		for (int j = 0; j < is; j++)
			e[j] = (1 - t) * f1[j] + t * f2[j];
		Assert.assertArrayEquals(e, v, 1e-15);
	}

	private void checkInterpolate(Dataset a, double x, double y) {
		int s0 = a.getShapeRef()[0];
		int s1 = a.getShapeRef()[1];
//		double v = Maths.interpolate(a, x, y);
		double v = Maths.interpolate(a, new double[] {x, y});
		if (x <= -1 || x >= s0 || y <= -1 || y >= s1) {
			Assert.assertEquals(0, v, 1e-15);
			return;
		}

		int i = (int) Math.floor(x);
		int j = (int) Math.floor(y);
		double t1 = x - i;
		double t2 = y - j;
		double f1 = 0, f2 = 0, f3 = 0, f4 = 0;
		if (y < 0) {
			if (x < 0) {
				f4 = a.getDouble(0, 0);
			} else if (x >= s0 - 1) {
				f3 = a.getDouble(s0 - 1, 0);
			} else {
				f3 = a.getDouble(i, 0);
				f4 = a.getDouble(i + 1, 0);
			}
		} else if (y >= s1 - 1) {
			if (x < 0) {
				f2 = a.getDouble(0, s1 - 1);
			} else if (x >= s0 - 1) {
				f1 = a.getDouble(s0 - 1, s1 - 1);
			} else {
				f1 = a.getDouble(i, s1 - 1);
				f2 = a.getDouble(i + 1, s1 -1);
			}
		} else {
			if (x < 0) {
				f2 = a.getDouble(0, j);
				f4 = a.getDouble(0, j + 1);
			} else if (x >= s0 - 1) {
				f1 = a.getDouble(s0 - 1, j);
				f3 = a.getDouble(s0 - 1, j + 1);
			} else {
				f1 = a.getDouble(i, j);
				f2 = a.getDouble(i + 1, j);
				f3 = a.getDouble(i, j + 1);
				f4 = a.getDouble(i + 1, j + 1);
			}
		}
		double r = (1 - t1) * (1 - t2) * f1 + t1 * (1 - t2) * f2 + (1 - t1) * t2 * f3 + t1 * t2 * f4;
		Assert.assertEquals(r, v, 1e-15);

		v = Maths.interpolate(a, DatasetFactory.ones(a), x, y);
		Assert.assertEquals(r, v, 1e-15);
	}

	private void checkInterpolateArray(CompoundDataset a, double x, double y) {
		int s0 = a.getShapeRef()[0];
		int s1 = a.getShapeRef()[1];
		int is = a.getElementsPerItem();
		double[] v = new double[is];
		Maths.interpolate(v, a, x, y);
		if (x <= -1 || x >= s0 || y <= -1 || y >= s1) {
			Assert.assertArrayEquals(new double[is], v, 1e-15);
			return;
		}

		double[] f1 = new double[is];
		double[] f2 = new double[is];
		double[] f3 = new double[is];
		double[] f4 = new double[is];
		int i = (int) Math.floor(x);
		int j = (int) Math.floor(y);
		double t1 = x - i;
		double t2 = y - j;
		if (y < 0) {
			if (x < 0) {
				a.getDoubleArray(f4, 0, 0);
			} else if (x >= s0 - 1) {
				a.getDoubleArray(f3, s0 - 1, 0);
			} else {
				a.getDoubleArray(f3, i, 0);
				a.getDoubleArray(f4, i + 1, 0);
			}
		} else if (y >= s1 - 1) {
			if (x < 0) {
				a.getDoubleArray(f2, 0, s1 - 1);
			} else if (x >= s0 - 1) {
				a.getDoubleArray(f1, s0 - 1, s1 - 1);
			} else {
				a.getDoubleArray(f1, i, s1 - 1);
				a.getDoubleArray(f2, i + 1, s1 -1);
			}
		} else {
			if (x < 0) {
				a.getDoubleArray(f2, 0, j);
				a.getDoubleArray(f4, 0, j + 1);
			} else if (x >= s0 - 1) {
				a.getDoubleArray(f1, s0 - 1, j);
				a.getDoubleArray(f3, s0 - 1, j + 1);
			} else {
				a.getDoubleArray(f1, i, j);
				a.getDoubleArray(f2, i + 1, j);
				a.getDoubleArray(f3, i, j + 1);
				a.getDoubleArray(f4, i + 1, j + 1);
			}
		}
		for (j = 0; j < is; j++) {
			f1[j] = (1 - t1) * (1 - t2) * f1[j] + t1 * (1 - t2) * f2[j] + (1 - t1) * t2 * f3[j] + t1 * t2 * f4[j];
		}
		Assert.assertArrayEquals(f1, v, 1e-15);
	}

	@Test
	public void testLinearInterpolation() {
		Dataset xa = DatasetFactory.createRange(IntegerDataset.class, 60);
		xa.iadd(1);

		double[] xc = {-1.25, -1, -0.25, 0, 0.25, 58.25, 59, 59.25, 60, 60.25};
//		double[] xc = {59};
		for (double x : xc) {
//			System.out.printf("%g\n", x);
			checkInterpolate(xa, x);
			checkInterpolate2(xa, x);
			checkInterpolate3(xa, x);
		}

		Dataset xb = DatasetFactory.createRange(IntegerDataset.class, 120);
		xb.setShape(60, 2);
		xb.ifloorDivide(2);
		xb = DatasetUtils.createCompoundDatasetFromLastAxis(xb, true);

		for (double x : xc) {
			checkInterpolate(xb, x);
			checkInterpolate2(xb, x);
			checkInterpolate3(xb, x);
		}

		TestUtils.assertDatasetEquals(Maths.interpolate(DatasetFactory.createFromObject(new double[] {1, 2, 3}), DatasetFactory.createFromObject(new double[] {3, 2, 0}), DatasetFactory.createFromObject(new double[] {0, 1, 1.5, 2.72, 3.14}), 3, 0), DatasetFactory.createFromObject(new double[] {3. ,  3. ,  2.5 ,  0.56,  0.}));

		CompoundDataset cxb = (CompoundDataset) xb;
		for (double x : xc) {
			checkInterpolateArray(cxb, x);
		}

		xa.setShape(6, 10);
		xc = new double[] {-1.25, -1, -0.25, 0, 0.25, 5.25, 6, 6.25, 7};
		double[] yc = {-1.25, -1, -0.25, 0, 0.25, 8.25, 9, 9.25, 10, 10.25};
		for (double x : xc) {
			for (double y : yc) {
//				System.out.printf("%g %g\n", x, y);
				checkInterpolate(xa, x, y);
			}
		}

		cxb.setShape(6, 10);
//		xc = new double[] {-0.25, 0, 0.25, 5.25, 6, 6.25, 7};
//		yc = new double[] {9.25, 10, 10.25};
		for (double x : xc) {
			for (double y : yc) {
//				System.out.printf("%g %g\n", x, y);
				checkInterpolateArray(cxb, x, y);
			}
		}
	}

	@Test
	public void testBitwise() {
		Dataset xa = DatasetFactory.createRange(ByteDataset.class, -4, 4, 1);
		Dataset xb = DatasetFactory.createRange(ByteDataset.class, 8);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 1, 2, 3, 0, 1, 2, 3}),
				Maths.bitwiseAnd(xa, xb), ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-4, -3, -2, -1, 4, 5, 6, 7}),
				Maths.bitwiseOr(xa, xb), ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-4, -4, -4, -4, 4, 4, 4, 4}),
				Maths.bitwiseXor(xa, xb), ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {3, 2, 1, 0, -1, -2, -3, -4}),
				Maths.bitwiseInvert(xa), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-1, -2, -3, -4, -5, -6, -7, -8}),
				Maths.bitwiseInvert(xb), ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-4, -6, -8, -8, 0, 32, -128, -128}),
				Maths.leftShift(xa, xb), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 0, 0, 0, 4, 10, 24, 56}),
				Maths.leftShift(xb, xa), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 0, 0, 0, 0, 2, 8, 24}),
				Maths.leftShift(xa, xa), ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-4, -2, -1, -1, 0, 0, 0, 0}),
				Maths.rightShift(xa, xb), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 0, 0, 0, 4, 2, 1, 0}),
				Maths.rightShift(xb, xa), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-1, -1, -1, -1, 0, 0, 0, 0}),
				Maths.rightShift(xa, xa), ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-4, 126, 63, 31, 0, 0, 0, 0}),
				Maths.unsignedRightShift(xa, xb), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 0, 0, 0, 4, 2, 1, 0}),
				Maths.unsignedRightShift(xb, xa), ABSERRD, ABSERRD);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}),
				Maths.unsignedRightShift(xa, xa), ABSERRD, ABSERRD);
	}

	@Test
	public void testDivideTowardsFloor() {
		Dataset xa = DatasetFactory.createRange(ByteDataset.class, -4, 4, 1);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-2, -2, -1, -1,  0,  0,  1,  1}),
				Maths.divideTowardsFloor(xa, 2), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {2, 1, 1,  0,  0,  -1,  -1, -2}),
				Maths.divideTowardsFloor(xa, -2), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {-1.6, -1.2, -0.8, -0.4, 0, 0.4, 0.8, 1.2}),
				Maths.divideTowardsFloor(xa, 2.5), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new float[] {1.6f, 1.2f, 0.8f, 0.4f, 0 , -0.4f, -0.8f, -1.2f}),
				Maths.divideTowardsFloor(xa, -2.5f), true, ABSERRD, ABSERRD);
	}

	@Test
	public void testFloorDivide() {
		Dataset xa = DatasetFactory.createRange(ByteDataset.class, -4, 4, 1);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {-2, -2, -1, -1,  0,  0,  1,  1}),
				Maths.floorDivide(xa, 2), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {2, 1, 1,  0,  0,  -1,  -1, -2}),
				Maths.floorDivide(xa, -2), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {-2, -2, -1, -1,  0,  0,  0,  1}),
				Maths.floorDivide(xa, 2.5), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new float[] {1, 1,  0,  0,  0, -1,  -1, -2}),
				Maths.floorDivide(xa, -2.5f), true, ABSERRD, ABSERRD);
	}

	@Test
	public void testFloorRemainder() {
		Dataset xa = DatasetFactory.createRange(ByteDataset.class, -4, 4, 1);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, 1, 0, 1, 0, 1, 0, 1}),
				Maths.floorRemainder(xa, 2), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new byte[] {0, -1, 0, -1, 0, -1, 0, -1}),
				Maths.floorRemainder(xa, -2), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {1, 2, 0.5, 1.5, 0, 1, 2, 0.5}),
				Maths.floorRemainder(xa, 2.5), true, ABSERRD, ABSERRD);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new float[] {-1.5f, -0.5f, -2, -1, 0, -1.5f, -0.5f, -2}),
				Maths.floorRemainder(xa, -2.5f), true, ABSERRD, ABSERRD);
	}

	@Test
	public void testArctan2Integer() {
		Dataset a = DatasetFactory.createFromObject(new int[] { 4, 2, 6 });
		Dataset b = DatasetFactory.createFromObject(new int[] { 1, 2, 3 });

		int size = a.getSize();
		int[] c = new int[size];
		for (int i = 0; i < size; i++) {
			double atan2 = Math.atan2(a.getDouble(i), b.getDouble(i));
			c[i] = (int) atan2;
		}
		Dataset expectedResult = DatasetFactory.createFromObject(c);

		Dataset actualResult = Maths.arctan2(a, b);
		TestUtils.assertDatasetEquals(expectedResult, actualResult, true, ABSERRD, ABSERRD);
	}

	@Test
	public void testHypotenusInteger() {
		Dataset a = DatasetFactory.createFromObject(new int[] { 4, 2, 6 });
		Dataset b = DatasetFactory.createFromObject(new int[] { 1, 2, 3 });

		int size = a.getSize();
		int[] c = new int[size];
		for (int i = 0; i < size; i++) {
			double hypot = Math.hypot(a.getDouble(i), b.getDouble(i));
			c[i] = (int) hypot;
		}
		Dataset expectedResult = DatasetFactory.createFromObject(c);

		Dataset actualResult = Maths.hypot(a, b);
		TestUtils.assertDatasetEquals(expectedResult, actualResult, true, ABSERRD, ABSERRD);
	}

	@Test
	public void testAbsComplexInput() {
		double[] ds = new double[] { -0, 1, 2, -3, 4, 5 };
		Dataset a = DatasetFactory.createFromObject(ComplexDoubleDataset.class, ds);
		Dataset o = DatasetFactory.createFromObject(ComplexDoubleDataset.class, new double[] { 0, 4, 0, 0, 0, 0 });

		int size = ds.length;
		double[] c = new double[size];
		for (int i = 0; i < size; i = i + 2) {
			double val = Math.hypot(ds[i], ds[i + 1]);
			c[i] = val; // real part
			c[i + 1] = 0; // imaginary part
		}
		Dataset expectedResult = DatasetFactory.createFromObject(ComplexDoubleDataset.class, c);

		Dataset actualResult = Maths.abs(a, o);
		TestUtils.assertDatasetEquals(expectedResult, actualResult, true, ABSERRD, ABSERRD);
	}

	@Test
	public void testSingleInputCompoundOutputHandling() {
		short[] sa = new short[] { 0, -1, -3, 4};
		int is = 2;
		testSingleInputCompoundOutputHandling(DatasetFactory.createFromObject(is, CompoundShortDataset.class, sa));
		testSingleInputCompoundOutputHandling(DatasetFactory.createFromObject(is, CompoundFloatDataset.class, sa));
	}

	private void testSingleInputCompoundOutputHandling(CompoundDataset a) {
		int is = a.getElementsPerItem();
		int os;
		Dataset o;
		int size = a.getSize();
		short[] c;
		int ms;

		// 2-element -> 1-element
		os = 1;
		o = DatasetFactory.createFromObject(os, CompoundShortDataset.class, new short[] { 0, 4});
		c = new short[os*size];
		ms = Math.min(is, os);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < ms; j++) {
				short val = (short) Math.tan(a.getElementLongAbs(i*is + j));
				c[i*os + j] = val;
			}
		}
		Dataset expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);
		Maths.tan(a, o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);

		// 2-element -> 2-element
		os = 2;
		o = DatasetFactory.createFromObject(os, CompoundShortDataset.class, new short[] { 0, 4, 0, 0});
		c = new short[os*size];
		ms = Math.min(is, os);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < ms; j++) {
				short val = (short) Math.tan(a.getElementLongAbs(i*is + j));
				c[i*os + j] = val;
			}
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);
		Maths.tan(a, o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);

		// 1-element -> 3-element
		os = 3;
		o = DatasetFactory.createFromObject(os, CompoundShortDataset.class, new short[] { 0, 4, 1, 0, 0, -2});
		c = new short[os*size];
		for (int i = 0; i < size; i++) {
			short val = (short) Math.tan(a.getElementLongAbs(i*is));
			for (int j = 0; j < os; j++) {
				c[i*os + j] = val;
			}
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);
		Maths.tan(a.getElements(0), o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);
	}

	@Test
	public void testDoubleInputCompoundOutputHandling() {
		short[] sa = new short[] { 0, -1, -3, 4};
		int is = 2;
		short[] sb = new short[] { -1, 2, -1, 3};

		testDoubleInputCompoundOutputHandling(DatasetFactory.createFromObject(is, CompoundShortDataset.class, sa),
				DatasetFactory.createFromObject(is, CompoundShortDataset.class, sb));

		testDoubleInputCompoundOutputHandling(DatasetFactory.createFromObject(is, CompoundFloatDataset.class, sa),
				DatasetFactory.createFromObject(is, CompoundFloatDataset.class, sb));
	}

	private void testDoubleInputCompoundOutputHandling(CompoundDataset a, CompoundDataset b) {
		int is = a.getElementsPerItem();
		int os;
		Dataset o;
		Dataset expectedResult;
		int size = a.getSize();
		short[] c;

		// 2-element + 2-element -> 1-element
		os = 1;
		o = DatasetFactory.createFromObject(os, CompoundShortDataset.class, new short[] { 4, -5});
		c = new short[os*size];
		for (int i = 0; i < size; i++) {
			short val = (short) (a.getElementLongAbs(i*is) + b.getElementLongAbs(i*is));
			c[i*os] = val;
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);

		Maths.add(a, b, o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);

		// 2-element + 2-element -> 2-element
		os = 2;
		o = DatasetFactory.createFromObject(os, CompoundShortDataset.class, new short[] { 0, 4, 0, -5});
		c = new short[os*size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < os; j++) {
				short val = (short) (a.getElementLongAbs(i*is + j) + b.getElementLongAbs(i*is + j));
				c[i*os + j] = val;
			}
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);

		Maths.add(a, b, o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);

		// 1-element + 2-element -> 2-element
		o.fill(-1);
		c = new short[os*size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < os; j++) {
				short val = (short) (a.getElementLongAbs(i*is) + b.getElementLongAbs(i*is + j));
				c[i*os + j] = val;
			}
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);

		Maths.add(a.getElements(0), b, o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);

		// 2-element + 1-element -> 2-element
		o.fill(-1);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < os; j++) {
				short val = (short) (a.getElementLongAbs(i*is + j) + b.getElementLongAbs(i*is));
				c[i*os + j] = val;
			}
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);

		Maths.add(a, b.getElements(0), o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);

		// 1-element + 1-element -> 2-element
		o.fill(-1);
		for (int i = 0; i < size; i++) {
			short val = (short) (a.getElementLongAbs(i*is) + b.getElementLongAbs(i*is));
			for (int j = 0; j < os; j++) {
				c[i*os + j] = val;
			}
		}
		expectedResult = DatasetFactory.createFromObject(os, CompoundShortDataset.class, c);

		Maths.add(a.getElements(0), b.getElements(0), o);
		TestUtils.assertDatasetEquals(expectedResult, o, true, ABSERRD, ABSERRD);
	}

	@Test
	public void testComplexInputMaths() {
		double[] da = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		Dataset a, b, c;

		a = new ComplexDoubleDataset(da);
		// complex operand + float operand
		b = DatasetFactory.createRange(6);

		c = DatasetFactory.ones(6);
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0, 3, 6, 9, 12, 15}), c);

		c = DatasetFactory.ones(ComplexFloatDataset.class, 6).iadd(new Complex(0, -0.5));
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(ComplexFloatDataset.class,
				new float[] {0, 1, 3, 3, 6, 5, 9, 7, 12, 9, 15, 11}), c);

		c = DatasetFactory.ones(ShortDataset.class, 6);
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new short[] {0, 3, 6, 9, 12, 15}), c);

		// complex operand + integer operand
		b = DatasetFactory.createRange(ByteDataset.class, 6);

		c = DatasetFactory.ones(6);
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0, 3, 6, 9, 12, 15}), c);

		c = DatasetFactory.ones(ComplexFloatDataset.class, 6).iadd(new Complex(0, -0.5));
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(ComplexFloatDataset.class,
				new float[] {0, 1, 3, 3, 6, 5, 9, 7, 12, 9, 15, 11}), c);

		c = DatasetFactory.ones(ShortDataset.class, 6);
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new short[] {0, 3, 6, 9, 12, 15}), c);
	}

	@Test
	public void testComplexOutputMaths() {
		Dataset a, b, c;

		Dataset e = DatasetFactory.createFromObject(ComplexFloatDataset.class,
				new float[] {0, 0, 3, 0, 6, 0, 9, 0, 12, 0, 15, 0});
		a = DatasetFactory.createRange(ByteDataset.class, 6).imultiply(2);
		b = DatasetFactory.createRange(ByteDataset.class, 6);
		c = DatasetFactory.ones(ComplexFloatDataset.class, 6).iadd(new Complex(0, -0.5));
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(e, c);

		b = DatasetFactory.createRange(DoubleDataset.class, 6);
		c = DatasetFactory.ones(ComplexFloatDataset.class, 6).iadd(new Complex(0, -0.5));
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(e, c);

		e = e.cast(ComplexDoubleDataset.class);
		a = DatasetFactory.createRange(FloatDataset.class, 6).imultiply(2);
		b = DatasetFactory.createRange(ByteDataset.class, 6);
		c = DatasetFactory.ones(ComplexDoubleDataset.class, 6).iadd(new Complex(0, -0.5));
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(e, c);

		b = DatasetFactory.createRange(DoubleDataset.class, 6);
		c = DatasetFactory.ones(ComplexDoubleDataset.class, 6).iadd(new Complex(0, -0.5));
		Maths.add(a, b, c);
		TestUtils.assertDatasetEquals(e, c);
	}

	@Test
	public void testClipping() {
		Dataset a = DatasetFactory.createFromObject(FloatDataset.class,
				new float[] {0, 0, 3, 0, 6, 0, 9, 0, 12, 0, 15, 0});

		ByteDataset b = a.cast(ByteDataset.class);
		Dataset c = b;
		TestUtils.assertDatasetEquals(Maths.clip(c, 0, 9), Maths.upperClip(c, 9));
		TestUtils.assertDatasetEquals(Maths.clip(c, 0, 8.1), Maths.upperClip(c, 8.1));
		TestUtils.assertDatasetEquals(Maths.clip(c, 2, 15), Maths.lowerClip(c, 2));
		TestUtils.assertDatasetEquals(Maths.clip(c, 2.1, 15), Maths.lowerClip(c, 2.1));

		a.idivide(10);
		c = a;
		TestUtils.assertDatasetEquals(Maths.clip(c, 0, 0.9), Maths.upperClip(c, 0.9));
		TestUtils.assertDatasetEquals(Maths.clip(c, 0, 0.81), Maths.upperClip(c, 0.81));
		TestUtils.assertDatasetEquals(Maths.clip(c, 0.2, 15), Maths.lowerClip(c, 0.2));
		TestUtils.assertDatasetEquals(Maths.clip(c, 0.21, 15), Maths.lowerClip(c, 0.21));
	}

	@Test
	public void testDerivativeOnSliceViews() {
		Dataset x = Random.rand(60).sort(null);
		Dataset y = Random.rand(60);

		testDerivativeOnSliceViews(x, y, null);
		testDerivativeOnSliceViews(x, y, new Slice(15));
		testDerivativeOnSliceViews(x, y, new Slice(2, null));
		testDerivativeOnSliceViews(x, y, new Slice(2, null, 3));
		testDerivativeOnSliceViews(x, y, new Slice(2, 15));
		testDerivativeOnSliceViews(x, y, new Slice(15, null, -2));

		x.setShape(20, 3);
		y.setShape(x.getShapeRef());
		x = DatasetUtils.createCompoundDatasetFromLastAxis(x, true);
		y = DatasetUtils.createCompoundDatasetFromLastAxis(y, true);
		testDerivativeOnSliceViews(x, y, null);
		testDerivativeOnSliceViews(x, y, new Slice(15));
		testDerivativeOnSliceViews(x, y, new Slice(2, null));
		testDerivativeOnSliceViews(x, y, new Slice(2, null, 3));
		testDerivativeOnSliceViews(x, y, new Slice(2, 15));
		testDerivativeOnSliceViews(x, y, new Slice(15, null, -2));
	}

	private void testDerivativeOnSliceViews(Dataset x, Dataset y, Slice s) {
		TestUtils.assertDatasetEquals(Maths.derivative(x.getSlice(s), y.getSlice(s), 2), Maths.derivative(x.getSlice(s), y.getSliceView(s), 2));
	}
}
