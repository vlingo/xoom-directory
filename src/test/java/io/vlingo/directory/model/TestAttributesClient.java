package io.vlingo.directory.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vlingo.cluster.model.attribute.Attribute;
import io.vlingo.cluster.model.attribute.AttributeSet;
import io.vlingo.cluster.model.attribute.AttributesProtocol;
import io.vlingo.cluster.model.attribute.TrackedAttribute;

public class TestAttributesClient implements AttributesProtocol {
  private final Map<String, AttributeSet> attributeSets;
  
  public TestAttributesClient() {
    attributeSets = new HashMap<>();
  }
  
  @Override
  public <T> void add(final String attributeSetName, final String attributeName, final T value) {
    AttributeSet set = attributeSets.get(attributeSetName);
    if (set == null) {
      set = AttributeSet.named(attributeSetName);
      attributeSets.put(attributeSetName, set);
    }
    set.addIfAbsent(Attribute.from(attributeName, value));
  }

  @Override
  public Collection<AttributeSet> all() {
    return attributeSets.values();
  }

  @Override
  public Collection<Attribute<?>> allOf(final String attributeSetName) {
    final List<Attribute<?>> all = new ArrayList<>();
    final AttributeSet set = attributeSets.get(attributeSetName);
    if (set.isDefined()) {
      for (final TrackedAttribute tracked : set.all()) {
        if (tracked.isPresent()) {
          all.add(tracked.attribute);
        }
      }
    }
    return all;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Attribute<T> attribute(String attributeSetName, String attributeName) {
    final AttributeSet set = attributeSets.get(attributeSetName);
    if (set.isDefined()) {
      final TrackedAttribute tracked = set.attributeNamed(attributeName);
      if (tracked.isPresent()) {
        return (Attribute<T>) tracked.attribute;
      }
    }
    return (Attribute<T>) Attribute.Undefined;
  }

  @Override
  public <T> void replace(String attributeSetName, String attributeName, T value) {
    final AttributeSet set = attributeSets.get(attributeSetName);
    
    if (!set.isNone()) {
      final TrackedAttribute tracked = set.attributeNamed(attributeName);
      
      if (tracked.isPresent()) {
        final Attribute<T> other = Attribute.from(attributeName, value);
        
        if (!tracked.sameAs(other)) {
          set.replace(tracked.replacingValueWith(other));
        }
      }
    }
  }

  @Override
  public <T> void remove(String attributeSetName, String attributeName) {
    final AttributeSet set = attributeSets.get(attributeSetName);
    
    if (!set.isNone()) {
      final TrackedAttribute tracked = set.attributeNamed(attributeName);
      
      if (tracked.isPresent()) {
        set.remove(tracked.attribute);
      }
    }    
  }

  @Override
  public <T> void removeAll(String attributeSetName) {
    attributeSets.remove(attributeSetName);
  }
}
