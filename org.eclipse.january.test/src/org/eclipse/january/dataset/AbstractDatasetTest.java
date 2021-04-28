/*-
 * Copyright (c) 2012, 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.january.dataset;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.eclipse.january.asserts.TestUtils;
import org.eclipse.january.metadata.StatisticsMetadata;
import org.junit.Assert;
import org.junit.Test;

public class AbstractDatasetTest {
	@Test
	public void testBestDType() {
		assertEquals(FloatDataset.class, InterfaceUtils.getBestInterface(ShortDataset.class, FloatDataset.class));
		assertEquals(DoubleDataset.class, InterfaceUtils.getBestInterface(LongDataset.class, FloatDataset.class));
		assertEquals(ComplexFloatDataset.class, InterfaceUtils.getBestInterface(FloatDataset.class, ComplexFloatDataset.class));
		assertEquals(ComplexDoubleDataset.class, InterfaceUtils.getBestInterface(IntegerDataset.class, ComplexFloatDataset.class));
	}

	@Test
	public void testCompatibleShapes() {
		assertTrue("[] and []", ShapeUtils.areShapesCompatible(new int[] {}, new int[] {}));
		assertTrue("[1] and []", ShapeUtils.areShapesCompatible(new int[] {1}, new int[] {}));
		assertFalse("[2] and []", ShapeUtils.areShapesCompatible(new int[] {2}, new int[] {}));
		assertTrue("[2] and [2]", ShapeUtils.areShapesCompatible(new int[] {2}, new int[] {2}));
		assertTrue("[3] and [3]", ShapeUtils.areShapesCompatible(new int[] {3}, new int[] {3}));
		assertTrue("[1,2] and [2]", ShapeUtils.areShapesCompatible(new int[] {1,2}, new int[] {2}));
		assertTrue("[2] and [1,2]", ShapeUtils.areShapesCompatible(new int[] {2}, new int[] {1,2}));
		assertFalse("[10,10] and [10,10,10]", ShapeUtils.areShapesCompatible(new int[] {10,10}, new int[] {10,10,10}));
		assertFalse("[10,10,10] and [10,10]", ShapeUtils.areShapesCompatible(new int[] {10,10,10}, new int[] {10,10}));
		assertTrue("[2] and [2,1,1,1]", ShapeUtils.areShapesCompatible(new int[] {2}, new int[] {2,1,1,1}));
		assertTrue("[2,1] and [2,1,1,1]", ShapeUtils.areShapesCompatible(new int[] {2,1}, new int[] {2,1,1,1}));
		assertFalse("[2,1] and [3,1,1,2]", ShapeUtils.areShapesCompatible(new int[] {2,1}, new int[] {3,1,1,2}));
		assertFalse("[2,1] and [3,1,1,1]", ShapeUtils.areShapesCompatible(new int[] {2,1}, new int[] {3,1,1,1}));
		assertTrue("[1,2,1] and [2,1,1,1]", ShapeUtils.areShapesCompatible(new int[] {1,2,1}, new int[] {2,1,1,1}));
		assertTrue("[1,2,1,3] and [2,1,1,1,3]", ShapeUtils.areShapesCompatible(new int[] {1,2,1,3}, new int[] {2,1,1,1,3}));
		assertTrue("[2,1,1] and [1,1,2]", ShapeUtils.areShapesCompatible(new int[] {2,1,1}, new int[] {1,1,2}));
		assertFalse("[2,1,1] and [1,1,3]", ShapeUtils.areShapesCompatible(new int[] {2,1,1}, new int[] {1,1,3}));
		assertFalse("[2,1,4] and [2,1,1,3]", ShapeUtils.areShapesCompatible(new int[] {2,1,4}, new int[] {2,1,1,3}));
		assertFalse("[2,1,4] and [2,1,3]", ShapeUtils.areShapesCompatible(new int[] {2,1,4}, new int[] {2,1,3}));
		assertFalse("[2,4] and [2,3]", ShapeUtils.areShapesCompatible(new int[] {2,4}, new int[] {2,3}));
		assertTrue("[2,1,4] and [2,1,4]", ShapeUtils.areShapesCompatible(new int[] {2,1,4}, new int[] {2,1,4}));
		assertTrue("[2,1,4] and [2,1,1,4]", ShapeUtils.areShapesCompatible(new int[] {2,1,4}, new int[] {2,1,1,4}));
		assertFalse("[2,4] and [2,4,3]", ShapeUtils.areShapesCompatible(new int[] {2,4}, new int[] {2,4,3}));
		assertFalse("[2,1,4] and [2,4,3]", ShapeUtils.areShapesCompatible(new int[] {2,1,4}, new int[] {2,4,3}));
		assertTrue(ShapeUtils.areShapesCompatible(new int[] {}, new int[] {}, 0));
		assertTrue(ShapeUtils.areShapesCompatible(new int[] {2}, new int[] {3}, 0));
		assertFalse(ShapeUtils.areShapesCompatible(new int[] {2,4}, new int[] {3,4}, 1));
		assertTrue(ShapeUtils.areShapesCompatible(new int[] {2,4}, new int[] {3,4}, 0));
//		assertTrue(ShapeUtils.areShapesCompatible(new int[] {}, new int[] {}));
	}

	@Test
	public void testBroadcastCompatibleShapes() {
		assertTrue("[] and []", ShapeUtils.areShapesBroadcastCompatible(new int[] {}, new int[] {}));
		assertTrue("[1] and []", ShapeUtils.areShapesBroadcastCompatible(new int[] {1}, new int[] {}));
		assertTrue("[2] and []", ShapeUtils.areShapesBroadcastCompatible(new int[] {2}, new int[] {}));
		assertTrue("[2] and [2]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2}, new int[] {2}));
		assertTrue("[3] and [3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {3}, new int[] {3}));
		assertTrue("[1,2] and [2]", ShapeUtils.areShapesBroadcastCompatible(new int[] {1,2}, new int[] {2}));
		assertTrue("[2] and [1,2]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2}, new int[] {1,2}));
		assertTrue("[4,2] and [1,2]", ShapeUtils.areShapesBroadcastCompatible(new int[] {4,2}, new int[] {1,2}));
		assertTrue("[10,10] and [10,10,10]", ShapeUtils.areShapesBroadcastCompatible(new int[] {10,10}, new int[] {10,10,10}));
		assertTrue("[10,10,10] and [10,10]", ShapeUtils.areShapesBroadcastCompatible(new int[] {10,10,10}, new int[] {10,10}));

		
		assertTrue("[2] and [2,1,1,1]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2}, new int[] {2,1,1,1}));
		assertTrue("[2,1] and [2,1,1,1]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1}, new int[] {2,1,1,1}));
		assertTrue("[2,1] and [3,1,1,2]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1}, new int[] {3,1,1,2}));
		assertTrue("[2,1] and [3,1,1,1]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1}, new int[] {3,1,1,1}));
		assertTrue("[1,2,1] and [2,1,1,1]", ShapeUtils.areShapesBroadcastCompatible(new int[] {1,2,1}, new int[] {2,1,1,1}));
		assertTrue("[1,2,1,3] and [2,1,1,1,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {1,2,1,3}, new int[] {2,1,1,1,3}));
		assertTrue("[2,1,1] and [1,1,2]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,1}, new int[] {1,1,2}));
		assertTrue("[2,1,1] and [1,1,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,1}, new int[] {1,1,3}));
		assertFalse("[2,1,4] and [2,1,1,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,4}, new int[] {2,1,1,3}));
		assertFalse("[2,1,4] and [2,1,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,4}, new int[] {2,1,3}));
		assertFalse("[2,4] and [2,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,4}, new int[] {2,3}));
		assertTrue("[2,1,4] and [2,1,4]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,4}, new int[] {2,1,4}));
		assertTrue("[2,1,4] and [2,1,1,4]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,4}, new int[] {2,1,1,4}));
		assertFalse("[2,4] and [2,4,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,4}, new int[] {2,4,3}));
		assertFalse("[2,1,4] and [2,4,3]", ShapeUtils.areShapesBroadcastCompatible(new int[] {2,1,4}, new int[] {2,4,3}));
	}

	@Test
	public void testMaxMin() {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 12);
		a.setShape(3,4);
		assertEquals("Max", 11, a.max().doubleValue(), 1e-6);
		assertEquals("Max 0", DatasetFactory.createFromObject(new double[] {8,9,10,11}), a.max(0));
		assertEquals("Max 1", DatasetFactory.createFromObject(new double[] {3,7,11}), a.max(1));
		assertEquals("Max -2", DatasetFactory.createFromObject(new double[] {8,9,10,11}), a.max(-2));
		assertEquals("Max -1", DatasetFactory.createFromObject(new double[] {3,7,11}), a.max(-1));
		assertEquals("Max arg", 11, a.argMax());
		assertEquals("Max arg 0 ", DatasetFactory.createFromObject(new int[] {2,2,2,2}), a.argMax(0));
		assertEquals("Max arg 1 ", DatasetFactory.createFromObject(new int[] {3,3,3}), a.argMax(1));
		assertEquals("Max arg -2 ", DatasetFactory.createFromObject(new int[] {2,2,2,2}), a.argMax(-2));
		assertEquals("Max arg -1 ", DatasetFactory.createFromObject(new int[] {3,3,3}), a.argMax(-1));
		a.set(Double.NaN, 1, 0);
		System.out.println(a.toString(true));
		assertTrue("Max", Double.isNaN(a.max().doubleValue()));
		assertTrue("Max", !Double.isNaN(a.max(true).doubleValue()));
		assertTrue("Max 0", equalsWithNaNs(DatasetFactory.createFromObject(new double[] {Double.NaN,9,10,11}), a.max(0)));
		assertTrue("Max 1", equalsWithNaNs(DatasetFactory.createFromObject(new double[] {3,Double.NaN,11}), a.max(1)));
		assertEquals("Max arg", 4, a.argMax());
		assertEquals("Max arg 0 ", DatasetFactory.createFromObject(new int[] {1,2,2,2}), a.argMax(0));
		assertEquals("Max arg 1 ", DatasetFactory.createFromObject(new int[] {3,0,3}), a.argMax(1));
		assertEquals("Max", 11, a.max(true).doubleValue(), 1e-6);
		assertEquals("Max 0", DatasetFactory.createFromObject(new double[] {8,9,10,11}), a.max(0, true));
		assertEquals("Max 1", DatasetFactory.createFromObject(new double[] {3,7,11}), a.max(1, true));
		assertEquals("Max arg", 11, a.argMax(true));
		assertEquals("Max arg 0 ", DatasetFactory.createFromObject(new int[] {2,2,2,2}), a.argMax(0, true));
		assertEquals("Max arg 1 ", DatasetFactory.createFromObject(new int[] {3,3,3}), a.argMax(1, true));

		a.set(Double.NEGATIVE_INFINITY, 1, 1);
		System.out.println(a.toString(true));
		assertTrue("Max", Double.isNaN(a.max().doubleValue()));
		assertTrue("Max", !Double.isNaN(a.max(true).doubleValue()));
		assertTrue("Max", Double.isNaN(a.max(false, true).doubleValue()));
		assertTrue("Max", !Double.isNaN(a.max(true, false).doubleValue()));
		assertEquals("Max", 11, a.max(true).doubleValue(), 1e-6);
		assertTrue("Min", Double.isNaN(a.min().doubleValue()));
		assertTrue("Min", !Double.isNaN(a.min(true).doubleValue()));
		assertTrue("Min", Double.isNaN(a.min(false, true).doubleValue()));
		assertTrue("Min", !Double.isNaN(a.min(true, false).doubleValue()));
		assertTrue("Min", !Double.isInfinite(a.min().doubleValue()));
		assertTrue("Min", !Double.isInfinite(a.min(true).doubleValue()));
		assertTrue("Min", !Double.isInfinite(a.min(false, true).doubleValue()));
		assertTrue("Min", Double.isInfinite(a.min(true, false).doubleValue()));
		assertEquals("Min", 0, a.min(true).doubleValue(), 1e-6);

		// test other code path
		Dataset b = DatasetFactory.createRange(DoubleDataset.class, 12);
		b.setShape(3,4);
		b.mean(); // trigger summary stats calculation
		assertEquals("Max", 11, b.max().doubleValue(), 1e-6);
		assertEquals("Max arg", 11, b.argMax());
		b.set(Double.NaN, 1, 0);
		b.mean(); // trigger summary stats calculation
		assertTrue("Max", Double.isNaN(b.max().doubleValue()));
		assertEquals("Max arg", 4, b.argMax());
		b.mean(true);
		assertEquals("Max", 11, b.max(true).doubleValue(), 1e-6);
		assertEquals("Max arg", 11, b.argMax(true));

		// check strided datasets give same max/min positions
		a = DatasetFactory.createRange(DoubleDataset.class, 12).reshape(3, 4);
		b = a.getSliceView(new Slice(1, null), new Slice(0, null, 2));
		Dataset c = a.getSlice(new Slice(1, null), new Slice(0, null, 2));

		Assert.assertEquals(c.argMax(), b.argMax());
		Assert.assertEquals(c.argMin(), b.argMin());
	}

	@Test
	public void testGetSpeed() {
		final int ITERATIONS = 1000;
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 1000000);
		long start, startN, startP;

		start = -System.nanoTime();
		for (int i = 0; i < 10; i++) {
			a.getDouble(i);
		}
		start += System.nanoTime();

		start = -System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			a.getDouble(i);
		}
		start += System.nanoTime();

		startN = -System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			a.getDouble(new int[] {i});
		}
		startN += System.nanoTime();

		startP = -System.nanoTime();
		int[] pos = new int[1];
		for (int i = 0; i < ITERATIONS; i++) {
			pos[0] = i;
			a.getDouble(pos);
		}
		startP += System.nanoTime();

		System.out.printf("Get 1D double took %gus (cf %gus and %gus)\n", start*1e-3/ITERATIONS, startN*1e-3/ITERATIONS, startP*1e-3/ITERATIONS);

		a.setShape(1000, 1000);
		start = -System.nanoTime();
		for (int i = 0; i < 10; i++) {
			a.getDouble(i, i);
		}
		start += System.nanoTime();

		start = -System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			a.getDouble(i, i);
		}
		start += System.nanoTime();

		startN = -System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			a.getDouble(new int[] {i, i});
		}
		startN += System.nanoTime();

		startP = -System.nanoTime();
		pos = new int[2];
		for (int i = 0; i < ITERATIONS; i++) {
			pos[0] = i;
			pos[1] = i;
			a.getDouble(pos);
		}
		startP += System.nanoTime();

		System.out.printf("Get 2D double took %gus (cf %gus and %gus)\n", start*1e-3/ITERATIONS, startN*1e-3/ITERATIONS, startP*1e-3/ITERATIONS);
	}

	@Test
	public void testHash() {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 12);
		a.setShape(3,4);
		Dataset b = DatasetFactory.createRange(DoubleDataset.class, 12);
		b.setShape(3,4);

		b.mean(); // trigger other code path
		assertEquals("Hash code", a.hashCode(), b.hashCode());

		a.set(Double.POSITIVE_INFINITY, 1, 0);
		b.set(Double.POSITIVE_INFINITY, 1, 0);
		b.mean(); // trigger other code path
		assertEquals("Hash code", a.hashCode(), b.hashCode());

		a.set(Double.NaN, 0, 1);
		b.set(Double.NaN, 0, 1);
		b.mean(); // trigger other code path
		assertEquals("Hash code", a.hashCode(), b.hashCode());
	}

	private static boolean equalsWithNaNs(Dataset a, Dataset b) {
		if (a.equals(b))
			return true;

		IndexIterator ita = a.getIterator();
		IndexIterator itb = b.getIterator();
		while (ita.hasNext() && itb.hasNext()) {
			final double av = a.getElementDoubleAbs(ita.index);
			final double bv = b.getElementDoubleAbs(itb.index);
			if (Double.isNaN(av)) {
				if (!Double.isNaN(bv))
					return false;
			} else {
				if (av != bv)
					return false;
			}
		}
		return true;
	}

	@Test
	public void testMaxSpeed() {
		long start;
		long elapsed;
		final int ITERATIONS = 200;

		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 1000000);
		for (int i = 0; i < 10; i++) {
			a.set(1, 0);
			start = -System.nanoTime();
			a.max();
			start += System.nanoTime();
		}
		elapsed = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			a.set(1, 0);
			start = -System.nanoTime();
			a.max();
			start += System.nanoTime();

			elapsed += start;
		}
		System.out.printf("Max double calculation took %g ms\n", elapsed*1e-6/ITERATIONS);

		a = DatasetFactory.createRange(ShortDataset.class, 1000000);
		elapsed = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			a.set(1, 0);
			start = -System.nanoTime();
			a.max();
			start += System.nanoTime();

			elapsed += start;
		}
		System.out.printf("Max short calculation took %g ms\n", elapsed*1e-6/ITERATIONS);
	}

	@Test
	public void testSort() {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 12);
		a.set(Double.NaN, 0);
		a.setShape(3, 4);
		a.sort(-1);
		TestUtils.verbosePrintf("%s\n", a.toString());
		assertEquals("First element", 1, a.getDouble(0,0), 1e-6);
		assertTrue("0,3 element", Double.isNaN(a.getDouble(0,3)));
		assertEquals("Final element", 11, a.getDouble(2,3), 1e-6);

		a.sort(0);
		TestUtils.verbosePrintf("%s\n", a.toString());
		assertEquals("First element", 1, a.getDouble(0,0), 1e-6);
		assertEquals("0,3 element", 7, a.getDouble(0,3), 1e-6);
		assertTrue("Final element", Double.isNaN(a.getDouble(2,3)));

		a.set(12, 0, 0);
		a.sort(null);
		TestUtils.verbosePrintf("%s\n", a.toString());
		assertEquals("First element", 2, a.getDouble(0, 0), 1e-6);
		assertEquals("2,2 element", 12, a.getDouble(2,2), 1e-6);
		assertTrue("Final element", Double.isNaN(a.getDouble(2,3)));

		// sort a view
		a = DatasetFactory.createRange(DoubleDataset.class, 12);
		Dataset b = a.getSliceView(new Slice(null, null, -2));
		b.sort(null);
		assertEquals("First element", 1, b.getDouble(0), 1e-6);
		assertEquals("Final element", 11, b.getDouble(5), 1e-6);
		// side effect on base dataset
		assertEquals("First element", 0, a.getDouble(0), 1e-6);
		assertEquals("Second element", 11, a.getDouble(1), 1e-6);
		assertEquals("Final element", 1, a.getDouble(11), 1e-6);

		// sort ancillary datasets
		b = DatasetFactory.createRange(IntegerDataset.class, 12);
		a = b.getSlice(new Slice(null, null, -1)).cast(ShortDataset.class);
		DatasetUtils.sort(a, b);
		assertEquals("First element", 0, a.getInt(0));
		assertEquals("Second element", 1, a.getInt(1));
		assertEquals("First element", 11, b.getInt(0));
		assertEquals("Second element", 10, b.getInt(1));
	}

	@Test
	public void testPut() {
		Dataset d1 = DatasetFactory.createRange(DoubleDataset.class, 6);
		
		DatasetUtils.put(d1, new int[] {2, 5}, DatasetFactory.createFromObject(new double[] {-2, -5.5}));
		TestUtils.assertDatasetEquals(d1, DatasetFactory.createFromObject(new double[] {0, 1, -2, 3, 4, -5.5}));
	
		DatasetUtils.put(d1, DatasetFactory.createFromObject(new int[] {0, 4}), DatasetFactory.createFromObject(new double[] {-2, -5.5}));
		TestUtils.assertDatasetEquals(d1, DatasetFactory.createFromObject(new double[] {-2, 1, -2, 3, -5.5, -5.5}));
	
		d1 = DatasetFactory.createRange(DoubleDataset.class, 6).reshape(2, 3);
		DatasetUtils.put(d1, new int[] {2, 5}, DatasetFactory.createFromObject(new double[] {-2, -5.5}));
		TestUtils.assertDatasetEquals(d1, DatasetFactory.createFromObject(new double[] {0, 1, -2, 3, 4, -5.5}).reshape(2, 3));
	
		DatasetUtils.put(d1, DatasetFactory.createFromObject(new int[] {0, 4}), DatasetFactory.createFromObject(new double[] {-2, -5.5}));
		TestUtils.assertDatasetEquals(d1, DatasetFactory.createFromObject(new double[] {-2, 1, -2, 3, -5.5, -5.5}).reshape(2, 3));
	}

	@Test
	public void testTake() {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 12);
		Dataset t;

		t = DatasetUtils.take(a, new int[] {0, 2, 4}, null);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {0, 2, 4}, 0);
		TestUtils.verbosePrintf("%s\n", t.toString());

		a.setShape(new int[] {3,4});
		TestUtils.verbosePrintf("%s\n", a.toString());

		t = DatasetUtils.take(a, new int[] {0}, 0);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {1}, 0);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {2}, 0);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {0}, 1);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {1}, 1);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {2}, 1);
		TestUtils.verbosePrintf("%s\n", t.toString());

		t = DatasetUtils.take(a, new int[] {3}, 1);
		TestUtils.verbosePrintf("%s\n", t.toString());
	}

	/**
	 * Tests for squeeze method
	 */
	@Test
	public void testSqueeze() {
		Dataset ds = DatasetFactory.createRange(DoubleDataset.class, 10);
		ds.setShape(2,1,5);

		ds.squeeze();
		assertEquals(2, ds.getShapeRef().length);
		assertEquals(2, ds.getShapeRef()[0]);
		assertEquals(5, ds.getShapeRef()[1]);

		int[] os, ns;
		os = new int[] { 1, 1 };
		ns = ShapeUtils.squeezeShape(os, false);
		assertEquals(0, ns.length);
		ns = ShapeUtils.squeezeShape(os, true);
		assertEquals(0, ns.length);

		os = new int[] { 2, 1, 5 };
		ns = ShapeUtils.squeezeShape(os, false);
		assertEquals(2, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(5, ns[1]);
		ns = ShapeUtils.squeezeShape(os, true);
		assertEquals(3, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(1, ns[1]);
		assertEquals(5, ns[2]);

		os = new int[] { 2, 1, 5, 1 };
		ns = ShapeUtils.squeezeShape(os, false);
		assertEquals(2, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(5, ns[1]);
		ns = ShapeUtils.squeezeShape(os, true);
		assertEquals(3, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(1, ns[1]);
		assertEquals(5, ns[2]);

		os = new int[] { 1, 2, 1, 5 };
		ns = ShapeUtils.squeezeShape(os, false);
		assertEquals(2, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(5, ns[1]);
		ns = ShapeUtils.squeezeShape(os, true);
		assertEquals(3, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(1, ns[1]);
		assertEquals(5, ns[2]);

		os = new int[] { 1, 2, 1, 5, 1 };
		ns = ShapeUtils.squeezeShape(os, false);
		assertEquals(2, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(5, ns[1]);
		ns = ShapeUtils.squeezeShape(os, true);
		assertEquals(3, ns.length);
		assertEquals(2, ns[0]);
		assertEquals(1, ns[1]);
		assertEquals(5, ns[2]);
	}

	/**
	 * Tests for tile method
	 */
	@Test
	public void testTile() {
		// 1D
		Dataset ds = DatasetFactory.createRange(DoubleDataset.class, 3);

		Dataset ta = DatasetUtils.tile(ds, 2);
		double[] xa = { 0., 1., 2., 0., 1., 2. };

		assertEquals(1, ta.getShapeRef().length);
		assertEquals(6, ta.getShapeRef()[0]);
		for (int i = 0; i < xa.length; i++) {
			assertEquals(xa[i], ((DoubleDataset) ta).getData()[i], 1e-6);
		}

		Dataset tb = DatasetUtils.tile(ds, 1, 2);

		assertEquals(2, tb.getShapeRef().length);
		assertEquals(1, tb.getShapeRef()[0]);
		assertEquals(6, tb.getShapeRef()[1]);
		for (int i = 0; i < xa.length; i++) {
			assertEquals(xa[i], ((DoubleDataset) tb).getData()[i], 1e-6);
		}

		Dataset tc = DatasetUtils.tile(ds, 2, 1);

		assertEquals(2, tc.getShapeRef().length);
		assertEquals(2, tc.getShapeRef()[0]);
		assertEquals(3, tc.getShapeRef()[1]);
		for (int i = 0; i < xa.length; i++) {
			assertEquals(xa[i], ((DoubleDataset) tc).getData()[i], 1e-6);
		}

		// 2D
		ds = DatasetFactory.createRange(DoubleDataset.class, 6);
		ds.setShape(2,3);
		Dataset td = DatasetUtils.tile(ds, 2);
		double[] xd = { 0., 1., 2., 0., 1., 2., 3., 4., 5., 3., 4., 5. };

		assertEquals(2, td.getShapeRef().length);
		assertEquals(2, td.getShapeRef()[0]);
		assertEquals(6, td.getShapeRef()[1]);
		for (int i = 0; i < xd.length; i++) {
			assertEquals(xd[i], ((DoubleDataset) td).getData()[i], 1e-6);
		}

		Dataset te = DatasetUtils.tile(ds, 1, 2);

		assertEquals(2, te.getShapeRef().length);
		assertEquals(2, te.getShapeRef()[0]);
		assertEquals(6, te.getShapeRef()[1]);
		for (int i = 0; i < xd.length; i++) {
			assertEquals(xd[i], ((DoubleDataset) te).getData()[i], 1e-6);
		}

		Dataset tf = DatasetUtils.tile(ds, 2, 1);
		double[] xf = { 0., 1., 2., 3., 4., 5., 0., 1., 2., 3., 4., 5. };

		assertEquals(2, tf.getShapeRef().length);
		assertEquals(4, tf.getShapeRef()[0]);
		assertEquals(3, tf.getShapeRef()[1]);
		for (int i = 0; i < xf.length; i++) {
			assertEquals(xf[i], ((DoubleDataset) tf).getData()[i], 1e-6);
		}
	}

	@Test
	public void testTileSpeed() {
		int[][] blocks = {{1024,1}, {256,4}, {64,16}, {32,32}, {16,64}, {4, 256}, {1,1024}};
//		int[][] blocks = {{1024,64}, {256,64}, {64,64}, {32,64}, {16,64}, {4, 64}, {1,64}};

		int[][] shapes = { {1024, 2048}, {2048, 2048}, {2048, 1024} };

		for (int b = 0; b < blocks.length; b++) {
			for (int s = 0; s < shapes.length; s++) {
				for (int n = 0; n < 3; n++)
					runTile(blocks[b][0], blocks[b][1], shapes[s][0], shapes[s][1]);
			}
		}
	}

	private void runTile(final int srows, final int scols, final int rows, final int cols) {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, srows * scols).reshape(srows, scols);

		long start, end;

		TestUtils.verbosePrintf("Tile %sx%d Block %dx%d: ", rows, cols, srows, scols);

		final int nrows = rows/srows;
		final int ncols = cols/scols;

		start = System.currentTimeMillis();
		DoubleDataset b = DatasetFactory.zeros(DoubleDataset.class, rows, cols);
		final double[] da = (double[]) a.getBuffer();
		final double[] db = b.getData();
		if (scols == 1) {
			for (int i = 0; i < db.length; i++) {
				db[i] = da[(i / cols) % srows];
			}
		} else if (srows == 1) {
			for (int i = 0; i < db.length; i++) {
				db[i] = da[i % scols];
			}
		} else {
			for (int i = 0; i < db.length; i++) {
				db[i] = da[((i / cols) % srows) * scols + i % scols];
			}
		}
		end = System.currentTimeMillis();
		long diff1 = end - start;
		TestUtils.verbosePrintf("array = %d ms, ", diff1);

		start = System.currentTimeMillis();
		final Dataset tiled = DatasetUtils.tile(a, nrows, ncols);
		end = System.currentTimeMillis();
		long diff2 = end - start;
		TestUtils.verbosePrintf("tile = %d ms\n", diff2);

		assertEquals(rows, tiled.getShapeRef()[0]);
		assertEquals(cols, tiled.getShapeRef()[1]);
		assertEquals("Datasets not equal", tiled, b);

		assertTrue("Creation of tile took more than 50x as long as array creation of same size! (It took "
				+ diff2 + "ms)", diff2 <= (diff1 * 50));
	}

	/**
	 * Tests for transpose method
	 */
	@Test
	public void testTranspose() {
		Dataset ds = DatasetFactory.createFromObject(DoubleDataset.class, (Object) null);
		TestUtils.assertDatasetEquals(ds, ds.transpose());
		ds = DatasetFactory.createRange(DoubleDataset.class, 6);
		TestUtils.assertDatasetEquals(ds, ds.transpose());

		ds.setShape(2,3);
		Dataset ta = DatasetUtils.transpose(ds, 1, 0);
		double[][] xa = { { 0., 1., 2. }, { 3., 4., 5. } };

		assertEquals(2, ta.getShapeRef().length);
		assertEquals(3, ta.getShapeRef()[0]);
		assertEquals(2, ta.getShapeRef()[1]);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals(xa[i][j], ta.getDouble(j, i), 1e-6);
			}
		}
		TestUtils.assertDatasetEquals(ta, ds.getTransposedView(1, 0).getSlice());

		ds.set(-2, 1, 2);
		assertEquals(-2., ds.getDouble(1, 2), 1e-6);
		assertEquals(5., ta.getDouble(2, 1), 1e-6);
		ta = ds.getTransposedView(); // test view has changed
		assertEquals(-2., ta.getDouble(2, 1), 1e-6);

		// 3D
		ds = DatasetFactory.createRange(DoubleDataset.class, 24);
		ds.setShape(2, 3, 4);

		double[][][] xb = { {{ 0., 1., 2., 3.}, {4., 5., 6., 7.}, {8., 9., 10., 11. }},
				{{12., 13., 14., 15.}, {16., 17., 18., 19.}, {20., 21., 22., 23.}} };

		Dataset tb;

		try {
			tb = DatasetUtils.transpose(ds, 0);
		} catch (IllegalArgumentException e) {
			// this is correct.
		} catch (Exception e) {
			fail("wrong exception type passed from incorrect arguments being passed to the constructor");
		}

		try {
			tb = DatasetUtils.transpose(ds, 0, -1, 0);
		} catch (IllegalArgumentException e) {
			// this is correct.
		} catch (Exception e) {
			fail("wrong exception type passed from incorrect arguments being passed to the constructor");
		}

		try {
			tb = DatasetUtils.transpose(ds, 0, 1, 1);
		} catch (IllegalArgumentException e) {
			// this is correct.
		} catch (Exception e) {
			fail("wrong exception type passed from incorrect arguments being passed to the constructor");
		}

		tb = DatasetUtils.transpose(ds, 0, 1, 2);
		assertEquals(3, tb.getShapeRef().length);
		assertEquals(2, tb.getShapeRef()[0]);
		assertEquals(3, tb.getShapeRef()[1]);
		assertEquals(4, tb.getShapeRef()[2]);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 4; k++) {
					assertEquals(xb[i][j][k], tb.getDouble(i, j, k), 1e-6);
				}
			}
		}

		tb = DatasetUtils.transpose(ds, 1, 0, 2);
		assertEquals(3, tb.getShapeRef().length);
		assertEquals(3, tb.getShapeRef()[0]);
		assertEquals(2, tb.getShapeRef()[1]);
		assertEquals(4, tb.getShapeRef()[2]);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 4; k++) {
					assertEquals(xb[i][j][k], tb.getDouble(j, i, k), 1e-6);
				}
			}
		}

		tb = DatasetUtils.transpose(ds, 2, 0, 1);
		assertEquals(3, tb.getShapeRef().length);
		assertEquals(4, tb.getShapeRef()[0]);
		assertEquals(2, tb.getShapeRef()[1]);
		assertEquals(3, tb.getShapeRef()[2]);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 4; k++) {
					assertEquals(xb[i][j][k], tb.getDouble(k, i, j), 1e-6);
				}
			}
		}
	}

	/**
	 * Tests for repeat method
	 */
	@Test
	public void testRepeat() {
		// 2D
		Dataset ds = DatasetFactory.createRange(DoubleDataset.class, 6);
		ds.setShape(2,3);

		double[] xa = { 0., 0., 1., 1., 2., 2., 3., 3., 4., 4., 5., 5. };
		DoubleDataset ta = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {2}, -1);
		assertEquals(1, ta.getShapeRef().length);
		assertEquals(12, ta.getShapeRef()[0]);
		for (int i = 0; i < 12; i++) {
			assertEquals(xa[i], ta.get(i), 1e-6);
		}

		double[][] xb = { { 0., 0., 1., 1., 2., 2. }, {  3., 3., 4., 4., 5., 5. }  };
		DoubleDataset tb = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {2}, 1);
		assertEquals(2, tb.getShapeRef().length);
		assertEquals(2, tb.getShapeRef()[0]);
		assertEquals(6, tb.getShapeRef()[1]);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 6; j++) {
				assertEquals(xb[i][j], tb.get(i, j), 1e-6);
			}
		}

		double[][] xc = { { 0., 1., 2. }, { 0., 1., 2. }, {  3., 4., 5. }, {  3., 4., 5. }  };
		DoubleDataset tc = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {2}, 0);
		assertEquals(2, tc.getShapeRef().length);
		assertEquals(4, tc.getShapeRef()[0]);
		assertEquals(3, tc.getShapeRef()[1]);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals(xc[i][j], tc.get(i, j), 1e-6);
			}
		}

		double[][] xd = { { 0., 1., 2. }, { 0., 1., 2. }, {  3., 4., 5. } };
		DoubleDataset td = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {2, 1}, 0);
		assertEquals(2, td.getShapeRef().length);
		assertEquals(3, td.getShapeRef()[0]);
		assertEquals(3, td.getShapeRef()[1]);
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals(xd[i][j], td.get(i, j), 1e-6);
			}
		}

		double[][] xe = { { 0., 1., 1., 2., 2., 2.}, {  3., 4., 4., 5., 5., 5. }  };
		DoubleDataset te = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {1, 2, 3}, 1);
		assertEquals(2, te.getShapeRef().length);
		assertEquals(2, te.getShapeRef()[0]);
		assertEquals(6, te.getShapeRef()[1]);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 6; j++) {
				assertEquals(xe[i][j], te.get(i, j), 1e-6);
			}
		}

		double[] xf = { 0., 1., 2., 2., 5., 5., 5. };
		DoubleDataset tf = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {1, 1, 2, 0, 0, 3}, -1);
		assertEquals(1, tf.getShapeRef().length);
		assertEquals(7, tf.getShapeRef()[0]);
		for (int i = 0; i < 7; i++) {
			assertEquals(xf[i], tf.get(i), 1e-6);
		}

		try {
			tf = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {0}, 3);
		} catch (IllegalArgumentException e) {
			// this is correct.
		} catch (Exception e) {
			fail("wrong exception type passed from incorrect arguments being passed to the constructor");
		}

		try {
			tf = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {2, 1}, -1);
		} catch (IllegalArgumentException e) {
			// this is correct.
		} catch (Exception e) {
			fail("wrong exception type passed from incorrect arguments being passed to the constructor");
		}

		try {
			tf = (DoubleDataset) DatasetUtils.repeat(ds, new int[] {-1}, -1);
		} catch (IllegalArgumentException e) {
			// this is correct.
		} catch (Exception e) {
			fail("wrong exception type passed from incorrect arguments being passed to the constructor");
		}
	}

	/**
	 * Tests for resize method
	 */
	@Test
	public void testResize() {
		int size = 6;
		Dataset ds = DatasetFactory.createRange(DoubleDataset.class, size);
		Dataset tf;
		IndexIterator it;

		tf = DatasetUtils.resize(ds, 3);
		assertArrayEquals(new int[] {3}, tf.getShapeRef());
		it = tf.getIterator();
		while (it.hasNext()) {
			assertEquals(it.index % size, tf.getElementDoubleAbs(it.index), 1e-6);
		}

		tf = DatasetUtils.resize(ds, 8);
		assertArrayEquals(new int[] {8}, tf.getShapeRef());
		it = tf.getIterator();
		while (it.hasNext()) {
			assertEquals(it.index % size, tf.getElementDoubleAbs(it.index), 1e-6);
		}

		tf = DatasetUtils.resize(ds, 3, 4);
		assertArrayEquals(new int[] {3, 4}, tf.getShapeRef());
		it = tf.getIterator();
		while (it.hasNext()) {
			assertEquals(it.index % size, tf.getElementDoubleAbs(it.index), 1e-6);
		}

		ds.setShape(2,3);

		tf = DatasetUtils.resize(ds, 3);
		assertArrayEquals(new int[] {3}, tf.getShapeRef());
		it = tf.getIterator();
		while (it.hasNext()) {
			assertEquals(it.index % size, tf.getElementDoubleAbs(it.index), 1e-6);
		}

		tf = DatasetUtils.resize(ds, 8);
		assertArrayEquals(new int[] {8}, tf.getShapeRef());
		it = tf.getIterator();
		while (it.hasNext()) {
			assertEquals(it.index % size, tf.getElementDoubleAbs(it.index), 1e-6);
		}

		tf = DatasetUtils.resize(ds, 3, 4);
		assertArrayEquals(new int[] {3, 4}, tf.getShapeRef());
		it = tf.getIterator();
		while (it.hasNext()) {
			assertEquals(it.index % size, tf.getElementDoubleAbs(it.index), 1e-6);
		}
	}

	/**
	 * Test contents functions
	 */
	@Test
	public void testContents() {
		double[] x = { 0, 2., -12.3 };
		double[] y = { 2.3, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
		double[] z = { 1e14, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };

		Dataset ta = DatasetFactory.createFromObject(x);
		assertEquals(false, ta.containsNans());
		assertEquals(false, ta.containsInfs());

		Dataset tb = DatasetFactory.createFromObject(y);
		assertEquals(true, tb.containsNans());
		assertEquals(true, tb.containsInfs());
		assertEquals(true, Double.isNaN(tb.min().doubleValue()));
		assertEquals(false, Double.isInfinite(tb.min().doubleValue()));
		assertEquals(true, Double.isNaN(tb.max().doubleValue()));
		assertEquals(false, Double.isInfinite(tb.max().doubleValue()));

		Dataset f = tb.cast(FloatDataset.class);
		assertEquals(true, f.containsNans());
		assertEquals(true, f.containsInfs());
		assertEquals(true, Double.isNaN(f.min().doubleValue()));
		assertEquals(false, Double.isInfinite(f.min().doubleValue()));
		assertEquals(true, Double.isNaN(f.max().doubleValue()));
		assertEquals(false, Double.isInfinite(f.max().doubleValue()));

		Dataset tc = DatasetFactory.createFromObject(z);
		assertEquals(true, Double.isInfinite(tc.min().doubleValue()));
		assertEquals(true, Double.isInfinite(tc.max().doubleValue()));
	}
	
	@Test
	public void testView() {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 60);
		Dataset b = a.getView(true);
		assertEquals(true, a.equals(b));

		a.setShape(5, 3, 4);
		b = a.getTransposedView();
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 4; k++) {
					assertEquals(a.getDouble(i, j, k), b.getDouble(k, j, i), 1e-12);
				}
			}
		}
	}

	/**
	 * Test equals and hashCode
	 */
	@Test
	public void testEquals() {
		Dataset a, b, c, d, e;
		a = DatasetFactory.createRange(DoubleDataset.class, 20);
		b = DatasetFactory.createRange(DoubleDataset.class, 20);
		c = a.clone();
		d = Maths.add(a, 0.5);
		e = DatasetFactory.createRange(FloatDataset.class, 20);

		assertTrue(a.equals(b));
		assertFalse(a == b);
		assertTrue(a.equals(c));
		assertFalse(a == c);
		assertFalse(a.equals(d));
		assertFalse(a.equals(e));
		HashSet<Dataset> set = new HashSet<Dataset>();
		assertTrue(set.add(a));
		assertTrue(set.contains(a));
		assertTrue(set.contains(b));
		assertTrue(set.contains(c));
		assertFalse(set.contains(d));
		assertFalse(set.contains(e));
		assertFalse(set.add(b)); // b is same as a so do nothing
		assertEquals(1, set.size());
		assertTrue(set.add(d));
		assertTrue(set.add(e));
		assertEquals(3, set.size());
		assertTrue(set.contains(d));
		assertTrue(set.contains(e));
		assertTrue(set.contains(Maths.subtract(d, 0.5)));
		assertFalse(set.contains(Maths.subtract(d, 0.5001)));
		assertTrue(set.contains(e.cast(DoubleDataset.class)));
		assertTrue(set.contains(b.cast(FloatDataset.class)));
	}

	@Test
	public void testPrint() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 1000000);

		System.out.println(a);

		System.out.println(a.reshape(1000, 1000));

		System.out.println(a.reshape(100, 100, 100));

