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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.Format;
import java.util.Arrays;
import java.util.List;

import org.eclipse.january.DatasetException;
import org.eclipse.january.IMonitor;
import org.eclipse.january.MetadataException;
import org.eclipse.january.metadata.ErrorMetadata;
import org.eclipse.january.metadata.MetadataFactory;
import org.eclipse.january.metadata.StatisticsMetadata;
import org.eclipse.january.metadata.internal.ErrorMetadataImpl;
import org.eclipse.january.metadata.internal.StatisticsMetadataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic container class for data 
 * <p>
 * Each subclass has an array of primitive types, elements of this array are grouped or
 * compounded to make items 
 * <p>
 * Data items can be boolean, integer, float, complex float, vector float, etc
 */
public abstract class AbstractDataset extends LazyDatasetBase implements Dataset {
	// pin UID to base class
	private static final long serialVersionUID = Dataset.serialVersionUID;

	private static final Logger logger = LoggerFactory.getLogger(AbstractDataset.class);

	protected int size; // number of items

	transient protected AbstractDataset base; // is null when not a view
	protected int[] stride; // can be null for row-major, contiguous datasets
	protected int offset;

	/**
	 * The data itself, held in a 1D array, but the object will wrap it to appear as possessing as many dimensions as
	 * wanted
	 */
	protected Serializable odata = null;

	/**
	 * Set aliased data as base data
	 */
	abstract protected void setData();

	/**
	 * Constructor required for serialisation.
	 */
	public AbstractDataset() {
	}

	@Override
	public synchronized Dataset synchronizedCopy() {
		return clone();
	}

	@Override
	public Class<?> getElementClass() {
		return InterfaceUtils.getElementClass(getClass());
	}

	@Override
	public int getDType() {
		return DTypeUtils.getDType(getClass());
	}

	@Override
	public int hashCode() {
		return getStats().getHash(shape);
	}

	@Override
	abstract public AbstractDataset clone();

	protected Format stringFormat = null;

	@Override
	public void setStringFormat(Format format) {
		stringFormat = format;
	}

	@Override
	public Dataset copy(final int dtype) {
		return copy(DTypeUtils.getInterface(dtype));
	}

	@Override
	public <T extends Dataset> T copy(Class<T> clazz) {
		return DatasetUtils.copy(clazz, this);
	}

