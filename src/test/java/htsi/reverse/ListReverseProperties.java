package htsi.reverse;

import java.util.*;

import net.jqwik.api.*;

class ListReverseProperties {

	@Property
	boolean reverseTwiceIsOriginal(@ForAll List<Integer> aList) {
		return reverse(reverse(aList)).equals(aList);
	}

	@Property(afterFailure = AfterFailureMode.RANDOM_SEED)
	boolean reverseKeepsTheOriginalList(@ForAll List<Integer> aList) {
		return reverse(aList).equals(aList);
	}

	<T> List<T> reverse(List<T> original) {
		List<T> clone = new ArrayList<>(original);
		Collections.reverse(clone);
		return clone;
	}
}
