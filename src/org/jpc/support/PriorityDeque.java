/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 
    Details (including contact information) can be found at: 

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.support;

import java.util.*;

/**
 * An unbounded priority {@linkplain Deque deque} based on a twin-heap structure
 * stored in a linear array.
 * <p>
 * This implementation provides O(log(n)) time for insertion and tail or head
 * removal.
 * 
 * @see "S. C. Chang, M. W. Du, Diamond deque: A simple data structure for priority deques, Information Processing Letters, v.46 n.5, p.231-237, July 9, 1993"
 * @author Chris Dennis
 * @param <E> the type of elements held in this collection 
 */
public class PriorityDeque<E> extends AbstractQueue<E> implements Deque<E>
{
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    
    private transient Object[] queue;
    
    private int size = 0;

    private transient int modCount = 0;

    /**
     * Constructs an empty <code>PriorityDeque</code> with a default initial
     * capacity.
     */
    public PriorityDeque()
    {
	this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a <code>PriorityDeque</code> and fills it with the given
     * collection of elements. 
     * @param c elements to be inserted
     */
    public PriorityDeque(Collection<? extends E> c)
    {
        this(c.isEmpty() ? DEFAULT_INITIAL_CAPACITY : c.size());
        addAll(c);
    }
    
    /**
     * Constructs a <code>PriorityDeque</code> with the given initial capacity.
     * @param initialCapacity initial internal storage size
     */
    public PriorityDeque(int initialCapacity)
    {
	if (initialCapacity < 1)
	    throw new IllegalArgumentException();
	this.queue = new Object[initialCapacity];        
    }
        
    public int size()
    {
	return size;
    }

    @Override
    public boolean contains(Object o)
    {
	return indexOf(o) != -1;
    }

    @Override
    public boolean remove(Object o)
    {
	int i = indexOf(o);
	if (i == -1)
	    return false;
	else {
	    removeAt(i);
	    return true;
	}
    }

    public E pop()
    {
	return removeFirst();
    }

    public void push(E o)
    {
	add(o);
    }

    public E peek()
    {
	return peekFirst();
    }

    public E poll()
    {
	return pollFirst();
    }

    public boolean offerLast(E o)
    {
	return offer(o);
    }

    public boolean offerFirst(E o)
    {
	return offer(o);
    }

    public void addLast(E o)
    {
	add(o);
    }

    public void addFirst(E o)
    {
	add(o);
    }

    public E getFirst()
    {
	return element();
    }

    public E removeFirst()
    {
	return remove();
    }

    public boolean offer(E o)
    {
	if (o == null)
	    throw new NullPointerException();
	modCount++;
	int i = size;
	if (i >= queue.length)
	    grow(i + 1);
	size = i + 1;
	if (i == 0)
	    queue[0] = o;
	else {
	    int idx = siftUp(i, o);
	    siftDown(idx, o);
	}
	return true;
    }

    public E pollFirst() {
        if (size == 0)
            return null;
        int s = --size;
        modCount++;
        E result = (E)queue[0];
        E x = (E)queue[s];
        queue[s] = null;
        if (s > 1)
            siftDown(0, x);
	else
	    queue[0] = x;

        return result;
    }

    public E peekFirst() {
        if (size == 0)
            return null;
        return (E)queue[0];
    }

    public E pollLast() {
	if (size < 2)
	    return pollFirst();

        int s = --size; //1
        modCount++;
        E result = (E)queue[1];
        E x = (E)queue[s];

	queue[s] = null;
	if (s > 1)
	    siftUp(1, x);

        return result;
    }

    public E peekLast() {
        if (size == 0)
            return null;
	if (size == 1)
	    return peekFirst();
	
	return (E)queue[1];
    }

    public E getLast() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    public boolean removeFirstOccurrence(Object o)
    {
        int index = -1;
        Object obj = null;
        
        for (int i = 0; i < size; i++)
            if (o.equals(queue[i])) {
                Comparable x = (Comparable)queue[i];
                if ((obj == null) || (x.compareTo(obj) < 0)) {
                    obj = x;
                    index = i;
                }
            }

        if (index >= 0) {
            removeAt(index);
            return true;
        } else
            return false;        
    }

    public boolean removeLastOccurrence(Object o)
    {
        int index = -1;
        Object obj = null;
        
        for (int i = 0; i < size; i++)
            if (o.equals(queue[i])) {
                Comparable x = (Comparable)queue[i];
                if ((obj == null) || (x.compareTo(obj) > 0)) {
                    obj = x;
                    index = i;
                }
            }

        if (index >= 0) {
            removeAt(index);
            return true;
        } else
            return false;        
    }
    
    public E removeLast() {
        E x = pollLast();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    private void grow(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new IllegalStateException("Oversized Collection");
	int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = ((oldCapacity < 64)?
                           ((oldCapacity + 1) * 2):
                           ((oldCapacity / 2) * 3));
        if (newCapacity < 0) // overflow
            newCapacity = Integer.MAX_VALUE;
        if (newCapacity < minCapacity)
            newCapacity = minCapacity;
        Object[] temp = new Object[newCapacity];
        System.arraycopy(queue, 0, temp, 0, queue.length);
        queue = temp;
    }

    private int indexOf(Object o) {
	if (o != null) {
            for (int i = 0; i < size; i++)
                if (o.equals(queue[i]))
                    return i;
        }
        return -1;
    }

    private E removeAt(int i) {
        assert i >= 0 && i < size;
        modCount++;
        int s = --size;
        if (s == i) // removed last element
            queue[i] = null;
        else {
            E moved = (E)queue[s];
            queue[s] = null;
	    if (s > 1) {
		siftDown(i, moved);
		if (queue[i] == moved) {
		    siftUp(i, moved);
		    if (queue[i] != moved)
			return moved;
		}
	    } else
		queue[0] = moved;
        }
        return null;
    }

    private int siftUp(int k, E x)
    {
	Comparable key = (Comparable) x;
	while (k != 0) {
	    int predecessor = findPredecessor(k);
	    assert predecessor < size : "Predecessor Outside Limit: " + predecessor;
	    E o = (E)queue[predecessor];
	    if (key.compareTo(o) >= 0)
		break;
	    queue[k] = o;
	    k = predecessor;
	}
	queue[k] = key;
	return k;
    }
    
    private int siftDown(int k, E x)
    {
	Comparable key = (Comparable)x;
	while (k != 1) {
	    int successor = findSuccessor(k);
	    assert successor < size : "Successor Outside Limit: " + successor;
	    E o = (E)queue[successor];
	    if (key.compareTo(o) <= 0)
		break;
	    queue[k] = o;
	    k = successor;
	}
	queue[k] = key;
	return k;
    }

    private int findPredecessor(int index)
    {
	if (index == 0)
	    return 0;

	if ((index & 0x1) == 0) { // bubble is in min-heap
	    if ((index & 0x2) == 0)
		return (index >>> 1) - 2; //B0
	    else
		return (index >>> 1) - 1; //B1
	} else { // bubble is in max-heap
	    int leftIP = (index << 1) + 1;
	    int rightIP = leftIP + 2;
	    if (rightIP < size) { //B2
		Comparable left = (Comparable)queue[leftIP];
		if (left.compareTo(queue[rightIP]) >= 0)
		    return leftIP;
		else
		    return rightIP;
	    } else if (leftIP < size)
		return leftIP; //B3 + B4
	    else
		return index - 1; //B5 + B6 + B7
	}
    }

    private int findSuccessor(int index)
    {
	if (index == 1)
	    return 1;

	if ((index & 0x1) == 0) { // bubble is in min-heap
	    int leftIS = (index << 1) + 2;
	    int rightIS = leftIS + 2;
	    if (rightIS < size) { //T0
		Comparable left = (Comparable)queue[leftIS];
		if (left.compareTo(queue[rightIS]) <= 0)
		    return leftIS;
		else
		    return rightIS;
	    } else if (leftIS < size)
		return leftIS; //T3
	    else if (index + 1 < size)
		return index + 1;
	    else
		return findSuccessor(index + 1);
	} else { // bubble is in max-heap
	    if ((index & 0x2) == 0)
		return (index >>> 1) - 1; //T2
	    else
		return (index >>> 1); //T1
	}
    }

    public Iterator<E> iterator()
    {
        final Object[] sorted = new Object[size];
        System.arraycopy(queue, 0, sorted, 0, sorted.length);
        Arrays.sort(sorted);
        return new ArrayIterator<E>(sorted);
    }
    
    public Iterator<E> descendingIterator()
    {
        final Object[] sorted = new Object[size];
        System.arraycopy(queue, 0, sorted, 0, sorted.length);
        Arrays.sort(sorted);
        return new PriorityDeque.ArrayReverseIterator<E>(sorted);
    }
    
    private static class ArrayIterator<E> implements Iterator<E>
    {
        private final Object[] values;
        private int offset;
        
        public ArrayIterator(Object[] data)
        {
            values = data;
        }

        public boolean hasNext()
        {
            return offset < values.length;
        }

        public E next()
        {
            try {
                return (E)values[offset++];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Iterator.remove() not supported");
        }
        
    }

    private static class ArrayReverseIterator<E> implements Iterator<E>
    {
        private final Object[] values;
        private int offset;
        
        public ArrayReverseIterator(Object[] data)
        {
            values = data;
            offset = values.length - 1;
        }

        public boolean hasNext()
        {
            return offset >= 0;
        }

        public E next()
        {
            try {
                return (E)values[offset--];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Iterator.remove() not supported");
        }
        
    }    
}
