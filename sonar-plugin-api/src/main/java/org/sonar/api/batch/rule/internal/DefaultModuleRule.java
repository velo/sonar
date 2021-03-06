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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.batch.rule.ModuleRule;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

@Immutable
class DefaultModuleRule implements ModuleRule {
  private final RuleKey ruleKey;
  private final String severity, engineKey;
  private final Map<String, String> params;

  DefaultModuleRule(NewModuleRule newModuleRule) {
    this.severity = newModuleRule.severity;
    this.engineKey = newModuleRule.engineKey;
    this.ruleKey = newModuleRule.ruleKey;
    this.params = ImmutableMap.copyOf(newModuleRule.params);
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  @Override
  public String severity() {
    return severity;
  }

  @Override
  public String param(String key) {
    return params.get(key);
  }

  @Override
  public Map<String, String> params() {
    // immutable
    return params;
  }

  @Override
  public String engineKey() {
    return engineKey;
  }
}
