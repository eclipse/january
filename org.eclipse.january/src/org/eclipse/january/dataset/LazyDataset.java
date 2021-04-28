/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Chang - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.january.dataset;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.january.DatasetException;
import org.eclipse.january.IMonitor;
import org.eclipse.january.io.ILazyLoader;
import org.eclipse.january.metadata.MetadataFactory;
import org.eclipse.january.metadata.MetadataType;
import org.eclipse.january.metadata.OriginMetadata;
import org.eclipse.january.metadata.Reshapeable;
import org.eclipse.january.metadata.Sliceable;
import org.eclipse.january.metadata.Transposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyDataset extends LazyDatasetBase implements Serializable, Cloneable {
	private static final long serialVersionUID = 2467865859867440242L;

	private static final Logger logger = LoggerFactory.getLogger(LazyDataset.class);

	protected Map<Class<? extends MetadataType>, List<MetadataType>> oMetadata = null;
	protected int[] oShape; // original shape
	protected long  size;   // number of items
	private Class<? extends Dataset> clazz = null;
	protected int   isize;  // number of elements per item

	protected ILazyLoader loader;

	// relative to loader
	protected int[] begSlice = null; // slice begin
	protected int[] delSlice = null; // slice delta
	/**
	 * @since 2.2
	 */
	protected int[] sShape   = null; // sliced shape

	/**
	 * @since 2.2
	 */
	protected int[] padding = null; // differences in shape from original (or sliced) shape
	protected int[] map; // transposition map (same length as current shape)

	/**
	 * Create a lazy dataset
	 * @param loader
	 * @param name
	 * @param elements
	 * @param clazz dataset interface (can be null to represent undefined interface)
	 * @param shape
	 * @since 2.3
	 */
	public LazyDataset(ILazyLoader loader, String name, int elements, Class<? extends Dataset> clazz, int... shape) {
		this.loader = loader;
		this.name = name;
		this.isize = elements;
		this.clazz = clazz;
		this.shape = shape.clone();
		this.oShape = this.shape;
		try {
			size = ShapeUtils.calcLongSize(shape);
		} catch (IllegalArgumentException e) {
			size = Long.MAX_VALUE; // this indicates that the entire dataset cannot be read in! 
		}
	}

	/**
	 * Create a lazy dataset
	 * @param loader
	 * @param name
	 * @param clazz dataset interface (can be null to represent undefined interface)
	 * @param shape
	 * @since 2.3
	 */
	public LazyDataset(ILazyLoader loader, String name, Class<? extends Dataset> clazz, int... shape) {
		this(loader, name, 1, clazz, shape);
	}

	/**
	 * Create a lazy dataset
	 * @param name
	 * @param dtype dataset type
	 * @param elements
	 * @param shape
	 * @param loader
	 * @deprecated Use {@link #LazyDataset(ILazyLoader, String, int, Class, int[])}
	 */
	@Deprecated
	public LazyDataset(String name, int dtype, int elements, int[] shape, ILazyLoader loader) {
		this(loader, name, elements, DTypeUtils.getInterface(dtype), shape);
	}

	/**
	 * Create a lazy dataset
	 * @param name
	 * @param dtype dataset type
	 * @param shape
	 * @param loader
	 * @deprecated Use {@link #LazyDataset(ILazyLoader, String, int, Class, int[])}
	 */
	@Deprecated
	public LazyDataset(String name, int dtype, int[] shape, ILazyLoader loader) {
		this(name, dtype, 1, shape, loader);
	}

	LazyDataset(LazyDataset other) {
		name  = other.name;
		shape = other.shape.clone();
		metadata  = other.copyMetadata();
		oMetadata = other.oMetadata;
		oShape = other.oShape;
		size   = other.size;
		clazz  = other.clazz;
		isize  = other.isize;
		loader = other.loader;

		begSlice = other.begSlice;
		delSlice = other.delSlice;
		sShape   = other.sShape;
		padding  = other.padding;
		map      = other.map;
	}

	/**
	 * Create a lazy dataset based on in-memory data (handy for testing)
	 * @param dataset
	 */
	public static LazyDataset createLazyDataset(final Dataset dataset) {
		return new LazyDataset(new ILazyLoader() {
			private static final long serialVersionUID = -6725268922780517523L;

			final Dataset d = dataset;

			@Override
			public boolean isFileReadable() {
				return true;
			}

			@Override
			public Dataset getDataset(IMonitor mon, SliceND slice) throws IOException {
				return d.getSlice(mon, slice);
			}
		}, dataset.getName(), dataset.getElementsPerItem(), dataset.getClass(), dataset.getShapeRef());
	}

	@Override
	public Class<?> getElementClass() {
		return InterfaceUtils.getElementClass(clazz);
	}

	/**
	 * Can return -1 when dataset interface is not defined
	 */
	@Override
	public int getDType() {
		return clazz == null ? -1 : DTypeUtils.getDType(clazz);
	}

	/**
	 * @return dataset interface that supports element class and number of elements. Can be null when undefined
	 * @since 2.3
	 */
	public Class<? extends Dataset> getInterface() {
		return clazz;
	}

	/**
	 * Set interface
	 * @param clazz
	 * @since 2.3
	 */
	public void setInterface(Class<? extends Dataset> clazz) {
		this.clazz = clazz;
	}

	/**
	 * Can return -1 for unknown
	 */
	@Override
	public int getElementsPerItem() {
		return isize;
	}

	@Override
	public int getSize() {
		return (int) size;
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();

		if (name != null && name.length() > 0) {
			out.append("Lazy dataset '");
			out.append(name);
			out.append("' has shape [");
		} else {
			out.append("Lazy dataset shape is [");
		}
		int rank = shape == null ? 0 : shape.length;

		if (rank > 0 && shape[0] >= 0) {
			out.append(shape[0]);
		}
		for (int i = 1; i < rank; i++) {
			out.append(", " + shape[i]);
		}
		out.append(']');

		return out.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(oShape);
		result = prime * result + (int) (size ^ (size >>> 32));
		result = prime * result + Objects.hashCode(clazz);
		result = prime * result + isize;
		result = prime * result + Objects.hashCode(loader);
		result = prime * result + Arrays.hashCode(begSlice);
		result = prime * result + Arrays.hashCode(delSlice);
		result = prime * result + Arrays.hashCode(sShape);
		result = prime * result + Arrays.hashCode(padding);
		result = prime * result + Arrays.hashCode(map);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}

		LazyDataset other = (LazyDataset) obj;
		if (!Arrays.equals(oShape, other.oShape)) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		if (!Objects.equals(clazz, other.clazz)) {
			return false;
		}
		if (isize != other.isize) {
			return false;
		}

		if (loader != other.loader) {
			return false;
		}

		if (!Arrays.equals(begSlice, other.begSlice)) {
			return false;
		}
		if (!Arrays.equals(delSlice, other.delSlice)) {
			return false;
		}
		if (!Arrays.equals(sShape, other.sShape)) {
			return false;
		}
		if (!Arrays.equals(padding, other.padding)) {
			return false;
		}
		if (!Arrays.equals(map, other.map)) {
			return false;
		}

		return true;
	}

	@Override
	public LazyDataset clone() {
		return new LazyDataset(this);
	}

	@Override
	public void setShape(int... shape) {
		setShapeInternal(shape.clone());
	}

	@Override
	public LazyDataset squeezeEnds() {
		setShapeInternal(ShapeUtils.squeezeShape(shape, true));
		return this;
	}

	@Override
	public Dataset getSlice(int[] start, int[] stop, int[] step) throws DatasetException {
		return getSlice(null, start, stop, step);
	}

	@Override
	public Dataset getSlice(Slice... slice) throws DatasetException {
		if (slice == null || slice.length == 0) {
			return internalGetSlice(null, new SliceND(shape));
		}
		return internalGetSlice(null, new SliceND(shape, slice));
	}

	@Override
	public Dataset getSlice(SliceND slice) throws DatasetException {
		return getSlice(null, slice);
	}

	@Override
	public Dataset getSlice(IMonitor monitor, Slice... slice) throws DatasetException {
		if (slice == null || slice.length == 0) {
			return internalGetSlice(monitor, new SliceND(shape));
		}
		return internalGetSlice(monitor, new SliceND(shape, slice));
	}

	@Override
	public LazyDataset getSliceView(Slice... slice) {
		if (slice == null || slice.length == 0) {
			return getSliceView(new SliceND(shape));
		}
		return getSliceView(new SliceND(shape, slice));
	}

	/**
	 * @param nShape
	 */
	private void setShapeInternal(int... nShape) {
		// work out transposed (sliced) shape (instead of removing padding from current shape)
		if (size != 0) {
			int[] pShape = calcTransposed(map, sShape == null ? oShape : sShape);
			padding = ShapeUtils.calcShapePadding(pShape, nShape);
		}

		if (metadata != null) {
			storeMetadata(metadata, Reshapeable.class);
			metadata = copyMetadata();
			reshapeMetadata(shape, nShape);
		}
		shape = nShape;
	}

	@Override
	public LazyDataset getSliceView(int[] start, int[] stop, int[] step) {
		return internalGetSliceView(new SliceND(shape, start, stop, step));
	}

	@Override
	public LazyDataset getSliceView(SliceND slice) {
		checkSliceND(slice);
		return internalGetSliceView(slice);
	}

	protected LazyDataset internalGetSliceView(SliceND slice) {
		LazyDataset view = clone();
		if (slice.isAll()) {
			return view;
		}

		SliceND nslice = calcTrueSlice(slice);
		if (nslice != null) {
			view.begSlice = nslice.getStart();
			view.delSlice = nslice.getStep();
			view.sShape = nslice.getShape();
		}
		view.shape = slice.getShape();
		view.size = ShapeUtils.calcLongSize(view.shape);
		view.storeMetadata(metadata, Sliceable.class);

		view.sliceMetadata(true, slice);
		return view;
	}

	@Override
	public Dataset getSlice(IMonitor monitor, int[] start, int[] stop, int[] step) throws DatasetException {
		return internalGetSlice(monitor, new SliceND(shape, start, stop, step));
	}

	@Override
	public Dataset getSlice(IMonitor monitor, SliceND slice) throws DatasetException {
		checkSliceND(slice);

		return internalGetSlice(monitor, slice);
	}

	protected Dataset internalGetSlice(IMonitor monitor, SliceND slice) throws DatasetException {
		if (loader != null && !loader.isFileReadable()) {
			return null;
		}

		SliceND nslice = calcTrueSlice(slice);

		Dataset a;
		if (nslice == null) {
			a = DatasetFactory.zeros(clazz == null ? DoubleDataset.class : clazz, slice.getShape());
		} else {
			try {
				a = DatasetUtils.convertToDataset(loader.getDataset(monitor, nslice));
			} catch (IOException e) {
				logger.error("Problem getting {}: {}", String.format("slice %s %s %s from %s", Arrays.toString(slice.getStart()), Arrays.toString(slice.getStop()),
								Arrays.toString(slice.getStep()), loader), e);
				throw new DatasetException(e);
			}
		}
		a.setName(name + AbstractDataset.BLOCK_OPEN + (nslice == null ? slice : nslice) + AbstractDataset.BLOCK_CLOSE);
		if (metadata != null && a instanceof LazyDatasetBase) {
			LazyDatasetBase ba = (LazyDatasetBase) a;
			ba.metadata = copyMetadata();
			if (oMetadata != null) {
				ba.restoreMetadata(oMetadata);
			}
			// metadata axis may be larger than data
			if (nslice != null && (!nslice.isAll() || nslice.getMaxShape() != nslice.getShape())) {
				ba.sliceMetadata(true, nslice);
			}
		}

		if (nslice != null) {
			if (map != null) {
				a = a.getTransposedView(map);
			}
			if (padding != null) {
				a.setShape(slice.getShape());
			}
		}
		a.addMetadata(MetadataFactory.createMetadata(OriginMetadata.class, this, nslice == null ? slice.convertToSlice() : nslice.convertToSlice(), oShape, null, name));

		if (clazz == null) {
			clazz = a.getClass();
		}
		return a;
	}

	@Override
	public LazyDataset getTransposedView(final int... axes) {
		LazyDataset view = clone();

		int[] naxes = checkPermutatedAxes(shape, axes);
		if (naxes == null) {
			return view;
		}

		view.shape = calcTransposed(naxes, shape);
		if (view.size != 0 && padding != null) { // work out transpose by reverting effect of padding
			int or = oShape.length;
			int nr = shape.length;
			int j = 0; // naxes index
			int[] mShape = calcTransposed(map, sShape == null ? oShape : sShape); // pre-padded shape
			int m = 0; // shape index
			int e = -1; // index of unit dimension
			final List<Integer> uaxes = new LinkedList<>();
			for (int a : naxes) {
				uaxes.add(a);
			}
			List<Integer> oList = new ArrayList<>(); // dimensions left out by padding (in order)
			int np = padding.length;
			for (int i = 0; i < np; i++) {
				int p = padding[i];
				if (p > 0) { // remove added dimensions
					for (int k = 0; k < p; k++, j++) {
						uaxes.remove((Integer) j);
					}
				} else if (p == 0) { // leave alone
					if (mShape[m] == 1) { // bump up last unit dimension index
						e = m;
					}
					j++;
					m++;
				} else { // add omitted dimensions to list
					p = -p;
					for (int k = 0; k < p; k++) {
						e = find(mShape, 1, e + 1);
						oList.add(e);
					}
				}
			}
			
			int[] omitted = new int[oList.size()];
			j = 0;
			for (Integer o : oList) {
				omitted[j++] = o;
			}
			int[] used = new int[or - omitted.length]; // all dimensions not omitted in pre-padded shape
			j = 0;
			for (int i = 0; i < or; i++) {
				if (Arrays.binarySearch(omitted, i) < 0) {
					used[j++] = i;
				}
			}

			int[] vaxes = new int[uaxes.size()];
			j = 0;
			for (int i = 0; i < nr; i++) { // remap dimension numbering
				int l = uaxes.indexOf(i);
				if (l >= 0) {
					vaxes[l] = used[j++];
				}
			}
			int[] taxes = new int[or];
			j = 0;
			for (int i = 0; i < or; i++) { // reassemble map
				if (Arrays.binarySearch(omitted, i) >= 0) {
					taxes[i] = i;
				} else {
					taxes[i] = vaxes[j++];
				}
			}

			naxes = taxes;
		}

		view.map = map == null ? naxes : calcTransposed(naxes, map);
		if (view.size != 0) {
			// work out transposed (sliced) shape
			int[] tShape = calcTransposed(view.map, sShape == null ? oShape : sShape);
			try {
				view.padding = ShapeUtils.calcShapePadding(tShape, view.shape);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage() + ": " + Arrays.toString(tShape) + " cf " + Arrays.toString(view.shape));
			}
		}
		view.storeMetadata(metadata, Transposable.class);
		view.transposeMetadata(axes);
		return view;
	}

	private static int find(int[] map, int m, int off) {
		for (int i = off, imax = map.length; i < imax; i++) {
			if (map[i] == m) {
				return i;
			}
		}
		return -1;
	}

	private static int[] calcTransposed(int[] map, int[] values) {
		if (values == null) {
			return null;
		}
		int r = values.length;
		if (map == null || r < 2) {
			return values;
		}
		int[] ovalues = new int[r];
		for (int i = 0; i < r; i++) {
			ovalues[i] = values[map[i]];
		}
		return ovalues;
	}

	/**
	 * Calculate absolute slice
	 * @param slice
	 * @return true slice or null if zero-sized
	 */
	protected final SliceND calcTrueSlice(SliceND slice) {
		/*
		 * Lazy dataset operations: getTransposedView (T), getSliceView (G), setShape/squeezeEnds (S+/S-):
		 * 
		 *     . T sets shape, base, and map in new view
		 *     . G sets shape, size, begSlice and delSlice in new view
		 *     . S sets shape, shapePadding in current view
		 * 
		 * Then getSlice needs to interpret all info to find true slice, load data, get transposition (view)
		 * and set shape. Therefore:
		 *     . S needs to update shapePadding only
		 *     . T needs to update shapePadding too
		 *     . G needs to work out true slice to update
		 * 
		 * slice -> true slice
		 *   adjusts for shape (S^-1) then remap dimensions (T^-1)
		 */

		if (slice == null) {
			slice = new SliceND(shape);
		}

		if (ShapeUtils.calcLongSize(slice.getShape()) == 0) {
			return null;
		}

		int[] nshape;
		int[] nstart;
		int[] nstep;

		int r = oShape.length;
		if (padding == null) {
			nshape = slice.getShape();
			nstart = slice.getStart();
			nstep = slice.getStep();
		} else {
			final int[] lshape = slice.getShape();
			final int[] lstart = slice.getStart();
			final int[] lstep  = slice.getStep();

			nstart = new int[r];
			nstep = new int[r];
			nshape = new int[r];
			int i = 0;
			int j = 0;
			for (int p : padding) { // remove padding
				if (p == 0) {
					nshape[i] = lshape[j];
					nstart[i] = lstart[j];
					nstep[i]  = lstep[j];
					i++;
					j++;
				} else if (p < 0) {
					int imax = i - p;
					while (i < imax) {
						nshape[i] = 1;
						nstep[i]  = 1;
						i++;
					}
				} else {
					j += p;
				}
			}
		}

		if (map != null && r > 1) { // transpose dimensions
			int[] pshape = new int[r];
			int[] pstart = new int[r];
			int[] pstep = new int[r];
			for (int i = 0; i < r; i++) {
				int m = map[i];
				pshape[m] = nshape[i];
				pstart[m] = nstart[i];
				pstep[m]  = nstep[i];
			}

			nshape = pshape;
			nstart = pstart;
			nstep  = pstep;
		}

		int[] nstop = new int[r];
		if (begSlice != null) { // find net slice
			for (int i = 0; i < r; i++) {
				int b = begSlice[i];
				int d = delSlice[i];
				nstart[i] = b + nstart[i] * d;
				int nd = nstep[i] * d;
				nstep[i] = nd;
				nstop[i]  = nstart[i] + (nshape[i] - 1) * nd + (nd >= 0 ? 1 : -1);
			}
		} else {
			for (int i = 0; i < r; i++) {
				int d = nstep[i];
				nstop[i] = nstart[i] + (nshape[i] - 1) * d + (d >= 0 ? 1 : -1);
			}
		}

		return createSlice(nstart, nstop, nstep);
	}

	protected SliceND createSlice(int[] nstart, int[] nstop, int[] nstep) {
		return SliceND.createSlice(oShape, null, nstart, nstop, nstep);
	}

	/**
	 * Transform data so that it can be used in setSlice of saver
	 * @param data
	 * @param tslice true slice 
	 * @return data with dimensions adjusted and remapped 
	 */
	final IDataset transformInput(IDataset data, SliceND tslice) {
		if (padding != null) { // remove padding
			data = data.getSliceView();
			int[] nshape = tslice.getShape();
			data.setShape(nshape);
		}

		return map == null ? data : data.getTransposedView(map);
	}

	/**
	 * Store metadata items that has given annotation
	 * @param origMetadata
	 * @param aclazz
	 */
	private void storeMetadata(Map<Class<? extends MetadataType>, List<MetadataType>> origMetadata, Class<? extends Annotation> aclazz) {
		List<Class<? extends MetadataType>> mclazzes = findAnnotatedMetadata(aclazz);
		if (mclazzes.size() == 0) {
			return;
		}

		if (oMetadata == null) {
			oMetadata = new HashMap<Class<? extends MetadataType>, List<MetadataType>>();
		}
		for (Class<? extends MetadataType> mc : mclazzes) {
			if (oMetadata.containsKey(mc)) {
				continue; // do not overwrite original
			}

			List<MetadataType> l = origMetadata.get(mc);
			List<MetadataType> nl = new ArrayList<MetadataType>(l.size());
			for (MetadataType m : l) {
				nl.add(m.clone());
			}
			oMetadata.put(mc, nl);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Class<? extends MetadataType>> findAnnotatedMetadata(Class<? extends Annotation> aclazz) {
		List<Class<? extends MetadataType>> mclazzes = new ArrayList<Class<? extends MetadataType>>();
		if (metadata == null) {
			return mclazzes;
		}

		for (Class<? extends MetadataType> c : metadata.keySet()) {
			boolean hasAnn = false;
			for (MetadataType m : metadata.get(c)) {
				if (m == null) {
					continue;
				}

				Class<? extends MetadataType> mc = m.getClass();
				do { // iterate over super-classes
					for (Field f : mc.getDeclaredFields()) {
						if (f.isAnnotationPresent(aclazz)) {
							hasAnn = true;
							break;
						}
					}
					Class<?> sclazz = mc.getSuperclass();
					if (!MetadataType.class.isAssignableFrom(sclazz)) {
						break;
					}
					mc = (Class<? extends MetadataType>) sclazz;
				} while (!hasAnn);
				if (hasAnn) {
					break;
				}
			}
			if (hasAnn) {
				mclazzes.add(c);
			}
		}
		return mclazzes;
	}

	/**
	 * Gets the maximum size of a slice of a dataset in a given dimension
	 * which should normally fit in memory. Note that it might be possible
	 * to get more in memory, this is a conservative estimate and seems to
	 * almost always work at the size returned; providing Xmx is less than
	 * the physical memory.
	 * 
	 * To get more in memory increase -Xmx setting or use an expression
	 * which calls a rolling function (like rmean) instead of slicing directly
	 * to memory.
	 * 
	 * @param lazySet
	 * @param dimension
	 * @return maximum size of dimension that can be sliced.
	 */
	public static int getMaxSliceLength(ILazyDataset lazySet, int dimension) {
		// size in bytes of each item
		final double size = InterfaceUtils.getItemBytes(lazySet.getElementsPerItem(), InterfaceUtils.getInterface(lazySet));
		
		// Max in bytes takes into account our minimum requirement
		final double max  = Math.max(Runtime.getRuntime().totalMemory(), Runtime.getRuntime().maxMemory());
		
		// Firstly if the whole dataset it likely to fit in memory, then we allow it.
		// Space specified in bytes per item available
		final double space = max/lazySet.getSize();

		// If we have room for this whole dataset, then fine
		int[] shape = lazySet.getShape();
		if (space >= size) {
			return shape[dimension];
		}

		// Otherwise estimate what we can fit in, conservatively.
		// First get size of one slice, see it that fits, if not, still return 1
		double sizeOneSlice = size; // in bytes
		for (int dim = 0; dim < shape.length; dim++) {
			if (dim == dimension) {
				continue;
			}
			sizeOneSlice *= shape[dim];
		}
		double avail = max / sizeOneSlice;
		if (avail < 1) {
			return 1;
		}

		// We fudge this to leave some room
		return (int) Math.floor(avail/4d);
	}
}
