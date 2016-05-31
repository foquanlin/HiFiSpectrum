package app.android.hifispectrum.proc;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Bartosz on 19/05/2015.
 */
public class PushQueue<T> implements Iterable<T> {

    private List<T> _list;
    private int _index;
    private int _capacity;

    public PushQueue(int capacity, T defaultValue) {
        _capacity = capacity;
        _list = new ArrayList<T>(capacity);
        _index = 0;

        for(int i=0;i<capacity;i++) {
            _list.add(defaultValue);
        }
    }

    public void push(T item) {
        _list.set(_index, item);
        _index = (_index + 1) % _capacity;
    }

    public void pushRange(Iterable<T> items) {
        for(T item : items) {
            push(item);
        }
    }

    @Override
    public Iterator<T> iterator() {
        PushQueueIterator<T> iterator = new PushQueueIterator<T>(_list, _capacity - _index - 1);
        return iterator;
    }

    private static class PushQueueIterator<T> implements Iterator<T> {

        private List<T> _list;
        private int _index;
        private int _endPosition;
        private int _capacity;

        public PushQueueIterator(List<T> list, int index) {
            _list = list;
            _capacity = list.size();
            _index = index;
            _endPosition = index-1;
            if(_endPosition < 0) {
                _endPosition += _capacity;
            }
        }

        @Override
        public boolean hasNext() {
            boolean hasNext =  _index != _endPosition;
            return hasNext;
        }

        @Override
        public T next() {
            _index = ((_index+1) % _capacity);
            return _list.get(_index);
        }

        @Override
        public void remove() {
            return;
        }
    }

}
