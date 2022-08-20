# CombatComponents
Minestom extension for item's tags driven combat system.


## Usage
Put the extension jar in the extensions folder of your server and implement your own combat components in your server or other extension.

### Example
```java
// register your own combat components
eventNode.addListener(CombatLoadingEvent.class, event -> {
    event.builder().addComponent("set", (context, data) -> {
        if (data instanceof NBTFloat nbtFloat)
            context.modifyDamage(damage -> nbtFloat.getValue());
    });
    event.builder().addComponent("add_mul", (context, data) -> {
        if (data instanceof NBTCompound compound && compound.getFloat("add") != null && compound.getFloat("mul") != null) {
            context.modifyDamage(damage -> (damage + compound.getFloat("add")) * compound.getFloat("mul"));
        }
    });
});

// create item with combat components
ItemStack item = ItemStack.builder(Material.STONE)
        .set(CombatExtension.COMBAT_TAG, List.of(
                new CombatComponent("set", new NBTFloat(5f)),
                new CombatComponent("add_mul", NBT.Compound(builder -> {
                    builder.put("add", new NBTFloat(5f));
                    builder.put("mul", new NBTFloat(5f));
                }))
        ))
        .build();
```