# CombatComponents
Minestom extension for tags driven combat system.


## Usage
Create a listener for the event `CombatLoadingEvent` and register your combat components.

```java
eventNode.addListener(CombatLoadingEvent.class, event -> {
    event.builder().addComponent("component_name", (context, data) -> {
        // your code
    });
});
```

Add your components to item or entity tags.

```java
item.setTag(CombatExtension.COMBAT_TAG, List.of(
        new CombatComponent("component_1", null,
        new CombatComponent("component_2", new NBTFloat(10f)),
        // ...
        new CombatComponent("component_n", false, new NBTInt(1))
));
```

Set components preferences of entity (by default `List.of(ComponentHolderType.ITEM_IN_MAIN_HAND)`).

```java
entity.setTag(CombatExtension.COMPONENT_PREFERENCES_TAG, List.of(ComponentHolderType.ITEM_IN_MAIN_HAND, ComponentHolderType.ENTITY));
```


## Building
Build using `gradlew build`