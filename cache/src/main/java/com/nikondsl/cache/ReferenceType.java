package com.nikondsl.cache;

/**
 * Type of references which will be used as wrappers for holding values.
 * By default SOFT references are used. That means that values will be removed from heap
 * just before OutOfMemoryError (OOME) appears. If there is no risk of OOME then values will remain
 * in heap and will behave as usual (STRONG) references. WEAK references type means that values
 * will be removed from heap just before minor GC collector routine.
 */
public enum ReferenceType {
	STRONG, //usual java references
	SOFT, //soft references as value holders, used by default
	WEAK; //weak references as value holders.
}
