package com.github.ars_zero.common.explosion;

public final class ExplosionWorkList {
    private long[] positions;
    private int[] dist2;
    private int size;

    public ExplosionWorkList(int initialCapacity) {
        int cap = Math.max(16, initialCapacity);
        this.positions = new long[cap];
        this.dist2 = new int[cap];
    }

    public int size() {
        return size;
    }

    public long positionAt(int index) {
        return positions[index];
    }

    public void add(long position, int distanceSquared) {
        ensureCapacity(size + 1);
        positions[size] = position;
        dist2[size] = distanceSquared;
        size++;
    }

    public void sortByDistanceAscending() {
        if (size <= 1) {
            return;
        }
        quickSort(0, size - 1);
    }

    private void ensureCapacity(int needed) {
        if (needed <= positions.length) {
            return;
        }
        int newCap = Math.max(needed, positions.length + (positions.length >> 1));
        long[] newPositions = new long[newCap];
        int[] newDist2 = new int[newCap];
        System.arraycopy(positions, 0, newPositions, 0, size);
        System.arraycopy(dist2, 0, newDist2, 0, size);
        positions = newPositions;
        dist2 = newDist2;
    }

    private void quickSort(int left, int right) {
        int i = left;
        int j = right;
        int pivot = dist2[(left + right) >>> 1];

        while (i <= j) {
            while (dist2[i] < pivot) {
                i++;
            }
            while (dist2[j] > pivot) {
                j--;
            }
            if (i <= j) {
                swap(i, j);
                i++;
                j--;
            }
        }

        if (left < j) {
            quickSort(left, j);
        }
        if (i < right) {
            quickSort(i, right);
        }
    }

    private void swap(int a, int b) {
        long p = positions[a];
        positions[a] = positions[b];
        positions[b] = p;

        int d = dist2[a];
        dist2[a] = dist2[b];
        dist2[b] = d;
    }
}

