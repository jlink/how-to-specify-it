package htsi.bst;

import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;

import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.statistics.Statistics;

import static htsi.bst.BSTUtils.*;

class BST_Properties {

	@Group
	class Validity {
		@Property
		boolean arbitrary_valid(@ForAll("trees") BST<Integer, Integer> bst) {
			return isValid(bst);
		}

		@Example
		boolean nil_valid() {
			BST<?, ?> nil = BST.nil();
			return isValid(nil);
		}

		@Property
		boolean insert_valid(
				@ForAll("trees") BST<Integer, Integer> bst,
				@ForAll Integer key
		) {
			return isValid(bst.insert(key, 42));
		}

		@Property
		boolean delete_valid(
				@ForAll("trees") BST<Integer, Integer> bst,
				@ForAll Integer key
		) {
			// Assume.that(isValid(bst));
			return isValid(bst.delete(key));
		}

		@Property
		boolean union_valid(
				@ForAll("trees") BST<Integer, Integer> bst,
				@ForAll("trees") BST<Integer, Integer> other
		) {
			return isValid(BST.union(bst, other));
		}
	}

	@Group
	class Postconditions {

		// prop_InsertPost k v t k′ =
		//   find k′ (insert k v t) === if k ≡ k′ then Just v else find k′ t
		@Property
		boolean insert_post(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst,
				@ForAll Integer otherKey
		) {
			Optional<Integer> found = bst.insert(key, value).find(otherKey);
			Optional<Integer> expected =
					otherKey.equals(key) ? Optional.of(value) : bst.find(otherKey);
			return found.equals(expected);
		}

		@Property
		boolean delete_post(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return bst.delete(key).find(key).equals(Optional.empty());
		}

		@Property
		boolean insert_post_same_key(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return insert_post(key, value, bst, key);
		}

		// prop_UnionPost t t′ k = find k (union t t′) === (find k t <|> find k t′)
		@Property
		boolean union_post(
				@ForAll("trees") BST<Integer, Integer> left,
				@ForAll("trees") BST<Integer, Integer> right,
				@ForAll Integer key
		) {
			BST<Integer, Integer> union = BST.union(left, right);
			Integer previousValue = left.find(key).orElse(right.find(key).orElse(null));
			Integer unionValue = union.find(key).orElse(null);
			return Objects.equals(unionValue, previousValue);
		}

		@Property
		boolean find_post_present(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return bst.insert(key, value).find(key).equals(Optional.of(value));
		}

		@Property
		boolean find_post_absent(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return bst.delete(key).find(key).equals(Optional.empty());
		}

		// prop_InsertDeleteComplete k t = case find k t of
		//   Nothing → t === delete k t
		//   Just v →t ===insert k v t
		@Property
		boolean insert_delete_complete(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			Optional<Integer> found = bst.find(key);
			if (!found.isPresent()) {
				return bst.equals(bst.delete(key));
			} else {
				return bst.equals(bst.insert(key, found.get()));
			}
		}
	}

	@Group
	class Metamorphic {

		//prop_InsertInsert (k, v) (k′, v′) t =
		//  insert k v (insert k′ v′ t) === insert k′ v′ (insert k v t)
		@Property
		@Disabled
		boolean insert_insert1(
				@ForAll Integer key1, @ForAll Integer value1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return bst.insert(key1, value1).insert(key2, value2)
					  .equals(bst.insert(key2, value2).insert(key1, value1));
		}

		// prop_InsertInsert (k, v) (k′, v′) t = insert k v (insert k′ v′ t)
		//   ===
		//   if k ≡ k′ then insert k v t else insert k′ v′ (insert k v t)
		@Property
		boolean insert_insert(
				@ForAll Integer key1, @ForAll Integer value1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			BST<Integer, Integer> inserted = bst.insert(key1, value1).insert(key2, value2);
			BST<Integer, Integer> expected =
					key1.equals(key2)
							? bst.insert(key2, value2)
							: bst.insert(key2, value2).insert(key1, value1);
			return equivalent(inserted, expected);
		}

		@Property
		boolean insert_insert_weak(
				@ForAll Integer key1, @ForAll Integer value1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			Assume.that(!key1.equals(key2));
			return equivalent(
					bst.insert(key1, value1).insert(key2, value2),
					bst.insert(key2, value2).insert(key1, value1)
			);
		}

