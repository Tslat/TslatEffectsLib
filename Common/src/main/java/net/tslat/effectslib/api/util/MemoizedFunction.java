package net.tslat.effectslib.api.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.function.Function;

/**
 * Memoizing function that caches the result of computed values to save additional lookup on future calls.
 * <p>Similar to {@link net.minecraft.Util.memoize() Util#memoize}, but utilises FastUtil instead of a concurrent map for a faster implementation</p>
 */
public class MemoizedFunction<I, O> implements Function<I, O> {
    private final Map<I, O> cache = new Object2ObjectOpenHashMap<>();
    private final Function<I, O> function;

    public MemoizedFunction(Function<I, O> function) {
        this.function = function;
    }

    @Override
    public O apply(I i) {
        return this.cache.computeIfAbsent(i, this.function);
    }
}