//		System.out.println(a.reshape(10, 10, 100, 100));

		Dataset b = DatasetFactory.createRange(IntegerDataset.class, 12);

		System.out.println(b);

		System.out.println(b.reshape(1,12));

		System.out.println(b.reshape(4,1,3));
	}

	@Test
	public void testToString() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 100);

		assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]", a.getSliceView(new Slice(10)).toString(true));

		AbstractDataset.setMaxLineLength(20);
		assertEquals("[0, 1, 2, ..., 97, 98, 99]", a.toString(true));

		AbstractDataset.setMaxLineLength(15);
		assertEquals("[10, 11, ..., 13, 14]", a.getSliceView(new Slice(10, 15)).toString(true));

		AbstractDataset.setMaxLineLength(30);
		assertEquals("[0, 1, 2, 3, 4, ..., 96, 97, 98, 99]", a.toString(true));

		assertEquals("[99, 98, 97, 96, ..., 4, 3, 2, 1, 0]", a.getSliceView(new Slice(null, null, -1)).toString(true));

		assertEquals("[]", a.getSliceView(new Slice(10, 10)).toString(true));
	}

	@Test
	public void testSlicing() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 1000);
		Dataset s, t;
		IndexIterator is, it;

		s = a.getSlice(null, new int[] {10}, null);
		assertEquals(1, s.getShapeRef().length);
		assertEquals(10, s.getShapeRef()[0]);

		is = s.getIterator();
		for (int i = 0; is.hasNext(); i++) {
			assertEquals(i, s.getElementLongAbs(is.index));
		}

		t = a.getSlice(new Slice(10));
		assertEquals(1, t.getShapeRef().length);
		assertEquals(10, t.getShapeRef()[0]);

		it = t.getIterator();
		for (int i = 0; it.hasNext(); i++) {
			assertEquals(i, t.getElementLongAbs(it.index));
		}

		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(new int[]{0}, new int[] {10}, null);
		assertEquals(1, s.getShapeRef().length);
		assertEquals(10, s.getShapeRef()[0]);

		s = a.getSlice(new int[]{-1000}, new int[] {10}, null);
		assertEquals(1, s.getShapeRef().length);
		assertEquals(10, s.getShapeRef()[0]);

		s = a.getSlice(new int[] {9}, null, new int[] {-1});
		assertEquals(1, s.getShapeRef().length);
		assertEquals(10, s.getShapeRef()[0]);

		is = s.getIterator();
		for (int i = 9; is.hasNext(); i--) {
			assertEquals(i, s.getElementLongAbs(is.index));
		}

		t = a.getSlice(new Slice(9, null, -1));
		assertEquals(1, t.getShapeRef().length);
		assertEquals(10, t.getShapeRef()[0]);

		it = t.getIterator();
		for (int i = 9; it.hasNext(); i--) {
			assertEquals(i, t.getElementLongAbs(it.index));
		}

		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(new int[] {2}, new int[] {10}, null);
		t = a.getSlice(new Slice(2, 10));
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(new int[] {2}, new int[] {10}, new int[] {3});
		t = a.getSlice(new Slice(2, 10, 3));
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		t = a.getSlice(new Slice(2000));
		assertArrayEquals(a.getShapeRef(), t.getShapeRef());

		t = a.getSlice(new Slice(12, 10, 3));
		assertArrayEquals(new int[] {0}, t.getShapeRef());

		t = a.getSlice(new Slice(2, 10, -3));
		assertArrayEquals(new int[] {0}, t.getShapeRef());

		a.setShape(10, 10, 10);

		s = a.getSlice(null, null, null);
		t = a.getSlice();
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(null, null, null);
		Slice[] slice = null;
		t = a.getSlice(slice);
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(null, new int[] {8, 10, 10}, null);
		t = a.getSlice(new Slice(8));
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(null, new int[] {8, 3, 10}, null);
		t = a.getSlice(new Slice(8), new Slice(3));
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		s = a.getSlice(null, new int[] {8, 10, 3}, null);
		t = a.getSlice(new Slice(8), null, new Slice(3));
		is = s.getIterator();
		it = t.getIterator();
		while (is.hasNext() && it.hasNext()) {
			assertEquals(s.getElementLongAbs(is.index), t.getElementLongAbs(it.index));
		}

		// test input SliceND checking
		try {
			a.getSlice(new SliceND(null));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			a.getSlice(new SliceND(new int[0]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			a.getSlice(new SliceND(new int[2]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		s = a.getSlice(new SliceND(a.getShape()));
		Assert.assertEquals("Full slice", a, s);
	}

	@Test
	public void testSlicingViews() {
		Dataset a, b, c;
		a = DatasetFactory.createRange(32).reshape(4, 8);
		checkSliceView(a, new int[] {0, 1}, new int[] {3, 5}, new int[] {1, 2});
		checkSliceView(a, new int[] {1, -1}, new int[] {-1, 3}, new int[] {1, -2});

		a = DatasetFactory.createRange(60).reshape(6, 10);
		b = checkSliceView(a, new int[] {0, 1}, new int[] {6, 8}, new int[] {1, 2}); // 6x4
		c = b.getSliceView(new int[] {0, 1}, new int[] {1, 4}, null);
		c.setShape(3);
		checkSliceView(b, new int[] {1, 0}, new int[] {5, 3}, new int[] {2, 1});
		checkSliceView(b, new int[] {1, -1}, new int[] {5, 2}, new int[] {2, -1});
		c = a.getSlice(new int[] {0, 1}, new int[] {6, 8}, new int[] {1, 2});
		b.setShape(2,3,4);
		c.setShape(2,3,4);
		assertEquals(c, b);
		b.setShape(6,4);
		b.setShape(6,2,2);
		c.setShape(6,2,2);
		assertEquals(c, b);
		b.setShape(6,4);
		try {
			b.setShape(2,12);
			fail("Should have raised an exception");
		} catch (IllegalArgumentException iae) {
			// expected
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		}

		b = checkSliceView(a, new int[] {1, -1}, new int[] {-1, 2}, new int[] {1, -2}); // 4x4
		checkSliceView(b, new int[] {1, 0}, new int[] {4, 3}, new int[] {2, 1});
		checkSliceView(b, new int[] {1, -1}, new int[] {4, 2}, new int[] {2, -1});

		b = checkSliceView(a, new int[] {0, 1}, new int[] {1, 8}, new int[] {1, 2}); // 1x4
		b = checkSliceView(a, new int[] {0, 1}, new int[] {6, 2}, new int[] {1, 2}); // 6x1

		// test special case of zero-rank dataset
		a = DatasetFactory.createFromObject(DoubleDataset.class , 1.);
		b = a.getSliceView();
		b.setShape(1);
		assertTrue(b.getIterator().hasNext());

		// test input SliceND checking
		a = DatasetFactory.createRange(60).reshape(6, 10);
		try {
			a.getSliceView(new SliceND(null));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			a.getSliceView(new SliceND(new int[0]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			a.getSliceView(new SliceND(new int[2]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		b = a.getSliceView(new SliceND(a.getShape()));
		Assert.assertEquals("Full slice", a, b);
	}

	private Dataset checkSliceView(Dataset a, int[] start, int[] stop, int[] step) {
		Dataset s = a.getSliceView(start, stop, step).squeeze();
		Dataset t = a.getSlice(start, stop, step).squeeze();
		assertArrayEquals(t.getShapeRef(), s.getShapeRef());
		assertEquals(t.toString(true), t, s);
		IndexIterator iter = s.getIterator(true);
		int[] pos = iter.getPos();
		while (iter.hasNext()) {
			assertEquals(iter.index, s.get1DIndex(pos));
			int[] p = s.getNDPosition(iter.index);
			assertArrayEquals(Arrays.toString(pos) + " : " + Arrays.toString(p), pos, p);
		}

		// test for correct copying of non-contiguous datasets
		assertArrayEquals(((DoubleDataset) t.flatten()).getData(), ((DoubleDataset) s.flatten()).getData(), 1e-15);

		TestUtils.assertEquals("Max", t.max().doubleValue(), s.max().doubleValue());
		TestUtils.assertEquals("Min", t.min().doubleValue(), s.min().doubleValue());
		return s;
	}

	@Test
	public void testSliceStrings() {
		String s;
		s = Slice.createString(new int[] {3}, null, null, null);
		assertEquals(":", s);

		s = Slice.createString(new int[] {3}, null, null, new int[] {1});
		assertEquals(":", s);

		s = Slice.createString(new int[] {3}, null, new int[] {2}, new int[] {1});
		assertEquals(":2", s);

		s = Slice.createString(new int[] {4}, new int[] {1}, new int[] {3}, new int[] {1});
		assertEquals("1:3", s);

		s = Slice.createString(new int[] {4}, new int[] {1}, new int[] {2}, new int[] {1});
		assertEquals("1", s);

		s = Slice.createString(new int[] {4}, new int[] {1}, new int[] {3}, new int[] {2});
		assertEquals("1", s);

		s = Slice.createString(new int[] {5}, null, null, new int[] {2});
		assertEquals("::2", s);

		s = Slice.createString(new int[] {5}, new int[] {1}, new int[] {4}, new int[] {2});
		assertEquals("1:4:2", s);

		s = Slice.createString(new int[] {5}, new int[] {1}, new int[] {5}, new int[] {2});
		assertEquals("1::2", s);

		s = Slice.createString(new int[] {5}, new int[] {1}, new int[] {3}, new int[] {2});
		assertEquals("1", s);

		s = Slice.createString(new int[] {3}, null, null, new int[] {-1});
		assertEquals("::-1", s);

		s = Slice.createString(new int[] {5}, new int[] {3}, new int[] {1}, new int[] {-1});
		assertEquals("3:1:-1", s);

		s = Slice.createString(new int[] {5}, new int[] {4}, new int[] {1}, new int[] {-1});
		assertEquals(":1:-1", s);

		s = Slice.createString(new int[] {5}, new int[] {3}, new int[] {0}, new int[] {-1});
		assertEquals("3:0:-1", s);

		s = Slice.createString(new int[] {5}, new int[] {3}, new int[] {-1}, new int[] {-1});
		assertEquals("3::-1", s);

		s = Slice.createString(new int[] {5}, new int[] {3}, new int[] {2}, new int[] {-1});
		assertEquals("3", s);

		s = Slice.createString(new int[] {5}, new int[] {3}, new int[] {1}, new int[] {-2});
		assertEquals("3", s);

		s = Slice.createString(new int[] {3, 2}, null, null, null);
		assertEquals(":,:", s);
	}

	@Test
	public void testSetSlice() {
		Dataset a = DatasetFactory.createRange(100).reshape(20, 5);
		a.setSlice(-2, null, new Slice(null, null, 2));

		assertEquals(-2, a.getDouble(0, 0), 1e-15);
		assertEquals(1,  a.getDouble(0, 1), 1e-15);
		assertEquals(-2, a.getDouble(0, 2), 1e-15);
		assertEquals(3,  a.getDouble(0, 3), 1e-15);
		assertEquals(-2, a.getDouble(0, 4), 1e-15);

		// with broadcasting
		a = DatasetFactory.createRange(100).reshape(20, 5);
		a.setSlice(DatasetFactory.createRange(ShortDataset.class, 3), new Slice(2, 10), new Slice(null, null, 2));

		assertEquals(0, a.getDouble(0, 0), 1e-15);
		assertEquals(1, a.getDouble(0, 1), 1e-15);
		assertEquals(2, a.getDouble(0, 2), 1e-15);
		assertEquals(3, a.getDouble(0, 3), 1e-15);
		assertEquals(4, a.getDouble(0, 4), 1e-15);

		assertEquals(5, a.getDouble(1, 0), 1e-15);
		assertEquals(6, a.getDouble(1, 1), 1e-15);
		assertEquals(7, a.getDouble(1, 2), 1e-15);
		assertEquals(8, a.getDouble(1, 3), 1e-15);
		assertEquals(9, a.getDouble(1, 4), 1e-15);

		assertEquals(0, a.getDouble(2, 0), 1e-15);
		assertEquals(11, a.getDouble(2, 1), 1e-15);
		assertEquals(1, a.getDouble(2, 2), 1e-15);
		assertEquals(13, a.getDouble(2, 3), 1e-15);
		assertEquals(2, a.getDouble(2, 4), 1e-15);

		try {
			a.setSlice(DatasetFactory.createRange(ShortDataset.class, 3), new Slice(2, 7, 2), new Slice(2, 3));
			fail("Should have thrown an IAE");
		} catch (IllegalArgumentException e) {
		}

		// compound
		CompoundDataset c = DatasetFactory.createRange(3, CompoundDoubleDataset.class, 100).reshape(20, 5);
		c.setSlice(DatasetFactory.createRange(ShortDataset.class, 3), new Slice(2, 10), new Slice(null, null, 2));
	}

	@Test
	public void testFillDataset() {
		Dataset other = DatasetFactory.zeros(10);
		Dataset a = DatasetFactory.createRange(100).reshape(20, 5);

		assertEquals(0.0, other.max().doubleValue(), 1e-14);
		a.fillDataset(other, a.getSliceIterator(null, new int[] {10, 1}, null));
		assertEquals(45.0, other.max().doubleValue(), 1e-14);

		CompoundDataset c = DatasetFactory.createRange(3, CompoundDoubleDataset.class, 100).reshape(20, 5);
		CompoundDataset cOther = DatasetFactory.compoundZeros(3, CompoundDoubleDataset.class, 10);
		assertArrayEquals(new double[] {0, 0, 0}, cOther.maxItem(), 1e-14);
		c.fillDataset(cOther, c.getSliceIterator(null, new int[] {10, 1}, null));
		assertArrayEquals(new double[] {45.0, 0, 0}, cOther.maxItem(), 1e-14);
	}

	@Test
	public void test1DErrors() {
		
		// test 1D errors for single value
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 100);
		a.setErrors(5);
		
		assertEquals(5.0, a.getError(0), 0.001);
		assertEquals(5.0, a.getError(50), 0.001);
		assertEquals(5.0, a.getError(99), 0.001);

		assertTrue(a.hasErrors());

		// now for pulling out the full error array
		Dataset error = a.getErrors();
		
		// check compatibility
		try {
			ShapeUtils.checkCompatibility(a, error);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
		
		assertEquals(5.0, error.getDouble(0), 0.001);
		assertEquals(5.0, error.getDouble(50), 0.001);
		assertEquals(5.0, error.getDouble(99), 0.001);
		
		// Now set the error as a whole array
		a.setErrors(Maths.multiply(error, 2));
		
		assertEquals(10.0, a.getError(0), 0.001);
		assertEquals(10.0, a.getError(50), 0.001);
		assertEquals(10.0, a.getError(99), 0.001);
		
		// test pulling the error out again, to make sure its correct
		Dataset error2 = a.getErrors();
		
		// check compatibility
		try {
			ShapeUtils.checkCompatibility(a, error2);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
		
		assertEquals(10.0, error2.getDouble(0), 0.001);
		assertEquals(10.0, error2.getDouble(50), 0.001);
		assertEquals(10.0, error2.getDouble(99), 0.001);
	}
	
	
	@Test
	public void test2DErrors() {
		
		// test 1D errors for single value
		Dataset a = DatasetFactory.zeros(IntegerDataset.class, 100, 100);
		a.setErrors(5);
		
		assertEquals(5.0, a.getError(0,0), 0.001);
		assertEquals(5.0, a.getError(50,50), 0.001);
		assertEquals(5.0, a.getError(99,99), 0.001);

		assertTrue(a.hasErrors());

		// now for pulling out the full error array
		Dataset error = a.getErrors();
		
		// check compatibility
		try {
			ShapeUtils.checkCompatibility(a, error);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
		
		assertEquals(5.0, error.getDouble(0,0), 0.001);
		assertEquals(5.0, error.getDouble(50,50), 0.001);
		assertEquals(5.0, error.getDouble(99,99), 0.001);
		
		// Now set the error as a whole array
		a.setErrors(Maths.multiply(error, 2));
		
		assertEquals(10.0, a.getError(0,0), 0.001);
		assertEquals(10.0, a.getError(50,50), 0.001);
		assertEquals(10.0, a.getError(99,99), 0.001);
		
		// test pulling the error out again, to make sure its correct
		Dataset error2 = a.getErrors();
		
		// check compatibility
		try {
			ShapeUtils.checkCompatibility(a, error2);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
		
		assertEquals(10.0, error2.getDouble(0,0), 0.001);
		assertEquals(10.0, error2.getDouble(50,50), 0.001);
		assertEquals(10.0, error2.getDouble(99,99), 0.001);
	}

	@Test
	public void testErrors() {
		// test errors when reshaped and sliced
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 12).reshape(4, 3);
		Dataset e = DatasetFactory.createRange(DoubleDataset.class, 12).reshape(4, 3);

		a.setErrors(e);

		Dataset b = a.reshape(2, 2, 3);
		Dataset f = b.getErrors();
		assertArrayEquals(b.getShapeRef(), f.getShapeRef());

		Dataset c = b.getSlice(new Slice(1));
		assertArrayEquals(c.getShapeRef(), c.getErrors().getShapeRef());

		Dataset d = b.getSliceView(new Slice(1));
		assertArrayEquals(d.getShapeRef(), d.getErrors().getShapeRef());
	}

	@Test
	public void testSetErrorBuffer() {
		
		Dataset a = DatasetFactory.zeros(IntegerDataset.class, 100, 100);
		Dataset err = DatasetFactory.createLinearSpace(DoubleDataset.class, 0, a.getSize() - 1, a.getSize());
		err.setShape(a.getShapeRef());
		
		a.setErrorBuffer(null);
		assertFalse(a.hasErrors());
		
		a.setErrorBuffer(25.0);
		
		assertEquals(5.0, a.getError(0,0), 0.001);
		assertEquals(5.0, a.getError(50,50), 0.001);
		assertEquals(5.0, a.getError(99,99), 0.001);

		assertTrue(a.hasErrors());
		
		// now for pulling out the full error array and check compatibility
		Dataset error = a.getErrors();
		try {
			ShapeUtils.checkCompatibility(a, error);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
				
		a.setErrorBuffer(err);

		assertEquals(0.0, a.getError(0,0), 0.001);
		assertEquals(Math.sqrt(50.0 + 100*50.0), a.getError(50,50), 0.001);
		assertEquals(Math.sqrt(99.0 + 100*99.0), a.getError(99,99), 0.001);

		assertTrue(a.hasErrors());
		
		// now for pulling out the full error array and check compatibility
		error = a.getErrors();
		try {
			ShapeUtils.checkCompatibility(a, error);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
		
		a.setErrorBuffer(err.getBuffer());

		assertEquals(0.0, a.getError(0,0), 0.001);
		assertEquals(Math.sqrt(35.0 + 100*25.0), a.getError(25,35), 0.001);
		assertEquals(Math.sqrt(99.0 + 100*99.0), a.getError(99,99), 0.001);

		assertTrue(a.hasErrors());
		
		// now for pulling out the full error array and check compatibility
		error = a.getErrors();
		try {
			ShapeUtils.checkCompatibility(a, error);
		} catch (Exception e) {
			fail("Error shape is not the same as input datasets");
		}
	}
	
	@Test
	public void testInternalErrors() {
		
		// test 1D errors for single value
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 100);
		a.setErrors(5);
		
		// should be squared
		Number ne = (Number) a.getErrorBuffer().getObjectAbs(0);
		assertEquals(25.0, ne.doubleValue(), 0.001);
		
		// now for pulling out the full error array
		Dataset error = a.getErrors();
	
		a.setErrors(Maths.multiply(error, 2));
		
		// should also be squared
		Dataset ae = a.getErrorBuffer();
		assertEquals(100.0, ae.getDouble(0), 0.001);
		assertEquals(100.0, ae.getDouble(50), 0.001);
		assertEquals(100.0, ae.getDouble(99), 0.001);	
	}

	@Test
	public void testZeroRankDatasets() {
		Dataset a;
		a = DatasetFactory.ones();
		assertEquals("Rank", 0, a.getRank());
		assertEquals("Shape", 0, a.getShapeRef().length);
		assertEquals("Value", 1.0, a.getObject());
		assertEquals("Max", 1.0, a.max());
		assertEquals("Min", 1.0, a.min());
		assertEquals("MaxPos", 0, a.maxPos().length);
		assertEquals("MinPos", 0, a.minPos().length);
		assertEquals("ArgMax", 0, a.argMax());
		assertEquals("ArgMin", 0, a.argMin());
		assertTrue("Value",  a.equals(Double.valueOf(1.0)));

		a = DatasetFactory.zeros(ShortDataset.class);
		assertEquals("Rank", 0, a.getRank());
		assertEquals("Shape", 0, a.getShapeRef().length);
		assertEquals("Value", (short) 0, a.getObject());

		a = DatasetFactory.createFromObject(new Complex(1.0, -0.5));
		assertEquals("Rank", 0, a.getRank());
		assertEquals("Shape", 0, a.getShapeRef().length);
		assertEquals("Value", new Complex(1.0, -0.5), a.getObject());
		assertEquals("Real view value", 1.0, a.getRealView().getObject());
		assertEquals("Imaginary view value", -0.5, ((ComplexDoubleDataset) a).getImaginaryView().getObject());

		a = DatasetFactory.createFromObject(1.f);
		assertEquals("Rank", 0, a.getRank());
		assertEquals("Shape", 0, a.getShapeRef().length);
		assertEquals("Value", 1.f, a.getObject());

		a = DatasetFactory.ones(1);
		a.squeeze();
		assertEquals("Rank", 0, a.getRank());
		assertEquals("Shape", 0, a.getShapeRef().length);

		a = DatasetFactory.createFromObject(1.f);
		assertEquals("Equals", a, DatasetFactory.createFromObject(1.f));
		assertFalse("Differs", a.equals(DatasetFactory.createFromObject(2.f)));
	}

	@Test
	public void testConcatenate() {
		Dataset a, b, c, d;
		a = DatasetFactory.createRange(6);
		b = DatasetFactory.createRange(6., 8, 1);
		c = DatasetUtils.concatenate(new IDataset[] {a, b}, 0);
		d = DatasetFactory.createRange(8);
		assertEquals("Rank", 1, c.getRank());
		assertTrue("Dataset", c.equals(d));

		a = DatasetFactory.createRange(6).reshape(3,2);
		b = DatasetFactory.createRange(6., 8, 1).reshape(1,2);
		c = DatasetUtils.concatenate(new IDataset[] {a, b}, 0);
		d = DatasetFactory.createRange(8).reshape(4,2);
		assertEquals("Rank", 2, c.getRank());
		assertTrue("Dataset", c.equals(d));

		a.setShape(2,3);
		b = DatasetFactory.createRange(6., 9, 1).reshape(1,3);
		c = DatasetUtils.concatenate(new IDataset[] {a, b}, 0);
		d = DatasetFactory.createRange(9).reshape(3,3);
		assertEquals("Rank", 2, c.getRank());
		assertTrue("Dataset", c.equals(d));

		a = DatasetFactory.createRange(2).reshape(1,2);
		b = DatasetFactory.createRange(3., 5, 1).reshape(1,2);
		a = DatasetUtils.concatenate(new IDataset[] {a, b}, 0);
		b = DatasetFactory.createRange(2., 6, 3).reshape(2,1);
		c = DatasetUtils.concatenate(new IDataset[] {a, b}, 1);
		d = DatasetFactory.createRange(6).reshape(2,3);
		assertEquals("Rank", 2, c.getRank());
		assertTrue("Dataset", c.equals(d));
	}

	@Test
	public void testSum() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 1024*1024);
		assertEquals("Sum", -524288, a.sum()); // 549755289600

		a = a.cast(DoubleDataset.class);
		assertEquals("Sum", 1024*512*(1024.*1024 - 1), ((Number) a.sum()).doubleValue(), 1e-6);

		a = DatasetFactory.createRange(DoubleDataset.class, 12);
		a.setShape(3,4);
		assertEquals("Sum", 11*6, ((Number) a.sum()).doubleValue(), 1e-6);
		a.set(Double.NaN, 0,0);
		assertTrue("Sum", Double.isNaN(((Number) a.sum()).doubleValue()));
		assertEquals("Sum", 11*6, ((Number) a.sum(true)).doubleValue(), 1e-6);
	}

	@Test
	public void testMakeFinite() {
		Dataset a = DatasetFactory.createFromObject(new double[] {0, Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY });
		DatasetUtils.makeFinite(a);

		assertTrue("Make finite", DatasetFactory.createFromObject(new double[] {0, Double.MAX_VALUE, 0, -Double.MAX_VALUE}).equals(a));
	}

	@Test
	public void testCast() {
		Dataset a, c, d;

		d = new LongDataset();
		c = DatasetUtils.cast(DoubleDataset.class, d);

		long[] udata;
		udata = new long[] {0, 1, 127, 128, 255, 256, 32767, 32768, 65535, 65536, 2147483647L, 2147483648L, 4294967295L, 4294967296L};
		d = new LongDataset(udata);
		c = DatasetUtils.cast(IntegerDataset.class, d);
		Assert.assertTrue(c.max().doubleValue() < d.max().doubleValue()); // check stored values
		a = DatasetFactory.createFromObject(true, c);
		assertEquals("Cast", 0, a.getLong(13));
		for (int i = 0; i < 13; i++) {
			assertEquals("Cast", udata[i], a.getLong(i));
		}

		c = DatasetUtils.cast(ShortDataset.class, d);
		Assert.assertTrue(c.max().doubleValue() < d.max().doubleValue());
		a = DatasetFactory.createFromObject(true, c);
		assertEquals("Cast", 0, a.getLong(9));
		for (int i = 0; i < 9; i++) {
			assertEquals("Cast", udata[i], a.getLong(i));
		}

		c = DatasetUtils.cast(ByteDataset.class, d);
		Assert.assertTrue(c.max().doubleValue() < d.max().doubleValue());
		a = DatasetFactory.createFromObject(true, c);
		assertEquals("Cast", 0, a.getLong(5));
		for (int i = 0; i < 5; i++) {
			assertEquals("Cast", udata[i], a.getLong(i));
		}

		// check create does not promote dataset type unnecessarily
		c = DatasetFactory.createFromObject(new long[] {0, 1L<<62});
		a = DatasetFactory.createFromObject(true, c);
		assertTrue(c == a);

		c = DatasetFactory.createFromObject(new int[] {0, 1<<30});
		a = DatasetFactory.createFromObject(true, c);
		assertTrue(c == a);

		c = DatasetFactory.createFromObject(new short[] {0, 1<<14});
		a = DatasetFactory.createFromObject(true, c);
		assertTrue(c == a);

		c = DatasetFactory.createFromObject(new byte[] {0, 1<<6});
		a = DatasetFactory.createFromObject(true, c);
		assertTrue(c == a);
	}

	@Test
	public void testCopy() {
		int[] idata = new int[] {1, -2, 3};
		Dataset d = new IntegerDataset(idata, null);
		Dataset c = d.copy(IntegerDataset.class);
		assertTrue(c != d);
		TestUtils.assertDatasetEquals(c, d);

		c = d.copy(FloatDataset.class);
		TestUtils.assertDatasetEquals(c, d, false);
	}

	@Test
	public void testRoll() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10);

		Dataset r = DatasetUtils.roll(a, 2, null);
		TestUtils.assertDatasetEquals(r, Maths.add(a, 10-2).iremainder(10), 1e-6, 1e-6);

		r = DatasetUtils.roll(a, -2, null);
		TestUtils.assertDatasetEquals(r, Maths.add(a, 10+2).iremainder(10), 1e-6, 1e-6);

		a.setShape(2,5);
		r = DatasetUtils.roll(a, 1, null);
		TestUtils.assertDatasetEquals(r, Maths.add(a, 10-1).iremainder(10).reshape(2,5), 1e-6, 1e-6);

		r = DatasetUtils.roll(a, 1, 0);
		TestUtils.assertDatasetEquals(r, Maths.add(a, 5).iremainder(10).reshape(2,5), 1e-6, 1e-6);

		r = DatasetUtils.roll(a, 1, 1);
		TestUtils.assertDatasetEquals(r, DatasetFactory.createFromObject(new int[] {4, 0, 1, 2, 3, 9, 5, 6, 7, 8}, 2,5), 1e-6, 1e-6);

		r = DatasetUtils.roll(a, 1, -2);
		TestUtils.assertDatasetEquals(r, Maths.add(a, 5).iremainder(10).reshape(2,5), 1e-6, 1e-6);

		r = DatasetUtils.roll(a, 1, -1);
		TestUtils.assertDatasetEquals(r, DatasetFactory.createFromObject(new int[] {4, 0, 1, 2, 3, 9, 5, 6, 7, 8}, 2,5), 1e-6, 1e-6);
	}

	@Test
	public void testRollAxis() {
		Dataset a = DatasetFactory.ones(ByteDataset.class, 3, 4, 5, 6);
		Assert.assertArrayEquals(new int[] {3, 6, 4, 5}, DatasetUtils.rollAxis(a, 3, 1).getShapeRef());
		Assert.assertArrayEquals(new int[] {5, 3, 4, 6}, DatasetUtils.rollAxis(a, 2, 0).getShapeRef());
		Assert.assertArrayEquals(new int[] {3, 5, 6, 4}, DatasetUtils.rollAxis(a, 1, 4).getShapeRef());
		Assert.assertArrayEquals(new int[] {3, 6, 4, 5}, DatasetUtils.rollAxis(a, -1, 1).getShapeRef());
	}

	@Test
	public void testFindOccurrences() {
		Dataset a = DatasetFactory.createFromObject(new double[] {0, 0, 3, 7, -4, 2, 1});
		Dataset v = DatasetFactory.createRange(DoubleDataset.class, -3, 3, 1);

		Dataset indexes = DatasetUtils.findFirstOccurrences(a, v);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {-1, -1, -1, 0, 6, 5}, null), indexes, true, 1, 1);
	}

	@Test
	public void testFindIndexes() {
		Dataset a = DatasetFactory.createFromObject(new double[] {0, 0, 3, 7, -4, 2, 1});
		Dataset v = DatasetFactory.createRange(DoubleDataset.class, -3, 3, 1);

		IntegerDataset indexes = DatasetUtils.findIndexesForValues(a, v);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {3, 3, -1, -1, -1, 5, 4}, null), indexes, true, 1, 1);

		v = DatasetFactory.createFromObject(new double[] {-4, 0, 1, 2, 3, 7});
		indexes = DatasetUtils.findIndexesForValues(a, v);
		TestUtils.assertDatasetEquals(a, v.getBy1DIndex(indexes), true, 1e-6, 1e-6);
	}

	@Test
	public void testAppend() {
		double[] x = { 0., 1., 2., 3., 4., 5. };
		Dataset d1 = DatasetFactory.createRange(DoubleDataset.class, 3.);
		Dataset d2 = DatasetFactory.createRange(DoubleDataset.class, 3., 6., 1.);
		Dataset d3 = DatasetUtils.append(d1, d2, 0);

		for (int i = 0; i < x.length; i++) {
			assertEquals("Append 1", x[i], d3.getDouble(i), 1e-8);
		}

		d1.setShape(1, 3);
		d2.setShape(1, 3);
		d3 = DatasetUtils.append(d1, d2, 0);
		Dataset d4 = DatasetFactory.createFromObject(x);

		d4.setShape(2, 3);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals("Append 2", d4.getDouble(i, j), d3.getDouble(i, j), 1e-8);
			}
		}

		d3 = DatasetUtils.append(d1, d2, 1);
		d4 = DatasetFactory.createFromObject(x);
		d4.setShape(1, 6);
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < 6; j++) {
				assertEquals("Append 3", d4.getDouble(i, j), d3.getDouble(i, j), 1e-8);
			}
		}

		d3 = DatasetUtils.append(d1, d2, -1);
		TestUtils.assertDatasetEquals(d4, d3);
	}

	@Test
	public void testSelect() {
		Dataset a = DatasetFactory.createFromObject(new double[] { 0, 1, 3, 5, -7, -9 });
		Dataset b = DatasetFactory.createFromObject(new double[] { 0.01, 1.2, 2.9, 5, -7.1, -9 });

		Dataset c = a.clone().reshape(2, 3);
		BooleanDataset d = DatasetFactory.createFromObject(BooleanDataset.class, new boolean[] {false, true, false, false, true, false}, 2, 3);

		Dataset e = DatasetUtils.select(new BooleanDataset[] {d}, new Object[] {c}, -2);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {-2, 1, -2, -2, -7, -2}, 2, 3), e);

		Dataset f = b.clone().reshape(2, 3);
		BooleanDataset g = DatasetFactory.createFromObject(BooleanDataset.class, new boolean[] {false, true, true, false, false, false}, 2, 3);

		e = DatasetUtils.select(new BooleanDataset[] {d, g}, new Dataset[] {c, f}, -2.5);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {-2.5, 1, 2.9, -2.5, -7, -2.5}, 2, 3), e);

		e = DatasetUtils.select(d, c, -2);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {-2, 1, -2, -2, -7, -2}, 2, 3), e);
	}

	@Test
	public void testChoose() {
		Dataset a = DatasetFactory.createFromObject(new double[] { 0, 1, 3, 5, -7, -9 });
		Dataset b = DatasetFactory.createFromObject(new double[] { 0.01, 1.2, 2.9, 5, -7.1, -9 });

		Dataset c = a.clone().reshape(2, 3);
		IntegerDataset d = DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, 0, 1, 1, 0, 1}, 2, 3);

		Dataset e = DatasetUtils.choose(d, new Object[] {c, -2}, true, false);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0, 1, -2, -2, -7, -2}, 2, 3), e);

		d = DatasetFactory.createFromObject(IntegerDataset.class, new int[] {-2, 0, 3, 1, 0, 2}, 2, 3);
		try {
			e = DatasetUtils.choose(d, new Object[] {c, -2}, true, false);
			fail("Should have thrown an array index OOB exception");
		} catch (ArrayIndexOutOfBoundsException oob) {
			// expected
		}
		e = DatasetUtils.choose(d, new Object[] {c, -2}, false, false);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0, 1, -2, -2, -7, -9}, 2, 3), e);

		e = DatasetUtils.choose(d, new Object[] {c, -2}, false, true);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0, 1, -2, -2, -7, -2}, 2, 3), e);

		Dataset f = b.clone().reshape(2, 3);
		IntegerDataset g = DatasetFactory.createFromObject(IntegerDataset.class, new int[] {1, 0, 1, 1, 2, 2}, 2, 3);

		e = DatasetUtils.choose(g, new Object[] {c, f, -2}, true, false);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0.01, 1, 2.9, 5, -2, -2}, 2, 3), e);

		g = DatasetFactory.createFromObject(IntegerDataset.class, new int[] {-1, 3, 1, 1, 2, 2}, 2, 3);
		try {
			e = DatasetUtils.choose(d, new Object[] {c, f, -2}, true, false);
			fail("Should have thrown an array index OOB exception");
		} catch (ArrayIndexOutOfBoundsException oob) {
			// expected
		}

		e = DatasetUtils.choose(g, new Object[] {c, f, -2}, false, false);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {-2, 1, 2.9, 5, -2, -2}, 2, 3), e);

		e = DatasetUtils.choose(g, new Object[] {c, f, -2}, false, true);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new double[] {0, -2, 2.9, 5, -2, -2}, 2, 3), e);
	}

	@Test
	public void testSize() {
		int[] zero = new int[] {0};
		int[] one = new int[] {};
		int[] small = new int[] {2};
		int[] medium = new int[] {1024, 1024};
		int[] large = new int[] {1024, 1024, 1024};
		int[] xxxlarge = new int[] {1024, 1024, 1024, 1024};
		int[] bad = new int[] {1024, -1, 1024};

		assertEquals(0, ShapeUtils.calcLongSize(zero));
		assertEquals(0, ShapeUtils.calcSize(zero));

		assertEquals(1, ShapeUtils.calcLongSize(one));
		assertEquals(1, ShapeUtils.calcSize(one));

		assertEquals(2, ShapeUtils.calcLongSize(small));
		assertEquals(2, ShapeUtils.calcSize(small));

		assertEquals(1024*1024, ShapeUtils.calcLongSize(medium));
		assertEquals(1024*1024, ShapeUtils.calcSize(medium));

		assertEquals(1024*1024*1024, ShapeUtils.calcLongSize(large));
		assertEquals(1024*1024*1024, ShapeUtils.calcSize(large));

		assertEquals(1024*1024*1024*1024L, ShapeUtils.calcLongSize(xxxlarge));
		try {
			ShapeUtils.calcSize(xxxlarge);
			fail("Should have thrown an illegal argument exception");
		} catch (IllegalArgumentException e) {
			// expected
		} catch (Throwable t) {
			fail("Should have thrown an illegal argument exception");
		}

		try {
			ShapeUtils.calcLongSize(bad);
			fail("Should have thrown an illegal argument exception");
		} catch (IllegalArgumentException e) {
			// expected
		} catch (Throwable t) {
			fail("Should have thrown an illegal argument exception");
		}

		try {
			ShapeUtils.calcSize(bad);
			fail("Should have thrown an illegal argument exception");
		} catch (IllegalArgumentException e) {
			// expected
		} catch (Throwable t) {
			fail("Should have thrown an illegal argument exception");
		}
	}

	@Test
	public void testFill() {
		Dataset a = DatasetFactory.createRange(DoubleDataset.class, 12);

		Dataset b = DatasetFactory.zeros(a);
		a.fill(0);
		TestUtils.assertDatasetEquals(b, a, 1e-15, 1e-20);

		a.fill(0.);
		TestUtils.assertDatasetEquals(b, a, 1e-15, 1e-20);

		a.fill(0L);
		TestUtils.assertDatasetEquals(b, a, 1e-15, 1e-20);

		a.fill(new Complex(0));
		TestUtils.assertDatasetEquals(b, a, 1e-15, 1e-20);

		a.fill(DatasetFactory.createFromObject(0));
		TestUtils.assertDatasetEquals(b, a, 1e-15, 1e-20);

		a.fill(DatasetFactory.createFromObject(new int[] {0}));
		TestUtils.assertDatasetEquals(b, a, 1e-15, 1e-20);

		try {
			a.fill(DatasetFactory.createFromObject(new int[] {0, 1}));
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testPositions() {
		int[] shape = new int[] { 23, 34, 2 };
		int[] indexes = new int[] {1, 10, 70, 171};
		List<IntegerDataset> list = DatasetUtils.calcPositionsFromIndexes(DatasetFactory.createFromObject(IntegerDataset.class, indexes, 2, 2), shape);

		Assert.assertEquals(shape.length, list.size());
		IntegerDataset l = list.get(0);
		Assert.assertEquals(2, l.getRank());
		Assert.assertEquals(2, l.getShapeRef()[0]);
		Assert.assertEquals(2, l.getShapeRef()[1]);

		checkPositions(list, new int[] {0, 0, 1},  0, 0);
		checkPositions(list, new int[] {0, 5, 0},  0, 1);
		checkPositions(list, new int[] {1, 1, 0},  1, 0);
		checkPositions(list, new int[] {2, 17, 1},  1, 1);
	}

	private void checkPositions(List<IntegerDataset> list, int[] expected, int... position) {
		int j = 0;
		for (int i : expected) {
			IntegerDataset l = list.get(j++);
			Assert.assertEquals(i, l.getInt(position));
		}
	}

	@Test
	public void testIndexes() {
		List<IntegerDataset> list = new ArrayList<IntegerDataset>();
		int[] shape = new int[] { 23, 34, 2 };
		list.add(DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, 0, 1, 2}, 2, 2));
		list.add(DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, 5, 1, 17}, 2, 2));
		list.add(DatasetFactory.createFromObject(IntegerDataset.class, new int[] {1, 0, 0, 1}, 2, 2));
		IntegerDataset indexes = DatasetUtils.calcIndexesFromPositions(list, shape, null);

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {1, 10, 70, 171}, 2, 2), indexes);

		list.set(1, DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, -5, 1, 17}, 2, 2));
		try {
			indexes = DatasetUtils.calcIndexesFromPositions(list, shape, null);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
		}

		list.set(1, DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, 34, 1, 17}, 2, 2));
		try {
			indexes = DatasetUtils.calcIndexesFromPositions(list, shape, null);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
		}

		list.set(1, DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, 39, 1, 17}, 2, 2));
		indexes = DatasetUtils.calcIndexesFromPositions(list, shape, 1);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {1, 10, 70, 171}, 2, 2), indexes);

		list.set(1, DatasetFactory.createFromObject(IntegerDataset.class, new int[] {0, -29, 1, 17}, 2, 2));
		indexes = DatasetUtils.calcIndexesFromPositions(list, shape, 1);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {1, 10, 70, 171}, 2, 2), indexes);

		list.set(1, DatasetFactory.createFromObject(IntegerDataset.class, new int[] {-2, 5, 1, 17}, 2, 2));
		indexes = DatasetUtils.calcIndexesFromPositions(list, shape, 2);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {1, 10, 70, 171}, 2, 2), indexes);

		list.set(1, DatasetFactory.createFromObject(IntegerDataset.class, new int[] {34, 5, 1, 17}, 2, 2));
		indexes = DatasetUtils.calcIndexesFromPositions(list, shape, 2);
		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {33*2 + 1, 10, 70, 171}, 2, 2), indexes);
	}

	@Test
	public void testSetByBoolean() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10);
		a.max();
		a.setByBoolean(0, Comparisons.greaterThan(a, 5));
		Assert.assertEquals(a.max().longValue(), 5);
	}

	@Test
	public void testExtract() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 20).reshape(4,5);
		Dataset b = DatasetFactory.createFromObject(new boolean[] {true, false, true, false, false});

		TestUtils.assertDatasetEquals(DatasetFactory.createFromObject(new int[] {0, 2, 5, 7, 10, 12, 15, 17}),
				DatasetUtils.extract(a, b));
	}

	@Test
	public void testSetBy1DIndex() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10);
		a.max();
		a.setBy1DIndex(0, Comparisons.nonZero(Comparisons.greaterThan(a, 5)).get(0));
		Assert.assertEquals(5, a.max().longValue());
	}

	@Test
	public void testSetByPosition() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10);
		Assert.assertEquals(9, a.max().longValue());
		List<IntegerDataset> list = Comparisons.nonZero(Comparisons.greaterThan(a, 5));
		a.setByIndexes(0, list.get(0));
		Assert.assertEquals(5, a.max().longValue());

		a = DatasetFactory.createRange(IntegerDataset.class, 10).reshape(2, 5);
		Assert.assertEquals(9, a.max().longValue());
		list = Comparisons.nonZero(Comparisons.greaterThan(a, 5));
		a.setByIndexes(0, list.get(0), list.get(1));
		Assert.assertEquals(5, a.max().longValue());
	}

	@Test
	public void testSetByNegativePositions() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10);
		a.setByIndexes(0, DatasetFactory.createFromObject(new int[] {1, 5, -8}));
		Assert.assertEquals(0, a.getInt(1));
		Assert.assertEquals(0, a.getInt(5));
		Assert.assertEquals(0, a.getInt(-8));

		a = DatasetFactory.createRange(IntegerDataset.class, 10).reshape(2, 5);
		a.setByIndexes(-2, DatasetFactory.createFromObject(new int[] {1, -1, 0}), DatasetFactory.createFromObject(new int[] {0, 2, -4}));
		Assert.assertEquals(-2, a.getInt(1, 0));
		Assert.assertEquals(-2, a.getInt(-1, 2));
		Assert.assertEquals(-2, a.getInt(0, -4));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetByInvalidPositivePositions() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10).reshape(2, 5);
		a.setByIndexes(-2, DatasetFactory.createFromObject(new int[] {1, -1, 2}), DatasetFactory.createFromObject(new int[] {0, 2, -4}));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetByInvalidPositivePositions2() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10).reshape(2, 5);
		a.setByIndexes(-2, DatasetFactory.createFromObject(new int[] {1, -1, 3}), DatasetFactory.createFromObject(new int[] {0, 2, -4}));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetByInvalidNegativePositions() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 10).reshape(2, 5);
		a.setByIndexes(-2, DatasetFactory.createFromObject(new int[] {1, -1, 0}), DatasetFactory.createFromObject(new int[] {0, 2, -6}));
	}

	@Test
	public void testReshape() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 60);
		Dataset b = a.getSliceView(new int[] {1}, null, new int[] {2});
		Dataset c = a.getSlice(new int[] {1}, null, new int[] {2});
		TestUtils.assertDatasetEquals(c, b);

		// check if strides still work
		b.setShape(6, 5);
		c.setShape(6, 5);
		TestUtils.assertDatasetEquals(c, b);

		b.setShape(1, 6, 5);
		c.setShape(1, 6, 5);
		TestUtils.assertDatasetEquals(c, b);

		b.setShape(1, 6, 1, 5);
		c.setShape(1, 6, 1, 5);
		TestUtils.assertDatasetEquals(c, b);

		b.setShape(30);
		c.setShape(30);
		TestUtils.assertDatasetEquals(c, b);

		b.setShape(6, 5);
		try {
			Dataset d = b.getSliceView(new Slice(1,6,2));
			d.setShape(15);
			Assert.fail("Should have thrown an illegal argument exception");
		} catch (IllegalArgumentException e) {
			// do nothing
		} catch (Exception e) {
			Assert.fail("Should have thrown an illegal argument exception");
		}
	}

	@Test
	public void testDatasetVariance() {
		Random.seed(12345);
		final Dataset image = Maths.multiply(Random.rand(new int[] { 10, 10 }), 1);
		double mean = ((Number) image.mean()).doubleValue();
		Dataset square = Maths.square(Maths.subtract(image, mean));
		double var = ((Number) square.mean()).doubleValue();

		Assert.assertEquals(var, image.variance(true), var * 1.e-15);
	}

	@Test
	public void testBroadcast() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 3);
		Dataset b = checkBroadcast2D(a, false, 2, 3);
		Assert.assertEquals(1, b.getInt(0, 1));
		Assert.assertEquals(1, b.getInt(1, 1));

		a.setShape(3, 1);
		b = checkBroadcast2D(a, true, 3, 4);
		Assert.assertEquals(1, b.getInt(1, 0));
		Assert.assertEquals(1, b.getInt(1, 1));
	}

	private Dataset checkBroadcast2D(Dataset a, boolean broadcastFirstDim, int... broadcastShape) {
		Dataset b = a.getBroadcastView(broadcastShape);
		Assert.assertArrayEquals(broadcastShape, b.getShapeRef());
		int size = ShapeUtils.calcSize(broadcastShape);
		Assert.assertEquals(size, b.getSize());

		IndexIterator it = b.getIterator(true);
		int[] pos = it.getPos();
		int i = 0;
		while (it.hasNext()) {
			i++;
			if (broadcastFirstDim) {
				Assert.assertEquals(a.getInt(pos[0], 0), b.getInt(pos));
				Assert.assertEquals(a.getInt(pos[0], 0), b.getElementLongAbs(it.index));
			} else {
				Assert.assertEquals(a.getInt(pos[1]), b.getInt(pos));
				Assert.assertEquals(a.getInt(pos[1]), b.getElementLongAbs(it.index));
			}
		}
		Assert.assertEquals(size, i);

		return b;
	}

	@Test
	public void testBroadcastSliceView() {
		Dataset a = DatasetFactory.createRange(IntegerDataset.class, 12);
		Dataset b = a.getSliceView(new Slice(5, 8)).getBroadcastView(2, 3);

		Dataset r = DatasetFactory.createRange(IntegerDataset.class, 5, 8, 1).reshape(1, 3);
		Dataset c = DatasetUtils.concatenate(new Dataset[] {r, r}, 0);
		TestUtils.assertDatasetEquals(c, b);

		b = a.getSliceView(new Slice(5, 6)).getBroadcastView(3, 3);
		c = DatasetFactory.zeros(IntegerDataset.class, 3, 3).fill(5);
		TestUtils.assertDatasetEquals(c, b);
	}

	@Test
	public void testFlips() {
		Dataset a = Random.rand(new int[] {3, 5});
		TestUtils.assertDatasetEquals(a.getSlice(new Slice(null, null, -1)), DatasetUtils.flipUpDown(a));

		TestUtils.assertDatasetEquals(a.getSlice((Slice) null, new Slice(null, null, -1)), DatasetUtils.flipLeftRight(a));

		Dataset b = DatasetUtils.rotate90(a, 1);
		TestUtils.assertDatasetEquals(a, DatasetUtils.rotate90(a, 0));
		TestUtils.assertDatasetEquals(b, DatasetUtils.rotate90(a, 1));
		TestUtils.assertDatasetEquals(DatasetUtils.rotate90(b, 1), DatasetUtils.rotate90(a, 2));
		TestUtils.assertDatasetEquals(DatasetUtils.rotate90(b, 2), DatasetUtils.rotate90(a, 3));
		TestUtils.assertDatasetEquals(DatasetUtils.rotate90(a, -1), DatasetUtils.rotate90(a, 3));
		TestUtils.assertDatasetEquals(a, DatasetUtils.rotate90(a, 4));
	}

	@Test
	public void testSerializable() throws IOException, ClassNotFoundException {
		Dataset a = DatasetFactory.createRange(12);
		a.setShape(3,4);
		assertEquals("Max", 11, a.max().doubleValue(), 1e-6);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		ObjectOutputStream ostream = new ObjectOutputStream(out);
		ostream.writeObject(a);
		ostream.close();
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		ObjectInputStream istream = new ObjectInputStream(in);
		Dataset b = Dataset.class.cast(istream.readObject());

		TestUtils.assertDatasetEquals(a, b);
		StatisticsMetadata<?> am = a.getFirstMetadata(StatisticsMetadata.class);
		StatisticsMetadata<?> bm = b.getFirstMetadata(StatisticsMetadata.class);
		assertEquals(am.getMinimum(), bm.getMinimum());
		assertEquals(am.getMaximum(), bm.getMaximum());
	}
}