		// prop_InsertDelete (k,v) k′ t =
		//   insert k v (delete k′ t)
		//   􏰂equivalent
		//   if k ≡ k′ then insert k v t else delete k′ (insert k v t)
		@Property
		boolean insert_delete(
				@ForAll Integer key1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			BST<Integer, Integer> deleted = bst.delete(key1).insert(key2, value2);
			BST<Integer, Integer> expected =
					key1.equals(key2)
							? bst.insert(key2, value2)
							: bst.insert(key2, value2).delete(key1);
			return equivalent(deleted, expected);
		}

		@Property
		boolean insert_delete_weak(
				@ForAll Integer key1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			Assume.that(!key1.equals(key2));
			return equivalent(
					bst.delete(key1).insert(key2, value2),
					bst.insert(key2, value2).delete(key1)
			);
		}

		// prop_InsertUnion (k, v) t t′ =
		//   insert k v (union t t′) 􏰂
		//   equivalent
		//   union (insert k v t) t′
		@Property
		boolean insert_union(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			BST<Integer, Integer> unionInsert = BST.union(bst1, bst2).insert(key, value);
			BST<Integer, Integer> insertUnion = BST.union(bst1.insert(key, value), bst2);
			return equivalent(unionInsert, insertUnion);
		}

		// prop_DeleteInsertWeak k (k′, v′) t = k ̸≡ k′ =⇒ delete k (insert k′ v′ t) 􏰂 insert k′ v′ (delete k t)
		@Property
		boolean delete_insert_weak(
				@ForAll Integer key1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			Assume.that(!key1.equals(key2));
			return equivalent(
					bst.insert(key2, value2).delete(key1),
					bst.delete(key1).insert(key2, value2)
			);
		}

		// prop_DeleteNil k = delete k nil === (nil :: Tree)
		@Property
		boolean delete_nil(@ForAll Integer key) {
			BST<Integer, Integer> nil = BST.nil();
			return nil.delete(key).equals(nil);
		}

		// prop_DeleteInsert k (k′, v′) t = delete k (insert k′ v′ t)
		//   􏰂if k ≡ k′ then delete k t else insert k′ v′ (delete k t)
		@Property
		boolean delete_insert(
				@ForAll Integer key1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			BST<Integer, Integer> inserted = bst.insert(key2, value2).delete(key1);
			BST<Integer, Integer> expected =
					key1.equals(key2)
							? bst.delete(key1)
							: bst.delete(key1).insert(key2, value2);
			return equivalent(inserted, expected);
		}

		// prop_DeleteDelete k k′ t = delete k (delete k′ t) =eqv=􏰂 delete k′ (delete k t)
		@Property
		boolean delete_delete(
				@ForAll Integer key1,
				@ForAll Integer key2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return equivalent(
					bst.delete(key2).delete(key1),
					bst.delete(key1).delete(key2)
			);
		}

		// prop_DeleteUnion k t t′ =
		//   delete k (union t t′) 􏰂=eqv= union (delete k t) (delete k t′)
		@Property
		boolean delete_union(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			return equivalent(
					BST.union(bst1, bst2).delete(key),
					BST.union(bst1.delete(key), bst2.delete(key))
			);
		}

		// prop_UnionNil1 t = union nil t === t
		@Property
		boolean union_nil1(@ForAll("trees") BST<Integer, Integer> bst) {
			return BST.union(bst, BST.nil()).equals(bst);
		}

		// prop_UnionNil2 t = union t nil === t
		@Property
		boolean union_nil2(@ForAll("trees") BST<Integer, Integer> bst) {
			return BST.union(BST.nil(), bst).equals(bst);
		}

		// prop_UnionDeleteInsert t t′ (k,v) =
		//   union (delete k t) (insert k v t′) 􏰂 =eqv= insert k v (union t t′)
		@Property
		boolean union_delete_insert(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			return equivalent(
					BST.union(bst1.delete(key), bst2.insert(key, value)),
					BST.union(bst1, bst2).insert(key, value)
			);
		}

