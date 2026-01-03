package io.github.misode.invrestore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.misode.invrestore.InvRestore;
import io.github.misode.invrestore.RandomBase62;
import io.github.misode.invrestore.Styles;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public record Snapshot(String id, Event event, UUID playerUuid, String playerName, Instant time, ResourceKey<Level> dimension, Vec3 position, SnapshotItems contents, float health, Hunger hunger, Experience xp, List<MobEffectInstance> effects, PlayerAdvancements.Data advancements, List<ResourceKey<Recipe<?>>> recipes, Map<Stat<?>, Integer> stats) implements Comparable<Snapshot> {
    public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(b -> b.group(
            Codec.STRING.fieldOf("id").forGetter(Snapshot::id),
            Event.CODEC.fieldOf("event").forGetter(Snapshot::event),
            UUIDUtil.CODEC.fieldOf("player_uuid").forGetter(Snapshot::playerUuid),
            Codec.STRING.fieldOf("player_name").forGetter(Snapshot::playerName),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time").forGetter(Snapshot::time),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Snapshot::dimension),
            Vec3.CODEC.fieldOf("position").forGetter(Snapshot::position),
            SnapshotItems.CODEC.fieldOf("contents").forGetter(Snapshot::contents),
            Codec.FLOAT.fieldOf("health").forGetter(Snapshot::health),
            Hunger.CODEC.fieldOf("hunger").forGetter(Snapshot::hunger),
            Experience.CODEC.fieldOf("xp").forGetter(Snapshot::xp),
            effectListCodec().fieldOf("effects").forGetter(Snapshot::effects),
            PlayerAdvancements.Data.CODEC.fieldOf("advancements").forGetter(Snapshot::advancements),
            Recipe.KEY_CODEC.listOf().fieldOf("recipes").forGetter(Snapshot::recipes),
            ServerStatsCounter.STATS_CODEC.fieldOf("stats").forGetter(Snapshot::stats)
    ).apply(b, Snapshot::new));

    public static Snapshot create(ServerPlayer player, Event event) {
        String id = RandomBase62.generate(12);
        UUID playerUuid = player.getUUID();
        String playerName = player.getGameProfile().getName();
        SnapshotItems contents = SnapshotItems.fromPlayer(player);
        float health = player.getHealth();
        FoodData foodData = player.getFoodData();
        Hunger hunger = new Hunger(foodData.getFoodLevel(), foodData.getSaturationLevel());
        Experience xp = new Experience(Mth.floor(player.experienceProgress * player.getXpNeededForNextLevel()), player.experienceLevel);
        List<MobEffectInstance> effects = player.getActiveEffects().stream().toList();
        for (var effect : effects) {
            InvRestore.LOGGER.info("effect: {}, amplifier: {}, duration: {}", effect.getEffect(), effect.getDuration(), effect.getAmplifier());
        }
        PlayerAdvancements.Data advancements = player.getAdvancements().asData();
        ServerRecipeBook.Packed recipeBook = player.getRecipeBook().pack();
        List<ResourceKey<Recipe<?>>> recipes = recipeBook.known();
        Map<Stat<?>, Integer> statsMap = new HashMap<>();
        Registry<StatType<?>> statTypeRegistry = BuiltInRegistries.STAT_TYPE;
        statTypeRegistry.forEach(statType -> {
            Registry<?> registry = statType.getRegistry();
            registry.keySet().forEach(resourceLocation -> {
                Object value = registry.getValue(resourceLocation);
                Pair<Stat<?>, Integer> stat = createTypedStat(statType, value, player.getStats());
                if (stat != null) {
                    statsMap.put(stat.getA(), stat.getB());
                }
            });
        });
        return new Snapshot(id, event, playerUuid, playerName, Instant.now(), player.level().dimension(), player.position(), contents, health, hunger, xp, effects, advancements, recipes, statsMap);
    }

    @SuppressWarnings("unchecked")
    private static <T> Pair<Stat<?>, Integer> createTypedStat(StatType<T> statType, Object value, StatsCounter statsCounter) {
        T typedValue = (T) value;
        Stat<T> stat = statType.get(typedValue);
        int statInt = statsCounter.getValue(stat);
        if (statInt != 0) {
            return new Pair<>(stat, statInt);
        }
        return null;
    }

    private static Codec<List<MobEffectInstance>> effectListCodec() {
        return MobEffectInstance.CODEC.listOf().orElse(List.of()).xmap(effect -> {
            List<MobEffectInstance> effects = NonNullList.create();
            effects.addAll(effect);
            return effects;
        }, mobEffects -> {
            List<MobEffectInstance> effects = new ArrayList<>(mobEffects);
            return effects;
        });
    }

    public static Snapshot fromDeath(ServerPlayer player, DamageSource source) {
        return Snapshot.create(player, new DeathEvent(source.getLocalizedDeathMessage(player).getString()));
    }

    public static Snapshot fromJoin(ServerPlayer player) {
        return Snapshot.create(player, JoinEvent.INSTANCE);
    }

    public static Snapshot fromDisconnect(ServerPlayer player) {
        return Snapshot.create(player, DisconnectEvent.INSTANCE);
    }

    public static Snapshot fromLevelChange(ServerPlayer player, ResourceKey<Level> origin) {
        return Snapshot.create(player, new LevelChangeEvent(origin, player.level().dimension()));
    }

    @Override
    public int compareTo(@NotNull Snapshot o) {
        return -this.time.compareTo(o.time);
    }

    public String formatTimeAgo() {
        Duration duration = Duration.between(this.time, Instant.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s ago";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m ago";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h ago";
        } else {
            return (seconds / 86400) + "d ago";
        }
    }

    public String formatPos() {
        DecimalFormat f = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return f.format(this.position.x) + " " + f.format(this.position.y) + " " + f.format(this.position.z);
    }

    public void restoreEverything(ServerPlayer player) {
        this.restorePosition(player);
        this.restoreExperience(player);
        this.restoreSaturation(player);
        this.restoreFood(player);
        this.restoreHealth(player);
        this.restoreEffects(player);
        this.restoreAdvancements(player);
        this.restoreRecipes(player);
        this.restoreStats(player);
    }

    public void restorePosition(ServerPlayer player) {
        killNearbyItemXp(player);

        player.teleportTo(
                player.level().getServer().getLevel(this.dimension()),
                this.position().x, this.position().y, this.position().z,
                Set.of(),
                player.getYRot(), player.getXRot(),
                false
        );
    }

    private void killNearbyItemXp(ServerPlayer player) {
        var server = player.level().getServer();
        var serverLevel = player.level();
        var commandSourceStack = new CommandSourceStack(
                CommandSource.NULL,
                Vec3.atLowerCornerOf(serverLevel.getSharedSpawnPos()),
                Vec2.ZERO,
                serverLevel,
                3,
                "Server",
                Component.literal("Server"),
                server,
                player
        );

        double x = position.x;
        double y = position.y;
        double z = position.z;

        String itemArgument = "@e[type=item,distance=..15,x=%s,y=%s,z=%s]".formatted(x, y, z);
        server.getCommands().performPrefixedCommand(commandSourceStack,
                "/kill " + itemArgument
        );

        String xpArgument = "@e[type=experience_orb,distance=..15,x=%s,y=%s,z=%s]".formatted(x, y, z);
        server.getCommands().performPrefixedCommand(commandSourceStack,
                "/kill " + xpArgument
        );
    }

    public void restoreExperience(ServerPlayer player) {
        player.setExperienceLevels(this.xp().levels());
        player.setExperiencePoints((int) this.xp().points());
    }

    public void restoreSaturation(ServerPlayer player) {
        player.getFoodData().setSaturation(this.hunger().saturation());
    }

    public void restoreFood(ServerPlayer player) {
        player.getFoodData().setFoodLevel(this.hunger().food());
    }

    public void restoreHealth(ServerPlayer player) {
        if (this.health() != 0.0f) {
            player.setHealth(this.health());
        }
    }

    private void restoreEffects(ServerPlayer player) {
        player.removeAllEffects();
        for (var effect : effects) {
            player.addEffect(effect);
        }
    }

    private void restoreAdvancements(ServerPlayer player) {
        player.getAdvancements().applyFrom(player.level().getServer().getAdvancements(), advancements);
    }

    private void restoreRecipes(ServerPlayer player) {
        RecipeManager recipeManager = player.level().getServer().getRecipeManager();
        player.getRecipeBook().removeRecipes(recipeManager.getRecipes(), player);
        Collection<RecipeHolder<?>> recipeHolders = new ArrayList<>();
        recipes.forEach(recipeKey -> recipeHolders.add(recipeManager.byKey(recipeKey).get()));
        player.getRecipeBook().addRecipes(recipeHolders, player);
    }

    private void restoreStats(ServerPlayer player) {
        for (var stat : BuiltInRegistries.STAT_TYPE) {
            resetStatsForType(player, stat);
        }
        for (Map.Entry<Stat<?>, Integer> stat : stats.entrySet()) {
            player.awardStat(stat.getKey(), stat.getValue());
        }
    }

    private <T> void resetStatsForType(ServerPlayer player, StatType<T> statType) {
        Registry<T> registry = statType.getRegistry();
        for (ResourceLocation id : registry.keySet()) {
            T entry = registry.getValue(id);
            if (entry != null) {
                player.resetStat(statType.get(entry));
            }
        }
    }

//    public String formatHunger() {
//        DecimalFormat f = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
//        return this.hunger.food() + " " + f.format(this.hunger.saturation());
//    }
//
//    public String formatXp() {
//        DecimalFormat f = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
//        return f.format(this.xp.points()) + " " + this.xp.levels();
//    }

    public interface Event {
        EventType<?> getType();
        MutableComponent formatEmoji(boolean dark);
        MutableComponent formatVerb();

        Codec<Event> CODEC = EventType.REGISTRY.byNameCodec()
                .dispatch("type", Event::getType, EventType::codec);

        EventType<DeathEvent> DEATH = register("death", new EventType<>(DeathEvent.CODEC));
        EventType<JoinEvent> JOIN = register("join", new EventType<>(JoinEvent.CODEC));
        EventType<DisconnectEvent> DISCONNECT = register("disconnect", new EventType<>(DisconnectEvent.CODEC));
        EventType<LevelChangeEvent> LEVEL_CHANGE = register("level_change", new EventType<>(LevelChangeEvent.CODEC));
        EventType<AutoSaveEvent> AUTO_SAVE = register("auto_save", new EventType<>(AutoSaveEvent.CODEC));

        private static <T extends Event> EventType<T> register(String name, EventType<T> type) {
            return Registry.register(EventType.REGISTRY, InvRestore.id(name), type);
        }
    }

    public record EventType<T extends Event>(MapCodec<T> codec) {
        public static final Registry<EventType<?>> REGISTRY = new MappedRegistry<>(
                ResourceKey.createRegistryKey(InvRestore.id("snapshot_event")), Lifecycle.stable());
    }

    public record DeathEvent(String deathMessage) implements Event {
        public static final MapCodec<DeathEvent> CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
                Codec.STRING.fieldOf("death_message").forGetter(DeathEvent::deathMessage)
        ).apply(b, DeathEvent::new));

        @Override
        public EventType<DeathEvent> getType() {
            return Event.DEATH;
        }

        @Override
        public MutableComponent formatEmoji(boolean dark) {
            return Component.literal("☠")
                    .withStyle(dark ? Styles.GUI_DEATH : Styles.CHAT_DEATH);
        }

        @Override
        public MutableComponent formatVerb() {
            return Component.literal("died").withStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(this.deathMessage)
                            .withStyle(Styles.LIST_HIGHLIGHT)))
            );
        }
    }

    public record JoinEvent() implements Event {
        public static final JoinEvent INSTANCE = new JoinEvent();
        public static final MapCodec<JoinEvent> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public EventType<JoinEvent> getType() {
            return Event.JOIN;
        }

        @Override
        public MutableComponent formatEmoji(boolean dark) {
            return Component.literal("▶")
                    .withStyle(dark ? Styles.GUI_JOIN : Styles.CHAT_JOIN);
        }

        @Override
        public MutableComponent formatVerb() {
            return Component.literal("joined");
        }
    }

    public record DisconnectEvent() implements Event {
        public static final DisconnectEvent INSTANCE = new DisconnectEvent();
        public static final MapCodec<DisconnectEvent> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public EventType<DisconnectEvent> getType() {
            return Event.DISCONNECT;
        }

        @Override
        public MutableComponent formatEmoji(boolean dark) {
            return Component.literal("◀")
                    .withStyle(dark ? Styles.GUI_DISCONNECT : Styles.CHAT_DISCONNECT);
        }

        @Override
        public MutableComponent formatVerb() {
            return Component.literal("left");
        }
    }

    public record LevelChangeEvent(ResourceKey<Level> origin, ResourceKey<Level> destination) implements Event {
        public static final MapCodec<LevelChangeEvent> CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("origin").forGetter(LevelChangeEvent::origin),
                Level.RESOURCE_KEY_CODEC.fieldOf("destination").forGetter(LevelChangeEvent::destination)
        ).apply(b, LevelChangeEvent::new));

        @Override
        public EventType<LevelChangeEvent> getType() {
            return Event.LEVEL_CHANGE;
        }

        @Override
        public MutableComponent formatEmoji(boolean dark) {
            return Component.literal("🔀")
                    .withStyle(dark ? Styles.GUI_LEVEL_CHANGE : Styles.CHAT_LEVEL_CHANGE);
        }

        @Override
        public MutableComponent formatVerb() {
            return Component.literal("traveled").withStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent.ShowText(Component.empty()
                            .append(Component.literal(formatLevel(origin)).withStyle(Styles.LIST_HIGHLIGHT))
                            .append(Component.literal(" ➡ ").withStyle(Styles.LIST_DEFAULT))
                            .append(Component.literal(formatLevel(destination)).withStyle(Styles.LIST_HIGHLIGHT))
                    ))
            );
        }

        private static String formatLevel(ResourceKey<Level> level) {
            if (level.location().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                return level.location().getPath();
            }
            return level.location().toString();
        }
    }

    public record AutoSaveEvent() implements Event {
        public static final AutoSaveEvent INSTANCE = new AutoSaveEvent();
        public static final MapCodec<AutoSaveEvent> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public EventType<AutoSaveEvent> getType() {
            return Event.AUTO_SAVE;
        }

        @Override
        public MutableComponent formatEmoji(boolean dark) {
            return Component.literal("⌚")
                    .withStyle(dark ? Styles.GUI_AUTO_SAVE : Styles.CHAT_AUTO_SAVE);
        }

        @Override
        public MutableComponent formatVerb() {
            return Component.literal("auto-saved");
        }
    }
}