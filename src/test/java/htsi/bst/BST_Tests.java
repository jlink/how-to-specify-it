package htsi.bst;

import java.util.AbstractMap.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class BST_Tests {

	private final BST<Integer, String> bst = BST.nil();

	@Example
	void new_bst_is_empty() {
		assertThat(bst.isEmpty()).isTrue();
		assertThat(bst.size()).isEqualTo(0);
	}

	@Example
	void an_inserted_value_can_be_found() {
		BST<Integer, String> updated = bst.insert(3, "three");
		assertThat(updated.isEmpty()).isFalse();
		assertThat(updated.find(3)).isPresent();
		assertThat(updated.find(3).get()).isEqualTo("three");
		assertThat(updated.size()).isEqualTo(1);

		assertThat(bst.isEmpty()).isTrue();
	}

	@Example
	void a_replaced_value_can_be_found() {
		BST<Integer, String> first = bst.insert(3, "three");
		BST<Integer, String> updated = first.insert(3, "drei");
		assertThat(updated.find(3).get()).isEqualTo("drei");
		assertThat(first.find(3).get()).isEqualTo("three");
	}

	@Example
	void three_inserted_values_can_be_found() {
		BST<Integer, String> updated =
				bst.insert(10, "ten")
				   .insert(1, "one")
				   .insert(20, "twenty");

		assertThat(updated.isEmpty()).isFalse();
		assertThat(updated.size()).isEqualTo(3);
		assertThat(updated.find(1)).isPresent();
		assertThat(updated.find(10)).isPresent();
		assertThat(updated.find(20)).isPresent();

		assertThat(bst.isEmpty()).isTrue();
	}

	@Example
	void filled_with_same_values_in_same_order_are_equal() {
		BST<Integer, String> first =
				bst.insert(10, "ten")
				   .insert(1, "one")
				   .insert(20, "twenty");

		BST<Integer, String> second =
				bst.insert(10, "ten")
				   .insert(1, "one")
				   .insert(20, "twenty");

		assertThat(first.equals(second)).isTrue();
		assertThat(second.equals(first)).isTrue();

		BST<Integer, String> differentOrder =
				bst.insert(1, "one")
				   .insert(10, "ten")
				   .insert(20, "twenty");
		assertThat(first.equals(differentOrder)).isFalse();
		assertThat(differentOrder.equals(first)).isFalse();
	}

	@Example
	void toString_contains_all_keys_with_values() {
		BST<Integer, String> updated =
				bst.insert(10, "ten")
				   .insert(1, "one")
				   .insert(20, "twenty");

		String toString = updated.toString();
		System.out.println(toString);
		assertThat(toString).contains("10=ten");
		assertThat(toString).contains("1=one");
		assertThat(toString).contains("20=twenty");
	}

	@Example
	void a_deleted_value_can_no_longer_be_found() {
		BST<Integer, String> ten = bst.insert(10, "ten");
		assertThat(ten.delete(10).find(10)).isNotPresent();

		BST<Integer, String> ten_one = ten.insert(1, "one");
		assertThat(ten_one.delete(10).find(10)).isNotPresent();

		BST<Integer, String> ten_one_twenty = ten_one.insert(20, "twenty");

		BST<Integer, String> deleted1 = ten_one_twenty.delete(1);
		assertThat(deleted1.find(1)).isNotPresent();
		assertThat(deleted1.find(10)).isPresent();
		assertThat(deleted1.find(20)).isPresent();

		BST<Integer, String> deleted20 = ten_one_twenty.delete(20);
		assertThat(deleted20.find(20)).isNotPresent();
		assertThat(deleted20.find(1)).isPresent();
		assertThat(deleted20.find(10)).isPresent();

		BST<Integer, String> deleted10 = ten_one_twenty.delete(10);
		assertThat(deleted10.find(10)).isNotPresent();
		assertThat(deleted10.find(1)).isPresent();
		assertThat(deleted10.find(20)).isPresent();
	}

	@Example
	void keys_returns_set_of_inserted_keys() {
		BST<Integer, String> updated =
				bst.insert(1, "one")
				   .insert(2, "two")
				   .insert(3, "three");

		assertThat(updated.keys()).containsExactlyInAnyOrder(1, 2, 3);
	}

	@Example
	void toList_returns_key_value_pairs() {
		BST<Integer, String> updated =
				bst.insert(1, "one")
				   .insert(2, "two")
				   .insert(3, "three");

		assertThat(updated.toList()).containsExactlyInAnyOrder(
				new SimpleEntry<>(1, "one"),
				new SimpleEntry<>(2, "two"),
				new SimpleEntry<>(3, "three")
		);
	}

	@Example
	void union_of_two_bsts_contains_keys_of_both() {
		BST<Integer, String> one =
				bst.insert(1, "one")
				   .insert(2, "two")
				   .insert(3, "three");

		BST<Integer, String> two =
				bst.insert(4, "four")
				   .insert(5, "five")
				   .insert(3, "eerht");

		BST<Integer, String> union = BST.union(one, two);

		assertThat(union.toList()).containsExactlyInAnyOrder(
				new SimpleEntry<>(1, "one"),
				new SimpleEntry<>(2, "two"),
				new SimpleEntry<>(3, "three"),
				new SimpleEntry<>(4, "four"),
				new SimpleEntry<>(5, "five")
		);
	}

	@Example
	void insert_twice_still_valid() {
		BST<Integer, String> one = bst.insert(1, "one").insert(1, "two");
		assertThat(BSTUtils.isValid(one)).isTrue();
	}
}