		// prop_UnionUnionIdem t = union t t 􏰂=eqv= t
		@Property
		boolean union_union_idem(@ForAll("trees") BST<Integer, Integer> bst) {
			return BST.union(bst, bst).equals(bst);
		}

		// prop_UnionUnionAssoc t1 t2 t3 =
		//   union (union t1 t2 ) t3 === union t1 (union t2 t3 )
		@Property
		boolean union_union_assoc(
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2,
				@ForAll("trees") BST<Integer, Integer> bst3
		) {
			BST<Integer, Integer> left = BST.union(BST.union(bst1, bst2), bst3);
			BST<Integer, Integer> right = BST.union(bst1, BST.union(bst2, bst3));
			return left.equals(right);
		}

		// prop_FindNil k = find k (nil :: Tree) === Nothing
		@Property
		boolean find_nil(@ForAll Integer key) {
			BST<Integer, Integer> nil = BST.nil();
			return nil.find(key).equals(Optional.empty());
		}

		// prop_FindInsert k (k′, v′) t =
		//   find k (insert k′ v′ t) === if k ≡ k′ then Just v′ else find k t
		@Property
		boolean find_insert(
				@ForAll Integer key1,
				@ForAll Integer key2, @ForAll Integer value2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			Optional<Integer> found = bst.insert(key2, value2).find(key1);
			Optional<Integer> expected =
					key1.equals(key2)
							? Optional.of(value2)
							: bst.find(key1);
			return found.equals(expected);
		}

		// prop_FindDelete k k′ t =
		//   find k (delete k′ t) === if k ≡ k′ then Nothing else find k t
		@Property
		boolean find_delete(
				@ForAll Integer key1,
				@ForAll Integer key2,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			Optional<Integer> found = bst.delete(key2).find(key1);
			Optional<Integer> expected =
					key1.equals(key2)
							? Optional.empty()
							: bst.find(key1);
			return found.equals(expected);
		}

		// prop_FindUnion k t t′ = find k (union t t′) === (find k t <|> find k t′)
		@Property
		boolean find_union(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			Optional<Integer> found = BST.union(bst1, bst2).find(key);
			Optional<Integer> expected = bst1.find(key).isPresent() ?
												 bst1.find(key) : bst2.find(key);
			return found.equals(expected);
		}

	}

	@Group
	class Equivalence {

		// prop_InsertPreservesEquiv k v t t′ =
		//   t =eqv= t′ =⇒ insert k v t =eqv= insert k v t′
		@Property
		@Disabled("Does not generate enough examples with assumption fulfilled")
		boolean insert_preserves_equivalence_exhausted(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			Assume.that(equivalent(bst1, bst2));
			return equivalent(
					bst1.insert(key, value),
					bst2.insert(key, value)
			);
		}

		// prop_InsertPreservesEquiv k v (t :􏰂: t′) = insert k v t 􏰂 insert k v t′
		@Property
		boolean insert_preserves_equivalence(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts
		) {
			return equivalent(
					bsts.get1().insert(key, value),
					bsts.get2().insert(key, value)
			);
		}

		// prop_DeletePreservesEquiv k (t :􏰂: t′) = delete k t 􏰂 delete k t′
		@Property
		boolean delete_preserves_equivalence(
				@ForAll Integer key,
				@ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts
		) {
			return equivalent(
					bsts.get1().delete(key),
					bsts.get2().delete(key)
			);
		}

		// prop_UnionPreservesEquiv (t1 :􏰂: t1′) (t2 :􏰂: t2′) = union t1 t2 􏰂 union t1′ t2′
		@Property
		boolean union_preserves_equivalence(
				@ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts1,
				@ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts2
		) {
			return equivalent(
					BST.union(bsts1.get1(), bsts2.get1()),
					BST.union(bsts1.get2(), bsts2.get2())
			);
		}

		// prop_FindPreservesEquiv k (t :􏰂: t′) = find k t === find k t′
		@Property
		boolean find_preserves_equivalence(
				@ForAll Integer key,
				@ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts
		) {
			return bsts.get1().find(key).equals(bsts.get2().find(key));
		}

		// prop_Equivs (t :􏰂:t′)=t 􏰂t′
		@Property
		boolean equivalent_trees_are_equivalent(
				@ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts
		) {
			return equivalent(bsts.get1(), bsts.get2());
		}

