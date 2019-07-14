package htsi.treeMap;

import java.util.*;

import net.jqwik.api.*;

class TreeMap_Properties {

	@Property
	boolean insert_insert(
			@ForAll Integer key1, @ForAll Integer value1,
			@ForAll Integer key2, @ForAll Integer value2,
			@ForAll("treeMaps") TreeMap<Integer, Integer> original
	) {
		TreeMap<Integer, Integer> inserted = new TreeMap<>(original);
		inserted.put(key1, value1);
		inserted.put(key2, value2);
		TreeMap<Integer, Integer> expected = new TreeMap<>(original);
		expected.put(key2, value2);
		if (!key1.equals(key2)) {
			expected.put(key1, value1);
		}
		return inserted.equals(expected);
	}

	@Provide
	Arbitrary<TreeMap> treeMaps() {
		Arbitrary<Integer> keys = keys();
		Arbitrary<Integer> values = Arbitraries.integers();
		Arbitrary<List<Tuple.Tuple2<Integer, Integer>>> keysAndValues =
				Combinators.combine(keys, values).as(Tuple::of).list();

		return keysAndValues.map(keyValueList -> {
			TreeMap<Integer, Integer> treeMap = new TreeMap<>();
			for (Tuple.Tuple2<Integer, Integer> kv : keyValueList) {
				treeMap.put(kv.get1(), kv.get2());
			}
			return treeMap;
		});

	}

	@Provide
	Arbitrary<Integer> keys() {
		return Arbitraries.oneOf(
				Arbitraries.integers().between(0, 50),
				Arbitraries.integers()
		).unique();
	}

}
