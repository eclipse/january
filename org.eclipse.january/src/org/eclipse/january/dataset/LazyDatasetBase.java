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
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.january.DatasetException;
import org.eclipse.january.MetadataException;
import org.eclipse.january.metadata.Dirtiable;
import org.eclipse.january.metadata.ErrorMetadata;
import org.eclipse.january.metadata.IMetadata;
import org.eclipse.january.metadata.MetadataFactory;
import org.eclipse.january.metadata.MetadataType;
import org.eclipse.january.metadata.Reshapeable;
import org.eclipse.january.metadata.Sliceable;
import org.eclipse.january.metadata.Transposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base for both lazy and normal dataset implementations
 */
public abstract class LazyDatasetBase implements ILazyDataset, Serializable {

	private static final long serialVersionUID = 767926846438976050L;

	private static final Logger logger = LoggerFactory.getLogger(LazyDatasetBase.class);

	protected static boolean catchExceptions;

	static {
		/**
		 * Boolean to set to true if running jython scripts that utilise ScisoftPy in IDE
		 */
		try {
			catchExceptions = Boolean.getBoolean("run.in.eclipse");
		} catch (SecurityException e) {
			// set a default for when the security manager does not allow access to the requested key
			catchExceptions = false;
		}
	}

	transient private boolean dirty = true; // indicate dirty state of metadata
	protected String name = "";

	/**
	 * The shape or dimensions of the dataset
	 */
	protected int[] shape;

	protected ConcurrentMap<Class<? extends MetadataType>, List<MetadataType>> metadata = null;

	/**
	 * @return type of dataset item
	 */
	abstract public int getDType();

