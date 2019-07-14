package htsi.bst;

import java.io.*;
import java.util.AbstractMap.*;
import java.util.*;

public class BST<K extends Comparable<K>, V> implements Serializable {

	private static final BST NIL = new BST<>();

	//	nil :: BST k v
	public static <K extends Comparable<K>, V> BST<K, V> nil() {
		//noinspection unchecked
		return BST.NIL;
	}

	//	union :: Ord k ⇒ BST k v → BST k v → BST k v
	public static <K extends Comparable<K>, V> BST<K, V> union(BST<K, V> bst1, BST<K, V> bst2) {
		// bug(8)
		// BST<K, V> union = bst1;
		// for (Map.Entry<K, V> entry : bst2.toList()) {
		// 	union = union.insert(entry);
		// }
		BST<K, V> union = bst2;
		for (Map.Entry<K, V> entry : bst1.toList()) {
			union = union.insert(entry);
		}
		return union;
	}

	private final BST<K, V> left;
	final Map.Entry<K, V> entry;
	private final BST<K, V> right;

	private BST() {
		this(nil(), null, nil());
	}

	private BST(BST<K, V> left, Map.Entry<K, V> entry, BST<K, V> right) {
		this.left = left;
		this.entry = entry;
		this.right = right;
	}

	public K key() {
		return entry == null ? null : entry.getKey();
	}

	public V value() {
		return entry == null ? null : entry.getValue();
	}

	public Optional<BST<K, V>> left() {
		if (getLeft().isLeaf())
			return Optional.empty();
		return Optional.of(left);
	}

	public Optional<BST<K, V>> right() {
		if (getRight().isLeaf())
			return Optional.empty();
		return Optional.of(right);
	}

	public boolean isLeaf() {
		return entry == null;
	}

	public boolean isEmpty() {
		return entry == null;
	}

	public int size() {
		if (isLeaf()) {
			return 0;
		}
		return 1 + getLeft().size() + getRight().size();
	}

	//	find ::Ord k ⇒k →BST k v →Maybe v
	public Optional<V> find(K key) {
		if (isLeaf()) {
			return Optional.empty();
		}
		if (entry.getKey().compareTo(key) > 0) {
			return getLeft().find(key);
		}
		if (entry.getKey().compareTo(key) < 0) {
			return getRight().find(key);
		}
		return Optional.of(entry.getValue());
	}

	//	insert :: Ord k ⇒ k → v → BST k v → BST k v
	public BST<K, V> insert(K key, V value) {
		SimpleImmutableEntry<K, V> newEntry = new SimpleImmutableEntry<>(key, value);
		return insert(newEntry);
	}

	@SuppressWarnings("unchecked")
	private BST<K, V> insert(Map.Entry<K, V> newEntry) {
		BST<K, V> branch = new BST<>(NIL, newEntry, NIL);
		return insert(branch);
	}

	private BST<K, V> insert(BST<K, V> branch) {
		// bug(1):
		// return branch;
		if (isLeaf()) {
			return branch;
		}
		if (this.entry.getKey().compareTo(branch.entry.getKey()) > 0) {
			return new BST<>(getLeft().insert(branch), this.entry, right);
		}
		if (this.entry.getKey().compareTo(branch.entry.getKey()) < 0) {
			return new BST<>(left, this.entry, getRight().insert(branch));
		}
		// bug(2):
		// return new BST<>(left, entry, getRight().insert(branch));
		// bug(3):
		// return this;
		return new BST<>(left, branch.entry, right);
	}

	private BST<K, V> getRight() {
		return this.right == null ? NIL : this.right;
	}

	private BST<K, V> getLeft() {
		return this.left == null ? NIL : this.left;
	}

	//	delete::Ord k ⇒k →BST k v →BST k v
	public BST<K, V> delete(K key) {
		if (isLeaf()) {
			return this;
		}
		// bug(5)
		// if (entry.getKey().compareTo(key) < 0) {
		if (entry.getKey().compareTo(key) > 0) {
			return new BST<>(getLeft().delete(key), entry, right);
		}
		// bug(5)
		// if (entry.getKey().compareTo(key) > 0) {
		if (entry.getKey().compareTo(key) < 0) {
			return new BST<>(left, entry, getRight().delete(key));
		}
		if (getLeft().isLeaf()) {
			return right;
		}
		if (getRight().isLeaf()) {
			return left;
		}
		return right.insert(getLeft());
	}

	//	bug(4)
	// public BST<K, V> delete(K key) {
	// 	if (isLeaf()) {
	// 		return this;
	// 	}
	// 	if (entry.getKey().compareTo(key) > 0) {
	// 		return getLeft().delete(key);
	// 	}
	// 	if (entry.getKey().compareTo(key) < 0) {
	// 		return getRight().delete(key);
	// 	}
	// 	if (getLeft().isLeaf()) {
	// 		return right;
	// 	}
	// 	if (getRight().isLeaf()) {
	// 		return left;
	// 	}
	// 	return right.insert(getLeft());
	// }

	//	keys ::BSTkv→[k]
	public List<K> keys() {
		if (entry == null) {
			return new ArrayList<>();
		}
		List<K> keys = new ArrayList<>();
		keys.add(entry.getKey());
		keys.addAll(getLeft().keys());
		keys.addAll(getRight().keys());
		return keys;
	}

	//	toList :: BST k v → [ (k , v ) ]
	public List<Map.Entry<K, V>> toList() {
		if (entry == null) {
			return new ArrayList<>();
		}
		List<Map.Entry<K, V>> entries = new ArrayList<>();
		entries.add(entry);
		entries.addAll(getLeft().toList());
		entries.addAll(getRight().toList());
		return entries;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BST<?, ?> bst = (BST<?, ?>) o;
		if (!Objects.equals(getLeft(), bst.getLeft())) return false;
		if (!Objects.equals(entry, bst.entry)) return false;
		return Objects.equals(getRight(), bst.getRight());
	}

	@Override
	public int hashCode() {
		if (entry == null) {
			return 0;
		}
		int result = getLeft().hashCode();
		result = 31 * result + entry.hashCode();
		result = 31 * result + getRight().hashCode();
		return result;
	}

	@Override
	public String toString() {
		if (isLeaf()) {
			return "NIL";
		}
		String leftString = getLeft().isLeaf() ? "" : " left: " + getLeft().toString();
		String rightString = getRight().isLeaf() ? "" : " right: " + getRight().toString();
		return String.format(
				"[%s%s%s]",
				entry.toString(),
				leftString,
				rightString
		);
	}
}