		@Provide
		Arbitrary<Tuple2<BST, BST>> equivalentTrees() {
			Arbitrary<Integer> keys = Arbitraries.integers().unique();
			Arbitrary<Integer> values = Arbitraries.integers();
			Arbitrary<List<Tuple2<Integer, Integer>>> keysAndValues =
					Combinators.combine(keys, values).as(Tuple::of).list();

			return keysAndValues.map(keyValueList -> {
				BST<Integer, Integer> bst1 = BST.nil();
				for (Tuple2<Integer, Integer> kv : keyValueList) {
					bst1 = bst1.insert(kv.get1(), kv.get2());
				}
				Collections.shuffle(keyValueList);
				BST<Integer, Integer> bst2 = BST.nil();
				for (Tuple2<Integer, Integer> kv : keyValueList) {
					bst2 = bst2.insert(kv.get1(), kv.get2());
				}
				return Tuple.of(bst1, bst2);
			});
		}
	}

	@Group
	class Completeness_Testing {

		// prop_UnionNil1 t = union nil t === t
		@Property
		boolean union_nil1(@ForAll("trees") BST<Integer, Integer> bst) {
			return BST.union(bst, BST.nil()).equals(bst);
		}

		// prop_UnionInsert t t′ (k, v) =
		//   union (insert k v t) t′ 􏰂 insert k v (union t t′)
		@Property
		boolean union_insert(
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2,
				@ForAll Integer key, @ForAll Integer value
		) {
			return equivalent(
					BST.union(bst1.insert(key, value), bst2),
					BST.union(bst1, bst2).insert(key, value)
			);
		}

		// prop_InsertComplete t = t === foldl (flip $ uncurry insert) nil (insertions t)
		@Property
		boolean insert_complete(@ForAll("trees") BST<Integer, Integer> bst) {
			List<Entry<Integer, Integer>> insertions = insertions(bst);
			BST<Integer, Integer> newBst = BST.nil();
			for (Entry<Integer, Integer> insertion : insertions) {
				newBst = newBst.insert(insertion.getKey(), insertion.getValue());
			}
			return bst.equals(newBst);
		}

		// prop_InsertCompleteForDelete k t = prop_InsertComplete (delete k t)
		@Property
		boolean insert_complete_for_delete(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			return insert_complete(bst.delete(key));
		}

		// prop_InsertCompleteForUnion t t′ = prop_InsertComplete (union t t′)
		@Property
		boolean insert_complete_for_union(
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			return insert_complete(BST.union(bst1, bst2));
		}
	}

	@Group
	class Model_Based_Properties {

		// prop_InsertModel k v t = toList (insert k v t) === L.insert (k, v) (toList t)
		@Property
		@Disabled("Duplicate keys are not considered")
		boolean insert_model_naive(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			List<Entry<Integer, Integer>> model = bst.toList();
			model.add(new SimpleImmutableEntry<>(key, value));
			return bst.insert(key, value).toList().equals(model);
		}

		// prop_InsertModel k v t =
		//  toList (insert k v t ) === L.insert (k , v ) (deleteKey k $ toList t )
		@Property
		boolean insert_model(
				@ForAll Integer key, @ForAll Integer value,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			List<Entry<Integer, Integer>> model = removeKey(bst.toList(), key);
			model.add(new SimpleImmutableEntry<>(key, value));
			List<Entry<Integer, Integer>> entries = bst.insert(key, value).toList();
			return equalsIgnoreOrder(entries, model);
		}

		private List<Entry<Integer, Integer>> removeKey(
				List<Entry<Integer, Integer>> model, @ForAll Integer key
		) {
			model.removeIf(entry -> entry.getKey().equals(key));
			return model;
		}

		// prop_NilModel = toList (nil :: Tree) === [ ]
		@Example
		boolean nil_model() {
			return BST.nil().toList().isEmpty();
		}

		// prop_DeleteModel k t = toList (delete k t) === deleteKey k (toList t)
		@Property
		boolean delete_model(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			List<Entry<Integer, Integer>> model = removeKey(bst.toList(), key);
			List<Entry<Integer, Integer>> entries = bst.delete(key).toList();
			return equalsIgnoreOrder(entries, model);
		}