	@Override
	public Dataset cast(final int dtype) {
		return cast(DTypeUtils.getInterface(dtype));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Dataset> T cast(Class<T> clazz) {
		if (clazz.isInstance(this)) {
			return (T) this;
		}
		return DatasetUtils.cast(clazz, this);
	}

	/**
	 * {@inheritDoc Dataset#cast(int, Class, boolean)}
	 * @since 2.3
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Dataset> T cast(final int isize, Class<T> clazz, final boolean repeat) {
		if (clazz.isInstance(this) && getElementsPerItem() == isize) {
			return (T) this;
		}
		return DatasetUtils.cast(isize, clazz, this, repeat);
	}

	@Override
	public Dataset cast(final boolean repeat, final int dtype, final int isize) {
		return cast(isize, DTypeUtils.getInterface(dtype), repeat);
	}

	@Override
	abstract public AbstractDataset getView(boolean deepCopyMetadata);

	/**
	 * Copy fields from original to view
	 * @param orig
	 * @param view
	 * @param clone if true, then clone everything but bulk data
	 * @param cloneMetadata if true, clone metadata
	 */
	protected static void copyToView(Dataset orig, AbstractDataset view, boolean clone, boolean cloneMetadata) {
		view.name = orig.getName();
		view.size = orig.getSize();
		view.odata = orig.getBuffer();
		view.offset = orig.getOffset();
		AbstractDataset a = orig instanceof AbstractDataset ? (AbstractDataset) orig : null;
		view.base = a == null ? null : a.base;

		int[] s = a == null ? null : a.stride;
		if (clone) {
			view.shape = orig.getShapeRef() == null ? null : orig.getShape();
			view.stride = s == null ? null : s.clone();
		} else {
			view.shape = orig.getShapeRef();
			view.stride = s;
		}

		view.metadata = getMetadataMap(orig, cloneMetadata);
		Class<? extends Dataset> oc = InterfaceUtils.findSubInterface(orig.getClass());
		Class<? extends Dataset> vc = InterfaceUtils.findSubInterface(view.getClass());
		if (!oc.equals(vc)) {
			view.setDirty();
		}
	}

	@Override
	public IntegerDataset getIndices() {
		final IntegerDataset ret = DatasetUtils.indices(shape);
		if (getName() != null) {
			ret.setName("Indices of " + getName());
		}
		return ret;
	}

	@Override
	public Dataset getTransposedView(int... axes) {
		axes = checkPermutatedAxes(shape, axes);

		AbstractDataset t = getView(true);
		if (axes == null || getRank() == 1)
			return t;

		int rank = shape.length;
		int[] tstride = new int[rank];
		int[] toffset = new int[1];
		int[] nshape = createStrides(new SliceND(shape), this, tstride, toffset);
		int[] nstride = new int[rank];
		for (int i = 0; i < rank; i++) {
			final int ax = axes[i];
			nstride[i] = tstride[ax];
			nshape[i] = shape[ax];
		}
		t.shape = nshape;
		t.stride = nstride;
		t.offset = toffset[0];
		t.base = this;
		t.setDirty();
		t.transposeMetadata(axes);
		return t;
	}

	@Override
	public Dataset transpose(int... axes) {
		Dataset t = getTransposedView(axes);
		return t == null ? clone() : t.clone();
	}

	@Override
	public Dataset swapAxes(int axis1, int axis2) {
		int rank = shape.length;
		axis1 = ShapeUtils.checkAxis(rank, axis1);
		axis2 = ShapeUtils.checkAxis(rank, axis2);

		if (rank == 1 || axis1 == axis2) {
			return this;
		}

		int[] axes = new int[rank];
		for (int i = 0; i < rank; i++) {
			axes[i] = i;
		}		

		axes[axis1] = axis2;
		axes[axis2] = axis1;
		return getTransposedView(axes);
	}

	boolean isContiguous() {
		if (stride == null)
			return true;

		if (offset != 0)
			return false;

		int s = getElementsPerItem();
		for (int j = getRank() - 1; j >= 0; j--) {
			if (stride[j] != s) {
				return false;
			}
			s *= shape[j];
		}

		return true;
	}

	@Override
	public Dataset flatten() {
		if (!isContiguous()) { // need to make a copy if not contiguous
			return clone().flatten();
		}
		return reshape(size);
	}

	/**
	 * Fill dataset from object at depth dimension
	 * @param obj
	 * @param depth
	 * @param pos position
	 */
	protected void fillData(Object obj, final int depth, final int[] pos) {
		if (obj == null) {
			Class<?> c = InterfaceUtils.getElementClass(getClass());
			if (Float.class.equals(c)) {
				set(Float.NaN, pos);
			} else if (Float.class.equals(c)) {
				set(Double.NaN, pos);
			}
			return;
		}

		Class<?> clazz = obj.getClass();
		if (obj instanceof List<?>) {
			List<?> jl = (List<?>) obj;
			int l = jl.size();
			for (int i = 0; i < l; i++) {
				Object lo = jl.get(i);
				fillData(lo, depth + 1, pos);
				pos[depth]++;
			}
			pos[depth] = 0;
		} else if (clazz.isArray()) {
			int l = Array.getLength(obj);
			if (clazz.equals(odata.getClass())) {
				System.arraycopy(obj, 0, odata, get1DIndex(pos), l);
			} else if (clazz.getComponentType().isPrimitive()) {
				for (int i = 0; i < l; i++) {
					set(Array.get(obj, i), pos);
					pos[depth]++;
				}
				pos[depth] = 0;
			} else {
				for (int i = 0; i < l; i++) {
					fillData(Array.get(obj, i), depth + 1, pos);
					pos[depth]++;
				}
				pos[depth] = 0;
			}
		} else if (obj instanceof IDataset) {
			boolean[] a = new boolean[shape.length];
			Arrays.fill(a, depth, a.length, true);
			setSlice(obj, getSliceIteratorFromAxes(pos, a));
		} else {
			set(obj, pos);
		}
	}

	@Override
	public IndexIterator getIterator(final boolean withPosition) {
		if (stride != null) {
			return base.getSize() == 1  ? (withPosition ? new PositionIterator(offset, shape) :
				new SingleItemIterator(offset, size)) : new StrideIterator(shape, stride, offset);
		}
		if (shape == null) {
			return new NullIterator();
		}

		return withPosition ? new ContiguousIteratorWithPosition(shape, size) : new ContiguousIterator(size);
	}

	@Override
	public IndexIterator getIterator() {
		return getIterator(false);
	}

	@Override
	public PositionIterator getPositionIterator(final int... axes) {
		return new PositionIterator(shape, axes);
	}

	@Override
	public IndexIterator getSliceIterator(final int[] start, final int[] stop, final int[] step) {
		return internalGetSliceIterator(new SliceND(shape, start, stop, step));
	}

	/**
	 * @param slice
	 * @return an slice iterator that operates like an IndexIterator
	 */
	public IndexIterator getSliceIterator(SliceND slice) {
		checkSliceND(slice);
		return internalGetSliceIterator(slice);
	}

	/**
	 * @param slice
	 * @return an slice iterator that operates like an IndexIterator
	 */
	protected IndexIterator internalGetSliceIterator(SliceND slice) {
		if (ShapeUtils.calcLongSize(slice.getShape()) == 0) {
			return new NullIterator(shape, slice.getShape());
		}
		if (stride != null) {
			return new StrideIterator(getElementsPerItem(), shape, stride, offset, slice);
		}
		return new SliceIterator(shape, size, slice);
	}

	@Override
	public SliceIterator getSliceIteratorFromAxes(final int[] pos, boolean[] axes) {
		int rank = shape.length;
		int[] start;
		int[] stop = new int[rank];
		int[] step = new int[rank];

		if (pos == null) {
			start = new int[rank];
		} else if (pos.length == rank) {
			start = pos.clone();
		} else {
			throw new IllegalArgumentException("pos array length is not equal to rank of dataset");
		}
		if (axes == null) {
			axes = new boolean[rank];
			Arrays.fill(axes, true);
		} else if (axes.length != rank) {
			throw new IllegalArgumentException("axes array length is not equal to rank of dataset");
		}

		for (int i = 0; i < rank; i++) {
			if (axes[i]) {
				stop[i] = shape[i];
			} else {
				stop[i] = start[i] + 1;
			}
			step[i] = 1;
		}
		return (SliceIterator) getSliceIterator(start, stop, step);
	}

	@Override
	public BooleanIterator getBooleanIterator(Dataset choice) {
		return getBooleanIterator(choice, true);
	}

	@Override
	public BooleanIterator getBooleanIterator(Dataset choice, boolean value) {
		return BooleanIterator.createIterator(value, this, choice, this);
	}

	@Override
	public Dataset getByBoolean(Dataset selection) {
		checkCompatibility(selection);

		final int length = ((Number) selection.sum()).intValue();
		final int is = getElementsPerItem();
		Dataset r = DatasetFactory.zeros(is, getClass(), length);
		BooleanIterator biter = getBooleanIterator(selection);

		int i = 0;
		while (biter.hasNext()) {
			r.setObjectAbs(i, getObjectAbs(biter.index));
			i += is;
		}
		return r;
	}

	@Override
	public Dataset getBy1DIndex(IntegerDataset index) {
		final int is = getElementsPerItem();
		final Dataset r = DatasetFactory.zeros(is, getClass(), index.getShapeRef());
		final IntegerIterator iter = new IntegerIterator(index, size, is);

		int i = 0;
		while (iter.hasNext()) {
			r.setObjectAbs(i, getObjectAbs(iter.index));
			i += is;
		}
		return r;
	}

	@Override
	public Dataset getByIndexes(final Object... indexes) {
		final IntegersIterator iter = new IntegersIterator(shape, indexes);
		final int is = getElementsPerItem();
		final Dataset r = DatasetFactory.zeros(is, getClass(), iter.getShape());

		final int[] pos = iter.getPos();
		int i = 0;
		while (iter.hasNext()) {
			r.setObjectAbs(i, getObject(pos));
			i += is;
		}
		return r;
	}

	@Override
	public boolean hasFloatingPointElements() {
		Class<?> cls = getElementClass();
		return cls == Float.class || cls == Double.class;
	}

	@Override
	public int getElementsPerItem() {
		return InterfaceUtils.getElementsPerItem(getClass());
	}

	@Override
	public int getItemBytes() {
		return InterfaceUtils.getItemBytes(getElementsPerItem(), getClass());
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int[] getShape() {
		// make a copy of the dimensions data, and put that out
		if (shape == null) {
			logger.warn("Shape is null!!!");
			return new int[] {};
		}
		return shape.clone();
	}

	@Override
	public int getRank() {
		return shape == null ? 0 : shape.length;
	}

	@Override
	public int getNbytes() {
		return getSize() * getItemBytes();
	}

	/**
	 * Check for -1 placeholder in shape and replace if necessary
	 * @param shape
	 * @param size
	 */
	private void checkShape(int[] shape, int size) {
		if (shape == null) {
			if (size == 0) {
				return;
			}
			logger.error("New shape must not be null for nonzero-sized dataset");
			throw new IllegalArgumentException("New shape must not be null for nonzero-sized dataset");
		}
		int rank = shape.length;
		int found = -1;
		int nsize = 1;
		for (int i = 0; i < rank; i++) {
			int d = shape[i];
			if (d == -1) {
				if (found == -1) {
					found = i;
				} else {
					logger.error("Can only have one -1 placeholder in shape");
					throw new IllegalArgumentException("Can only have one -1 placeholder in shape");
				}
			} else {
				nsize *= d;
			}
		}
		if (found >= 0) {
			shape[found] = size/nsize;
		} else if (nsize != size && !(rank == 0 && size == 0)) {
			logger.error("New shape is not same size as old shape");
			throw new IllegalArgumentException("New size is not same as the old size. Old size is "+size+" new size is "+nsize+" and shape is "+Arrays.toString(shape));
		}
	}

	@Override
	public void setShape(final int... shape) {
		int[] nshape = shape == null ? null : shape.clone();
		checkShape(nshape, size);
		if (Arrays.equals(this.shape, nshape)) {
			return;
		}

		if (stride != null) {
			// the only compatible shapes are ones where new dimensions are factors of old dimensions
			// or are combined adjacent old dimensions 
			int[] oshape = this.shape;
			int orank = oshape.length;
			int nrank = nshape.length;
			int diff = nrank - orank;
			int[] nstride = new int[nrank];
			boolean ones = true;
			// work forwards for broadcasting cases
			for (int i = 0, j = 0; i < orank || j < nrank;) {
				if (j >= diff && i < orank && j < nrank && oshape[i] == nshape[j]) {
					nstride[j++] = stride[i++];
				} else if (j < nrank && nshape[j] == 1) {
					nstride[j++] = 0;
				} else if (i < orank && oshape[i] == 1) {
					i++;
				} else {
					if (j < nrank)
						j++;
					if (i < orank)
						i++;
					ones = false;
				}
			}
			if (!ones) { // not just ones differ in shapes
				int[] ostride = stride;
				int ob = 0;
				int oe = 1;
				int nb = 0;
				int ne = 1;
				while (ob < orank && nb < nrank) {
					int ol = oshape[ob];
					int nl = nshape[nb];
					
					if (nl < ol) { // find group of shape dimensions that form common size
						do { // case where new shape spreads single dimension over several dimensions
							if (ne == nrank) {
								break;
							}
							nl *= nshape[ne++];
						} while (nl < ol);
						if (nl != ol) {
							logger.error("Subshape is incompatible with single dimension");
							throw new IllegalArgumentException("Subshape is incompatible with single dimension");
						}
						int on = ne - 1;
						while (nshape[on] == 1) {
							on--;
						}

						nstride[on] = ostride[ob];
						for (int n = on - 1; n >= nb; n--) {
							if (nshape[n] == 1)
								continue;

							nstride[n] = nshape[on] * nstride[on];
							on = n;
						}
					} else if (ol < nl) {
						do { // case where new shape combines several dimensions into one dimension
							if (oe == orank) {
								break;
							}
							ol *= oshape[oe++];
						} while (ol < nl);
						if (nl != ol) {
							logger.error("Single dimension is incompatible with subshape");
							throw new IllegalArgumentException("Single dimension is incompatible with subshape");
						}

						int oo = oe - 1;
						while (oshape[oo] == 1) {
							oo--;
						}
						int os = ostride[oo];
						for (int o = oo - 1; o >= ob; o--) {
							if (oshape[o] == 1)
								continue;
							if (ostride[o] != oshape[oo] * ostride[oo]) {
								logger.error("Subshape cannot be a non-contiguous view");
								throw new IllegalArgumentException("Subshape cannot be a non-contiguous view");
							}
							oo = o;
						}
						nstride[nb] = os;
					} else {
						nstride[nb] = ostride[ob];
					}

					ob = oe++;
					nb = ne++;
				}
			}
	
			stride = nstride;
		}

		setDirty();
		if (this.shape != null && metadata != null) {
			reshapeMetadata(this.shape, nshape);
		}
		this.shape = nshape;
	}

	@Override
	public int[] getShapeRef() {
		return shape;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public int[] getStrides() {
		return stride;
	}

	@Override
	public Serializable getBuffer() {
		return odata;
	}

	@Override
	public void overrideInternal(Serializable buffer, int... shape) {
		if (buffer != null) {
			odata = buffer;
			setData();
			setDirty();
		}
	
		if (shape != null) {
			this.shape = shape.clone();
			size = ShapeUtils.calcSize(this.shape);
		}
	}

	/**
	 * Create a stride array from dataset
	 * @param a dataset
	 * @param offset output offset
	 * @return new strides
	 */
	public static int[] createStrides(Dataset a, final int[] offset) {
		return createStrides(a.getElementsPerItem(), a.getShapeRef(), a.getStrides(), a.getOffset(), offset);
	}

	/**
	 * Create a stride array from dataset
	 * @param isize
	 * @param shape
	 * @param oStride original stride
	 * @param oOffset original offset (only used if there is an original stride)
	 * @param offset output offset
	 * @return new strides
	 */
	public static int[] createStrides(final int isize, final int[] shape, final int[] oStride, final int oOffset, final int[] offset) {
		int rank = shape.length;
		final int[] stride;
		if (oStride == null) {
			offset[0] = 0;
			stride = new int[rank];
			int s = isize;
			for (int j = rank - 1; j >= 0; j--) {
				stride[j] = s;
				s *= shape[j];
			}
		} else {
			offset[0] = oOffset;
			stride = oStride.clone();
		}
		return stride;
	}

	/**
	 * Create a stride array from slice information and a dataset
	 * @param slice
	 * @param a dataset
	 * @param stride output stride
	 * @param offset output offset
	 * @return new shape
	 */
	public static int[] createStrides(final SliceND slice, final Dataset a, final int[] stride, final int[] offset) {
		return createStrides(slice, a.getElementsPerItem(), a.getShapeRef(), a.getStrides(), a.getOffset(), stride, offset);
	}

	/**
	 * Create a stride array from slice and dataset information
	 * @param slice
	 * @param isize
	 * @param shape
	 * @param oStride original stride
	 * @param oOffset original offset (only used if there is an original stride)
	 * @param stride output stride
	 * @param offset output offset
	 * @return new shape
	 */
	public static int[] createStrides(final SliceND slice, final int isize, final int[] shape, final int[] oStride, final int oOffset, final int[] stride, final int[] offset) {
		int[] lstart = slice.getStart();
		int[] lstep = slice.getStep();
		int[] newShape = slice.getShape();
		int rank = shape.length;

		if (oStride == null) {
			int s = isize;
			offset[0] = 0;
			for (int j = rank - 1; j >= 0; j--) {
				stride[j] = s * lstep[j];
				offset[0] += s * lstart[j];
				s *= shape[j];
			}
		} else {
			offset[0] = oOffset;
			for (int j = 0; j < rank; j++) {
				int s = oStride[j];
				stride[j] = lstep[j] * s;
				offset[0] += lstart[j] * s;
			}
		}

		return newShape;
	}

	@Override
	public Dataset getBroadcastView(int... broadcastShape) {
		AbstractDataset view = getView(true);
		
		if (!Arrays.equals(shape, broadcastShape)) {
			List<int[]> nShapes = BroadcastUtils.broadcastShapesToMax(broadcastShape, shape);
			view.setShape(nShapes.get(0));
			view.stride = BroadcastUtils.createBroadcastStrides(view, broadcastShape);
			view.base = this;
			view.shape = broadcastShape.clone();
			view.size = ShapeUtils.calcSize(broadcastShape);
			if (view.name == null || view.name.isEmpty()) {
				view.name = "Broadcast from " + Arrays.toString(shape);
			} else {
				view.name = "Broadcast of " + view.name + " from " + Arrays.toString(shape);
			}
		}
		return view;
	}

	@Override
	public Dataset getSliceView(final int[] start, final int[] stop, final int[] step) {
		return internalGetSliceView(new SliceND(shape, start, stop, step));
	}

	@Override
	public Dataset getSliceView(Slice... slice) {
		if (slice == null || slice.length == 0) {
			return getView(true);
		}

		return internalGetSliceView(new SliceND(shape, slice));
	}

	/**
	 * Get a slice of the dataset. The returned dataset is a view on a selection of items
	 * @param slice
	 * @return slice view
	 */
	@Override
	public Dataset getSliceView(SliceND slice) {
		checkSliceND(slice);
		return internalGetSliceView(slice);
	}

	private Dataset internalGetSliceView(SliceND slice) {
		if (slice.isAll()) {
			return getView(true);
		}

		final int rank = shape.length;
		int[] sStride = new int[rank];
		int[] sOffset = new int[1];

		int[] sShape = createStrides(slice, this, sStride, sOffset);
	
		AbstractDataset s = getView(false);
		s.shape = sShape;
		s.size = ShapeUtils.calcSize(sShape);
		s.stride = sStride;
		s.offset = sOffset[0];
		s.base = this;

		s.metadata = copyMetadata();
		s.sliceMetadata(true, slice);

		s.setDirty();
		s.setName(name + BLOCK_OPEN + slice + BLOCK_CLOSE);

		return s;
	}

	/**
	 * Get flattened view index of given position 
	 * @param pos
	 *            the integer array specifying the n-D position
	 * @return the index on the flattened dataset
	 */
	private int getFlat1DIndex(final int[] pos) {
		final int imax = pos.length;
		if (imax == 0) {
			return 0;
		}

		return get1DIndexFromShape(pos);
	}

	/**
	 * @since 2.0
	 */
	protected int getFirst1DIndex() {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot find an index from a null shape");
		}
		return stride == null ? 0 : offset;
	}

	@Override
	public int get1DIndex(final int... n) {
		if (n.length == 0 && shape.length == 0)
			return offset;

		return stride == null ? get1DIndexFromShape(n) : get1DIndexFromStrides(n);
	}

	private static void throwAIOOBException(int i, int s, int d) {
		throw new ArrayIndexOutOfBoundsException("Index (" + i + ") out of range [-" + s + "," + s
				+ "] in dimension " + d);
	}

	/**
	 * @param i
	 * @return the index on the data array corresponding to that location
	 */
	protected int get1DIndex(int i) {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot find an index from a null shape");
		}
		if (shape.length > 1) {
			logger.error("This dataset is not 1D but was addressed as such");
			throw new UnsupportedOperationException("This dataset is not 1D but was addressed as such");
		}
		if (i < 0) {
			i += shape[0];
		}
		if (i < 0 || i >= shape[0]) {
			throwAIOOBException(i, shape[0], 0);
		}
		return stride == null ? i : i*stride[0] + offset;
	}

	/**
	 * @param i
	 * @param j
	 * @return the index on the data array corresponding to that location
	 */
	protected int get1DIndex(int i, int j) {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot find an index from a null shape");
		}
		if (shape.length != 2) {
			logger.error("This dataset is not 2D but was addressed as such");
			throw new UnsupportedOperationException("This dataset is not 2D but was addressed as such");
		}
		if (i < 0) {
			i += shape[0];
		}
		if (i < 0 || i >= shape[0]) {
			throwAIOOBException(i, shape[0], 0);
		}
		if (j < 0) {
			j += shape[1];
		}
		if (j < 0 || j >= shape[1]) {
			throwAIOOBException(i, shape[1], 1);
		}
		return stride == null ? i*shape[1] + j : i*stride[0] + j*stride[1] + offset;
	}

