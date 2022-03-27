package utils; /*******************************************************************************
 * Lmdp* - L*-Based Learning of Markov Decision Processes
 *  Copyright (C) 2019 TU Graz
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/


/**
 * This is an immutable pair of values, an adapted version of 
 * <code>org.apache.commons.lang3.tuple.ImmutablePair</code>.
 * 
 * It caches its hash-value for fast usage as key in hash maps.
 *  
 * @author Martin Tappler
 *
 * @param <L> type of left value
 * @param <R> type of right value
 */
public class FastImmPair<L, R> {

    public final L left;
    public final R right;
    private Integer hashCache = null;

    public static <L, R> FastImmPair<L, R> of(final L left, final R right) {
        return new FastImmPair<>(left, right);
    }

  
    public FastImmPair(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

 
    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }


	@Override
	public int hashCode() {
		if(hashCache != null)
			return hashCache;
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		hashCache = result;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(obj.hashCode() != this.hashCode())
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		FastImmPair other = (FastImmPair) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "<" + left + "," + right + ">";
	}

    
}