		// prop_UnionModel t t′ =
		//   toList (union t t′) === L.sort (L.unionBy ((≡) ‘on‘ fst) (toList t) (toList t′))
		@Property
		boolean union_model(
				@ForAll("trees") BST<Integer, Integer> bst1,
				@ForAll("trees") BST<Integer, Integer> bst2
		) {
			List<Entry<Integer, Integer>> bst2Model = bst2.toList();
			for (Entry<Integer, Integer> entry : bst1.toList()) {
				bst2Model = removeKey(bst2Model, entry.getKey());
			}
			List<Entry<Integer, Integer>> model = bst1.toList();
			model.addAll(bst2Model);
			List<Entry<Integer, Integer>> entries = BST.union(bst1, bst2).toList();
			return equalsIgnoreOrder(entries, model);
		}

		// prop_FindModel k t = find k t === L.lookup k (toList t)
		@Property
		boolean find_model(
				@ForAll Integer key,
				@ForAll("trees") BST<Integer, Integer> bst
		) {
			List<Entry<Integer, Integer>> model = bst.toList();
			Optional<Integer> expectedFindResult =
					model.stream()
						 .filter(entry -> entry.getKey().equals(key))
						 .map(Entry::getValue)
						 .findFirst();
			return bst.find(key).equals(expectedFindResult);
		}

		private boolean equalsIgnoreOrder(List<Entry<Integer, Integer>> list1, List<Entry<Integer, Integer>> list2) {
			list1.sort(Comparator.comparing(Entry::getKey));
			list2.sort(Comparator.comparing(Entry::getKey));
			return list1.equals(list2);
		}

	}

	// prop_Measure k t =
	//   label (if k ∈ keys t then "present" else "absent") $
	//   label (if t ≡ nil then "empty" else
	//     if keys t ≡ [k] then "just k" else
	//     if (all (>=􏰁 k) (keys t)) then "at start" else
	//     if (all (􏰀<= k) (keys t)) then "at end" else "middle") $
	//   True
	@Property(tries = 1_000_000)
	@Disabled("takes about 5 mins to run")
	void measure(
			// @ForAll Integer key,
			@ForAll("keys") Integer key,
			@ForAll("trees") BST<Integer, Integer> bst
	) {
		List<Integer> keys = bst.keys();
		String frequency = keys.contains(key) ? "present" : "absent";
		Statistics.label("frequency").collect(frequency);

		String position =
				bst.isLeaf() ? "empty" :
						keys.equals(Collections.singletonList(key)) ? "just key" :
						keys.stream().allMatch(k -> k.compareTo(key) >= 0) ? "at start" :
						keys.stream().allMatch(k -> k.compareTo(key) <= 0) ? "at end" :
						"middle";
		Statistics.label("position").collect(position);

		String size = bst.isEmpty() ? "empty" :
				bst.size() < 10 ? "< 10" :
				bst.size() < 100 ? "10 >= x < 100" : ">= 100";
		Statistics.label("size").collect(size);
	}

	@Property(afterFailure = AfterFailureMode.RANDOM_SEED)
	@Disabled("supposed to fail")
	boolean unique(@ForAll("keys") int x, @ForAll("keys") int y) {
		//Statistics.collect(x == y);
		return x != y;
	}

	@Provide
	Arbitrary<BST<Integer, Integer>> trees() {
		Arbitrary<Integer> keys = keys();
		// Arbitrary<Integer> keys = Arbitraries.integers().unique();
		Arbitrary<Integer> values = Arbitraries.integers();
		Arbitrary<List<Tuple2<Integer, Integer>>> keysAndValues =
				Combinators.combine(keys, values).as(Tuple::of).list();

		// This could be implemented as streaming and reducing
		// but that'd probably be less understandable
		return keysAndValues.map(keyValueList -> {
			BST<Integer, Integer> bst = BST.nil();
			for (Tuple2<Integer, Integer> kv : keyValueList) {
				bst = bst.insert(kv.get1(), kv.get2());
			}
			return bst;
		});
	}

	@Provide
	Arbitrary<Integer> keys() {
		return Arbitraries.oneOf(
				Arbitraries.integers().between(-25, 25),
				Arbitraries.integers()
		).unique();
	}

}