	protected int get1DIndexFromShape(final int[] n) {
		return get1DIndexFromShape(shape, n);
	}

	protected static int get1DIndexFromShape(final int[] shape, final int[] n) {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot find an index from a null shape");
		}
		final int rank = shape.length;
		if (rank != n.length) {
			String errMsg = String.format("Number of position values must be equal to rank of %d", rank);
			logger.error(errMsg);
			throw new IllegalArgumentException(errMsg);
		}
		int index = 0;
		for (int i = 0; i < rank; i++) {
			final int si = shape[i];
			int ni = n[i];
			if (ni < 0) {
				ni += si;
			}
			if (ni < 0 || ni >= si) {
				throwAIOOBException(ni, si, i);
			}
			index = index * si + ni;
		}

		return index;
	}

	private int get1DIndexFromStrides(final int[] n) {
		return get1DIndexFromStrides(shape, stride, offset, n);
	}

	private static int get1DIndexFromStrides(final int[] shape, final int[] stride, final int offset, final int[] n) {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot find an index from a null shape");
		}
		final int rank = shape.length;
		if (rank != n.length) {
			String errMsg = String.format("Number of position values must be equal to rank of %d", rank);
			logger.error(errMsg);
			throw new IllegalArgumentException(errMsg);
		}
		int index = offset;
		for (int i = 0; i < rank; i++) {
			final int st = stride[i];
			if (st != 0) { // not broadcasted
				final int si = shape[i];
				int ni = n[i];
				if (ni < 0) {
					ni += si;
				}
				if (ni < 0 || ni >= si) {
					throwAIOOBException(ni, si, i);
				}
				index += st * ni;
			}
		}
		return index;
	}

	@Override
	public int[] getNDPosition(final int n) {
		if (isIndexInRange(n)) {
			throw new IllegalArgumentException("Index provided " + n
					+ "is larger then the size of the containing array");
		}

		return stride == null ? ShapeUtils.getNDPositionFromShape(n, shape) : getNDPositionFromStrides(n);
	}

	private boolean isIndexInRange(final int n) {
		if (stride == null) {
			return n >= size;
		}
		return n >= getBufferLength();
	}

	/**
	 * @return entire buffer length
	 */
	abstract protected int getBufferLength();

	private int[] getNDPositionFromStrides(int n) {
		n -= offset;
		int rank = shape.length;
		if (rank == 1) {
			return new int[] { n / stride[0] };
		}

		int[] output = new int[rank];
		int i = 0;
		while (i != n) { // TODO find more efficient way than this exhaustive search
			int j = rank - 1;
			for (; j >= 0; j--) {
				output[j]++;
				i += stride[j];
				if (output[j] >= shape[j]) {
					output[j] = 0;
					i -= shape[j] * stride[j];
				} else {
					break;
				}
			}
			if (j == -1) {
				logger.error("Index was not found in this strided dataset");
				throw new IllegalArgumentException("Index was not found in this strided dataset");
			}
		}

		return output;
	}

	@Override
	public int checkAxis(int axis) {
		return ShapeUtils.checkAxis(shape.length, axis);
	}

	@Deprecated
	protected static int checkAxis(int rank, int axis) {
		return ShapeUtils.checkAxis(rank, axis);
	}

	protected static final char BLOCK_OPEN = '[';
	protected static final char BLOCK_CLOSE = ']';

	@Override
	public String toString() {
		final int rank = shape == null ? 0 : shape.length;
		final StringBuilder out = new StringBuilder();

		if (InterfaceUtils.isElemental(getClass())) {
			out.append("Dataset ");
		} else {
			out.append("Compound dataset (");
			out.append(getElementsPerItem());
			out.append(") ");
		}

		if (name != null && name.length() > 0) {
			out.append("'");
			out.append(name);
			out.append("' has shape ");
		} else {
			out.append("shape is ");
		}

		out.append(BLOCK_OPEN);
		if (rank > 0) {
			out.append(shape[0]);
		}
		for (int i = 1; i < rank; i++) {
			out.append(", " + shape[i]);
		}
		out.append(BLOCK_CLOSE);
		return out.toString();
	}

	@Override
	public String toString(boolean showData) {
		if (!showData) {
			return toString();
		}

		if (size == 0) {
			return "[]";
		}

		final int rank = shape == null ? 0 : shape.length;
		final StringBuilder out = new StringBuilder();

		if (rank > 0) {
			int[] pos = new int[rank];
			final StringBuilder lead = new StringBuilder();
			printBlocks(out, lead, 0, pos);
		} else {
			out.append(getString());
		}
		return out.toString();
	}

	/**
	 * Limit to strings output via the toString() method
	 */
	private static int maxStringLength = 120;

	/**
	 * Set maximum line length for toString() method
	 * @param maxLineLength
	 */
	public static void setMaxLineLength(int maxLineLength) {
		maxStringLength = maxLineLength;
	}

	/**
	 * @return maximum line length for toString() method
	 */
	public static int getMaxLineLength() {
		return maxStringLength;
	}

	/**
	 * Limit to number of sub-blocks output via the toString() method
	 */
	private static final int MAX_SUBBLOCKS = 6;

	private final static String SEPARATOR = ",";
	private final static String SPACE = " ";
	private final static String ELLIPSIS = "...";
	private final static String NEWLINE = "\n";

	/**
	 * Make a line of output for last dimension of dataset
	 * 
	 * @param start
	 * @return line
	 */
	private StringBuilder makeLine(final int end, final int[] start) {
		StringBuilder line = new StringBuilder();
		final int[] pos;
		if (end >= start.length) {
			pos = Arrays.copyOf(start, end + 1);
		} else {
			pos = start;
		}
		pos[end] = 0;
		line.append(BLOCK_OPEN);
		line.append(getString(pos));

		final int length = shape[end];

		// trim elements printed if length exceed estimate of maximum elements
		int excess = length - maxStringLength / 3; // space + number + separator
		int midIndex = -1;
		if (excess > 0) {
			int index = (length - excess) / 2;
			for (int y = 1; y < index; y++) {
				line.append(SEPARATOR + SPACE);
				pos[end] = y;
				line.append(getString(pos));
			}
			midIndex = line.length() + 2;
			index = (length + excess) / 2;
			for (int y = index; y < length; y++) {
				line.append(SEPARATOR + SPACE);
				pos[end] = y;
				line.append(getString(pos));
			}
		} else {
			for (int y = 1; y < length; y++) {
				line.append(SEPARATOR + SPACE);
				pos[end] = y;
				line.append(getString(pos));
			}
		}
		line.append(BLOCK_CLOSE);

		// trim string down to limit
		int lineLength = line.length();
		excess = lineLength - maxStringLength - ELLIPSIS.length() - 1;
		if (excess > 0) {
			int index = (lineLength - excess) / 2;
			if (midIndex > 0 && index > midIndex) {
				index = midIndex;
			} else {
				index = line.lastIndexOf(SEPARATOR, index) + 2;
			}
			StringBuilder out = new StringBuilder(line.subSequence(0, index));
			out.append(ELLIPSIS + SEPARATOR);
			index = (lineLength + excess) / 2;
			if (midIndex > 0 && index <= midIndex) {
				index = midIndex - 1;
			} else {
				index = line.indexOf(SEPARATOR, index) + 1;
			}
			out.append(line.subSequence(index, lineLength));
			return out;
		} else if (midIndex > 0) { // add ellipsis
			StringBuilder out = new StringBuilder(line.subSequence(0, midIndex));
			out.append(ELLIPSIS + SEPARATOR + SPACE);
			out.append(line.subSequence(midIndex, lineLength));
			return out;
		}

		return line;
	}

	/**
	 * recursive method to print blocks
	 */
	private void printBlocks(final StringBuilder out, final StringBuilder lead, final int level, final int[] pos) {
		if (out.length() > 0) {
			char last = out.charAt(out.length() - 1);
			if (last != BLOCK_OPEN) {
				out.append(lead);
			}
		}
		final int end = getRank() - 1;
		if (level != end) {
			out.append(BLOCK_OPEN);
			int length = shape[level];

			// first sub-block
			pos[level] = 0;
			StringBuilder newlead = new StringBuilder(lead);
			newlead.append(SPACE);
			printBlocks(out, newlead, level + 1, pos);
			if (length < 2) { // escape
				out.append(BLOCK_CLOSE);
				return;
			}

			out.append(SEPARATOR + NEWLINE);
			for (int i = level + 1; i < end; i++) {
				out.append(NEWLINE);
			}

			// middle sub-blocks
			if (length < MAX_SUBBLOCKS) {
				for (int x = 1; x < length - 1; x++) {
					pos[level] = x;
					printBlocks(out, newlead, level + 1, pos);
					if (end <= level + 1) {
						out.append(SEPARATOR + NEWLINE);
					} else {
						out.append(SEPARATOR + NEWLINE + NEWLINE);
					}
				}
			} else {
				final int excess = length - MAX_SUBBLOCKS;
				int xmax = (length - excess) / 2;
				for (int x = 1; x < xmax; x++) {
					pos[level] = x;
					printBlocks(out, newlead, level + 1, pos);
					if (end <= level + 1) {
						out.append(SEPARATOR + NEWLINE);
					} else {
						out.append(SEPARATOR + NEWLINE + NEWLINE);
					}
				}
				out.append(newlead);
				out.append(ELLIPSIS + SEPARATOR + NEWLINE);
				xmax = (length + excess) / 2;
				for (int x = xmax; x < length - 1; x++) {
					pos[level] = x;
					printBlocks(out, newlead, level + 1, pos);
					if (end <= level + 1) {
						out.append(SEPARATOR + NEWLINE);
					} else {
						out.append(SEPARATOR + NEWLINE + NEWLINE);
					}
				}
			}

			// last sub-block
			pos[level] = length - 1;
			printBlocks(out, newlead, level + 1, pos);
			out.append(BLOCK_CLOSE);
		} else {
			out.append(makeLine(end, pos));
		}
	}

	@Override
	public Dataset squeezeEnds() {
		return squeeze(true);
	}

	@Override
	public Dataset squeeze() {
		return squeeze(false);
	}

	@Override
	public Dataset squeeze(boolean onlyFromEnds) {
		final int[] tshape = ShapeUtils.squeezeShape(shape, onlyFromEnds);
		final int[] oshape = shape;
		if (stride == null) {
			shape = tshape;
		} else {
			int rank = shape.length;
			int trank = tshape.length;
			if (trank < rank) {
				int[] tstride = new int[tshape.length];
				if (onlyFromEnds) {
					for (int i = 0; i < rank; i++) {
						if (shape[i] != 1) {
							for (int k = 0; k < trank; k++) {
								tstride[k] = stride[i++];
							}
							break;
						}
					}
				} else {
					int t = 0;
					for (int i = 0; i < rank; i++) {
						if (shape[i] != 1) {
							tstride[t++] = stride[i];
						}
					}
				}
				shape = tshape;
				stride = tstride;
			}
		}

		setDirty();
		reshapeMetadata(oshape, shape);
		return this;
	}

	@Override
	public boolean isCompatibleWith(final ILazyDataset g) {
		return ShapeUtils.areShapesCompatible(shape, g.getShape());
	}

	@Override
	public void checkCompatibility(final ILazyDataset g) throws IllegalArgumentException {
		ShapeUtils.checkCompatibility(this, g);
	}

	@Override
	public Dataset reshape(final int... shape) {
		Dataset a;
		try {
			a = getView(true);
			a.setShape(shape);
		} catch (IllegalArgumentException e) {
			a = clone();
			a.setShape(shape);
		}
		return a;
	}

	/**
	 * @param start
	 * @param stop
	 * @param step
	 * @return number of steps to take
	 */
	protected static int calcSteps(final double start, final double stop, final double step) {
		return Math.max(0, (int) Math.ceil((stop - start) / step));
	}

	@Override
	public boolean isComplex() {
		return false;
	}

	@Override
	public Dataset getRealPart() {
		return this;
	}

	@Override
	public Dataset getRealView() {
		return getView(true);
	}

	@Override
	public Dataset getSlice(final int[] start, final int[] stop, final int[] step) {
		return internalGetSlice(new SliceND(shape, start, stop, step));
	}

	@Override
	public Dataset getSlice(Slice... slice) {
		return internalGetSlice(new SliceND(shape, slice));
	}

	@Override
	public Dataset getSlice(IMonitor monitor, Slice... slice) {
		return getSlice(slice);
	}

	@Override
	public Dataset getSlice(IMonitor monitor, SliceND slice) {
		return getSlice(slice);
	}

	@Override
	public Dataset getSlice(IMonitor monitor, int[] start, int[] stop, int[] step) {
		return getSlice(start, stop, step);
	}

	/**
	 * Get a slice of the dataset. The returned dataset is a copied selection of items
	 * @param slice
	 * @return The dataset of the sliced data
	 */
	@Override
	public Dataset getSlice(final SliceND slice) {
		checkSliceND(slice);
		return internalGetSlice(slice);
	}

	private Dataset internalGetSlice(final SliceND slice) {
		SliceIterator it = (SliceIterator) internalGetSliceIterator(slice);
		AbstractDataset s = getSlice(it);
		s.metadata = copyMetadata();
		s.setDirty();
		s.sliceMetadata(true, slice);
		return s;
	}

	/**
	 * Get a slice of the dataset. The returned dataset is a copied selection of items
	 * 
	 * @param iterator Slice iterator
	 * @return The dataset of the sliced data
	 */
	abstract public AbstractDataset getSlice(final SliceIterator iterator);

	@Override
	public Dataset setSlice(final Object obj, final SliceND slice) {
		checkSliceND(slice);
		return internalSetSlice(obj, slice);
	}

	private Dataset internalSetSlice(final Object obj, final SliceND slice) {
		Dataset ds;
		if (obj instanceof Dataset) {
			ds = (Dataset) obj;
		} else if (obj instanceof IDataset) {
			ds = DatasetUtils.convertToDataset((IDataset) obj);
		} else {
			Class<? extends Dataset> dClass = getClass();
			if (!BooleanDataset.class.equals(dClass)) {
				dClass = InterfaceUtils.getLargestInterface(this);
			}
			ds = DatasetFactory.createFromObject(getElementsPerItem(), dClass, obj);
		}

		return setSlicedView(getSliceView(slice), ds);
	}

	@Override
	public Dataset setSlice(final Object obj, final int[] start, final int[] stop, final int[] step) {
		return internalSetSlice(obj, new SliceND(shape, start, stop, step));
	}

	/**
	 * Set a view of current dataset to given dataset with broadcasting
	 * @param view
	 * @param d
	 * @return this dataset
	 */
	abstract Dataset setSlicedView(Dataset view, Dataset d);

	@Override
	public Dataset setSlice(Object obj, Slice... slice) {
		if (slice == null || slice.length == 0) {
			return internalSetSlice(obj, new SliceND(shape));
		}
		return internalSetSlice(obj, new SliceND(shape, slice));
	}

	@Override
	public boolean all() {
		return Comparisons.allTrue(this);
	}

	@Override
	public BooleanDataset all(final int axis) {
		return Comparisons.allTrue(this, axis);
	}

	@Override
	public boolean any() {
		return Comparisons.anyTrue(this);
	}

	@Override
	public BooleanDataset any(final int axis) {
		return Comparisons.anyTrue(this, axis);
	}

	@Override
	public Dataset ifloorDivide(final Object o) {
		return idivide(o).ifloor();
	}

	@Override
	public double residual(final Object o) {
		return residual(o, null, false);
	}

	@Override
	public double residual(final Object o, boolean ignoreNaNs) {
		return residual(o, null, ignoreNaNs);
	}

	/**
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	protected StatisticsMetadata<Number> getStats() {
		StatisticsMetadata<Number> md = getFirstMetadata(StatisticsMetadata.class);
		if (md == null || md.isDirty(this)) {
			md = new StatisticsMetadataImpl<Number>();
			md.initialize(this);
			setMetadata(md);
		}
		return md;
	}

	/**
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	protected StatisticsMetadata<String> getStringStats() {
		StatisticsMetadata<String> md = getFirstMetadata(StatisticsMetadata.class);
		if (md == null || md.isDirty(this)) {
			md = new StatisticsMetadataImpl<String>();
			md.initialize(this);
			setMetadata(md);
		}
		return md;
	}

	@Override
	public Number max(boolean... ignoreInvalids) {
		return getStats().getMaximum(ignoreInvalids);
	}

	@Override
	public Dataset max(int axis, boolean... ignoreInvalids) {
		return getStats().getMaximum(axis, ignoreInvalids);
	}

	@Override
	public Dataset max(int[] axes, boolean... ignoreInvalids) {
		return getStats().getMaximum(axes, ignoreInvalids);
	}

	@Override
	public Number min(boolean... ignoreInvalids) {
		return getStats().getMinimum(ignoreInvalids);
	}

	@Override
	public Dataset min(int axis, boolean... ignoreInvalids) {
		return getStats().getMinimum(axis, ignoreInvalids);
	}

	@Override
	public Dataset min(int[] axes, boolean... ignoreInvalids) {
		return getStats().getMinimum(axes, ignoreInvalids);
	}

	@Override
	public int argMax(boolean... ignoreInvalids) {
		return getFlat1DIndex(maxPos(ignoreInvalids));
	}

	/**
	 * @since 2.0
	 */
	@Override
	public IntegerDataset argMax(int axis, boolean... ignoreInvalids) {
		return (IntegerDataset) getStats().getArgMaximum(axis, ignoreInvalids);
	}

	@Override
	public int argMin(boolean... ignoreInvalids) {
		return getFlat1DIndex(minPos(ignoreInvalids));
	}

	/**
	 * @since 2.0
	 */
	@Override
	public IntegerDataset argMin(int axis, boolean... ignoreInvalids) {
		return (IntegerDataset) getStats().getArgMinimum(axis, ignoreInvalids);
	}

	@Override
	public Number peakToPeak(boolean... ignoreInvalids) {
		return InterfaceUtils.fromDoubleToBiggestNumber(getClass(), max(ignoreInvalids).doubleValue() - min(ignoreInvalids).doubleValue());
	}

	@Override
	public Dataset peakToPeak(int axis,  boolean... ignoreInvalids) {
		return Maths.subtract(max(axis, ignoreInvalids), min(axis, ignoreInvalids));
	}

	@Override
	public Dataset peakToPeak(int[] axes,  boolean... ignoreInvalids) {
		return Maths.subtract(max(axes, ignoreInvalids), min(axes, ignoreInvalids));
	}

	@Override
	public long count(boolean... ignoreInvalids) {
		return getStats().getCount(ignoreInvalids);
	}

	@Override
	public Dataset count(int axis, boolean... ignoreInvalids) {
		return getStats().getCount(axis, ignoreInvalids);
	}

	@Override
	public Dataset count(int[] axes, boolean... ignoreInvalids) {
		return getStats().getCount(axes, ignoreInvalids);
	}

	@Override
	public Object sum(boolean... ignoreInvalids) {
		return InterfaceUtils.toBiggestNumber(getClass(), getStats().getSum(ignoreInvalids));
	}

	@Override
	public Dataset sum(int axis, boolean... ignoreInvalids) {
		return getStats().getSum(axis, ignoreInvalids);
	}

	@Override
	public Dataset sum(int[] axes, boolean... ignoreInvalids) {
		return getStats().getSum(axes, ignoreInvalids);
	}

	@Override
	public Object product(boolean... ignoreInvalids) {
		return Stats.product(this, ignoreInvalids);
	}

	@Override
	public Dataset product(int axis, boolean... ignoreInvalids) {
		return Stats.product(this, axis, ignoreInvalids);
	}

	@Override
	public Dataset product(int[] axes, boolean... ignoreInvalids) {
		return Stats.product(this, axes, ignoreInvalids);
	}

	@Override
	public Object mean(boolean... ignoreInvalids) {
		return getStats().getMean(ignoreInvalids);
	}

	@Override
	public Dataset mean(int axis, boolean... ignoreInvalids) {
		return getStats().getMean(axis, ignoreInvalids);
	}

	@Override
	public Dataset mean(int[] axes, boolean... ignoreInvalids) {
		return getStats().getMean(axes, ignoreInvalids);
	}

	@Override
	public double variance() {
		return variance(false);
	}

	@Override
	public double variance(boolean isWholePopulation, boolean... ignoreInvalids) {
		return getStats().getVariance(isWholePopulation, ignoreInvalids);
	}

	@Override
	public Dataset variance(int axis) {
		return getStats().getVariance(axis, false);
	}

	@Override
	public Dataset variance(int[] axes) {
		return getStats().getVariance(axes, false);
	}

	@Override
	public Dataset variance(int axis, boolean isWholePopulation, boolean... ignoreInvalids) {
		return getStats().getVariance(axis, isWholePopulation, ignoreInvalids);
	}

	@Override
	public Dataset variance(int[] axes, boolean isWholePopulation, boolean... ignoreInvalids) {
		return getStats().getVariance(axes, isWholePopulation, ignoreInvalids);
	}

	@Override
	public double stdDeviation() {
		return Math.sqrt(variance());
	}

	@Override
	public double stdDeviation(boolean isWholePopulation, boolean... ignoreInvalids) {
		return Math.sqrt(variance(isWholePopulation, ignoreInvalids));
	}

	@Override
	public Dataset stdDeviation(int axis) {
		return Maths.sqrt(variance(axis, false));
	}

	@Override
	public Dataset stdDeviation(int[] axes) {
		return Maths.sqrt(variance(axes, false));
	}

	@Override
	public Dataset stdDeviation(int axis, boolean isWholePopulation, boolean... ignoreInvalids) {
		return Maths.sqrt(variance(axis, isWholePopulation, ignoreInvalids));
	}

	@Override
	public Dataset stdDeviation(int[] axes, boolean isWholePopulation, boolean... ignoreInvalids) {
		return Maths.sqrt(variance(axes, isWholePopulation, ignoreInvalids));
	}

	@Override
	public double rootMeanSquare(boolean... ignoreInvalids) {
		StatisticsMetadata<Number> stats = getStats();
		final double mean = stats.getMean(ignoreInvalids).doubleValue();
		final double var = stats.getVariance(true, ignoreInvalids);
		return Math.sqrt(var + mean * mean);
	}

	@Override
	public Dataset rootMeanSquare(int axis, boolean... ignoreInvalids) {
		StatisticsMetadata<Number> stats = getStats();
		Dataset v = stats.getVariance(axis, true, ignoreInvalids);
		Dataset m = stats.getMean(axis, ignoreInvalids);
		Dataset result = Maths.multiply(m, m);
		return Maths.sqrt(result.iadd(v));
	}

	@Override
	public Dataset rootMeanSquare(int[] axes, boolean... ignoreInvalids) {
		StatisticsMetadata<Number> stats = getStats();
		Dataset v = stats.getVariance(axes, true, ignoreInvalids);
		Dataset m = stats.getMean(axes, ignoreInvalids);
		Dataset result = Maths.multiply(m, m);
		return Maths.sqrt(result.iadd(v));
	}

	/**
	 * Set item from compatible dataset in a direct and speedy way. Remember to setDirty afterwards.
	 * 
	 * @param dindex
	 * @param sindex
	 * @param src
	 *            is the source data buffer
	 */
	protected abstract void setItemDirect(final int dindex, final int sindex, final Object src);

	/**
	 * @return error broadcasted to current shape
	 */
	private Dataset getBroadcastedInternalError() {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot get error for null dataset");
		}
		ILazyDataset led = super.getErrors();
		if (led == null)
			return null;

		Dataset ed = null;
		try {
			ed = DatasetUtils.sliceAndConvertLazyDataset(led);
		} catch (DatasetException e) {
			logger.error("Could not get data from lazy dataset", e);
		}
		if (led != ed) {
			setErrors(ed); // set back
		}

		return ed.getBroadcastView(shape);
	}

	@Override
	public Dataset getErrors() {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return null;

		return ed;
	}

	@Override
	public double getError() {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return 0;

		return ed.getDouble();
	}

	@Override
	public double getError(final int i) {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return 0;

		return ed.getDouble(i);
	}

	@Override
	public double getError(final int i, final int j) {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return 0;

		return ed.getDouble(i, j);
	}

	@Override
	public double getError(int... pos) {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return 0;

		return ed.getDouble(pos);
	}

	@Override
	public double[] getErrorArray(final int i) {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return null;

		return new double[] {getError(i)};
	}

	@Override
	public double[] getErrorArray(final int i, final int j) {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return null;

		return new double[] {getError(i, j)};
	}

	@Override
	public double[] getErrorArray(int... pos) {
		Dataset ed = getBroadcastedInternalError();
		if (ed == null)
			return null;

		return new double[] {getError(pos)};
	}

	protected Dataset getInternalSquaredError() {
		Dataset sed = getErrorBuffer().getBroadcastView(shape);
		return sed;
	}

	@Override
	public Dataset getErrorBuffer() {
		ErrorMetadata emd = getErrorMetadata();
		if (emd == null)
			return null;

		if (!(emd instanceof ErrorMetadataImpl)) {
			ILazyDataset led = emd.getError();
			Dataset ed;
			try {
				ed = DatasetUtils.sliceAndConvertLazyDataset(led);
				emd = MetadataFactory.createMetadata(ErrorMetadata.class);
				setMetadata(emd);
				emd.setError(ed);
			} catch (MetadataException me) {
				logger.error("Could not create metadata", me);
			} catch (DatasetException e) {
				logger.error("Could not get data from lazy dataset", e);
			}
		}

		return ((ErrorMetadataImpl) emd).getSquaredError();
	}

	/**
	 * Set a copy of the buffer that backs the (squared) error data
	 * @param buffer can be null, anything that can be used to create a DoubleDataset or CompoundDoubleDataset
	 */
	@Override
	public void setErrorBuffer(Serializable buffer) {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot set error buffer for null dataset");
		}
		if (buffer == null) {
			clearMetadata(ErrorMetadata.class);
			return;
		}

		IDataset d = (IDataset) createFromSerializable(buffer, false);
		ErrorMetadata emd = getErrorMetadata();
		if (!(emd instanceof ErrorMetadataImpl)) {
			try {
				emd = MetadataFactory.createMetadata(ErrorMetadata.class);
				setMetadata(emd);
			} catch (MetadataException me) {
				logger.error("Could not create metadata", me);
			}
		}
		((ErrorMetadataImpl) emd).setSquaredError(d);
	}
}