	@Override
	public LazyDatasetBase clone() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!getClass().equals(obj.getClass())) {
			return false;
		}

		LazyDatasetBase other = (LazyDatasetBase) obj;
		if (getDType() != other.getDType()) {
			return false;
		}
		if (getElementsPerItem() != other.getElementsPerItem()) {
			return false;
		}
		if (!Arrays.equals(shape, other.shape)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = getDType() * 17 + getElementsPerItem();
		int rank = shape.length;
		for (int i = 0; i < rank; i++) {
			hash = hash*17 + shape[i];
		}
		return hash;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int[] getShape() {
		return shape.clone();
	}

	@Override
	public int getRank() {
		return shape.length;
	}

	/**
	 * This method allows anything that dirties the dataset to clear various metadata values
	 * so that the other methods can work correctly.
	 * @since 2.1
	 */
	public void setDirty() {
		dirty = true;
	}

	protected void checkSliceND(SliceND slice) {
		if (slice != null) {
			int[] source = slice.getSourceShape();
			boolean fail = false;
			if (slice.isExpanded()) {
				fail = shape.length != source.length;
				if (!fail) {
					for (int i = 0; i < shape.length; i++) {
						if (shape[i] > source[i]) {
							fail = true;
							break;
						}
					}
				}
			} else {
				fail = !Arrays.equals(shape, source);
			}
			if (fail) {
				throw new IllegalArgumentException("Slice's shape must match dataset's shape");
			}
		}
	}

	/**
	 * Find first sub-interface of (or class that directly implements) MetadataType
	 * @param clazz
	 * @return sub-interface
	 * @exception IllegalArgumentException when given class is {@link MetadataType} or an anonymous sub-class of it
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends MetadataType> findMetadataTypeSubInterfaces(Class<? extends MetadataType> clazz) {
		if (clazz.equals(MetadataType.class)) {
			throw new IllegalArgumentException("Cannot accept MetadataType");
		}

		if (clazz.isInterface()) {
			return clazz;
		}

		if (clazz.isAnonymousClass()) { // special case
			Class<?> s = clazz.getSuperclass();
			if (!s.equals(Object.class)) {
				// only use super class if it is not an anonymous class of an interface
				clazz = (Class<? extends MetadataType>) s;
			}
		}

		for (Class<?> c : clazz.getInterfaces()) {
			if (c.equals(MetadataType.class)) {
				if (clazz.isAnonymousClass()) {
					throw new IllegalArgumentException("Cannot accept anonymous subclasses of MetadataType");
				}
				return clazz;
			}
			if (MetadataType.class.isAssignableFrom(c)) {
				return (Class<? extends MetadataType>) c;
			}
		}

		Class<?> c = clazz.getSuperclass(); // Naughty: someone has sub-classed a metadata class
		if (c != null) {
			return findMetadataTypeSubInterfaces((Class<? extends MetadataType>) c);
		}

		logger.error("Somehow the search for metadata type interface ended in a bad place");
		assert false; // should not be able to get here!!!
		return null;
	}

	@Override
	public void setMetadata(MetadataType metadata) {
		addMetadata(metadata, true);
	}

	@Override
	public void addMetadata(MetadataType metadata) {
		addMetadata(metadata, false);
	}

	private synchronized void addMetadata(MetadataType metadata, boolean clear) {
		if (metadata == null) {
			return;
		}

		if (this.metadata == null) {
			this.metadata = new ConcurrentHashMap<Class<? extends MetadataType>, List<MetadataType>>();
		}

		Class<? extends MetadataType> clazz = findMetadataTypeSubInterfaces(metadata.getClass());
		if (!this.metadata.containsKey(clazz)) {
			this.metadata.put(clazz, new ArrayList<MetadataType>());
		} else if (clear) {
			this.metadata.get(clazz).clear();
		}
		this.metadata.get(clazz).add(metadata);

		// add for special case of sub-interfaces of IMetadata
		if (!IMetadata.class.equals(clazz) && IMetadata.class.isAssignableFrom(clazz)) {
			clazz = IMetadata.class;
			if (!this.metadata.containsKey(clazz)) {
				this.metadata.put(clazz, new ArrayList<MetadataType>());
			} else if (clear) {
				this.metadata.get(clazz).clear();
			}
			this.metadata.get(clazz).add(metadata);
		}
	}

	@Override
	@Deprecated
	public synchronized IMetadata getMetadata() {
		return getFirstMetadata(IMetadata.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <S extends MetadataType, T extends S> List<S> getMetadata(Class<T> clazz) throws MetadataException {
		if (metadata == null) {
			dirty = false;
			return null;
		}

		if (dirty) {
			dirtyMetadata();
			dirty = false;
		}

		if (clazz == null) {
			List<S> all = new ArrayList<S>();
			for (Class<? extends MetadataType> c : metadata.keySet()) {
				all.addAll((Collection<S>) metadata.get(c));
			}
			return all;
		}

		return (List<S>) metadata.get(findMetadataTypeSubInterfaces(clazz));
	}

	@Override
	public synchronized <S extends MetadataType, T extends S> S getFirstMetadata(Class<T> clazz) {
		try {
			List<S> ml = getMetadata(clazz);
			if (ml == null) {
				return null;
			}
			for (S t : ml) {
				if (clazz.isInstance(t)) {
					return t;
				}
			}
		} catch (Exception e) {
			logger.error("Get metadata failed!",e);
		}

		return null;
	}

	@Override
	public synchronized void clearMetadata(Class<? extends MetadataType> clazz) {
		if (metadata == null) {
			return;
		}

		if (clazz == null) {
			metadata.clear();
			return;
		}

		List<MetadataType> list = metadata.get(findMetadataTypeSubInterfaces(clazz));
		if( list != null) {
			list.clear();
		}
	}

	/**
	 * @since 2.0
	 */
	protected synchronized ConcurrentMap<Class<? extends MetadataType>, List<MetadataType>> copyMetadata() {
		return copyMetadata(metadata);
	}

	/**
	 * @since 2.0
	 */
	protected static ConcurrentMap<Class<? extends MetadataType>, List<MetadataType>> copyMetadata(Map<Class<? extends MetadataType>, List<MetadataType>> metadata) {
		if (metadata == null) {
			return null;
		}

		ConcurrentHashMap<Class<? extends MetadataType>, List<MetadataType>> map = new ConcurrentHashMap<Class<? extends MetadataType>, List<MetadataType>>();
		copyMetadata(metadata, map);
		return map;
	}

	private static void copyMetadata(Map<Class<? extends MetadataType>, List<MetadataType>> inMetadata,
			Map<Class<? extends MetadataType>, List<MetadataType>> outMetadata) {
		for (Class<? extends MetadataType> c : inMetadata.keySet()) {
			List<MetadataType> l = inMetadata.get(c);
			List<MetadataType> nl = new ArrayList<MetadataType>(l.size());
			outMetadata.put(c, nl);
			for (MetadataType m : l) {
				if (m == null || isMetadataDirty(m)) { // skip dirty metadata
					continue;
				}
				nl.add(m.clone());
			}
		}
	}

	protected void restoreMetadata(Map<Class<? extends MetadataType>, List<MetadataType>> oldMetadata) {
		copyMetadata(oldMetadata, metadata);
	}

	/**
	 * @since 2.2
	 */
	protected static ConcurrentMap<Class<? extends MetadataType>, List<MetadataType>> getMetadataMap(ILazyDataset a, boolean clone) {
		List<MetadataType> all = null;
		try {
			all = a.getMetadata(null);
		} catch (Exception e) {
		}
		if (all == null) {
			return null;
		}

		ConcurrentMap<Class<? extends MetadataType>, List<MetadataType>> map = new ConcurrentHashMap<Class<? extends MetadataType>, List<MetadataType>>();

		for (MetadataType m : all) {
			if (m == null || isMetadataDirty(m)) { // skip dirty metadata
				continue;
			}
			Class<? extends MetadataType> c = findMetadataTypeSubInterfaces(m.getClass());
			List<MetadataType> l = map.get(c);
			if (l == null) {
				l = new ArrayList<MetadataType>();
				map.put(c, l);
			}
			if (clone) {
				m = m.clone();
			}
			l.add(m);
		}
		return map;
	}

	private static boolean isMetadataDirty(MetadataType m) {
		Class<? extends MetadataType> c = m.getClass();
		for (Field f : c.getDeclaredFields()) {
			if (f.isAnnotationPresent(Dirtiable.class)) {
				Class<?> t = f.getType();
				if (t.equals(boolean.class) || t.equals(Boolean.class)) {
					try {
						f.setAccessible(true);
						Object o = f.get(m);
						if (o.equals(true)) {
							return true;
						}
					} catch (Exception e) {
						logger.debug("Could not retrieve value of dirty variable: {}", c.getCanonicalName(), e);
					}
				}
			}
		}

		return false;
	}

	interface MetadatasetAnnotationOperation {
		/**
		 * Process value of given field
		 * <p>
		 * When the field is not a container then the returned value
		 * may replace the old value
		 * @param f given field
		 * @param o value of field
		 * @return transformed field
		 */
		Object processField(Field f, Object o);

		/**
		 * @return annotated class
		 */
		Class<? extends Annotation> getAnnClass();

		/**
		 * @param axis
		 * @return number of dimensions to insert or remove
		 */
		int change(int axis);

		/**
		 * 
		 * @return rank or -1 to match
		 */
		int getNewRank();

		/**
		 * Run on given lazy dataset
		 * @param lz
		 * @return 
		 */
		ILazyDataset run(ILazyDataset lz);
	}

	class MdsSlice implements MetadatasetAnnotationOperation {
		private boolean asView;
		private SliceND slice;
		private int[] oShape;
		private long oSize;

		public MdsSlice(boolean asView, SliceND slice) {
			this.asView = asView;
			this.slice = slice;
			oShape = slice.getSourceShape();
			oSize = ShapeUtils.calcLongSize(oShape);
		}

		@Override
		public Object processField(Field field, Object o) {
			return o;
		}

		@Override
		public Class<? extends Annotation> getAnnClass() {
			return Sliceable.class;
		}

		@Override
		public int change(int axis) {
			return 0;
		}

		@Override
		public int getNewRank() {
			return -1;
		}

		@Override
		public ILazyDataset run(ILazyDataset lz) {
			int rank = lz.getRank();
			if (slice.getStart().length != rank) {
				throw new IllegalArgumentException("Slice rank does not match dataset!");
			}

			int[] shape = lz.getShape();
			SliceND nslice;
			if (!ShapeUtils.areShapesBroadcastCompatible(oShape, shape)) {
				nslice = new SliceND(shape);
				for (int i = 0; i < rank; i++) {
					int s = shape[i];
					int os = oShape[i];
					if (s >= os) {
						nslice.setSlice(i, 0, os, 1);
					} else if (s == 1) {
						nslice.setSlice(i, 0, 1, 1);
					} else {
						throw new IllegalArgumentException("Sliceable dataset has non-unit dimension less than host!");
					}
				}
				lz = lz.getSliceView(nslice);
				shape = nslice.getShape();
			}
			if (lz.getSize() == oSize) {
				nslice = slice;
			} else {
				nslice = slice.clone();
				for (int i = 0; i < rank; i++) {
					int s = shape[i];
					if (s >= oShape[i]) {
						continue;
					} else if (s == 1) {
						nslice.setSlice(i, 0, 1, 1);
					} else {
						throw new IllegalArgumentException("Sliceable dataset has non-unit dimension less than host!");
					}
				}
				nslice.updateSourceShape(shape);
			}

			if (asView || (lz instanceof IDataset)) {
				return lz.getSliceView(nslice);
			}
			try {
				return lz.getSlice(nslice);
			} catch (DatasetException e) {
				logger.error("Could not slice dataset in metadata", e);
				return null;
			}
		}
	}

	class MdsReshape implements MetadatasetAnnotationOperation {
		private boolean matchRank;
		private int[] oldShape;
		private int[] newShape;
		boolean onesOnly;
		int[] differences;

		/*
		 * if only ones then record differences (insertions and deletions)
		 * 
		 * if shape changing, find broadcasted dimensions and disallow
		 * merging that include those dimensions
		 */
		public MdsReshape(final int[] oldShape, final int[] newShape) {
			this.oldShape = oldShape;
			this.newShape = newShape;
			differences = null;
		}

		@Override
		public Object processField(Field field, Object o) {
			Annotation a = field.getAnnotation(Reshapeable.class);
			if (a != null) { // cannot be null
				matchRank = ((Reshapeable) a).matchRank();
			}
			return o;
		}

		@Override
		public Class<? extends Annotation> getAnnClass() {
			return Reshapeable.class;
		}

		@Override
		public int change(int axis) {
			if (matchRank) {
				if (differences == null) {
					init();
				}

				if (onesOnly) {
					return differences == null ? 0 : differences[axis];
				}
				throw new UnsupportedOperationException("TODO support other shape operations");
			}
			return 0;
		}

		@Override
		public int getNewRank() {
			return matchRank ? newShape.length : -1;
		}

		private void init() {
			int or = oldShape.length - 1;
			int nr = newShape.length - 1;
			if (or < 0 || nr < 0) { // zero-rank shapes
				onesOnly = true;
				differences = new int[1];
				differences[0] = or < 0 ? nr + 1 : or + 1;
				return;
			}
			onesOnly = ShapeUtils.differsByOnes(oldShape, newShape);
			int ob = 0;
			int nb = 0;
			if (onesOnly) {
				differences = ShapeUtils.calcShapePadding(oldShape, newShape);
			} else {
				differences = new int[or + 2];
				if (matchRank) {
					logger.error("Combining dimensions is currently not supported");
					throw new IllegalArgumentException("Combining dimensions is currently not supported");
				}
				// work out mapping: contiguous dimensions can be grouped or split
				while (ob <= or && nb <= nr) {
					int ol = oldShape[ob];
					while (ol == 1 && ol <= or) {
						ob++;
						ol = oldShape[ob];
					}
					int oe = ob + 1;
					int nl = newShape[nb];
					while (nl == 1 && nl <= nr) {
						nb++;
						nl = newShape[nb];
					}
					int ne = nb + 1;
					if (ol < nl) {
						differences[ob] = 1;
						do { // case where new shape combines several dimensions into one dimension
							if (oe == (or + 1)) {
								break;
							}
							differences[oe] = 1;
							ol *= oldShape[oe++];
						} while (ol < nl);
						differences[oe - 1] = oe - ob; // signal end with difference
						if (nl != ol) {
							logger.error("Single dimension is incompatible with subshape");
							throw new IllegalArgumentException("Single dimension is incompatible with subshape");
						}
					} else if (ol > nl) {
						do { // case where new shape spreads single dimension over several dimensions
							if (ne == (nr + 1)) {
								break;
							}
							nl *= newShape[ne++];
						} while (nl < ol);
						if (nl != ol) {
							logger.error("Subshape is incompatible with single dimension");
							throw new IllegalArgumentException("Subshape is incompatible with single dimension");
						}
					}

					ob = oe;
					nb = ne;
				}
			}
		}

		@Override
		public ILazyDataset run(ILazyDataset lz) {
			if (differences == null) {
				init();
			}

			int[] lshape = lz.getShape();
			if (Arrays.equals(newShape, lshape)) {
				return lz;
			}
			int or = lshape.length;
			int nr = newShape.length;
			int[] nshape;
			if (onesOnly) {
				nshape = ShapeUtils.padShape(differences, nr, lshape);
			} else {
				nshape = new int[nr];
				boolean[] broadcast = new boolean[or];
				for (int ob = 0; ob < or; ob++) {
					broadcast[ob] = oldShape[ob] != 1 && lshape[ob] == 1;
				}
				int osize = lz.getSize();

				// cannot do 3x5x... to 15x... if metadata is broadcasting (i.e. 1x5x...)
				int ob = 0;
				int nsize = 1;
				for (int i = 0; i < nr; i++) {
					if (ob < or && broadcast[ob]) {
						if (differences[ob] != 0) {
							logger.error("Metadata contains a broadcast axis which cannot be reshaped");
							throw new IllegalArgumentException("Metadata contains a broadcast axis which cannot be reshaped");
						}
						nshape[i] = 1;
					} else {
						nshape[i] = nsize < osize ? newShape[i] : 1;
					}
					nsize *= nshape[i];
					ob++;
				}
			}

			ILazyDataset nlz;
			if (lz instanceof Dataset) {
				nlz = ((Dataset) lz).reshape(nshape);
			} else {
				nlz = lz.getSliceView();
				nlz.setShape(nshape);
			}
			return nlz;
		}
	}

	class MdsTranspose implements MetadatasetAnnotationOperation {
		int[] map;

		public MdsTranspose(final int[] axesMap) {
			map = axesMap;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Object processField(Field f, Object o) {
			// reorder arrays and lists according the axes map
			if (o.getClass().isArray()) {
				int l = Array.getLength(o);
				if (l == map.length) {
					Object narray = Array.newInstance(o.getClass().getComponentType(), l);
					for (int i = 0; i < l; i++) {
						Array.set(narray, i, Array.get(o, map[i]));
					}
					for (int i = 0; i < l; i++) {
						Array.set(o, i, Array.get(narray, i));
					}
				}
			} else if (o instanceof List<?>) {
				List list = (List) o;
				int l = list.size();
				if (l == map.length) {
					Object narray = Array.newInstance(o.getClass().getComponentType(), l);
					for (int i = 0; i < l; i++) {
						Array.set(narray, i, list.get(map[i]));
					}
					list.clear();
					for (int i = 0; i < l; i++) {
						list.add(Array.get(narray, i));
					}
				}
			}
			return o;
		}

		@Override
		public Class<? extends Annotation> getAnnClass() {
			return Transposable.class;
		}

		@Override
		public int change(int axis) {
			return 0;
		}

		@Override
		public int getNewRank() {
			return -1;
		}

		@Override
		public ILazyDataset run(ILazyDataset lz) {
			return lz.getTransposedView(map);
		}
	}

	class MdsDirty implements MetadatasetAnnotationOperation {

		@Override
		public Object processField(Field f, Object o) {
			// throw exception if not boolean???
			Class<?> t = f.getType();
			if (t.equals(boolean.class) || t.equals(Boolean.class)) {
				if (o.equals(false)) {
					o = true;
				}
			}
			return o;
		}

		@Override
		public Class<? extends Annotation> getAnnClass() {
			return Dirtiable.class;
		}

		@Override
		public int change(int axis) {
			return 0;
		}

		@Override
		public int getNewRank() {
			return -1;
		}

		@Override
		public ILazyDataset run(ILazyDataset lz) {
			return lz;
		}
	}

	/**
	 * Slice all datasets in metadata that are annotated by @Sliceable. Call this on the new sliced
	 * dataset after cloning the metadata
	 * @param asView if true then just a view
	 * @param slice
	 */
	protected void sliceMetadata(boolean asView, final SliceND slice) {
		processAnnotatedMetadata(new MdsSlice(asView, slice), true);
	}

	/**
	 * Reshape all datasets in metadata that are annotated by @Reshapeable. Call this when squeezing
	 * or setting the shape
	 * 
	 * @param newShape
	 */
	protected void reshapeMetadata(final int[] oldShape, final int[] newShape) {
		processAnnotatedMetadata(new MdsReshape(oldShape, newShape), true);
	}

	/**
	 * Transpose all datasets in metadata that are annotated by @Transposable. Call this on the transposed
	 * dataset after cloning the metadata
	 * @param axesMap
	 */
	protected void transposeMetadata(final int[] axesMap) {
		processAnnotatedMetadata(new MdsTranspose(axesMap), true);
	}

	/**
	 * Dirty metadata that are annotated by @Dirtiable. Call this when the dataset has been modified
	 * @since 2.0
	 */
	protected void dirtyMetadata() {
		processAnnotatedMetadata(new MdsDirty(), true);
	}

	@SuppressWarnings("unchecked")
	private void processAnnotatedMetadata(MetadatasetAnnotationOperation op, boolean throwException) {
		if (metadata == null)
			return;

		for (List<MetadataType> l : metadata.values()) {
			for (MetadataType m : l) {
				if (m == null) {
					continue;
				}

				Class<? extends MetadataType> mc = m.getClass();
				do { // iterate over super-classes
					processClass(op, m, mc, throwException);
					Class<?> sclazz = mc.getSuperclass();
					if (!MetadataType.class.isAssignableFrom(sclazz)) {
						break;
					}
					mc = (Class<? extends MetadataType>) sclazz;
				} while (true);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void processClass(MetadatasetAnnotationOperation op, MetadataType m, Class<? extends MetadataType> mc, boolean throwException) {
		for (Field f : mc.getDeclaredFields()) {
			if (!f.isAnnotationPresent(op.getAnnClass()))
				continue;

			try {
				f.setAccessible(true);
				Object o = f.get(m);
				if (o == null) {
					continue;
				}

				Object no = op.processField(f, o);
				if (no != o) {
					f.set(m, no);
					continue;
				}
				Object r = null;
				if (o instanceof ILazyDataset) {
					try {
						f.set(m, op.run((ILazyDataset) o));
					} catch (Exception e) {
						logger.error("Problem processing " + o, e);
						if (!catchExceptions) {
							throw e;
						}
					}
				} else if (o.getClass().isArray()) {
					int l = Array.getLength(o);

					for (int i = 0; r == null && i < l; i++) {
						r = Array.get(o, i);
					}
					int n = op.getNewRank();
					if (r == null) {
						if (n < 0 || n != l) { // all nulls be need to match rank as necessary
							f.set(m, Array.newInstance(o.getClass().getComponentType(), n < 0 ? l : n));
						}
						continue;
					}
					if (n < 0) {
						n = l;
					}
					Object narray = Array.newInstance(r.getClass(), n);
					for (int i = 0, si = 0, di = 0; di < n && si < l; i++) {
						int c = op.change(i);
						if (c == 0) {
							Array.set(narray, di++, processObject(op, Array.get(o, si++)));
						} else if (c > 0) {
							di += c; // add nulls by skipping forward in destination array
						} else if (c < 0) {
							si -= c; // remove dimensions by skipping forward in source array
						}
					}
					if (n == l) {
						for (int i = 0; i < l; i++) {
							Array.set(o, i, Array.get(narray, i));
						}
					} else {
						f.set(m, narray);
					}
				} else if (o instanceof List<?>) {
					List list = (List) o;
					int l = list.size();

					for (int i = 0; r == null && i < l; i++) {
						r = list.get(i);
					}
					int n = op.getNewRank();
					if (r == null) {
						if (n < 0 || n != l) { // all nulls be need to match rank as necessary
							list.clear();
							for (int i = 0, imax = n < 0 ? l : n; i < imax; i++) {
								list.add(null);
							}
						}
						continue;
					}

					if (n < 0) {
						n = l;
					}
					Object narray = Array.newInstance(r.getClass(), n);
					for (int i = 0, si = 0, di = 0; i < l && si < l; i++) {
						int c = op.change(i);
						if (c == 0) {
							Array.set(narray, di++, processObject(op, list.get(si++)));
						} else if (c > 0) {
							di += c; // add nulls by skipping forward in destination array
						} else if (c < 0) {
							si -= c; // remove dimensions by skipping forward in source array
						}
					}
					list.clear();
					for (int i = 0; i < n; i++) {
						list.add(Array.get(narray, i));
					}
				} else if (o instanceof Map<?,?>) {
					Map map = (Map) o;
					for (Object k : map.keySet()) {
						map.put(k, processObject(op, map.get(k)));
					}
				}
			} catch (Exception e) {
				logger.error("Problem occurred when processing metadata of class {}: {}", mc.getCanonicalName(), e);
				if (throwException) {
					throw new RuntimeException(e);
				} 
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object processObject(MetadatasetAnnotationOperation op, Object o) throws Exception {
		if (o == null) {
			return o;
		}

		if (o instanceof ILazyDataset) {
			try {
				return op.run((ILazyDataset) o);
			} catch (Exception e) {
				logger.error("Problem processing " + o, e);
				if (!catchExceptions) {
					throw e;
				}
			}
		} else if (o.getClass().isArray()) {
			int l = Array.getLength(o);
			for (int i = 0; i < l; i++) {
				Array.set(o, i, processObject(op, Array.get(o, i)));
			}
		} else if (o instanceof List<?>) {
			List list = (List) o;
			for (int i = 0, imax = list.size(); i < imax; i++) {
				list.set(i, processObject(op, list.get(i)));
			}
		} else if (o instanceof Map<?,?>) {
			Map map = (Map) o;
			for (Object k : map.keySet()) {
				map.put(k, processObject(op, map.get(k)));
			}
		}
		return o;
	}

	protected ILazyDataset createFromSerializable(Serializable blob, boolean keepLazy) {
		ILazyDataset d = null;
		if (blob instanceof ILazyDataset) {
			d = (ILazyDataset) blob;
			if (d instanceof IDataset) {
				Dataset ed = DatasetUtils.convertToDataset((IDataset) d);
				int is = ed.getElementsPerItem();
				if (is != 1 && is != getElementsPerItem()) {
					throw new IllegalArgumentException("Dataset has incompatible number of elements with this dataset");
				}
				d = ed.cast(is == 1 ? DoubleDataset.class: CompoundDoubleDataset.class);
			} else if (!keepLazy) {
				final int is = getElementsPerItem();
				try {
					d = DatasetUtils.cast(is == 1 ? DoubleDataset.class: CompoundDoubleDataset.class, d.getSlice());
				} catch (DatasetException e) {
					logger.error("Could not get data from lazy dataset", e);
					return null;
				}
			}
		} else {
			final int is = getElementsPerItem();
			if (is == 1) {
				d = DatasetFactory.createFromObject(DoubleDataset.class, blob);
			} else {
				try {
					d = DatasetFactory.createFromObject(is, CompoundDoubleDataset.class, blob);
				} catch (IllegalArgumentException e) { // if only single value supplied try again
					d = DatasetFactory.createFromObject(DoubleDataset.class, blob);
				}
			}
			if (d.getSize() == getSize() && !Arrays.equals(d.getShape(), shape)) {
				d.setShape(shape.clone());
			}
		}
		List<int[]> s = BroadcastUtils.broadcastShapesToMax(shape, d.getShape());
		d.setShape(s.get(0));

		return d;
	}

	@Override
	public void setErrors(Serializable errors) {
		if (shape == null) {
			throw new IllegalArgumentException("Cannot set errors for null dataset");
		}
		if (errors == null) {
			clearMetadata(ErrorMetadata.class);
			return;
		}
		if (errors == this) {
			logger.warn("Ignoring setting error to itself as this will lead to infinite recursion");
			return;
		}

		ILazyDataset errorData = createFromSerializable(errors, true);

		ErrorMetadata emd = getErrorMetadata();
		if (emd == null) {
			try {
				emd = MetadataFactory.createMetadata(ErrorMetadata.class);
				setMetadata(emd);
			} catch (MetadataException me) {
				logger.error("Could not create metadata", me);
			}
		}
		emd.setError(errorData);
	}

	protected ErrorMetadata getErrorMetadata() {
		try {
			List<ErrorMetadata> el = getMetadata(ErrorMetadata.class);
			if (el != null && !el.isEmpty()) {
				 return el.get(0);
			}
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	public ILazyDataset getErrors() {
		ErrorMetadata emd = getErrorMetadata();
		return emd == null ? null : emd.getError();
	}

	@Override
	public boolean hasErrors() {
		return LazyDatasetBase.this.getErrors() != null;
	}

	/**
	 * Check permutation axes
	 * @param shape
	 * @param axes
	 * @return cleaned up copy of axes or null if trivial
	 */
	public static int[] checkPermutatedAxes(int[] shape, int... axes) {
		int rank = shape == null ? 0 : shape.length;

		if (axes == null || axes.length == 0) {
			axes = new int[rank];
			for (int i = 0; i < rank; i++) {
				axes[i] = rank - 1 - i;
			}
		} else {
			axes = axes.clone();
		}

		if (axes.length != rank) {
			logger.error("axis permutation has length {} that does not match dataset's rank {}", axes.length, rank);
			throw new IllegalArgumentException("axis permutation does not match shape of dataset");
		}
	
		// check all permutation values are within bounds
		for (int i = 0; i < rank; i++) {
			axes[i] = ShapeUtils.checkAxis(rank, axes[i]);
		}
	
		// check for a valid permutation (is this an unnecessary restriction?)
		int[] perm = axes.clone();
		Arrays.sort(perm);

		for (int i = 0; i < rank; i++) {
			if (perm[i] != i) {
				logger.error("axis permutation is not valid: it does not contain complete set of axes");
				throw new IllegalArgumentException("axis permutation does not contain complete set of axes");
			}
		}

		if (Arrays.equals(axes, perm)) {
			return null; // signal identity or trivial permutation
		}

		return axes;
	}
}
