/*-
 * Copyright 2015, 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.january.dataset;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.january.IMonitor;
import org.eclipse.january.io.ILazyDynamicLoader;
import org.eclipse.january.io.ILazyLoader;
import org.eclipse.january.metadata.AxesMetadata;
import org.eclipse.january.metadata.DynamicMetadataUtils;
import org.eclipse.january.metadata.MetadataFactory;
import org.junit.Assert;
import org.junit.Test;

public class LazyDynamicLoaderTest {
	
	@Test
	public void testLoader() throws Exception {
		
		Dataset range = DatasetFactory.createRange(LongDataset.class, 10*13*20*20);
		range.setShape(new int[]{10,13,20,20} );
		
		LazyDynamicDataset dataset = getDataset(range,2,"data");
		
		AxesMetadata ax = MetadataFactory.createMetadata(AxesMetadata.class, 4);
		ax.setAxis(0, DatasetFactory.createRange(LongDataset.class, 10));
		ax.setAxis(1, DatasetFactory.createRange(LongDataset.class, 13));
		dataset.addMetadata(ax);
		
		int max = 399;
		
		for (int i = 0 ; i < (10*13); i++) {
			SliceND slice = new SliceND(dataset.getShape());
			int[] shape = dataset.getShape();
			slice.setSlice(0, shape[0]-1, shape[0], 1);
			slice.setSlice(1, shape[1]-1, shape[1], 1);
			IDataset s = dataset.getSlice(slice);
			Assert.assertEquals(max, s.max().intValue());
			max+=400;
			
			AxesMetadata axm = s.getFirstMetadata(AxesMetadata.class);
			
			Assert.assertEquals(axm.getAxis(0)[0].getSlice().getInt(0, 0, 0, 0), shape[0]-1);
			Assert.assertEquals(axm.getAxis(1)[0].getSlice().getInt(0, 0, 0, 0), shape[1]-1);
			
			dataset.refreshShape();
		}
		
		Assert.assertArrayEquals(new int[]{10,13,20,20},dataset.getShape());
		
	}
	
	@Test
	public void testDynamicAxesMetadata() throws Exception {
		
		int[] maxShape = new int[]{10,13,20,20};
		
		Dataset range = DatasetFactory.createRange(LongDataset.class, 10*13*20*20);
		range.setShape(maxShape);
		
		LazyDynamicDataset dataset = getDataset(range,2,"data");
		LazyDynamicDataset ax1 = getDataset(DatasetFactory.createRange(LongDataset.class, 10),0,"ax0");
		LazyDynamicDataset ax2 = getDataset(DatasetFactory.createRange(LongDataset.class, 13),0,"ax1");
		
		AxesMetadata ax = MetadataFactory.createMetadata(AxesMetadata.class, 4);
		ax.setAxis(0, ax1);
		ax.setAxis(1, ax2);
		dataset.addMetadata(ax);
		
		int max = 399;
		
		SliceND slice = new SliceND(dataset.getShape(),maxShape);
		int[] axShape = new int[]{1,1,1,1};
		
		for (int i = 0 ; i < (10*13); i++) {
			int[] shape = dataset.getShape();
			@SuppressWarnings("unused")
			int[] so = DynamicMetadataUtils.refreshDynamicAxesMetadata(dataset.getMetadata(AxesMetadata.class), shape);
			slice.setSlice(0, shape[0]-1, shape[0], 1);
			slice.setSlice(1, shape[1]-1, shape[1], 1);
			IDataset s = dataset.getSlice(slice);
			Assert.assertEquals((long) max, s.max().longValue());
			max+=400;
			
			AxesMetadata axm = s.getFirstMetadata(AxesMetadata.class);

			IDataset s0 = axm.getAxis(0)[0].getSlice();
			Assert.assertArrayEquals(axShape, s0.getShape());
			Assert.assertEquals(s0.getInt(0, 0, 0, 0), shape[0]-1);
			s0 = axm.getAxis(1)[0].getSlice();
			Assert.assertArrayEquals(axShape, s0.getShape());
			Assert.assertEquals(axm.getAxis(1)[0].getSlice().getInt(0, 0, 0, 0), shape[1]-1);

			dataset.refreshShape();
		}
		
		Assert.assertArrayEquals(new int[]{10,13,20,20},dataset.getShape());
		
	}

	private LazyDynamicDataset getDataset(IDataset data, int detectorRank, String name) {
		
		int[] shape = determineInitialShape(data.getShape(), detectorRank);
		return new LazyDynamicDataset(new LazyDynamicTestLoader(data, detectorRank), name, 1, DoubleDataset.class, shape, data.getShape());
	}
	
	private static int[] determineInitialShape(int[] maxShape, int detectorRank) {
		int[] shape = maxShape.clone();
		Arrays.fill(shape, 1);
		for (int i = 0; i < detectorRank; i++) {
			shape[shape.length-i-1] = maxShape[shape.length-i-1];
		}
		
		return shape;
	}
	
	private class LazyDynamicTestLoader implements ILazyLoader,ILazyDynamicLoader {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2336998015537666270L;
		int[] currentShape;
		IDataset data;
		SliceNDIterator iterator;
		boolean hasNext = true;
		private int[] dShape;
		
		LazyDynamicTestLoader(IDataset data, int detectorRank) {
			this.data = data;
			this.dShape = data.getShape();
			this.currentShape = determineInitialShape(dShape, detectorRank);
			int[] axes = new int[detectorRank];
			for (int i = 0; i < detectorRank; i++) axes[i] = data.getRank()-1-i;
			iterator = new SliceNDIterator(new SliceND(dShape), axes);
			iterator.hasNext();
		}
		
		@Override
		public int[] refreshShape() {
			if (!iterator.hasNext()) hasNext = false;
			if (!hasNext) return data.getShape();
			SliceND sliceND = iterator.getCurrentSlice();
			currentShape = sliceND.getStop();
			return currentShape.clone();
		}

		@Override
		public boolean isFileReadable() {
			return true;
		}

		@Override
		public IDataset getDataset(IMonitor mon, SliceND slice) throws IOException {
			SliceND nSlice = slice.clone();
			nSlice.updateSourceShape(dShape);
			return data.getSlice(nSlice);
		}
		
	}
	
}
