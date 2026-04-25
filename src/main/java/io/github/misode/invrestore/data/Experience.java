package io.github.misode.invrestore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Experience(float points, int levels) {
    public static final Codec<Experience> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("points").forGetter(xp -> xp.points),
            Codec.INT.fieldOf("levels").forGetter(xp -> xp.levels)
    ).apply(instance, Experience::new));
}
