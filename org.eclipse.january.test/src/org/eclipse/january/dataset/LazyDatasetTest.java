/*-
 * Copyright (c) 2012, 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.january.dataset;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.january.DatasetException;
import org.eclipse.january.asserts.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class LazyDatasetTest {

	private void setShape(String msg, boolean well, LazyDataset l, int... shape) {
		try {
			l.setShape(shape);
			if (well) {
				TestUtils.verbosePrintf("Succeeded setting shape for %s\n", msg);
			} else {
				Assert.fail("Should have thrown exception for " + msg);
			}
		} catch (IllegalArgumentException iae) {
			if (well) {
				Assert.fail("Unexpected exception for " + msg);
			} else {
				TestUtils.verbosePrintf("Correctly failed setting shape for %s\n", msg);
			}
		} catch (Exception e) {
			msg += ": " + e.getMessage();
			if (well) {
				Assert.fail("Unexpected exception for " + msg);
			} else {
				Assert.fail("Thrown wrong exception for " + msg);
			}
		}
	}

	@Test
	public void testSetShape() {
		LazyDataset ld = new LazyDataset(null, "", IntegerDataset.class, 1, 2, 3, 4);

		setShape("check on same rank", true, ld, 1, 2, 3, 4);
		setShape("check on same rank", false, ld, 1, 2, 3, 5);

		setShape("check on greater rank", true, ld, 1, 1, 1, 2, 3, 4);
		setShape("check on greater rank", false, ld, 1, 2, 2, 3, 5);
		setShape("check on greater rank", false, ld, 2, 1, 2, 3, 4);

		setShape("check on greater rank", true, ld, 2, 3, 4, 1, 1, 1);
		setShape("check on greater rank", true, ld, 1, 1, 2, 3, 4, 1, 1, 1);
		setShape("check on greater rank", false, ld, 2, 3, 4, 5);

		setShape("check on lesser rank", true, ld, 2, 3, 4);
		setShape("check on lesser rank", false, ld, 3, 4);
		setShape("check on lesser rank", false, ld, 2, 3);

		ld = new LazyDataset(null, "", IntegerDataset.class, 2, 3, 4, 1);
		setShape("check on lesser rank", true, ld, 2, 3, 4);

		ld = new LazyDataset(null, "", IntegerDataset.class, 1, 2, 3, 4, 1);
		setShape("check on lesser rank", true, ld, 2, 3, 4);
	}

	@Test
	public void testGetSlice() throws Exception {
		final int[] shape = new int[] {1, 2, 3, 4};
		final Dataset d = Random.randn(shape);
		LazyDataset ld = LazyDataset.createLazyDataset(d);

		Slice[] slice;
		slice = new Slice[]{null, new Slice(1), null, new Slice(1, 3)};
		Assert.assertEquals("Full slice", d, ld.getSlice());
		Assert.assertEquals("Full slice", d, ld.getSlice((Slice) null));
		Assert.assertEquals("Full slice", d, ld.getSlice((Slice) null, null));
		Assert.assertEquals("Full slice", d, ld.getSlice(null, (SliceND) null));
		Assert.assertEquals("Full slice", d, ld.getSlice(null, null, null));
		Assert.assertEquals("Full slice", d, ld.getSlice(null, null, new int[] {1, 1, 1, 1}));
		Assert.assertEquals("Full slice", d, ld.getSlice(new int[4], null, new int[] {1, 1, 1, 1}));
		Assert.assertEquals("Full slice", d, ld.getSlice(new int[4], new int[] { 1, 2, 3, 4 }, new int[] { 1, 1, 1, 1 }));
		Assert.assertEquals("Part slice", d.getSlice(slice), ld.getSlice(slice));

		Dataset nd;
		ld.setShape(1, 1, 1, 2, 3, 4);
		nd = d.getView(true);
		nd.setShape(1, 1, 1, 2, 3, 4);
		Assert.assertEquals("Full slice", nd, ld.getSlice());
		slice = new Slice[]{null, null, null, new Slice(1), null, new Slice(1, 3)};
		Assert.assertEquals("Part slice", nd.getSlice(slice), ld.getSlice(slice));

		ld.setShape(2, 3, 4);
		nd = d.getView(true);
		nd.setShape(2, 3, 4);
		Assert.assertEquals("Full slice", nd, ld.getSlice());
		slice = new Slice[]{new Slice(1), null, new Slice(1, 3)};
		Assert.assertEquals("Part slice", nd.getSlice(slice), ld.getSlice(slice));

		ld.setShape(2, 3, 4, 1, 1, 1);
		nd = d.getView(true);
		nd.setShape(2, 3, 4, 1, 1, 1);
		Assert.assertEquals("Full slice", nd, ld.getSlice());
		slice = new Slice[]{new Slice(1), null, new Slice(1, 3), null, null, null};
		Assert.assertEquals("Part slice", nd.getSlice(slice), ld.getSlice(slice));

		ld.setShape(1, 2, 3, 4, 1, 1, 1);
		nd = d.getView(true);
		nd.setShape(1, 2, 3, 4, 1, 1, 1);
		Assert.assertEquals("Full slice", nd, ld.getSlice());
		slice = new Slice[]{null, new Slice(1), null, new Slice(1, 3), null, null, null};
		Assert.assertEquals("Part slice", nd.getSlice(slice), ld.getSlice(slice));

		// test negative slice
		ld.setShape(shape);
		slice = new Slice[]{new Slice(null, null, -1), null, null, null};
		nd = ld.getSlice(slice);
		Assert.assertEquals("Full negative slice", d.getSlice(slice), nd);

		slice = new Slice[]{null, null, null, new Slice(null, null, -1)};
		nd = ld.getSlice(slice);
		Assert.assertEquals("Full negative slice", d.getSlice(slice), nd);

		// test input SliceND checking
		try {
			ld.getSlice(new SliceND(null));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			ld.getSlice(new SliceND(new int[0]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			ld.getSlice(new SliceND(new int[2]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		nd = ld.getSlice(new SliceND(ld.getShape()));
		Assert.assertEquals("Full slice", d.getSlice(), nd);
	}

	@Test
	public void testGetSliceView() throws Exception {
		final int[] shape = new int[] {6, 2, 4, 1};
		final Dataset d = Random.randn(shape);
		LazyDataset ld = LazyDataset.createLazyDataset(d);

		Slice[] slice;
		slice = new Slice[]{new Slice(1, null, 3), new Slice(1), new Slice(1, 3), null};
		ILazyDataset l = ld.getSliceView(null, shape, null);
		TestUtils.verbosePrintf("%s\n", l.toString());
		Assert.assertEquals("Full slice", d, l.getSlice());
		l = ld.getSliceView(slice);
		TestUtils.verbosePrintf("%s\n", l.toString());
		Assert.assertEquals("Part slice", d.getSlice(slice), l.getSlice());

		l = ld.getSliceView();
		TestUtils.verbosePrintf("%s\n", l.toString());
		Assert.assertEquals("Full slice", d, l.getSlice());
		l = ld.getSliceView(slice);
		TestUtils.verbosePrintf("%s\n", l.toString());
		Assert.assertEquals("Part slice", d.getSlice(slice), l.getSlice());

		l = ld.getSliceView();
		l.squeezeEnds();
		Assert.assertEquals("Full slice", 3, l.getSlice().getRank());

		// test negative slice views
		slice = new Slice[]{new Slice(null, null, -1), null, null, null};
		l = ld.getSliceView(slice);
		Assert.assertEquals("Full negative slice", d.getSlice(slice), l.getSlice());

		slice = new Slice[]{null, null, null, new Slice(null, null, -1)};
		l = ld.getSliceView(slice);
		Assert.assertEquals("Full negative slice", d.getSlice(slice), l.getSlice());

		// test input SliceND checking
		try {
			ld.getSliceView(new SliceND(null));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			ld.getSliceView(new SliceND(new int[0]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		try {
			ld.getSliceView(new SliceND(new int[2]));
			fail();
		} catch (IllegalArgumentException e) {
			System.out.println("As expected: " + e);
		}

		l = ld.getSliceView(new SliceND(ld.getShape()));
		Assert.assertEquals("Full slice", d, l.getSlice());
	}

	@Test
	public void testShape() throws DatasetException {
		Dataset data = Random.rand(new int[] {1, 2, 3, 4});
		data.setName("random");
		LazyDataset ld = LazyDataset.createLazyDataset(data);

		ld.setShape(1, 1, 2, 3, 4, 1);
		Assert.assertArrayEquals(new int[] {1, 1, 2, 3, 4, 1}, ld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), ld.getSlice(), true, 1e-14, 1e-14);

		LazyDataset tld = ld.getSliceView();
		Assert.assertArrayEquals(new int[] {1, 1, 2, 3, 4, 1}, tld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), tld.getSlice(), true, 1e-14, 1e-14);

		ld.setShape(1, 2, 3, 4);
		tld = ld.getSliceView();
		ld.setShape(1, 1, 2, 3, 4, 1);
		Assert.assertArrayEquals(new int[] {1, 1, 2, 3, 4, 1}, ld.getShape());
		Assert.assertArrayEquals(new int[] {1, 2, 3, 4}, tld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), ld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), ld.getSliceView().getSlice(), true, 1e-14, 1e-14);

		tld.setShape(1, 1, 2, 3, 4, 1);
		Assert.assertArrayEquals(new int[] {1, 1, 2, 3, 4, 1}, tld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), tld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), tld.getSliceView().getSlice(), true, 1e-14, 1e-14);

		LazyDataset uld = tld.getSliceView();
		Assert.assertArrayEquals(new int[] {1, 1, 2, 3, 4, 1}, uld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), uld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1), uld.getSliceView().getSlice(), true, 1e-14, 1e-14);

		uld.setShape(2, 3, 4);
		Assert.assertArrayEquals(new int[] {2, 3, 4}, uld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(2, 3, 4), uld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.reshape(2, 3, 4), uld.getSliceView().getSlice(), true, 1e-14, 1e-14);

		Slice[] slice = new Slice[] {null, null, null, new Slice(1, null, 2)};
		ld.setShape(1, 2, 3, 4);
		tld = ld.getSliceView(slice);
		tld.squeezeEnds();
		TestUtils.assertDatasetEquals(data.getSliceView(slice).squeezeEnds(), tld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.getSliceView(slice).squeezeEnds(), tld.getSliceView().getSlice(), true, 1e-14, 1e-14);
	}

	@Test
	public void testTranspose() throws DatasetException {
		Dataset data = Random.rand(new int[] {1, 2, 3, 4});
		data.setName("random");
		LazyDataset ld = LazyDataset.createLazyDataset(data);

		LazyDataset tld = ld.getTransposedView(3, 1, 0, 2);
		Assert.assertEquals(tld.getSize(), ld.getSize());
		Assert.assertArrayEquals(new int[] {4, 2, 1, 3}, tld.getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2), tld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.transpose(3, 1, 0, 2), tld.getSlice(), true, 1e-14, 1e-14);

		LazyDataset uld = tld.getTransposedView(3, 2, 1, 0);
		Assert.assertArrayEquals(new int[] {3, 1, 2, 4}, uld.getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).getTransposedView(3, 2, 1, 0), uld.getSlice(), true, 1e-14, 1e-14);

		Assert.assertArrayEquals(new int[] {3, 1, 2, 4}, ld.getTransposedView(2, 0, 1, 3).getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(2, 0, 1, 3), uld.getSlice(), true, 1e-14, 1e-14);

		// set shape of transposed view
		tld.setShape(1, 4, 2, 1, 3, 1);
		Assert.assertArrayEquals(new int[] {1, 4, 2, 1, 3, 1}, tld.getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).reshape(1, 4, 2, 1, 3, 1), tld.getSlice(), true, 1e-14, 1e-14);

		// get transpose of shaped dataset
		ld.setShape(1, 1, 2, 3, 4, 1);
		tld = ld.getTransposedView(3, 1, 0, 2, 4, 5);
		Assert.assertArrayEquals(new int[] {3, 1, 1, 2, 4, 1}, tld.getShape());
		TestUtils.assertDatasetEquals(data.reshape(1, 1, 2, 3, 4, 1).getTransposedView(3, 1, 0, 2, 4, 5), tld.getSlice(), true, 1e-14, 1e-14);

		// get transpose of shaped sliced dataset
		ld.setShape(1, 2, 3, 4);
		tld = ld.getSliceView(null, null, null, new Slice(1, null, 2));
		TestUtils.assertDatasetEquals(data.getSliceView(null, null, null, new Slice(1, null, 2)), tld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.getSliceView(null, null, null, new Slice(1, null, 2)), tld.getSliceView().getSlice(), true, 1e-14, 1e-14);
		uld = tld.getTransposedView(3, 1, 0, 2);
		Assert.assertEquals(tld.getSize(), uld.getSize());
		Assert.assertArrayEquals(new int[] {2, 2, 1, 3}, uld.getShape());
		TestUtils.assertDatasetEquals(data.getSliceView(null, null, null, new Slice(1, null, 2)).getTransposedView(3, 1, 0, 2), uld.getSlice(), true, 1e-14, 1e-14);

		// slice after transpose
		tld = ld.getTransposedView(3, 1, 0, 2);
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2), tld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2), tld.getSliceView().getSlice(), true, 1e-14, 1e-14);
		LazyDataset vld = tld.getSliceView(new Slice(1, null, 2));
		Assert.assertEquals(12, vld.getSize());
		Assert.assertArrayEquals(new int[] {2, 2, 1, 3}, vld.getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).getSliceView(new Slice(1, null, 2)), vld.getSlice(), true, 1e-14, 1e-14);

		// transpose of reshaped transpose
		tld.setShape(1, 4, 2, 1, 3, 1);
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).reshape(1, 4, 2, 1, 3, 1), tld.getSlice(), true, 1e-14, 1e-14);
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).reshape(1, 4, 2, 1, 3, 1), tld.getSliceView().getSlice(), true, 1e-14, 1e-14);
		uld = tld.getTransposedView();
		Assert.assertArrayEquals(new int[] {1, 3, 1, 2, 4, 1}, uld.getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).reshape(1, 4, 2, 1, 3, 1).getTransposedView(), uld.getSlice(), true, 1e-14, 1e-14);
		uld.setShape(3, 1, 2, 4, 1);
		Assert.assertArrayEquals(new int[] {3, 1, 2, 4, 1}, uld.getShape());
		TestUtils.assertDatasetEquals(data.getTransposedView(3, 1, 0, 2).reshape(1, 4, 2, 1, 3, 1).getTransposedView().reshape(3, 1, 2, 4, 1),
				uld.getSlice(), true, 1e-14, 1e-14);
	}

	@Test
	public void testSlicePadRankSlice() throws DatasetException {
		Dataset data = Random.rand(10);
		data.setName("random");
		LazyDataset ld = LazyDataset.createLazyDataset(data);

		LazyDataset sv = ld.getSliceView(new int[] { 0 }, new int[] { 5 }, null);
		sv.setShape(new int[] { 1, 1, 5 });
		LazyDataset view = sv.getSliceView(new int[] { 0, 0, 0 }, new int[] { 1, 1, 4 }, null);
		TestUtils.assertDatasetEquals(data.getSliceView(new Slice(5)).reshape(1, 1, 5).getSliceView(null, null, new Slice(4)),
				view.getSlice(), true, 1e-14, 1e-14);
	}

	@Test
	public void testMultipleTransposeAndShape() throws DatasetException {
		ILazyDataset d = Random.rand(new int[] {3, 2, 1, 5});
		ILazyDataset l = LazyDataset.createLazyDataset((Dataset) d);
	
		ILazyDataset td = d.getTransposedView(2, 0, 3, 1);
		ILazyDataset tl = l.getTransposedView(2, 0, 3, 1); // shape = 1, 3, 5, 2
		TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(td),
				DatasetUtils.sliceAndConvertLazyDataset(tl));
		
		td.setShape(3, 5, 1, 1, 2);
		tl.setShape(3, 5, 1, 1, 2); // pad = -1, 0, 0, 2, 0
		TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(td),
				DatasetUtils.sliceAndConvertLazyDataset(tl));
	
		ILazyDataset ttd = td.getTransposedView(1, 3, 4, 2, 0);
		ILazyDataset ttl = tl.getTransposedView(1, 3, 4, 2, 0); // shape = 5, 1, 2, 1, 3
		TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(ttd),
				DatasetUtils.sliceAndConvertLazyDataset(ttl));

		ttd.setShape(5, 2, 1, 1, 1, 3, 1, 1, 1);
		ttl.setShape(5, 2, 1, 1, 1, 3, 1, 1, 1);
		TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(ttd),
				DatasetUtils.sliceAndConvertLazyDataset(ttl));

		ttd = ttd.getTransposedView(5, 7, 1, 3, 2, 8, 6, 4, 0);
		ttl = ttl.getTransposedView(5, 7, 1, 3, 2, 8, 6, 4, 0);
		TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(ttd),
				DatasetUtils.sliceAndConvertLazyDataset(ttl));
	}

	@Test
	public void testSqueezedSliceView() throws DatasetException {
		Dataset data = Random.rand(new int[] {2, 10});
		data.setName("random");
		LazyDataset ld = LazyDataset.createLazyDataset(data);

		LazyDataset sv = ld.getSliceView(new Slice(1,2));
		Dataset s = DatasetUtils.sliceAndConvertLazyDataset(sv);
		s.squeezeEnds();
		TestUtils.assertDatasetEquals(data.getSlice(new Slice(1,2)).squeeze(), s);

		sv.squeezeEnds();
		TestUtils.assertDatasetEquals(data.getSlice(new Slice(1,2)).squeeze(), sv.getSlice());
	}

	enum LDOp {
		TRANSPOSE() {
			@Override
			public ILazyDataset operate(ILazyDataset a, int... p) {
				int r = a.getRank();
				int d = r - p.length;
				if (d > 0) {
					p = Arrays.copyOf(p, r);
					for (int i = r - d; i < r; i++) {
						p[i] = i;
					}
				} else if (d < 0) {
					p = Arrays.copyOf(p, r);
					Integer[] q = new Integer[r];
					for (int i = 0; i < r; i++) {
						q[i] = i;
					}
					final int[] fp = p;
					Arrays.sort(q, new Comparator<Integer>() {
						@Override
						public int compare(Integer o1, Integer o2) {
							return Integer.compare(fp[o1], fp[o2]);
						}
						
					});
					for (int i = 0; i < r; i++) {
						p[q[i]] = i;
					}
				}
				return a.getTransposedView(p);
			}
		},
		SHAPE() {
			@Override
			public ILazyDataset operate(ILazyDataset a, int... p) {
				int d = 0;
				for (int i : p) {
					d += i;
				}

				a = a.getSliceView();
				a.setShape(ShapeUtils.padShape(p, d + a.getRank(), a.getShape()));
				return a;
			}
		},
		SHAPEL() { // pad on left
			@Override
			public ILazyDataset operate(ILazyDataset a, int... p) {
				int[] np = new int[a.getRank() + 1];
				int d = p[0];
				np[0] = d;

				a = a.getSliceView();
				a.setShape(ShapeUtils.padShape(np, d + a.getRank(), a.getShape()));
				return a;
			}
		},
		SHAPER() { // pad on right
			@Override
			public ILazyDataset operate(ILazyDataset a, int... p) {
				int[] np = new int[a.getRank() + 1];
				int d = p[0];
				np[a.getRank()] = d;

				a = a.getSliceView();
				a.setShape(ShapeUtils.padShape(np, d + a.getRank(), a.getShape()));
				return a;
			}
		},
		SLICE() { // dimension (can be -ve), stop [or start, stop (and step)]
			@Override
			public ILazyDataset operate(ILazyDataset a, int... p) {
				Slice s = null;
				switch (p.length) {
				case 4:
					s = new Slice(p[1], p[2], p[3]);
					break;
				case 3:
					s = new Slice(p[1], p[2]);
					break;
				case 2:
					s = new Slice(null, p[1]);
					break;
				default:
					break;
				}
				Slice[] ss = new Slice[a.getRank()];
				int[] shape = a.getShape();
				int d = p[0];
				// ensure slice is compatible with dimension
				if (d < 0) {
					d += shape.length;
					while (d >= 0) {
						if (s.getStart() != null) {
							int b = s.getStart();
							if (b > 0 && b >= shape[d]) {
								d--;
								continue;
							}
						}
						if (s.getStop() != null) {
							int e = s.getStop();
							if (e > 0 && e > shape[d]) {
								d--;
								continue;
							}
						}
						break;
					}
				} else {
					while (d < shape.length) {
						if (s.getStart() != null) {
							int b = s.getStart();
							if (b > 0 && b >= shape[d]) {
								d++;
								continue;
							}
						}
						if (s.getStop() != null) {
							int e = s.getStop();
							if (e > 0 && e > shape[d]) {
								d++;
								continue;
							}
						}
						break;
					}
				}
				if (d < 0 || d >= shape.length) {
					return null;
				}

				ss[d] = s;
				return a.getSliceView(ss);
			}
		},
		SQUEEZE() { // pad on left
			@Override
			public ILazyDataset operate(ILazyDataset a, int... p) {
				a = a.getSliceView();
				a.squeezeEnds();
				return a;
			}
		},
		;

		abstract ILazyDataset operate(ILazyDataset a, int... p);
	}

	static class Operator {
		private LDOp op;
		private int[] p;

		public Operator(LDOp op, int... p) {
			this.op = op;
			this.p = p;
		}

		public ILazyDataset apply(ILazyDataset a) {
			return op.operate(a, p);
		}

		@Override
		public String toString() {
			return op.toString() + ": " + Arrays.toString(p);
		}
	}

	@Test
	public void testTPT() throws DatasetException {
		testOps(new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.SHAPE, 2, 0, 0, 0, -1, 0, 1),
				new Operator(LDOp.TRANSPOSE, 1, 4, 6, 0, 5, 3, 2));
	}

	@Test
	public void testTGT() throws DatasetException {
		testOps(new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.SLICE, 4, -1, 0, -2),
				new Operator(LDOp.TRANSPOSE, 1, 4, 0, 3, 2));
	}

	@Test
	public void testTSG() throws DatasetException {
		testOps(new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.SHAPEL, 3),
				new Operator(LDOp.SLICE, 3, 3, 0, -2));
	}

	@Test
	public void testTTSG() throws DatasetException {
		testOps(new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.TRANSPOSE, 1, 2, 3, 4, 0),
				new Operator(LDOp.SHAPEL, 1),
				new Operator(LDOp.SLICE, 0, -1, 0, -1));
	}

	@Test
	public void testTTQT() throws DatasetException {
		testOps(new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.SQUEEZE),
				new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3)
				);
	}

	@Test
	public void testSTQT() throws DatasetException {
		testOps(new Operator(LDOp.SHAPEL, 2),
				new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3),
				new Operator(LDOp.SQUEEZE),
				new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3)
				);
	}

	@Test
	public void testSGST() throws DatasetException {
		testOps(new Operator(LDOp.SHAPEL, 2),
				new Operator(LDOp.SLICE, 0, -1, 0, -1),
				new Operator(LDOp.SHAPEL, 2),
				new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3)
				);
	}

	@Test
	public void testS() throws DatasetException {
		testOps(
				new Operator(LDOp.SLICE, 0, 1),
				new Operator(LDOp.SQUEEZE),
				new Operator(LDOp.TRANSPOSE, 2, 3, 4, 1, 0),
				new Operator(LDOp.TRANSPOSE, 4, 1, 0, 2, 3)
				);
	}

	private void testOps(Operator... ops) throws DatasetException {
		ILazyDataset d = Random.rand(new int[] {2, 3, 1, 4, 5});
		d.setName("random");
		ILazyDataset l = LazyDataset.createLazyDataset((Dataset) d);

		for (Operator o : ops) {
			System.out.println(o + ": " + d);
			d = o.apply(d);
			if (d == null) {
				break;
			}
			System.out.println("\t\t\t -> " + d);
			l = o.apply(l);
			System.out.println("\t\t\t -> " + l);
			TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(d),
					DatasetUtils.sliceAndConvertLazyDataset(l));
		}
	}

	private static Operator createOperator(LDOp op, int[] pShapeL, int[] pShapeR, int[] pSlice, int[] pTranspose) {
		switch (op) {
		case SHAPEL:
			return new Operator(op, pShapeL);
		case SHAPER:
			return new Operator(op, pShapeR);
		case SLICE:
			return new Operator(op, pSlice);
		case SQUEEZE:
			return new Operator(op);
		case TRANSPOSE:
			return new Operator(op, pTranspose);
		default:
			return new Operator(op, null);
		}
	}

	@Test
	public void testAll() throws DatasetException {
		final int[][][] pAll = new int[][][] {
			{{2}, {3}, {2,1}, {4,1,0,2,3}},
			{{3}, {1}, {-2,1,-1,2}, {1,2,3,4,0}},
			{{1}, {2}, {0,1}, {2,3,4,1,0}},
			{{4}, {2}, {0,-1,0,-1}, {3,0,2,4,1}}
		};

		int n = pAll.length;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				for (int k = 0; k < n; k++) {
					for (int l = 0; l < n; l++) {
						testAll(pAll[i], pAll[j], pAll[k], pAll[l]);
					}
				}
			}
		}
	}

	private void testAll(final int[][] p1, final int[][] p2, final int[][] p3, final int[][] p4)
			throws DatasetException {
		Dataset d0 = Random.rand(new int[] {2, 3, 1, 4, 5});
		d0.setName("random");
		LazyDataset l0 = LazyDataset.createLazyDataset(d0);

		for (LDOp op1 : LDOp.values()) {
			if (op1 == LDOp.SHAPE) {
				continue;
			}
			Operator o1 = createOperator(op1, p1[0], p1[1], p1[2], p1[3]);
			TestUtils.verbosePrintln(o1.toString());
			ILazyDataset d1 = o1.apply(d0);
			ILazyDataset l1 = o1.apply(l0);
			TestUtils.verbosePrintln("\t\t -> " + Arrays.toString(l1.getShape()));

			TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(d1),
					DatasetUtils.sliceAndConvertLazyDataset(l1));

			for (LDOp op2 : LDOp.values()) {
				if (op2 == LDOp.SHAPE) {
					continue;
				}
				Operator o2 = createOperator(op2, p2[0], p2[1], p2[2], p2[3]);
				TestUtils.verbosePrintln("\t" + o2);
				ILazyDataset d2 = o2.apply(d1);
				ILazyDataset l2 = o2.apply(l1);
				TestUtils.verbosePrintln("\t\t\t -> " + Arrays.toString(l2.getShape()));

				TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(d2),
						DatasetUtils.sliceAndConvertLazyDataset(l2));

				for (LDOp op3 : LDOp.values()) {
					if (op3 == LDOp.SHAPE) {
						continue;
					}
					Operator o3 = createOperator(op3, p3[0], p3[1], p3[2], p3[3]);
					TestUtils.verbosePrintln("\t\t" + o3);
					ILazyDataset d3 = o3.apply(d2);
					ILazyDataset l3 = o3.apply(l2);
					TestUtils.verbosePrintln("\t\t\t\t -> " + Arrays.toString(l3.getShape()));

					TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(d3),
							DatasetUtils.sliceAndConvertLazyDataset(l3));

					for (LDOp op4 : LDOp.values()) {
						if (op4 == LDOp.SHAPE) {
							continue;
						}
						Operator o4 = createOperator(op4, p4[0], p4[1], p4[2], p4[3]);
						TestUtils.verbosePrintln("\t\t\t" + o4);
						ILazyDataset d4 = o4.apply(d3);
						if (d4 == null) {
							continue;
						}
						ILazyDataset l4 = o4.apply(l3);
						TestUtils.verbosePrintln("\t\t\t\t -> " + Arrays.toString(l4.getShape()));

						TestUtils.assertDatasetEquals(DatasetUtils.sliceAndConvertLazyDataset(d4),
								DatasetUtils.sliceAndConvertLazyDataset(l4));
					}
				}
			}
		}
	}
}
