/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.component;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.component.Component;
import org.sonar.api.utils.Paging;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.7
 */
public class DefaultComponentFinder {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultComponentFinder.class);

  public DefaultComponentQueryResult find(ComponentQuery query, List<Component> allComponents) {
    LOG.debug("ComponentQuery : {}", query);
    Collection<Component> foundComponents = search(query, allComponents);

    // Sort components
    Collection<? extends Component> sortedComponents = new ComponentsFinderSort(foundComponents, query).sort();

    // Apply pagination if needed
    if (ComponentQuery.NO_PAGINATION == query.pageSize()) {
      return new DefaultComponentQueryResult(sortedComponents).setQuery(query);
    } else {
      Paging paging = Paging.create(query.pageSize(), query.pageIndex(), foundComponents.size());
      Collection<? extends Component> pagedComponents = pagedComponents(sortedComponents, paging);
      return new DefaultComponentQueryResult(pagedComponents).setPaging(paging).setQuery(query);
    }
  }

  private Collection<Component> search(final ComponentQuery query, List<? extends Component> allComponents) {
    return newArrayList(Iterables.filter(allComponents, new Predicate<Component>() {
      @Override
      public boolean apply(Component component) {
        return new KeyFilter().accept(component, query.keys()) &&
          new NameFilter().accept(component, query.names());
      }
    }));
  }

  abstract static class Filter {

    abstract String field(Component component);

    final boolean accept(Component component, Collection<String> collections) {
      if (!collections.isEmpty()) {
        for (String item : collections) {
          if (field(component).toLowerCase().contains(item.toLowerCase())) {
            return true;
          }
        }
        return false;
      }
      return true;
    }
  }

  static class NameFilter extends Filter {
    @Override
    String field(Component component) {
      return component.name();
    }
  }

  static class KeyFilter extends Filter {
    @Override
    String field(Component component) {
      return component.key();
    }
  }

  private Collection<? extends Component> pagedComponents(Collection<? extends Component> components, Paging paging) {
    Set<Component> pagedComponents = Sets.newLinkedHashSet();
    int index = 0;
    for (Component component : components) {
      if (index >= paging.offset() && pagedComponents.size() < paging.pageSize()) {
        pagedComponents.add(component);
      } else if (pagedComponents.size() >= paging.pageSize()) {
        break;
      }
      index++;
    }
    return pagedComponents;
  }

}
