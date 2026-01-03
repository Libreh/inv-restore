package io.github.misode.invrestore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Hunger(int food, float saturation) {
    public static final Codec<Hunger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("food").forGetter(hunger -> hunger.food),
            Codec.FLOAT.fieldOf("saturation").forGetter(hunger -> hunger.saturation)
    ).apply(instance, Hunger::new));
}
