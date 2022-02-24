package info.kgeorgiy.ja.Zaitsev.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final List<E> arr;
    private final Comparator<? super E> cmp;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        TreeSet<E> temp = new TreeSet<>(comparator);
        temp.addAll(collection);
        arr = new ArrayList<>(temp);
        cmp = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(arr).iterator();
    }

    @Override
    public int size() {
        return arr.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return cmp;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, toElement, false);
    }

    @SuppressWarnings("unchecked")
    private int compare(E a, E b) {
        if (comparator() == null) {
            return ((Comparable<? super E>) a).compareTo(b);
        }
        return comparator().compare(a, b);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        if (isEmpty() || compare(toElement, first()) < 0) {
            return new ArraySet<>(Collections.emptyList(), cmp);
        }
        return subSet(first(), toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        if (isEmpty() || compare(last(), fromElement) < 0) {
            return new ArraySet<>(Collections.emptyList(), cmp);
        }
        return subSet(fromElement, last(), true);
    }

    @Override
    public E first() {
        if (arr.isEmpty()) {
            throw new NoSuchElementException();
        }
        return arr.get(0);
    }

    @Override
    public E last() {
        if (this.isEmpty()) {
            throw new NoSuchElementException();
        }
        return arr.get(arr.size() - 1);
    }

    private int findElement(E elem, int foundShift, int notFoundShift) {
        int pos = Collections.binarySearch(arr, elem, cmp);
        if (pos < 0) {
            pos = -pos - 1 + foundShift;
        } else {
            pos = pos + notFoundShift;
        }
        return pos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(arr, (E) o, cmp) >= 0;
    }

    private SortedSet<E> subSet(E fromElement, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement=" + fromElement + " should be less or equal than toElement=" + toElement);
        }
//        int l = findElement(fromElement, fromInclusive ? 0 : 1, fromInclusive ? 0 : 1);
        int l = findElement(fromElement, 0, 0);
        int r = findElement(toElement, toInclusive ? 1 : 0, toInclusive ? 1 : 0);
        if (0 <= l && l <= r && r <= size()) {
            return new ArraySet<>(arr.subList(l, r), cmp);
        } else {
            return new ArraySet<>(Collections.emptyList(), cmp);
        }
    }

}
