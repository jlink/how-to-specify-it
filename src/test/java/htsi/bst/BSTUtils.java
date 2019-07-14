package htsi.bst;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

class BSTUtils {

	public static <K extends Comparable<K>, V> boolean isValid(BST<K, V> bst) {
		if (bst.isLeaf()) {
			return true;
		}
		return isValid(bst.left()) && isValid(bst.right())
					   && keys(bst.left()).allMatch(k -> k.compareTo(bst.key()) < 0)
					   && keys(bst.right()).allMatch(k -> k.compareTo(bst.key()) > 0);
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private static <K extends Comparable<K>, V> Stream<K> keys(Optional<BST<K, V>> bst) {
		return bst.map(BST::keys).orElse(Collections.emptyList()).stream();
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private static <K extends Comparable<K>, V> boolean isValid(Optional<BST<K, V>> optionalBST) {
		return optionalBST.map(BSTUtils::isValid).orElse(true);
	}

	@SuppressWarnings("unchecked")
	public static boolean equivalent(BST bst1, BST bst2) {
		return new HashSet<>(bst1.toList()).equals(new HashSet(bst2.toList()));
	}

	// insertions Leaf = [ ]
	// insertions (Branch l k v r ) = (k , v ) : insertions l + insertions r
	public static <K extends Comparable<K>, V> List<Entry<K, V>> insertions(BST<K, V> bst) {
		if (bst.isLeaf()) {
			return Collections.emptyList();
		}
		List<Entry<K, V>> insertions = new ArrayList<>();
		insertions.add(bst.entry);
		bst.left().ifPresent(left -> insertions.addAll(insertions(left)));
		bst.right().ifPresent(right -> insertions.addAll(insertions(right)));
		return insertions;
	}
}
