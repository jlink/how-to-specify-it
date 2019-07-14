# How to Specify it! In Java!

In July 2019 [John Hughes](https://twitter.com/rjmh), 
unarguably the most prominent proponent of Property-based Testing, 
published [_How to Specify it!_](https://www.dropbox.com/s/tx2b84kae4bw1p4/paper.pdf).
In this paper he presents 

> "five generic approaches to writing \[...\] specifications" 

a.k.a. _properties_.

Throughout the paper he uses [QuickCheck in Haskell](http://hackage.haskell.org/package/QuickCheck)
as tool and language of choice. Since quite some developers are not
familiar with Haskell I want to transfer the examples into Java 
using [jqwik](https://jqwik.net) as property testing library. 
[John was kind enough](https://twitter.com/rjmh/status/1147034204439490560) 
to allow me to use _his text_ enriched by my examples. His paper is published under [CC-BY license](https://creativecommons.org/licenses/by/2.0/) and so is my "remix".


#### Changes I made to the original text

John's original text

> is formatted as quotation, 

whereas my sentences are just normal paragraphs.

In a few places I left out a couple of words or sentences that do not 
make sense in the Java context. In one situation I skipped over almost
a full page - this is noted in the text. 

Where necessary I inserted a few sentences of my own
to explain differences between the original and this version.
At the end of the article I appended a 
[personal addendum](#personal-addendum) in which I address 
a few open questions.

Moreover, I translated Haskell style variable names 
to longer Java names:
- `t` became `bst` or `bst1`
- `t′` became `bst2`
- `k` became `key` or `key1`
- `k′` became `key2`
- `v` became `value` or `value1`
- `v′` became `value2`
 
I also replaced some terms like ~~QuickCheck~~ with _jqwik_ by 
striking through the original. 
Moreover, whenever the floating text referenced variables or functions
from the code I used the names from the Java code.

I turned the end notes of references into links
pointing to PDF versions of the referenced paper. In the one case
where I could not find a publicly available PDF I reference the 
publisher's library entry.

#### The Code

You can find [all the code on github](https://github.com/jlink/how-to-specify-it/tree/master/src).


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents  

  - [Abstract.](#abstract)
- [1&nbsp;&nbsp; Introduction](#1-introduction)
- [2&nbsp;&nbsp; A Primer in Property-Based Testing](#2-a-primer-in-property-based-testing)
- [3&nbsp;&nbsp; Our Running Example: Binary Search Trees](#3-our-running-example-binary-search-trees)
- [4&nbsp;&nbsp; Approaches to Writing Properties](#4-approaches-to-writing-properties)
  - [4.1&nbsp;&nbsp; Validity Testing](#41-validity-testing)
  - [4.2&nbsp;&nbsp; Postconditions](#42-postconditions)
  - [4.3&nbsp;&nbsp; Metamorphic Properties](#43-metamorphic-properties)
    - [Preservation of Equivalence](#preservation-of-equivalence)
  - [4.4&nbsp;&nbsp; Inductive Testing](#44-inductive-testing)
  - [4.5&nbsp;&nbsp; Model-based Properties](#45-model-based-properties)
  - [4.6&nbsp;&nbsp; A Note on Generation](#46-a-note-on-generation)
- [5&nbsp;&nbsp; Bug Hunting](#5-bug-hunting)
  - [5.1&nbsp;&nbsp; Bug finding effectiveness](#51-bug-finding-effectiveness)
    - [Differences between QuickCheck and _jqwik_ Bug Hunting Results](#differences-between-quickcheck-and-_jqwik_-bug-hunting-results)
  - [5.2&nbsp;&nbsp; Bug finding performance](#52-bug-finding-performance)
  - [5.3&nbsp;&nbsp; Lessons](#53-lessons)
- [6&nbsp;&nbsp; Discussion](#6-discussion)
- [Personal Addendum](#personal-addendum)
  - [Bug Hunting with Unit Tests](#bug-hunting-with-unit-tests)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### Abstract. 

> Property-based testing tools test software against a _specification_, rather than a set of examples. This tutorial paper presents five generic approaches to writing such specifications (for purely functional code). We discuss the costs, benefits, and bug-finding power of each approach, with reference to a simple example with eight buggy variants. The lessons learned should help the reader to develop effective property-based tests in the future.

## 1&nbsp;&nbsp; Introduction

> Searching for “property-based testing” on Youtube results in a lot of hits. Most of the top 100 consist of talks recorded at developer conferences and meetings, where (mostly) other people than this author present ideas, tools and methods for property-based testing, or applications that make use of it. Clearly, property-based testing is an idea whose time has come. But clearly, it is also poorly understood, requiring explanation over and over again!
>
> We have found that many developers trying property-based testing for the first time find it difficult to identify _properties to write_ — and find the simple examples in tutorials difficult to generalize. 
> This is known as the [oracle problem](http://www0.cs.ucl.ac.uk/staff/m.harman/tse-oracle.pdf), 
> and it is common to all approaches that use test case generation.
>
> In this paper, therefore, we take a simple — but non-trivial — example of a purely functional data structure, and present five different approaches to writing properties, along with the pitfalls of each to keep in mind. We compare and contrast their effectiveness with the help of eight buggy implementations. We hope that the concrete advice presented here will enable readers to side-step the “where do I start?” question, and quickly derive the benefits that property-based testing has to offer.

## 2&nbsp;&nbsp; A Primer in Property-Based Testing 

> Property-based testing is an approach to random testing pioneered by 
> [QuickCheck in Haskell](http://users.cs.northwestern.edu/~robby/courses/395-495-2009-fall/quick.pdf). 
> There is no precise definition of the term: indeed, MacIver [writes](https://hypothesis.works/articles/what-is-property-based-testing/):
> > ‘Historically the definition of property-based testing has been “The thing that QuickCheck does”.’
> The basic idea has been reimplemented many times — Wikipedia currently lists more than 50 implementations, in 36 different programming languages3, of all programming paradigms. These implementations vary in quality and features, but the ideas in this paper should be relevant to a user of any of them.
>
> Suppose, then, that we need to test the `reverse` function on lists. 

Since JDK's `Collections.reverse` method changes the original list in place we'll wrap it with a copying method. So here's the reverse method
that we actually use for testing:

```java
List<T> reverse(List<T> original) {
    List<T> clone = new ArrayList<>(original);
    Collections.reverse(clone);
    return clone;
}
```

> Any developer will be able to write a unit test such as the following:

```java
@Example 
boolean reverseList() {
    List<Integer> aList = asList(1, 2, 3);
    return reverse(aList).equals(asList(3, 2, 1));
} 
```

`@Example` is _jqwik's_ equivalent to JUnit's `@Test`. Moreover it allows
to return a boolean value to state success or failure of a test. For
more complicated checks the assertion style of most Java test libraries
is also supported.

> This test is written in the same form as most test cases worldwide: we apply the function under test (`reverse`) to known arguments (1,2,3), and then compare the result to a known expected value (3,2,1). Developers are practiced in coming up with these examples, and predicting expected results. But what happens when we try to write a property instead?

```java
@Property
boolean reverseList(@ForAll List<Integer> aList) {
    return reverse(aList).equals(???);
} 
```

> The property is parameterised on `aList`, which will be randomly generated by ~~QuickCheck~~ _jqwik_ \[...\].
>
> The property can clearly test `reverse` in a much wider range of cases than the unit test — any randomly generated list, rather than just the list [1,2,3] — which is a great advantage. But the question is: _what is the expected result_? That is, what should we replace `???` by in the definition above? Since the argument to `reverse` is not known in advance, we cannot precompute the expected result. We could write test code to _predict_ it, as in

```java
@Property
boolean reverseList(@ForAll List<Integer> aList) {
    return reverse(aList).equals(predictRev(aList));
} 
```

> but `predictRev` _is not easier to write than reverse_ — it is _exactly the same function_!
>
> This is the most obvious approach to writing properties — to replicate the implementation in the test code — and it is deeply unsatisfying. It is both an _expensive_ approach, because the replica of the implementation may be as complex as the implementation under test, and of _low value_, because there is a grave risk that misconceptions in the implementation will be replicated in the test code. “Expensive” and “low value” is an unfortunate combination of characteristics for a software testing method, and all too often leads developers to abandon property-based testing altogether.
>
> We can finesse the problem by rewriting the property so that it does not refer to an expected result, instead checking some _property_ of the result. For example, `reverse` is its own inverse:

```java
@Property
boolean reverseTwiceIsOriginal(@ForAll List<Integer> aList) {
    return reverse(reverse(aList)).equals(aList);
} 
```

> Now we can pass the property to ~~QuickCheck~~ _jqwik_, to run a series of random tests (by default ~~100~~ 1000):

```text
ListReverseProperties:reverseTwiceIsOriginal =
 
tries = 1000         | # of calls to property
checks = 1000        | # of not rejected calls
```

> We have met our goal of testing `reverse` on 1000 random lists, but this property is not very strong — if we had accidentally defined

```java
List<T> reverse(List<T> list) {
    return list;
} 
```

> then it would still pass (whereas the unit test above would report a bug).
> 
> We can define another property that this _buggy_ implementation of `reverse` passes, but the correct definition fails:

```java
@Property
boolean reverseKeepsTheOriginalList(@ForAll List<Integer> aList) {
    return reverse(aList).equals(aList);
} 
```

```
AssertionFailedError: Property [ListReverseProperties:reverseKeepsTheOriginalList] falsified with sample [[0, -1]]

tries = 2                     | # of calls to property
checks = 2                    | # of not rejected calls
sample = [[0, -1]]
```

Here the `sample` line shows the value of `aList` for which the test failed: (0, -1).

> Interestingly, the counterexample ~~QuickCheck~~ _jqwik_ reports for this property is always (0, -1) or (0, 1). These are not the random counterexamples that ~~QuickCheck~~ _jqwik_ finds first; they are the result of _shrinking_ the random counterexamples via a systematic greedy search for a simpler failing test. Shrinking lists tries to remove elements, and numbers shrink towards zero; the reason we see these two counterexamples is that `aList` must contain at least two different elements to falsify the property, and 0 and 1/-1 are the smallest pair of different integers. Shrinking is one of the most useful features of property-based testing, resulting in counterexamples which are usually easy to debug, because _every part_ of the counterexample is relevant to the failure.
>
> Now we have seen the benefits of property-based testing — random generation of very many test cases, and shrinking of counterexamples to minimal failing tests — and the major pitfall: the temptation to replicate the implementation in the tests, incurring high costs for little benefit. In the remainder of this paper, we present systematic ways to define properties _without_ falling into this trap. We will (largely) ignore the question of how to generate _effective_ test cases — that are good at reaching buggy behaviour in the implementation under test — because in the absence of good properties, good generators are of little value.

## 3&nbsp;&nbsp; Our Running Example: Binary Search Trees

> The code we shall develop properties for is an implementation of finite maps (from keys to values) as binary search trees. 

Here's the public interface of class 
[`BST`](https://github.com/jlink/how-to-specify-it/blob/master/src/test/java/htsi/bst/BST.java):

```java
public class BST<K extends Comparable<K>, V> {
    public static <K extends Comparable<K>, V> BST<K, V> nil();
    public static <K extends Comparable<K>, V> BST<K, V> union(BST<K, V> bst1, BST<K, V> bst2);
       
    public K key();
    public V value();
    public Optional<BST<K, V>> left();
    public Optional<BST<K, V>> right();
    public boolean isEmpty();
    public boolean isLeaf();
    public int size();
       
    public Optional<V> find(K key);
    public BST<K, V> insert(K key, V value);
    public BST<K, V> delete(K key);
    public List<K> keys();
    public List<Map.Entry<K, V>> toList();
}
```

The method names were chosen to resemble the Haskell version as much as possible. 
Moreover, the implementation follows a pattern rather unusual in Java: 
Every instance of a `BST` is immutable, i.e. the changing methods - `insert`, `delete` and `union` - return a new instance of `BST`. 
An empty instance can be accessed through `BST.nil()`.

> The operations we will test are those that create trees (nil, insert, delete and union), and that find the value associated with a key in the tree. We will also use auxiliary operations: toList, which returns a sorted list of the key-value pairs in the tree, and keys which is defined in terms of it. The implementation itself is standard, and is not included here.
>
> Before writing properties of binary search trees, we must define a generator: 

```java
import net.jqwik.api.Tuple.*;

@Provide
Arbitrary<BST<Integer, Integer>> trees() {
    Arbitrary<Integer> keys = Arbitraries.integers().unique();
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
```

_Generators_ have type `Arbitrary` in _jqwik_; they are usually fed
to properties through methods annotated with `@Provide`. 
Shrinking behaviour is also automatically derived.

> We need to fix an instance type for testing; for the time being, we choose to let both keys and values be integers. `Integer` is usually an acceptably good choice as an instance for testing polymorphic properties, although we will return to this choice later.

Strictly speaking this would not be necessary for _jqwik_ since the framework
can randomly choose any type that's compatible with the generic type
definition. To be closer to the original version I went with `Integer` nonetheless.

## 4&nbsp;&nbsp; Approaches to Writing Properties

### 4.1&nbsp;&nbsp; Validity Testing

> Like many data-structures, binary search trees satisfy an important invariant— and so we can write properties to test that the invariant is preserved.
>
> In this case the invariant is captured by the 
> [following function](https://github.com/jlink/how-to-specify-it/blob/master/src/test/java/htsi/bst/BSTUtils.java#L9):

```java
boolean isValid(BST bst) {
    if (bst.isLeaf()) {
        return true;
    }
    return isValid(bst.left()) 
             && isValid(bst.right())
             && keys(bst.left()).allMatch(k -> k.compareTo(bst.key()) < 0)
             && keys(bst.right()).allMatch(k -> k.compareTo(bst.key()) > 0);
}
```
I spare you the clumsy generic Java types and the implementation of method `keys` and `isValid(Optional<BST>)`. 

> That is, all the keys in a left subtree must be less than the key in the node, and all the keys in the right subtree must be greater.
>
> This definition is obviously correct, but also inefficient: it is quadratic in the size of the tree in the worst case. A more efficient definition would exploit the validity of the left and right subtrees, and compare only the last key in the left subtree, and the first key in the right subtree, against the key in a Branch node. But the equivalence of these two definitions depends on reasoning, and we prefer to avoid reasoning that is not checked by tests — if it turns out to be wrong, or is invalidated by later changes to the code, then tests using the more efficient definition might fail to detect some bugs. Testing that two definitions are equivalent would require testing a property such as

```java
@Property
boolean valid_and_fastValid_are_equivalent(@ForAll("trees") BST bst) {
    return valid(bst) == fastValid(bst);
}
``` 

> and to do so, we would need a generator that can produce both valid and invalid trees, so this is not a straightforward extension. We prefer, therefore, to use the obvious-but-inefficient definition, at least initially.
>
> Now it is straightforward to define properties that check that every operation that constructs a tree, constructs a valid one:

```java
@Example
boolean nil_is_valid() {
    BST<Integer, Integer> nil = BST.nil();
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
    return isValid(bst.delete(key));
}

@Property
boolean union_valid(
        @ForAll("trees") BST<Integer, Integer> bst,
        @ForAll("trees") BST<Integer, Integer> other
) {
    return isValid(BST.union(bst, other));
}
``` 

> However, these properties, by themselves, do not provide good testing for validity. To see why, let us plant a bug in insert, so that it creates duplicate entries when inserting a key that is already present (bug (2) in [section 5](#5-bug-hunting)). Property `insert_valid` fails as it should, but so do `delete_valid` and `union_valid`:

```
AssertionFailedError: Property [BST Properties:insert valid] 
    falsified with sample 
        [0=0
        left:  NIL
        right: 0=0
               left:  NIL
               right: NIL, 
        0]
       
AssertionFailedError: Property [BST Properties:delete valid] 
    falsified with sample 
        [0=0
        left:  NIL
        right: 0=0
               left:  NIL
               right: NIL, 
        -1]   
       
AssertionFailedError: Property [BST Properties:union valid] 
    falsified with sample 
        [2=0
            left:  NIL
            right: NIL, 
        2=0
            left:  NIL
            right: NIL]
``` 

> Thus, at first sight, there is nothing to indicate that the bug is in insert; all of insert, delete and union can return invalid trees! However, delete and union are given invalid trees as inputs in the tests above, and we cannot expect them to return valid trees in this case, so these reported failures are “false positives”.
>
> The problem here is that the generator for trees is producing invalid ones (because it is defined in terms of insert). We could add a precondition to each property, requiring the tree to be valid, as in:

```java
@Property
boolean delete_valid(
        @ForAll("trees") BST<Integer, Integer> bst,
        @ForAll Integer key
) {
    Assume.that(isValid(bst));
    return isValid(bst.delete(key));
}
```

> which would discard invalid test cases (not satisfying the precondition) without running them, and thus make the properties pass. This is potentially inefficient (we might spend much of our testing time discarding test cases), but it is also really just applying a sticking plaster: what we want is that all generated trees should be valid! We can test this by defining an additional property:

```java
@Property
boolean arbitrary_valid(@ForAll("trees") BST<Integer, Integer> bst) {
    return isValid(bst);
}
```

> which at first sight seems to be testing that all trees are valid, but in fact tests that all trees generated by the Arbitrary instance are valid. If this property fails, then it is the generator that needs to be fixed — there is no point in looking at failures of other properties, as they are likely caused by the failing generator.
> 
> Usually the generator for a type is intended to fulfill its invariant, but — as in this case — is defined independently. A property such as `arbitrary_valid` is essential to check that these definitions are mutually consistent.

At this point 
[in the original paper](https://www.dropbox.com/s/tx2b84kae4bw1p4/paper.pdf)
there is almost a full page discussing problems in shrinking that lead
to invalid shrunk data. However, this is not an issue in _jqwik's_
implementation. In general, _jqwik_ supports
[integrated shrinking](https://jqwik.net/docs/current/user-guide.html#integrated-shrinking);
thereby all preconditions added during data generation will also be
preserved while shrinking. 

> This section illustrates well the importance of testing our tests; it is vital to test generators and shrinkers independently of the operations under test, because a bug in either can result in very many hard-to-debug failures in other properties.
>
> Validity properties are important to test, whenever a datatype has an invariant, but they are far from sufficient by themselves. Consider this: if every function returning a BST were defined to return nil in every case, then all the properties written so far would pass. insert could be defined to delete the key instead, or union could be defined to implement set difference — as long as the invariant is preserved, the properties will still pass. Thus we must move on to properties that better capture the intended behaviour of each operation.

### 4.2&nbsp;&nbsp; Postconditions

> A postcondition is a property that should be True after a call, or (equivalently, for a pure function) True of its result. Thus we can define properties by asking ourselves “What should be True after calling f ?”. For example, after calling insert, then we should be able to find the key just inserted, and any previously inserted keys with unchanged values.

```java
@Property
boolean insert_post(
        @ForAll Integer key, @ForAll Integer value,
        @ForAll("trees") BST<Integer, Integer> bst,
        @ForAll Integer otherKey
) {
    Optional<Integer> found = bst.insert(key, value).find(otherKey);
    if (otherKey.equals(key)) {
        return found.map(v -> v.equals(value)).orElse(false);
    } else {
        return found.equals(bst.find(otherKey));
    }
}
```

> One may wonder whether it is best to parameterize this property on two different keys, or just on one: after all, for the type chosen, k and k′ are equal in only around 3.3% of tests, so most test effort is devoted to checking that other keys than the one inserted are preserved. However, using the same key for k and k′ would weaken the property drastically — for example, an implementation of insert that discarded the original tree entirely would still pass. Moreover, nothing hinders us from defining and testing a specialized property:

```java
@Property
boolean insert_post_same_key(
        @ForAll Integer key, @ForAll Integer value,
        @ForAll("trees") BST<Integer, Integer> bst
) {
    return insert_post(key, value, bst, key);
}
```

> Testing this property devotes all test effort to the case of finding a newly inserted key, but does not require us to replicate the logic in the more general postcondition.
>
> We can write similar postconditions for delete and union; writing the property for union forces us to specify that union is left-biased (since union of finite maps cannot be commutative).

```java
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
```

> Postconditions are not always as easy to write. For example, consider a postcondition for find. The return value is either `Optional.empty()`, in case the key is not found in the tree, or `Optional.of(v)`, in the case where it is present with value v. So it seems that, to write a postcondition for find, we need to be able to determine whether a given key is present in a tree, and if so, with what associated value. But this is exactly what find does! So it seems we are in the awkward situation discussed in the introduction: in order to test find, we need to reimplement it.
>
> We can finesse this problem using a very powerful and general idea, that of constructing a test case whose outcome is easy to predict. In this case, we know that a tree must contain a key `key`, if we have just inserted it. Likewise, we know that a tree cannot contain a key `key`, if we have just deleted it. Thus we can write two postconditions for find, covering the two cases:

```java
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
```

> But there is a risk, when we write properties in this form, that we are only testing very special cases. Can we be certain that every tree, containing key `key` with value `value`, can be expressed in the form `tree.insert(key, value)`? Can we be certain that every tree not containing `key` can be expressed in the form `tree.delete(key)`? If not, then the postconditions we wrote for find may be less effective tests than we think.
>
> Fortunately, for this data structure, every tree can be expressed in one of these two forms, because inserting a key that is already present, or deleting one that is not, is a no-op. We express this as another property to test:

```java
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
```

### 4.3&nbsp;&nbsp; Metamorphic Properties

> [Metamorphic testing](http://www.cs.hku.hk/research/techreps/document/TR-2017-04.pdf) 
> is a successful approach to the oracle problem in many contexts. The basic idea is this: even if the expected result of a function call such as `tree.insert(key, value)` may be difficult to predict, we may still be able to express an expected relationship between this result, and the result of a related call. For example, if we insert an additional key into `tree` before calling `insert(key, value)`, we might expect the additional key to be inserted into the result also.
>
> Formalizing this intuition, we might define the property

```java
@Property
boolean insert_insert(
        @ForAll Integer key1, @ForAll Integer value1,
        @ForAll Integer key2, @ForAll Integer value2,
        @ForAll("trees") BST<Integer, Integer> bst
) {
    return bst.insert(key1, value1).insert(key2, value2)
            .equals(bst.insert(key2, value2).insert(key1, value1));
}
```

> Informally, we expect the effect of inserting `key1` `value1` into t before calling `insert(key2,value2)`, to be that they are also inserted into the result. A metamorphic property (almost) always relates two calls to the function under test: in this case, the function under test is insert, and the two calls are `bst.insert(key2, value2)` and `bst.insert(key1, value1).insert(key2, value2)`. The latter is constructed by modifying the argument, in this case also using insert, and the property expresses an expected relationship between the values of the two calls. Metamorphic testing is a fruitful source of property ideas, since if we are given O(n) operations to test, each of which can also be used as a modifier, then there are potentially O(n<sup>2</sup>) properties that we can define.
>
> However, the property above is not true: testing it yields:

```
org.opentest4j.AssertionFailedError: Property [Metamorphic:insert insert] falsified with sample [0, 0, 0, 1, NIL]

tries = 3                     | # of calls to property
checks = 3                    | # of not rejected calls
sample = [0, 0, 0, 1, NIL]
```

> This is not surprising. The property states that the order of insertions does not matter, while the failing test case inserts the same key twice with different values — of course the order of insertion matters in this case, because “the last insertion wins”. A first stab at a metamorphic property may often require correction; ~~QuickCheck~~ _jqwik_ is good at showing us what it is that needs fixing. We just need to consider two equal keys as a special case:

```java
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
    return inserted.equals(expected);
}
```

> Unfortunately, this property still fails:

```
org.opentest4j.AssertionFailedError: Property [Metamorphic:insert insert] falsified with sample [0, 0, -1, 0, NIL]

tries = 2                     | # of calls to property
checks = 2                    | # of not rejected calls
sample = [0, 0, -1, 0, NIL]
```

> Inspecting the two resulting trees, we can see that changing the order of insertion results in trees with different shapes, but containing the same keys and values. Arguably this does not matter: we should not care what shape of tree each operation returns, provided it contains the right information. To make our property pass, we must make this idea explicit. We therefore define an equivalence relation on trees that is true if they have the same contents,

```java
boolean equivalent(BST bst1, BST bst2) {
    return new HashSet<>(bst1.toList()).equals(new HashSet(bst2.toList());
}
```

> and re-express the property in terms of this equivalence:

```java
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
```

> Now, at last, the property passes. (We discuss why we need both this equivalence, and structural equality on trees, in [section 6](#6-a-note-on-generation)).
>
> There is a different way to address the first problem — that the order of insertions does matter, when inserting the same key twice. That is to require the keys to be different, via a precondition:

```java
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
```

> This lets us keep the property in a simpler form, but is weaker, since it no longer captures that “the last insert wins”. We will return to this point later.
>
> We can go on to define further metamorphic properties for insert, with different modifiers — delete and union:

```java
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
boolean insert_union(
        @ForAll Integer key, @ForAll Integer value,
        @ForAll("trees") BST<Integer, Integer> bst1,
        @ForAll("trees") BST<Integer, Integer> bst2
) {
    BST<Integer, Integer> unionInsert = BST.union(bst1, bst2).insert(key, value);
    BST<Integer, Integer> insertUnion = BST.union(bst1.insert(key, value), bst2);
    return equivalent(unionInsert, insertUnion);
}
```

> and, in a similar way, metamorphic properties for the other functions in the API under test. We derived sixteen different properties in this way, which are ~~listed in Appendix A~~ [available on Github](https://github.com/jlink/how-to-specify-it/blob/master/src/test/java/htsi/bst/BST_Properties.java#L133). The trickiest case is union, which as a binary operation, can have either argument modified — or both. We also found that some properties could be motivated in more than one way. For example, property `insert_union` (above) can be motivated as a metamorphic test for insert, in which the argument is modified by union, or as a metamorphic test for union, in which the argument is modified by insert. Likewise, the metamorphic tests we wrote for find replicated the postconditions we wrote above for insert, delete and union. We do not see this as a problem: that there is more than one way to motivate a property does not make it any less useful, or any harder to come up with!


#### Preservation of Equivalence

> Now that we have an equivalence relation on trees, we may wonder whether the operations under test preserve it. For example, we might try to test whether insert preserves equivalence as follows:

```java
@Property
boolean insert_preserves_equivalence(
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
```

> This kind of property is important, since many of our metamorphic properties only allow us to conclude that two expressions are equivalent; to use these conclusions in further reasoning, we need to know that equivalence is preserved by each operation.
>
> Unfortunately, testing the property above does not work; it is very, very unlikely that two randomly generated trees `bst1` and `bst2` will be equivalent, and thus almost all generated tests are discarded.

Running the property above with _jqwik_ will fail with the following 
error message:

```
org.opentest4j.AssertionFailedError: Property [Equivalence:insert preserves equivalence] 
    exhausted after [1000] tries and [1000] rejections

tries = 1000                  | # of calls to property
checks = 0                    | # of not rejected calls
```

> To test this kind of property, we need to generate equivalent pairs of trees together. We can do so be defining a type of equivalent pairs, with a custom generator:

```java
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
```

> This generator constructs two equivalent trees by inserting the same list of keys and values in two different orders. The properties using this type are shown below, along with properties to test the new generator:

```java
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

@Property
boolean find_preserves_equivalence(
        @ForAll Integer key,
        @ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts
) {
    return bsts.get1().find(key).equals(bsts.get2().find(key));
}

@Property
boolean equivalent_trees_are_equivalent(
        @ForAll("equivalentTrees") Tuple2<BST<Integer, Integer>, BST<Integer, Integer>> bsts
) {
    return equivalent(bsts.get1(), bsts.get2());
}
```

### 4.4&nbsp;&nbsp; Inductive Testing

> Metamorphic properties do not, in general, completely specify the behaviour of the code under test. However, in some cases, a subset of metamorphic properties does form a complete specification. Consider, for example, the following two properties of union:

```java
    @Property
    boolean union_nil1(@ForAll("trees") BST<Integer, Integer> bst) {
        return BST.union(bst, BST.nil()).equals(bst);
    }

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
```

> We can argue that these two properties characterize the behaviour of union precisely (up to equivalence of trees), by induction on the size of union’s first argument. This idea is due to Claessen.
>
> However, there is a hidden assumption in the argument above — namely, that any non-empty tree `bst` can be expressed in the form `bst2.insert(key, value)`, for some smaller tree `bst2`, or equivalently, that any tree can be constructed using insertions only. There is no reason to believe this a priori — it might be that some tree shapes can only be constructed by delete or union. So, to confirm that these two properties uniquely characterize union, we must test this assumption.
> 
> One way to do so is to define a function that maps a tree to a list of insertions that recreate it. It is sufficient to insert the key in each node before the keys in its subtrees:

```java
List<Entry<K, V>> insertions(BST<K, V> bst) {
    if (bst.isLeaf()) {
        return Collections.emptyList();
    }
    List<Entry<K, V>> insertions = new ArrayList<>();
    insertions.add(bst.entry);
    bst.left().ifPresent(left -> insertions.addAll(insertions(left)));
    bst.right().ifPresent(right -> insertions.addAll(insertions(right)));
    return insertions;
}
```

> Now we can write a property to check that every tree can be reconstructed from its list of insertions:

```java
@Property
boolean insert_complete(@ForAll("trees") BST<Integer, Integer> bst) {
    List<Entry<Integer, Integer>> insertions = insertions(bst);
    BST<Integer, Integer> newBst = BST.nil();
    for (Entry<Integer, Integer> insertion : insertions) {
        newBst = newBst.insert(insertion.getKey(), insertion.getValue());
    }
    return bst.equals(newBst);
}
```

> However, this is not sufficient! Recall that the generator we are using, defined in section 3, generates a tree by performing a list of insertions! It is clear that any such tree can be built using only insert, and so the property above can never fail, but what we need to know is that the same is true of trees returned by delete and union! We must thus define additional properties to test this:

```java
@Property
boolean insert_complete_for_delete(
        @ForAll Integer key,
        @ForAll("trees") BST<Integer, Integer> bst
) {
    return insert_complete(bst.delete(key));
}

@Property
boolean insert_complete_for_union(
        @ForAll("trees") BST<Integer, Integer> bst1,
        @ForAll("trees") BST<Integer, Integer> bst2
) {
    return insert_complete(BST.union(bst1, bst2));
}
```

> Together, these properties also justify our choice of generator — they show that we really can generate any tree constructable using the tree API. If we could not demonstrate that trees returned by delete and union can also be constructed using insert, then we could define a more complex generator for trees that uses all the API operations, rather than just insert — a workable approach, but considerably trickier, and harder to tune for a good distribution of test data.
>           
> Finally, we note that in these completeness properties, it is vital to check structural equality between trees, and not just equivalence. The whole point is to show that delete and union cannot construct otherwise unreachable shapes of trees, which might provoke bugs in the implementation.

### 4.5&nbsp;&nbsp; Model-based Properties

> In 1972, Hoare published [an approach to proving the correctness of data representations](https://link.springer.com/article/10.1007%2FBF00289507), by relating them to abstract data using an abstraction function. Hoare defines a concrete and abstract implementation for each operation, and then proves that diagrams such as this one commute:

<pre style="font-family: monospace">
                  abstraction
             t ---------------> kvs
             |                  |
 insert k v  |                  | abstract insert k v
             |                  |
             v                  v             
             t′---------------> kvs′
                  abstraction
</pre>

> It follows that any sequence of concrete operations behaves in the same way as the same sequence of abstract ones.
>
> We can use the same idea for testing. In this case we will use ordered lists of key-value pairs as the abstract implementation, which means we can use toList as the abstraction function. Since `java.util.List` already provides an insertion function for ordered lists, it is tempting to define

```java
@Property
boolean insert_model(
        @Forll Integer key, @ForAll Integer value,
        @ForAll("trees") BST<Integer, Integer> bst
) {
    List<Entry<Integer, Integer>> model = bst.toList();
    model.add(new SimpleImmutableEntry<>(key, value));
    return bst.insert(key, value).toList().equals(model);
}
```

> However, this property fails:

```
org.opentest4j.AssertionFailedError: Property [Model Based Properties:insert model] falsified with sample [0, 0, [0=0]]

tries = 2                     | # of calls to property
checks = 2                    | # of not rejected calls
sample = [0, 0, [0=0]]
```

> The problem is that the insertion function in Data.List may create duplicate elements, but insert for trees does not. So it is not quite the correct abstract implementation; we can correct this by deleting the key if it is initially present:

```java
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
```

The Java version also required to make testing for equivalence of entry
lists ignore the order of entries. I'm not sure why that isn't creating
problems with Haskell and Quickcheck. 

Here's the rest of the model based properties:

```java
@Example
boolean nil_model() {
    return BST.nil().toList().isEmpty();
}

@Property
boolean delete_model(
        @ForAll Integer key,
        @ForAll("trees") BST<Integer, Integer> bst
) {
    List<Entry<Integer, Integer>> model = removeKey(bst.toList(), key);
    List<Entry<Integer, Integer>> entries = bst.delete(key).toList();
    return equalsIgnoreOrder(entries, model);
}

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
```

### 4.6&nbsp;&nbsp; A Note on Generation

> Throughout this paper, we have used integers as test data, for both keys and values. This is generally an acceptable choice, although not necessarily ideal. It is useful to measure the distribution of test data, to judge whether or not tests are likely to find bugs efficiently. In this case, many properties refer to one or more keys, and a tree, generated independently. We may therefore wonder, how often does such a key actually occur in an independently generated tree?
>
> To find out, we can define a property just for measurement. We measure not only how often `key` appears in `bst`, but also where among the keys of `bst` it appears:

```java
@Property(tries = 1_000_000)
void measure(
        @ForAll Integer key,
        @ForAll("trees") BST<Integer, Integer> bst
) {
    List<Integer> keys = bst.keys();
    
    String frequency = keys.contains(key) ? "present" : "absent";
    Statistics.label("frequency").collect(frequency);

    String position =
            bst.isLeaf() ? "empty"
            : keys.equals(Collections.singletonList(key)) ? "just key"
            : keys.get(0).equals(key) ? "at start"
            : keys.get(keys.size() - 1 ).equals(key) ? "at end"
            : "middle";
    Statistics.label("position").collect(position);
}
```

> This property never fails, it just collects information about the generated tests, which is reported in tables afterwards. Running a million tests results in

```
[BST Properties:measure] frequency = 
    absent  : 89 %
    present : 11 %

[BST Properties:measure] position = 
    middle   : 98.18 %
    empty    : 1.36 %
    at end   : 0.24 %
    at start : 0.21 %
    just key : 0.0 %
```

> From the second table, we can see that `key` appears at the beginning or end of the keys in `bst` about ~~10%~~ 0.5% of the time for each case, while it appears somewhere in the middle of the sequences of keys ~~75%~~ 98% of the time. This looks quite reasonable. On the other hand, _in almost ~~80%~~ 90% of tests, key is not found in the tree at all!_
>
> For some of the properties we defined, this will result in quite inefficient testing. For example, consider the postcondition for insert:

```java
@Property
boolean insert_post(
        @ForAll Integer key, @ForAll Integer value,
        @ForAll("trees") BST<Integer, Integer> bst,
        @ForAll Integer otherKey
) {
    Optional<Integer> found = bst.insert(key, value).find(otherKey);
    if (otherKey.equals(key)) {
        return found.map(v -> v.equals(value)).orElse(false);
    } else {
        return found.equals(bst.find(otherKey));
    }
}
``` 

> In almost 80% of tests `otherKey` will not be present in `bst`, and since `otherKey` is rarely equal to `key`, then in most of these cases both sides of the equation will be `Optional.empty()`. In effect, we spend most of our effort testing that inserting `key` does not insert an unrelated key `otherKey` into the tree! While this would be a serious bug if it occurred, it seems disproportionate to devote so much test effort to this kind of case.
>
> More reasonable would be to divide our test effort roughly equally between cases in which the given key does occur in the random tree, and cases in which it does not. We can achieve this by changing the generation of keys.  

For example, we can give keys between 0 and 50 an additional
chance of being chosen which will raise the probability that
a number is both in the generated tree and the generated single key:

```java
@Provide
Arbitrary<Integer> keys() {
    return Arbitraries.oneOf(
            Arbitraries.integers().between(0, 50),
            Arbitraries.integers()
    ).unique();
}
```

This requires to both use this function the `trees()` generator method and changing the annotation of key values to `@ForAll("keys") Integer`.

> Testing property `measure` using this type for keys results in the following, ~~much~~ somewhat better, distribution:

```
[BST Properties:measure] frequency = 
    present : 58 %
    absent  : 42 %

[BST Properties:measure] position = 
    middle   : 97.75 %
    empty    : 1.39 %
    at start : 0.72 %
    at end   : 0.13 %
    just key : 0.0 %
```

> This example illustrates that “collisions” (that is, cases in which we randomly choose the same value in two places) can be important test cases. Indeed, consider the following (obviously false) property:

```java
@Property
boolean unique(@ForAll int x, @ForAll int y) {
    return x != y;
}
```

> If we were to choose x and y uniformly from the entire range of 64-bit integers, then ~~QuickCheck~~ _jqwik_ would never be able to falsify it, in practice. If we use ~~QuickCheck~~ _jqwik_’s built-in Int generator, then the property fails in around ~~3.3%~~ 0.2% of cases. Using the `keys` generator we have just defined, the property fails in ~~9.3%~~ 1% of cases. The choice of generator should be made on the basis of how important collisions are as test cases.

## 5&nbsp;&nbsp; Bug Hunting

> To evaluate the properties we have written, we created eight buggy implementations of binary search trees, with bugs ranging from subtle to blatant. These implementations are listed here:

|Bug&nbsp;#|Description|
|:--------:|-----------|
|1         |_insert_ discards the existing tree, returning a single-node tree just containing the newly inserted value.|
|2         |_insert_ fails to recognize and update an existing key, inserting a duplicate entry instead.|
|3         |_insert_ fails to update an existing key, leaving the tree unchanged instead.|
|4         |_delete_ fails to rebuild the tree above the key being deleted, returning only the remainder of the tree from that point on (an easy mistake for those used to imperative programming to make).|
|5         |Key comparisons reversed in _delete_; only works correctly at the root of the tree.|
|6         |_union_ wrongly assumes that all the keys in the first argument precede those in the second.|
|7         |_union_ wrongly assumes that if the key at the root of `bst1` is smaller than the key at the root of `bst2`, then all the keys in `bst1` will be smaller than the key at the root of `bst2`.|
|8         |_union_ works correctly, except that when both trees contain the same key, the left argument does not always take priority.|

In my bug hunting attempts I left out bugs #6 and #7 because the
implementation of union they assume is completely different from what I 
had actually implemented. That's why the tables below are missing #6 and #7.

> The results of testing each property for each buggy version are:

|  Validity Properties |#1 |#2 |#3 |#4 |#5 |#8 |
|----------------------|:-:|:-:|:-:|:-:|:-:|:-:|
|`arbitrary valid`     |   | Ox|   |   |   |   |
|`nil valid`           |   |   |   |   |   |   |
|`insert valid`        |   | X |   |   |   |   |
|`delete valid`        |   | Ox|   |   |   |   |
|`union valid`         |   | X |   |   |   |   |

|  Postconditions        |#1 |#2 |#3 |#4 |#5 |#8 |
|:-----------------------|:-:|:-:|:-:|:-:|:-:|:-:|
|`insert post`           | X | X | X |   |   |   |
|`delete post`           |   | Ox|   | Ox| X |   |
|`find post present`     |   | X | X |   |   |   |
|`find post absent`      |   | Ox|   |   | X |   |
|`insert delete complete`|   | X |   | X |   |   |
|`insert post same key`  |   | Xo|   |   |   |   |
|`union post`            |   | Xo| Xo|   |   | X |

|Metamorphic Properties|#1 |#2 |#3 |#4 |#5 |#8 |
|:---------------------|:-:|:-:|:-:|:-:|:-:|:-:|
|`insert insert weak`  | X |   |   |   |   |   |
|`insert insert`       | X | X | X |   |   |   |
|`insert delete weak`  |   |   |   | X |   |   |
|`insert delete`       |   | X | X | X |   |   |
|`insert union`        |   | Ox| Ox|   |   | X |
|`delete nil`          |   |   |   |   |   |   |
|`delete insert weak`  |   |   |   | X |   |   |
|`delete insert`       | X | X |   | X | X |   |
|`delete delete`       |   |   |   | X | Ox|   |
|`delete union`        | X | X |   | X | X | Ox|
|`union nil1`          |   |   |   |   |   |   |
|`union nil2`          |   |   |   |   |   |   |
|`union delete insert` | X | X | X | X | X | Ox|
|`union union idem`    |   | Xo|   |   |   |   |
|`union union assoc`   |   |   |   |   |   | Ox|
|`find nil`            |   |   |   |   |   |   |
|`find insert`         | X | X | X |   |   |   |
|`find delete`         |   |   |   | X | X |   |
|`find union`          | Xo| Xo|   |   |   | X |


|Equivalence Properties        |#1 |#2 |#3 |#4 |#5 |#8 |
|:-----------------------------|:-:|:-:|:-:|:-:|:-:|:-:|
|`insert preserves equivalence`|   |   |   |   |   |   |
|`delete preserves equivalence`| X |   |   | X | X |   |
|`union preserves equivalence` | X |   |   |   |   | Ox|
|`find preserves equivalence`  | X |   |   |   |   |   |

|Insert Completeness         |#1 |#2 |#3 |#4 |#5 |#8 |
|:---------------------------|:-:|:-:|:-:|:-:|:-:|:-:|
|`insert complete`           |   |   |   |   |   |   |
|`insert complete for union` | Ox|   |   |   |   |   |
|`insert complete for delete`|   |   |   |   |   |   |


|Model-based Properties|#1 |#2 |#3 |#4 |#5 |#8 |
|:---------------------|:-:|:-:|:-:|:-:|:-:|:-:|
|`nil model`           |   |   |   |   |   |   |
|`insert model`        | X | X | X |   |   |   |
|`delete model`        |   |   |   | X | X |   |
|`union model`         | X | X | Xo|   |   | X |
|`find model`          |   |   |   |   |   |   |

> We make the following observations.


### 5.1&nbsp;&nbsp; Bug finding effectiveness
 
> Validity properties miss many bugs (five of six), as do “preservation of equivalence” and “completeness of insertion” properties. In contrast, every bug is found by at least one postcondition, metamorphic property, and model-based property.
>
> Invalid test data provokes false positives. Bug #2, which causes invalid trees to be generated as test cases, causes many properties that do not use insert to fail. This is why property `arbitrary_valid` is so important — when it fails, we need not waste time debugging false positives in properties unrelated to the bug. Because of these false positives, we ignore bug #2 in the rest of this discussion.

In the _jqwik_ implementation bug #2 does not make generated trees invalid
since duplicate keys are filtered out from the beginning.

> Model-based properties are effective at finding bugs; each property tests just one operation, and finds every bug in that operation. In fact, the model-based properties together form a complete specification of the code, and so should be expected to find every bug.
>
> Postconditions are quite effective; each postcondition for a buggy operation finds all the bugs we planted in it, but some postconditions are less effective than we might expect. For example, property `find_post_present` uses both find and insert, so we might expect it to reveal the three bugs in insert, but it reveals only two of them.
>
> Metamorphic properties are less effective individually, but powerful in combination. Weak properties miss bugs (compare each line ending in Weak with the line below), because their preconditions to exclude tricky test cases result in tricky bugs escaping detection. But even stronger-looking properties that we might expect to find bugs miss them — property `insert_delete` misses bug #1 in insert, property `delete_insert` misses bug #3 in insert, and so on. Degenerate metamorphic properties involving `nil` are particularly ineffective. Metamorphic properties are essentially an axiomatization of the API under test, and there is no guarantee that this axiomatization is complete, so some bugs might be missed altogether.

#### Differences between QuickCheck and _jqwik_ Bug Hunting Results

As you can see in the table there is a bit of difference between the 
QuickCheck results and the properties run with _jqwik_: `Ox` means that
QuickCheck found a bug where _jqwik_ did not - 
`Xo` is the other way round. 
There are several potential causes for those differences:

- In the paper the bugs are only described in prose. 
  Thus my implementations are probably different than those 
  done by the original authors.
- Data generation - especially the distribution of values across the 
  full integer range - differs between QuickCheck and _jqwik_ 
  which can lead to more or less collisions and thus to 
  better or worse bug detection.
- Some of the properties are not fully specified in the original
  paper. My interpretation of the property's name might not fit 
  the actual Haskell code.
  
All in all, however, the results are quite similar and allow the same
conclusions.

### 5.2&nbsp;&nbsp; Bug finding performance

The text in this section is without any changes and without adaptation 
to the jqwik properties since I have _not_ replicated the 
statistical analysis. I might do that later, though.

> Hitherto we have discussed which properties can find bugs, given enough testing time. But it also matters how quickly a property can find a bug. We measured the mean time to failure across seven of the eight bugs (omitting bug #2), for every failing postcondition, (non-weak) metamorphic property, and model-based property. Each mean-time-to-failure was measured by testing the property 1,000 times with different random seeds, and taking the mean number of tests needed to provoke the failure. The results are summarized below:
>
> |Property type|Min|Max|Mean|
> |-------------|---|---|----|
> |Postcondition|7.1|245|77  |
> |Metamorphic  |2.4|714|56  | 
> |Model-based  |3.1|9.8|5.8 |
>
> In this example model-based properties find bugs far faster than postconditions or metamorphic properties, while metamorphic properties find bugs a little faster than postconditions on average, but their mean time to failure varies more.
> 
> Digging a little deeper, for the same bug in union, `prop_UnionPost` fails after 50 tests on average, while `prop_UnionModel` fails after only 8.4 tests, even though they are logically equivalent. The reason is that after computing a union that is affected by the bug, the model-based property checks that the model of the result is correct — which requires every key and value to be correct. The post-condition, on the other hand, checks that a random key has the correct value in the result. Thus `prop_UnionPost` may exercise the bug many times without detecting it. Each model-based test may take a little longer to run, because it validates the result of union more thoroughly, but this is not significant compared to the enormous difference in the number of tests required to find the bug.
>

### 5.3&nbsp;&nbsp; Lessons

> These results suggest that, if time is limited, then writing model-based properties may offer the best return on investment, in combination with validity properties to ensure we don’t encounter confusing failures caused by invalid data. In situations where the model is complex (and thus expensive) to define, or where the model resembles the implementation so closely that the same bugs are likely in each, then metamorphic properties offer an effective alternative, at the cost of writing many more properties.
>

## 6&nbsp;&nbsp; Discussion

> We have discussed a number of different kinds of property that a developer can try to formulate to test an implementation: invariant properties, postconditions, metamorphic properties, inductive properties, and model-based properties. Each kind of property is based on a widely applicable idea, usable in many different settings. When writing metamorphic properties, we discovered the need to define equivalence of data structures, and thus also to define properties that test for preservation of equivalence. We discussed the importance of completeness — our test data generator should be able to generate any test case — and saw how to test this. We saw the importance of testing both our generators and our shrinkers, to ensure that other properties are tested with valid data. We saw how to measure the distribution of test data, to ensure that test effort is well spent.
>
> Model-based testing seemed the most effective approach overall, revealing all our bugs with a small number of properties, and generally finding bugs fast. But metamorphic testing was a fertile source of ideas, and was almost as effective at revealing bugs, so is a useful alternative, especially in situations where a model is expensive to construct.
> 
> We saw that some properties must use equivalence to compare values, while other properties must use structural equality. Thus, we need two notions of “equality” for the data structures under test. In fact, it is the equivalence which ought to be exported as the equality instance for binary search trees, because structural equality distinguishes representations that ought to be considered equal outside the abstraction barrier of the abstract data type. Yet we need to use structural equality in some properties, and of course, we want to use the derived Eq instance for the representation datatype for this. So we appear to need two Eq instances for the same type! 

In Java this translates to "we need two `equals` methods for the 
same type". This is obviously not possible - like you cannot have 
two Eq instances in Haskell for the same type.

> The solution to this conundrum is to define two types: a data type of representations with a derived structural equality, which is not exported to clients, and a newtype isomorphic to this datatype, which is exported, with an Eq instance which defines equality to be equivalence. This approach does mean that some properties must be inside the abstraction barrier of the data type, and thus must be placed in the same module as the implementation, which may not be desirable as it mixes test code and implementation code. An alternative is to define an Internals module which exports the representation type, and can be imported by test code, but is not used by client modules.

A direct transfer of this idea would require to wrap the implementing
class - having a structural equality - within a public type that offers
equivalence as equality.

> The ideas in this paper are applicable to testing any pure code, but code with side-effects demands a somewhat different approach. In this case, every operation has an implicit “state” argument, and an invisible state result, making properties harder to formulate. Test cases are sequences of operations, to set up the state for each operation under test, and to observe changes made to the state afterwards. Nevertheless, the same ideas can be adapted to this setting; in particular, there are a number of state-machine modelling libraries for property-based testing tools that support a “model-based” approach in a stateful setting. State machine modelling is heavily used at Quviq AB for testing customer software, and an account of some of these examples can be found 
> [here](https://publications.lib.chalmers.se/records/fulltext/232550/local_232550.pdf).

The code examples in this articles have shown that the pure code approach
can be used in Java as well. However, the implementation of `BST` with
its immutable interface is neither memory-efficient nor does it perform 
exceptionally well. Immutable data structures are not very common in
Java but they are not totally unheard of; functional libraries (e.g. 
[vavr](https://www.vavr.io/)) have them as well as multi-function 
libraries like [Eclipse Collections](https://www.eclipse.org/collections/). 

Translating the properties
to mutable data structures would require some copying of in-between 
states in order to have them ready for later equality and equivalence 
checking. Consider the translation of metamorphic property `insert_insert`
to a stateful `java.util.TreeMap`:

```java
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
```

It's more clumsy than the original but very similar in structure.
If you change the `original` - instead of copying it first - reporting
of failures might become confusing since reporting will show the state
of the generated parameter _after_ the property was run, and not as
we might expect it _before_ execution.

> We hope the reader will find the ideas in this paper helpful in developing effective property-based tests in the future.

## Personal Addendum

Understanding the 
[original paper](https://www.dropbox.com/s/tx2b84kae4bw1p4/paper.pdf)
and transferring the code to Java taught me a lot about good and 
efficient properties. As maintainer of [jqwik](https://jqwik.net) I was
also confronted with weaknesses of the library and chances to improve it.

The section about bug hunting triggered another question: How good are
the kind of unit tests I usually write at detecting bugs. 

### Bug Hunting with Unit Tests

Initially, I had created the class `BST` using test-driven development
before I started to apply all the different kind properties. You can find the resulting suite of unit tests 
[here](https://github.com/jlink/how-to-specify-it/blob/master/src/test/java/htsi/bst/BST_Tests.javaa). 
Those tests did _not_ lead me to a bug-free implementation; the most 
serious error I had made was in the deletion code in which full subtrees
could sometimes get lost. So writing and running the properties
definitely helped me weed out some of my blunders.
 
However, when using the unit tests to run them against the bugs from
[section 5 on bug hunting](#5-bug-hunting) the result was the following:

|Failing Unit Tests|#1 |#2 |#3 |#4 |#5 |#8 |
|:-----------------|:-:|:-:|:-:|:-:|:-:|:-:|
|failure count     | 7 | 3 | 2 | 1 | 1 | 1 |

The 11 tests were able to detect each of the bugs. 
Comparing the efficacy of properties vs unit tests might be an 
interesting topic for further research. I'd be happy to participate!

